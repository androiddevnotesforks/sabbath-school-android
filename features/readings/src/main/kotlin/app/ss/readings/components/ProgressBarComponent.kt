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

package app.ss.readings.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import app.ss.design.compose.extensions.modifier.asPlaceholder
import app.ss.design.compose.theme.SsTheme
import app.ss.design.compose.widget.placeholder.PlaceholderDefaults
import app.ss.readings.SSReadingViewModel
import app.ss.readings.databinding.SsReadingProgressBarBinding
import app.ss.readings.model.ReadingsState
import ss.foundation.coroutines.flow.collectIn
import app.ss.readings.R as LessonsR

class ProgressBarComponent(
    private val binding: SsReadingProgressBarBinding,
    viewModel: SSReadingViewModel,
    owner: LifecycleOwner
) {
    init {
        viewModel.viewState.collectIn(owner) { state ->
            binding.root.isVisible = state == ReadingsState.Loading
        }

        binding.composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { SsTheme { Surface { ContentLoading() }} }
        }
    }
}

@Composable
private fun ContentLoading(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize(),
        contentPadding = WindowInsets.systemBars.asPaddingValues()
    ) {
        item {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(id = LessonsR.dimen.ss_reading_toolbar_height))
                    .asPlaceholder(true, shape = RectangleShape)
            )
        }

        item {
            Spacer(
                modifier = Modifier
                    .height(16.dp)
                    .fillMaxWidth()
            )
        }

        items(15) {position ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Spacer(
                    modifier = Modifier
                        .height(14.dp)
                        .weight(1f)
                        .asPlaceholder(true, shape = PlaceholderDefaults.shape)
                )

                val size by remember(position) { mutableIntStateOf((0..200).random()) }

                Spacer(modifier = Modifier.width(size.dp))
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun Preview() {
    SsTheme { ContentLoading() }
}
