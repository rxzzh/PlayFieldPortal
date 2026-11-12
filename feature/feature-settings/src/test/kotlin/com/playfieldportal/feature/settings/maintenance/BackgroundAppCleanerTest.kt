package com.playfieldportal.feature.settings.maintenance

import android.content.pm.ApplicationInfo
import org.junit.Assert.*
import org.junit.Test

class BackgroundAppCleanerTest {
    @Test fun `only platforms that allow other-app cleanup offer the action`() {
        assertTrue(backgroundCleanupSupported(33))
        assertFalse(backgroundCleanupSupported(34))
        assertFalse(backgroundCleanupSupported(37))
    }
    @Test fun `launcher core and persistent packages are protected while user-facing apps remain eligible`() {
        val homes = setOf("other.home")
        assertFalse(cleanupCandidate("pfp", "pfp", 0, homes))
        assertFalse(cleanupCandidate("other.home", "pfp", 0, homes))
        assertFalse(cleanupCandidate("system", "pfp", 0, homes, uid = 1000))
        assertFalse(cleanupCandidate("persistent", "pfp", ApplicationInfo.FLAG_PERSISTENT, homes))
        assertTrue(cleanupCandidate("chrome", "pfp", ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP, homes, uid = 10080))
        assertTrue(cleanupCandidate("org.ppsspp.ppsspp", "pfp", 0, homes))
    }
}
