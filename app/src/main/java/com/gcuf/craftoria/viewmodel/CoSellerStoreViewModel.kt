package com.gcuf.craftoria.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gcuf.craftoria.data.model.*
import com.gcuf.craftoria.data.repository.CoSellerStoreRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CoSellerStoreViewModel(
    private val storeRepository: CoSellerStoreRepository = CoSellerStoreRepository() ) : ViewModel()
{

    companion object {
        private const val TAG = "CoSellerStoreViewModel"
    }

    private val firestore = FirebaseFirestore.getInstance()

    // Realtime listeners
    private var userStoresListener: ListenerRegistration? = null
    private var activeStoresListener: ListenerRegistration? = null

    private val _uiState = MutableStateFlow<CoSellerStoreState>(CoSellerStoreState.Idle)
    val uiState: StateFlow<CoSellerStoreState> = _uiState.asStateFlow()

    private val _stores = MutableStateFlow<List<CoSellerStore>>(emptyList())
    val stores: StateFlow<List<CoSellerStore>> = _stores.asStateFlow()

    private val _currentStore = MutableStateFlow<CoSellerStore?>(null)
    val currentStore: StateFlow<CoSellerStore?> = _currentStore.asStateFlow()

    private val _storeMembers = MutableStateFlow<List<StoreMember>>(emptyList())
    val storeMembers: StateFlow<List<StoreMember>> = _storeMembers.asStateFlow()

    private val _storeProducts = MutableStateFlow<List<Product>>(emptyList())
    val storeProducts: StateFlow<List<Product>> = _storeProducts.asStateFlow()

    private val _storeInvitations = MutableStateFlow<List<StoreInvitation>>(emptyList())
    val storeInvitations: StateFlow<List<StoreInvitation>> = _storeInvitations.asStateFlow()

    // ✅ Active public stores (used on Buyer HomeScreen)
    private val _activeStores = MutableStateFlow<List<CoSellerStore>>(emptyList())
    val activeStores: StateFlow<List<CoSellerStore>> = _activeStores.asStateFlow()

    // ✅ Loading state for AllStoresScreen
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadUserStores(userId: String) {
        // Switch to realtime listener: listen to all active co_seller_stores and filter on client
        userStoresListener?.remove()
        _uiState.value = CoSellerStoreState.Loading

        userStoresListener = firestore.collection("co_seller_stores")
            .whereEqualTo("is_active", true)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Failed to observe user stores", e)
                    _uiState.value = CoSellerStoreState.Error(e.message ?: "Failed to load stores")
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    _stores.value = emptyList()
                    _uiState.value = CoSellerStoreState.Empty
                    return@addSnapshotListener
                }

                val allStores = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(CoSellerStore::class.java)?.copy(id = doc.id)
                }

                // Filter: user is owner or member
                val userStores = allStores.filter { store ->
                    store.ownerId == userId || store.memberIds.contains(userId)
                }.distinctBy { it.id }

                _stores.value = userStores
                _uiState.value = if (userStores.isEmpty()) {
                    CoSellerStoreState.Empty
                } else {
                    CoSellerStoreState.Success
                }
            }
    }

    // ✅ LOAD ALL ACTIVE STORES (Buyer Side) – now realtime
    fun loadAllActiveStores() {
        if (activeStoresListener != null) return  // already observing

        _isLoading.value = true
        activeStoresListener = firestore.collection("co_seller_stores")
            .whereEqualTo("is_active", true)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Failed to observe active stores", e)
                    _isLoading.value = false
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    _activeStores.value = emptyList()
                    _isLoading.value = false
                    return@addSnapshotListener
                }

                val stores = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(CoSellerStore::class.java)?.copy(id = doc.id)
                }
                
                // ✅ NEW: Filter out stores whose owners are deleted
                viewModelScope.launch {
                    try {
                        val filteredStores = stores.filter { store ->
                            val ownerDoc = firestore.collection("users").document(store.ownerId).get().await()
                            val ownerStatus = ownerDoc.getString("status") ?: ""
                            ownerStatus != "deleted"
                        }
                        _activeStores.value = filteredStores
                        _isLoading.value = false
                        Log.d(TAG, "✅ Realtime: loaded ${filteredStores.size} active stores (${stores.size} before filtering)")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error filtering active stores", e)
                        _activeStores.value = stores // Fallback to unfiltered
                        _isLoading.value = false
                    }
                }
            }
    }

    fun createStore(
        context: Context,
        store: CoSellerStore,
        logoUri: Uri?,
        bannerUri: Uri?,
        invitedEmails: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = CoSellerStoreState.Loading

                val result = storeRepository.createStore(
                    context = context,
                    store = store,
                    logoUri = logoUri,
                    bannerUri = bannerUri,
                    invitedEmails = invitedEmails
                )

                if (result.isSuccess) {
                    _uiState.value = CoSellerStoreState.ActionSuccess(
                        "Store created successfully!",
                        result.getOrNull() ?: ""
                    )
                } else {
                    _uiState.value = CoSellerStoreState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to create store"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create store", e)
                _uiState.value = CoSellerStoreState.Error(e.message ?: "Failed to create store")
            }
        }
    }

    fun loadStoreDetails(storeId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = CoSellerStoreState.Loading

                val storeResult = storeRepository.getStoreById(storeId)
                if (storeResult.isSuccess) {
                    _currentStore.value = storeResult.getOrNull()
                }

                val membersResult = storeRepository.getStoreMembers(storeId)
                if (membersResult.isSuccess) {
                    _storeMembers.value = membersResult.getOrNull() ?: emptyList()
                }

                val productsResult = storeRepository.getStoreProducts(storeId)
                if (productsResult.isSuccess) {
                    _storeProducts.value = productsResult.getOrNull() ?: emptyList()
                }

                val invitationsResult = storeRepository.getStoreInvitations(storeId)
                if (invitationsResult.isSuccess) {
                    _storeInvitations.value = invitationsResult.getOrNull() ?: emptyList()
                }

                _uiState.value = CoSellerStoreState.Success
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load store details", e)
                _uiState.value = CoSellerStoreState.Error(e.message ?: "Failed to load store")
            }
        }
    }

    fun updateStore(
        context: Context,
        storeId: String,
        store: CoSellerStore,
        newLogoUri: Uri?,
        newBannerUri: Uri?
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = CoSellerStoreState.Loading

                val result = storeRepository.updateStore(context, storeId, store, newLogoUri, newBannerUri)

                if (result.isSuccess) {
                    loadStoreDetails(storeId)
                    _uiState.value = CoSellerStoreState.ActionSuccess("Store updated successfully!", storeId)
                } else {
                    _uiState.value = CoSellerStoreState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to update store"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update store", e)
                _uiState.value = CoSellerStoreState.Error(e.message ?: "Failed to update store")
            }
        }
    }

    fun deleteStore(storeId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = CoSellerStoreState.Loading

                val result = storeRepository.deleteStore(storeId)

                if (result.isSuccess) {
                    _uiState.value = CoSellerStoreState.ActionSuccess("Store deleted successfully!", "")
                } else {
                    _uiState.value = CoSellerStoreState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to delete store"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete store", e)
                _uiState.value = CoSellerStoreState.Error(e.message ?: "Failed to delete store")
            }
        }
    }

    fun sendInvitation(invitation: StoreInvitation) {
        viewModelScope.launch {
            try {
                val result = storeRepository.sendInvitation(invitation)

                if (result.isSuccess) {
                    loadStoreDetails(invitation.storeId)
                    _uiState.value = CoSellerStoreState.ActionSuccess("Invitation sent!", "")
                } else {
                    _uiState.value = CoSellerStoreState.Error("Failed to send invitation")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send invitation", e)
                _uiState.value = CoSellerStoreState.Error("Failed to send invitation")
            }
        }
    }

    suspend fun acceptInvitationAsync(invitationId: String, userId: String, userName: String): Result<Unit> {
        return try {
            val result = storeRepository.acceptInvitation(invitationId, userId, userName)
            
            if (result.isSuccess) {
                _uiState.value = CoSellerStoreState.ActionSuccess("Invitation accepted!", "")
            } else {
                _uiState.value = CoSellerStoreState.Error("Failed to accept invitation")
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to accept invitation", e)
            _uiState.value = CoSellerStoreState.Error("Failed to accept invitation")
            Result.failure(e)
        }
    }

    fun acceptInvitation(invitationId: String, userId: String, userName: String) {
        viewModelScope.launch {
            acceptInvitationAsync(invitationId, userId, userName)
        }
    }

    fun removeMember(storeId: String, userId: String) {
        viewModelScope.launch {
            try {
                val result = storeRepository.removeMember(storeId, userId)

                if (result.isSuccess) {
                    loadStoreDetails(storeId)
                    _uiState.value = CoSellerStoreState.ActionSuccess("Member removed successfully!", "")
                } else {
                    _uiState.value = CoSellerStoreState.Error("Failed to remove member")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove member", e)
                _uiState.value = CoSellerStoreState.Error("Failed to remove member")
            }
        }
    }

    fun updatePaymentSplitConfig(storeId: String, splitConfig: Map<String, Double>) {
        viewModelScope.launch {
            val result = storeRepository.updatePaymentSplitConfig(storeId, splitConfig)
            if (result.isSuccess) {
                _uiState.value = CoSellerStoreState.ActionSuccess("Earnings split saved successfully", storeId)
                loadStoreDetails(storeId)
            } else {
                _uiState.value = CoSellerStoreState.Error(
                    result.exceptionOrNull()?.message ?: "Failed to save split config"
                )
            }
        }
    }

    fun deleteProduct(productId: String, storeId: String) {
        viewModelScope.launch {
            try {
                val result = storeRepository.deleteProduct(productId, storeId)

                if (result.isSuccess) {
                    loadStoreDetails(storeId)
                    _uiState.value = CoSellerStoreState.ActionSuccess("Product deleted successfully!", "")
                } else {
                    _uiState.value = CoSellerStoreState.Error("Failed to delete product")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete product", e)
                _uiState.value = CoSellerStoreState.Error("Failed to delete product")
            }
        }
    }

    fun leaveStore(storeId: String, userId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = CoSellerStoreState.Loading

                val result = storeRepository.leaveStore(storeId, userId)

                if (result.isSuccess) {
                    _uiState.value = CoSellerStoreState.ActionSuccess("You have left the store successfully!", "")
                } else {
                    _uiState.value = CoSellerStoreState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to leave store"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to leave store", e)
                _uiState.value = CoSellerStoreState.Error(e.message ?: "Failed to leave store")
            }
        }
    }

    fun resetState() {
        _uiState.value = CoSellerStoreState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        userStoresListener?.remove()
        activeStoresListener?.remove()
    }

}

sealed class CoSellerStoreState
{
    object Idle : CoSellerStoreState()
    object Loading : CoSellerStoreState()
    object Success : CoSellerStoreState()
    object Empty : CoSellerStoreState()
    data class ActionSuccess(val message: String, val storeId: String) : CoSellerStoreState()
    data class Error(val message: String) : CoSellerStoreState()
}