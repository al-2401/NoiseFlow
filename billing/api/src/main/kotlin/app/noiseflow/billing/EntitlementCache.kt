package app.noiseflow.billing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remembers that someone paid, so the app keeps working offline.
 *
 * A sleep app is used on aeroplanes, in basements and on phones in airplane
 * mode. Asking the store to confirm the purchase before unlocking anything
 * would mean the one time someone really needs this, it refuses. So the cached
 * answer is authoritative and the store only ever upgrades it; we would rather
 * grant Pro to a lapsed subscriber for a while than take it from a paying one
 * on a flight.
 */
@Singleton
class EntitlementCache @Inject constructor() {

    private val _entitlement = MutableStateFlow(Entitlement.FREE)
    val entitlement: StateFlow<Entitlement> = _entitlement.asStateFlow()

    fun update(value: Entitlement) {
        _entitlement.value = value
    }

    /** Called when the store says the purchase is gone for good, not on error. */
    fun revoke() {
        _entitlement.value = Entitlement.FREE
    }
}
