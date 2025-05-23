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

package app.ss.design.compose.extensions.content

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource

/**
 * A helper spec for a string literal or string resource.
 * Will typically be used in a [androidx.compose.material3.Text].
 * **/
interface ContentSpec {

    @Immutable
    data class Str(val content: String) : ContentSpec

    @Immutable
    data class Res(@param:StringRes val content: Int) : ContentSpec
}

/**
 * Usage:
 *
 * ```
 * @Composable
 * fun TextComposable(spec: ContentSpec) {
 *  Text(text = spec.asText())
 * }
 * ```
 */
@Composable
fun ContentSpec.asText(): String {
    return when (this) {
        is ContentSpec.Str -> content
        is ContentSpec.Res -> stringResource(id = content)
        else -> ""
    }
}
