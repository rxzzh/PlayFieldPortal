package com.playfieldportal.core.data.repository

import android.content.Context
import android.graphics.Typeface
import android.graphics.fonts.Font
import android.graphics.fonts.FontFamily
import android.net.Uri
import android.provider.OpenableColumns
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.playfieldportal.core.data.datastore.pfpDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Device-local fonts, imported once through SAF; no font binaries travel with themes or APKs. */
@Singleton
class CustomFontStore @Inject constructor(@ApplicationContext private val context: Context) {
    data class Selection(val fileName: String, val displayName: String)
    data class Loaded(val selection: Selection, val typeface: Typeface)

    private val directory = File(context.filesDir, "custom_fonts")
    private val mutex = Mutex()

    val loadedFont = context.pfpDataStore.data
        .map(::selection)
        .distinctUntilChanged()
        .map { selected ->
            selected?.let {
                runCatching { Loaded(it, loadTypeface(File(directory, it.fileName))) }.getOrNull()
            }
        }
        .catch { emit(null) }
        .flowOn(Dispatchers.IO)

    fun selection(prefs: Preferences): Selection? {
        val fileName = prefs[KEY_FILE] ?: return null
        // Never interpret persisted preferences as an arbitrary filesystem path.
        if (!fileName.matches(Regex("font_[a-f0-9-]+\\.font"))) return null
        if (!File(directory, fileName).isFile) return null
        return Selection(fileName, prefs[KEY_NAME] ?: "Custom font")
    }

    suspend fun importFont(uri: Uri): String = withContext(Dispatchers.IO) {
        mutex.withLock {
            val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
                ?.filterNot { it.isISOControl() }?.take(128)?.takeIf { it.isNotBlank() }
                ?: "Custom font"
            directory.mkdirs()
            val staged = File(directory, "stage_${UUID.randomUUID()}.tmp")
            var committed = false
            val destination = File(directory, "font_${UUID.randomUUID()}.font")
            try {
                val input = context.contentResolver.openInputStream(uri)
                    ?: throw IOException("Could not open the selected font")
                input.use { source -> staged.outputStream().use { copyBoundedFont(source, it) } }
                // Font.Builder rejects malformed fonts rather than quietly returning Roboto.
                loadTypeface(staged)
                if (!staged.renameTo(destination)) throw IOException("Could not save the font")
                context.pfpDataStore.edit { prefs ->
                    prefs[KEY_FILE] = destination.name
                    prefs[KEY_NAME] = name
                }
                committed = true
                pruneExcept(destination)
                name
            } finally {
                staged.delete()
                if (!committed) destination.delete()
            }
        }
    }

    suspend fun reset() = withContext(Dispatchers.IO) {
        mutex.withLock {
            context.pfpDataStore.edit { it.remove(KEY_FILE); it.remove(KEY_NAME) }
            pruneExcept(null)
        }
    }

    private fun pruneExcept(keep: File?) {
        directory.listFiles()?.filter { it != keep }?.forEach { it.delete() }
    }

    companion object {
        val KEY_FILE = stringPreferencesKey("display_custom_font_file")
        val KEY_NAME = stringPreferencesKey("display_custom_font_name")
        const val MAX_BYTES = 16 * 1024 * 1024

        internal fun copyBoundedFont(input: InputStream, output: java.io.OutputStream) {
            val header = ByteArray(4)
            var headerSize = 0
            while (headerSize < header.size) {
                val count = input.read(header, headerSize, header.size - headerSize)
                if (count == -1) throw IOException("Choose a valid OTF or TTF font")
                headerSize += count
            }
            val valid = header.contentEquals(byteArrayOf(0, 1, 0, 0)) ||
                header.contentEquals("OTTO".toByteArray(Charsets.US_ASCII)) ||
                header.contentEquals("true".toByteArray(Charsets.US_ASCII))
            if (!valid) throw IOException("Choose a valid OTF or TTF font")
            output.write(header)
            var total = header.size
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count == -1) break
                total += count
                if (total > MAX_BYTES) throw IOException("Fonts must be 16 MB or smaller")
                output.write(buffer, 0, count)
            }
        }

        private fun loadTypeface(file: File): Typeface {
            val font = Font.Builder(file).build()
            return Typeface.CustomFallbackBuilder(FontFamily.Builder(font).build())
                .setStyle(font.style)
                .setSystemFallback("sans-serif")
                .build()
        }
    }
}
