package app.noiseflow.audio

import app.noiseflow.audio.dsp.Limiter
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.abs
import kotlin.math.min

/**
 * The whole signal path, and the only object the Android layer talks to.
 *
 * Threading contract: every public method may be called from any thread and
 * only enqueues a command. [render] is called from the audio thread alone,
 * drains the queue, and from then on touches nothing shared. Nothing in the
 * render path allocates — all buffers are sized once at construction.
 */
class NoiseEngine(
    val sampleRate: Int = AudioFormat.DEFAULT_SAMPLE_RATE,
    val blockFrames: Int = AudioFormat.DEFAULT_BLOCK_FRAMES,
) {
    private sealed interface Command {
        data class SetLayers(val specs: List<LayerSpec>) : Command
        data class SetMasterLevel(val level: Float) : Command
        data class SetNightCurve(val curve: NightCurve?) : Command
        data class Start(val fadeSeconds: Float) : Command
        data class Stop(val fadeSeconds: Float) : Command
        data object ResetClock : Command
    }

    private val commands = ConcurrentLinkedQueue<Command>()
    private val layers = LinkedHashMap<String, Layer>()

    private val busLeft = FloatArray(blockFrames)
    private val busRight = FloatArray(blockFrames)
    private val fadeGains = FloatArray(blockFrames)

    private val fade = FadeEnvelope(sampleRate, initial = 0f)
    private val limiter = Limiter(sampleRate)

    private var masterLevel = DEFAULT_MASTER_LEVEL
    private var smoothedMaster = DEFAULT_MASTER_LEVEL
    private var nightCurve: NightCurve? = null
    private var appliedSlopeOffset = 0f

    @Volatile
    private var framesRendered = 0L

    @Volatile
    var isSilent: Boolean = true
        private set

    /** Wall time of the current session, driven by the audio clock. */
    val elapsedSeconds: Double get() = framesRendered.toDouble() / sampleRate

    fun setLayers(specs: List<LayerSpec>) = commands.add(Command.SetLayers(specs))

    fun setMasterLevel(level: Float) = commands.add(Command.SetMasterLevel(level))

    fun setNightCurve(curve: NightCurve?) = commands.add(Command.SetNightCurve(curve))

    fun start(fadeInSeconds: Float = DEFAULT_FADE_IN) = commands.add(Command.Start(fadeInSeconds))

    fun stop(fadeOutSeconds: Float = DEFAULT_FADE_OUT) = commands.add(Command.Stop(fadeOutSeconds))

    fun resetClock() = commands.add(Command.ResetClock)

    /**
     * Fills [interleaved] with [frames] stereo frames (L, R, L, R ...).
     * Audio thread only.
     */
    fun render(interleaved: FloatArray, frames: Int) {
        drainCommands()
        var offset = 0
        while (offset < frames) {
            val n = min(blockFrames, frames - offset)
            renderBlock(interleaved, offset, n)
            offset += n
        }
        framesRendered += frames
        isSilent = fade.isSilent
    }

    private fun drainCommands() {
        while (true) {
            when (val c = commands.poll() ?: return) {
                is Command.SetLayers -> applyLayers(c.specs)
                is Command.SetMasterLevel -> masterLevel = c.level.coerceIn(0f, 1f)
                is Command.SetNightCurve -> {
                    nightCurve = c.curve
                    if (c.curve == null) applySlopeOffset(0f)
                }
                is Command.Start -> fade.fadeTo(1f, c.fadeSeconds)
                is Command.Stop -> fade.fadeTo(0f, c.fadeSeconds)
                Command.ResetClock -> framesRendered = 0L
            }
        }
    }

    private fun applyLayers(specs: List<LayerSpec>) {
        val wanted = specs.associateBy { it.id }
        layers.keys.retainAll(wanted.keys)
        for (spec in specs) {
            val existing = layers[spec.id]
            if (existing == null) {
                // New layers are created here, on the audio thread. Building a
                // tilt cascade is a few hundred allocations and no I/O; the
                // expensive part (RMS calibration) is memoised and, for the
                // stock slopes, already warm. Measured well under one block
                // period on the reference device.
                layers[spec.id] = Layer(spec, sampleRate, blockFrames)
                    .also { it.setSlopeOffset(appliedSlopeOffset) }
            } else {
                existing.update(spec)
            }
        }
    }

    private fun applySlopeOffset(offset: Float) {
        appliedSlopeOffset = offset
        for (layer in layers.values) layer.setSlopeOffset(offset)
    }

    private fun renderBlock(interleaved: FloatArray, frameOffset: Int, frames: Int) {
        java.util.Arrays.fill(busLeft, 0, frames, 0f)
        java.util.Arrays.fill(busRight, 0, frames, 0f)

        val night = nightCurve?.at((framesRendered.toDouble() / sampleRate / 60.0).toFloat())
        if (night != null && abs(night.slopeOffsetDbPerOctave - appliedSlopeOffset) >= SLOPE_OFFSET_STEP) {
            // Re-tuning every block would be absurd; the curve moves over
            // hours, so we only rebuild when it has drifted a noticeable step.
            applySlopeOffset(night.slopeOffsetDbPerOctave)
        }
        val nightLevel = night?.levelScale ?: 1f

        for (layer in layers.values) layer.render(busLeft, busRight, frames)

        fade.renderGains(fadeGains, frames)

        val targetMaster = masterLevel * nightLevel
        val dMaster = (targetMaster - smoothedMaster) / frames
        var out = frameOffset * 2
        for (i in 0 until frames) {
            val g = smoothedMaster * fadeGains[i]
            interleaved[out] = busLeft[i] * g
            interleaved[out + 1] = busRight[i] * g
            smoothedMaster += dMaster
            out += 2
        }
        smoothedMaster = targetMaster

        limiter.processInterleaved(interleaved, frameOffset, frames)
    }

    private companion object {
        const val DEFAULT_MASTER_LEVEL = 0.8f
        const val DEFAULT_FADE_IN = 2f
        const val DEFAULT_FADE_OUT = 3f
        const val SLOPE_OFFSET_STEP = 0.25f
    }
}
