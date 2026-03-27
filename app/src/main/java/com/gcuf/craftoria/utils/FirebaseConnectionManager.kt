package com.gcuf.craftoria.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Manages Firebase connection state and quality monitoring
 * Tracks online/offline status, auth state, and connection quality
 */
class FirebaseConnectionManager(private val context: Context) {
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    
    private val _connectionState = MutableLiveData<ConnectionState>()
    val connectionState: LiveData<ConnectionState> = _connectionState
    
    private val _connectionQuality = MutableLiveData<ConnectionQuality>()
    val connectionQuality: LiveData<ConnectionQuality> = _connectionQuality
    
    private val _isAuthenticated = MutableLiveData<Boolean>()
    val isAuthenticated: LiveData<Boolean> = _isAuthenticated
    
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var qualityCheckJob: kotlinx.coroutines.Job? = null
    
    init {
        setupNetworkMonitoring()
        setupAuthMonitoring()
        startQualityMonitoring()
    }
    
    /**
     * Setup network connectivity monitoring
     */
    private fun setupNetworkMonitoring() {
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                _connectionState.postValue(ConnectionState.ONLINE)
                startQualityMonitoring()
            }
            
            override fun onLost(network: Network) {
                super.onLost(network)
                _connectionState.postValue(ConnectionState.OFFLINE)
                _connectionQuality.postValue(ConnectionQuality.OFFLINE)
            }
            
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                super.onCapabilitiesChanged(network, capabilities)
                val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                if (hasInternet) {
                    _connectionState.postValue(ConnectionState.ONLINE)
                }
            }
        }
        
        try {
            connectivityManager.registerDefaultNetworkCallback(networkCallback!!)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseConnectionManager", "Error registering network callback", e)
        }
    }
    
    /**
     * Setup Firebase auth monitoring
     */
    private fun setupAuthMonitoring() {
        auth.addAuthStateListener { firebaseAuth ->
            _isAuthenticated.postValue(firebaseAuth.currentUser != null)
        }
    }
    
    /**
     * Start monitoring connection quality
     */
    private fun startQualityMonitoring() {
        qualityCheckJob?.cancel()
        qualityCheckJob = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                try {
                    val quality = checkConnectionQuality()
                    _connectionQuality.postValue(quality)
                    delay(30000) // Check every 30 seconds
                } catch (e: Exception) {
                    android.util.Log.e("FirebaseConnectionManager", "Error checking connection quality", e)
                    delay(30000)
                }
            }
        }
    }
    
    /**
     * Check connection quality by measuring latency
     */
    private suspend fun checkConnectionQuality(): ConnectionQuality {
        return try {
            val startTime = System.currentTimeMillis()
            val url = URL("https://www.google.com")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.requestMethod = "HEAD"
            
            val responseCode = connection.responseCode
            val latency = System.currentTimeMillis() - startTime
            
            connection.disconnect()
            
            when {
                responseCode != 200 -> ConnectionQuality.SLOW
                latency > 1000 -> ConnectionQuality.SLOW
                else -> ConnectionQuality.GOOD
            }
        } catch (e: IOException) {
            ConnectionQuality.OFFLINE
        } catch (e: Exception) {
            ConnectionQuality.SLOW
        }
    }
    
    /**
     * Get current connection state
     */
    fun isOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    
    /**
     * Get current auth state
     */
    fun isAuthenticated(): Boolean = auth.currentUser != null
    
    /**
     * Check if fully connected (online + authenticated)
     */
    fun isConnected(): Boolean = isOnline() && isAuthenticated()
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
            } catch (e: Exception) {
                android.util.Log.e("FirebaseConnectionManager", "Error unregistering network callback", e)
            }
        }
        qualityCheckJob?.cancel()
    }
}

/**
 * Connection state enum
 */
enum class ConnectionState {
    ONLINE,
    OFFLINE,
    CONNECTING
}

/**
 * Connection quality enum
 */
enum class ConnectionQuality {
    GOOD,
    SLOW,
    OFFLINE
}
