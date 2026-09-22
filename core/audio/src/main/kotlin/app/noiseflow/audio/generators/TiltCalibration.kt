package app.noiseflow.audio.generators

import app.noiseflow.audio.AudioFormat
import app.noiseflow.audio.Rng
import app.noiseflow.audio.dsp.DcBlocker
import app.noiseflow.audio.dsp.TiltFilter
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.floor
import kotlin.math.sqrt

/**
 * Makes every slope come out at the same loudness.
 *
 * A -12 dB/oct cascade and a +6 dB/oct cascade differ in output level by tens
 * of dB, so without this the Tone slider would double as a volume control —
 * which, at 3am, is the single most annoying bug this app could have.
 *
 * The gain is measured rather than derived: render a fixed white-noise burst
 * through the chain and take the RMS. Deterministic (fixed seed), and correct
 * for whatever the filter actually does rather than for what the maths says it
 * should do.
 *
 * Measuring on every slider movement would be far too slow, so gains are
 * measured on a coarse grid and interpolated. The curve is smooth, so linear
 * interpolation between 1.5 dB/oct steps is well inside audibility.
 */
internal object TiltCalibration {
    private const val GRID_STEP = 1.5f
    private const val MEASURE_SECONDS = 1.0f
    private const val WARMUP_SECONDS = 0.25f
    private const val CALIBRATION_SEED = 0x5EEDL

    private val cache = ConcurrentHashMap<Long, Float>()

    fun gainFor(slopeDbPerOctave: Float, sampleRate: Int): Float {
        val slope = slopeDbPerOctave.coerceIn(TiltFilter.MIN_SLOPE, TiltFilter.MAX_SLOPE)
        val index = floor(slope / GRID_STEP)
        val lower = (index * GRID_STEP).coerceIn(TiltFilter.MIN_SLOPE, TiltFilter.MAX_SLOPE)
        val upper = (lower + GRID_STEP).coerceAtMost(TiltFilter.MAX_SLOPE)
        if (upper <= lower) return measured(lower, sampleRate)

        val t = (slope - lower) / (upper - lower)
        return measured(lower, sampleRate) * (1f - t) + measured(upper, sampleRate) * t
    }

    private fun measured(slope: Float, sampleRate: Int): Float {
        val key = key(slope, sampleRate)
        cache[key]?.let { return it }
        val g = measure(slope, sampleRate)
        cache[key] = g
        return g
    }

    private fun measure(slope: Float, sampleRate: Int): Float {
        val rng = Rng(CALIBRATION_SEED)
        val tilt = TiltFilter(slope, sampleRate)
        val dc = DcBlocker(sampleRate = sampleRate)

        val warmup = (WARMUP_SECONDS * sampleRate).toInt()
        repeat(warmup) { dc.process(tilt.process(rng.nextBipolar())) }

        val n = (MEASURE_SECONDS * sampleRate).toInt()
        var sum = 0.0
        repeat(n) {
            val v = dc.process(tilt.process(rng.nextBipolar()))
            sum += v.toDouble() * v.toDouble()
        }
        val rms = sqrt(sum / n).toFloat()
        return if (rms <= 0f || !rms.isFinite()) 1f else AudioFormat.GENERATOR_TARGET_RMS / rms
    }

    private fun key(slope: Float, sampleRate: Int): Long =
        (Math.round(slope * 100f).toLong() shl 32) or sampleRate.toLong()
}
