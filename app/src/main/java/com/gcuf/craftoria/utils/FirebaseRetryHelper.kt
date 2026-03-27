package com.gcuf.craftoria.utils

import android.util.Log
import kotlinx.coroutines.delay

/**
 * Retry configuration for Firebase operations
 */
data class RetryConfig(
    val maxRetries: Int = 3,
    val initialDelayMs: Long = 1000,
    val maxDelayMs: Long = 10000,
    val backoffMultiplier: Double = 2.0
)

/**
 * Helper for retrying Firebase operations with exponential backoff
 */
object FirebaseRetryHelper {
    
    /**
     * Retry an async operation with exponential backoff
     * @param operationName Name for logging
     * @param config Retry configuration
     * @param operation The operation to retry
     * @return Result of the operation
     */
    suspend inline fun <T> withRetry(
        operationName: String,
        config: RetryConfig = RetryConfig(),
        crossinline operation: suspend () -> T
    ): T {
        var lastError: Exception? = null
        var delay = config.initialDelayMs
        
        repeat(config.maxRetries + 1) { attempt ->
            try {
                return operation()
            } catch (e: Exception) {
                lastError = e
                
                // Don't retry on auth errors
                if (e.message?.contains("permission-denied") == true || 
                    e.message?.contains("unauthenticated") == true) {
                    throw e
                }
                
                if (attempt < config.maxRetries) {
                    Log.w(
                        "FirebaseRetryHelper",
                        "$operationName failed (attempt ${attempt + 1}/${config.maxRetries + 1}). " +
                        "Retrying in ${delay}ms...",
                        e
                    )
                    delay(delay)
                    delay = (delay * config.backoffMultiplier).toLong()
                        .coerceAtMost(config.maxDelayMs)
                }
            }
        }
        
        throw lastError ?: Exception("$operationName failed after ${config.maxRetries + 1} attempts")
    }
    
    /**
     * Retry a blocking operation with exponential backoff
     */
    inline fun <T> withRetryBlocking(
        operationName: String,
        config: RetryConfig = RetryConfig(),
        crossinline operation: () -> T
    ): T {
        var lastError: Exception? = null
        var delay = config.initialDelayMs
        
        repeat(config.maxRetries + 1) { attempt ->
            try {
                return operation()
            } catch (e: Exception) {
                lastError = e
                
                if (e.message?.contains("permission-denied") == true || 
                    e.message?.contains("unauthenticated") == true) {
                    throw e
                }
                
                if (attempt < config.maxRetries) {
                    Log.w(
                        "FirebaseRetryHelper",
                        "$operationName failed (attempt ${attempt + 1}/${config.maxRetries + 1}). " +
                        "Retrying in ${delay}ms...",
                        e
                    )
                    Thread.sleep(delay)
                    delay = (delay * config.backoffMultiplier).toLong()
                        .coerceAtMost(config.maxDelayMs)
                }
            }
        }
        
        throw lastError ?: Exception("$operationName failed after ${config.maxRetries + 1} attempts")
    }
}

/**
 * Extension function for Flow-based operations
 */
suspend inline fun <T> retryableFirebaseOperation(
    operationName: String,
    config: RetryConfig = RetryConfig(),
    crossinline operation: suspend () -> T
): Result<T> = try {
    Result.success(
        FirebaseRetryHelper.withRetry(operationName, config) {
            operation()
        }
    )
} catch (e: Exception) {
    Log.e("FirebaseRetryHelper", "Operation $operationName failed permanently", e)
    Result.failure(e)
}
