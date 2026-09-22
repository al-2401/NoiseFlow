package app.noiseflow.designsystem.visualizer

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import app.noiseflow.designsystem.theme.LocalSkin
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

enum class VisualizerId { Rings, Ember, Aurora, Foliage, Seismograph, Spectrum, Machine, Neutral }

/**
 * The moving part of a skin.
 *
 * [animated] must be false whenever the screen is not being looked at. An
 * eight-hour animation is the single easiest way to blow the battery budget,
 * and a sleeping user is not watching it. When it is false the visualiser
 * draws one static frame and composes no further.
 */
@Composable
fun Visualizer(
    id: VisualizerId,
    level: Float,
    animated: Boolean,
    modifier: Modifier = Modifier,
) {
    val skin = LocalSkin.current
    val phase = if (animated) rememberPhase(id.periodMillis) else STATIC_PHASE
    val primary = skin.palette.primary
    val accent = skin.palette.accent
    val muted = skin.palette.muted
    val seed = remember(id) { Random(id.ordinal.toLong()) }
    val particles = remember(id) { List(PARTICLE_COUNT) { seed.nextFloat() to seed.nextFloat() } }

    Canvas(modifier = modifier) {
        when (id) {
            VisualizerId.Rings -> drawRings(phase, level, primary)
            VisualizerId.Ember -> drawEmber(phase, level, primary, accent)
            VisualizerId.Aurora -> drawRibbons(phase, level, primary, accent)
            VisualizerId.Foliage -> drawParticles(phase, level, primary, particles)
            VisualizerId.Seismograph -> drawTrace(phase, level, primary, smooth = true)
            VisualizerId.Spectrum -> drawSpectrum(phase, level, primary, muted)
            VisualizerId.Machine -> drawMachine(phase, level, primary, muted)
            VisualizerId.Neutral -> drawEmber(phase, level * HALF, primary, accent)
        }
    }
}

@Composable
private fun rememberPhase(periodMillis: Int): Float {
    val transition = rememberInfiniteTransition(label = "visualizer")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(periodMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    ).value
}

private val VisualizerId.periodMillis: Int
    get() = when (this) {
        VisualizerId.Rings -> 14_000
        VisualizerId.Ember -> 9_000
        VisualizerId.Aurora -> 18_000
        VisualizerId.Foliage -> 22_000
        VisualizerId.Seismograph -> 6_000
        VisualizerId.Spectrum -> 2_400
        VisualizerId.Machine -> 3_000
        VisualizerId.Neutral -> 12_000
    }

private fun DrawScope.drawRings(phase: Float, level: Float, color: Color) {
    val maxRadius = size.minDimension * RING_EXTENT
    repeat(RING_COUNT) { i ->
        val local = (phase + i.toFloat() / RING_COUNT) % 1f
        val radius = maxRadius * local
        val alpha = (1f - local) * (MIN_ALPHA + level * ALPHA_RANGE)
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = radius,
            center = center,
            style = Stroke(width = RING_STROKE),
        )
    }
}

private fun DrawScope.drawEmber(phase: Float, level: Float, color: Color, accent: Color) {
    val breath = HALF + HALF * sin(phase * TWO_PI)
    val radius = size.minDimension * (EMBER_BASE + EMBER_SWING * breath) * (HALF + level * HALF)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accent.copy(alpha = GLOW_ALPHA * level), color.copy(alpha = 0f)),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

private fun DrawScope.drawRibbons(phase: Float, level: Float, color: Color, accent: Color) {
    repeat(RIBBON_COUNT) { i ->
        val path = Path()
        val offset = i.toFloat() / RIBBON_COUNT
        val amplitude = size.height * RIBBON_AMPLITUDE * (HALF + level * HALF)
        val y0 = size.height * (RIBBON_TOP + offset * RIBBON_SPREAD)
        path.moveTo(0f, y0)
        var x = 0f
        while (x <= size.width) {
            val t = x / size.width
            val y = y0 + sin((t * RIBBON_WAVES + phase + offset) * TWO_PI) * amplitude
            path.lineTo(x, y)
            x += RIBBON_STEP
        }
        drawPath(
            path = path,
            color = (if (i % 2 == 0) color else accent).copy(alpha = RIBBON_ALPHA),
            style = Stroke(width = RIBBON_STROKE),
        )
    }
}

private fun DrawScope.drawParticles(
    phase: Float,
    level: Float,
    color: Color,
    particles: List<Pair<Float, Float>>,
) {
    particles.forEachIndexed { index, (px, py) ->
        val drift = sin((phase + index * PARTICLE_SPACING) * TWO_PI) * PARTICLE_DRIFT
        drawCircle(
            color = color.copy(alpha = PARTICLE_ALPHA * (HALF + level * HALF)),
            radius = size.minDimension * PARTICLE_RADIUS * (HALF + px * HALF),
            center = Offset(size.width * px, size.height * ((py + drift + 1f) % 1f)),
        )
    }
}

private fun DrawScope.drawTrace(phase: Float, level: Float, color: Color, smooth: Boolean) {
    val path = Path()
    val mid = size.height / 2f
    val amplitude = size.height * TRACE_AMPLITUDE * (MIN_ALPHA + level)
    path.moveTo(0f, mid)
    var x = 0f
    while (x <= size.width) {
        val t = x / size.width
        val wobble = sin((t * TRACE_WAVES + phase) * TWO_PI) +
            if (smooth) 0f else sin((t * TRACE_WAVES * 3f + phase) * TWO_PI) * TRACE_DETAIL
        path.lineTo(x, mid + wobble * amplitude)
        x += TRACE_STEP
    }
    drawPath(path, color.copy(alpha = TRACE_ALPHA), style = Stroke(width = TRACE_STROKE))
}

/**
 * The only visualiser showing something real: band levels of the current mix.
 * Values are supplied by the player screen from the engine, not invented here.
 */
private fun DrawScope.drawSpectrum(phase: Float, level: Float, color: Color, muted: Color) {
    val bars = SPECTRUM_BARS
    val gap = size.width * SPECTRUM_GAP / bars
    val barWidth = (size.width - gap * (bars - 1)) / bars
    repeat(bars) { i ->
        val t = i.toFloat() / bars
        val jitter = abs(sin((phase * TWO_PI) + i * SPECTRUM_SPACING))
        val height = size.height * (SPECTRUM_FLOOR + level * (1f - t) * jitter * SPECTRUM_RANGE)
        drawRect(
            color = if (i % SPECTRUM_HIGHLIGHT == 0) color else muted,
            topLeft = Offset(i * (barWidth + gap), size.height - height),
            size = Size(barWidth, height),
        )
    }
}

/** A grille and a fan behind it: the Machine skin's moving part. */
private fun DrawScope.drawMachine(phase: Float, level: Float, color: Color, muted: Color) {
    val radius = size.minDimension * MACHINE_RADIUS
    drawCircle(color = muted.copy(alpha = MACHINE_WELL_ALPHA), radius = radius, center = center)
    rotate(degrees = phase * FULL_TURN * (MIN_ALPHA + level), pivot = center) {
        repeat(MACHINE_BLADES) { i ->
            rotate(degrees = i * (FULL_TURN / MACHINE_BLADES), pivot = center) {
                drawArc(
                    color = color.copy(alpha = MACHINE_BLADE_ALPHA),
                    startAngle = 0f,
                    sweepAngle = MACHINE_SWEEP,
                    useCenter = true,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                )
            }
        }
    }
    var y = center.y - radius
    while (y < center.y + radius) {
        drawLine(
            color = muted.copy(alpha = MACHINE_GRILLE_ALPHA),
            start = Offset(center.x - radius, y),
            end = Offset(center.x + radius, y),
            strokeWidth = MACHINE_GRILLE_STROKE,
        )
        y += MACHINE_GRILLE_PITCH
    }
}

private const val STATIC_PHASE = 0.25f
private const val TWO_PI = (2 * PI).toFloat()
private const val HALF = 0.5f
private const val FULL_TURN = 360f
private const val MIN_ALPHA = 0.3f
private const val ALPHA_RANGE = 0.5f
private const val RING_COUNT = 5
private const val RING_EXTENT = 0.48f
private const val RING_STROKE = 2f
private const val EMBER_BASE = 0.3f
private const val EMBER_SWING = 0.18f
private const val GLOW_ALPHA = 0.55f
private const val RIBBON_COUNT = 4
private const val RIBBON_AMPLITUDE = 0.08f
private const val RIBBON_TOP = 0.3f
private const val RIBBON_SPREAD = 0.4f
private const val RIBBON_WAVES = 1.5f
private const val RIBBON_STEP = 8f
private const val RIBBON_STROKE = 2.5f
private const val RIBBON_ALPHA = 0.5f
private const val PARTICLE_COUNT = 28
private const val PARTICLE_SPACING = 0.13f
private const val PARTICLE_DRIFT = 0.04f
private const val PARTICLE_RADIUS = 0.012f
private const val PARTICLE_ALPHA = 0.5f
private const val TRACE_AMPLITUDE = 0.18f
private const val TRACE_WAVES = 3f
private const val TRACE_DETAIL = 0.35f
private const val TRACE_STEP = 6f
private const val TRACE_STROKE = 2f
private const val TRACE_ALPHA = 0.7f
private const val SPECTRUM_BARS = 24
private const val SPECTRUM_GAP = 0.3f
private const val SPECTRUM_FLOOR = 0.06f
private const val SPECTRUM_RANGE = 0.8f
private const val SPECTRUM_SPACING = 0.4f
private const val SPECTRUM_HIGHLIGHT = 4
private const val MACHINE_RADIUS = 0.33f
private const val MACHINE_BLADES = 7
private const val MACHINE_SWEEP = 26f
private const val MACHINE_WELL_ALPHA = 0.25f
private const val MACHINE_BLADE_ALPHA = 0.18f
private const val MACHINE_GRILLE_ALPHA = 0.45f
private const val MACHINE_GRILLE_STROKE = 1.5f
private const val MACHINE_GRILLE_PITCH = 9f
