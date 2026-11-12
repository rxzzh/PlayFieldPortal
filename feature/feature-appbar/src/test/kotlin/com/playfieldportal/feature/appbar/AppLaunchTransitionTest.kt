package com.playfieldportal.feature.appbar

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppLaunchTransitionTest {
    private val dispatcher = StandardTestDispatcher()
    private val context = mockk<Context>(relaxed = true)
    private val pm = mockk<PackageManager>(relaxed = true)
    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        every { context.packageManager } returns pm
        every { pm.getLaunchIntentForPackage(any()) } answers { Intent("test.LAUNCH") }
        every { context.startActivity(any()) } just Runs
    }
    @After fun teardown() = Dispatchers.resetMain()

    @Test fun `software opens without waiting for a presentation and repeated taps never queue`() = runTest(dispatcher) {
        var starts = 0
        val repo = InstalledAppRepository(context)
        assertTrue(repo.launchApp("test.app") { starts++ })
        assertFalse(repo.launchApp("other.app"))
        runCurrent()
        verify(exactly = 1) { context.startActivity(any()) }
        assertEquals(1, starts)
    }

    @Test fun `missing software target does not launch or report success`() = runTest(dispatcher) {
        every { pm.getLaunchIntentForPackage("missing") } returns null
        var starts = 0
        val repo = InstalledAppRepository(context)
        assertFalse(repo.launchApp("missing") { starts++ })
        advanceUntilIdle()
        assertEquals(0, starts)
        verify(exactly = 0) { context.startActivity(any()) }
    }

    @Test fun `failed launch does not report success or block later launches`() = runTest(dispatcher) {
        var attempts = 0
        var starts = 0
        every { context.startActivity(any()) } answers {
            attempts++
            if (attempts == 1) throw SecurityException("test rejection")
        }
        val repo = InstalledAppRepository(context)
        assertTrue(repo.launchApp("first") { starts++ })
        advanceUntilIdle()
        assertEquals(0, starts)
        assertTrue(repo.launchApp("second") { starts++ })
        advanceUntilIdle()
        assertEquals(1, starts)
        assertEquals(2, attempts)
    }
}
