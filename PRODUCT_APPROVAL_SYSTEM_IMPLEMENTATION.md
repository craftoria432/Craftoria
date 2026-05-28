# Product Approval System Implementation

## Overview
Implemented a professional product approval workflow for Craftoria to ensure only genuine handicrafts are listed on the platform. Products now go through a pending approval status before becoming visible to buyers.

## Changes Made

### 1. Product Model (`Product.kt`)
Added approval-related fields to the Product data class:

```kotlin
// ✅ APPROVAL SYSTEM
@get:PropertyName("approval_status")
@set:PropertyName("approval_status")
var approvalStatus: String = "pending", // pending, approved, rejected

@get:PropertyName("rejection_reason")
@set:PropertyName("rejection_reason")
var rejectionReason: String? = null,

@get:PropertyName("approved_at")
@set:PropertyName("approved_at")
var approvedAt: Long? = null,

@get:PropertyName("approved_by")
@set:PropertyName("approved_by")
var approvedBy: String? = null,
```

**Updated toMap() function** to include all approval fields in Firestore serialization.

### 2. Product Filter Enum (`ProductRepository.kt`)
Added PENDING filter to ProductFilter enum:

```kotlin
enum class ProductFilter {
    ALL,
    ACTIVE,
    INACTIVE,
    OUT_OF_STOCK,
    DRAFTS,
    PENDING  // ✅ NEW: For products awaiting approval
}
```

### 3. Product Repository (`ProductRepository.kt`)
Updated `getSellerProducts()` function to filter pending products:

```kotlin
ProductFilter.PENDING -> {
    // ✅ NEW: Show products awaiting approval
    products.filter { it.approvalStatus == "pending" && !it.isDraft }
}
```

### 4. Add Product ViewModel (`AddProductViewModel.kt`)
Updated `publishProduct()` function to set approval status to "pending" when creating new products:

```kotlin
val product = Product(
    // ... other fields ...
    approvalStatus = "pending"  // ✅ NEW: Set to pending for approval
)
```

### 5. Manage Products Screen (`ManageProductsScreen.kt`)

#### Added Pending Filter Tab
Updated `FilterTabs()` composable to include "Pending" filter:

```kotlin
val filters = listOf(
    ProductFilter.ALL to "All",
    ProductFilter.ACTIVE to "Active",
    ProductFilter.INACTIVE to "Inactive",
    ProductFilter.OUT_OF_STOCK to "Out of Stock",
    ProductFilter.DRAFTS to "Drafts",
    ProductFilter.PENDING to "Pending"  // ✅ NEW
)
```

#### Added Approval Status Badges
Updated ProductCard to display approval status:

```kotlin
// ✅ NEW: Show approval status badge
if (product.approvalStatus == "pending") {
    ApprovalBadge(status = "pending")
} else if (product.approvalStatus == "rejected") {
    ApprovalBadge(status = "rejected")
}
```

#### New ApprovalBadge Composable
Added new composable function to display approval status with color coding:

```kotlin
@Composable
fun ApprovalBadge(status: String) {
    val (text, backgroundColor, textColor) = when (status) {
        "pending" -> Triple("Pending", Color(0xFFFFF3CD), Color(0xFF856404))
        "rejected" -> Triple("Rejected", Color(0xFFF8D7DA), Color(0xFF721C24))
        else -> Triple("Approved", Color(0xFFE8F5E8), Color(0xFF2E7D2E))
    }
    // ... badge rendering ...
}
```

## Product Status Flow

```
SELLER UPLOADS PRODUCT
        ↓
approval_status = "pending"
        ↓
ADMIN REVIEWS (Web Dashboard)
        ↓
    ┌───┴───┐
    ↓       ↓
APPROVED  REJECTED
    ↓       ↓
ACTIVE   INACTIVE
(visible) (hidden)
```

## Seller Experience

1. **Product Upload**: Seller creates product → automatically set to `approval_status: "pending"`
2. **Pending View**: Seller sees "Pending" badge on their product in Manage Products screen
3. **Filter Tab**: Sellers can filter by "Pending" to see all products awaiting approval
4. **Approval Notification**: (Future) Seller receives notification when product is approved/rejected
5. **Rejection Reason**: (Future) If rejected, seller can see rejection reason and resubmit

## Admin Dashboard Changes (Web)

The web dashboard (`ProductManagement.jsx`) will need updates to:

1. Add "Approve" button for pending products
2. Add "Reject" button with reason modal
3. Filter products by approval status
4. Show approval metadata (approved_by, approved_at)

### Recommended Web Implementation

```javascript
// Approve product
const handleApproveProduct = async (product) => {
    await updateDoc(doc(db, 'products', product.id), {
        approvalStatus: 'approved',
        approvedAt: serverTimestamp(),
        approvedBy: currentUser.id,
        isActive: true  // Auto-activate on approval
    });
    toast.success('Product approved!');
};

// Reject product
const handleRejectProduct = async (product, reason) => {
    await updateDoc(doc(db, 'products', product.id), {
        approvalStatus: 'rejected',
        rejectionReason: reason,
        isActive: false
    });
    toast.success('Product rejected');
};
```

## Firestore Schema

Products collection now includes:

```json
{
    "id": "product_123",
    "title": "Handmade Ceramic Vase",
    "approval_status": "pending",  // NEW
    "rejection_reason": null,       // NEW
    "approved_at": null,            // NEW
    "approved_by": null,            // NEW
    "is_active": true,
    "seller_id": "seller_456",
    // ... other fields ...
}
```

## Benefits

✅ **Quality Control** - Only genuine handicrafts are listed
✅ **Brand Protection** - Maintains Craftoria's reputation
✅ **Fraud Prevention** - Catches counterfeit products early
✅ **Seller Accountability** - Creates barrier for low-quality sellers
✅ **Legal Compliance** - Provides content moderation records

## Next Steps

1. **Web Dashboard**: Implement approve/reject functionality
2. **Notifications**: Add seller notifications for approval/rejection
3. **Bulk Actions**: Add bulk approve/reject for admins
4. **Auto-Approval**: Add rules for auto-approving verified sellers
5. **Analytics**: Track approval rates and rejection reasons

## Testing Checklist

- [ ] New products are created with `approval_status: "pending"`
- [ ] Pending filter shows only pending products
- [ ] Approval badges display correctly
- [ ] Sellers can see pending products in their dashboard
- [ ] Rejected products show rejection badge
- [ ] Approved products show approved badge
- [ ] Pending products don't appear in buyer search (requires buyer-side filtering)
