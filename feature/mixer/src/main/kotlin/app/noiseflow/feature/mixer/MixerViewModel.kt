package app.noiseflow.feature.mixer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.noiseflow.audio.LayerSpec
import app.noiseflow.audio.ModulationShape
import app.noiseflow.audio.generators.SoundSpec
import app.noiseflow.billing.BillingGateway
import app.noiseflow.billing.FeatureGate
import app.noiseflow.playback.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class MixerUiState(
    val layers: List<LayerSpec> = emptyList(),
    val isPro: Boolean = false,
    val layerLimit: Int = FeatureGate.FREE_LAYER_LIMIT,
) {
    val canAddLayer: Boolean get() = layers.size < layerLimit
}

@HiltViewModel
class MixerViewModel @Inject constructor(
    private val playback: PlaybackController,
    billing: BillingGateway,
) : ViewModel() {

    val state: StateFlow<MixerUiState> = combine(
        playback.state,
        billing.entitlement,
    ) { play, entitlement ->
        MixerUiState(
            layers = play.layers,
            isPro = entitlement.isPro,
            layerLimit = FeatureGate.layerLimit(entitlement.isPro),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), MixerUiState())

    fun addLayer(sound: SoundSpec) {
        val current = state.value
        if (!current.canAddLayer) return
        val layer = LayerSpec(id = "layer.${System.currentTimeMillis()}", sound = sound)
        playback.setMix(current.layers + layer)
    }

    fun removeLayer(id: String) {
        playback.setMix(state.value.layers.filterNot { it.id == id })
    }

    fun update(id: String, transform: (LayerSpec) -> LayerSpec) {
        playback.setMix(state.value.layers.map { if (it.id == id) transform(it) else it })
    }

    fun setLevel(id: String, level: Float) = update(id) { it.copy(level = level) }

    fun setPan(id: String, pan: Float) = update(id) { it.copy(pan = pan) }

    fun setWidth(id: String, width: Float) = update(id) { it.copy(width = width) }

    /** Pro-only; the caller is expected to have shown the paywall already. */
    fun setRoom(id: String, room: Float) {
        if (!FeatureGate.canUseRoom(state.value.isPro)) return
        update(id) { it.copy(room = room) }
    }

    fun setModulation(id: String, shape: ModulationShape) {
        if (!FeatureGate.canUseModulation(state.value.isPro)) return
        update(id) { it.copy(modulation = shape, modulationDepth = DEFAULT_DEPTH) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val DEFAULT_DEPTH = 0.35f
    }
}
