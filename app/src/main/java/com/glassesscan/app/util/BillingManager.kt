package com.glassesscan.app.util

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.glassesscan.app.data.settings.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BillingManager(
    private val context: Context,
    private val settingsManager: SettingsManager
) : PurchasesUpdatedListener {

    private val TAG = "BillingManager"

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .enablePrepaidPlans()
                .build()
        )
        .enableAutoServiceReconnection()
        .build()

    private val _isBillingReady = MutableStateFlow(false)
    val isBillingReady = _isBillingReady.asStateFlow()

    private val _premiumPrice = MutableStateFlow<String?>(null)
    val premiumPrice = _premiumPrice.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main)

    // Subscription or In-App Product ID from Play Console
    private val PRODUCT_ID = "premium_protection"

    init {
        startConnection()
    }

    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _isBillingReady.value = true
                    queryPurchases()
                    fetchProductDetails()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Connection lost, auto-reconnection handled by BillingClient
            }
        })
    }

    private fun fetchProductDetails() {
        scope.launch {
            // First try SUBS
            val subsList = listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRODUCT_ID)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )
            val subsParams = QueryProductDetailsParams.newBuilder().setProductList(subsList).build()

            try {
                val subsResult = withContext(Dispatchers.IO) {
                    billingClient.queryProductDetails(subsParams)
                }
                val subsDetails = subsResult.productDetailsList?.find { it.productId == PRODUCT_ID }
                val subsPrice = subsDetails?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice

                if (subsPrice != null) {
                    _premiumPrice.value = subsPrice
                    Log.d(TAG, "Fetched SUBS price: $subsPrice")
                    return@launch
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching SUBS details", e)
            }

            // Fallback to INAPP (one-time purchase)
            val inAppList = listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRODUCT_ID)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            )
            val inAppParams = QueryProductDetailsParams.newBuilder().setProductList(inAppList).build()

            try {
                val inAppResult = withContext(Dispatchers.IO) {
                    billingClient.queryProductDetails(inAppParams)
                }
                val inAppDetails = inAppResult.productDetailsList?.find { it.productId == PRODUCT_ID }
                val inAppPrice = inAppDetails?.oneTimePurchaseOfferDetails?.formattedPrice

                if (inAppPrice != null) {
                    _premiumPrice.value = inAppPrice
                    Log.d(TAG, "Fetched INAPP price: $inAppPrice")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching INAPP details", e)
            }
        }
    }

    fun queryPurchases() {
        if (!_isBillingReady.value) return

        scope.launch {
            try {
                // Check SUBS
                val subsParams = QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
                val subsResult = withContext(Dispatchers.IO) {
                    billingClient.queryPurchasesAsync(subsParams)
                }

                var hasPremium = false
                if (subsResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    hasPremium = subsResult.purchasesList.any { purchase ->
                        purchase.products.contains(PRODUCT_ID) &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                    }
                }

                // Check INAPP if not found in SUBS
                if (!hasPremium) {
                    val inAppParams = QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                    val inAppResult = withContext(Dispatchers.IO) {
                        billingClient.queryPurchasesAsync(inAppParams)
                    }
                    if (inAppResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        hasPremium = inAppResult.purchasesList.any { purchase ->
                            purchase.products.contains(PRODUCT_ID) &&
                            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                        }
                    }
                }

                if (hasPremium) {
                    settingsManager.setPremium(true)
                    RealtimeDbManager.syncUserProfile(AuthManager.getUserEmail(), true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception querying purchases: ${e.message}")
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity) {
        scope.launch {
            try {
                // Try SUBS first
                val subsList = listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
                val subsParams = QueryProductDetailsParams.newBuilder().setProductList(subsList).build()
                val subsResult = withContext(Dispatchers.IO) {
                    billingClient.queryProductDetails(subsParams)
                }

                val subsDetails = subsResult.productDetailsList?.find { it.productId == PRODUCT_ID }
                val offerToken = subsDetails?.subscriptionOfferDetails?.firstOrNull()?.offerToken

                if (subsDetails != null && offerToken != null) {
                    val productDetailsParamsList = listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(subsDetails)
                            .setOfferToken(offerToken)
                            .build()
                    )
                    val billingFlowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(productDetailsParamsList)
                        .build()

                    billingClient.launchBillingFlow(activity, billingFlowParams)
                    return@launch
                }

                // Fallback to INAPP
                val inAppList = listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
                val inAppParams = QueryProductDetailsParams.newBuilder().setProductList(inAppList).build()
                val inAppResult = withContext(Dispatchers.IO) {
                    billingClient.queryProductDetails(inAppParams)
                }

                val inAppDetails = inAppResult.productDetailsList?.find { it.productId == PRODUCT_ID }
                if (inAppDetails != null) {
                    val productDetailsParamsList = listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(inAppDetails)
                            .build()
                    )
                    val billingFlowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(productDetailsParamsList)
                        .build()

                    billingClient.launchBillingFlow(activity, billingFlowParams)
                } else {
                    Log.e(TAG, "No product details found for $PRODUCT_ID in SUBS or INAPP")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception launching purchase flow: ${e.message}")
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "User canceled the purchase flow")
        } else {
            Log.w(TAG, "onPurchasesUpdated error: ${billingResult.debugMessage}")
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            scope.launch {
                // Immediately activate premium locally and sync to cloud
                settingsManager.setPremium(true)
                RealtimeDbManager.syncUserProfile(AuthManager.getUserEmail(), true)

                if (!purchase.isAcknowledged) {
                    val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    withContext(Dispatchers.IO) {
                        billingClient.acknowledgePurchase(acknowledgePurchaseParams)
                    }
                }
            }
        }
    }
}
