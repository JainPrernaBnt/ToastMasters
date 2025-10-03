package com.bntsoft.toastmasters.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.InputStream

object ImageUtils {

    fun compressImageToBase64(
        context: Context,
        uri: Uri,
        maxSizeKB: Int = 200 // keep smaller so it's safe for Firestore
    ): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)

            // First pass: only get dimensions
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            // Scale down aggressively for profile pics
            val sampleSize = calculateSampleSize(options.outWidth, options.outHeight, 512, 512)

            val finalOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = false
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }

            val newInputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(newInputStream, null, finalOptions)
            newInputStream?.close()

            if (bitmap == null) return null

            var quality = 90
            var compressedData: ByteArray

            do {
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                compressedData = outputStream.toByteArray()
                quality -= 10
            } while (compressedData.size > maxSizeKB * 1024 && quality > 10)

            bitmap.recycle()

            if (compressedData.isEmpty()) return null

            // Convert to base64 (no line breaks)
            Base64.encodeToString(compressedData, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
    
    fun base64ToBitmap(base64String: String): Bitmap? {
        return try {
            // Clean the base64 string - remove data URI prefix if present
            val cleanBase64 = when {
                base64String.startsWith("data:image/") -> {
                    val commaIndex = base64String.indexOf(",")
                    if (commaIndex != -1) base64String.substring(commaIndex + 1) else base64String
                }
                else -> base64String
            }.trim()
            
            // Validate base64 string is not empty
            if (cleanBase64.isEmpty()) return null
            
            // Decode base64 to byte array (try NO_WRAP first, then DEFAULT)
            val decodedBytes = try {
                Base64.decode(cleanBase64, Base64.NO_WRAP)
            } catch (e: IllegalArgumentException) {
                Base64.decode(cleanBase64, Base64.DEFAULT)
            }
            
            // Validate decoded bytes
            if (decodedBytes.isEmpty()) return null
            
            // Create bitmap with error handling
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = false
                inPreferredConfig = Bitmap.Config.RGB_565 // Use less memory
                inSampleSize = 1
            }
            
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size, options)
        } catch (e: IllegalArgumentException) {
            // Invalid base64 string
            null
        } catch (e: OutOfMemoryError) {
            // Image too large
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
