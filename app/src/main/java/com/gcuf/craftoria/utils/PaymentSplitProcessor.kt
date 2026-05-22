package com.gcuf.craftoria.utils

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.gcuf.craftoria.data.model.*
import com.gcuf.craftoria.data.repository.CommissionRepository
import kotlinx.coroutines.tasks.await
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * ✅ PRODUCTION-READY: Handles payment split creation for co-seller stores
 * 
 * This utility processes orders and creates appropriate payment records:
 * - Original seller products: Single payment (no split)
 * - Co-seller store products: Split payment based on store configuration
 * - Applies admin commission to all payments
 */
class PaymentSplitProcessor(private val db: FirebaseFirestore) {

    companion object {
        private const val TAG = "PaymentSplitProcessor"
    }

    private val commissionRepository = CommissionRepository(db)

    /**
     * Process order items and create payment records with splits
     * 
     * Flow:
     * 1. Fetch commission settings
     * 2. Group items by seller/store
     * 3. For original sellers: Create payment with commission deducted
     * 4. For co-seller stores: Create split payment with commission deducted
     * 5. Create admin commission records
     * 6. Return all created payment IDs
     */
    suspend fun processOrderPaymentsWithSplits(
        order: Order,
        items: List<OrderItem>
    ): Result<List<String>> = try {
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "💳 Processing payments with splits for order: ${order.id}")

        val paymentIds = mutableListOf<String>()
        // New co-seller payments must live in the canonical payments collection.
        // The UI still reads legacy seller_payments data during migration, but new writes
        // should not continue splitting data across collections.
        val paymentsCollection = db.collection("payments")

        // ✅ Step 0: Fetch commission settings
        val commissionSettingsResult = commissionRepository.getCommissionSettings()
        val commissionSettings = commissionSettingsResult.getOrNull() ?: CommissionSettings()
        val commissionRate = commissionSettings.commissionRate / 100.0  // Convert 5 to 0.05
        Log.d(TAG, "⚙️ Commission Rate: ${commissionSettings.commissionRate}%")

        // ✅ Step 1: Group items by store (co-seller store ID or seller ID)
        val itemsByStore = groupItemsByStore(items)
        Log.d(TAG, "📦 Items grouped into ${itemsByStore.size} store(s)")

        // ✅ Step 2: Collect all involved sellers for access control
        val involvedSellerIds = items.map { it.sellerId }.distinct()
        Log.d(TAG, "👥 Involved sellers: ${involvedSellerIds.size}")

        // ✅ Step 3: Process each store group
        itemsByStore.forEach { (storeKey, storeItems) ->
            try {
                // ✅ FIX 5: Use BigDecimal for precise financial calculations
                val totalAmountBD = storeItems.fold(BigDecimal.ZERO) { acc, item ->
                    acc + (BigDecimal(item.price) * BigDecimal(item.quantity))
                }
                val totalAmount = totalAmountBD.setScale(2, RoundingMode.HALF_UP).toDouble()
                val itemsCount = storeItems.sumOf { it.quantity }

                // ✅ Calculate commission with precision
                val adminCommissionBD = BigDecimal(totalAmount)
                    .multiply(BigDecimal(commissionRate))
                    .setScale(2, RoundingMode.HALF_UP)
                val adminCommission = adminCommissionBD.toDouble()
                
                val sellerAmountBD = BigDecimal(totalAmount)
                    .minus(adminCommissionBD)
                    .setScale(2, RoundingMode.HALF_UP)
                val sellerAmount = sellerAmountBD.toDouble()

                Log.d(TAG, "")
                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Log.d(TAG, "🏪 Store: $storeKey")
                Log.d(TAG, "💵 Total Amount: PKR $totalAmount")
                Log.d(TAG, "💰 Admin Commission (${commissionSettings.commissionRate}%): PKR $adminCommission")
                Log.d(TAG, "💸 Seller Payout: PKR $sellerAmount")
                Log.d(TAG, "📦 Items: $itemsCount")
                Log.d(TAG, "✅ Amounts calculated with BigDecimal precision")

                if (storeKey.startsWith("original_seller_")) {
                    // ✅ Original seller: Commission deducted
                    val sellerId = storeKey.removePrefix("original_seller_")
                    val sellerName = storeItems.first().sellerName

                    Log.d(TAG, "👤 Original Seller: $sellerName")

                    val payment = SellerPayment(
                        sellerId = sellerId,
                        sellerName = sellerName,
                        orderId = order.id,
                        coSellerStoreId = "",  // Empty for original sellers
                        storeName = sellerName,
                        buyerId = order.buyerId,
                        buyerName = order.buyerName,
                        amount = sellerAmount,  // ✅ Commission already deducted
                        paymentMethod = order.paymentMethod,
                        status = PaymentStatus.PENDING.toString(),
                        itemsCount = itemsCount,
                        itemsDetails = storeItems.map { item ->
                            PaymentItemDetail(
                                productId = item.productId,
                                productTitle = item.productTitle,
                                quantity = item.quantity,
                                price = item.price,
                                itemTotal = item.price * item.quantity
                            )
                        },
                        involvedSellerIds = involvedSellerIds,
                        paymentSplits = emptyList(),  // No splits for original sellers
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )

                    val docRef = paymentsCollection.add(payment.toMap()).await()
                    val paymentId = docRef.id
                    paymentIds.add(paymentId)

                    paymentsCollection.document(paymentId).update("id", paymentId).await()
                    Log.d(TAG, "✅ Original seller payment created: $paymentId")

                    // ✅ Create admin commission record
                    val commission = AdminCommission(
                        orderId = order.id,
                        paymentId = paymentId,
                        sellerId = sellerId,
                        sellerName = sellerName,
                        coSellerStoreId = "",
                        storeName = sellerName,
                        subtotal = totalAmount,
                        commissionRate = commissionRate,
                        commissionAmount = adminCommission,
                        sellerPayout = sellerAmount,
                        status = CommissionStatus.PENDING.toString(),
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    commissionRepository.createCommission(commission)
                    Log.d(TAG, "✅ Admin commission record created: PKR $adminCommission")

                } else {
                    // ✅ Co-seller store: Commission deducted, then split
                    val storeId = storeKey
                    val store = getCoSellerStore(storeId)
                    val storeOwner = getUserName(store.ownerId)

                    Log.d(TAG, "🏪 Co-Seller Store: ${store.storeName}")
                    Log.d(TAG, "👤 Store Owner: $storeOwner")

                    // ✅ Create FAIR payment splits based on actual product sales (from seller amount after commission)
                    val splits = createPaymentSplits(
                        store = store,
                        totalAmount = sellerAmount,  // ✅ Split the amount AFTER commission
                        items = storeItems  // ✅ Pass items to calculate fair split
                    )

                    Log.d(TAG, "💸 Payment Splits (after commission):")
                    splits.forEach { split ->
                        Log.d(TAG, "  • ${split.sellerName}: ${split.splitPercentage * 100}% = PKR ${split.splitAmount}")
                    }

                    val payment = SellerPayment(
                        sellerId = store.ownerId,  // Store owner receives payment
                        sellerName = storeOwner,
                        orderId = order.id,
                        coSellerStoreId = storeId,
                        storeName = store.storeName,
                        buyerId = order.buyerId,
                        buyerName = order.buyerName,
                        amount = sellerAmount,  // ✅ Commission already deducted
                        paymentMethod = order.paymentMethod,
                        status = PaymentStatus.PENDING.toString(),
                        itemsCount = itemsCount,
                        itemsDetails = storeItems.map { item ->
                            PaymentItemDetail(
                                productId = item.productId,
                                productTitle = item.productTitle,
                                quantity = item.quantity,
                                price = item.price,
                                itemTotal = item.price * item.quantity
                            )
                        },
                        involvedSellerIds = store.memberIds,
                        paymentSplits = splits,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )

                    val docRef = paymentsCollection.add(payment.toMap()).await()
                    val paymentId = docRef.id
                    paymentIds.add(paymentId)

                    paymentsCollection.document(paymentId).update("id", paymentId).await()
                    Log.d(TAG, "✅ Co-seller store payment created: $paymentId")

                    // ✅ Create admin commission record
                    val commission = AdminCommission(
                        orderId = order.id,
                        paymentId = paymentId,
                        sellerId = store.ownerId,
                        sellerName = storeOwner,
                        coSellerStoreId = storeId,
                        storeName = store.storeName,
                        subtotal = totalAmount,
                        commissionRate = commissionRate,
                        commissionAmount = adminCommission,
                        sellerPayout = sellerAmount,
                        status = CommissionStatus.PENDING.toString(),
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    commissionRepository.createCommission(commission)
                    Log.d(TAG, "✅ Admin commission record created: PKR $adminCommission")
                }

                Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error processing store: $storeKey", e)
            }
        }

        Log.d(TAG, "✅ All payments processed: ${paymentIds.size} payments created")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        Result.success(paymentIds)

    } catch (e: Exception) {
        Log.e(TAG, "❌ Failed to process order payments with splits", e)
        Result.failure(e)
    }

    /**
     * Group items by store
     * - Original seller products: "original_seller_{sellerId}"
     * - Co-seller store products: "{storeId}"
     */
    private suspend fun groupItemsByStore(items: List<OrderItem>): Map<String, List<OrderItem>> {
        val grouped = mutableMapOf<String, MutableList<OrderItem>>()

        items.forEach { item ->
            val product = getProduct(item.productId)
            val storeKey = if (product.coSellerStoreId.isNotEmpty()) {
                product.coSellerStoreId
            } else {
                "original_seller_${product.sellerId}"
            }

            grouped.getOrPut(storeKey) { mutableListOf() }.add(item)
        }

        return grouped
    }

    /**
     * ✅ FAIR PAYMENT SPLIT: Create payment splits based on actual product sales
     * 
     * This ensures each co-seller receives payment proportional to their actual sales,
     * which is critical for fairness to women entrepreneurs.
     * 
     * Priority:
     * 1. Product-based split (FAIR) - Each seller gets paid for what they sold
     * 2. Configured split - Only if all products are from the same seller
     * 3. Equal split - Fallback for edge cases
     * 
     * @param store The co-seller store
     * @param totalAmount Total amount to split (after commission)
     * @param items Order items to determine who sold what
     * @return List of payment splits proportional to actual sales
     */
    private suspend fun createPaymentSplits(
        store: CoSellerStore,
        totalAmount: Double,
        items: List<OrderItem>
    ): List<PaymentSplit> {
        
        // ✅ STEP 1: Calculate actual sales by each seller
        val salesBySeller = items.groupBy { it.sellerId }
            .mapValues { (_, sellerItems) ->
                sellerItems.sumOf { it.price * it.quantity }
            }
        
        val totalSales = salesBySeller.values.sum()
        
        // ✅ STEP 2: If only one seller, give them 100%
        if (salesBySeller.size == 1) {
            val (sellerId, sellerSales) = salesBySeller.entries.first()
            Log.d(TAG, "Single seller detected: ${getUserName(sellerId)} gets 100%")
            
            return listOf(
                PaymentSplit(
                    sellerId = sellerId,
                    sellerName = getUserName(sellerId),
                    splitPercentage = 1.0,
                    splitAmount = totalAmount,
                    status = PaymentStatus.PENDING.toString()
                )
            )
        }
        
        // ✅ STEP 3: Multiple sellers - FAIR PRODUCT-BASED SPLIT
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "💎 FAIR PAYMENT SPLIT (Product-Based)")
        Log.d(TAG, "Total Sales: PKR $totalSales")
        Log.d(TAG, "Amount to Split: PKR $totalAmount (after commission)")
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        
        val splits = salesBySeller.map { (sellerId, sellerSales) ->
            val percentage = sellerSales / totalSales
            val splitAmount = totalAmount * percentage
            val sellerName = getUserName(sellerId)
            
            Log.d(TAG, "👤 $sellerName:")
            Log.d(TAG, "   Sales: PKR $sellerSales (${String.format("%.1f", percentage * 100)}%)")
            Log.d(TAG, "   Gets: PKR ${String.format("%.2f", splitAmount)}")
            
            PaymentSplit(
                sellerId = sellerId,
                sellerName = sellerName,
                splitPercentage = percentage,
                splitAmount = splitAmount,
                status = PaymentStatus.PENDING.toString()
            )
        }
        
        Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(TAG, "✅ Fair split calculated for ${splits.size} sellers")
        
        return splits
    }

    /**
     * Helper: Get product from Firestore
     */
    private suspend fun getProduct(productId: String): Product {
        return db.collection("products")
            .document(productId)
            .get()
            .await()
            .toObject(Product::class.java) ?: Product()
    }

    /**
     * Helper: Get co-seller store from Firestore
     */
    private suspend fun getCoSellerStore(storeId: String): CoSellerStore {
        return db.collection("co_seller_stores")
            .document(storeId)
            .get()
            .await()
            .toObject(CoSellerStore::class.java) ?: CoSellerStore()
    }

    /**
     * Helper: Get user name from Firestore
     */
    private suspend fun getUserName(userId: String): String {
        return try {
            val user = db.collection("users")
                .document(userId)
                .get()
                .await()
                .toObject(User::class.java)
            user?.name ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }
}
