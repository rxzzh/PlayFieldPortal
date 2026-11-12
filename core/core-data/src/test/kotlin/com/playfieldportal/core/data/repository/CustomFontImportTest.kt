package com.playfieldportal.core.data.repository

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CustomFontImportTest {
    @Test
    fun `rejects non-font and truncated input before writing output`() {
        for (bytes in listOf(byteArrayOf(), byteArrayOf(0, 1), "not a font".toByteArray())) {
            val output = ByteArrayOutputStream()
            assertThrows(IOException::class.java) {
                CustomFontStore.copyBoundedFont(ByteArrayInputStream(bytes), output)
            }
            assertEquals(0, output.size())
        }
    }

    @Test
    fun `keeps bytes unchanged for OTF and TTF streams with short reads`() {
        for (header in listOf("OTTO".toByteArray(), byteArrayOf(0, 1, 0, 0))) {
            val bytes = header + ByteArray(257) { it.toByte() }
            val input = object : ByteArrayInputStream(bytes) {
                override fun read(b: ByteArray, off: Int, len: Int): Int = super.read(b, off, minOf(len, 1))
            }
            val output = ByteArrayOutputStream()
            CustomFontStore.copyBoundedFont(input, output)
            assertArrayEquals(bytes, output.toByteArray())
        }
    }

    @Test
    fun `rejects oversized input without storing the oversized bytes`() {
        val bytes = "OTTO".toByteArray() + ByteArray(CustomFontStore.MAX_BYTES)
        val output = ByteArrayOutputStream()
        assertThrows(IOException::class.java) {
            CustomFontStore.copyBoundedFont(ByteArrayInputStream(bytes), output)
        }
        org.junit.Assert.assertTrue(output.size() <= CustomFontStore.MAX_BYTES)
    }
}
