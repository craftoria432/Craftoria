package com.gcuf.craftoria.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcuf.craftoria.data.model.StoreRating
import com.gcuf.craftoria.data.repository.StoreRatingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class StoreRatingState {
    object Idle : StoreRatingState()
    object Loading : StoreRatingState()
    data class Success(val message: String) : StoreRatingState()
    data class Error(val message: String) : StoreRatingState()
}

class StoreRatingViewModel(
    private val storeRatingRepository: StoreRatingRepository = StoreRatingRepository()
) : ViewModel() {

    private val _ratingState = MutableStateFlow<StoreRatingState>(StoreRatingState.Idle)
    val ratingState: StateFlow<StoreRatingState> = _ratingState.asStateFlow()

    private val _buyerRating = MutableStateFlow<StoreRating?>(null)
    val buyerRating: StateFlow<StoreRating?> = _buyerRating.asStateFlow()

    private val _storeRatings = MutableStateFlow<List<StoreRating>>(emptyList())
    val storeRatings: StateFlow<List<StoreRating>> = _storeRatings.asStateFlow()

    companion object {
        private const val TAG = "StoreRatingViewModel"
    }

    fun submitRating(
        storeId: String,
        buyerId: String,
        rating: Int,
        review: String,
        buyerName: String = ""
    ) {
        viewModelScope.launch {
            try {
                _ratingState.value = StoreRatingState.Loading

                val result = storeRatingRepository.submitRating(
                    storeId = storeId,
                    buyerId = buyerId,
                    rating = rating,
                    review = review,
                    buyerName = buyerName
                )

                if (result.isSuccess) {
                    _ratingState.value = StoreRatingState.Success(
                        if (_buyerRating.value != null) "Rating updated successfully!" else "Thank you for rating!"
                    )
                    // Refresh buyer rating
                    loadBuyerRating(storeId, buyerId)
                    // Refresh all ratings
                    loadStoreRatings(storeId)
                } else {
                    _ratingState.value = StoreRatingState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to submit rating"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error submitting rating", e)
                _ratingState.value = StoreRatingState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun loadBuyerRating(storeId: String, buyerId: String) {
        viewModelScope.launch {
            try {
                val result = storeRatingRepository.getBuyerRating(storeId, buyerId)

                if (result.isSuccess) {
                    _buyerRating.value = result.getOrNull()
                } else {
                    Log.e(TAG, "Failed to load buyer rating", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading buyer rating", e)
            }
        }
    }

    fun loadStoreRatings(storeId: String) {
        viewModelScope.launch {
            try {
                val result = storeRatingRepository.getStoreRatings(storeId)

                if (result.isSuccess) {
                    _storeRatings.value = result.getOrNull() ?: emptyList()
                } else {
                    Log.e(TAG, "Failed to load store ratings", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading store ratings", e)
            }
        }
    }

    fun resetState() {
        _ratingState.value = StoreRatingState.Idle
    }
}
