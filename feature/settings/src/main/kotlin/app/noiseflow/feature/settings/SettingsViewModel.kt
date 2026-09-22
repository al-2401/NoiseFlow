package app.noiseflow.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.noiseflow.billing.BillingGateway
import app.noiseflow.data.model.AppSettings
import app.noiseflow.data.store.AppStateRepository
import app.noiseflow.i18n.AppLocale
import app.noiseflow.playback.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val isPro: Boolean = false,
    val currentLocaleTag: String? = null,
    val availableLocales: List<String> = AppLocale.supported,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: AppStateRepository,
    private val playback: PlaybackController,
    private val billing: BillingGateway,
) : ViewModel() {

    val state: StateFlow<SettingsUiState> = combine(
        repository.settings,
        billing.entitlement,
    ) { settings, entitlement ->
        SettingsUiState(
            settings = settings,
            isPro = entitlement.isPro,
            currentLocaleTag = AppLocale.current(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SettingsUiState())

    fun setSkin(id: String) {
        viewModelScope.launch { repository.updateSettings { it.copy(skinId = id) } }
    }

    /** Null means "follow the system", which is the default. */
    fun setLocale(tag: String?) {
        AppLocale.set(tag)
        viewModelScope.launch { repository.updateSettings { it.copy(localeTag = tag) } }
    }

    fun setNightCurve(enabled: Boolean) {
        playback.setNightCurveEnabled(enabled)
        viewModelScope.launch { repository.updateSettings { it.copy(nightCurveEnabled = enabled) } }
    }

    fun setVolumeCapEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(volumeCapEnabled = enabled) }
            applyLevel()
        }
    }

    fun setVolumeCap(cap: Float) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(volumeCap = cap) }
            applyLevel()
        }
    }

    fun setAnalyticsOptIn(enabled: Boolean) {
        viewModelScope.launch { repository.updateSettings { it.copy(analyticsOptIn = enabled) } }
    }

    fun restorePurchases() {
        viewModelScope.launch { billing.restore() }
    }

    private fun applyLevel() {
        val settings = state.value.settings
        val cap = if (settings.volumeCapEnabled) settings.volumeCap else 1f
        playback.setMasterLevel(minOf(settings.masterLevel, cap))
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
