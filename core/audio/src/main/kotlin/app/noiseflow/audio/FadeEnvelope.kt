package app.noiseflow.audio

import kotlin.math.PI
import kotlin.math.cos

/**
 * Raised-cosine level ramp.
 *
 * Used for start, stop and the sleep timer. A linear fade-out is audible as a
 * sudden "gone" at the end; a raised cosine tails off the way people expect
 * sound to disappear, which matters because the whole point of the sleep timer
 * is that it must not wake anyone up.
 */
class FadeEnvelope(private val sampleRate: Int, initial: Float = 0f) {
    private var from = initial
    private var to = initial
    private var position = 0
    private var length = 0

    val value: Float
        get() = if (length == 0) to else shaped(position.toFloat() / length)

    val isIdle: Boolean get() = length == 0

    /** True once a fade to silence has finished — the engine's stop signal. */
    val isSilent: Boolean get() = length == 0 && to <= 0f

    fun fadeTo(target: Float, seconds: Float) {
        from = value
        to = target.coerceIn(0f, 1f)
        length = (seconds * sampleRate).toInt().coerceAtLeast(0)
        position = 0
        if (length == 0) from = to
    }

    fun jumpTo(target: Float) {
        from = target
        to = target
        position = 0
        length = 0
    }

    fun renderGains(out: FloatArray, frames: Int) {
        if (length == 0) {
            java.util.Arrays.fill(out, 0, frames, to)
            return
        }
        for (i in 0 until frames) {
            out[i] = if (position >= length) to else shaped(position.toFloat() / length)
            position++
        }
        if (position >= length) {
            from = to
            length = 0
            position = 0
        }
    }

    private fun shaped(t: Float): Float {
        val curve = (0.5 - 0.5 * cos(PI * t.coerceIn(0f, 1f))).toFloat()
        return from + (to - from) * curve
    }
}
