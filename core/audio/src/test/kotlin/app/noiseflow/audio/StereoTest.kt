package app.noiseflow.audio

import app.noiseflow.audio.generators.SoundSpec
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertTrue

class StereoTest {

    private val sampleRate = AudioFormat.DEFAULT_SAMPLE_RATE
    private val blockFrames = 2048

    private fun correlationAtWidth(width: Float): Double {
        val layer = Layer(
            LayerSpec("test", SoundSpec.Tilted(-3f), level = 1f, width = width),
            sampleRate,
            blockFrames,
        )
        val l = FloatArray(blockFrames)
        val r = FloatArray(blockFrames)
        var sumLR = 0.0
        var sumLL = 0.0
        var sumRR = 0.0
        repeat(40) {
            java.util.Arrays.fill(l, 0f)
            java.util.Arrays.fill(r, 0f)
            layer.render(l, r, blockFrames)
            for (i in 0 until blockFrames) {
                sumLR += l[i].toDouble() * r[i]
                sumLL += l[i].toDouble() * l[i]
                sumRR += r[i].toDouble() * r[i]
            }
        }
        return sumLR / sqrt(sumLL * sumRR)
    }

    @Test
    fun `width zero is mono and width one is fully decorrelated`() {
        val mono = correlationAtWidth(0f)
        val wide = correlationAtWidth(1f)
        assertTrue(mono > 0.99, "width=0 should be identical in both ears, correlation was $mono")
        assertTrue(abs(wide) < 0.05, "width=1 should be independent streams, correlation was $wide")
    }

    @Test
    fun `narrowing the image does not change the level`() {
        // Equal-power width matrix: collapsing to mono must not sound louder.
        fun power(width: Float): Double {
            val layer = Layer(
                LayerSpec("p", SoundSpec.Tilted(-3f), level = 1f, width = width),
                sampleRate,
                blockFrames,
            )
            val l = FloatArray(blockFrames)
            val r = FloatArray(blockFrames)
            var sum = 0.0
            repeat(40) {
                java.util.Arrays.fill(l, 0f)
                java.util.Arrays.fill(r, 0f)
                layer.render(l, r, blockFrames)
                for (i in 0 until blockFrames) {
                    sum += l[i].toDouble() * l[i] + r[i].toDouble() * r[i]
                }
            }
            return sum
        }
        val ratioDb = 10.0 * kotlin.math.log10(power(0f) / power(1f))
        assertTrue(abs(ratioDb) < 0.5, "mono vs wide differed by $ratioDb dB")
    }

    @Test
    fun `panning moves energy between channels`() {
        val layer = Layer(
            LayerSpec("pan", SoundSpec.Tilted(0f), level = 1f, pan = -1f),
            sampleRate,
            blockFrames,
        )
        val l = FloatArray(blockFrames)
        val r = FloatArray(blockFrames)
        var el = 0.0
        var er = 0.0
        repeat(20) {
            java.util.Arrays.fill(l, 0f)
            java.util.Arrays.fill(r, 0f)
            layer.render(l, r, blockFrames)
            for (i in 0 until blockFrames) {
                el += l[i].toDouble() * l[i]
                er += r[i].toDouble() * r[i]
            }
        }
        assertTrue(er / el < 0.01, "hard left still put ${er / el} of its energy on the right")
    }
}
