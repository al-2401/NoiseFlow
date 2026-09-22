package app.noiseflow.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val skinId: String = "midnight",
    /** BCP-47 tag, or null to follow the system. Mirrors per-app language. */
    val localeTag: String? = null,
    val masterLevel: Float = 0.8f,
    val nightCurveEnabled: Boolean = false,
    val sleepTimerMinutes: Int? = null,
    val fadeBeforeAlarmMinutes: Int? = null,
    /**
     * Hearing safety. Caps the master level regardless of what the slider
     * says. On by default, because the people most affected -- parents running
     * this in a nursery all night -- are the least likely to go looking for
     * the setting.
     */
    val volumeCapEnabled: Boolean = true,
    val volumeCap: Float = 0.7f,
    val analyticsOptIn: Boolean = false,
    val onboardingComplete: Boolean = false,
)

@Serializable
data class AppState(
    /** Bumped whenever a migration is needed; see AppStateSerializer. */
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val settings: AppSettings = AppSettings(),
    val userPresets: List<Preset> = emptyList(),
    /** What was playing last, so the widget and cold start can resume it. */
    val lastMix: Preset? = null,
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}
