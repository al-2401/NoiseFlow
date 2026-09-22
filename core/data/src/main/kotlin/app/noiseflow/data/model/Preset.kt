package app.noiseflow.data.model

import app.noiseflow.audio.LayerSpec
import app.noiseflow.audio.ModulationShape
import app.noiseflow.audio.generators.SoundSpec
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The persisted shape of a sound.
 *
 * Deliberately a separate type from [LayerSpec] rather than serialising the
 * engine's own model: the DSP will keep changing, and a saved preset from
 * version 1.0 has to keep working. This is the stable contract; the mapping
 * below absorbs the churn.
 */
@Serializable
sealed interface SoundConfig {
    @Serializable
    @SerialName("tilt")
    data class Tilt(val slopeDbPerOctave: Float) : SoundConfig

    @Serializable
    @SerialName("grey")
    data object Grey : SoundConfig

    @Serializable
    @SerialName("green")
    data object Green : SoundConfig
}

@Serializable
data class LayerConfig(
    val id: String,
    val sound: SoundConfig,
    val level: Float = 0.7f,
    val pan: Float = 0f,
    val width: Float = 1f,
    val modulation: ModulationConfig = ModulationConfig.NONE,
    val modulationDepth: Float = 0.35f,
    val room: Float = 0f,
)

@Serializable
enum class ModulationConfig { NONE, WAVES, BREATH, DRIFT }

@Serializable
data class Preset(
    val id: String,
    val name: String,
    val layers: List<LayerConfig>,
    /**
     * Factory presets are not editable in place; editing one saves a copy.
     * Keeps the shipped set intact and makes "restore defaults" trivial.
     */
    val isFactory: Boolean = false,
    val createdAtMillis: Long = 0L,
)

fun SoundConfig.toSpec(): SoundSpec = when (this) {
    is SoundConfig.Tilt -> SoundSpec.Tilted(slopeDbPerOctave)
    SoundConfig.Grey -> SoundSpec.Grey
    SoundConfig.Green -> SoundSpec.Green
}

fun SoundSpec.toConfig(): SoundConfig = when (this) {
    is SoundSpec.Tilted -> SoundConfig.Tilt(slopeDbPerOctave)
    SoundSpec.Grey -> SoundConfig.Grey
    SoundSpec.Green -> SoundConfig.Green
}

fun ModulationConfig.toShape(): ModulationShape = when (this) {
    ModulationConfig.NONE -> ModulationShape.NONE
    ModulationConfig.WAVES -> ModulationShape.WAVES
    ModulationConfig.BREATH -> ModulationShape.BREATH
    ModulationConfig.DRIFT -> ModulationShape.DRIFT
}

fun ModulationShape.toConfig(): ModulationConfig = when (this) {
    ModulationShape.NONE -> ModulationConfig.NONE
    ModulationShape.WAVES -> ModulationConfig.WAVES
    ModulationShape.BREATH -> ModulationConfig.BREATH
    ModulationShape.DRIFT -> ModulationConfig.DRIFT
}

fun LayerConfig.toSpec(): LayerSpec = LayerSpec(
    id = id,
    sound = sound.toSpec(),
    level = level,
    pan = pan,
    width = width,
    modulation = modulation.toShape(),
    modulationDepth = modulationDepth,
    room = room,
)

fun LayerSpec.toConfig(): LayerConfig = LayerConfig(
    id = id,
    sound = sound.toConfig(),
    level = level,
    pan = pan,
    width = width,
    modulation = modulation.toConfig(),
    modulationDepth = modulationDepth,
    room = room,
)

fun Preset.toSpecs(): List<LayerSpec> = layers.map { it.toSpec() }
