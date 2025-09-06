package com.bntsoft.toastmasters.data.remote

import com.bntsoft.toastmasters.domain.model.AgendaItem
import com.bntsoft.toastmasters.domain.model.AgendaStatus
import com.bntsoft.toastmasters.domain.model.Meeting
import com.bntsoft.toastmasters.domain.model.MeetingAgenda
import com.bntsoft.toastmasters.utils.Result
import com.bntsoft.toastmasters.utils.Result.Error
import com.bntsoft.toastmasters.utils.Result.Success
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
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
            val document = firestore.collection(MEETINGS_COLLECTION)
                .document(meetingId)
                .get()
                .await()

            if (document.exists()) {
                val agenda = tryDeserializeMeetingAgenda(document)
                    ?: buildAgendaFromSnapshot(document)
                    ?: return Error(Exception("Failed to parse meeting agenda"))

                Success(agenda)
            } else {
                Error(Exception("Meeting agenda not found"))
            }
        } catch (e: Exception) {
            Error(e)
        }
    }

    /**
     * Build a lightweight MeetingAgenda from top-level meeting document fields as a fallback
     * when full deserialization is not possible. This keeps the UI responsive.
     */
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
                    val time = try { if (!startStr.isNullOrBlank()) LocalTime.parse(startStr) else LocalTime.MIDNIGHT } catch (_: Exception) { LocalTime.MIDNIGHT }
                    val ldt = LocalDateTime.of(date, time)
                    Timestamp(ldt.atZone(java.time.ZoneId.systemDefault()).toEpochSecond(), 0)
                } catch (_: Exception) { null }
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
                    snapshot.getString("agendaStatus")?.let { AgendaStatus.valueOf(it) } ?: AgendaStatus.DRAFT
                } catch (_: Exception) { AgendaStatus.DRAFT }
            )
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun saveMeetingAgenda(agenda: MeetingAgenda): Result<Unit> {
        return try {
            val agendaData = agenda.copy(updatedAt = Timestamp.now())

            firestore.collection(MEETINGS_COLLECTION)
                .document(agenda.id)
                .set(agendaData)
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
                .update("agendaStatus", status.name,
                    "updatedAt", FieldValue.serverTimestamp())
                .await()

            Success(Unit)
        } catch (e: Exception) {
            Error(e)
        }
    }

    override fun observeMeetingAgenda(meetingId: String): Flow<Result<MeetingAgenda>> = callbackFlow {
        val listener = firestore.collection(MEETINGS_COLLECTION)
            .document(meetingId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Error(Exception(error)))
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val agenda = tryDeserializeMeetingAgenda(snapshot) ?: buildAgendaFromSnapshot(snapshot)
                    if (agenda != null) {
                        trySend(Success(agenda))
                    }
                } else {
                    trySend(Error(Exception("Meeting agenda not found")))
                }
            }

        awaitClose { listener.remove() }
    }

    /**
     * Safely deserialize a MeetingAgenda from a DocumentSnapshot. If the document contains
     * legacy Long values for createdAt/updatedAt, update them to Firestore Timestamps and
     * return null so the caller can wait for the next snapshot.
     */
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
                                val time = try { if (!docStart.isNullOrBlank()) LocalTime.parse(docStart) else LocalTime.MIDNIGHT } catch (e: Exception) { LocalTime.MIDNIGHT }
                                val ldt = LocalDateTime.of(date, time)
                                Timestamp(ldt.atZone(java.time.ZoneId.systemDefault()).toEpochSecond(), 0)
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

    override suspend fun getAgendaItem(meetingId: String, itemId: String): Result<AgendaItem> {
        return try {
            val document = firestore.collection(MEETINGS_COLLECTION)
                .document(meetingId)
                .collection(AGENDA_ITEMS_SUBCOLLECTION)
                .document(itemId)
                .get()
                .await()

            if (document.exists()) {
                val item = document.toObject<AgendaItem>()
                    ?.copy(id = document.id)
                    ?: return Error(Exception("Failed to parse agenda item"))

                Success(item)
            } else {
                Error(Exception("Agenda item not found"))
            }
        } catch (e: Exception) {
            Error(e)
        }
    }

    override suspend fun saveAgendaItem(meetingId: String, item: AgendaItem): Result<String> {
        return try {
            val itemData = item.copy(updatedAt = Timestamp.now())
            val itemRef = firestore.collection(MEETINGS_COLLECTION)
                .document(meetingId)
                .collection(AGENDA_ITEMS_SUBCOLLECTION)
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
            firestore.collection(MEETINGS_COLLECTION)
                .document(meetingId)
                .collection(AGENDA_ITEMS_SUBCOLLECTION)
                .document(itemId)
                .delete()
                .await()

            Success(Unit)
        } catch (e: Exception) {
            Error(e)
        }
    }

    override suspend fun reorderAgendaItems(meetingId: String, items: List<AgendaItem>): Result<Unit> {
        return try {
            val batch = firestore.batch()
            val now = Timestamp.now()

            items.forEachIndexed { index, item ->
                val itemRef = firestore.collection(MEETINGS_COLLECTION)
                    .document(meetingId)
                    .collection(AGENDA_ITEMS_SUBCOLLECTION)
                    .document(item.id)

                batch.update(itemRef, "orderIndex", index, "updatedAt", now)
            }

            batch.commit().await()
            Success(Unit)
        } catch (e: Exception) {
            Error(e)
        }
    }

    override suspend fun saveAllAgendaItems(meetingId: String, items: List<AgendaItem>): Result<Unit> {
        return try {
            val batch = firestore.batch()
            val now = Timestamp.now()

            items.forEachIndexed { index, item ->
                val itemData = item.copy(
                    orderIndex = index,
                    updatedAt = now
                )

                val itemRef = firestore.collection(MEETINGS_COLLECTION)
                    .document(meetingId)
                    .collection(AGENDA_ITEMS_SUBCOLLECTION)
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
}
