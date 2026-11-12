package com.playfieldportal.feature.settings.ui

import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.text.format.Formatter
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.playfieldportal.feature.settings.maintenance.BackgroundAppCleaner
import com.playfieldportal.feature.settings.maintenance.backgroundCleanupSupported
import kotlinx.coroutines.launch

@Composable
fun MemorySettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val cleaner = remember(context) { BackgroundAppCleaner(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var available by remember { mutableLongStateOf(cleaner.availableBytes()) }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    val supported = backgroundCleanupSupported(Build.VERSION.SDK_INT)
    LifecycleResumeEffect(Unit) {
        available = cleaner.availableBytes()
        onPauseOrDispose { }
    }
    SettingsScaffold(title = "Settings", subtitle = "Memory & Background Apps", onBack = onBack, modifier = modifier) {
        Column(Modifier.fillMaxSize()) {
            SettingsGroup("Memory")
            SettingsRow(label = "Available Memory", sublabel = Formatter.formatShortFileSize(context, available), onClick = null)
            SettingsGroup("Background Apps")
            SettingsRow(
                label = "Clear Background Apps",
                sublabel = when {
                    busy -> "Cleaning…"
                    result != null -> result!!
                    !supported -> "Android 14+ restricts other-app cleanup. Use Android App Settings."
                    else -> "Save progress first. Releases cached apps; Android may retain recently used or protected apps."
                },
                focusKey = "memory_clean_background",
                onClick = if (busy || !supported) null else ({
                    busy = true
                    scope.launch {
                        try {
                            val r = cleaner.clean()
                            available = r.afterBytes
                            result = "Requested cleanup for ${r.requested} apps" +
                                (if (r.failed > 0) " (${r.failed} denied)" else "") +
                                ". Free memory: ${Formatter.formatShortFileSize(context, r.beforeBytes)} → ${Formatter.formatShortFileSize(context, r.afterBytes)}"
                        } catch (e: Exception) {
                            if (e is kotlinx.coroutines.CancellationException) throw e
                            result = "Cleanup unavailable: ${e.message}"
                        } finally { busy = false }
                    }
                }),
            )
            SettingsRow(label = "Android App Settings", sublabel = "Manually force stop a game Android keeps active",
                focusKey = "memory_android_apps", onClick = { context.startActivity(Intent(Settings.ACTION_APPLICATION_SETTINGS)) })
        }
    }
}
