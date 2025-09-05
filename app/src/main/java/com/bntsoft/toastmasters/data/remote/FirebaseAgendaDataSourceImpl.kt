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
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val MEETINGS_COLLECTION = "meetings"
private const val AGENDA_ITEMS_SUBCOLLECTION = "agenda"

@Singleton
class FirebaseAgendaDataSourceImpl @Inject constructor() : FirebaseAgendaDataSource {

    private val firestore: FirebaseFirestore = Firebase.firestore

    override suspend fun getMeetingAgenda(meetingId: String): Result<MeetingAgenda> {
        return try {
            val document = firestore.collection(MEETINGS_COLLECTION)
                .document(meetingId)
                .get()
                .await()

            if (document.exists()) {
                val agenda = document.toObject<MeetingAgenda>()
                    ?.let { existingAgenda ->
                        existingAgenda.copy(
                            id = document.id,
                            meeting = existingAgenda.meeting.copy(id = document.id)
                        )
                    } ?: return Error(Exception("Failed to parse meeting agenda"))
                
                Success(agenda)
            } else {
                Error(Exception("Meeting agenda not found"))
            }
        } catch (e: Exception) {
            Error(e)
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
                    val agenda = snapshot.toObject<MeetingAgenda>()
                        ?.let { existingAgenda ->
                            existingAgenda.copy(
                                id = snapshot.id,
                                meeting = existingAgenda.meeting.copy(id = snapshot.id)
                            )
                        } ?: return@addSnapshotListener
                        
                    trySend(Success(agenda))
                } else {
                    trySend(Error(Exception("Meeting agenda not found")))
                }
            }

        awaitClose { listener.remove() }
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
