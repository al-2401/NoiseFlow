package app.noiseflow.audio.generators

import app.noiseflow.audio.dsp.TiltFilter

/** What a layer generates, independent of how loud or where it sits. */
sealed interface SoundSpec {
    /**
     * Any point on the Tone slider. The named colours are just labelled
     * positions on this axis, which is the whole premise of the product.
     */
    data class Tilted(val slopeDbPerOctave: Float) : SoundSpec

    /** Psychoacoustically flat: sounds equally loud across the spectrum. */
    data object Grey : SoundSpec

    /** Narrow band around 500 Hz, the "natural" mid-heavy hiss. */
    data object Green : SoundSpec
}

/** Preset positions surfaced in the UI as detents on the Tone slider. */
enum class NoiseType(val spec: SoundSpec) {
    BROWN(SoundSpec.Tilted(TiltFilter.BROWN)),
    PINK(SoundSpec.Tilted(TiltFilter.PINK)),
    WHITE(SoundSpec.Tilted(TiltFilter.WHITE)),
    BLUE(SoundSpec.Tilted(TiltFilter.BLUE)),
    VIOLET(SoundSpec.Tilted(TiltFilter.VIOLET)),
    GREY(SoundSpec.Grey),
    GREEN(SoundSpec.Green),
    ;

    companion object {
        /** Nearest named type for a free-form slope, for labelling the slider. */
        fun nearestTo(slopeDbPerOctave: Float): NoiseType =
            entries
                .filter { it.spec is SoundSpec.Tilted }
                .minBy { kotlin.math.abs((it.spec as SoundSpec.Tilted).slopeDbPerOctave - slopeDbPerOctave) }
    }
}
