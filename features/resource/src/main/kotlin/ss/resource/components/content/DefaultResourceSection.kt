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

package ss.resource.components.content

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ss.design.compose.extensions.modifier.asPlaceholder
import app.ss.design.compose.theme.SsTheme
import app.ss.design.compose.widget.content.ContentBox
import app.ss.design.compose.widget.icon.IconBox
import app.ss.design.compose.widget.icon.Icons
import app.ss.design.compose.widget.image.RemoteImage

@Immutable
data class DefaultResourceSection(
    override val id: String,
    val leadingContent: String?,
    val overlineContent: String?,
    val headLineContent: String,
    val supportingContent: String?,
    val isArticle: Boolean,
    val blogCover: String?
) : ResourceSectionSpec {

    @Composable
    override fun Content(modifier: Modifier) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .sizeIn(minHeight = 44.dp)
                .padding(horizontal = 6.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable {}
                .padding(horizontal = 12.dp)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingContent?.let {
                Text(
                    text = it,
                    modifier = Modifier.padding(end = 12.dp),
                    style = SsTheme.typography.titleSmall.copy(fontSize = 22.sp),
                    color = SsTheme.colors.primaryForeground,
                    maxLines = 1,
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                overlineContent?.let {
                    Text(
                        text = it,
                        modifier = Modifier,
                        style = SsTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                        color = SsTheme.colors.secondaryForeground,
                        maxLines = 1,
                    )
                }

                Text(
                    text = headLineContent,
                    modifier = Modifier,
                    style = SsTheme.typography.titleMedium.copy(fontSize = 18.sp),
                    color = SsTheme.colors.primaryForeground,
                    maxLines = 2,
                )

                supportingContent?.let {
                    Text(
                        text = it,
                        modifier = Modifier,
                        style = SsTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                        color = SsTheme.colors.secondaryForeground,
                        maxLines = 1,
                    )
                }
            }

            if (isArticle) {
                IconBox(Icons.OpenInBrowser)
            }

            blogCover?.let {
                ContentBox(
                    content = RemoteImage(
                        data = it,
                        loading = {
                            Spacer(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .asPlaceholder(true)
                            )
                        },
                        error = {
                            Spacer(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.secondaryContainer, CoverImageShape)
                            )
                        }
                    ),
                    modifier = Modifier
                        .width(80.dp)
                        .aspectRatio(16f / 9f)
                        .background(MaterialTheme.colorScheme.secondaryContainer, CoverImageShape)
                        .shadow(4.dp, CoverImageShape)
                )
            }
        }

    }
}

private val CoverImageShape = RoundedCornerShape(6.dp)

@PreviewLightDark
@Composable
private fun Preview() {
    SsTheme {
        Surface {
            DefaultResourceSection(
                id = "123",
                leadingContent = "1",
                overlineContent = "This is the overline",
                headLineContent = "This is the Headline",
                supportingContent = "Dec 22 - Dec 28",
                isArticle = true,
                blogCover = "https://via"
            ).Content()
        }
    }
}
