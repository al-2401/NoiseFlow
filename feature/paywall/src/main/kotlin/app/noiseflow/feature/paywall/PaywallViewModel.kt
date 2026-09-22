package app.noiseflow.feature.paywall

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.noiseflow.billing.BillingGateway
import app.noiseflow.billing.ProProduct
import app.noiseflow.billing.ProductId
import app.noiseflow.billing.PurchaseResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PaywallUiState(
    val products: List<ProProduct> = emptyList(),
    val selected: ProductId = ProductId.YEARLY,
    val isPro: Boolean = false,
    val isWorking: Boolean = false,
    val error: String? = null,
)

/**
 * One screen, no countdown timers, no fake discounts, and the free column is
 * shown honestly next to the paid one. The product's whole positioning is that
 * we do not do what the rest of this category does.
 */
@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val billing: BillingGateway,
) : ViewModel() {

    private val _state = MutableStateFlow(PaywallUiState())
    val state: StateFlow<PaywallUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update { it.copy(products = billing.products()) }
        }
        viewModelScope.launch {
            billing.entitlement.collect { entitlement ->
                _state.update { it.copy(isPro = entitlement.isPro) }
            }
        }
    }

    fun select(id: ProductId) = _state.update { it.copy(selected = id) }

    fun purchase(activity: Activity) {
        viewModelScope.launch {
            _state.update { it.copy(isWorking = true, error = null) }
            val result = billing.purchase(activity, _state.value.selected)
            _state.update {
                it.copy(
                    isWorking = false,
                    // Cancelling is a normal thing to do, not an error to
                    // scold someone about.
                    error = (result as? PurchaseResult.Failed)?.message,
                )
            }
        }
    }

    fun restore() {
        viewModelScope.launch {
            _state.update { it.copy(isWorking = true) }
            billing.restore()
            _state.update { it.copy(isWorking = false) }
        }
    }
}
