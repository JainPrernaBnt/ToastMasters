package com.bntsoft.toastmasters.domain.repository

import android.net.Uri

interface ProfileRepository {
    suspend fun updateProfilePicture(userId: String, imageUri: Uri): Result<String>
    suspend fun deleteProfilePicture(userId: String): Result<Boolean>
}
