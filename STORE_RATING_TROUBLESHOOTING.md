# Store Rating Feature - Troubleshooting Guide

## Common Issues & Solutions

### Issue 1: Rating Button Not Showing
**Symptoms**: "Rate This Store" button doesn't appear on store view

**Causes & Solutions**:
1. **Buyer not logged in**
   - Check: `currentUserId` is empty
   - Solution: Ensure buyer is authenticated before viewing store
   - Code: `if (currentUserId.isNotEmpty()) { /* show button */ }`

2. **Store data not loaded**
   - Check: `currentStore` is null
   - Solution: Wait for `coSellerStoreViewModel.loadStoreDetails()` to complete
   - Code: Add loading state check

3. **Screen not rendering**
   - Check: `uiState` is still `Loading`
   - Solution: Verify network connection and Firebase connectivity

**Debug Steps**:
```kotlin
// Add logging to StorePublicViewScreen
Log.d("StoreRating", "currentUserId: $currentUserId")
Log.d("StoreRating", "currentStore: ${currentStore?.storeName}")
Log.d("StoreRating", "showRatingDialog: $showRatingDialog")
```

---

### Issue 2: Rating Dialog Won't Open
**Symptoms**: Button exists but clicking doesn't open dialog

**Causes & Solutions**:
1. **State not updating**
   - Check: `showRatingDialog` state variable
   - Solution: Verify `onClick = { showRatingDialog = true }` is correct
   - Code: Use `remember { mutableStateOf(false) }`

2. **Dialog condition not met**
   - Check: `if (showRatingDialog && currentStore != null)`
   - Solution: Ensure both conditions are true
   - Code: Add null checks

3. **Recomposition issue**
   - Check: State changes not triggering recompose
   - Solution: Use `remember` for state variables
   - Code: `var showRatingDialog by remember { mutableStateOf(false) }`

**Debug Steps**:
```kotlin
// Add logging to button click
Button(
    onClick = { 
        Log.d("StoreRating", "Button clicked")
        showRatingDialog = true 
        Log.d("StoreRating", "showRatingDialog set to: $showRatingDialog")
    },
    ...
)
```

---

### Issue 3: Star Selector Not Working
**Symptoms**: Clicking stars doesn't change rating

**Causes & Solutions**:
1. **Rating state not updating**
   - Check: `rating` state in RateStoreDialog
   - Solution: Verify `var rating by remember { mutableStateOf(currentRating) }`
   - Code: Ensure state is mutable

2. **Click handler not attached**
   - Check: `.clickable()` modifier on star icons
   - Solution: Verify modifier is present
   - Code: `.clickable(enabled = !isLoading) { rating = index + 1 }`

3. **Loading state blocking clicks**
   - Check: `isLoading` parameter
   - Solution: Disable clicks during submission
   - Code: `enabled = !isLoading`

**Debug Steps**:
```kotlin
// In RateStoreDialog
repeat(5) { index ->
    Icon(
        ...
        modifier = Modifier
            .clickable(enabled = !isLoading) { 
                Log.d("StoreRating", "Star $index clicked")
                rating = index + 1 
            }
    )
}
```

---

### Issue 4: Submit Button Disabled
**Symptoms**: "Submit Rating" button is grayed out

**Causes & Solutions**:
1. **No rating selected**
   - Check: `rating > 0`
   - Solution: Select at least 1 star
   - Code: `enabled = rating > 0 && !isLoading`

2. **Loading state active**
   - Check: `isLoading` is true
   - Solution: Wait for previous submission to complete
   - Code: Check `ratingState is StoreRatingState.Loading`

3. **Button condition wrong**
   - Check: Button `enabled` parameter
   - Solution: Verify condition logic
   - Code: `enabled = rating > 0 && !isLoading`

**Debug Steps**:
```kotlin
Button(
    onClick = { onSubmit(rating, review) },
    enabled = rating > 0 && !isLoading,
    ...
) {
    Log.d("StoreRating", "Button enabled: ${rating > 0 && !isLoading}")
}
```

---

### Issue 5: Rating Not Submitting
**Symptoms**: Click submit but nothing happens

**Causes & Solutions**:
1. **Firebase not connected**
   - Check: Network connectivity
   - Solution: Verify internet connection
   - Code: Check Firebase initialization

2. **Firestore security rules blocking write**
   - Check: Firebase console > Firestore > Rules
   - Solution: Update rules to allow authenticated writes
   - Code:
   ```
   match /store_ratings/{document=**} {
     allow read: if request.auth != null;
     allow create: if request.auth != null;
     allow update: if request.auth.uid == resource.data.buyer_id;
   }
   ```

3. **Repository error not caught**
   - Check: Error handling in ViewModel
   - Solution: Add try-catch and logging
   - Code: Check `Result.failure()` handling

4. **ViewModel not calling repository**
   - Check: `submitRating()` implementation
   - Solution: Verify coroutine scope
   - Code: Use `viewModelScope.launch`

**Debug Steps**:
```kotlin
// In StoreRatingViewModel
fun submitRating(...) {
    viewModelScope.launch {
        try {
            Log.d("StoreRating", "Submitting rating...")
            _ratingState.value = StoreRatingState.Loading
            
            val result = storeRatingRepository.submitRating(...)
            Log.d("StoreRating", "Result: $result")
            
            if (result.isSuccess) {
                Log.d("StoreRating", "Success!")
                _ratingState.value = StoreRatingState.Success(...)
            } else {
                Log.e("StoreRating", "Error: ${result.exceptionOrNull()}")
                _ratingState.value = StoreRatingState.Error(...)
            }
        } catch (e: Exception) {
            Log.e("StoreRating", "Exception: ${e.message}", e)
            _ratingState.value = StoreRatingState.Error(e.message ?: "Unknown error")
        }
    }
}
```

---

### Issue 6: Success Message Not Showing
**Symptoms**: Rating submitted but no confirmation

**Causes & Solutions**:
1. **Snackbar not configured**
   - Check: `SnackbarHost` in Scaffold
   - Solution: Verify snackbar setup
   - Code: `snackbarHost = { SnackbarHost(snackbarHostState) }`

2. **LaunchedEffect not triggered**
   - Check: `LaunchedEffect(ratingState)`
   - Solution: Verify state changes
   - Code: Ensure `ratingState` is in dependency list

3. **Message not set**
   - Check: `StoreRatingState.Success` message
   - Solution: Verify message is not empty
   - Code: `StoreRatingState.Success("Thank you for rating!")`

**Debug Steps**:
```kotlin
LaunchedEffect(ratingState) {
    Log.d("StoreRating", "ratingState changed: $ratingState")
    when (ratingState) {
        is StoreRatingState.Success -> {
            Log.d("StoreRating", "Showing success message")
            snackbarHostState.showSnackbar(
                message = (ratingState as StoreRatingState.Success).message,
                duration = SnackbarDuration.Short
            )
        }
        ...
    }
}
```

---

### Issue 7: Average Rating Not Updating
**Symptoms**: Store rating shows old value after submission

**Causes & Solutions**:
1. **Average not recalculated**
   - Check: `recalculateStoreRating()` called
   - Solution: Verify it's called after submission
   - Code: Call in `submitRating()` after save

2. **Store data not refreshed**
   - Check: `loadStoreDetails()` called
   - Solution: Reload store after rating
   - Code: Add to success handler

3. **Firestore update failed**
   - Check: Firebase console for errors
   - Solution: Verify write permissions
   - Code: Check security rules

**Debug Steps**:
```kotlin
// In StoreRatingRepository
private suspend fun recalculateStoreRating(storeId: String) {
    try {
        Log.d("StoreRating", "Recalculating rating for store: $storeId")
        val ratings = ratingsCollection
            .whereEqualTo("store_id", storeId)
            .get()
            .await()
        
        Log.d("StoreRating", "Found ${ratings.size()} ratings")
        
        val averageRating = ratings.documents.mapNotNull { 
            it.getLong("rating")?.toInt() 
        }.average()
        
        Log.d("StoreRating", "New average: $averageRating")
        
        storesCollection.document(storeId).update(
            mapOf("average_rating" to averageRating)
        ).await()
        
        Log.d("StoreRating", "Store updated successfully")
    } catch (e: Exception) {
        Log.e("StoreRating", "Error recalculating: ${e.message}", e)
    }
}
```

---

### Issue 8: Existing Rating Not Loading
**Symptoms**: "Update Your Rating" button shows but dialog is empty

**Causes & Solutions**:
1. **Buyer rating not loaded**
   - Check: `loadBuyerRating()` called
   - Solution: Verify it's called on screen load
   - Code: `LaunchedEffect(storeId) { loadBuyerRating(...) }`

2. **Rating data not passed to dialog**
   - Check: `currentRating` and `currentReview` parameters
   - Solution: Verify values passed correctly
   - Code: `currentRating = buyerRating?.rating ?: 0`

3. **State not updating**
   - Check: `buyerRating` state
   - Solution: Verify `collectAsState()` is working
   - Code: `val buyerRating by storeRatingViewModel.buyerRating.collectAsState()`

**Debug Steps**:
```kotlin
LaunchedEffect(storeId) {
    Log.d("StoreRating", "Loading buyer rating for store: $storeId")
    storeRatingViewModel.loadBuyerRating(storeId, currentUserId)
}

// In dialog
Log.d("StoreRating", "Current rating: ${buyerRating?.rating}")
Log.d("StoreRating", "Current review: ${buyerRating?.review}")
```

---

## Firebase Security Rules

### Correct Rules for Store Ratings:
```javascript
match /store_ratings/{document=**} {
  // Allow anyone to read ratings
  allow read: if request.auth != null;
  
  // Allow authenticated users to create ratings
  allow create: if request.auth != null 
    && request.resource.data.buyer_id == request.auth.uid;
  
  // Allow users to update only their own ratings
  allow update: if request.auth != null 
    && resource.data.buyer_id == request.auth.uid;
  
  // Prevent deletion
  allow delete: if false;
}

match /co_seller_stores/{document=**} {
  // Allow anyone to read store info
  allow read: if true;
  
  // Only allow updates to average_rating and rating_count
  allow update: if request.resource.data.average_rating is number
    && request.resource.data.rating_count is number;
}
```

---

## Logging Checklist

Enable these logs to debug issues:
```kotlin
// In StorePublicViewScreen
Log.d("StoreRating", "Screen loaded")
Log.d("StoreRating", "currentUserId: $currentUserId")
Log.d("StoreRating", "currentStore: ${currentStore?.storeName}")
Log.d("StoreRating", "buyerRating: $buyerRating")
Log.d("StoreRating", "ratingState: $ratingState")

// In RateStoreDialog
Log.d("StoreRating", "Dialog opened")
Log.d("StoreRating", "Rating selected: $rating")
Log.d("StoreRating", "Review text: $review")

// In StoreRatingViewModel
Log.d("StoreRating", "submitRating called")
Log.d("StoreRating", "ratingState: $ratingState")

// In StoreRatingRepository
Log.d("StoreRating", "submitRating in repository")
Log.d("StoreRating", "Checking existing ratings...")
Log.d("StoreRating", "Recalculating average...")
```

---

## Quick Fixes

| Problem | Quick Fix |
|---------|-----------|
| Button not showing | Check `currentUserId` is not empty |
| Dialog won't open | Verify `currentStore` is not null |
| Stars not clickable | Check `isLoading` is false |
| Submit disabled | Select at least 1 star |
| Not submitting | Check Firebase connection |
| No success message | Verify snackbar is configured |
| Rating not updating | Reload store details |
| Old rating showing | Clear app cache |

---

## Contact & Support

For additional help:
1. Check Firebase console for errors
2. Review Firestore security rules
3. Check network connectivity
4. Clear app cache and reload
5. Check browser console for JavaScript errors
6. Verify all files are in correct locations

**Status**: ✅ All troubleshooting steps documented
