package app.noiseflow.audio.generators

import app.noiseflow.audio.NoiseSource
import app.noiseflow.audio.Rng
import app.noiseflow.audio.dsp.Biquad
import app.noiseflow.audio.dsp.DcBlocker
import app.noiseflow.audio.dsp.TiltFilter

/** White noise shaped to an arbitrary spectral slope. The workhorse. */
class TiltedNoise(
    slopeDbPerOctave: Float,
    private val sampleRate: Int,
    seed: Long,
) : NoiseSource {
    private val rng = Rng(seed)
    private val tilt = TiltFilter(slopeDbPerOctave, sampleRate)
    private val dc = DcBlocker(sampleRate = sampleRate)
    private val gain = TiltCalibration.gainFor(slopeDbPerOctave, sampleRate)

    override fun render(out: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            out[i] = gain * dc.process(tilt.process(rng.nextBipolar()))
        }
    }

    override fun reset() {
        tilt.reset()
        dc.reset()
    }
}

/**
 * Grey noise: white weighted by the inverse of an equal-loudness contour, so
 * every band *sounds* equally present. This is a three-band approximation of
 * the inverse 60-phon curve, not the real ISO 226 table — the error is a
 * couple of dB, which is inaudible for masking noise and costs three biquads
 * instead of a filter bank.
 */
class GreyNoise(private val sampleRate: Int, seed: Long) : NoiseSource {
    private val rng = Rng(seed)
    private val low = Biquad.lowShelf(LOW_SHELF_HZ, LOW_SHELF_DB, sampleRate)
    private val dip = Biquad.peaking(DIP_HZ, DIP_Q, DIP_DB, sampleRate)
    private val high = Biquad.highShelf(HIGH_SHELF_HZ, HIGH_SHELF_DB, sampleRate)
    private val dc = DcBlocker(sampleRate = sampleRate)
    private val gain = RmsCalibration.gain("grey", sampleRate) { freshChain(sampleRate) }

    override fun render(out: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            val x = rng.nextBipolar()
            out[i] = gain * dc.process(high.process(dip.process(low.process(x))))
        }
    }

    override fun reset() {
        low.reset(); dip.reset(); high.reset(); dc.reset()
    }

    private companion object {
        const val LOW_SHELF_HZ = 250f
        const val LOW_SHELF_DB = 12f
        const val DIP_HZ = 3_500f
        const val DIP_Q = 1.0f
        const val DIP_DB = -9f
        const val HIGH_SHELF_HZ = 9_000f
        const val HIGH_SHELF_DB = 6f

        fun freshChain(sampleRate: Int): (Float) -> Float {
            val low = Biquad.lowShelf(LOW_SHELF_HZ, LOW_SHELF_DB, sampleRate)
            val dip = Biquad.peaking(DIP_HZ, DIP_Q, DIP_DB, sampleRate)
            val high = Biquad.highShelf(HIGH_SHELF_HZ, HIGH_SHELF_DB, sampleRate)
            val dc = DcBlocker(sampleRate = sampleRate)
            return { x -> dc.process(high.process(dip.process(low.process(x)))) }
        }
    }
}

/** Green noise: mid-band emphasis around 500 Hz. Two cascaded band-passes. */
class GreenNoise(private val sampleRate: Int, seed: Long) : NoiseSource {
    private val rng = Rng(seed)
    private val bp1 = Biquad.bandPass(CENTRE_HZ, Q, sampleRate)
    private val bp2 = Biquad.bandPass(CENTRE_HZ, Q, sampleRate)
    private val gain = RmsCalibration.gain("green", sampleRate) { freshChain(sampleRate) }

    override fun render(out: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            out[i] = gain * bp2.process(bp1.process(rng.nextBipolar()))
        }
    }

    override fun reset() {
        bp1.reset(); bp2.reset()
    }

    private companion object {
        const val CENTRE_HZ = 500f
        const val Q = 0.8f

        fun freshChain(sampleRate: Int): (Float) -> Float {
            val a = Biquad.bandPass(CENTRE_HZ, Q, sampleRate)
            val b = Biquad.bandPass(CENTRE_HZ, Q, sampleRate)
            return { x -> b.process(a.process(x)) }
        }
    }
}

/** Builds the source for a [SoundSpec]. */
fun SoundSpec.createSource(sampleRate: Int, seed: Long): NoiseSource = when (this) {
    is SoundSpec.Tilted -> TiltedNoise(slopeDbPerOctave, sampleRate, seed)
    SoundSpec.Grey -> GreyNoise(sampleRate, seed)
    SoundSpec.Green -> GreenNoise(sampleRate, seed)
}
