package app.noiseflow.i18n

import android.content.Context
import app.noiseflow.audio.dsp.TiltFilter
import app.noiseflow.audio.generators.NoiseType
import java.text.NumberFormat
import kotlin.math.abs

/**
 * Turns a spectral slope into words.
 *
 * Two problems solved here. First, nobody knows what "-4.5 dB/oct" means, so
 * every value is labelled with the nearest familiar colour plus a plain-words
 * hint. Second, the colour names themselves translate badly -- "brown noise"
 * is not a phrase in most languages -- so the rule from the localisation doc
 * is: keep the recognisable colour term and put the meaning underneath it.
 */
object ToneLabels {

    /** e.g. "Pink · softer" or, near a detent, just "Pink". */
    fun describe(context: Context, slopeDbPerOctave: Float): String {
        val nearest = NoiseType.nearestTo(slopeDbPerOctave)
        val name = context.getString(nearest.nameRes())
        val exact = detentSlope(nearest)
        val delta = slopeDbPerOctave - exact
        return when {
            abs(delta) < DETENT_TOLERANCE -> name
            delta < 0 -> context.getString(R.string.tone_deeper_than, name)
            else -> context.getString(R.string.tone_brighter_than, name)
        }
    }

    /** What a screen reader announces: the words plus the actual number. */
    fun accessibilityValue(context: Context, slopeDbPerOctave: Float): String {
        val number = NumberFormat.getNumberInstance().apply {
            maximumFractionDigits = 1
            minimumFractionDigits = 1
        }.format(slopeDbPerOctave)
        return context.getString(
            R.string.tone_accessibility,
            describe(context, slopeDbPerOctave),
            number,
        )
    }

    fun shortHint(context: Context, type: NoiseType): String = context.getString(type.hintRes())

    private fun detentSlope(type: NoiseType): Float = when (type) {
        NoiseType.BROWN -> TiltFilter.BROWN
        NoiseType.PINK -> TiltFilter.PINK
        NoiseType.WHITE -> TiltFilter.WHITE
        NoiseType.BLUE -> TiltFilter.BLUE
        NoiseType.VIOLET -> TiltFilter.VIOLET
        else -> TiltFilter.WHITE
    }

    private fun NoiseType.nameRes(): Int = when (this) {
        NoiseType.BROWN -> R.string.noise_brown
        NoiseType.PINK -> R.string.noise_pink
        NoiseType.WHITE -> R.string.noise_white
        NoiseType.BLUE -> R.string.noise_blue
        NoiseType.VIOLET -> R.string.noise_violet
        NoiseType.GREY -> R.string.noise_grey
        NoiseType.GREEN -> R.string.noise_green
    }

    private fun NoiseType.hintRes(): Int = when (this) {
        NoiseType.BROWN -> R.string.noise_brown_hint
        NoiseType.PINK -> R.string.noise_pink_hint
        NoiseType.WHITE -> R.string.noise_white_hint
        NoiseType.BLUE -> R.string.noise_blue_hint
        NoiseType.VIOLET -> R.string.noise_violet_hint
        NoiseType.GREY -> R.string.noise_grey_hint
        NoiseType.GREEN -> R.string.noise_green_hint
    }

    private const val DETENT_TOLERANCE = 0.4f
}
