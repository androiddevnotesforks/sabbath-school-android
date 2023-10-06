/*
 * Copyright (c) 2023. Adventech <info@adventech.io>
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

package ss.lessons.impl.repository

import app.ss.models.SSDay
import app.ss.models.SSLesson
import app.ss.models.SSLessonInfo
import app.ss.models.SSRead
import app.ss.models.SSReadComments
import app.ss.models.SSReadHighlights
import app.ss.network.NetworkResource
import app.ss.network.safeApiCall
import app.ss.storage.db.dao.LessonsDao
import app.ss.storage.db.dao.ReadsDao
import app.ss.storage.db.entity.LessonEntity
import app.ss.storage.db.entity.ReadEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import ss.foundation.android.connectivity.ConnectivityHelper
import ss.foundation.coroutines.DispatcherProvider
import ss.foundation.coroutines.Scopable
import ss.foundation.coroutines.ioScopable
import ss.lessons.api.SSLessonsApi
import ss.lessons.api.repository.LessonsRepositoryV2
import ss.lessons.model.result.LessonReads
import ss.misc.DateHelper
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private const val LESSON_READS_DEBOUNCE = 450L

@Singleton
internal class LessonsRepositoryV2Impl @Inject constructor(
    private val lessonsApi: SSLessonsApi,
    private val lessonsDao: LessonsDao,
    private val readsDao: ReadsDao,
    private val connectivityHelper: ConnectivityHelper,
    private val dispatcherProvider: DispatcherProvider,
) : LessonsRepositoryV2, Scopable by ioScopable(dispatcherProvider) {

    private val today = DateTime.now().withTimeAtStartOfDay()

    override fun getLessonInfo(
        lessonIndex: String
    ): Flow<Result<SSLessonInfo>> = lessonsDao
        .getAsFlow(lessonIndex)
        .filterNotNull()
        .map { Result.success(it.toInfoModel()) }
        .onStart { syncLessonInfo(lessonIndex) }
        .flowOn(dispatcherProvider.io)
        .catch {
            Timber.e(it)
            emit(Result.failure(it))
        }

    private fun syncLessonInfo(lessonIndex: String) = scope.launch {
        val (language, lessonId, quarterlyId) = lessonIndex.run {
            Triple(
                substringBefore('-'),
                substringAfterLast('-'),
                substringAfter('-')
                    .substringBeforeLast('-')
            )
        }
        when (val response = safeApiCall(connectivityHelper) { lessonsApi.getLessonInfo(language, quarterlyId, lessonId) }) {
            is NetworkResource.Failure -> {
                Timber.e("Failed to fetch Lesson Info: isNetwork=${response.isNetworkError}, ${response.errorBody}")
            }

            is NetworkResource.Success -> response.value.body()?.let { info ->
                info.run {
                    lessonsDao.updateInfo(
                        lesson.index,
                        days,
                        pdfs,
                        lesson.title,
                        lesson.cover,
                        lesson.path,
                        lesson.full_path,
                        lesson.pdfOnly
                    )
                }
            }
        }
    }

    override fun getDayRead(day: SSDay): Flow<Result<SSRead>> = readsDao.getAsFlow(day.index)
        .filterNotNull()
        .map { Result.success(it.toModel()) }
        .onStart { syncRead(day) }
        .flowOn(dispatcherProvider.io)
        .catch {
            Timber.e(it)
            emit(Result.failure(it))
        }

    private fun syncRead(day: SSDay) = scope.launch {
        when (val response = safeApiCall(connectivityHelper) { lessonsApi.getDayRead("${day.full_read_path}/index.json") }) {
            is NetworkResource.Failure -> {
                Timber.e("Failed to fetch Day Read: isNetwork=${response.isNetworkError}, ${response.errorBody}")
            }

            is NetworkResource.Success -> response.value.body()?.let { info -> readsDao.insertItem(info.toEntity()) }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    override fun getLessonReads(lessonIndex: String): Flow<Result<LessonReads>> = getLessonInfo(lessonIndex)
        .flatMapLatest { result ->
            if (result.isSuccess) {
                val lessonInfo = result.getOrThrow()
                val readsMap = mutableMapOf<Int, SSRead>()
                val data = lessonInfo.days.map {
                    SSReadComments(it.index, emptyList()) to SSReadHighlights(it.index)
                }

                collectReads(lessonInfo)
                    .map { (index, readResult) ->
                        if (readResult.isSuccess) {
                            val ssDay = readResult.getOrThrow()
                            readsMap[index] = ssDay

                            Result.success(
                                LessonReads(
                                    readIndex = lessonInfo.findReadPosition(),
                                    lessonInfo = lessonInfo,
                                    reads = readsMap.entries
                                        .sortedBy { it.key }
                                        .map { it.value },
                                    comments = data.map { it.first },
                                    highlights = data.map { it.second },
                                )
                            )
                        } else {
                            Result.failure(
                                readResult.exceptionOrNull() ?: Throwable(
                                    "Failed to fetch Day Read: $lessonIndex, Day $index"
                                )
                            )
                        }

                    }
            } else {
                flowOf(
                    Result.failure(
                        result.exceptionOrNull() ?: Throwable("Failed to fetch Lesson Info: $lessonIndex")
                    )
                )
            }
        }
        .debounce(LESSON_READS_DEBOUNCE)
        .flowOn(dispatcherProvider.io)
        .catch {
            Timber.e(it)
            emit(Result.failure(it))
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun collectReads(lessonInfo: SSLessonInfo): Flow<Pair<Int, Result<SSRead>>> {
        return lessonInfo.days.withIndex().asFlow()
            .flatMapMerge { (index, day) -> getDayRead(day).map { index to it } }
    }

    private fun SSLessonInfo.findReadPosition(): Int {
        var readIndex = 0
        for ((index, ssDay) in days.withIndex()) {
            val startDate = DateHelper.parseDate(ssDay.date)
            if (startDate?.isEqual(today) == true && readIndex < 6) {
                readIndex = index
            }
        }
        return readIndex
    }

    private fun LessonEntity.toInfoModel(): SSLessonInfo = SSLessonInfo(
        lesson = SSLesson(
            title = title,
            start_date = start_date,
            end_date = end_date,
            cover = cover,
            id = id,
            index = index,
            path = path,
            full_path = full_path,
            pdfOnly = pdfOnly
        ),
        days = days,
        pdfs = pdfs
    )

    private fun ReadEntity.toModel(): SSRead = SSRead(
        index = index,
        id = id,
        date = date,
        title = title,
        content = content,
        bible = bible
    )

    private fun SSRead.toEntity(): ReadEntity = ReadEntity(
        index = index,
        id = id,
        date = date,
        title = title,
        content = content,
        bible = bible
    )
}