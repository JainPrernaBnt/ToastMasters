package com.bntsoft.toastmasters.data.repository

import android.content.Context
import android.net.Uri
import com.bntsoft.toastmasters.data.remote.FirestoreService
import com.bntsoft.toastmasters.domain.repository.ProfileRepository
import com.bntsoft.toastmasters.utils.ImageUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestoreService: FirestoreService
) : ProfileRepository {

    override suspend fun updateProfilePicture(userId: String, imageUri: Uri): Result<String> {
        return try {
            val base64Image = ImageUtils.compressImageToBase64(context, imageUri)
                ?: return Result.failure(Exception("Failed to compress image"))

            val updates = mapOf("profilePictureUrl" to base64Image)
            firestoreService.getUserDocument(userId).update(updates).await()

            // Return the string (Base64) to ViewModel
            Result.success(base64Image)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteProfilePicture(userId: String): Result<Boolean> {
        return try {
            // Update Firestore to remove profile picture
            val updates = mapOf("profilePictureUrl" to null)
            firestoreService.getUserDocument(userId).update(updates).await()

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
