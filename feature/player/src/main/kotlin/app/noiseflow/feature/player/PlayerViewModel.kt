package app.noiseflow.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.noiseflow.audio.generators.SoundSpec
import app.noiseflow.billing.BillingGateway
import app.noiseflow.billing.FeatureGate
import app.noiseflow.data.model.AppSettings
import app.noiseflow.data.model.FactoryPresets
import app.noiseflow.data.model.Preset
import app.noiseflow.data.model.toConfig
import app.noiseflow.data.model.toSpecs
import app.noiseflow.data.store.AppStateRepository
import app.noiseflow.playback.PlaybackController
import app.noiseflow.playback.SleepTimer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val toneSlope: Float = DEFAULT_SLOPE,
    val masterLevel: Float = 0.8f,
    val volumeCap: Float = 1f,
    val timerRemainingMillis: Long? = null,
    val presetId: String? = null,
    val skinId: String = "midnight",
    val isPro: Boolean = false,
    val nightCurveEnabled: Boolean = false,
    val hasSound: Boolean = false,
) {
    /** The level actually sent to the engine, after the hearing-safety cap. */
    val effectiveLevel: Float get() = minOf(masterLevel, volumeCap)

    companion object {
        const val DEFAULT_SLOPE = -3f
    }
}

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playback: PlaybackController,
    private val repository: AppStateRepository,
    billing: BillingGateway,
) : ViewModel() {

    val state: StateFlow<PlayerUiState> = combine(
        playback.state,
        repository.state,
        playback.sleepTimer.state,
        billing.entitlement,
    ) { play, app, timer, entitlement ->
        PlayerUiState(
            isPlaying = play.isPlaying,
            toneSlope = play.layers.firstNotNullOfOrNull {
                (it.sound as? SoundSpec.Tilted)?.slopeDbPerOctave
            } ?: PlayerUiState.DEFAULT_SLOPE,
            masterLevel = app.settings.masterLevel,
            volumeCap = if (app.settings.volumeCapEnabled) app.settings.volumeCap else 1f,
            timerRemainingMillis = timer.remainingMillis(System.currentTimeMillis()),
            presetId = app.lastMix?.id,
            skinId = app.settings.skinId,
            isPro = entitlement.isPro,
            nightCurveEnabled = app.settings.nightCurveEnabled,
            hasSound = play.layers.isNotEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), PlayerUiState())

    init {
        viewModelScope.launch { restoreLastMix() }
    }

    /**
     * Cold start has to be able to make sound immediately -- the target is
     * unlock to audio in two seconds -- so the last mix is loaded before the
     * user touches anything, and only started when they ask.
     */
    private suspend fun restoreLastMix() {
        val saved = repository.lastMix.first() ?: FactoryPresets.byId(FactoryPresets.NURSERY)
        saved?.let { applyPreset(it, startPlaying = false) }
        val settings = repository.settings.first()
        applyLevel(settings)
        playback.setNightCurveEnabled(settings.nightCurveEnabled)
    }

    private fun applyLevel(settings: AppSettings) {
        val cap = if (settings.volumeCapEnabled) settings.volumeCap else 1f
        playback.setMasterLevel(minOf(settings.masterLevel, cap))
    }

    fun applyPreset(preset: Preset, startPlaying: Boolean = true) {
        val limit = FeatureGate.layerLimit(state.value.isPro)
        playback.setMix(preset.toSpecs().take(limit))
        viewModelScope.launch { repository.setLastMix(preset) }
        if (startPlaying) playback.play()
    }

    fun toggle() = playback.toggle()

    /**
     * Moving Tone retunes every tilted layer at once rather than only the
     * first. A two-layer mix is still one sound to the listener, so one
     * control moving one of them would feel broken.
     */
    fun setTone(slope: Float) {
        val layers = playback.state.value.layers.map { layer ->
            if (layer.sound is SoundSpec.Tilted) layer.copy(sound = SoundSpec.Tilted(slope)) else layer
        }
        playback.setMix(layers)
    }

    fun setLevel(level: Float) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(masterLevel = level) }
            applyLevel(repository.settings.first())
        }
    }

    fun setTimer(duration: Duration?) {
        playback.setSleepTimer(
            if (duration == null) SleepTimer.Mode.Off else SleepTimer.Mode.After(duration),
        )
    }

    fun setTimerUntilAlarm(lead: Duration) {
        playback.setSleepTimer(SleepTimer.Mode.BeforeAlarm(lead))
    }

    /** Saves whatever is playing now, so a good accident can be kept. */
    fun saveCurrentAs(name: String) {
        viewModelScope.launch {
            val preset = Preset(
                id = "user.${System.currentTimeMillis()}",
                name = name,
                layers = playback.state.value.layers.map { it.toConfig() },
                createdAtMillis = System.currentTimeMillis(),
            )
            repository.savePreset(preset)
            repository.setLastMix(preset)
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
