package app.noiseflow.audio.dsp

import kotlin.math.exp
import kotlin.math.PI

/**
 * First-order pole-zero section: H(z) = g * (1 - zc*z^-1) / (1 - pc*z^-1).
 *
 * The building block of [TiltFilter]. Normalised so the flat side of the
 * shelf has unity gain, which keeps a long cascade numerically sane.
 */
class OnePole(poleHz: Float, zeroHz: Float, sampleRate: Int) {
    private val pc: Float = coef(poleHz, sampleRate)
    private val zc: Float = coef(zeroHz, sampleRate)

    /**
     * Normalise at DC when the section falls (zero above pole) and at Nyquist
     * when it rises, so the section never has gain above 1 anywhere. Without
     * this a 20-section rising cascade overflows long before it is audible.
     */
    private val g: Float = if (zeroHz > poleHz) {
        (1f - pc) / (1f - zc)
    } else {
        (1f + pc) / (1f + zc)
    }

    private var x1 = 0f
    private var y1 = 0f

    fun process(x: Float): Float {
        val y = g * (x - zc * x1) + pc * y1
        x1 = x
        y1 = y
        return y
    }

    fun reset() {
        x1 = 0f
        y1 = 0f
    }

    private companion object {
        fun coef(hz: Float, sampleRate: Int): Float =
            exp(-2.0 * PI * hz / sampleRate).toFloat()
    }
}
