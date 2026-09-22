package app.noiseflow.i18n

import android.content.Context
import java.util.concurrent.TimeUnit

/**
 * Durations, pluralised properly.
 *
 * Russian, Ukrainian and Polish have three plural forms and Arabic has six, so
 * every one of these goes through a `plurals` resource. String concatenation
 * with a number is banned in this codebase for exactly this reason.
 */
object DurationLabels {

    /** "45 minutes" / "1 hour 5 minutes", for the sleep timer. */
    fun remaining(context: Context, millis: Long): String {
        val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(millis).coerceAtLeast(0)
        val hours = totalMinutes / MINUTES_PER_HOUR
        val minutes = totalMinutes % MINUTES_PER_HOUR
        return when {
            hours == 0L -> context.resources.getQuantityString(
                R.plurals.duration_minutes,
                minutes.toInt(),
                minutes.toInt(),
            )
            minutes == 0L -> context.resources.getQuantityString(
                R.plurals.duration_hours,
                hours.toInt(),
                hours.toInt(),
            )
            else -> context.getString(
                R.string.duration_hours_minutes,
                context.resources.getQuantityString(R.plurals.duration_hours, hours.toInt(), hours.toInt()),
                context.resources.getQuantityString(R.plurals.duration_minutes, minutes.toInt(), minutes.toInt()),
            )
        }
    }

    fun minutes(context: Context, minutes: Int): String =
        context.resources.getQuantityString(R.plurals.duration_minutes, minutes, minutes)

    private const val MINUTES_PER_HOUR = 60L
}
