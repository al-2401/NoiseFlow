package app.noiseflow.billing

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

/** The three things we sell. Deliberately few: no per-sound purchases. */
enum class ProductId(val sku: String) {
    MONTHLY("pro_monthly"),
    YEARLY("pro_yearly"),

    /**
     * The differentiator. Every large competitor has gone subscription-only or
     * prices a lifetime tier out of reach, and it is the loudest complaint in
     * their reviews.
     */
    LIFETIME("pro_lifetime"),
}

data class ProProduct(
    val id: ProductId,
    /** Already localised and currency-formatted by the store. Never format prices ourselves. */
    val formattedPrice: String,
    val available: Boolean = true,
)

data class Entitlement(val isPro: Boolean = false) {
    companion object {
        val FREE = Entitlement(false)
    }
}

sealed interface PurchaseResult {
    data object Success : PurchaseResult
    data object Cancelled : PurchaseResult
    data class Failed(val message: String?) : PurchaseResult
}

/**
 * One interface, two stores.
 *
 * Google Play is the main channel and RuStore the second; neither the paywall
 * nor any feature knows which one it is running against. This is also why
 * there is no RevenueCat: it would not cover RuStore, so we would be writing
 * the second implementation anyway, and it puts a network dependency in the
 * middle of an app that promises to work offline.
 */
interface BillingGateway {
    val entitlement: StateFlow<Entitlement>

    suspend fun products(): List<ProProduct>

    suspend fun purchase(activity: Activity, id: ProductId): PurchaseResult

    /** Re-reads what the store already knows. Safe to call on every start. */
    suspend fun restore()
}
