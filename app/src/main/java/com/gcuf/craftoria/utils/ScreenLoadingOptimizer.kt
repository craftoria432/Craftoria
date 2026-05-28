package com.gcuf.craftoria.utils

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ScreenLoadingOptimizer<T>(
    private val cacheKey: String,
    private val emptyValue: T,
    private val cacheDurationMs: Long = 5 * 60 * 1000 // 5 minutes default
) {
    private data class CachedData<T>(
        val data: T,
        val timestamp: Long
    )

    companion object {
        private const val TAG = "ScreenLoadingOptimizer"

        // Global in-memory cache shared across all instances
        private val globalCache = mutableMapOf<String, CachedData<*>>()

        fun clearAllCaches() {
            globalCache.clear()
            Log.d(TAG, "🗑️ All caches cleared")
        }

        fun getCacheStats(): Map<String, Long> {
            return globalCache.mapValues { (it.value as CachedData<*>).timestamp }
        }
    }

    private val _data = MutableStateFlow(emptyValue)
    val data: StateFlow<T> = _data.asStateFlow()

    private val _isFirstLoad = MutableStateFlow(true)
    val isFirstLoad: StateFlow<Boolean> = _isFirstLoad.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        // Load cached data immediately on initialization
        loadFromCache()
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadFromCache() {
        val cached = globalCache[cacheKey] as? CachedData<T>
        if (cached != null) {
            val age = System.currentTimeMillis() - cached.timestamp
            if (age < cacheDurationMs) {
                _data.value = cached.data
                _isFirstLoad.value = false
                Log.d(TAG, "✅ Loaded cached data for $cacheKey (age: ${age}ms)")
            } else {
                Log.d(TAG, "⏰ Cache expired for $cacheKey (age: ${age}ms)")
                globalCache.remove(cacheKey)
            }
        } else {
            Log.d(TAG, "📭 No cache found for $cacheKey")
        }
    }

    suspend fun loadData(
        forceRefresh: Boolean = false,
        dataLoader: suspend () -> Result<T>
    ) {
        try {
            val isFirst = _isFirstLoad.value

            if (isFirst) {
                Log.d(TAG, "🔄 First load for $cacheKey - showing loading")
            } else {
                Log.d(TAG, "🔄 Background refresh for $cacheKey - showing cached data")
                _isRefreshing.value = true
            }

            val result = dataLoader()

            if (result.isSuccess) {
                val newData = result.getOrNull() ?: emptyValue
                _data.value = newData
                _isFirstLoad.value = false
                _isRefreshing.value = false
                globalCache[cacheKey] = CachedData(newData, System.currentTimeMillis())
                Log.d(TAG, "✅ Data loaded and cached for $cacheKey")
            } else {
                Log.e(TAG, "❌ Failed to load data for $cacheKey: ${result.exceptionOrNull()?.message}")
                _isRefreshing.value = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading data for $cacheKey", e)
            _isRefreshing.value = false
        }
    }

    fun clearCache() {
        globalCache.remove(cacheKey)
        _data.value = emptyValue
        _isFirstLoad.value = true
        Log.d(TAG, "🗑️ Cache cleared for $cacheKey")
    }

    fun updateData(newData: T) {
        _data.value = newData
        globalCache[cacheKey] = CachedData(newData, System.currentTimeMillis())
        Log.d(TAG, "✅ Data updated directly for $cacheKey")
    }
}

class InstantLoadingStateManager {
    private val _showLoading = MutableStateFlow(true)
    val showLoading: StateFlow<Boolean> = _showLoading.asStateFlow()

    private val _showRefreshIndicator = MutableStateFlow(false)
    val showRefreshIndicator: StateFlow<Boolean> = _showRefreshIndicator.asStateFlow()

    private var hasLoadedOnce = false

    fun startLoading() {
        if (!hasLoadedOnce) {
            _showLoading.value = true
            Log.d("InstantLoadingState", "🔄 First load - showing loading")
        } else {
            _showRefreshIndicator.value = true
            Log.d("InstantLoadingState", "🔄 Refresh - showing indicator")
        }
    }

    fun stopLoading() {
        _showLoading.value = false
        _showRefreshIndicator.value = false
        hasLoadedOnce = true
        Log.d("InstantLoadingState", "✅ Loading complete")
    }

    fun reset() {
        _showLoading.value = true
        _showRefreshIndicator.value = false
        hasLoadedOnce = false
        Log.d("InstantLoadingState", "🔄 State reset")
    }
}

object ScreenTransitionOptimizer {
    private const val TAG = "ScreenTransitionOptimizer"

    suspend fun preloadScreen(
        screenKey: String,
        preloadAction: suspend () -> Unit
    ) {
        try {
            Log.d(TAG, "🚀 Preloading screen: $screenKey")
            preloadAction()
            Log.d(TAG, "✅ Screen preloaded: $screenKey")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to preload screen: $screenKey", e)
        }
    }
}