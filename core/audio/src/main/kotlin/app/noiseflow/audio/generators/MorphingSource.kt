package app.noiseflow.audio.generators

import app.noiseflow.audio.NoiseSource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Swaps one source for another without a click.
 *
 * Dragging the Tone slider rebuilds the filter cascade, and a rebuilt filter
 * starts from zero state — audible as a thump. So we keep the old source alive
 * for a moment and cross-fade. The fade is equal-power (sin/cos): the two
 * streams are uncorrelated noise, so equal-power holds the perceived level
 * exactly constant, where a linear fade would dip in the middle.
 */
class MorphingSource(
    initial: NoiseSource,
    sampleRate: Int,
    maxBlockFrames: Int,
) : NoiseSource {
    private val fadeFrames: Int = (FADE_SECONDS * sampleRate).toInt().coerceAtLeast(1)
    private val scratch = FloatArray(maxBlockFrames)

    private var current: NoiseSource = initial
    private var previous: NoiseSource? = null
    private var fadePosition = 0

    val active: NoiseSource get() = current

    fun morphTo(next: NoiseSource) {
        // A second change mid-fade drops the oldest stream rather than
        // stacking fades; at 64ms nobody can hear the difference.
        previous = current
        current = next
        fadePosition = 0
    }

    override fun render(out: FloatArray, frames: Int) {
        current.render(out, frames)
        val old = previous ?: return

        old.render(scratch, frames)
        var pos = fadePosition
        for (i in 0 until frames) {
            if (pos >= fadeFrames) {
                // Remaining samples are pure new source; nothing to mix.
                break
            }
            val t = pos.toFloat() / fadeFrames
            val angle = t * (PI.toFloat() / 2f)
            out[i] = out[i] * sin(angle) + scratch[i] * cos(angle)
            pos++
        }
        fadePosition = pos
        if (fadePosition >= fadeFrames) previous = null
    }

    override fun reset() {
        current.reset()
        previous = null
        fadePosition = 0
    }

    private companion object {
        const val FADE_SECONDS = 0.064f
    }
}
