package com.gcuf.craftoria.utils

import android.util.Log
import kotlinx.coroutines.delay

class PaymentRetryManager {
    companion object {
        private const val TAG = "PaymentRetryManager"
        private const val MAX_RETRIES = 3
        private const val INITIAL_DELAY_MS = 1000L
        private const val MAX_DELAY_MS = 10000L
    }

    suspend fun <T> executeWithRetry(
        maxRetries: Int = MAX_RETRIES,
        operation: suspend () -> Result<T>
    ): Result<T> {
        var lastException: Exception? = null
        var delayMs = INITIAL_DELAY_MS

        repeat(maxRetries) { attempt ->
            try {
                Log.d(TAG, "🔄 Attempt ${attempt + 1}/$maxRetries")
                val result = operation()

                if (result.isSuccess) {
                    Log.d(TAG, "✅ Operation succeeded on attempt ${attempt + 1}")
                    return result
                } else {
                    lastException = result.exceptionOrNull() as? Exception
                    Log.w(TAG, "⚠️ Attempt ${attempt + 1} failed: ${lastException?.message}")
                }
            } catch (e: Exception) {
                lastException = e
                Log.e(TAG, "❌ Exception on attempt ${attempt + 1}: ${e.message}", e)
            }

            if (attempt < maxRetries - 1) {
                Log.d(TAG, "⏳ Waiting ${delayMs}ms before retry...")
                delay(delayMs)
                delayMs = (delayMs * 2).coerceAtMost(MAX_DELAY_MS)
            }
        }

        val errorMsg = "Operation failed after $maxRetries attempts"
        Log.e(TAG, "❌ $errorMsg")
        return Result.failure(lastException ?: Exception(errorMsg))
    }

    suspend fun <T> executeWithExponentialBackoff(
        maxRetries: Int = MAX_RETRIES,
        initialDelayMs: Long = INITIAL_DELAY_MS,
        operation: suspend () -> Result<T>
    ): Result<T> {
        var lastException: Exception? = null
        var delayMs = initialDelayMs

        repeat(maxRetries) { attempt ->
            try {
                Log.d(TAG, "🔄 Exponential backoff attempt ${attempt + 1}/$maxRetries")
                val result = operation()

                if (result.isSuccess) {
                    Log.d(TAG, "✅ Operation succeeded on attempt ${attempt + 1}")
                    return result
                } else {
                    lastException = result.exceptionOrNull() as? Exception
                    Log.w(TAG, "⚠️ Attempt ${attempt + 1} failed: ${lastException?.message}")
                }
            } catch (e: Exception) {
                lastException = e
                Log.e(TAG, "❌ Exception on attempt ${attempt + 1}: ${e.message}", e)
            }

            if (attempt < maxRetries - 1) {
                Log.d(TAG, "⏳ Waiting ${delayMs}ms before retry...")
                delay(delayMs)
                delayMs = (delayMs * 2).coerceAtMost(MAX_DELAY_MS)
            }
        }

        val errorMsg = "Operation failed after $maxRetries attempts with exponential backoff"
        Log.e(TAG, "❌ $errorMsg")
        return Result.failure(lastException ?: Exception(errorMsg))
    }

    suspend fun <T> executeWithJitter(
        maxRetries: Int = MAX_RETRIES,
        baseDelayMs: Long = INITIAL_DELAY_MS,
        operation: suspend () -> Result<T>
    ): Result<T> {
        var lastException: Exception? = null
        var delayMs = baseDelayMs

        repeat(maxRetries) { attempt ->
            try {
                Log.d(TAG, "🔄 Jitter retry attempt ${attempt + 1}/$maxRetries")
                val result = operation()

                if (result.isSuccess) {
                    Log.d(TAG, "✅ Operation succeeded on attempt ${attempt + 1}")
                    return result
                } else {
                    lastException = result.exceptionOrNull() as? Exception
                    Log.w(TAG, "⚠️ Attempt ${attempt + 1} failed: ${lastException?.message}")
                }
            } catch (e: Exception) {
                lastException = e
                Log.e(TAG, "❌ Exception on attempt ${attempt + 1}: ${e.message}", e)
            }

            if (attempt < maxRetries - 1) {
                // Add jitter: random value between 0 and delayMs
                val jitter = (Math.random() * delayMs).toLong()
                val actualDelay = (delayMs + jitter).coerceAtMost(MAX_DELAY_MS)
                Log.d(TAG, "⏳ Waiting ${actualDelay}ms (base: ${delayMs}ms, jitter: ${jitter}ms) before retry...")
                delay(actualDelay)
                delayMs = (delayMs * 2).coerceAtMost(MAX_DELAY_MS)
            }
        }

        val errorMsg = "Operation failed after $maxRetries attempts with jitter"
        Log.e(TAG, "❌ $errorMsg")
        return Result.failure(lastException ?: Exception(errorMsg))
    }
}
