package app.noiseflow.audio

import kotlin.math.PI
import kotlin.math.cos

/** Slow amplitude movement that keeps a static noise bed from feeling dead. */
enum class ModulationShape(val rateHz: Float) {
    NONE(0f),

    /** ~14 s swell. Reads as surf without needing a surf recording. */
    WAVES(0.07f),

    /** 6 cycles per minute, the rate people are told to breathe at to relax. */
    BREATH(0.1f),

    /** Aimless slow wander. Least noticeable, best for all-night use. */
    DRIFT(0.02f),
}

/**
 * Produces a per-sample gain envelope.
 *
 * The oscillator runs at block rate and is linearly interpolated across the
 * block. At 0.02-0.1 Hz against a 46 ms block that is exact to far below
 * audibility, and it keeps a transcendental function out of the sample loop.
 */
class Modulator(
    private val shape: ModulationShape,
    private val sampleRate: Int,
    seed: Long,
) {
    var depth: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
        }

    private val rng = Rng(seed)
    private var phase = 0.0
    private var driftCurrent = 0.5f
    private var driftTarget = 0.5f
    private var driftFramesLeft = 0
    private var lastGain = 1f

    fun renderGains(out: FloatArray, frames: Int) {
        if (shape == ModulationShape.NONE || depth <= 0f) {
            java.util.Arrays.fill(out, 0, frames, 1f)
            lastGain = 1f
            return
        }
        val next = advance(frames)
        val step = (next - lastGain) / frames
        var g = lastGain
        for (i in 0 until frames) {
            out[i] = g
            g += step
        }
        lastGain = next
    }

    fun reset() {
        phase = 0.0
        lastGain = 1f
        driftCurrent = 0.5f
        driftTarget = 0.5f
        driftFramesLeft = 0
    }

    private fun advance(frames: Int): Float {
        val unipolar = when (shape) {
            ModulationShape.DRIFT -> advanceDrift(frames)
            else -> {
                phase = (phase + shape.rateHz.toDouble() * frames / sampleRate) % 1.0
                (0.5 - 0.5 * cos(2.0 * PI * phase)).toFloat()
            }
        }
        return 1f - depth + depth * unipolar
    }

    private fun advanceDrift(frames: Int): Float {
        driftFramesLeft -= frames
        if (driftFramesLeft <= 0) {
            driftTarget = (rng.nextBipolar() * 0.5f + 0.5f).coerceIn(0f, 1f)
            driftFramesLeft = (DRIFT_SEGMENT_SECONDS * sampleRate).toInt()
        }
        driftCurrent += (driftTarget - driftCurrent) * DRIFT_SMOOTHING
        return driftCurrent
    }

    private companion object {
        const val DRIFT_SEGMENT_SECONDS = 8f
        const val DRIFT_SMOOTHING = 0.15f
    }
}
