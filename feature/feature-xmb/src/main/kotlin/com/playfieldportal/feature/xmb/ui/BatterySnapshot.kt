package com.playfieldportal.feature.xmb.ui

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

internal data class BatterySnapshot(val level: Int? = null, val power: BatteryPowerState = BatteryPowerState.UNPLUGGED)

internal fun batteryPercent(level: Int, scale: Int): Int? =
    if (scale > 0 && level in 0..scale) (level.toLong() * 100L / scale).toInt() else null

internal fun batterySnapshotFrom(intent: Intent?, previous: BatterySnapshot = BatterySnapshot()): BatterySnapshot {
    if (intent == null) return previous
    val level = batteryPercent(intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1), intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1))
    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    return BatterySnapshot(
        level = level ?: previous.level,
        power = if (status < 0 && !intent.hasExtra(BatteryManager.EXTRA_PLUGGED)) previous.power else batteryPowerStateOf(status, intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)),
    )
}

internal fun readBatterySnapshot(context: Context, previous: BatterySnapshot = BatterySnapshot()): BatterySnapshot {
    val sticky = runCatching { context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) }.getOrNull()
    val result = batterySnapshotFrom(sticky, previous)
    if (result.level != null) return result
    val capacity = runCatching { context.getSystemService(BatteryManager::class.java)?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) }.getOrNull()
    return result.copy(level = capacity?.takeIf { it in 0..100 })
}
