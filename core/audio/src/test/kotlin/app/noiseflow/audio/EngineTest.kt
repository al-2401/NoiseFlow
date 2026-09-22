package app.noiseflow.audio

import app.noiseflow.audio.generators.NoiseType
import app.noiseflow.audio.generators.SoundSpec
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EngineTest {

    private val sampleRate = AudioFormat.DEFAULT_SAMPLE_RATE

    private fun engineWith(layers: List<LayerSpec>, master: Float = 1f): NoiseEngine =
        NoiseEngine(sampleRate, blockFrames = 1024).apply {
            setLayers(layers)
            setMasterLevel(master)
            start(fadeInSeconds = 0.01f)
        }

    private fun NoiseEngine.renderSeconds(seconds: Double): FloatArray {
        val frames = (seconds * sampleRate).toInt()
        val buffer = FloatArray(frames * 2)
        render(buffer, frames)
        return buffer
    }

    @Test
    fun `eight layers at full level never clip`() {
        // The limiter is the one safety device that must hold under abuse.
        val layers = (0 until 8).map {
            LayerSpec(
                id = "layer$it",
                sound = SoundSpec.Tilted(-6f + it * 1.5f),
                level = 1f,
                modulation = ModulationShape.WAVES,
                modulationDepth = 0.5f,
            )
        }
        val out = engineWith(layers).renderSeconds(6.0)
        assertTrue(out.all { it.isFinite() }, "engine produced a non-finite sample")
        val peak = out.maxOf { abs(it) }
        assertTrue(peak <= 1f, "peak $peak exceeded full scale")
    }

    @Test
    fun `stop fades to true silence`() {
        val engine = engineWith(listOf(LayerSpec("a", NoiseType.PINK.spec)))
        engine.renderSeconds(1.0)
        engine.stop(fadeOutSeconds = 1f)
        val tail = engine.renderSeconds(3.0)
        val lastSecond = tail.copyOfRange(tail.size - sampleRate * 2, tail.size)
        assertTrue(lastSecond.all { abs(it) < 1e-6f }, "audio continued after the fade-out")
        assertTrue(engine.isSilent, "engine did not report itself silent")
    }

    @Test
    fun `fade in ramps rather than jumps`() {
        val engine = NoiseEngine(sampleRate, blockFrames = 1024).apply {
            setLayers(listOf(LayerSpec("a", NoiseType.BROWN.spec, level = 1f)))
            setMasterLevel(1f)
            start(fadeInSeconds = 2f)
        }
        val out = engine.renderSeconds(2.0)
        val firstChunk = rms(out.copyOfRange(0, sampleRate / 10))
        val lastChunk = rms(out.copyOfRange(out.size - sampleRate / 10, out.size))
        assertTrue(firstChunk < lastChunk * 0.2, "start was not a fade: $firstChunk vs $lastChunk")
    }

    @Test
    fun `changing a layer level does not restart the noise`() {
        val engine = engineWith(listOf(LayerSpec("a", NoiseType.WHITE.spec, level = 0.3f)))
        engine.renderSeconds(1.0)
        engine.setLayers(listOf(LayerSpec("a", NoiseType.WHITE.spec, level = 0.9f)))
        val out = engine.renderSeconds(1.0)
        // A restart would show up as a gap while the new filter state settles.
        val firstWindow = rms(out.copyOfRange(0, 1024))
        assertTrue(firstWindow > 0.01, "level change caused a dropout")
    }

    @Test
    fun `removing a layer removes its sound`() {
        val engine = engineWith(
            listOf(
                LayerSpec("a", NoiseType.WHITE.spec, level = 1f),
                LayerSpec("b", NoiseType.BROWN.spec, level = 1f),
            ),
        )
        val both = rms(engine.renderSeconds(1.0))
        engine.setLayers(listOf(LayerSpec("a", NoiseType.WHITE.spec, level = 1f)))
        val one = rms(engine.renderSeconds(1.0))
        assertTrue(one < both, "dropping a layer did not reduce the level ($one vs $both)")
    }

    @Test
    fun `the audio clock tracks rendered frames`() {
        val engine = engineWith(listOf(LayerSpec("a", NoiseType.PINK.spec)))
        engine.renderSeconds(3.0)
        assertEquals(3.0, engine.elapsedSeconds, 0.01)
    }

    @Test
    fun `engine output is deterministic for a given set of layers`() {
        val specs = listOf(LayerSpec("a", NoiseType.PINK.spec, level = 0.8f))
        val a = engineWith(specs).renderSeconds(0.5)
        val b = engineWith(specs).renderSeconds(0.5)
        assertTrue(a.contentEquals(b))
    }

    @Test
    fun `night curve quietens the mix over the night`() {
        val specs = listOf(LayerSpec("a", NoiseType.PINK.spec, level = 1f))
        val engine = NoiseEngine(sampleRate, blockFrames = 1024).apply {
            setLayers(specs)
            setMasterLevel(1f)
            setNightCurve(NightCurve.DEFAULT)
            start(fadeInSeconds = 0.01f)
        }
        val early = rms(engine.renderSeconds(2.0))
        // Skip ahead by rendering into a throwaway buffer for two hours of audio.
        val skip = FloatArray(1024 * 2)
        repeat((2 * 3600 * sampleRate) / 1024) { engine.render(skip, 1024) }
        val late = rms(engine.renderSeconds(2.0))
        assertTrue(late < early * 0.95, "night curve did not reduce level: $early -> $late")
    }
}
