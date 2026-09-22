package app.noiseflow.audio

import app.noiseflow.audio.generators.GreenNoise
import app.noiseflow.audio.generators.GreyNoise
import app.noiseflow.audio.generators.TiltedNoise
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The product's central claim, under test: the Tone control is a real
 * spectral slope, continuous, and accurate at every point — not seven
 * hard-coded presets with colour names.
 */
class SpectralSlopeTest {

    private val sampleRate = AudioFormat.DEFAULT_SAMPLE_RATE
    private val tolerance = 0.5

    private fun measure(slope: Float): Double {
        val signal = TiltedNoise(slope, sampleRate, seed = 42L).renderSeconds(14.0, sampleRate)
        return Spectrum.slopeDbPerOctave(signal, sampleRate)
    }

    @Test
    fun `named noise colours hit their textbook slopes`() {
        for (target in listOf(-6f, -3f, 0f, 3f, 6f)) {
            val measured = measure(target)
            assertTrue(
                abs(measured - target) < tolerance,
                "slope $target dB/oct measured as ${"%.2f".format(measured)}",
            )
        }
    }

    @Test
    fun `slopes between the named colours are just as accurate`() {
        // If these fail, the Tone slider is a lie and the product has no story.
        for (target in listOf(-10f, -7.5f, -4.5f, -1.5f, 1.5f, 4.5f)) {
            val measured = measure(target)
            assertTrue(
                abs(measured - target) < tolerance,
                "slope $target dB/oct measured as ${"%.2f".format(measured)}",
            )
        }
    }

    @Test
    fun `slope is monotonic across the whole Tone range`() {
        var previous = Double.NEGATIVE_INFINITY
        var slope = -12f
        while (slope <= 6f) {
            val measured = measure(slope)
            assertTrue(measured > previous, "slope $slope did not increase (got $measured)")
            previous = measured
            slope += 2f
        }
    }

    @Test
    fun `grey noise is loudest where the ear is least sensitive`() {
        val signal = GreyNoise(sampleRate, seed = 7L).renderSeconds(14.0, sampleRate)
        val low = Spectrum.bandLevelDb(signal, sampleRate, 100.0)
        val mid = Spectrum.bandLevelDb(signal, sampleRate, 3_500.0)
        val high = Spectrum.bandLevelDb(signal, sampleRate, 12_000.0)
        assertTrue(low - mid > 8.0, "expected a dip at 3.5 kHz, got ${low - mid} dB below 100 Hz")
        assertTrue(high - mid > 3.0, "expected top-end lift, got ${high - mid} dB above 3.5 kHz")
    }

    @Test
    fun `green noise sits around 500 Hz`() {
        val signal = GreenNoise(sampleRate, seed = 9L).renderSeconds(14.0, sampleRate)
        val peak = Spectrum.peakBandHz(signal, sampleRate)
        assertTrue(peak in 315.0..800.0, "green noise peaked at $peak Hz")
    }
}
