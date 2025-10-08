package com.bntsoft.toastmasters.data.repository

import android.util.Log
import com.bntsoft.toastmasters.domain.model.Guest
import com.bntsoft.toastmasters.domain.model.MeetingAvailability
import com.bntsoft.toastmasters.domain.models.AvailabilityStatus
import com.bntsoft.toastmasters.domain.repository.GuestRepository
import com.bntsoft.toastmasters.utils.Resource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GuestRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : GuestRepository {

    override suspend fun getAllGuests(): List<Guest> {
        return try {
            val snapshot = firestore.collection("guests").get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Guest::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getGuestById(id: String): Guest? {
        return try {
            val doc = firestore.collection("guests").document(id).get().await()
            doc.toObject(Guest::class.java)?.copy(id = doc.id)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun createGuest(guest: Guest): Resource<Guest> {
        return try {
            val docRef = firestore.collection("guests").add(guest).await()
            val createdGuest = guest.copy(id = docRef.id)
            Resource.Success(createdGuest)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create guest")
        }
    }

    override suspend fun updateGuest(guest: Guest): Resource<Unit> {
        return try {
            firestore.collection("guests").document(guest.id).set(guest).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update guest")
        }
    }

    override suspend fun deleteGuest(id: String): Resource<Unit> {
        return try {
            firestore.collection("guests").document(id).delete().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete guest")
        }
    }

    override suspend fun addGuestToMeeting(meetingId: String, guestId: String): Resource<Unit> {
        return try {
            val guest = getGuestById(guestId)
            if (guest == null) {
                return Resource.Error("Guest not found")
            }

            val guestData = mapOf(
                "id" to guestId,
                "name" to guest.name,
                "email" to guest.email,
                "contact" to guest.contact
            )
            firestore.collection("meetings").document(meetingId)
                .collection("guest").document(guestId).set(guestData).await()

            val availabilityData = MeetingAvailability(
                userId = "",
                meetingId = meetingId,
                status = AvailabilityStatus.AVAILABLE,
                preferredRoles = emptyList(),
                timestamp = System.currentTimeMillis(),
                isBackout = false,
                backoutReason = null
            )
            firestore.collection("meetings").document(meetingId)
                .collection("availability").document(guestId).set(availabilityData).await()

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to add guest to meeting")
        }
    }

    override suspend fun removeGuestFromMeeting(meetingId: String, guestId: String): Resource<Unit> {
        return try {
            firestore.collection("meetings").document(meetingId)
                .collection("guest").document(guestId).delete().await()
            
            firestore.collection("meetings").document(meetingId)
                .collection("availability").document(guestId).delete().await()
            
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to remove guest from meeting")
        }
    }

    override suspend fun getGuestsForMeeting(meetingId: String): List<Guest> {
        return try {
            Log.d("GuestRepository", "Getting guests for meeting: $meetingId")
            val snapshot = firestore.collection("meetings").document(meetingId)
                .collection("guest").get().await()
            Log.d("GuestRepository", "Found ${snapshot.documents.size} guest documents")
            
            val guests = snapshot.documents.mapNotNull { doc ->
                Log.d("GuestRepository", "Processing guest document: ${doc.id}")
                Guest(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    email = doc.getString("email") ?: "",
                    contact = doc.getString("contact") ?: ""
                )
            }
            Log.d("GuestRepository", "Returning ${guests.size} guests")
            guests
        } catch (e: Exception) {
            Log.e("GuestRepository", "Error getting guests for meeting $meetingId: ${e.message}", e)
            emptyList()
        }
    }
}
