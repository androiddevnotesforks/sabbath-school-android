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

package ss.document

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import app.ss.design.compose.extensions.scroll.ScrollAlpha
import app.ss.design.compose.extensions.scroll.rememberScrollAlpha
import app.ss.design.compose.theme.SsTheme
import app.ss.design.compose.widget.scaffold.HazeScaffold
import com.slack.circuit.codegen.annotations.CircuitInject
import dagger.hilt.components.SingletonComponent
import ss.document.components.DocumentLoadingView
import ss.document.components.DocumentPager
import ss.document.components.DocumentTopAppBar
import ss.document.components.segment.hasCover
import ss.libraries.circuit.navigation.DocumentScreen

@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(DocumentScreen::class, SingletonComponent::class)
@Composable
fun DocumentScreenUi(state: State, modifier: Modifier = Modifier) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val listState = rememberLazyListState()
    val scrollAlpha: ScrollAlpha = rememberScrollAlpha(listState = listState)
    val collapsed by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    val toolbarTitle by remember(state) { derivedStateOf { if (collapsed) state.title else "" } }

    HazeScaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SsTheme.colors.primaryBackground.copy(
                    alpha = if (collapsed) 1f else scrollAlpha.alpha
                ),
                tonalElevation = if (collapsed) 4.dp else 0.dp
            ) {
                DocumentTopAppBar(
                    title = toolbarTitle,
                    scrollBehavior = scrollBehavior,
                    collapsible = state.hasCover,
                    collapsed = collapsed,
                    onNavBack = { state.eventSink(Event.OnNavBack) }
                )
            }
        },
    ) {
        when (state) {
            is State.Loading -> {
                DocumentLoadingView()
            }

            is State.Success -> {
                DocumentPager(
                    segments = state.segments,
                    selectedSegment = state.selectedSegment,
                    titleBelowCover = state.titleBelowCover,
                    listState = listState
                ) {
                    state.eventSink(SuccessEvent.OnPageChange(it))
                }
            }
        }
    }
}