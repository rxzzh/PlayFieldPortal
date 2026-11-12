package com.playfieldportal.feature.xmb.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.RuntimeShader
import android.os.Build
import android.os.PowerManager
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import com.playfieldportal.core.ui.motion.MotionWallpaperBackground
import com.playfieldportal.core.ui.motion.MotionWallpaperPolicy
import com.playfieldportal.themekit.MotionCrop
import com.playfieldportal.core.ui.theme.LocalPFPColors
import com.playfieldportal.core.ui.wave.WaveStyle
import kotlin.math.sin
import kotlinx.coroutines.delay

// Frozen "time" (seconds) used to pose the wave when animation is disabled.
private const val STATIC_TIME = 2.0f
private const val TAU = 6.2831853f

/**
 * Root background behind the XMB UI. The wallpaper automatically replaces the wave:
 *
 *  - When [motionWallpaperPath] is set, the looping motion wallpaper renders over its poster —
 *    but ONLY while [motionDecision] says PLAY/PLAY_REDUCED; a POSTER decision renders the poster
 *    still alone and never constructs an ExoPlayer. (When the wave would be frozen, the motion
 *    wallpaper is not merely paused — no decoder exists.)
 *  - Otherwise, when [customWallpaperPath] is set, the still wallpaper renders alone — no wave
 *    is drawn or animated, and no wave resources are allocated. A POSTER decision with a motion
 *    file set lands here too: the frozen path is literally the existing still-wallpaper composable.
 *  - When no wallpaper is set, the XMB wave is rendered, honoring [waveStyle].
 *
 * The motion decision itself is computed ONCE by the shell through [MotionWallpaperPolicy] (the
 * single definition of "the device is busy or conserving" that both the shader and the decoder
 * obey) and passed down; callers that render only a wave (boot) can ignore it entirely.
 *
 * The wave is a reconstruction of the PSP XMB flow: a soft, luminous ribbon low on the
 * screen built from several summed sine waves, with a glowing crest over
 * a monthly-tinted vertical gradient. On API 33+ it's a single AGSL fragment shader; older devices
 * get a Canvas fallback (glowing stroked ribbons) that keeps the same silhouette.
 */
@Composable
fun XmbBackground(
    waveStyle: WaveStyle,
    customWallpaperPath: String? = null,
    motionWallpaperPath: String? = null,
    motionDecision: MotionWallpaperPolicy.Decision = MotionWallpaperPolicy.Decision.PLAY,
    modifier: Modifier = Modifier,
    motionCrop: MotionCrop? = null,
) {
    when {
        customWallpaperPath != null && motionWallpaperPath != null && motionDecision != MotionWallpaperPolicy.Decision.POSTER ->
            MotionWallpaperBackground(
                posterPath = customWallpaperPath,
                motionPath = motionWallpaperPath,
                decision = motionDecision,
                modifier = modifier,
                motionCrop = motionCrop,
            )
        customWallpaperPath != null -> WallpaperBackground(customWallpaperPath, modifier)
        else -> WaveBackground(waveStyle, modifier)
    }
}

/**
 * Whether the XMB wave animation should be frozen to relieve the device, honoring the two
 * Settings ▸ Display toggles. The wave is a non-essential flourish, so it shouldn't burn power when
 * the system is already conserving it:
 *
 *  - [respectBatterySaver]: true while the OS is in battery-saver mode (live via
 *    `ACTION_POWER_SAVE_MODE_CHANGED`).
 *  - [thermalThrottleAware]: true while the OS reports at least moderate thermal throttling (live via
 *    a thermal-status listener; API 29+ only).
 *
 * Returns false whenever the matching setting is off or the platform can't report the state.
 */
@Composable
fun rememberWavePowerThrottle(
    respectBatterySaver: Boolean,
    thermalThrottleAware: Boolean,
): Boolean {
    val context = LocalContext.current
    val powerManager = remember(context) { context.getSystemService(PowerManager::class.java) }

    // Battery-saver: seed from the current value, then track ACTION_POWER_SAVE_MODE_CHANGED.
    var powerSave by remember { mutableStateOf(false) }
    DisposableEffect(powerManager, respectBatterySaver) {
        if (powerManager == null || !respectBatterySaver) {
            powerSave = false
            return@DisposableEffect onDispose {}
        }
        powerSave = powerManager.isPowerSaveMode
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                powerSave = powerManager.isPowerSaveMode
            }
        }
        context.registerReceiver(receiver, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED))
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    // Thermal throttling: status listener (available since API 29 == minSdk); freeze at MODERATE+.
    var thermalThrottling by remember { mutableStateOf(false) }
    DisposableEffect(powerManager, thermalThrottleAware) {
        if (powerManager == null || !thermalThrottleAware) {
            thermalThrottling = false
            return@DisposableEffect onDispose {}
        }
        thermalThrottling = powerManager.currentThermalStatus >= PowerManager.THERMAL_STATUS_MODERATE
        val listener = PowerManager.OnThermalStatusChangedListener { status ->
            thermalThrottling = status >= PowerManager.THERMAL_STATUS_MODERATE
        }
        powerManager.addThermalStatusListener(listener)
        onDispose { powerManager.removeThermalStatusListener(listener) }
    }

    return powerSave || thermalThrottling
}

@Composable
private fun WallpaperBackground(
    customWallpaperPath: String,
    modifier: Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        com.playfieldportal.core.ui.motion.ArtworkImage(
            model              = customWallpaperPath,
            contentDescription = null,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier.fillMaxSize(),
        )
        // Light scrim so the XMB labels stay readable over any wallpaper.
        Box(Modifier.fillMaxSize().background(com.playfieldportal.core.ui.theme.ThemeTokens.WallpaperScrim))
    }
}

@Composable
private fun WaveBackground(
    waveStyle: WaveStyle,
    modifier: Modifier,
) {
    val colors = LocalPFPColors.current
    val alphaScale = if (waveStyle.reduced) 0.5f else 1f
    val ampScale   = if (waveStyle.reduced) 0.65f else 1f

    // Continuously-increasing time in seconds since the first frame (so float precision stays sharp),
    // scaled by style speed. Frozen at STATIC_TIME when the wave shouldn't animate — no frame loop.
    // Only advances while this background is on screen.
    //
    // Two things keep it cheap. It is READ ONLY WHILE DRAWING (the waves take a State, not a
    // Float), so a tick repaints the wave's own layer and never recomposes this tree. And it ticks
    // at ~30 fps (WAVE_FRAME_INTERVAL_MS), not every vsync: the folds drift over seconds, and the
    // panel runs at 60-144 Hz. The clock is wall time, so the cap never changes the speed.
    val animated = waveStyle.animated
    val speed = if (waveStyle.reduced) 0.5f else 1f
    val time = produceState(STATIC_TIME, animated, speed) {
        if (!animated) {
            value = STATIC_TIME
            return@produceState
        }
        var startMs = -1L
        var previousFrame = -1L
        while (true) {
            withInfiniteAnimationFrameMillis { frameMs ->
                if (startMs < 0L) startMs = frameMs
                val frame = pspWaveFrameAt(frameMs - startMs)
                if (frame != previousFrame) {
                    value = waveTimeSeconds(frameMs, startMs, speed)
                    previousFrame = frame
                }
            }
        }
    }

    // Monthly-tinted vertical gradient: deep top (keeps the status strip legible) easing to the pale
    // bottom the wave sits against. Rebuilt only when the theme colours change.
    val gradient = remember(colors.backgroundTop, colors.backgroundBottom) {
        Brush.linearGradient(
            colorStops = arrayOf(
                0.00f to colors.backgroundTop,
                0.30f to colors.backgroundTop,
                0.70f to lerp(colors.backgroundTop, colors.backgroundBottom, 0.5f),
                1.00f to colors.backgroundBottom,
            )
        )
    }

    Box(modifier = modifier.fillMaxSize().background(gradient)) {
        // Its own layer, so each tick re-records only the wave — not the gradient, the bloom, or
        // anything drawn alongside the background.
        val waveLayer = Modifier.fillMaxSize().graphicsLayer()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ShaderWave(time, alphaScale, ampScale, waveLayer)
        } else {
            FallbackWave(time, alphaScale, ampScale, waveLayer)
        }
        // Soft off-centre light bloom — the same gentle highlight the XMB has near the crossbar.
        // The brush is built once per size, never per frame.
        Spacer(
            modifier = Modifier.fillMaxSize().drawWithCache {
                val bloom = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(x = size.width * 0.48f, y = size.height * 0.30f),
                    radius = size.minDimension * 0.62f,
                )
                onDrawBehind { drawRect(brush = bloom) }
            },
        )
    }
}

/** The animated wave repaints at ~30 fps; see WaveBackground. */
internal const val WAVE_FRAME_INTERVAL_MS = 33L

// Bucket VSYNC timestamps, rather than sleeping 33 ms after VSYNC (which produces ~20 fps
// on a 60 Hz screen). Draw at the source model's 30 fps on both 60 Hz and 120 Hz panels.
internal fun pspWaveFrameAt(elapsedMillis: Long): Long = elapsedMillis * 30L / 1000L

/** The wave's clock: seconds since its first frame, scaled by the style's speed. */
internal fun waveTimeSeconds(frameMs: Long, startMs: Long, speed: Float): Float =
    (frameMs - startMs) / 1000f * speed

// ── AGSL wave (API 33+) ──────────────────────────────────────────────────────
// Reconstructed folds driven by measured native pose/morph tracks. This is an analytic
// redraw, not the original GMO mesh or native PSP rendering program.
private const val AGSL_WAVE = """
uniform float2 iResolution;
uniform float phase;
uniform float tilt;
uniform float morph;
uniform float ampScale;
uniform float alphaScale;
const float TAU = 6.2831853;
float fold(float y, float c, float sheet, float edge, float softness) {
    float body = smoothstep(0.0, softness, y - c) * sheet;
    float rim = exp(-pow((y - c) * 90.0, 2.0)) * edge;
    float shoulder = exp(-pow((y - c - 0.018) * 20.0, 2.0)) * edge * 0.35;
    return body + rim + shoulder;
}
half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution;
    float x = uv.x;
    float bend = (x - 0.5) * (x - 0.5);
    float c1 = 0.59 + ampScale * (0.12 * bend - 0.07 * morph + 0.045 * sin(x * TAU * 0.65 + phase) + 0.035 * tilt);
    float c2 = 0.72 + ampScale * (0.10 * bend + 0.055 * sin(x * TAU * 0.48 - phase) + 0.05 * tilt);
    float c3 = 0.85 + ampScale * (0.035 * sin(x * TAU * 0.8 + phase) - 0.025 * morph);
    if (uv.y < min(c1, min(c2, c3)) - 0.07) return half4(0.0);
    float b = fold(uv.y, c1, 0.13, 0.12, 0.24)
            + fold(uv.y, c2, 0.12, 0.10, 0.19)
            + fold(uv.y, c3, 0.08, 0.06, 0.15);
    b = clamp(b * alphaScale, 0.0, 0.48);
    return half4(1.0, 1.0, 1.0, 1.0) * b;
}
"""

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun ShaderWave(time: State<Float>, alphaScale: Float, ampScale: Float, modifier: Modifier) {
    val shader = remember { RuntimeShader(AGSL_WAVE) }
    val brush = remember(shader) { ShaderBrush(shader) }
    Canvas(modifier = modifier) {
        shader.setFloatUniform("iResolution", size.width, size.height)
        // Read here, in the draw phase: a tick invalidates this draw only, never composition.
        val pose = pspWavePoseAt(time.value)
        shader.setFloatUniform("phase", pose.phase)
        shader.setFloatUniform("tilt", pose.tilt)
        shader.setFloatUniform("morph", pose.morph)
        shader.setFloatUniform("ampScale", ampScale)
        shader.setFloatUniform("alphaScale", alphaScale)
        // Skip known-transparent scanlines before invoking the fragment shader at all.
        // The analytic lower bound includes the shader's seven-percent transparent margin.
        clipRect(top = pspWaveInvisibleTop(pose, ampScale) * size.height) {
            drawRect(brush = brush)
        }
    }
}

// ── Canvas fallback (API < 33) ───────────────────────────────────────────────
// Same soft folds approximated with low-alpha white fills (the sheet) + faint crest strokes.
@Composable
private fun FallbackWave(time: State<Float>, alphaScale: Float, ampScale: Float, modifier: Modifier) {
    Spacer(
        modifier = modifier.drawWithCache {
            // Paths and strokes are made once per size and reused every frame; only their points move.
            val crest = Path()
            val fill = Path()
            val wide = Stroke(width = size.height * 0.022f)
            val thin = Stroke(width = size.height * 0.006f)
            onDrawBehind {
                // Read here, in the draw phase: a tick invalidates this draw only, never composition.
                val t = time.value
                val pose = pspWavePoseAt(t)
                drawPspFold(pose, 0, ampScale, crest, fill, wide, thin, .13f * alphaScale, .12f * alphaScale)
                drawPspFold(pose, 1, ampScale, crest, fill, wide, thin, .12f * alphaScale, .10f * alphaScale)
                drawPspFold(pose, 2, ampScale, crest, fill, wide, thin, .08f * alphaScale, .06f * alphaScale)
            }
        },
    )
}

private fun DrawScope.drawPspFold(
    pose: PspWavePose, layer: Int, amplitude: Float, crestPath: Path, fillPath: Path, wide: Stroke, thin: Stroke,
    sheet: Float, edge: Float,
) {
    val w = size.width
    val h = size.height
    val n = 48
    crestPath.reset()
    fillPath.reset()
    fillPath.moveTo(0f, h)
    for (i in 0..n) {
        val xx = i / n.toFloat()
        val y = pspWaveCrest(layer, xx, pose, amplitude) * h
        val x = xx * w
        if (i == 0) { crestPath.moveTo(x, y); fillPath.lineTo(x, y) }
        else { crestPath.lineTo(x, y); fillPath.lineTo(x, y) }
    }
    fillPath.lineTo(w, h)
    fillPath.close()

    // Sheet: a flat, faint white wash from the crest down — stacking the folds brightens the lower
    // screen like the reference. Crest: two soft white strokes for the gentle fold highlight.
    drawPath(fillPath, color = Color.White.copy(alpha = sheet))
    drawPath(crestPath, color = Color.White.copy(alpha = edge * 0.5f), style = wide)
    drawPath(crestPath, color = Color.White.copy(alpha = edge), style = thin)
}
