package app.noiseflow.billing.play

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import app.noiseflow.billing.BillingGateway
import app.noiseflow.billing.Entitlement
import app.noiseflow.billing.EntitlementCache
import app.noiseflow.billing.ProProduct
import app.noiseflow.billing.ProductId
import app.noiseflow.billing.PurchaseResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class PlayBillingGateway @Inject constructor(
    @ApplicationContext context: Context,
    private val cache: EntitlementCache,
    private val scope: CoroutineScope,
) : BillingGateway {

    override val entitlement: StateFlow<Entitlement> = cache.entitlement

    private val purchasesUpdated = PurchasesUpdatedListener { result, purchases ->
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            scope.launch { purchases.forEach { settle(it) } }
        }
    }

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdated)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
        )
        .build()

    private suspend fun connect(): Boolean {
        if (client.isReady) return true
        return suspendCancellableCoroutine { continuation ->
            client.startConnection(
                object : BillingClientStateListener {
                    override fun onBillingSetupFinished(result: BillingResult) {
                        if (continuation.isActive) {
                            continuation.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
                        }
                    }

                    override fun onBillingServiceDisconnected() {
                        if (continuation.isActive) continuation.resume(false)
                    }
                },
            )
        }
    }

    override suspend fun products(): List<ProProduct> {
        if (!connect()) return emptyList()
        val subs = queryDetails(BillingClient.ProductType.SUBS, SUBSCRIPTION_IDS)
        val oneOff = queryDetails(BillingClient.ProductType.INAPP, listOf(ProductId.LIFETIME))
        return (subs + oneOff).sortedBy { it.id.ordinal }
    }

    private suspend fun queryDetails(type: String, ids: List<ProductId>): List<ProProduct> {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                ids.map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it.sku)
                        .setProductType(type)
                        .build()
                },
            )
            .build()
        val result = client.queryProductDetails(params)
        val details = result.productDetailsList.orEmpty()
        return details.mapNotNull { detail ->
            val id = ProductId.entries.firstOrNull { it.sku == detail.productId } ?: return@mapNotNull null
            ProProduct(id = id, formattedPrice = detail.displayPrice())
        }
    }

    /**
     * The store already knows the user's currency, locale and tax rules, so the
     * price string comes from it verbatim. Formatting money ourselves is how
     * apps end up showing the wrong currency in half their markets.
     */
    private fun ProductDetails.displayPrice(): String =
        oneTimePurchaseOfferDetails?.formattedPrice
            ?: subscriptionOfferDetails
                ?.firstOrNull()
                ?.pricingPhases
                ?.pricingPhaseList
                ?.lastOrNull()
                ?.formattedPrice
                .orEmpty()

    override suspend fun purchase(activity: Activity, id: ProductId): PurchaseResult {
        if (!connect()) return PurchaseResult.Failed(message = null)

        val type = if (id == ProductId.LIFETIME) {
            BillingClient.ProductType.INAPP
        } else {
            BillingClient.ProductType.SUBS
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(id.sku)
                        .setProductType(type)
                        .build(),
                ),
            )
            .build()
        val detail = client.queryProductDetails(params).productDetailsList?.firstOrNull()
            ?: return PurchaseResult.Failed(message = null)

        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(detail)
            .apply {
                detail.subscriptionOfferDetails?.firstOrNull()?.offerToken?.let { setOfferToken(it) }
            }
            .build()

        val result = client.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(productParams)).build(),
        )
        return when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> PurchaseResult.Success
            BillingClient.BillingResponseCode.USER_CANCELED -> PurchaseResult.Cancelled
            else -> PurchaseResult.Failed(result.debugMessage)
        }
    }

    override suspend fun restore() {
        if (!connect()) return
        val subs = client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(),
        )
        val oneOff = client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
        )
        val all = subs.purchasesList + oneOff.purchasesList
        all.forEach { settle(it) }

        // Only downgrade when the store answered and said there is nothing.
        // A failed query must never take Pro away from someone on a plane.
        val answered = subs.billingResult.responseCode == BillingClient.BillingResponseCode.OK &&
            oneOff.billingResult.responseCode == BillingClient.BillingResponseCode.OK
        if (answered && all.none { it.purchaseState == Purchase.PurchaseState.PURCHASED }) {
            cache.revoke()
        }
    }

    private suspend fun settle(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (!purchase.isAcknowledged) {
            client.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build(),
            )
        }
        cache.update(Entitlement(isPro = true))
    }

    private companion object {
        val SUBSCRIPTION_IDS = listOf(ProductId.MONTHLY, ProductId.YEARLY)
    }
}
