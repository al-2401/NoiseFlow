package app.noiseflow.audio.dsp

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Continuous spectral tilt: shapes white noise to an arbitrary slope in
 * dB/octave. This is the product's core differentiator — the Tone slider from
 * docs/01-concept.md is literally this filter's parameter.
 *
 * Construction: a cascade of first-order pole-zero sections, logarithmically
 * spaced across the audio band. Within one spacing interval a section falls at
 * -6 dB/oct from its pole to its zero and is flat from the zero to the next
 * pole. Putting the zero a fraction `alpha` of the interval above the pole
 * therefore averages out to -6*alpha dB/oct across the whole band. Swapping
 * pole and zero gives the same thing rising.
 *
 * Slopes past +-6 dB/oct need more than one section per interval, so the
 * cascade is repeated: two stages of alpha=1 give -12 dB/oct.
 */
class TiltFilter(
    val slopeDbPerOctave: Float,
    private val sampleRate: Int,
) {
    private val sections: Array<OnePole> = build()

    fun process(x: Float): Float {
        var v = x
        for (s in sections) v = s.process(v)
        return v
    }

    fun reset() {
        for (s in sections) s.reset()
    }

    val sectionCount: Int get() = sections.size

    private fun build(): Array<OnePole> {
        val slope = slopeDbPerOctave.coerceIn(MIN_SLOPE, MAX_SLOPE)
        if (abs(slope) < FLAT_EPSILON) return emptyArray()

        val fMax = min(F_MAX, sampleRate * NYQUIST_MARGIN)
        val octaves = log2(fMax / F_MIN)
        val perStage = ceil(abs(slope) / DB_PER_OCTAVE_PER_SECTION).roundToInt().coerceAtLeast(1)
        val alpha = (abs(slope) / DB_PER_OCTAVE_PER_SECTION) / perStage

        val count = (octaves * SECTIONS_PER_OCTAVE).roundToInt() + 1
        val rho = (fMax / F_MIN).toDouble().pow(1.0 / (count - 1))
        val shift = rho.pow(alpha.toDouble()).toFloat()
        val falling = slope < 0f

        val out = ArrayList<OnePole>(count * perStage)
        repeat(perStage) {
            for (k in 0 until count) {
                val base = (F_MIN * rho.pow(k.toDouble())).toFloat()
                if (falling) {
                    out += OnePole(poleHz = base, zeroHz = base * shift, sampleRate = sampleRate)
                } else {
                    out += OnePole(poleHz = base * shift, zeroHz = base, sampleRate = sampleRate)
                }
            }
        }
        return out.toTypedArray()
    }

    companion object {
        /** Deep rumble, below anything a phone speaker reproduces as pitch. */
        const val MIN_SLOPE = -12f

        /** Bright hiss. Past this it is painful rather than useful. */
        const val MAX_SLOPE = 6f

        /** Well-known slopes, used as labelled detents on the Tone slider. */
        const val BROWN = -6f
        const val PINK = -3f
        const val WHITE = 0f
        const val BLUE = 3f
        const val VIOLET = 6f

        private const val F_MIN = 20f
        private const val F_MAX = 20_000f
        private const val NYQUIST_MARGIN = 0.45f
        private const val SECTIONS_PER_OCTAVE = 2f
        private const val DB_PER_OCTAVE_PER_SECTION = 6f
        private const val FLAT_EPSILON = 0.01f

        @Suppress("unused")
        private val LN2 = ln(2.0)
    }
}
