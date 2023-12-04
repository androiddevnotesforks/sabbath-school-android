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

package ss.services.media.impl

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import ss.foundation.coroutines.flow.flowInterval
import ss.libraries.media.api.DEFAULT_FORWARD
import ss.libraries.media.api.DEFAULT_REWIND
import ss.libraries.media.api.PLAYBACK_PROGRESS_INTERVAL
import ss.libraries.media.api.SSVideoPlayer
import ss.libraries.media.model.PlaybackProgressState
import ss.libraries.media.model.PlaybackSpeed
import ss.libraries.media.model.VideoPlaybackState
import ss.libraries.media.model.hasEnded
import ss.libraries.media.model.isBuffering
import timber.log.Timber
import javax.inject.Inject

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@ActivityScoped
internal class SSVideoPlayerImpl @Inject constructor(
    @ActivityContext private val context: Context,
) : SSVideoPlayer, Player.Listener, CoroutineScope by ProcessLifecycleOwner.get().lifecycleScope {

    private val exoPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(context)
            .setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus= */ true)
            .setSeekBackIncrementMs(DEFAULT_REWIND)
            .setSeekForwardIncrementMs(DEFAULT_FORWARD)
            .build().also { player ->
                player.playWhenReady = false
                player.addListener(this)
            }
    }

    override val playbackState = MutableStateFlow(VideoPlaybackState())
    override val playbackProgress = MutableStateFlow(PlaybackProgressState())
    override val playbackSpeed = MutableStateFlow(PlaybackSpeed.NORMAL)

    private var playbackProgressInterval: Job = Job()

    private var currentProgressInterval: Long = PLAYBACK_PROGRESS_INTERVAL

    init {
        startPlaybackProgress()
    }

    override fun playVideo(source: Uri, playerView: PlayerView) {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        }

        val mediaSource = DefaultMediaSourceFactory(context)
            .createMediaSource(MediaItem.fromUri(source))
        playerView.player = exoPlayer
        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
    }

    override fun playPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if (playbackState.value.hasEnded) {
                exoPlayer.seekTo(0)
            }
            playOnFocus()
        }
    }

    override fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
    }

    override fun fastForward() {
        val forwardTo = exoPlayer.currentPosition + DEFAULT_FORWARD
        if (forwardTo > exoPlayer.duration) {
            seekTo(exoPlayer.duration)
        } else {
            seekTo(forwardTo)
        }
    }

    override fun rewind() {
        val rewindTo = exoPlayer.currentPosition - DEFAULT_REWIND
        if (rewindTo < 0) {
            seekTo(0)
        } else {
            seekTo(rewindTo)
        }
    }

    override fun toggleSpeed() {
        val nextSpeed = when (this.playbackSpeed.value) {
            PlaybackSpeed.SLOW -> PlaybackSpeed.NORMAL
            PlaybackSpeed.NORMAL -> PlaybackSpeed.FAST
            PlaybackSpeed.FAST -> PlaybackSpeed.FASTEST
            PlaybackSpeed.FASTEST -> PlaybackSpeed.SLOW
        }

        if (playbackSpeed.tryEmit(nextSpeed)) {
            exoPlayer.setPlaybackSpeed(nextSpeed.speed)
            resetPlaybackProgressInterval()
        }
    }

    override fun onPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        }
    }

    override fun onResume() {
        if (exoPlayer.isPlaying.not() && playbackState.value.state == Player.STATE_READY) {
            playOnFocus()
        }
    }

    private fun playOnFocus() {
        exoPlayer.play()
    }

    override fun release() {
        exoPlayer.release()
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        Timber.e(error)
    }

    override fun onPlaybackStateChanged(state: Int) {
        super.onPlaybackStateChanged(state)
        playbackState.tryEmit(
            playbackState.value.copy(state = state)
        )

        if (state == Player.STATE_READY) {
            playOnFocus()
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        playbackState.tryEmit(
            playbackState.value.copy(isPlaying = isPlaying)
        )
    }

    private fun startPlaybackProgress() = launch {
        playbackState.collect { state ->
            playbackProgressInterval.cancel()

            val duration = exoPlayer.duration
            val position = exoPlayer.currentPosition

            if (state.state == Player.STATE_IDLE || duration < 1) {
                return@collect
            }

            val initial = PlaybackProgressState(duration, position, buffered = exoPlayer.bufferedPosition)
            playbackProgress.emit(initial)

            if (state.isPlaying && !state.isBuffering) {
                startPlaybackProgressInterval(initial)
            }
        }
    }

    private fun startPlaybackProgressInterval(initial: PlaybackProgressState) {
        playbackProgressInterval = launch {
            flowInterval(currentProgressInterval).collect {
                val current = playbackProgress.value.elapsed
                val elapsed = current + PLAYBACK_PROGRESS_INTERVAL
                playbackProgress.emit(initial.copy(elapsed = elapsed, buffered = exoPlayer.bufferedPosition))
            }
        }
    }

    private fun resetPlaybackProgressInterval() {
        val speed = playbackSpeed.value.speed
        currentProgressInterval = (PLAYBACK_PROGRESS_INTERVAL.toDouble() / speed).toLong()

        playbackProgressInterval.cancel()
        startPlaybackProgressInterval(playbackProgress.value)
    }
}