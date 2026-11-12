package com.playfieldportal.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily

val LocalPfpFontFamily = staticCompositionLocalOf<FontFamily?> { null }

/** Explicit shadow/size styles must still inherit the user's typeface. */
@Composable
fun TextStyle.withPfpFont(): TextStyle = withFontFallback(LocalPfpFontFamily.current)

internal fun TextStyle.withFontFallback(family: FontFamily?): TextStyle =
    if (family != null && (fontFamily == null || fontFamily == FontFamily.Default || fontFamily == FontFamily.SansSerif)) {
        copy(fontFamily = family)
    } else this

internal fun Typography.withPfpFont(family: FontFamily?): Typography {
    if (family == null) return this
    return copy(
        displayLarge = displayLarge.copy(fontFamily = family),
        displayMedium = displayMedium.copy(fontFamily = family),
        displaySmall = displaySmall.copy(fontFamily = family),
        headlineLarge = headlineLarge.copy(fontFamily = family),
        headlineMedium = headlineMedium.copy(fontFamily = family),
        headlineSmall = headlineSmall.copy(fontFamily = family),
        titleLarge = titleLarge.copy(fontFamily = family),
        titleMedium = titleMedium.copy(fontFamily = family),
        titleSmall = titleSmall.copy(fontFamily = family),
        bodyLarge = bodyLarge.copy(fontFamily = family),
        bodyMedium = bodyMedium.copy(fontFamily = family),
        bodySmall = bodySmall.copy(fontFamily = family),
        labelLarge = labelLarge.copy(fontFamily = family),
        labelMedium = labelMedium.copy(fontFamily = family),
        labelSmall = labelSmall.copy(fontFamily = family),
    )
}
