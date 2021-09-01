package app.ss.media.playback.players

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.os.bundleOf
import app.ss.media.playback.AudioFocusHelper
import app.ss.media.playback.AudioQueueManager
import app.ss.media.playback.BY_UI_KEY
import app.ss.media.playback.REPEAT_ALL
import app.ss.media.playback.REPEAT_ONE
import app.ss.media.playback.model.toMediaId
import app.ss.media.R
import app.ss.media.playback.model.AudioFile
import app.ss.media.playback.model.toMediaMetadata
import app.ss.media.playback.extensions.createDefaultPlaybackState
import app.ss.media.playback.extensions.isPlaying
import app.ss.media.playback.extensions.position
import app.ss.media.playback.extensions.repeatMode
import app.ss.media.playback.extensions.shuffleMode
import app.ss.media.playback.repository.AudioRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import timber.log.Timber

typealias OnMetaDataChanged = SSAudioPlayer.() -> Unit

const val REPEAT_MODE = "repeat_mode"
const val SHUFFLE_MODE = "shuffle_mode"
const val QUEUE_CURRENT_INDEX = "queue_current_index"
const val QUEUE_HAS_PREVIOUS = "queue_has_previous"
const val QUEUE_HAS_NEXT = "queue_has_next"

const val DEFAULT_FORWARD_REWIND = 10 * 1000

interface SSAudioPlayer {
    fun getSession(): MediaSessionCompat
    fun playAudio(extras: Bundle = bundleOf(BY_UI_KEY to true))
    suspend fun playAudio(id: String,)
    suspend fun playAudio(audio: AudioFile)
    fun seekTo(position: Long)
    fun fastForward()
    fun rewind()
    fun pause(extras: Bundle = bundleOf(BY_UI_KEY to true))
    fun stop(byUser: Boolean = true)
    fun release()
    fun onPlayingState(playing: OnIsPlaying<SSAudioPlayer>)
    fun onPrepared(prepared: OnPrepared<SSAudioPlayer>)
    fun onError(error: OnError<SSAudioPlayer>)
    fun onCompletion(completion: OnCompletion<SSAudioPlayer>)
    fun onMetaDataChanged(metaDataChanged: OnMetaDataChanged)
    fun updatePlaybackState(applier: PlaybackStateCompat.Builder.() -> Unit = {})
    fun setPlaybackState(state: PlaybackStateCompat)
    suspend fun setDataFromMediaId(_mediaId: String, extras: Bundle = bundleOf())
}

internal class SSAudioPlayerImpl(
    private val context: Context,
    private val audioPlayer: AudioPlayer,
    private val audioFocusHelper: AudioFocusHelper,
    private val queueManager: AudioQueueManager,
    private val repository: AudioRepository,
) : SSAudioPlayer, CoroutineScope by MainScope() {

    private var isInitialized: Boolean = false

    private var isPlayingCallback: OnIsPlaying<SSAudioPlayer> = { _, _ -> }
    private var preparedCallback: OnPrepared<SSAudioPlayer> = {}
    private var errorCallback: OnError<SSAudioPlayer> = {}
    private var completionCallback: OnCompletion<SSAudioPlayer> = {}
    private var metaDataChangedCallback: OnMetaDataChanged = {}

    private val metadataBuilder = MediaMetadataCompat.Builder()
    private val stateBuilder = createDefaultPlaybackState()

    private val pendingIntent = PendingIntent.getBroadcast(context, 0, Intent(Intent.ACTION_MEDIA_BUTTON), PendingIntent.FLAG_IMMUTABLE)

    private val mediaSession = MediaSessionCompat(context, context.getString(R.string.ss_app_name), null, pendingIntent).apply {
        setCallback(
            MediaSessionCallback(this, this@SSAudioPlayerImpl, audioFocusHelper)
        )
        setPlaybackState(stateBuilder.build())

        val sessionIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val sessionActivityPendingIntent = PendingIntent.getActivity(context, 0, sessionIntent, PendingIntent.FLAG_IMMUTABLE)
        setSessionActivity(sessionActivityPendingIntent)
        isActive = true
    }

    init {
        audioPlayer.onPrepared {
            preparedCallback(this@SSAudioPlayerImpl)
            launch {
                if (!mediaSession.isPlaying()) audioPlayer.seekTo(mediaSession.position())
                playAudio()
            }
        }

        audioPlayer.onCompletion {
            completionCallback(this@SSAudioPlayerImpl)
            val controller = getSession().controller
            when (controller.repeatMode) {
                PlaybackStateCompat.REPEAT_MODE_ONE -> controller.transportControls.sendCustomAction(REPEAT_ONE, null)
                PlaybackStateCompat.REPEAT_MODE_ALL -> controller.transportControls.sendCustomAction(REPEAT_ALL, null)
                else -> launch { goToStart() }
            }
        }
        audioPlayer.onBuffering {
            updatePlaybackState {
                setState(PlaybackStateCompat.STATE_BUFFERING, mediaSession.position(), 1F)
            }
        }
        audioPlayer.onIsPlaying { playing, byUi ->
            if (playing)
                updatePlaybackState {
                    setState(PlaybackStateCompat.STATE_PLAYING, mediaSession.position(), 1F)
                    setExtras(
                        bundleOf(
                            REPEAT_MODE to getSession().repeatMode,
                            SHUFFLE_MODE to getSession().shuffleMode
                        )
                    )
                }
            isPlayingCallback(playing, byUi)
        }
        audioPlayer.onReady {
            if (!audioPlayer.isPlaying()) {
                Timber.d("Player ready but not currently playing, requesting to play")
                audioPlayer.play()
            }
            updatePlaybackState {
                setState(PlaybackStateCompat.STATE_PLAYING, mediaSession.position(), 1F)
            }
        }
        audioPlayer.onError { throwable ->
            Timber.e(throwable, "AudioPlayer error")
            errorCallback(this@SSAudioPlayerImpl, throwable)
            isInitialized = false
            updatePlaybackState {
                setState(PlaybackStateCompat.STATE_ERROR, 0, 1F)
            }
        }
    }

    override fun getSession(): MediaSessionCompat = mediaSession

    override fun playAudio(extras: Bundle) {
        if (isInitialized) {
            audioPlayer.play()
            return
        }

        launch {
            repository.findAudioFile(queueManager.currentAudioId)?.let { audio ->
                audioPlayer.setSource(audio.source, false)

                isInitialized = true
                audioPlayer.prepare()
            } ?: run {
                Timber.e("Couldn't set new source")
            }
        }
    }

    override suspend fun playAudio(id: String) {
        if (audioFocusHelper.requestPlayback()) {
            val audio = repository.findAudioFile(id) ?: run {
                Timber.e("Audio by id: $id not found")
                updatePlaybackState {
                    setState(PlaybackStateCompat.STATE_ERROR, 0, 1F)
                }
                return
            }
            playAudio(audio)
        }
    }

    override suspend fun playAudio(audio: AudioFile) {
        queueManager.setCurrentAudioId(audio.id)
        isInitialized = false

        updatePlaybackState {
            setState(mediaSession.controller.playbackState.state, 0, 1F)
        }
        setMetaData(audio)
        playAudio()
    }

    override fun seekTo(position: Long) {
        if (isInitialized) {
            audioPlayer.seekTo(position)
            updatePlaybackState {
                setState(
                    mediaSession.controller.playbackState.state,
                    position,
                    1F
                )
            }
        } else updatePlaybackState {
            setState(
                mediaSession.controller.playbackState.state,
                position,
                1F
            )
        }
    }

    override fun fastForward() {
        val forwardTo = mediaSession.position() + DEFAULT_FORWARD_REWIND
        queueManager.currentAudio?.apply {
            /*val duration = durationMillis()
            if (forwardTo > duration) {
                seekTo(duration)
            } else {
                seekTo(forwardTo)
            }*/
        }
    }

    override fun rewind() {
        val rewindTo = mediaSession.position() - DEFAULT_FORWARD_REWIND
        if (rewindTo < 0) {
            seekTo(0)
        } else {
            seekTo(rewindTo)
        }
    }

    override fun pause(extras: Bundle) {
        if (isInitialized && (audioPlayer.isPlaying() || audioPlayer.isBuffering())) {
            audioPlayer.pause()
            updatePlaybackState {
                setState(PlaybackStateCompat.STATE_PAUSED, mediaSession.position(), 1F)
                setExtras(
                    extras + bundleOf(
                        REPEAT_MODE to getSession().repeatMode,
                        SHUFFLE_MODE to getSession().shuffleMode
                    )
                )
            }
        } else {
            Timber.d("Couldn't pause player: ${audioPlayer.isPlaying()}, $isInitialized")
        }
    }

    override fun stop(byUser: Boolean) {
        updatePlaybackState {
            setState(if (byUser) PlaybackStateCompat.STATE_NONE else PlaybackStateCompat.STATE_STOPPED, 0, 1F)
        }
        isInitialized = false
        audioPlayer.stop()
        isPlayingCallback(false, byUser)
    }

    override fun release() {
        mediaSession.apply {
            isActive = false
            release()
        }
        audioPlayer.release()
    }

    override fun onPlayingState(playing: OnIsPlaying<SSAudioPlayer>) {
        this.isPlayingCallback = playing
    }

    override fun onPrepared(prepared: OnPrepared<SSAudioPlayer>) {
        this.preparedCallback = prepared
    }

    override fun onError(error: OnError<SSAudioPlayer>) {
        this.errorCallback = error
    }

    override fun onCompletion(completion: OnCompletion<SSAudioPlayer>) {
        this.completionCallback = completion
    }

    override fun onMetaDataChanged(metaDataChanged: OnMetaDataChanged) {
        this.metaDataChangedCallback = metaDataChanged
    }

    override fun updatePlaybackState(applier: PlaybackStateCompat.Builder.() -> Unit) {
        applier(stateBuilder)
        stateBuilder.setExtras(
            stateBuilder.build().extras + bundleOf(
                QUEUE_CURRENT_INDEX to queueManager.currentAudioIndex,
                QUEUE_HAS_PREVIOUS to (queueManager.previousAudioIndex != null),
                QUEUE_HAS_NEXT to (queueManager.nextAudioIndex != null),
            )
        )
        setPlaybackState(stateBuilder.build())
    }

    override fun setPlaybackState(state: PlaybackStateCompat) {
        mediaSession.setPlaybackState(state)
        state.extras?.let { bundle ->
            mediaSession.setRepeatMode(bundle.getInt(REPEAT_MODE))
            mediaSession.setShuffleMode(bundle.getInt(SHUFFLE_MODE))
        }
    }

    override suspend fun setDataFromMediaId(_mediaId: String, extras: Bundle) {
        val mediaId = _mediaId.toMediaId()
        val audioId = extras.getString(QUEUE_MEDIA_ID_KEY) ?: mediaId.value
        val audio = repository.findAudioFile(audioId) ?: run {
            Timber.e("Couldn't find mediaId: $audioId")
            return
        }
        playAudio(audio)
    }

    private fun goToStart() {
        isInitialized = false

        stop()
    }

    private fun setMetaData(audio: AudioFile) {
        val player = this
        launch {
            val mediaMetadata = audio.toMediaMetadata(metadataBuilder).apply {
                /*val drawable = context.bookCoverFromKey(audio.book)
                val artwork = drawable?.toBitmap(COVER_IMAGE_SIZE, COVER_IMAGE_SIZE)
                if (artwork != null) {
                    putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, artwork)
                }*/
            }

            mediaSession.setMetadata(mediaMetadata.build())
            metaDataChangedCallback(player)
        }
    }
}

const val COVER_IMAGE_SIZE = 300 // px

operator fun Bundle?.plus(other: Bundle?) = this.apply { (this ?: Bundle()).putAll(other ?: Bundle()) }
