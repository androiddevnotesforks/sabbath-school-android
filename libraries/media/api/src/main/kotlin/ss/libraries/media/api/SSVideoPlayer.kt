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

package ss.libraries.media.api

import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.flow.StateFlow
import ss.libraries.media.model.PlaybackProgressState
import ss.libraries.media.model.PlaybackSpeed
import ss.libraries.media.model.VideoPlaybackState

@Stable
interface SSVideoPlayer {
    val playbackState: StateFlow<VideoPlaybackState>
    val playbackProgress: StateFlow<PlaybackProgressState>
    val playbackSpeed: StateFlow<PlaybackSpeed>
    fun playVideo(source: Uri, playerView: PlayerView)
    fun playPause()
    fun seekTo(position: Long)
    fun fastForward()
    fun rewind()
    fun toggleSpeed()
    fun onPause()
    fun onResume()
    fun release()
}