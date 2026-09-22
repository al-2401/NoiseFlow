package app.noiseflow.audio.dsp

import kotlin.math.PI

/**
 * y[n] = x[n] - x[n-1] + r*y[n-1].
 *
 * Brown noise puts most of its energy at the very bottom of the spectrum where
 * nothing can reproduce it; left alone it eats headroom and pushes speaker
 * cones around for no audible benefit. Always last in a generator chain.
 */
class DcBlocker(cutoffHz: Float = 18f, sampleRate: Int) {
    private val r: Float = (1.0 - 2.0 * PI * cutoffHz / sampleRate).toFloat()
    private var x1 = 0f
    private var y1 = 0f

    fun process(x: Float): Float {
        val y = x - x1 + r * y1
        x1 = x
        y1 = y
        return y
    }

    fun reset() {
        x1 = 0f
        y1 = 0f
    }
}
