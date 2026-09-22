package app.noiseflow.data.model

import app.noiseflow.audio.dsp.TiltFilter

/**
 * The presets shipped with the app.
 *
 * Named after situations rather than after noise colours, because nobody
 * reaches for "-4.5 dB/oct" at midnight -- they reach for "upstairs neighbour".
 * The colour is what the engine does; the situation is what the user has.
 */
object FactoryPresets {

    const val UPSTAIRS_NEIGHBOUR = "factory.neighbour"
    const val AEROPLANE = "factory.aeroplane"
    const val NURSERY = "factory.nursery"
    const val RAINFALL = "factory.rainfall"
    const val DEEP = "factory.deep"
    const val FOCUS = "factory.focus"
    const val SNORING = "factory.snoring"
    const val TRAFFIC = "factory.traffic"

    /**
     * Names are resolved from string resources at display time; the ids here
     * are what gets persisted, so renaming a preset never orphans a saved mix.
     */
    val all: List<Preset> = listOf(
        preset(
            UPSTAIRS_NEIGHBOUR,
            // Broadband with a mid lift: footsteps and voices live at 300-3k.
            layer("base", SoundConfig.Tilt(-4f), level = 0.8f),
            layer("body", SoundConfig.Green, level = 0.35f, room = 0.3f),
        ),
        preset(
            AEROPLANE,
            layer("hull", SoundConfig.Tilt(TiltFilter.BROWN), level = 0.85f, room = 0.45f),
            layer("air", SoundConfig.Tilt(-1.5f), level = 0.25f),
        ),
        preset(
            NURSERY,
            // Pink at a modest level: the classic recommendation, kept quiet
            // on purpose, with no top end to startle a sleeping infant.
            layer("pink", SoundConfig.Tilt(TiltFilter.PINK), level = 0.55f, room = 0.25f),
        ),
        preset(
            RAINFALL,
            layer("drizzle", SoundConfig.Tilt(0.5f), level = 0.45f, modulation = ModulationConfig.DRIFT),
            layer("patter", SoundConfig.Tilt(-2f), level = 0.5f, modulation = ModulationConfig.WAVES),
        ),
        preset(
            DEEP,
            layer("rumble", SoundConfig.Tilt(-9f), level = 0.9f),
        ),
        preset(
            FOCUS,
            layer("flat", SoundConfig.Grey, level = 0.6f),
        ),
        preset(
            SNORING,
            // Snoring is low and periodic; brown masks the fundamental, the
            // slow swell keeps the bed from feeling like a machine room.
            layer("mask", SoundConfig.Tilt(-7f), level = 0.85f, modulation = ModulationConfig.BREATH),
            layer("veil", SoundConfig.Tilt(-2f), level = 0.3f),
        ),
        preset(
            TRAFFIC,
            layer("road", SoundConfig.Tilt(-5f), level = 0.75f, modulation = ModulationConfig.DRIFT, room = 0.2f),
        ),
    )

    fun byId(id: String): Preset? = all.firstOrNull { it.id == id }

    private fun preset(id: String, vararg layers: LayerConfig) =
        Preset(id = id, name = id, layers = layers.toList(), isFactory = true)

    private fun layer(
        id: String,
        sound: SoundConfig,
        level: Float,
        modulation: ModulationConfig = ModulationConfig.NONE,
        room: Float = 0f,
    ) = LayerConfig(
        id = id,
        sound = sound,
        level = level,
        modulation = modulation,
        modulationDepth = if (modulation == ModulationConfig.NONE) 0f else 0.3f,
        room = room,
    )
}
