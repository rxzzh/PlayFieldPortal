package com.playfieldportal.feature.xmb.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchTargetIdentityTest {
    private val a = XMBItem("a", "A", gameId = 1L, isRealGame = true)
    private val b = XMBItem("b", "B", gameId = 2L, isRealGame = true)
    @Test fun `a reordered list still activates the drawn game`() {
        assertEquals(1, listOf(b, a).indexOfLaunchTarget(a))
    }
    @Test fun `a removed row cannot activate its replacement`() {
        assertEquals(-1, listOf(b).indexOfLaunchTarget(a))
    }
    @Test fun `the same UMD row with a different game is stale`() {
        assertEquals(-1, listOf(b.copy(id = "umd_slot")).indexOfLaunchTarget(a.copy(id = "umd_slot")))
    }
    @Test fun `replaced app and shortcut targets are stale`() {
        val app = XMBItem("app", "App", packageName = "old.app", shortcutId = "first")
        assertEquals(-1, listOf(app.copy(packageName = "new.app")).indexOfLaunchTarget(app))
        assertEquals(-1, listOf(app.copy(shortcutId = "second")).indexOfLaunchTarget(app))
        assertEquals(0, listOf(app.copy(title = "Renamed")).indexOfLaunchTarget(app))
    }
}
