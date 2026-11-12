package com.playfieldportal.feature.xmb.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PspWaveMotionTest {
    @Test fun `vsync bucketing preserves thirty frames on sixty and one twenty hertz panels`() {
        for (hz in listOf(60, 120)) {
            val frames = (0 until hz * 2).map { pspWaveFrameAt(it * 1000L / hz) }.distinct()
            assertEquals(60, frames.size)
        }
    }
    @Test fun `native fifty second loop closes continuously`() {
        val a = pspWavePoseAt(0f)
        val b = pspWavePoseAt(PSP_WAVE_PERIOD_SECONDS)
        assertEquals(a, b)
        val before = pspWavePoseAt(PSP_WAVE_PERIOD_SECONDS - .001f)
        for (layer in 0..2) for (i in 0..40) {
            assertEquals(pspWaveCrest(layer, i / 40f, a), pspWaveCrest(layer, i / 40f, before), .001f)
        }
    }
    @Test fun `motion retains measured pose and reaches morph crest`() {
        assertEquals(.642788f, pspWavePoseAt(0f).tilt, .0001f)
        assertEquals(0f, pspWavePoseAt(0f).morph, .0001f)
        assertTrue(pspWavePoseAt(16.6f).morph > .99f)
    }
    @Test fun `all folds stay inside screen throughout loop and reduced motion`() {
        for (t in 0..500) for (layer in 0..2) for (i in 0..20) for (amp in listOf(1f,.65f)) {
            val crest = pspWaveCrest(layer, i / 20f, pspWavePoseAt(t / 10f), amp)
            assertTrue(crest in .3f..1f)
        }
    }
    @Test fun `shader scissor never removes visible crests throughout the full loop`() {
        for (t in 0..500) for (layer in 0..2) for (i in 0..100) for (amp in listOf(1f, .65f)) {
            val pose = pspWavePoseAt(t / 10f)
            assertTrue(pspWaveInvisibleTop(pose, amp) <= pspWaveCrest(layer, i / 100f, pose, amp) - .07f + .000001f)
        }
    }
}
