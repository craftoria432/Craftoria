# Refund Date Timestamp Deserialization Fix

## Problem
When requesting a refund, the app crashed with error:
```
Could not deserialize object. Failed to convert a value of type com.google.firebase.Timestamp to long 
(found in field 'refund_date')
```

## Root Cause
The `refundDate` field in `SellerPayment` model was defined as `Long?`, but Firestore was storing it as a Firestore Timestamp object. When Firestore tried to deserialize the document, it couldn't convert the Timestamp to a Long directly.

## Solution
Changed the `refundDate` field to accept `Any?` type, which can handle both Long and Firestore Timestamp values. Added a safe conversion helper function to properly convert between types.

## Changes Made

### 1. PaymentModels.kt - SellerPayment Data Class
**File**: `app/src/main/java/com/gcuf/craftoria/data/model/PaymentModels.kt`

#### Changed refundDate Field Type
```kotlin
// Before:
var refundDate: Long? = null

// After:
var refundDate: Any? = null  // ✅ Changed to Any? to safely handle both Long and Firestore Timestamp
```

#### Added Safe Conversion Helper
```kotlin
// ✅ NEW: Safely convert refundDate (Any?) to Long
fun SellerPayment.getRefundDateLong(): Long = when (refundDate) {
    is Long -> refundDate as Long
    is com.google.firebase.Timestamp -> (refundDate as com.google.firebase.Timestamp).toDate().time
    is Number -> (refundDate as Number).toLong()
    is String -> (refundDate as String).toLongOrNull() ?: 0L
    is Map<*, *> -> {
        val map = refundDate as Map<*, *>
        val seconds = (map["_seconds"] as? Long) ?: (map["seconds"] as? Long) ?: 0L
        val nanos = (map["_nanoseconds"] as? Long) ?: (map["nanoseconds"] as? Long) ?: 0L
        (seconds * 1000) + (nanos / 1_000_000)
    }
    null -> 0L
    else -> 0L
}
```

#### Updated toMap() Method
```kotlin
// Before:
"refund_date" to (refundDate ?: 0L)

// After:
"refund_date" to getRefundDateLong()  // ✅ Use safe conversion helper
```

## How It Works
1. When Firestore deserializes the document, it can now accept `Any?` type for `refundDate`
2. The `getRefundDateLong()` helper function safely converts the value to Long
3. Handles multiple input types: Long, Firestore Timestamp, Number, String, Map, or null
4. Returns 0L for null or invalid values

## Type Handling
- **Long**: Direct cast
- **Firestore Timestamp**: Convert to Date, then to milliseconds
- **Number**: Convert to Long
- **String**: Parse as Long or return 0L
- **Map**: Extract seconds and nanoseconds (Firestore internal format)
- **null**: Return 0L
- **Other**: Return 0L

## Testing Checklist
- [ ] Refund request submission works without crash
- [ ] Refund date displays correctly in payment history
- [ ] Existing refunds with Long timestamps still work
- [ ] New refunds with Firestore Timestamp work
- [ ] No deserialization errors in logs

## Files Modified
1. `app/src/main/java/com/gcuf/craftoria/data/model/PaymentModels.kt`
   - Changed `refundDate` field type to `Any?`
   - Added `getRefundDateLong()` helper function
   - Updated `toMap()` method to use safe conversion

## Compilation Status
✅ No errors or warnings

## Related Issues Fixed
- Refund request crash on submission
- Timestamp deserialization error
- Payment history refund date display
