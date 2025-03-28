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

package app.ss.media.playback.ui.video

import android.content.Context
import app.ss.models.media.SSVideo
import app.ss.models.media.SSVideosInfo
import com.slack.circuit.runtime.CircuitUiState

data class VideosScreenState(
    val data: VideoListData = VideoListData.Empty,
    val eventSink: (VideosScreenEvent) -> Unit = {}
): CircuitUiState

sealed interface VideosScreenEvent {
    data class OnVideoSelected(val context: Context, val video: SSVideo) : VideosScreenEvent
}

sealed interface VideoListData : CircuitUiState {
    val showDragHandle: Boolean

    data class Horizontal(
        val data: List<SSVideosInfo>,
        val target: String?,
        override val showDragHandle: Boolean = false,
    ) : VideoListData

    data class Vertical(
        val featured: SSVideo,
        val clips: List<SSVideo>,
        override val showDragHandle: Boolean = false,
    ) : VideoListData

    data object Empty : VideoListData {
        override val showDragHandle: Boolean = false
    }
}
