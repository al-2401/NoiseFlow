package app.noiseflow.feature.presets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.noiseflow.billing.BillingGateway
import app.noiseflow.billing.FeatureGate
import app.noiseflow.data.model.FactoryPresets
import app.noiseflow.data.model.Preset
import app.noiseflow.data.model.toSpecs
import app.noiseflow.data.store.AppStateRepository
import app.noiseflow.data.store.PresetLink
import app.noiseflow.playback.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PresetsUiState(
    val factory: List<Preset> = emptyList(),
    val mine: List<Preset> = emptyList(),
    val activeId: String? = null,
    val isPro: Boolean = false,
    val canSaveMore: Boolean = true,
)

sealed interface PresetEvent {
    data class Share(val uri: String) : PresetEvent
    data object Imported : PresetEvent
    data object ImportFailed : PresetEvent
}

@HiltViewModel
class PresetsViewModel @Inject constructor(
    private val repository: AppStateRepository,
    private val playback: PlaybackController,
    billing: BillingGateway,
) : ViewModel() {

    private val _events = MutableSharedFlow<PresetEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<PresetEvent> = _events

    val state: StateFlow<PresetsUiState> = combine(
        repository.state,
        billing.entitlement,
    ) { app, entitlement ->
        PresetsUiState(
            factory = FactoryPresets.all,
            mine = app.userPresets,
            activeId = app.lastMix?.id,
            isPro = entitlement.isPro,
            canSaveMore = app.userPresets.size < FeatureGate.presetLimit(entitlement.isPro),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), PresetsUiState())

    fun apply(preset: Preset) {
        playback.setMix(preset.toSpecs().take(FeatureGate.layerLimit(state.value.isPro)))
        viewModelScope.launch { repository.setLastMix(preset) }
        playback.play()
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.deletePreset(id) }
    }

    fun rename(id: String, name: String) {
        viewModelScope.launch { repository.renamePreset(id, name) }
    }

    fun share(preset: Preset) {
        viewModelScope.launch { _events.emit(PresetEvent.Share(PresetLink.toUri(preset))) }
    }

    /** Called from the deep link handler in the activity. */
    fun import(uri: String) {
        viewModelScope.launch {
            val incoming = PresetLink.fromUri(uri)
            if (incoming == null) {
                _events.emit(PresetEvent.ImportFailed)
                return@launch
            }
            // Re-id on import so a shared preset can never overwrite one of
            // the recipient's own with a colliding id.
            val stored = incoming.copy(
                id = "user.${System.currentTimeMillis()}",
                isFactory = false,
                createdAtMillis = System.currentTimeMillis(),
            )
            repository.savePreset(stored)
            _events.emit(PresetEvent.Imported)
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
