package com.bntsoft.toastmasters.utils

import android.graphics.Bitmap
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

object GlideExtensions {
    
    fun loadProfilePicture(imageView: ImageView, profilePictureUrl: String?, placeholderRes: Int) {
        if (!profilePictureUrl.isNullOrEmpty()) {
            // Check if it's a base64 string
            if (profilePictureUrl.startsWith("data:image") || !profilePictureUrl.startsWith("http")) {
                // It's a base64 string, convert to bitmap
                val bitmap = ImageUtils.base64ToBitmap(profilePictureUrl)
                if (bitmap != null) {
                    Glide.with(imageView.context)
                        .load(bitmap)
                        .circleCrop()
                        .placeholder(placeholderRes)
                        .error(placeholderRes)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(imageView)
                } else {
                    // Failed to decode base64, show placeholder
                    Glide.with(imageView.context)
                        .load(placeholderRes)
                        .circleCrop()
                        .into(imageView)
                }
            } else {
                // It's a URL, load normally
                Glide.with(imageView.context)
                    .load(profilePictureUrl)
                    .circleCrop()
                    .placeholder(placeholderRes)
                    .error(placeholderRes)
                    .into(imageView)
            }
        } else {
            // No profile picture, show placeholder
            Glide.with(imageView.context)
                .load(placeholderRes)
                .circleCrop()
                .into(imageView)
        }
    }
}
