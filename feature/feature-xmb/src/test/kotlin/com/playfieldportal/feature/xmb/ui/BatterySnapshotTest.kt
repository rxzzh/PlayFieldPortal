package com.playfieldportal.feature.xmb.ui

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BatterySnapshotTest {
    private fun reading(level: Int, scale: Int = 100) = Intent(Intent.ACTION_BATTERY_CHANGED)
        .putExtra(BatteryManager.EXTRA_LEVEL, level).putExtra(BatteryManager.EXTRA_SCALE, scale)
        .putExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_CHARGING)
        .putExtra(BatteryManager.EXTRA_PLUGGED, BatteryManager.BATTERY_PLUGGED_USB)
    @Test fun `cold start reads cached level before a new broadcast arrives`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.sendStickyBroadcast(reading(97))
        assertEquals(97, readBatterySnapshot(context).level)
    }
    @Test fun `returning to foreground refreshes a changed sticky level`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.sendStickyBroadcast(reading(97))
        val old = readBatterySnapshot(context)
        context.sendStickyBroadcast(reading(46))
        assertEquals(46, readBatterySnapshot(context, old).level)
    }
    @Test fun `missing or malformed readings do not invent zero percent`() {
        val old = BatterySnapshot(73, BatteryPowerState.PLUGGED)
        assertEquals(old, batterySnapshotFrom(null, old))
        assertEquals(old, batterySnapshotFrom(Intent(Intent.ACTION_BATTERY_CHANGED), old))
        assertEquals(73, batterySnapshotFrom(reading(-1), old).level)
        assertEquals(73, batterySnapshotFrom(reading(60, 0), old).level)
        assertEquals(73, batterySnapshotFrom(reading(101), old).level)
    }
    @Test fun `a genuine empty battery stays zero and non-percent scales normalize safely`() {
        assertEquals(0, batterySnapshotFrom(reading(0)).level)
        assertEquals(75, batterySnapshotFrom(reading(150, 200)).level)
        assertEquals(100, batteryPercent(Int.MAX_VALUE, Int.MAX_VALUE))
        assertNull(batteryPercent(-1, 100))
    }
}
