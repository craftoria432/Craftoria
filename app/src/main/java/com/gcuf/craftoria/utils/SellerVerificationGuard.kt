package com.gcuf.craftoria.utils

import com.gcuf.craftoria.data.model.User
import com.gcuf.craftoria.data.model.UserRole
import com.gcuf.craftoria.data.model.VerificationStatus

/**
 * Security guard to enforce seller verification across the app
 * 
 * CRITICAL SECURITY RULE:
 * Sellers MUST be APPROVED before accessing ANY seller features:
 * - Dashboard
 * - Products (Add/Edit/Manage)
 * - Orders
 * - Payments
 * - Refunds
 * - Messages
 * - Negotiations
 * 
 * This prevents unverified sellers from:
 * 1. Creating products
 * 2. Viewing/managing orders
 * 3. Accessing payment information
 * 4. Communicating with buyers
 * 5. Any other seller-specific functionality
 */
object SellerVerificationGuard {
    
    /**
     * Check if a seller is verified and can access seller features
     * 
     * @param user The current user
     * @return true if user is a verified seller, false otherwise
     */
    fun isSellerVerified(user: User?): Boolean {
        if (user == null) return false
        if (user.role != UserRole.SELLER) return false
        return user.verificationStatus == VerificationStatus.APPROVED
    }
    
    /**
     * Check if a seller is unverified and should be blocked from seller features
     * 
     * @param user The current user
     * @return true if user is an unverified seller, false otherwise
     */
    fun isSellerUnverified(user: User?): Boolean {
        if (user == null) return false
        if (user.role != UserRole.SELLER) return false
        return user.verificationStatus != VerificationStatus.APPROVED
    }
    
    /**
     * Get the verification status message for display
     * 
     * @param user The current user
     * @return Human-readable verification status message
     */
    fun getVerificationStatusMessage(user: User?): String {
        if (user == null || user.role != UserRole.SELLER) {
            return "Not a seller account"
        }
        
        return when (user.verificationStatus) {
            VerificationStatus.NOT_SUBMITTED -> "Verification not submitted"
            VerificationStatus.PENDING -> "Verification pending admin approval"
            VerificationStatus.APPROVED -> "Verified seller"
            VerificationStatus.REJECTED -> "Verification rejected: ${user.rejectionReason}"
        }
    }
    
    /**
     * Check if seller can create products
     * Only verified sellers can create products
     */
    fun canCreateProducts(user: User?): Boolean = isSellerVerified(user)
    
    /**
     * Check if seller can manage products
     * Only verified sellers can manage products
     */
    fun canManageProducts(user: User?): Boolean = isSellerVerified(user)
    
    /**
     * Check if seller can view orders
     * Only verified sellers can view orders
     */
    fun canViewOrders(user: User?): Boolean = isSellerVerified(user)
    
    /**
     * Check if seller can access payments
     * Only verified sellers can access payments
     */
    fun canAccessPayments(user: User?): Boolean = isSellerVerified(user)
    
    /**
     * Check if seller can access dashboard
     * Only verified sellers can access dashboard
     */
    fun canAccessDashboard(user: User?): Boolean = isSellerVerified(user)
}
