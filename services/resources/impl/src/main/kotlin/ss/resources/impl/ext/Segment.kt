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

package ss.resources.impl.ext

import io.adventech.blockkit.model.resource.Segment
import ss.libraries.storage.api.entity.SegmentEntity

internal fun Segment.toEntity(): SegmentEntity {
    return SegmentEntity(
        id = this.id,
        index = this.index,
        name = this.name,
        title = this.title,
        type = this.type,
        resourceId = this.resourceId,
        markdownTitle = this.markdownTitle,
        subtitle = this.subtitle,
        markdownSubtitle = this.markdownSubtitle,
        titleBelowCover = this.titleBelowCover,
        cover = this.cover,
        blocks = this.blocks,
        date = this.date,
        background = this.background,
        pdf = this.pdf,
        video = this.video,
        style = this.style,
    )
}

internal fun SegmentEntity.toModel(): Segment {
    return Segment(
        id = this.id,
        index = this.index,
        name = this.name,
        title = this.title,
        type = this.type,
        resourceId = this.resourceId,
        markdownTitle = this.markdownTitle,
        subtitle = this.subtitle,
        markdownSubtitle = this.markdownSubtitle,
        titleBelowCover = this.titleBelowCover,
        cover = this.cover,
        blocks = this.blocks,
        date = this.date,
        background = this.background,
        pdf = this.pdf,
        video = this.video,
        style = style,
    )
}
