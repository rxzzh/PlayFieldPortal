package com.playfieldportal.feature.settings.maintenance

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

internal fun backgroundCleanupSupported(api: Int) = api < 34
internal fun cleanupCandidate(packageName: String, ownPackage: String, flags: Int, homes: Set<String>, uid: Int = 10000): Boolean =
    packageName != ownPackage && packageName !in homes &&
        uid >= 10000 && flags and ApplicationInfo.FLAG_PERSISTENT == 0

internal data class CleanupResult(val requested: Int, val failed: Int, val beforeBytes: Long, val afterBytes: Long)

/** Uses the OS's cached-process API, never force-stop, shell, root, or accessibility. */
internal class BackgroundAppCleaner(private val context: Context) {
    private val manager = requireNotNull(context.getSystemService(ActivityManager::class.java))
    fun availableBytes(): Long = ActivityManager.MemoryInfo().also(manager::getMemoryInfo).availMem

    @Suppress("DEPRECATION")
    suspend fun clean(): CleanupResult = withContext(Dispatchers.IO) {
        check(backgroundCleanupSupported(Build.VERSION.SDK_INT)) { "This Android version only permits cleaning our own processes." }
        val before = availableBytes()
        val pm = context.packageManager
        val homes = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
            .map { it.activityInfo.packageName }.toSet()
        val candidates = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .mapNotNull { it.activityInfo?.applicationInfo }.distinctBy { it.packageName }
            .filter { cleanupCandidate(it.packageName, context.packageName, it.flags, homes, it.uid) }
        var requested = 0
        var failed = 0
        for (app in candidates) {
            try {
                manager.killBackgroundProcesses(app.packageName)
                requested++
            } catch (_: SecurityException) { failed++ }
        }
        // Android reclaims asynchronously; protected or previous apps may be retained.
        delay(600)
        // Counts are distinct package requests, not claimed process deaths.
        CleanupResult(requested, failed, before, availableBytes())
    }
}
