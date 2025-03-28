/*
 * Copyright (c) 2025. Adventech <info@adventech.io>
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

package ss.resources.impl.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import app.ss.network.NetworkResource
import app.ss.network.safeApiCall
import dagger.hilt.android.qualifiers.ApplicationContext
import io.adventech.blockkit.model.feed.FeedType
import io.adventech.blockkit.model.input.UserInputRequest
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ss.foundation.android.connectivity.ConnectivityHelper
import ss.foundation.coroutines.DispatcherProvider
import ss.foundation.coroutines.Scopable
import ss.foundation.coroutines.defaultScopable
import ss.lessons.api.ResourcesApi
import ss.libraries.storage.api.dao.BibleVersionDao
import ss.libraries.storage.api.dao.DocumentsDao
import ss.libraries.storage.api.dao.FeedDao
import ss.libraries.storage.api.dao.FeedGroupDao
import ss.libraries.storage.api.dao.LanguagesDao
import ss.libraries.storage.api.dao.ResourcesDao
import ss.libraries.storage.api.dao.SegmentsDao
import ss.libraries.storage.api.dao.UserInputDao
import ss.libraries.storage.api.entity.BibleVersionEntity
import ss.libraries.storage.api.entity.LanguageEntity
import ss.libraries.storage.api.entity.UserInputEntity
import ss.resources.impl.ext.localId
import ss.resources.impl.ext.toEntity
import ss.resources.impl.ext.toInput
import ss.resources.impl.ext.type
import ss.resources.impl.work.DownloadResourceWork
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

interface SyncHelper {
    fun syncLanguages()
    fun syncFeed(language: String, type: FeedType)
    fun syncFeedGroup(id: String, language: String, type: FeedType)
    fun syncDocument(index: String)
    fun syncUserInput(documentId: String)
    fun saveUserInput(documentId: String, userInput: UserInputRequest)
    fun syncSegment(index: String)
    fun syncResource(index: String)
    fun saveBibleVersion(language: String, version: String)
}

internal class SyncHelperImpl @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val resourcesApi: ResourcesApi,
    private val feedDao: FeedDao,
    private val feedGroupDao: FeedGroupDao,
    private val languagesDao: LanguagesDao,
    private val documentsDao: DocumentsDao,
    private val userInputDao: UserInputDao,
    private val resourcesDao: ResourcesDao,
    private val segmentsDao: SegmentsDao,
    private val bibleVersionDao: BibleVersionDao,
    private val connectivityHelper: ConnectivityHelper,
    private val dispatcherProvider: DispatcherProvider
) : SyncHelper, Scopable by defaultScopable(dispatcherProvider) {

    private val exceptionLogger = CoroutineExceptionHandler { _, exception -> Timber.e(exception) }

    override fun syncLanguages() {
        scope.launch(exceptionLogger) {
            when (val response = safeApiCall(connectivityHelper) { resourcesApi.languages() }) {
                is NetworkResource.Failure -> Unit
                is NetworkResource.Success -> response.value.body()?.let { data ->
                    withContext(dispatcherProvider.io) {
                        languagesDao.insertAll(data.map {
                            LanguageEntity(
                                code = it.code,
                                name = it.name,
                                nativeName = getNativeLanguageName(it.code, it.name),
                                devo = it.devo,
                                pm = it.pm,
                                aij = it.aij,
                                ss = it.ss,
                                explore = it.explore,
                            )
                        })
                    }
                }
            }
        }
    }

    override fun syncFeed(language: String, type: FeedType) {
        scope.launch(exceptionLogger) {
            when (val response = safeApiCall(connectivityHelper) { resourcesApi.feed(language, type.name.lowercase()) }) {
                is NetworkResource.Failure -> {
                    Timber.e("Failed to fetch feed: $language-$type => ${response.throwable?.message}")
                }
                is NetworkResource.Success -> response.value.body()?.let { data ->
                    withContext(dispatcherProvider.io) {
                        feedDao.insertItem(data.toEntity(language, type))
                    }
                }
            }
        }
    }

    override fun syncFeedGroup(id: String, language: String, type: FeedType) {
        scope.launch(exceptionLogger) {
            when (val response = safeApiCall(connectivityHelper) { resourcesApi.feedGroup(language, type.name.lowercase(), id) }) {
                is NetworkResource.Failure -> {
                    Timber.e("Failed to fetch feed group: $language-$type, $id => ${response.throwable?.message}")
                }
                is NetworkResource.Success -> response.value.body()?.let { data ->
                    withContext(dispatcherProvider.io) {
                        feedGroupDao.insertItem(data.toEntity())
                    }
                }
            }
        }
    }

    override fun syncDocument(index: String) {
        scope.launch(exceptionLogger) {
            when (val response = safeApiCall(connectivityHelper) { resourcesApi.document(index) }) {
                is NetworkResource.Failure -> {
                    Timber.e("Failed to fetch document for index: $index => ${response.throwable?.message}")
                }
                is NetworkResource.Success -> response.value.body()?.let { data ->
                    withContext(dispatcherProvider.io) {
                        documentsDao.insertItem(data.toEntity())
                        segmentsDao.insertAll(data.segments.orEmpty().map { it.toEntity() })
                    }
                }
            }
        }
    }

    override fun syncUserInput(documentId: String) {
        scope.launch(exceptionLogger) {
            when (val response = safeApiCall(connectivityHelper) { resourcesApi.userInput(documentId) }) {
                is NetworkResource.Failure -> {
                    Timber.e("Failed to fetch user input for documentId: $documentId => ${response.throwable?.message}")
                }
                is NetworkResource.Success -> response.value.body()?.let { data ->
                    withContext(dispatcherProvider.io) {
                        userInputDao.insertAll(data.map { input ->
                            // Compare timestamp with local timestamp here
                            UserInputEntity(
                                localId = input.localId(documentId),
                                id = input.id,
                                documentId = documentId,
                                input = input,
                                timestamp = input.timestamp,
                            )
                        })
                    }
                }
            }
        }
    }

    override fun saveUserInput(documentId: String, userInput: UserInputRequest) {
        scope.launch(exceptionLogger) {
            withContext(dispatcherProvider.io) {
                val localId = userInput.localId(documentId)
                val id = userInputDao.getId(localId)
                val timestamp = System.currentTimeMillis()

                userInputDao.insertItem(
                    UserInputEntity(
                        localId = localId,
                        id = id,
                        documentId = documentId,
                        input = userInput.toInput(id ?: localId, timestamp),
                        timestamp = timestamp,
                    )
                )
            }

            resourcesApi.saveUserInput(
                inputType = userInput.type(),
                documentId = documentId,
                blockId = userInput.blockId,
                userInput = userInput,
            )
        }
    }

    override fun syncSegment(index: String) {
        scope.launch(exceptionLogger) {
            when (val response = safeApiCall(connectivityHelper) { resourcesApi.segment(index) }) {
                is NetworkResource.Failure -> {
                    Timber.e("Failed to fetch segment for index: $index => ${response.throwable?.message}")
                }
                is NetworkResource.Success -> response.value.body()?.let { data ->
                    withContext(dispatcherProvider.io) {
                        segmentsDao.insertItem(data.toEntity())
                    }
                }
            }
        }
    }

    override fun syncResource(index: String) {
        scope.launch(exceptionLogger) {
            when (val response = safeApiCall(connectivityHelper) {
                resourcesApi.resource(index)
            }) {
                is NetworkResource.Failure -> {
                    Timber.e("Failed to fetch Resource for index: $index => ${response.throwable?.message}")
                }
                is NetworkResource.Success -> {
                    response.value.body()?.let {
                        withContext(dispatcherProvider.io) {
                            resourcesDao.insertItem(it.toEntity())
                        }
                    }
                    downloadResource(index)
                }
            }
        }
    }

    override fun saveBibleVersion(language: String, version: String) {
        scope.launch(exceptionLogger) {
            withContext(dispatcherProvider.io) {
                bibleVersionDao.insertItem(BibleVersionEntity(language, version))
            }
        }
    }

    private fun getNativeLanguageName(languageCode: String, languageName: String): String {
        val loc = Locale(languageCode)
        val name = loc.getDisplayLanguage(loc).takeUnless { it == languageCode } ?: languageName
        return name.replaceFirstChar { it.uppercase() }
    }

    private fun downloadResource(index: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<DownloadResourceWork>()
            .setConstraints(constraints)
            .setInputData(workDataOf(DownloadResourceWork.INDEX_KEY to index))
            .build()

        val workManager = WorkManager.getInstance(appContext)
        workManager.enqueueUniqueWork(
            DownloadResourceWork::class.java.simpleName,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
