package app.noiseflow.audio

/**
 * How the sound should change over a night.
 *
 * Masking needs to be loudest while falling asleep and can ease off once sleep
 * is established; the spectrum also wants to get darker, because high
 * frequencies are what pull people back out of light sleep. Endel does
 * something similar from biometrics and a network connection — this does it
 * from a clock, offline, which is both honest and enough.
 */
class NightCurve(private val points: List<Point>) {

    data class Point(
        val atMinutes: Float,
        val levelScale: Float,
        val slopeOffsetDbPerOctave: Float,
    )

    data class State(val levelScale: Float, val slopeOffsetDbPerOctave: Float)

    fun at(elapsedMinutes: Float): State {
        if (points.isEmpty()) return FLAT
        if (elapsedMinutes <= points.first().atMinutes) {
            return State(points.first().levelScale, points.first().slopeOffsetDbPerOctave)
        }
        for (i in 0 until points.lastIndex) {
            val a = points[i]
            val b = points[i + 1]
            if (elapsedMinutes <= b.atMinutes) {
                val span = (b.atMinutes - a.atMinutes).coerceAtLeast(EPSILON)
                val t = ((elapsedMinutes - a.atMinutes) / span).coerceIn(0f, 1f)
                return State(
                    levelScale = a.levelScale + (b.levelScale - a.levelScale) * t,
                    slopeOffsetDbPerOctave = a.slopeOffsetDbPerOctave +
                        (b.slopeOffsetDbPerOctave - a.slopeOffsetDbPerOctave) * t,
                )
            }
        }
        val last = points.last()
        return State(last.levelScale, last.slopeOffsetDbPerOctave)
    }

    companion object {
        private const val EPSILON = 0.001f
        private val FLAT = State(1f, 0f)

        /** Full level while falling asleep, then gradually softer and darker. */
        val DEFAULT = NightCurve(
            listOf(
                Point(atMinutes = 0f, levelScale = 1f, slopeOffsetDbPerOctave = 0f),
                Point(atMinutes = 25f, levelScale = 1f, slopeOffsetDbPerOctave = 0f),
                Point(atMinutes = 50f, levelScale = 0.85f, slopeOffsetDbPerOctave = -1f),
                Point(atMinutes = 120f, levelScale = 0.72f, slopeOffsetDbPerOctave = -2f),
                Point(atMinutes = 420f, levelScale = 0.65f, slopeOffsetDbPerOctave = -2.5f),
            ),
        )
    }
}
