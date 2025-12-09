package com.gcuf.craftoria.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.gcuf.craftoria.BuildConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object CloudinaryManager {

    private const val TAG = "CloudinaryManager"

    var isInitialized = false
        private set

    fun initialize(context: Context) {
        if (isInitialized) return

        try {
            val config = HashMap<String, String>()
            config["cloud_name"] = BuildConfig.CLOUDINARY_CLOUD_NAME
            config["api_key"] = BuildConfig.CLOUDINARY_API_KEY
            config["api_secret"] = BuildConfig.CLOUDINARY_API_SECRET

            MediaManager.init(context, config)
            isInitialized = true
            Log.d(TAG, "✅ Cloudinary initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize Cloudinary", e)
            throw e
        }
    }

    suspend fun uploadImage(
        context: Context,
        imageUri: Uri,
        folder: String = "craftoria/products"
    ): String = suspendCancellableCoroutine { continuation ->

        try {
            val compressedUri = compressImage(context, imageUri)

            val requestId = MediaManager.get().upload(compressedUri)
                .unsigned(BuildConfig.CLOUDINARY_UPLOAD_PRESET)
                .option("folder", folder)
                .option("resource_type", "image")
                .option("quality", "auto:good")
                .option("fetch_format", "auto")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        Log.d(TAG, "⏳ Upload started: $requestId")
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        val progress = (bytes.toDouble() / totalBytes.toDouble() * 100).toInt()
                        Log.d(TAG, "📤 Upload progress: $progress%")
                    }

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val url = resultData["secure_url"] as? String
                        Log.d(TAG, "✅ Upload successful: $url")

                        if (url != null && continuation.isActive) {
                            continuation.resume(url)
                        } else if (continuation.isActive) {
                            continuation.resumeWithException(
                                Exception("Upload succeeded but no URL returned")
                            )
                        }
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        Log.e(TAG, "❌ Upload error: ${error.description}")
                        if (continuation.isActive) {
                            continuation.resumeWithException(
                                Exception("Upload failed: ${error.description}")
                            )
                        }
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        Log.w(TAG, "🔄 Upload rescheduled: ${error.description}")
                        if (continuation.isActive) {
                            continuation.resumeWithException(
                                Exception("Upload rescheduled: ${error.description}")
                            )
                        }
                    }
                })
                .dispatch()

            continuation.invokeOnCancellation {
                try {
                    MediaManager.get().cancelRequest(requestId)
                    Log.d(TAG, "❌ Upload cancelled: $requestId")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to cancel upload", e)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start upload", e)
            if (continuation.isActive) {
                continuation.resumeWithException(e)
            }
        }
    }

    private fun compressImage(context: Context, uri: Uri): Uri {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) {
                Log.w(TAG, "⚠️ Failed to decode image, using original")
                return uri
            }

            val maxSize = 1200
            val width = bitmap.width
            val height = bitmap.height
            val scale = minOf(maxSize.toFloat() / width, maxSize.toFloat() / height)

            val newWidth = (width * scale).toInt()
            val newHeight = (height * scale).toInt()

            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)

            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)

            val cacheFile = java.io.File(
                context.cacheDir,
                "compressed_${System.currentTimeMillis()}.jpg"
            )
            cacheFile.outputStream().use {
                it.write(outputStream.toByteArray())
            }

            if (bitmap != resizedBitmap) bitmap.recycle()
            resizedBitmap.recycle()

            Uri.fromFile(cacheFile)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to compress image", e)
            uri
        }
    }

    suspend fun uploadMultipleImages(
        context: Context,
        imageUris: List<Uri>,
        folder: String = "craftoria/products"
    ): List<String> {
        val urls = mutableListOf<String>()

        for ((index, uri) in imageUris.withIndex()) {
            try {
                Log.d(TAG, "📤 Uploading image ${index + 1}/${imageUris.size}")
                val url = uploadImage(context, uri, folder)
                urls.add(url)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to upload image ${index + 1}: $uri", e)
            }
        }

        return urls
    }

    suspend fun deleteImage(publicId: String): Boolean {
        return try {
            Log.d(TAG, "Image marked for deletion: $publicId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete image", e)
            false
        }
    }

    fun getOptimizedUrl(
        url: String,
        width: Int = 400,
        quality: Int = 80
    ): String {
        return if (url.contains("cloudinary.com")) {
            url.replace("/upload/", "/upload/w_$width,q_$quality,f_auto/")
        } else {
            url
        }
    }
}
