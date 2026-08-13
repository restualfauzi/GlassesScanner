package com.glassesscan.app.util

import android.app.Activity
import android.content.Context
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

    // Subscription ID from Play Console
    private val SUBSCRIPTION_ID = "premium_protection"

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
                // Connection lost, the library handles reconnection if enableAutoServiceReconnection is used.
                // However, startConnection should only be called once.
                // No-op here if auto-reconnection is enabled, or implement retry logic.
            }
        })
    }

    private fun fetchProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SUBSCRIPTION_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        scope.launch {
            try {
                // Using KTX suspend extension
                val productDetailsResult = withContext(Dispatchers.IO) {
                    billingClient.queryProductDetails(params)
                }
                
                if (productDetailsResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val productDetails = productDetailsResult.productDetailsList?.find { it.productId == SUBSCRIPTION_ID }
                    // Get the first offer's formatted price (typically the base plan)
                    val price = productDetails?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
                    _premiumPrice.value = price
                    println("Billing: Fetched subscription price: $price")
                } else {
                    println("Billing: Failed to fetch subscription details: ${productDetailsResult.billingResult.debugMessage}")
                }
            } catch (e: Exception) {
                println("Billing: Exception fetching product details: ${e.message}")
            }
        }
    }

    fun queryPurchases() {
        if (!_isBillingReady.value) return

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        scope.launch {
            try {
                // Using KTX suspend extension
                val purchasesResult = withContext(Dispatchers.IO) {
                    billingClient.queryPurchasesAsync(params)
                }
                
                if (purchasesResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val hasPremium = purchasesResult.purchasesList.any { purchase ->
                        purchase.products.contains(SUBSCRIPTION_ID) && 
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                    }
                    settingsManager.setPremium(hasPremium)
                }
            } catch (e: Exception) {
                println("Billing: Exception querying purchases: ${e.message}")
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity) {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SUBSCRIPTION_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        scope.launch {
            try {
                val productDetailsResult = withContext(Dispatchers.IO) {
                    billingClient.queryProductDetails(params)
                }
                
                if (productDetailsResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val productDetails = productDetailsResult.productDetailsList?.find { it.productId == SUBSCRIPTION_ID }
                    val offerToken = productDetails?.subscriptionOfferDetails?.firstOrNull()?.offerToken
                    
                    if (productDetails != null && offerToken != null) {
                        val productDetailsParamsList = listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .setOfferToken(offerToken)
                                .build()
                        )
                        
                        val billingFlowParams = BillingFlowParams.newBuilder()
                            .setProductDetailsParamsList(productDetailsParamsList)
                            .build()
                        
                        billingClient.launchBillingFlow(activity, billingFlowParams)
                    }
                }
            } catch (e: Exception) {
                println("Billing: Exception launching purchase flow: ${e.message}")
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            scope.launch {
                if (!purchase.isAcknowledged) {
                    val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    val billingResult = withContext(Dispatchers.IO) {
                        billingClient.acknowledgePurchase(acknowledgePurchaseParams)
                    }
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        settingsManager.setPremium(true)
                    }
                } else {
                    settingsManager.setPremium(true)
                }
            }
        }
    }
}
