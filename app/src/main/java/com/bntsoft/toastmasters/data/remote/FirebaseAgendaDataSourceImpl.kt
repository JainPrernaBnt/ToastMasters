package com.bntsoft.toastmasters.data.remote

import android.util.Log
import com.bntsoft.toastmasters.data.model.Abbreviations
import com.bntsoft.toastmasters.data.model.ClubInfo
import com.bntsoft.toastmasters.data.model.GrammarianDetails
import com.bntsoft.toastmasters.domain.model.AgendaItem
import com.bntsoft.toastmasters.domain.model.AgendaStatus
import com.bntsoft.toastmasters.domain.model.Meeting
import com.bntsoft.toastmasters.domain.model.MeetingAgenda
import com.bntsoft.toastmasters.utils.Result
import com.bntsoft.toastmasters.utils.Result.Error
import com.bntsoft.toastmasters.utils.Result.Success
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

private const val MEETINGS_COLLECTION = "meetings"
private const val AGENDA_ITEMS_SUBCOLLECTION = "agenda"
private const val GRAMMARIAN_DETAILS_SUBCOLLECTION = "grammarian_details"
private const val ABBREVIATIONS_SUBCOLLECTION = "abbreviations"

@Singleton
class FirebaseAgendaDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : FirebaseAgendaDataSource {

    // Resolve agendaId from meeting document; fallback to "default"
    private suspend fun resolveAgendaId(meetingId: String): String {
        return try {
            val meetingDoc = firestore.collection(MEETINGS_COLLECTION)
                .document(meetingId)
                .get()
                .await()
            meetingDoc.getString("agendaId") ?: "default"
        } catch (_: Exception) {
            "default"
        }
    }

    override suspend fun getMeetingAgenda(meetingId: String): Result<MeetingAgenda> {
        return try {
            // First get the main meeting document
            val meetingDoc = firestore.collection(MEETINGS_COLLECTION)
                .document(meetingId)
                .get()
                .await()

            if (!meetingDoc.exists()) {
                return Error(Exception("Meeting not found"))
            }

            // Get the agenda ID from the meeting document or use a default one
            val agendaId = meetingDoc.getString("agendaId") ?: "default"

            // Try to get agenda data from agenda/agendaId/agendaOfficers
            val agendaOfficersDocs = try {
                firestore.collection(MEETINGS_COLLECTION)
                    .document(meetingId)
                    .collection("agenda")
                    .document(agendaId)
                    .collection("agendaOfficers")
                    .limit(1)
                    .get()
                    .await()
            } catch (e: Exception) {
                // If agendaOfficers doesn't exist yet, that's fine - we'll use default values
                null
            }

            // Build agenda from both documents
            val agenda = if (agendaOfficersDocs != null && !agendaOfficersDocs.isEmpty) {
                val agendaOfficersDoc = agendaOfficersDocs.documents.firstOrNull()
                // Get officers and status from agendaOfficers subcollection
                val officers =
                    agendaOfficersDoc?.get("officers") as? Map<String, String> ?: emptyMap()
                val status = try {
                    val statusStr = agendaOfficersDoc?.getString("agendaStatus")
                    AgendaStatus.valueOf(statusStr ?: "DRAFT")
                } catch (e: Exception) {
                    AgendaStatus.DRAFT
                }

                // Create agenda with data from both documents
                buildAgendaFromSnapshot(meetingDoc)?.copy(
                    officers = officers,
                    agendaStatus = status
                )
            } else {
                // Fall back to old behavior if agendaOfficers doesn't exist
                tryDeserializeMeetingAgenda(meetingDoc)
            } ?: buildAgendaFromSnapshot(meetingDoc)
            ?: return Error(Exception("Failed to parse meeting agenda"))

            Success(agenda)
        } catch (e: Exception) {
            Error(e)
        }
    }

    private fun buildAgendaFromSnapshot(snapshot: DocumentSnapshot): MeetingAgenda? {
        return try {
            val theme = snapshot.getString("theme") ?: snapshot.getString("meetingTheme") ?: ""
            val venue = snapshot.getString("venue") ?: ""
            val dateStr = snapshot.getString("date")
            val startStr = snapshot.getString("startTime")
            val endStr = snapshot.getString("endTime")

            val formatter = DateTimeFormatter.ISO_LOCAL_DATE
            val meetingTs = if (!dateStr.isNullOrBlank()) {
                try {
                    val date = LocalDate.parse(dateStr, formatter)
                    val time = try {
                        if (!startStr.isNullOrBlank()) LocalTime.parse(startStr) else LocalTime.MIDNIGHT
                    } catch (_: Exception) {
                        LocalTime.MIDNIGHT
                    }
                    val ldt = LocalDateTime.of(date, time)
                    Timestamp(ldt.atZone(java.time.ZoneId.systemDefault()).toEpochSecond(), 0)
                } catch (_: Exception) {
                    null
                }
            } else null

            MeetingAgenda(
                id = snapshot.id,
                meeting = Meeting(
                    id = snapshot.id,
                    theme = theme,
                    location = venue
                ),
                meetingDate = meetingTs,
                startTime = startStr ?: "",
                endTime = endStr ?: "",
                officers = emptyMap(),
                agendaStatus = try {
                    snapshot.getString("agendaStatus")?.let { AgendaStatus.valueOf(it) }
                        ?: AgendaStatus.DRAFT
                } catch (_: Exception) {
                    AgendaStatus.DRAFT
                }
            )
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun saveMeetingAgenda(agenda: MeetingAgenda): Result<Unit> {
        return try {
            val agendaData = agenda.copy(updatedAt = Timestamp.now())
            val agendaId = agenda.id

            // Create a map with the meeting data including location
            val meetingData = hashMapOf<String, Any>(
                "theme" to agenda.meeting.theme,
                "location" to agenda.meeting.location,
                "venue" to agenda.meeting.location, // Save to both location and venue for backward compatibility
                "agendaId" to agendaId, // Store the agenda ID in the meeting document
                "updatedAt" to FieldValue.serverTimestamp()
            )

            // Update the main meeting document with basic info
            firestore.collection(MEETINGS_COLLECTION)
                .document(agenda.id)
                .set(meetingData, SetOptions.merge())
                .await()

            val agendaOfficersData = hashMapOf<String, Any>(
                "officers" to agenda.officers,
                "agendaStatus" to agenda.agendaStatus.name,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            firestore.collection(MEETINGS_COLLECTION)
                .document(agenda.id)
                .collection("agenda")
                .document(agendaId)
                .collection("agendaOfficers")
                .document(agendaId)
                .set(agendaOfficersData, SetOptions.merge())
                .await()

            Success(Unit)
        } catch (e: Exception) {
            Error(e)
        }
    }


    override suspend fun updateAgendaStatus(meetingId: String, status: AgendaStatus): Result<Unit> {
        return try {
            firestore.collection(MEETINGS_COLLECTION)
                .document(meetingId)
                .update(
                    "agendaStatus", status.name,
                    "updatedAt", FieldValue.serverTimestamp()
                )
                .await()

            Success(Unit)
        } catch (e: Exception) {
            Error(e)
        }
    }

    override fun observeMeetingAgenda(meetingId: String): Flow<Result<MeetingAgenda>> =
        callbackFlow {
            val meetingRef = firestore.collection(MEETINGS_COLLECTION).document(meetingId)

            // Listen to the meeting document to get the agenda ID
            val registration1 = meetingRef.addSnapshotListener { meetingSnapshot, error ->
                if (error != null) {
                    trySend(Error(Exception(error)))
                    return@addSnapshotListener
                }

                if (meetingSnapshot == null || !meetingSnapshot.exists()) {
                    trySend(Error(Exception("Meeting not found")))
                    return@addSnapshotListener
                }

                val agendaId = meetingSnapshot.getString("agendaId") ?: "default"
                val agendaOfficersRef = meetingRef
                    .collection("agenda")
                    .document(agendaId)
                    .collection("agendaOfficers")
                    .document(agendaId)

                // When meeting data changes, fetch the latest agenda officers
                agendaOfficersRef.get()
                    .addOnSuccessListener { agendaOfficersSnapshot ->
                        val agenda = if (agendaOfficersSnapshot.exists()) {
                            // Get officers and status from agendaOfficers
                            val officers =
                                agendaOfficersSnapshot.get("officers") as? Map<String, String>
                                    ?: emptyMap()
                            val status = try {
                                AgendaStatus.valueOf(
                                    agendaOfficersSnapshot.getString("agendaStatus") ?: "DRAFT"
                                )
                            } catch (e: Exception) {
                                AgendaStatus.DRAFT
                            }

                            // Create agenda with data from both documents
                            (tryDeserializeMeetingAgenda(meetingSnapshot)
                                ?: buildAgendaFromSnapshot(meetingSnapshot))?.copy(
                                officers = officers,
                                agendaStatus = status
                            )
                        } else {
                            // Fall back to old behavior if agendaOfficers doesn't exist
                            tryDeserializeMeetingAgenda(meetingSnapshot) ?: buildAgendaFromSnapshot(
                                meetingSnapshot
                            )
                        }

                        if (agenda != null) {
                            trySend(Success(agenda))
                        } else {
                            trySend(Error(Exception("Failed to parse meeting agenda")))
                        }
                    }
                    .addOnFailureListener { e ->
                        trySend(Error(e))
                    }
            }

            // Get the agenda ID from the meeting document first
            val agendaId = meetingRef.get().await().getString("agendaId") ?: "default"

            // Then listen to changes in the agendaOfficers document for this meeting
            val registration2 = firestore.collection(MEETINGS_COLLECTION)
                .document(meetingId)
                .collection("agenda")
                .document(agendaId)
                .collection("agendaOfficers")
                .document(agendaId)  // Use agendaId as the document ID
                .addSnapshotListener { documentSnapshot, error ->
                    if (error != null) {
                        trySend(Error(Exception(error)))
                        return@addSnapshotListener
                    }

                    documentSnapshot?.let { agendaOfficersSnapshot ->
                        // When agenda officers change, fetch the latest meeting data
                        meetingRef.get()
                            .addOnSuccessListener { meetingSnapshot ->
                                if (!meetingSnapshot.exists()) {
                                    trySend(Error(Exception("Meeting not found")))
                                } else {
                                    val agenda = if (agendaOfficersSnapshot.exists()) {
                                        // Get officers and status from agendaOfficers
                                        val officers =
                                            agendaOfficersSnapshot.get("officers") as? Map<String, String>
                                                ?: emptyMap()
                                        val status = try {
                                            val statusStr =
                                                agendaOfficersSnapshot.getString("agendaStatus")
                                            AgendaStatus.valueOf(statusStr ?: "DRAFT")
                                        } catch (e: Exception) {
                                            AgendaStatus.DRAFT
                                        }

                                        // Create agenda with data from both documents
                                        (tryDeserializeMeetingAgenda(meetingSnapshot)
                                            ?: buildAgendaFromSnapshot(meetingSnapshot))?.copy(
                                            officers = officers,
                                            agendaStatus = status
                                        )
                                    } else {
                                        // Fall back to old behavior if agendaOfficers doesn't exist
                                        tryDeserializeMeetingAgenda(meetingSnapshot)
                                            ?: buildAgendaFromSnapshot(meetingSnapshot)
                                    }

                                    if (agenda != null) {
                                        trySend(Success(agenda))
                                    } else {
                                        trySend(Error(Exception("Failed to parse meeting agenda")))
                                    }
                                }
                            }
                            .addOnFailureListener { e ->
                                trySend(Error(e))
                            }
                    }
                }

            awaitClose {
                registration1.remove()
                registration2.remove()
            }
        }

    override suspend fun getAgendaItem(meetingId: String, itemId: String): Result<AgendaItem> {
        return try {
            val agendaId = resolveAgendaId(meetingId)
            val document = firestore.collection(MEETINGS_COLLECTION)
                .document(meetingId)
                .collection("agenda")
                .document(agendaId)
                .collection("agendaItems")
                .document(itemId)
                .get()
                .await()

            if (document.exists()) {
                val data = document.data ?: return Error(Exception("No data found"))
                val cardSequence = data["cardSequence"] as? Map<*, *> ?: emptyMap<String, Any>()

                val item = AgendaItem(
                    id = document.id,
                    meetingId = meetingId,
                    activity = data["activity"] as? String ?: "",
                    presenterName = data["presenter"] as? String ?: "",
                    time = data["time"] as? String ?: "",
                    orderIndex = (data["order"] as? Number)?.toInt() ?: 0,
                    greenTime = (cardSequence["green"] as? Number)?.toInt() ?: 0,
                    yellowTime = (cardSequence["yellow"] as? Number)?.toInt() ?: 0,
                    redTime = (cardSequence["red"] as? Number)?.toInt() ?: 0,
                    updatedAt = data["updatedAt"] as? com.google.firebase.Timestamp
                        ?: com.google.firebase.Timestamp.now()
                )

                Success(item)
            } else {
                Error(Exception("Agenda item not found"))
            }
        } catch (e: Exception) {
            Error(e)
        }
    }

    override suspend fun getAgendaItems(meetingId: String): List<AgendaItem> {
        return try {
            val agendaId = resolveAgendaId(meetingId)
            val itemsSnapshot = firestore.collection(MEETINGS_COLLECTION)
                .document(meetingId)
                .collection("agenda")
                .document(agendaId)
                .collection("agendaItems")
                .orderBy("order")
                .get()
                .await()

            var items = itemsSnapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    val cardSequence = data["cardSequence"] as? Map<*, *> ?: emptyMap<String, Any>()

                    AgendaItem(
                        id = doc.id,
                        meetingId = meetingId,
                        activity = data["activity"] as? String ?: "",
                        presenterName = data["presenter"] as? String ?: "",
                        time = data["time"] as? String ?: "",
                        orderIndex = (data["order"] as? Number)?.toInt() ?: 0,
                        greenTime = (cardSequence["green"] as? Number)?.toInt() ?: 0,
                        yellowTime = (cardSequence["yellow"] as? Number)?.toInt() ?: 0,
                        redTime = (cardSequence["red"] as? Number)?.toInt() ?: 0,
                        updatedAt = data["updatedAt"] as? com.google.firebase.Timestamp
                            ?: com.google.firebase.Timestamp.now()
                    )
                } catch (e: Exception) {
                    null
                }
            }

            if (items.isEmpty()) {
                // Fallback: iterate all agendas under this meeting
                val agendaDocs = firestore.collection(MEETINGS_COLLECTION)
                    .document(meetingId)
                    .collection("agenda")
                    .get()
                    .await()

                val aggregated = mutableListOf<AgendaItem>()
                for (agendaDoc in agendaDocs) {
                    val snap = agendaDoc.reference
                        .collection("agendaItems")
                        .orderBy("order")
                        .get()
                        .await()
                    aggregated += snap.documents.mapNotNull { doc ->
                        try {
                            val data = doc.data ?: return@mapNotNull null
                            val cardSequence =
                                data["cardSequence"] as? Map<*, *> ?: emptyMap<String, Any>()
                            AgendaItem(
                                id = doc.id,
                                meetingId = meetingId,
                                activity = data["activity"] as? String ?: "",
                                presenterName = data["presenter"] as? String ?: "",
                                time = data["time"] as? String ?: "",
                                orderIndex = (data["order"] as? Number)?.toInt() ?: 0,
                                greenTime = (cardSequence["green"] as? Number)?.toInt() ?: 0,
                                yellowTime = (cardSequence["yellow"] as? Number)?.toInt() ?: 0,
                                redTime = (cardSequence["red"] as? Number)?.toInt() ?: 0,
                                updatedAt = data["updatedAt"] as? com.google.firebase.Timestamp
                                    ?: com.google.firebase.Timestamp.now()
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                items = aggregated
            }

            items.sortedBy { it.orderIndex }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun observeAgendaItems(meetingId: String): Flow<List<AgendaItem>> = callbackFlow {
        // Resolve agendaId once and observe that agenda's items only
        var registration: com.google.firebase.firestore.ListenerRegistration? = null

        firestore.collection(MEETINGS_COLLECTION)
            .document(meetingId)
            .get()
            .addOnSuccessListener { snapshot ->
                val agendaId = snapshot.getString("agendaId") ?: "default"
                val ref = firestore.collection(MEETINGS_COLLECTION)
                    .document(meetingId)
                    .collection("agenda")
                    .document(agendaId)
                    .collection("agendaItems")
                    .orderBy("order")

                registration = ref.addSnapshotListener { itemsSnapshot, error ->
                    if (error != null || itemsSnapshot == null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }

                    val items = itemsSnapshot.documents.mapNotNull { doc ->
                        try {
                            val data = doc.data ?: return@mapNotNull null
                            val cardSequence =
                                data["cardSequence"] as? Map<*, *> ?: emptyMap<String, Any>()
                            AgendaItem(
                                id = doc.id,
                                meetingId = meetingId,
                                activity = data["activity"] as? String ?: "",
                                presenterName = data["presenter"] as? String ?: "",
                                time = data["time"] as? String ?: "",
                                orderIndex = (data["order"] as? Number)?.toInt() ?: 0,
                                greenTime = (cardSequence["green"] as? Number)?.toInt() ?: 0,
                                yellowTime = (cardSequence["yellow"] as? Number)?.toInt() ?: 0,
                                redTime = (cardSequence["red"] as? Number)?.toInt() ?: 0,
                                updatedAt = data["updatedAt"] as? com.google.firebase.Timestamp
                                    ?: com.google.firebase.Timestamp.now()
                            )
                        } catch (_: Exception) {
                            null
                        }
                    }
                    trySend(items.sortedBy { it.orderIndex })
                }
            }
            .addOnFailureListener {
                trySend(emptyList())
            }

        awaitClose {
            registration?.remove()
        }
    }

    override suspend fun saveAgendaItem(
        meetingId: String,
        item: AgendaItem
    ): Result<String> {
        return try {
            val agendaId = resolveAgendaId(meetingId)
            val itemData = hashMapOf(
                "time" to item.time,
                "activity" to item.activity,
                "presenter" to item.presenterName,
                "order" to item.orderIndex,
                "meetingId" to meetingId,
                "cardSequence" to mapOf(
                    "green" to item.greenTime,
                    "yellow" to item.yellowTime,
                    "red" to item.redTime
                ),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val itemRef = firestore.collection(MEETINGS_COLLECTION)
                .document(meetingId)
                .collection("agenda")
                .document(agendaId)
                .collection("agendaItems")
                .document(item.id)

            itemRef.set(itemData, SetOptions.merge()).await()

            firestore.collection(MEETINGS_COLLECTION)
                .document(meetingId)
                .update("updatedAt", FieldValue.serverTimestamp())
                .await()

            Success(itemRef.id)
        } catch (e: Exception) {
            Error(e)
        }
    }

    override suspend fun deleteAgendaItem(meetingId: String, itemId: String): Result<Unit> {
        return try {
            val agendaId = resolveAgendaId(meetingId)
            firestore.collection(MEETINGS_COLLECTION)
                .document(meetingId)
                .collection("agenda")
                .document(agendaId)
                .collection("agendaItems")
                .document(itemId)
                .delete()
                .await()

            firestore.collection(MEETINGS_COLLECTION)
                .document(meetingId)
                .update("updatedAt", FieldValue.serverTimestamp())
                .await()

            Success(Unit)
        } catch (e: Exception) {
            Error(e)
        }
    }

    override suspend fun reorderAgendaItems(
        meetingId: String,
        items: List<AgendaItem>
    ): Result<Unit> {
        return try {
            val batch = firestore.batch()
            val now = Timestamp.now()
            val agendaId = resolveAgendaId(meetingId)
            val itemsRef = firestore.collection(MEETINGS_COLLECTION)
                .document(meetingId)
                .collection("agenda")
                .document(agendaId)
                .collection("agendaItems")

            for (item in items) {
                val itemRef = itemsRef.document(item.id)
                batch.update(itemRef, "order", item.orderIndex)
                batch.update(itemRef, "updatedAt", now)
            }

            batch.commit().await()

            firestore.collection(MEETINGS_COLLECTION)
                .document(meetingId)
                .update("updatedAt", FieldValue.serverTimestamp())
                .await()

            Success(Unit)
        } catch (e: Exception) {
            Error(e)
        }
    }

    override suspend fun saveAllAgendaItems(
        meetingId: String,
        items: List<AgendaItem>
    ): Result<Unit> {
        return try {
            val batch = firestore.batch()
            val agendaRef = firestore.collection(MEETINGS_COLLECTION)
                .document(meetingId)
                .collection("agenda")
                .document("default") // or use a specific agenda ID if needed

            // First, clear existing items if needed
            // Note: Be careful with this in production - you might want to merge instead
            val existingItems = getAgendaItems(meetingId)
            existingItems.forEach { item ->
                batch.delete(agendaRef.collection("agendaItems").document(item.id))
            }

            // Add all new items
            items.forEachIndexed { index, item ->
                val itemData = hashMapOf<String, Any>(
                    "time" to item.time,
                    "activity" to item.activity,
                    "presenter" to item.presenterName,
                    "order" to index,
                    "cardSequence" to mapOf(
                        "green" to item.greenTime,
                        "yellow" to item.yellowTime,
                        "red" to item.redTime
                    ),
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                val itemRef = agendaRef
                    .collection("agendaItems")
                    .document(item.id)

                batch.set(itemRef, itemData)
            }

            batch.commit().await()
            Success(Unit)
        } catch (e: Exception) {
            Error(e)
        }
    }

    override fun observeAgendaStatus(meetingId: String): Flow<AgendaStatus> {
        return callbackFlow {
            val listener = firestore.collection(MEETINGS_COLLECTION)
                .document(meetingId)
                .collection(AGENDA_ITEMS_SUBCOLLECTION)
                .document("status")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        close(e)
                        return@addSnapshotListener
                    }

                    val status = snapshot?.getString("status") ?: AgendaStatus.DRAFT.name
                    try {
                        trySend(AgendaStatus.valueOf(status))
                    } catch (e: IllegalArgumentException) {
                        trySend(AgendaStatus.DRAFT)
                    }
                }

            awaitClose { listener.remove() }
        }
    }

    override suspend fun getGrammarianDetails(
        meetingId: String,
        userId: String
    ): GrammarianDetails? {
        Log.d(
            "FirebaseAgendaDS",
            "Fetching grammarian details for meeting: $meetingId, user: $userId"
        )
        return try {
            val doc = firestore.collection(MEETINGS_COLLECTION)
                .document(meetingId)
                .collection(GRAMMARIAN_DETAILS_SUBCOLLECTION)
                .document(userId)
                .get()
                .await()

            if (doc.exists()) {
                val details = doc.toObject(GrammarianDetails::class.java)
                Log.d("FirebaseAgendaDS", "Found grammarian details: $details")
                details
            } else {
                Log.d("FirebaseAgendaDS", "No grammarian details found for user: $userId")
                null
            }
        } catch (e: Exception) {
            Log.e("FirebaseAgendaDS", "Error fetching grammarian details", e)
            null
        }
    }

    override suspend fun saveGrammarianDetails(
        meetingId: String,
        userId: String,
        details: GrammarianDetails
    ): Result<Unit> {
        Log.d(
            "FirebaseAgendaDS",
            "Saving grammarian details for meeting: $meetingId, user: $userId"
        )
        Log.d("FirebaseAgendaDS", "Details to save: $details")

        return try {
            // Ensure we have the correct IDs set
            val detailsToSave = details.copy(
                meetingID = meetingId,
                userId = userId
            )

            Log.d("FirebaseAgendaDS", "Saving to Firestore: $detailsToSave")

            firestore.collection(MEETINGS_COLLECTION)
                .document(meetingId)
                .collection(GRAMMARIAN_DETAILS_SUBCOLLECTION)
                .document(userId)
                .set(detailsToSave)
                .await()

            Log.d("FirebaseAgendaDS", "Successfully saved grammarian details to Firestore")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseAgendaDS", "Error saving grammarian details", e)
            Result.Error(e)
        }
    }

    override suspend fun getGrammarianDetailsForMeeting(meetingId: String): List<GrammarianDetails> {
        Log.d("FirebaseAgendaDS", "Fetching all grammarian details for meeting: $meetingId")
        return try {
            val result = firestore.collection(MEETINGS_COLLECTION)
                .document(meetingId)
                .collection(GRAMMARIAN_DETAILS_SUBCOLLECTION)
                .get()
                .await()
                .mapNotNull {
                    it.toObject(GrammarianDetails::class.java)?.also { details ->
                        Log.d("FirebaseAgendaDS", "Found grammarian details: $details")
                    }
                }
            Log.d("FirebaseAgendaDS", "Total grammarian details found: ${result.size}")
            result
        } catch (e: Exception) {
            Log.e("FirebaseAgendaDS", "Error fetching grammarian details for meeting", e)
            emptyList()
        }
    }

    private fun tryDeserializeMeetingAgenda(snapshot: DocumentSnapshot): MeetingAgenda? {
        return try {
            snapshot.toObject<MeetingAgenda>()
                ?.let { existingAgenda ->
                    // Pull meeting-level fields (theme, venue, date/times) from the meeting document if missing
                    val docTheme = snapshot.getString("theme") ?: snapshot.getString("meetingTheme")
                    val docVenue = snapshot.getString("venue")
                    val docDate = snapshot.getString("date") // e.g., yyyy-MM-dd
                    val docStart = snapshot.getString("startTime") // e.g., HH:mm
                    val docEnd = snapshot.getString("endTime") // e.g., HH:mm

                    // Compose meeting info
                    val updatedMeeting = existingAgenda.meeting.copy(
                        id = snapshot.id,
                        theme = if (docTheme.isNullOrBlank()) existingAgenda.meeting.theme else docTheme,
                        location = if (docVenue.isNullOrBlank()) existingAgenda.meeting.location else docVenue
                    )

                    // If meetingDate is null but date string exists, convert to Timestamp at start time (or start of day)
                    val formatter = DateTimeFormatter.ISO_LOCAL_DATE
                    val meetingDateTs = when {
                        existingAgenda.meetingDate != null -> existingAgenda.meetingDate
                        !docDate.isNullOrBlank() -> {
                            try {
                                val date = LocalDate.parse(docDate, formatter)
                                val time = try {
                                    if (!docStart.isNullOrBlank()) LocalTime.parse(docStart) else LocalTime.MIDNIGHT
                                } catch (e: Exception) {
                                    LocalTime.MIDNIGHT
                                }
                                val ldt = LocalDateTime.of(date, time)
                                Timestamp(
                                    ldt.atZone(java.time.ZoneId.systemDefault()).toEpochSecond(), 0
                                )
                            } catch (_: Exception) {
                                null
                            }
                        }

                        else -> null
                    }

                    existingAgenda.copy(
                        id = snapshot.id,
                        meeting = updatedMeeting,
                        meetingDate = meetingDateTs ?: existingAgenda.meetingDate,
                        startTime = if (!docStart.isNullOrBlank()) docStart else existingAgenda.startTime,
                        endTime = if (!docEnd.isNullOrBlank()) docEnd else existingAgenda.endTime
                    )
                }
        } catch (e: Exception) {
            // Handle the specific case where createdAt/updatedAt were stored as Longs
            val createdAt = snapshot.get("createdAt")
            val updatedAt = snapshot.get("updatedAt")

            var needsFix = false
            val updates = mutableMapOf<String, Any>()

            if (createdAt is Number) {
                // Treat as milliseconds since epoch
                val ms = createdAt.toLong()
                updates["createdAt"] = Timestamp(ms / 1000, ((ms % 1000) * 1_000_000).toInt())
                needsFix = true
            }
            if (updatedAt is Number) {
                val ms = updatedAt.toLong()
                updates["updatedAt"] = Timestamp(ms / 1000, ((ms % 1000) * 1_000_000).toInt())
                needsFix = true
            }

            if (needsFix) {
                // Fire and forget: normalize the fields so the next snapshot will succeed
                snapshot.reference.update(updates)
                return null
            }

            // For any other error, return null to avoid crashing listeners
            return null
        }
    }

    override suspend fun getAbbreviations(meetingId: String, agendaId: String): Abbreviations {
        return try {
            val doc = firestore.collection(MEETINGS_COLLECTION)
                .document(meetingId)
                .collection("agenda")
                .document(agendaId)
                .collection("agendaOfficers")
                .document(agendaId)
                .get()
                .await()

            val data = doc.data
            if (data != null && data.isNotEmpty()) {
                @Suppress("UNCHECKED_CAST")
                return (data["abbreviations"] as? Map<String, String>) ?: emptyMap()
            }
            emptyMap()
        } catch (e: Exception) {
            Log.e("FirebaseAgendaDS", "Error getting abbreviations", e)
            emptyMap()
        }
    }

    override suspend fun saveAbbreviations(
        meetingId: String,
        agendaId: String,
        abbreviations: Abbreviations
    ): Result<Unit> {
        return try {
            val docRef = firestore.collection(MEETINGS_COLLECTION)
                    .document(meetingId)
                    .collection("agenda")
                    .document(agendaId)
                    .collection("agendaOfficers")
                    .document(agendaId)

            // Create a new map to ensure we're not storing any null values
            val cleanAbbreviations =
                abbreviations.filterValues { it != null } as Map<String, String>

            val data = hashMapOf<String, Any>(
                "abbreviations" to cleanAbbreviations,
                "lastUpdated" to FieldValue.serverTimestamp()
            )

            // Use set with merge to preserve other fields in the document
            docRef.set(data, SetOptions.merge()).await()
            Success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseAgendaDS", "Error saving abbreviations", e)
            Error(e)
        }
    }

    override suspend fun deleteAbbreviation(
        meetingId: String,
        agendaId: String,
        abbreviationKey: String
    ): Result<Unit> {
        return try {
            val docRef = firestore.collection(MEETINGS_COLLECTION)
                .document(meetingId)
                .collection("agenda")
                .document(agendaId)
                .collection("agendaOfficers")
                .document(agendaId)

            // Use FieldValue.delete() to remove the specific abbreviation from the map
            val updates = hashMapOf<String, Any>(
                "abbreviations.$abbreviationKey" to FieldValue.delete()
            )

            docRef.update(updates).await()
            Success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseAgendaDS", "Error deleting abbreviation", e)
            Error(e)
        }
    }

    override suspend fun updateOfficers(
        meetingId: String,
        officers: Map<String, String>
    ): Result<Unit> {
        return try {
            val agendaId = resolveAgendaId(meetingId)

            // Only update the provided officer fields, leave others untouched
            val officerData = hashMapOf<String, Any>(
                "officers" to officers,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            // Merge only the provided officers into the existing document
            firestore.collection(MEETINGS_COLLECTION)
                .document(meetingId)
                .collection("agenda")
                .document(agendaId)
                .collection("agendaOfficers")
                .document(agendaId)
                .set(officerData, SetOptions.merge())
                .await()

            // Also update the main meeting document for quick access (merge)
            firestore.collection(MEETINGS_COLLECTION)
                .document(meetingId)
                .set(mapOf("officers" to officers), SetOptions.merge())
                .await()

            Success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseAgendaDS", "Error updating officers", e)
            Error(e)
        }
    }

    override suspend fun updateClubInfo(
        meetingId: String,
        clubName: String,
        clubNumber: String,
        district: String,
        area: String,
        mission: String
    ): Result<Unit> {
        return try {
            val agendaId = resolveAgendaId(meetingId)
            val clubInfo = mapOf(
                "clubName" to clubName,
                "clubNumber" to clubNumber,
                "district" to district,
                "area" to area,
                "mission" to mission,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            // Save club info in the same document as officers data
            val agendaOfficersDoc = firestore.collection(MEETINGS_COLLECTION)
                .document(meetingId)
                .collection("agenda")
                .document(agendaId)
                .collection("agendaOfficers")
                .document(agendaId)

            // Always update all club info fields together
            val clubInfoUpdate = mapOf(
                "clubInfo" to mapOf(
                    "clubName" to clubName,
                    "clubNumber" to clubNumber,
                    "district" to district,
                    "area" to area,
                    "mission" to mission,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            agendaOfficersDoc.set(clubInfoUpdate, SetOptions.merge()).await()

            // Also update the main meeting document for quick access (merge)
            firestore.collection(MEETINGS_COLLECTION)
                .document(meetingId)
                .set(
                    mapOf(
                        "clubName" to clubName,
                        "clubNumber" to clubNumber,
                        "district" to district,
                        "area" to area,
                        "mission" to mission
                    ),
                    SetOptions.merge()
                )
                .await()

            Success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseAgendaDS", "Error updating club info", e)
            Error(e)
        }
    }

    override suspend fun getOfficers(
        meetingId: String,
        agendaId: String
    ): Result<Map<String, String>> {
        return try {
            val docSnapshot = firestore.collection(MEETINGS_COLLECTION)
                .document(meetingId)
                .collection(AGENDA_ITEMS_SUBCOLLECTION)
                .document(agendaId)
                .collection("agendaOfficers")
                .document(agendaId)
                .get()
                .await()

            val officers = docSnapshot.get("officers") as? Map<String, String> ?: emptyMap()

            Log.d("FirebaseAgendaDS", "Fetched officers: $officers")
            Result.Success(officers)
        } catch (e: Exception) {
            Log.e("FirebaseAgendaDS", "Error fetching officers", e)
            Result.Error(e)
        }
    }

    override suspend fun getClubInfo(meetingId: String, agendaId: String): Result<ClubInfo> {
        return try {
            val docSnapshot = firestore.collection(MEETINGS_COLLECTION)
                .document(meetingId)
                .collection(AGENDA_ITEMS_SUBCOLLECTION)
                .document(agendaId)
                .collection("agendaOfficers")
                .document(agendaId)
                .get()
                .await()

            val clubName = docSnapshot.getString("clubName") ?: ""
            val clubNumber = docSnapshot.getString("clubNumber") ?: ""
            val district = docSnapshot.getString("district") ?: ""
            val area = docSnapshot.getString("area") ?: ""
            val mission = docSnapshot.getString("mission") ?: ""

            val clubInfo = ClubInfo(
                clubName = clubName,
                clubNumber = clubNumber,
                district = district,
                area = area,
                mission = mission
            )

            Log.d("FirebaseAgendaDS", "Fetched club info: $clubInfo")
            Result.Success(clubInfo)
        } catch (e: Exception) {
            Log.e("FirebaseAgendaDS", "Error fetching club info", e)
            Result.Error(e)
        }
    }
}