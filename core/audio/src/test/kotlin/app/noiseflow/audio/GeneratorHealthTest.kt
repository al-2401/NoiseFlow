package app.noiseflow.audio

import app.noiseflow.audio.generators.GreenNoise
import app.noiseflow.audio.generators.GreyNoise
import app.noiseflow.audio.generators.MorphingSource
import app.noiseflow.audio.generators.SoundSpec
import app.noiseflow.audio.generators.TiltedNoise
import app.noiseflow.audio.generators.createSource
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeneratorHealthTest {

    private val sampleRate = AudioFormat.DEFAULT_SAMPLE_RATE

    @Test
    fun `every slope comes out at the same loudness`() {
        // Otherwise the Tone slider doubles as a volume control, which is the
        // worst possible bug in a device someone reaches for half asleep.
        val levels = listOf(-12f, -9f, -6f, -3f, 0f, 3f, 6f).map { slope ->
            slope to rms(TiltedNoise(slope, sampleRate, seed = 3L).renderSeconds(4.0, sampleRate))
        }
        for ((slope, level) in levels) {
            val errorDb = 20.0 * kotlin.math.log10(level / AudioFormat.GENERATOR_TARGET_RMS)
            assertTrue(
                abs(errorDb) < 1.5,
                "slope $slope is ${"%.2f".format(errorDb)} dB off the target level",
            )
        }
    }

    @Test
    fun `grey and green are levelled too`() {
        for (source in listOf(GreyNoise(sampleRate, 1L), GreenNoise(sampleRate, 2L))) {
            val level = rms(source.renderSeconds(4.0, sampleRate))
            val errorDb = 20.0 * kotlin.math.log10(level / AudioFormat.GENERATOR_TARGET_RMS)
            assertTrue(abs(errorDb) < 1.5, "${source::class.simpleName} off by $errorDb dB")
        }
    }

    @Test
    fun `no DC offset even at the deepest slope`() {
        val signal = TiltedNoise(-12f, sampleRate, seed = 11L).renderSeconds(6.0, sampleRate)
        val mean = signal.fold(0.0) { acc, v -> acc + v } / signal.size
        assertTrue(abs(mean) < 1e-3, "DC offset $mean would waste headroom and push cones")
    }

    @Test
    fun `output stays finite and in range`() {
        for (slope in listOf(-12f, -6f, 0f, 6f)) {
            val signal = TiltedNoise(slope, sampleRate, seed = 5L).renderSeconds(4.0, sampleRate)
            assertTrue(signal.all { it.isFinite() }, "non-finite sample at slope $slope")
            assertTrue(signal.all { abs(it) < 4f }, "wild peak at slope $slope")
        }
    }

    @Test
    fun `same seed gives the same audio`() {
        val a = TiltedNoise(-3f, sampleRate, seed = 99L).renderSeconds(0.5, sampleRate)
        val b = TiltedNoise(-3f, sampleRate, seed = 99L).renderSeconds(0.5, sampleRate)
        assertTrue(a.contentEquals(b), "generator is not deterministic; spectral tests would be flaky")
    }

    @Test
    fun `different seeds give different audio`() {
        val a = TiltedNoise(-3f, sampleRate, seed = 1L).renderSeconds(0.5, sampleRate)
        val b = TiltedNoise(-3f, sampleRate, seed = 2L).renderSeconds(0.5, sampleRate)
        assertTrue(!a.contentEquals(b))
    }

    @Test
    fun `morphing between slopes never drops the level`() {
        // A gap here is an audible click every time the Tone slider moves.
        val source = MorphingSource(
            SoundSpec.Tilted(-6f).createSource(sampleRate, 1L),
            sampleRate,
            maxBlockFrames = 2048,
        )
        val block = FloatArray(2048)
        source.render(block, 2048)
        source.morphTo(SoundSpec.Tilted(0f).createSource(sampleRate, 2L))

        var minWindowRms = Double.MAX_VALUE
        repeat(8) {
            source.render(block, 2048)
            var i = 0
            while (i + 256 <= 2048) {
                minWindowRms = minOf(minWindowRms, rms(block.copyOfRange(i, i + 256)))
                i += 256
            }
        }
        val floor = AudioFormat.GENERATOR_TARGET_RMS * 0.4
        assertTrue(minWindowRms > floor, "level dipped to $minWindowRms during the cross-fade")
    }

    @Test
    fun `nearest named type labels the slider correctly`() {
        assertEquals(app.noiseflow.audio.generators.NoiseType.PINK, app.noiseflow.audio.generators.NoiseType.nearestTo(-2.8f))
        assertEquals(app.noiseflow.audio.generators.NoiseType.BROWN, app.noiseflow.audio.generators.NoiseType.nearestTo(-7f))
        assertEquals(app.noiseflow.audio.generators.NoiseType.VIOLET, app.noiseflow.audio.generators.NoiseType.nearestTo(5.5f))
    }
}
