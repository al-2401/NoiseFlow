package app.noiseflow.audio

import app.noiseflow.audio.dsp.RoomFilter
import app.noiseflow.audio.generators.MorphingSource
import app.noiseflow.audio.generators.SoundSpec
import app.noiseflow.audio.generators.createSource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** One sound in the mix, with everything the user can set on it. */
data class LayerSpec(
    val id: String,
    val sound: SoundSpec,
    val level: Float = 0.7f,
    /** -1 hard left .. +1 hard right. */
    val pan: Float = 0f,
    /**
     * 0 = identical in both ears, 1 = two independent noise streams.
     *
     * Defaults to fully decorrelated because mono noise on headphones
     * localises inside the listener's head and gets fatiguing over a night.
     */
    val width: Float = 1f,
    val modulation: ModulationShape = ModulationShape.NONE,
    val modulationDepth: Float = 0.35f,
    val room: Float = 0f,
    val seed: Long = id.hashCode().toLong() * 0x9E3779B1L,
)

/**
 * Renders one [LayerSpec] into the stereo bus.
 *
 * Two fully independent generators feed the two ears; [LayerSpec.width] then
 * rotates them towards mono with an equal-power matrix, so narrowing the image
 * never changes how loud the layer is.
 */
internal class Layer(
    spec: LayerSpec,
    private val sampleRate: Int,
    maxBlockFrames: Int,
) {
    var spec: LayerSpec = spec
        private set

    private var slopeOffset = 0f

    private val left = MorphingSource(
        spec.sound.createSource(sampleRate, spec.seed),
        sampleRate,
        maxBlockFrames,
    )
    private val right = MorphingSource(
        spec.sound.createSource(sampleRate, spec.seed xor DECORRELATION_SALT),
        sampleRate,
        maxBlockFrames,
    )

    private val roomLeft = RoomFilter(sampleRate).apply { setAmount(spec.room) }
    private val roomRight = RoomFilter(sampleRate).apply { setAmount(spec.room) }
    private val modulator = Modulator(spec.modulation, sampleRate, spec.seed xor MODULATION_SALT)
        .apply { depth = spec.modulationDepth }

    private val bufferLeft = FloatArray(maxBlockFrames)
    private val bufferRight = FloatArray(maxBlockFrames)
    private val modulationGains = FloatArray(maxBlockFrames)

    private var level = spec.level
    private var panLeft = panGainLeft(spec.pan)
    private var panRight = panGainRight(spec.pan)
    private var widthDirect = widthDirectFor(spec.width)
    private var widthCross = widthCrossFor(spec.width)

    fun update(next: LayerSpec) {
        if (next.sound != spec.sound) {
            morphTo(next.sound)
        }
        spec = next
        roomLeft.setAmount(next.room)
        roomRight.setAmount(next.room)
        modulator.depth = next.modulationDepth
    }

    /** Applied by the night curve; folded on top of the layer's own slope. */
    fun setSlopeOffset(offset: Float) {
        if (offset == slopeOffset) return
        slopeOffset = offset
        morphTo(spec.sound)
    }

    private fun morphTo(sound: SoundSpec) {
        val effective = when (sound) {
            is SoundSpec.Tilted -> SoundSpec.Tilted(sound.slopeDbPerOctave + slopeOffset)
            else -> sound
        }
        left.morphTo(effective.createSource(sampleRate, spec.seed))
        right.morphTo(effective.createSource(sampleRate, spec.seed xor DECORRELATION_SALT))
    }

    /** Adds this layer into the buses. Never allocates. */
    fun render(busLeft: FloatArray, busRight: FloatArray, frames: Int) {
        left.render(bufferLeft, frames)
        right.render(bufferRight, frames)
        modulator.renderGains(modulationGains, frames)

        val targetLevel = spec.level
        val targetPanLeft = panGainLeft(spec.pan)
        val targetPanRight = panGainRight(spec.pan)
        val targetDirect = widthDirectFor(spec.width)
        val targetCross = widthCrossFor(spec.width)

        val inverse = 1f / frames
        val dLevel = (targetLevel - level) * inverse
        val dPanLeft = (targetPanLeft - panLeft) * inverse
        val dPanRight = (targetPanRight - panRight) * inverse
        val dDirect = (targetDirect - widthDirect) * inverse
        val dCross = (targetCross - widthCross) * inverse

        for (i in 0 until frames) {
            val l = roomLeft.process(bufferLeft[i])
            val r = roomRight.process(bufferRight[i])
            val wl = widthDirect * l + widthCross * r
            val wr = widthCross * l + widthDirect * r
            val g = level * modulationGains[i]
            busLeft[i] += wl * panLeft * g
            busRight[i] += wr * panRight * g

            level += dLevel
            panLeft += dPanLeft
            panRight += dPanRight
            widthDirect += dDirect
            widthCross += dCross
        }

        level = targetLevel
        panLeft = targetPanLeft
        panRight = targetPanRight
        widthDirect = targetDirect
        widthCross = targetCross
    }

    fun reset() {
        left.reset()
        right.reset()
        roomLeft.reset()
        roomRight.reset()
        modulator.reset()
    }

    private companion object {
        const val DECORRELATION_SALT = 0x5DEADBEEFL
        const val MODULATION_SALT = 0x1C0FFEEL

        fun panGainLeft(pan: Float): Float = cos((pan.coerceIn(-1f, 1f) + 1f) * (PI.toFloat() / 4f))

        fun panGainRight(pan: Float): Float = sin((pan.coerceIn(-1f, 1f) + 1f) * (PI.toFloat() / 4f))

        /**
         * width=1 -> (1, 0), the two streams stay independent.
         * width=0 -> (0.707, 0.707), both ears get the same summed signal.
         * cos^2 + sin^2 = 1, so total power is identical at every setting.
         */
        fun widthDirectFor(width: Float): Float =
            cos((1f - width.coerceIn(0f, 1f)) * (PI.toFloat() / 4f))

        fun widthCrossFor(width: Float): Float =
            sin((1f - width.coerceIn(0f, 1f)) * (PI.toFloat() / 4f))
    }
}
