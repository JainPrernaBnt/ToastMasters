package com.bntsoft.toastmasters.data.repository

import com.bntsoft.toastmasters.data.model.MemberRole
import com.bntsoft.toastmasters.domain.repository.AssignedRoleRepository
import com.bntsoft.toastmasters.utils.Resource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssignedRoleRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : AssignedRoleRepository {

    override suspend fun getAssignedRoles(meetingId: String): Flow<Resource<List<MemberRole>>> =
        flow {
            emit(Resource.Loading())
            try {
                val snapshot = firestore.collection("meetings")
                    .document(meetingId)
                    .collection("assignedRole")
                    .get()
                    .await()

                val roles = snapshot.documents.mapNotNull { document ->
                    document.toObject(MemberRole::class.java)?.copy(id = document.id)
                }
                emit(Resource.Success(roles))
            } catch (e: Exception) {
                emit(Resource.Error(e.message ?: "An unknown error occurred"))
            }
        }

    override suspend fun getAssignedRole(
        meetingId: String,
        roleId: String
    ): Flow<Resource<MemberRole?>> =
        flow {
            emit(Resource.Loading())
            try {
                val document = firestore.collection("meetings")
                    .document(meetingId)
                    .collection("assignedRole")
                    .document(roleId)
                    .get()
                    .await()

                if (document.exists()) {
                    val role = document.toObject(MemberRole::class.java)?.copy(id = document.id)
                    emit(Resource.Success(role))
                } else {
                    emit(Resource.Success(null))
                }
            } catch (e: Exception) {
                emit(Resource.Error(e.message ?: "Failed to fetch assigned role"))
            }
        }

    override suspend fun saveAssignedRole(
        meetingId: String,
        assignedRole: MemberRole
    ): Resource<Unit> {
        return try {
            val roleData = hashMapOf(
                "memberName" to assignedRole.memberName,
                "roles" to assignedRole.roles
            )

            if (assignedRole.id.isNotEmpty()) {
                // Update existing role
                firestore.collection("meetings")
                    .document(meetingId)
                    .collection("assignedRole")
                    .document(assignedRole.id)
                    .set(roleData, SetOptions.merge())
                    .await()
            } else {
                // Add new role
                firestore.collection("meetings")
                    .document(meetingId)
                    .collection("assignedRole")
                    .add(roleData)
                    .await()
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to save assigned role")
        }
    }

    override suspend fun deleteAssignedRole(
        meetingId: String,
        roleId: String
    ): Resource<Unit> {
        return try {
            firestore.collection("meetings")
                .document(meetingId)
                .collection("assignedRole")
                .document(roleId)
                .delete()
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete assigned role")
        }
    }
}
