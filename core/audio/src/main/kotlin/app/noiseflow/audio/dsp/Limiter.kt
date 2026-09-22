package app.noiseflow.audio.dsp

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max

/**
 * Last thing in the chain, and the one non-negotiable safety device: eight
 * layers plus modulation can constructively peak well past full scale, and a
 * clipped burst at 3am is exactly the failure this product cannot have.
 *
 * Feed-forward peak limiter with a hard clamp behind it. The clamp should
 * never engage; it exists so that a bug upstream degrades into a rounded peak
 * rather than a digital crack.
 */
class Limiter(
    sampleRate: Int,
    private val threshold: Float = DEFAULT_THRESHOLD,
) {
    private val attack = coefficient(ATTACK_SECONDS, sampleRate)
    private val release = coefficient(RELEASE_SECONDS, sampleRate)
    private var gain = 1f

    /** Peak reduction currently applied, for diagnostics and tests. */
    val currentGain: Float get() = gain

    fun processInterleaved(buffer: FloatArray, frameOffset: Int, frames: Int) {
        var i = frameOffset * 2
        repeat(frames) {
            val peak = max(abs(buffer[i]), abs(buffer[i + 1]))
            val desired = if (peak > threshold) threshold / peak else 1f
            gain += (desired - gain) * if (desired < gain) attack else release
            buffer[i] = (buffer[i] * gain).coerceIn(-1f, 1f)
            buffer[i + 1] = (buffer[i + 1] * gain).coerceIn(-1f, 1f)
            i += 2
        }
    }

    fun reset() {
        gain = 1f
    }

    private companion object {
        const val DEFAULT_THRESHOLD = 0.95f
        const val ATTACK_SECONDS = 0.005f
        const val RELEASE_SECONDS = 0.250f

        fun coefficient(seconds: Float, sampleRate: Int): Float =
            (1.0 - exp(-1.0 / (seconds * sampleRate))).toFloat()
    }
}
