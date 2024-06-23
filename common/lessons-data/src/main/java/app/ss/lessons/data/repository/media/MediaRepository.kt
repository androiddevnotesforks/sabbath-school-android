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

package app.ss.lessons.data.repository.media

import android.net.Uri
import app.ss.models.media.AudioFile
import app.ss.models.media.SSAudio
import app.ss.models.media.SSVideosInfo
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ss.foundation.coroutines.DispatcherProvider
import ss.foundation.coroutines.Scopable
import ss.foundation.coroutines.defaultScopable
import ss.lessons.api.SSMediaApi
import ss.lessons.model.VideosInfoModel
import ss.lessons.model.request.SSMediaRequest
import ss.libraries.storage.api.dao.AudioDao
import ss.libraries.storage.api.dao.VideoInfoDao
import ss.libraries.storage.api.entity.AudioFileEntity
import ss.libraries.storage.api.entity.VideoInfoEntity
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

interface MediaRepository {
    fun getAudio(lessonIndex: String): Flow<List<SSAudio>>
    suspend fun findAudioFile(id: String): AudioFile?
    suspend fun updateDuration(id: String, duration: Long)
    suspend fun getPlayList(lessonIndex: String): List<AudioFile>
    fun getVideo(lessonIndex: String): Flow<List<SSVideosInfo>>
}

@Singleton
internal class MediaRepositoryImpl @Inject constructor(
    private val audioDao: AudioDao,
    private val videoInfoDao: VideoInfoDao,
    private val mediaApi: SSMediaApi,
    private val dispatcherProvider: DispatcherProvider
) : MediaRepository, Scopable by defaultScopable(dispatcherProvider) {

    private val exceptionLogger = CoroutineExceptionHandler { _, exception -> Timber.e(exception) }

    override fun getAudio(lessonIndex: String): Flow<List<SSAudio>> = audioDao
        .getAsFlow("$lessonIndex%")
        .map { entities -> entities.map { it.toSSAudio() } }
        .onStart { syncAudio(lessonIndex) }
        .flowOn(dispatcherProvider.io)

    private fun syncAudio(lessonIndex: String) = scope.launch(exceptionLogger) {
        val data = lessonIndex.toMediaRequest()?.let {
            mediaApi.getAudio(it.language, it.quarterlyId).body()
        } ?: return@launch
        val lessonAudios = data.filter { it.targetIndex.startsWith(lessonIndex) }.map { it.toEntity() }

        withContext(dispatcherProvider.io) {
            audioDao.delete(lessonIndex)
            audioDao.insertAll(lessonAudios)
        }
    }

    override suspend fun findAudioFile(id: String): AudioFile? = withContext(dispatcherProvider.io) {
        audioDao.findBy(id)?.toAudio()
    }

    override suspend fun updateDuration(id: String, duration: Long) = withContext(dispatcherProvider.io) {
        audioDao.update(duration, id)
    }

    override suspend fun getPlayList(lessonIndex: String): List<AudioFile> = withContext(dispatcherProvider.io) {
        audioDao.getBy("$lessonIndex%").map {
            it.toAudio()
        }
    }

    override fun getVideo(lessonIndex: String): Flow<List<SSVideosInfo>> = videoInfoDao
        .getAsFlow(lessonIndex)
        .map { entities -> entities.map { it.toModel() } }
        .onStart { syncVideo(lessonIndex) }
        .flowOn(dispatcherProvider.io)

    private fun syncVideo(lessonIndex: String) = scope.launch (exceptionLogger) {
        var apiLessonIndex = ""
        val data = lessonIndex.toMediaRequest()?.let { ssMediaRequest ->
            apiLessonIndex = "${ssMediaRequest.language}-${ssMediaRequest.quarterlyId}"
            val videos = mediaApi.getVideo(ssMediaRequest.language, ssMediaRequest.quarterlyId).body()
            videos?.mapIndexed { index, model ->
                model.toModel("$apiLessonIndex-$index", lessonIndex)
            }
        } ?: return@launch

        withContext(dispatcherProvider.io) {
            videoInfoDao.delete(apiLessonIndex)
            videoInfoDao.insertAll(data.map { it.toEntity() })
        }
    }
}

internal fun String.toMediaRequest(): SSMediaRequest? {
    if (this.isEmpty()) {
        return null
    }
    val langId = this.substringBeforeLast('-')
    val lang = langId.substringBefore('-')
    val id = langId.substringAfter('-')
    return SSMediaRequest(lang, id)
}

fun SSAudio.toEntity(): AudioFileEntity = AudioFileEntity(
    id = id,
    artist = artist,
    image = image,
    imageRatio = imageRatio,
    src = src,
    target = target,
    targetIndex = targetIndex,
    title = title
)

fun AudioFileEntity.toAudio(): AudioFile = AudioFile(
    id = id,
    artist = artist,
    image = image,
    imageRatio = imageRatio,
    source = Uri.parse(src),
    target = target,
    targetIndex = targetIndex,
    title = title
)

fun AudioFileEntity.toSSAudio(): SSAudio = SSAudio(
    id = id,
    artist = artist,
    image = image,
    imageRatio = imageRatio,
    src = src,
    target = target,
    targetIndex = targetIndex,
    title = title
)

private fun VideoInfoEntity.toModel(): SSVideosInfo = SSVideosInfo(
    id = id,
    artist = artist,
    clips = clips,
    lessonIndex = lessonIndex
)

private fun VideosInfoModel.toModel(
    id: String,
    lessonIndex: String
): SSVideosInfo = SSVideosInfo(
    id = id,
    artist = artist,
    clips = clips,
    lessonIndex = lessonIndex
)

private fun SSVideosInfo.toEntity(): VideoInfoEntity = VideoInfoEntity(
    id = id,
    artist = artist,
    clips = clips,
    lessonIndex = lessonIndex
)
