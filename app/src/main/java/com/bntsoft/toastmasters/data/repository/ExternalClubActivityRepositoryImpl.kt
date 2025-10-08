package com.bntsoft.toastmasters.data.repository

import com.bntsoft.toastmasters.domain.model.ExternalClubActivity
import com.bntsoft.toastmasters.domain.repository.ExternalClubActivityRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExternalClubActivityRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ExternalClubActivityRepository {

    companion object {
        private const val COLLECTION_NAME = "externalClubActivity"
    }

    override suspend fun addActivity(activity: ExternalClubActivity): Result<String> {
        return try {
            val docRef = firestore.collection(COLLECTION_NAME).document()
            val activityWithId = activity.copy(id = docRef.id)
            
            val activityMap = mapOf(
                "id" to activityWithId.id,
                "userId" to activityWithId.userId,
                "userName" to activityWithId.userName,
                "userProfilePicture" to activityWithId.userProfilePicture,
                "clubName" to activityWithId.clubName,
                "clubLocation" to activityWithId.clubLocation,
                "meetingDate" to activityWithId.meetingDate,
                "rolePlayed" to activityWithId.rolePlayed,
                "notes" to activityWithId.notes,
                "timestamp" to activityWithId.timestamp
            )
            
            docRef.set(activityMap).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateActivity(activity: ExternalClubActivity): Result<Unit> {
        return try {
            val activityMap = mapOf(
                "clubName" to activity.clubName,
                "clubLocation" to activity.clubLocation,
                "meetingDate" to activity.meetingDate,
                "rolePlayed" to activity.rolePlayed,
                "notes" to activity.notes,
                "timestamp" to System.currentTimeMillis()
            )
            
            firestore.collection(COLLECTION_NAME)
                .document(activity.id)
                .update(activityMap)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteActivity(activityId: String): Result<Unit> {
        return try {
            firestore.collection(COLLECTION_NAME)
                .document(activityId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getActivityById(activityId: String): Result<ExternalClubActivity?> {
        return try {
            val document = firestore.collection(COLLECTION_NAME)
                .document(activityId)
                .get()
                .await()
            
            if (document.exists()) {
                val activity = ExternalClubActivity(
                    id = document.getString("id") ?: "",
                    userId = document.getString("userId") ?: "",
                    userName = document.getString("userName") ?: "",
                    userProfilePicture = document.getString("userProfilePicture"),
                    clubName = document.getString("clubName") ?: "",
                    clubLocation = document.getString("clubLocation"),
                    meetingDate = document.getDate("meetingDate") ?: Date(),
                    rolePlayed = document.getString("rolePlayed") ?: "",
                    notes = document.getString("notes"),
                    timestamp = document.getLong("timestamp") ?: System.currentTimeMillis()
                )
                Result.success(activity)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getAllActivities(): Flow<List<ExternalClubActivity>> = callbackFlow {
        val listener = firestore.collection(COLLECTION_NAME)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val activities = snapshot?.documents?.mapNotNull { document ->
                    try {
                        ExternalClubActivity(
                            id = document.getString("id") ?: "",
                            userId = document.getString("userId") ?: "",
                            userName = document.getString("userName") ?: "",
                            userProfilePicture = document.getString("userProfilePicture"),
                            clubName = document.getString("clubName") ?: "",
                            clubLocation = document.getString("clubLocation"),
                            meetingDate = document.getDate("meetingDate") ?: Date(),
                            rolePlayed = document.getString("rolePlayed") ?: "",
                            notes = document.getString("notes"),
                            timestamp = document.getLong("timestamp") ?: System.currentTimeMillis()
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                
                trySend(activities)
            }
        
        awaitClose { listener.remove() }
    }

    override fun getUserActivities(userId: String): Flow<List<ExternalClubActivity>> = callbackFlow {
        val listener = firestore.collection(COLLECTION_NAME)
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val activities = snapshot?.documents?.mapNotNull { document ->
                    try {
                        ExternalClubActivity(
                            id = document.getString("id") ?: "",
                            userId = document.getString("userId") ?: "",
                            userName = document.getString("userName") ?: "",
                            userProfilePicture = document.getString("userProfilePicture"),
                            clubName = document.getString("clubName") ?: "",
                            clubLocation = document.getString("clubLocation"),
                            meetingDate = document.getDate("meetingDate") ?: Date(),
                            rolePlayed = document.getString("rolePlayed") ?: "",
                            notes = document.getString("notes"),
                            timestamp = document.getLong("timestamp") ?: System.currentTimeMillis()
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                
                trySend(activities)
            }
        
        awaitClose { listener.remove() }
    }
}
