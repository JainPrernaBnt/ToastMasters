package com.bntsoft.toastmasters.utils

import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey

object GlideExtensions {

    private const val TAG = "GlideExtensions"

    fun loadProfilePicture(imageView: ImageView, profilePictureUrl: String?, placeholderRes: Int) {
        try {
            Log.d(TAG, "Loading profile picture: ${if (profilePictureUrl.isNullOrEmpty()) "null/empty" else "has data (${profilePictureUrl.length} chars)"}")
            if (!profilePictureUrl.isNullOrEmpty()) {
                if (isContentUri(profilePictureUrl) || isFileUri(profilePictureUrl)) {
                    Log.d(TAG, "Loading URI image")
                    loadUriImage(imageView, profilePictureUrl, placeholderRes)
                } else if (profilePictureUrl.startsWith("http")) {
                    Log.d(TAG, "Loading URL image")
                    loadUrlImage(imageView, profilePictureUrl, placeholderRes)
                } else {
                    Log.d(TAG, "Loading base64 image")
                    loadBase64Image(imageView, profilePictureUrl, placeholderRes)
                }
            } else {
                Log.d(TAG, "No profile picture, showing placeholder")
                // No profile picture, show placeholder
                loadPlaceholder(imageView, placeholderRes)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading profile picture", e)
            loadPlaceholder(imageView, placeholderRes)
        }
    }

    private fun loadBase64Image(imageView: ImageView, base64String: String, placeholderRes: Int) {
        try {
            val bitmap = ImageUtils.base64ToBitmap(base64String)
            if (bitmap != null && !bitmap.isRecycled) {
                val requestOptions = RequestOptions()
                    .circleCrop()
                    .placeholder(placeholderRes)
                    .error(placeholderRes)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)

                Glide.with(imageView.context)
                    .load(bitmap)
                    .apply(requestOptions)
                    .into(imageView)
            } else {
                Log.w(TAG, "Failed to decode base64 image, showing placeholder")
                loadPlaceholder(imageView, placeholderRes)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading base64 image", e)
            loadPlaceholder(imageView, placeholderRes)
        }
    }

    private fun loadUrlImage(imageView: ImageView, url: String, placeholderRes: Int) {
        try {
            val requestOptions = RequestOptions()
                .circleCrop()
                .placeholder(placeholderRes)
                .error(placeholderRes)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)

            Glide.with(imageView.context)
                .load(url)
                .apply(requestOptions)
                .signature(ObjectKey(System.currentTimeMillis()))
                .into(imageView)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading URL image", e)
            loadPlaceholder(imageView, placeholderRes)
        }
    }


    private fun loadUriImage(imageView: ImageView, uri: String, placeholderRes: Int) {
        try {
            val requestOptions = RequestOptions()
                .circleCrop()
                .placeholder(placeholderRes)
                .error(placeholderRes)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)

            Glide.with(imageView.context)
                .load(uri)
                .apply(requestOptions)
                .into(imageView)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading URI image", e)
            loadPlaceholder(imageView, placeholderRes)
        }
    }

    private fun isContentUri(value: String): Boolean {
        return value.startsWith("content://")
    }

    private fun isFileUri(value: String): Boolean {
        return value.startsWith("file://")
    }

    private fun loadPlaceholder(imageView: ImageView, placeholderRes: Int) {
        try {
            Glide.with(imageView.context)
                .load(placeholderRes)
                .circleCrop()
                .into(imageView)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading placeholder", e)
            // Fallback to setting drawable directly
            imageView.setImageResource(placeholderRes)
        }
    }
}
