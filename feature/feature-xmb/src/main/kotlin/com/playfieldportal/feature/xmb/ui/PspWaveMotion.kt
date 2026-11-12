package com.playfieldportal.feature.xmb.ui

import kotlin.math.PI
import kotlin.math.sin

/**
 * Reconstructed PSP background motion, not an embedded firmware program or model.
 * Timing/pose samples measured from PSP 6.61 system_plugin_bg's mdl_bg:
 * 1500 frames / 30 fps, fcurve-0's X tilt and fcurve-1's morph weight.
 * The surface itself is redrawn analytically in AGSL/Canvas; no Sony texture is bundled.
 */
internal const val PSP_WAVE_PERIOD_SECONDS = 50f
internal data class PspWavePose(val phase: Float, val tilt: Float, val morph: Float)

internal fun pspWavePoseAt(seconds: Float): PspWavePose {
    val t = ((seconds % PSP_WAVE_PERIOD_SECONDS) + PSP_WAVE_PERIOD_SECONDS) % PSP_WAVE_PERIOD_SECONDS
    return PspWavePose(t / PSP_WAVE_PERIOD_SECONDS * (2 * PI).toFloat(), sampleTrack(TILT, t), sampleTrack(MORPH, t))
}

private fun sampleTrack(track: FloatArray, time: Float): Float {
    for (i in 2 until track.size step 2) {
        if (time <= track[i]) {
            val u = (time - track[i - 2]) / (track[i] - track[i - 2])
            return track[i - 1] + (track[i + 1] - track[i - 1]) * u
        }
    }
    return track.last()
}

/** Same crests as the AGSL path, also used by the Canvas fallback. */
internal fun pspWaveCrest(layer: Int, x: Float, pose: PspWavePose, amplitude: Float = 1f): Float {
    val tau = (2 * PI).toFloat()
    val bend = (x - .5f) * (x - .5f)
    return when (layer) {
        0 -> .59f + amplitude * (.12f * bend - .07f * pose.morph + .045f * sin(x * tau * .65f + pose.phase) + .035f * pose.tilt)
        1 -> .72f + amplitude * (.10f * bend + .055f * sin(x * tau * .48f - pose.phase) + .05f * pose.tilt)
        else -> .85f + amplitude * (.035f * sin(x * tau * .8f + pose.phase) - .025f * pose.morph)
    }
}

private val TILT = floatArrayOf(
    0.000000f, 0.642788f,
    0.066667f, 0.610099f,
    0.166667f, 0.567784f,
    0.300000f, 0.517835f,
    0.466667f, 0.461300f,
    0.666667f, 0.398957f,
    0.900000f, 0.331609f,
    1.166667f, 0.260239f,
    1.466667f, 0.186110f,
    1.800000f, 0.110843f,
    2.133333f, 0.042913f,
    2.466667f, -0.017493f,
    2.766667f, -0.065050f,
    3.066667f, -0.105579f,
    3.333333f, -0.135007f,
    3.600000f, -0.157252f,
    3.833333f, -0.169676f,
    4.066667f, -0.174297f,
    11.633333f, -0.245287f,
    16.500000f, -0.283978f,
    20.800000f, -0.310566f,
    25.000000f, -0.328620f,
    29.300000f, -0.339044f,
    33.466667f, -0.341698f,
    33.933333f, -0.335708f,
    34.433333f, -0.321421f,
    34.933333f, -0.299614f,
    35.466667f, -0.268666f,
    36.033333f, -0.227729f,
    36.600000f, -0.179150f,
    37.200000f, -0.120158f,
    37.866667f, -0.046578f,
    38.600000f, 0.042495f,
    39.266667f, 0.129114f,
    39.866667f, 0.210115f,
    40.466667f, 0.292501f,
    41.066667f, 0.374753f,
    41.666667f, 0.455322f,
    42.300000f, 0.536860f,
    43.000000f, 0.620807f,
    43.633333f, 0.689562f,
    44.233333f, 0.747258f,
    44.800000f, 0.794409f,
    45.366667f, 0.834018f,
    45.933333f, 0.865884f,
    46.466667f, 0.888734f,
    46.900000f, 0.902132f,
    47.266667f, 0.909699f,
    47.600000f, 0.913319f,
    47.900000f, 0.913539f,
    48.166667f, 0.910825f,
    48.400000f, 0.905643f,
    48.633333f, 0.897079f,
    48.833333f, 0.886206f,
    49.033333f, 0.870960f,
    49.200000f, 0.853787f,
    49.366667f, 0.831129f,
    49.500000f, 0.807746f,
    49.633333f, 0.778126f,
    49.733333f, 0.750571f,
    49.833333f, 0.717063f,
    49.933333f, 0.675821f,
    50.000000f, 0.642788f,
)

private val MORPH = floatArrayOf(
    0.000000f, 0.000000f,
    0.600000f, 0.003795f,
    1.233333f, 0.015618f,
    1.900000f, 0.036025f,
    2.600000f, 0.065415f,
    3.333333f, 0.104000f,
    4.133333f, 0.154006f,
    5.033333f, 0.218525f,
    6.066667f, 0.301031f,
    7.433333f, 0.419315f,
    10.000000f, 0.648000f,
    11.166667f, 0.745174f,
    12.100000f, 0.815914f,
    12.933333f, 0.871951f,
    13.700000f, 0.916228f,
    14.433333f, 0.950944f,
    15.100000f, 0.975153f,
    15.733333f, 0.990943f,
    16.333333f, 0.998816f,
    16.933333f, 0.999240f,
    17.533333f, 0.992169f,
    18.166667f, 0.977158f,
    18.833333f, 0.953694f,
    19.533333f, 0.921425f,
    20.300000f, 0.878148f,
    21.133333f, 0.823026f,
    22.066667f, 0.753096f,
    23.200000f, 0.659481f,
    25.033333f, 0.497000f,
    26.800000f, 0.340519f,
    27.933333f, 0.246904f,
    28.866667f, 0.176974f,
    29.700000f, 0.121852f,
    30.466667f, 0.078575f,
    31.166667f, 0.046306f,
    31.833333f, 0.022842f,
    32.466667f, 0.007831f,
    33.066667f, 0.000760f,
    50.000000f, 0.000000f,
)
