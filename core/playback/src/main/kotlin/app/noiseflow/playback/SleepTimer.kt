package app.noiseflow.playback

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * When the sound should stop, and how long it takes to get there.
 *
 * The fade is long on purpose. The timer exists so the app does not play all
 * night, but it must never be the thing that wakes someone up, so the sound
 * leaves over a minute rather than being cut.
 */
class SleepTimer(private val clock: () -> Long = System::currentTimeMillis) {

    sealed interface Mode {
        data object Off : Mode
        data class After(val duration: Duration) : Mode

        /** Fade out shortly before the system alarm clock goes off. */
        data class BeforeAlarm(val lead: Duration) : Mode
    }

    data class State(
        val mode: Mode = Mode.Off,
        val endsAtMillis: Long? = null,
    ) {
        fun remainingMillis(now: Long): Long? = endsAtMillis?.let { (it - now).coerceAtLeast(0) }
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    /** Fade length applied when the timer expires. */
    val fadeOutSeconds: Float = DEFAULT_FADE_OUT_SECONDS

    fun set(mode: Mode, nextAlarmMillis: Long? = null) {
        val now = clock()
        val endsAt = when (mode) {
            Mode.Off -> null
            is Mode.After -> now + mode.duration.inWholeMilliseconds
            is Mode.BeforeAlarm -> nextAlarmMillis?.minus(mode.lead.inWholeMilliseconds)
        }
        // An alarm that is already in the past, or missing entirely, means the
        // user asked for something we cannot honour. Falling back to "no timer"
        // is the safe direction: worst case the sound plays longer.
        _state.value = State(mode, endsAt?.takeIf { it > now })
    }

    fun clear() {
        _state.value = State()
    }

    fun hasExpired(): Boolean {
        val endsAt = _state.value.endsAtMillis ?: return false
        return clock() >= endsAt - (fadeOutSeconds * MILLIS_PER_SECOND).toLong()
    }

    companion object {
        val PRESETS = listOf(15.minutes, 30.minutes, 45.minutes, 60.minutes, 90.minutes)
        private const val DEFAULT_FADE_OUT_SECONDS = 60f
        private const val MILLIS_PER_SECOND = 1_000
    }
}
