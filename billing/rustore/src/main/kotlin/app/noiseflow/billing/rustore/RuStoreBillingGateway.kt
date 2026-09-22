package app.noiseflow.billing.rustore

import android.app.Activity
import app.noiseflow.billing.BillingGateway
import app.noiseflow.billing.Entitlement
import app.noiseflow.billing.EntitlementCache
import app.noiseflow.billing.ProProduct
import app.noiseflow.billing.ProductId
import app.noiseflow.billing.PurchaseResult
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * RuStore implementation -- NOT YET WIRED TO THE STORE SDK.
 *
 * This deliberately fails closed rather than pretending to work. The RuStore
 * billing SDK is published to RuStore's own Maven repository, which this
 * project does not yet declare, and writing calls against an SDK surface we
 * have not compiled against would produce code that looks finished and is not.
 *
 * To finish it (stage 4b in docs/03-roadmap.md):
 *  1. Add RuStore's Maven repository and the billing client to the catalog,
 *     behind the `rustore` flavor only.
 *  2. Initialise the client with the console-issued application id, and check
 *     `RuStoreBillingClient.checkPurchasesAvailability` before showing a
 *     paywall -- unlike Play, availability is not a given.
 *  3. Map their product model onto [ProProduct]; take the formatted price from
 *     the store, never format roubles ourselves.
 *  4. Confirm consumable and subscription purchases explicitly; RuStore does
 *     not auto-acknowledge the way Play does.
 *  5. On success call [EntitlementCache.update]; on a confirmed refund or
 *     expiry call [EntitlementCache.revoke]. Never revoke on a network error.
 *
 * Until then the rustore flavor ships without purchases, which is safe: the
 * free tier is fully usable and nobody is charged for something that will not
 * unlock.
 */
class RuStoreBillingGateway @Inject constructor(
    private val cache: EntitlementCache,
) : BillingGateway {

    override val entitlement: StateFlow<Entitlement> = cache.entitlement

    override suspend fun products(): List<ProProduct> = ProductId.entries.map {
        ProProduct(id = it, formattedPrice = "", available = false)
    }

    override suspend fun purchase(activity: Activity, id: ProductId): PurchaseResult =
        PurchaseResult.Failed(message = null)

    override suspend fun restore() = Unit
}
