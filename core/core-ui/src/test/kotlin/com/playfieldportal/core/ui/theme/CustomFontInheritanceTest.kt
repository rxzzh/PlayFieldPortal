package com.playfieldportal.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

class CustomFontInheritanceTest {
    @Test
    fun `shadow styles inherit chosen font without losing appearance`() {
        val shadow = Shadow(Color.Black)
        val style = TextStyle(shadow = shadow, fontSize = 17.sp)
        val inherited = style.withFontFallback(FontFamily.Cursive)
        assertEquals(FontFamily.Cursive, inherited.fontFamily)
        assertEquals(shadow, inherited.shadow)
        assertEquals(17.sp, inherited.fontSize)
    }

    @Test
    fun `explicit monospace is retained while system defaults follow the pick`() {
        assertEquals(FontFamily.Monospace,
            TextStyle(fontFamily = FontFamily.Monospace).withFontFallback(FontFamily.Cursive).fontFamily)
        assertEquals(FontFamily.Cursive,
            TextStyle(fontFamily = FontFamily.Default).withFontFallback(FontFamily.Cursive).fontFamily)
        assertEquals(TextStyle.Default, TextStyle.Default.withFontFallback(null))
    }
}
