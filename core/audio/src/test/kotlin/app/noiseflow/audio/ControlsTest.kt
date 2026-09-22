package app.noiseflow.audio

import app.noiseflow.audio.dsp.Limiter
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ControlsTest {

    private val sampleRate = AudioFormat.DEFAULT_SAMPLE_RATE

    @Test
    fun `limiter holds a loud signal under full scale`() {
        val limiter = Limiter(sampleRate)
        val frames = sampleRate
        val buffer = FloatArray(frames * 2)
        val rng = Rng(1L)
        for (i in buffer.indices) buffer[i] = rng.nextBipolar() * 3f
        limiter.processInterleaved(buffer, 0, frames)
        assertTrue(buffer.all { abs(it) <= 1f }, "limiter let a sample through above full scale")
        assertTrue(limiter.currentGain < 1f, "limiter never engaged on a 3x overload")
    }

    @Test
    fun `limiter leaves a quiet signal alone`() {
        val limiter = Limiter(sampleRate)
        val frames = sampleRate
        val buffer = FloatArray(frames * 2)
        val rng = Rng(2L)
        for (i in buffer.indices) buffer[i] = rng.nextBipolar() * 0.2f
        val before = buffer.copyOf()
        limiter.processInterleaved(buffer, 0, frames)
        for (i in buffer.indices) {
            assertTrue(abs(buffer[i] - before[i]) < 1e-4f, "limiter coloured a quiet signal")
        }
    }

    @Test
    fun `fade envelope is smooth and monotonic`() {
        val fade = FadeEnvelope(sampleRate)
        fade.fadeTo(1f, 1f)
        val gains = FloatArray(sampleRate)
        fade.renderGains(gains, sampleRate)
        assertTrue(gains.first() < 0.01f)
        assertTrue(gains.last() > 0.99f)
        for (i in 1 until gains.size) {
            assertTrue(gains[i] >= gains[i - 1] - 1e-6f, "fade went backwards at $i")
        }
    }

    @Test
    fun `fade envelope reports silence only when it gets there`() {
        val fade = FadeEnvelope(sampleRate, initial = 1f)
        fade.fadeTo(0f, 0.5f)
        val gains = FloatArray(sampleRate / 4)
        fade.renderGains(gains, gains.size)
        assertTrue(!fade.isSilent, "reported silent halfway through the fade")
        fade.renderGains(gains, gains.size)
        fade.renderGains(gains, gains.size)
        assertTrue(fade.isSilent, "never reported silent after the fade completed")
    }

    @Test
    fun `night curve interpolates between its key points`() {
        val curve = NightCurve.DEFAULT
        assertEquals(1f, curve.at(0f).levelScale, 1e-4f)
        assertEquals(1f, curve.at(25f).levelScale, 1e-4f)
        val midway = curve.at(37.5f).levelScale
        assertTrue(midway in 0.9f..1.0f, "midpoint was $midway")
        assertTrue(curve.at(600f).levelScale < curve.at(60f).levelScale)
        assertTrue(curve.at(600f).slopeOffsetDbPerOctave < 0f)
    }

    @Test
    fun `modulation stays within its depth and never inverts`() {
        val modulator = Modulator(ModulationShape.WAVES, sampleRate, seed = 4L).apply { depth = 0.6f }
        val gains = FloatArray(4096)
        var min = Float.MAX_VALUE
        var max = -Float.MAX_VALUE
        repeat(400) {
            modulator.renderGains(gains, gains.size)
            for (g in gains) {
                if (g < min) min = g
                if (g > max) max = g
            }
        }
        assertTrue(min >= 0.4f - 1e-3f, "modulation dipped to $min, below 1 - depth")
        assertTrue(max <= 1f + 1e-3f, "modulation peaked at $max, above unity")
        assertTrue(max - min > 0.4f, "modulation barely moved: $min..$max")
    }

    @Test
    fun `modulation disabled means constant gain`() {
        val modulator = Modulator(ModulationShape.NONE, sampleRate, seed = 4L).apply { depth = 0.9f }
        val gains = FloatArray(1024)
        modulator.renderGains(gains, gains.size)
        assertTrue(gains.all { it == 1f })
    }

    @Test
    fun `room control removes top end`() {
        val dry = app.noiseflow.audio.generators.TiltedNoise(0f, sampleRate, 1L)
            .renderSeconds(6.0, sampleRate)
        val room = app.noiseflow.audio.dsp.RoomFilter(sampleRate).apply { setAmount(1f) }
        val wet = FloatArray(dry.size) { room.process(dry[it]) }
        val dryHigh = Spectrum.bandLevelDb(dry, sampleRate, 8_000.0, segments = 32)
        val wetHigh = Spectrum.bandLevelDb(wet, sampleRate, 8_000.0, segments = 32)
        assertTrue(dryHigh - wetHigh > 20.0, "room only removed ${dryHigh - wetHigh} dB at 8 kHz")
    }
}
