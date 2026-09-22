package app.noiseflow.audio.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.pow

/** RBJ cookbook biquad, direct form I. */
class Biquad private constructor(
    private val b0: Float,
    private val b1: Float,
    private val b2: Float,
    private val a1: Float,
    private val a2: Float,
) {
    private var x1 = 0f
    private var x2 = 0f
    private var y1 = 0f
    private var y2 = 0f

    fun process(x: Float): Float {
        val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2 = x1
        x1 = x
        y2 = y1
        y1 = y
        return y
    }

    fun reset() {
        x1 = 0f; x2 = 0f; y1 = 0f; y2 = 0f
    }

    companion object {
        fun peaking(freqHz: Float, q: Float, gainDb: Float, sampleRate: Int): Biquad {
            val a = 10.0.pow(gainDb / 40.0)
            val w0 = 2.0 * PI * freqHz / sampleRate
            val alpha = sin(w0) / (2.0 * q)
            return normalise(
                b0 = 1.0 + alpha * a,
                b1 = -2.0 * cos(w0),
                b2 = 1.0 - alpha * a,
                a0 = 1.0 + alpha / a,
                a1 = -2.0 * cos(w0),
                a2 = 1.0 - alpha / a,
            )
        }

        fun lowShelf(freqHz: Float, gainDb: Float, sampleRate: Int): Biquad {
            val a = 10.0.pow(gainDb / 40.0)
            val w0 = 2.0 * PI * freqHz / sampleRate
            val alpha = sin(w0) / 2.0 * sqrt(2.0)
            val twoSqrtAAlpha = 2.0 * sqrt(a) * alpha
            return normalise(
                b0 = a * ((a + 1) - (a - 1) * cos(w0) + twoSqrtAAlpha),
                b1 = 2 * a * ((a - 1) - (a + 1) * cos(w0)),
                b2 = a * ((a + 1) - (a - 1) * cos(w0) - twoSqrtAAlpha),
                a0 = (a + 1) + (a - 1) * cos(w0) + twoSqrtAAlpha,
                a1 = -2 * ((a - 1) + (a + 1) * cos(w0)),
                a2 = (a + 1) + (a - 1) * cos(w0) - twoSqrtAAlpha,
            )
        }

        fun highShelf(freqHz: Float, gainDb: Float, sampleRate: Int): Biquad {
            val a = 10.0.pow(gainDb / 40.0)
            val w0 = 2.0 * PI * freqHz / sampleRate
            val alpha = sin(w0) / 2.0 * sqrt(2.0)
            val twoSqrtAAlpha = 2.0 * sqrt(a) * alpha
            return normalise(
                b0 = a * ((a + 1) + (a - 1) * cos(w0) + twoSqrtAAlpha),
                b1 = -2 * a * ((a - 1) + (a + 1) * cos(w0)),
                b2 = a * ((a + 1) + (a - 1) * cos(w0) - twoSqrtAAlpha),
                a0 = (a + 1) - (a - 1) * cos(w0) + twoSqrtAAlpha,
                a1 = 2 * ((a - 1) - (a + 1) * cos(w0)),
                a2 = (a + 1) - (a - 1) * cos(w0) - twoSqrtAAlpha,
            )
        }

        /** Constant skirt gain band-pass. */
        fun bandPass(freqHz: Float, q: Float, sampleRate: Int): Biquad {
            val w0 = 2.0 * PI * freqHz / sampleRate
            val alpha = sin(w0) / (2.0 * q)
            return normalise(
                b0 = alpha,
                b1 = 0.0,
                b2 = -alpha,
                a0 = 1.0 + alpha,
                a1 = -2.0 * cos(w0),
                a2 = 1.0 - alpha,
            )
        }

        fun lowPass(freqHz: Float, q: Float, sampleRate: Int): Biquad {
            val w0 = 2.0 * PI * freqHz / sampleRate
            val alpha = sin(w0) / (2.0 * q)
            return normalise(
                b0 = (1.0 - cos(w0)) / 2.0,
                b1 = 1.0 - cos(w0),
                b2 = (1.0 - cos(w0)) / 2.0,
                a0 = 1.0 + alpha,
                a1 = -2.0 * cos(w0),
                a2 = 1.0 - alpha,
            )
        }

        private fun normalise(
            b0: Double,
            b1: Double,
            b2: Double,
            a0: Double,
            a1: Double,
            a2: Double,
        ) = Biquad(
            (b0 / a0).toFloat(),
            (b1 / a0).toFloat(),
            (b2 / a0).toFloat(),
            (a1 / a0).toFloat(),
            (a2 / a0).toFloat(),
        )
    }
}
