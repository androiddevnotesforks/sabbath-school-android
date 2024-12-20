/*
 * Copyright (c) 2024. Adventech <info@adventech.io>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package ss.resources.impl

import app.ss.models.feed.FeedGroup
import app.ss.models.feed.FeedType
import app.ss.models.resource.Resource
import app.ss.network.NetworkResource
import app.ss.network.safeApiCall
import kotlinx.coroutines.withContext
import ss.foundation.android.connectivity.ConnectivityHelper
import ss.foundation.coroutines.DispatcherProvider
import ss.prefs.api.SSPrefs
import ss.lessons.api.ResourcesApi
import ss.resources.api.ResourcesRepository
import ss.resources.model.FeedModel
import ss.resources.model.LanguageModel
import javax.inject.Inject
import dagger.Lazy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import ss.libraries.storage.api.dao.LanguagesDao
import ss.libraries.storage.api.entity.LanguageEntity
import ss.resources.impl.sync.SyncHelper
import timber.log.Timber

internal class ResourcesRepositoryImpl @Inject constructor(
    private val resourcesApi: ResourcesApi,
    private val languagesDao: LanguagesDao,
    private val syncHelper: SyncHelper,
    private val dispatcherProvider: DispatcherProvider,
    private val connectivityHelper: ConnectivityHelper,
    private val ssPrefs: Lazy<SSPrefs>,
) : ResourcesRepository {

    override fun languages(query: String?): Flow<List<LanguageModel>> {
        return return (if (query.isNullOrEmpty()) {
            languagesDao.get().onStart { syncHelper.syncLanguages() }
        } else {
            languagesDao.search("%$query%")
        }).map { entities -> entities.map { it.toModel() } }
            .flowOn(dispatcherProvider.io)
            .catch {
                Timber.e(it)
                emit(emptyList())
            }
    }

    override fun language(code: String): Flow<LanguageModel> =
        languagesDao.get(code)
            .onStart { syncHelper.syncLanguages() }
            .filterNotNull()
            .map { entity -> entity.toModel() }
            .flowOn(dispatcherProvider.io)
            .catch { Timber.e(it) }

    private fun LanguageEntity.toModel() = LanguageModel(
        code = code,
        name = name,
        nativeName = nativeName,
        devo = devo,
        pm = pm,
        aij = aij,
        ss = ss,
    )

    override suspend fun feed(type: FeedType): Result<FeedModel> {
        return withContext(dispatcherProvider.default) {
            when (val resource = safeApiCall(connectivityHelper) {
                resourcesApi.feed(
                    language = ssPrefs.get().getLanguageCode(),
                    type = type.name.lowercase()
                )
            }) {
                is NetworkResource.Failure -> {
                    Result.failure(Throwable("Failed to fetch feed, ${resource.errorBody}"))
                }

                is NetworkResource.Success -> {
                    resource.value.body()?.let {
                        Result.success(
                            FeedModel(title = it.title, it.groups)
                        )
                    } ?: Result.failure(Throwable("Failed to fetch feed, body is null"))
                }
            }
        }
    }

    override suspend fun feedGroup(id: String, type: FeedType): Result<FeedGroup> {
        return withContext(dispatcherProvider.default) {
            when (val resource = safeApiCall(connectivityHelper) {
                resourcesApi.feedGroup(
                    language = ssPrefs.get().getLanguageCode(),
                    type = type.name.lowercase(),
                    groupId = id
                )
            }) {
                is NetworkResource.Failure -> {
                    Result.failure(Throwable("Failed to fetch feed group, ${resource.errorBody}"))
                }

                is NetworkResource.Success -> {
                    resource.value.body()?.let {
                        Result.success(it)
                    } ?: Result.failure(Throwable("Failed to fetch feed group, body is null"))
                }
            }
        }
    }

    override suspend fun resource(index: String): Result<Resource> {
        return withContext(dispatcherProvider.default) {
            when (val resource = safeApiCall(connectivityHelper) {
                resourcesApi.resource(index)
            }) {
                is NetworkResource.Failure -> {
                    Result.failure(Throwable("Failed to fetch Resource, ${resource.errorBody}"))
                }

                is NetworkResource.Success -> {
                    resource.value.body()?.let {
                        Result.success(it)
                    } ?: Result.failure(Throwable("Failed to fetch Resource, body is null"))
                }
            }
        }
    }
}
