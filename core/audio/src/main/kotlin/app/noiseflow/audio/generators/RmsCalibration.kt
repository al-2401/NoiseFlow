package app.noiseflow.audio.generators

import app.noiseflow.audio.AudioFormat
import app.noiseflow.audio.Rng
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt

/**
 * Same idea as [TiltCalibration] but for generators with no continuous
 * parameter: measure once per sample rate, cache forever.
 */
internal object RmsCalibration {
    private const val MEASURE_SECONDS = 1.0f
    private const val WARMUP_SECONDS = 0.25f
    private const val CALIBRATION_SEED = 0x5EEDL

    private val cache = ConcurrentHashMap<String, Float>()

    /**
     * [build] must return a *fresh* per-sample processing function with its own
     * filter state; it is called once, off the render thread.
     */
    fun gain(cacheKey: String, sampleRate: Int, build: () -> (Float) -> Float): Float =
        cache.getOrPut("$cacheKey@$sampleRate") {
            val rng = Rng(CALIBRATION_SEED)
            val step = build()
            repeat((WARMUP_SECONDS * sampleRate).toInt()) { step(rng.nextBipolar()) }

            val n = (MEASURE_SECONDS * sampleRate).toInt()
            var sum = 0.0
            repeat(n) {
                val v = step(rng.nextBipolar())
                sum += v.toDouble() * v.toDouble()
            }
            val rms = sqrt(sum / n).toFloat()
            if (rms <= 0f || !rms.isFinite()) 1f else AudioFormat.GENERATOR_TARGET_RMS / rms
        }
}
