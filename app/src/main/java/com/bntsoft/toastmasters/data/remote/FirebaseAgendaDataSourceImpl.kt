package com.bntsoft.toastmasters.data.remote

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
import javax.inject.Inject
import javax.inject.Singleton

private const val MEETINGS_COLLECTION = "meetings"
private const val AGENDA_ITEMS_SUBCOLLECTION = "agenda"

@Singleton
class FirebaseAgendaDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : FirebaseAgendaDataSource {

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
                                        val officers = agendaOfficersSnapshot.get("officers") as? Map<String, String> ?: emptyMap()
                                        val status = try {
                                            val statusStr = agendaOfficersSnapshot.getString("agendaStatus")
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
            val document = firestore.collection(MEETINGS_COLLECTION)
                .document(meetingId)
                .collection("agenda")
                .document(itemId)
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
                    updatedAt = data["updatedAt"] as? com.google.firebase.Timestamp ?: com.google.firebase.Timestamp.now()
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
            // First get the agenda document
            val agendaDocs = firestore.collection(MEETINGS_COLLECTION)
                .document(meetingId)
                .collection("agenda")
                .get()
                .await()

            if (agendaDocs.isEmpty) return emptyList()

            // For each agenda, get its items
            val allItems = mutableListOf<AgendaItem>()
            
            for (agendaDoc in agendaDocs) {
                val itemsSnapshot = agendaDoc.reference
                    .collection("agendaItems")
                    .orderBy("order")
                    .get()
                    .await()

                itemsSnapshot.documents.forEach { doc ->
                    try {
                        val data = doc.data ?: return@forEach
                        val cardSequence = data["cardSequence"] as? Map<*, *> ?: emptyMap<String, Any>()
                        
                        val item = AgendaItem(
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
                        allItems.add(item)
                    } catch (e: Exception) {
                        // Skip items that can't be parsed
                    }
                }
            }
            
            // Sort by order index as a fallback
            allItems.sortedBy { it.orderIndex }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun observeAgendaItems(meetingId: String): Flow<List<AgendaItem>> = callbackFlow {
        val agendaRef = firestore.collection(MEETINGS_COLLECTION)
            .document(meetingId)
            .collection("agenda")

        val listener = agendaRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Handle error
                return@addSnapshotListener
            }

            if (snapshot == null || snapshot.isEmpty) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            // For each agenda document, listen to its items
            snapshot.documents.forEach { agendaDoc ->
                agendaDoc.reference.collection("agendaItems")
                    .orderBy("order")
                    .addSnapshotListener { itemsSnapshot, itemsError ->
                        if (itemsError != null || itemsSnapshot == null) {
                            return@addSnapshotListener
                        }

                        val items = itemsSnapshot.documents.mapNotNull { doc ->
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

                        trySend(items.sortedBy { it.orderIndex })
                    }
            }
        }

        awaitClose { listener.remove() }
    }

    override suspend fun saveAgendaItem(meetingId: String, item: AgendaItem): Result<String> {
        return try {
            // Create a map with the desired structure
            val itemData = hashMapOf<String, Any>(
                "time" to item.time,
                "activity" to item.activity,
                "presenter" to item.presenterName,
                "order" to item.orderIndex,
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
                .document(item.id)
                .collection("agendaItems")
                .document(item.id)

            itemRef.set(itemData).await()

            // Update the parent meeting's updatedAt timestamp
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
            // First, find the agenda document that contains this item
            val agendaDocs = firestore.collection(MEETINGS_COLLECTION)
                .document(meetingId)
                .collection("agenda")
                .whereEqualTo("__name__", itemId) // Look for agenda document with ID matching itemId
                .get()
                .await()

            if (agendaDocs.isEmpty) {
                return Error(Exception("Agenda item not found"))
            }

            // Delete the agenda item
            val agendaDoc = agendaDocs.documents[0]
            agendaDoc.reference
                .collection("agendaItems")
                .document(itemId)
                .delete()
                .await()

            // Also delete the parent agenda document if needed
            // agendaDoc.reference.delete().await()

            // Update the parent meeting's updatedAt timestamp
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
            val agendaRef = firestore.collection(MEETINGS_COLLECTION)
                .document(meetingId)
                .collection("agenda")

            // First, get all agenda documents to find the ones we need to update
            val agendaDocs = agendaRef.get().await()
            
            // Create a map of item ID to its new order
            val orderMap = items.associate { it.id to it.orderIndex }
            
            // Find and update each item
            for (item in items) {
                // Find the agenda document for this item
                val agendaDoc = agendaDocs.documents.find { it.id == item.id } ?: continue
                
                // Update the order in the agendaItems subcollection
                val itemRef = agendaDoc.reference
                    .collection("agendaItems")
                    .document(item.id)
                
                // Update fields one by one to match the expected format
                batch.update(itemRef, "order", item.orderIndex)
                batch.update(itemRef, "updatedAt", now)
            }

            batch.commit().await()
            
            // Update the parent meeting's updatedAt timestamp
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
            val existingItems = agendaRef.collection("agendaItems").get().await()
            existingItems.forEach { batch.delete(it.reference) }

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

    override fun observeAgendaStatus(meetingId: String): Flow<AgendaStatus> = callbackFlow {
        val listener = firestore.collection(MEETINGS_COLLECTION)
            .document(meetingId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val status = snapshot.getString("agendaStatus")
                        ?.let { AgendaStatus.valueOf(it) }
                        ?: AgendaStatus.DRAFT

                    trySend(status)
                }
            }

        awaitClose { listener.remove() }
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
}
