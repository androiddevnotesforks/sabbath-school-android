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

package io.adventech.blockkit.ui.style

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import io.adventech.blockkit.ui.R

internal val LatoFontFamily = FontFamily(
    Font(R.font.lato_regular, FontWeight.Normal),
    Font(R.font.lato_medium, FontWeight.Medium),
    Font(R.font.lato_bold, FontWeight.Bold),
    Font(R.font.lato_bold, FontWeight.Bold, FontStyle.Italic),
    Font(R.font.lato_black, FontWeight.Black),
    Font(R.font.lato_black, FontWeight.Black, FontStyle.Italic),
    Font(R.font.lato_light, FontWeight.Light),
    Font(R.font.lato_thin, FontWeight.Thin),
    Font(R.font.lato_thin, FontWeight.Thin, FontStyle.Italic),
    Font(R.font.lato_italic, FontWeight.Normal, FontStyle.Italic),
)

private val GoogleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

internal data object FontProvider {
    fun googleFontFamily(name: String): FontFamily {
        val googleFontName = GoogleFont(name)
        return FontFamily(
            Font(
                googleFont = googleFontName,
                fontProvider = GoogleFontProvider,
                weight = FontWeight.Normal,
                style = FontStyle.Normal,
            ),
            Font(
                googleFont = googleFontName,
                fontProvider = GoogleFontProvider,
                weight = FontWeight.Bold,
                style = FontStyle.Normal,
            ),
            Font(
                googleFont = googleFontName,
                fontProvider = GoogleFontProvider,
                weight = FontWeight.Bold,
                style = FontStyle.Italic,
            ),
            Font(
                googleFont = googleFontName,
                fontProvider = GoogleFontProvider,
                weight = FontWeight.Normal,
                style = FontStyle.Italic,
            )
        )
    }
}