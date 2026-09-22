package app.noiseflow.audio.dsp

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow

/**
 * "Through the wall" / "in the next room": two cascaded one-poles, -12 dB/oct.
 *
 * Not a reverb. A real room simulation costs far more CPU than it is worth for
 * a sound that runs for eight hours, and most of what makes noise feel distant
 * is simply the missing top end.
 *
 * The cutoff is smoothed per sample rather than switched, so moving the slider
 * sweeps instead of stepping.
 */
class RoomFilter(private val sampleRate: Int) {
    private var target = coefFor(0f)
    private var a = target
    private var y1 = 0f
    private var y2 = 0f

    fun setAmount(amount: Float) {
        target = coefFor(amount.coerceIn(0f, 1f))
    }

    fun process(x: Float): Float {
        a += (target - a) * SMOOTHING
        y1 += a * (x - y1)
        y2 += a * (y1 - y2)
        return y2
    }

    fun reset() {
        a = target
        y1 = 0f
        y2 = 0f
    }

    private fun coefFor(amount: Float): Float {
        val cutoff = OPEN_HZ * (CLOSED_HZ / OPEN_HZ).pow(amount)
        return (1.0 - exp(-2.0 * PI * cutoff / sampleRate)).toFloat().coerceIn(0f, 1f)
    }

    private companion object {
        const val OPEN_HZ = 18_000f
        const val CLOSED_HZ = 400f
        const val SMOOTHING = 0.0005f
    }
}
