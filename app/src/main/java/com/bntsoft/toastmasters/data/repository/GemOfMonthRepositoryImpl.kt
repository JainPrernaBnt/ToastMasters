package com.bntsoft.toastmasters.data.repository

import android.util.Log
import com.bntsoft.toastmasters.data.model.GemMemberData
import com.bntsoft.toastmasters.data.model.User
import com.bntsoft.toastmasters.data.model.Winner
import com.bntsoft.toastmasters.domain.models.WinnerCategory
import com.bntsoft.toastmasters.domain.repository.GemOfMonthRepository
import com.bntsoft.toastmasters.domain.repository.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GemOfMonthRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val userRepository: UserRepository
) : GemOfMonthRepository {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override suspend fun getMemberPerformanceData(
        year: Int,
        month: Int
    ): Flow<List<GemMemberData>> = flow {
        try {
            Log.d("GemOfMonthRepository", "Starting getMemberPerformanceData for $year-$month")
            
            val meetingIds = getMeetingsForMonth(year, month)
            Log.d("GemOfMonthRepository", "Found ${meetingIds.size} meetings for $year-$month: $meetingIds")
            
            val domainUsers = userRepository.getAllApprovedUsers()
            Log.d("GemOfMonthRepository", "Found ${domainUsers.size} approved users")
            
            val memberDataList = domainUsers.map { domainUser ->
                Log.d("GemOfMonthRepository", "Processing user: ${domainUser.name} (${domainUser.id}) - Role: ${domainUser.role}")
                // Convert domain User to data User
                val dataUser = com.bntsoft.toastmasters.data.model.User(
                    id = domainUser.id,
                    name = domainUser.name,
                    email = domainUser.email,
                    phoneNumber = domainUser.phoneNumber,
                    address = domainUser.address,
                    gender = domainUser.gender,
                    joinedDate = domainUser.joinedDate,
                    toastmastersId = domainUser.toastmastersId,
                    clubId = domainUser.clubId,
                    profileImageUrl = "",
                    fcmToken = domainUser.fcmToken ?: "",
                    mentorNames = domainUser.mentorNames,
                    roles = listOf(com.bntsoft.toastmasters.domain.models.UserRole.MEMBER),
                    status = if (domainUser.isApproved) 
                        com.bntsoft.toastmasters.data.model.User.Status.APPROVED 
                    else 
                        com.bntsoft.toastmasters.data.model.User.Status.PENDING_APPROVAL,
                    lastLogin = null,
                    createdAt = domainUser.createdAt,
                    updatedAt = domainUser.updatedAt
                )
                
                val attendanceData = getAttendanceForMember(domainUser.id, meetingIds)
                Log.d("GemOfMonthRepository", "User ${domainUser.name}: Attendance ${attendanceData.attendedMeetings}/${attendanceData.totalMeetings}")
                
                val roleData = getRoleDataForMember(domainUser.id, meetingIds)
                Log.d("GemOfMonthRepository", "User ${domainUser.name}: Roles - Speaker:${roleData.speakerCount}, Evaluator:${roleData.evaluatorCount}, Other:${roleData.otherRolesCount}")
                
                val awards = getAwardsForMember(domainUser.id, meetingIds)
                Log.d("GemOfMonthRepository", "User ${domainUser.name}: Awards count: ${awards.size}")
                
                val gemHistory = getGemHistoryForMember(domainUser.id)
                Log.d("GemOfMonthRepository", "User ${domainUser.name}: Gem history: ${gemHistory.size} entries")
                
                val performanceScore = calculatePerformanceScore(
                    attendanceData,
                    roleData,
                    awards
                )
                Log.d("GemOfMonthRepository", "User ${domainUser.name}: Performance score: $performanceScore")
                
                val memberData = GemMemberData(
                    user = dataUser,
                    attendanceData = attendanceData,
                    roleData = roleData,
                    awards = awards,
                    gemHistory = gemHistory,
                    performanceScore = performanceScore
                )
                
                Log.d("GemOfMonthRepository", "User ${domainUser.name}: Is eligible: ${memberData.isEligible}")
                memberData
            }.filter { it.isEligible }
            
            Log.d("GemOfMonthRepository", "After filtering: ${memberDataList.size} eligible members")
            emit(memberDataList.sortedByDescending { it.performanceScore })
        } catch (e: Exception) {
            Log.e("GemOfMonthRepository", "Error getting member performance data", e)
            emit(emptyList())
        }
    }

    override suspend fun getMeetingsForMonth(
        year: Int,
        month: Int
    ): List<String> {
        return try {
            // Since date is stored as string in format "2025-10-04", we need to create date range strings
            val startDate = String.format("%04d-%02d-01", year, month)
            val endDate = if (month == 12) {
                String.format("%04d-01-01", year + 1)
            } else {
                String.format("%04d-%02d-01", year, month + 1)
            }
            
            Log.d("GemOfMonthRepository", "Date range: $startDate <= date < $endDate")
            Log.d("GemOfMonthRepository", "For October 2025, this would be: 2025-10-01 <= date < 2025-11-01")

            Log.d("GemOfMonthRepository", "Querying meetings between $startDate and $endDate")

            // First, let's check if there are ANY meetings in the database
            val allMeetingsSnapshot = firestore.collection("meetings")
                .limit(5)
                .get()
                .await()
            
            Log.d("GemOfMonthRepository", "Total meetings in database: ${allMeetingsSnapshot.size()}")
            allMeetingsSnapshot.documents.forEach { doc ->
                val date = doc.getString("date")
                val meetingId = doc.getString("meetingID")
                Log.d("GemOfMonthRepository", "Sample meeting: doc=${doc.id}, date=$date, meetingID=$meetingId")
            }

            val snapshot = firestore.collection("meetings")
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThan("date", endDate)
                .orderBy("date")
                .get()
                .await()

            val meetingIds = snapshot.documents.mapNotNull { it.getString("meetingID") }

            Log.d("GemOfMonthRepository", "Found meetings: $meetingIds")
            
            // Log meeting details for debugging
            snapshot.documents.forEach { doc ->
                val date = doc.getString("date")
                val status = doc.getString("status")
                val meetingId = doc.getString("meetingID")
                Log.d("GemOfMonthRepository", "Meeting doc ${doc.id}: date=$date, status=$status, meetingID=$meetingId")
            }
            
            meetingIds
        } catch (e: Exception) {
            Log.e("GemOfMonthRepository", "Error getting meetings for month", e)
            emptyList()
        }
    }

    override suspend fun getAttendanceForMember(
        userId: String,
        meetingIds: List<String>
    ): GemMemberData.AttendanceData {
        return try {
            var attendedCount = 0
            Log.d("GemOfMonthRepository", "Checking attendance for user $userId in ${meetingIds.size} meetings")
            
            for (meetingId in meetingIds) {
                // Check if user has any assigned role in this meeting
                val assignedRoleSnapshot = firestore.collection("meetings")
                    .document(meetingId)
                    .collection("assignedRole")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()
                
                Log.d("GemOfMonthRepository", "Meeting $meetingId: Found ${assignedRoleSnapshot.size()} role assignments for user $userId")
                
                // If user has any assigned role, they attended the meeting
                if (!assignedRoleSnapshot.isEmpty) {
                    attendedCount++
                    Log.d("GemOfMonthRepository", "User $userId attended meeting $meetingId")
                    
                    // Log the roles assigned
                    assignedRoleSnapshot.documents.forEach { doc ->
                        val roles = doc.get("roles") as? List<String> ?: emptyList()
                        val memberName = doc.getString("memberName")
                        Log.d("GemOfMonthRepository", "  - Roles: $roles, Member: $memberName")
                    }
                } else {
                    Log.d("GemOfMonthRepository", "User $userId did NOT attend meeting $meetingId")
                }
            }
            
            Log.d("GemOfMonthRepository", "Final attendance for user $userId: $attendedCount/${meetingIds.size}")
            
            GemMemberData.AttendanceData(
                attendedMeetings = attendedCount,
                totalMeetings = meetingIds.size
            )
        } catch (e: Exception) {
            Log.e("GemOfMonthRepository", "Error getting attendance for member $userId", e)
            GemMemberData.AttendanceData(0, meetingIds.size)
        }
    }

    override suspend fun getRoleDataForMember(
        userId: String,
        meetingIds: List<String>
    ): GemMemberData.RoleData {
        return try {
            val allRoles = mutableListOf<GemMemberData.RoleAssignment>()
            var speakerCount = 0
            var evaluatorCount = 0
            var otherRolesCount = 0
            
            Log.d("GemOfMonthRepository", "Getting role data for user $userId in ${meetingIds.size} meetings")
            
            for (meetingId in meetingIds) {
                val assignedRolesSnapshot = firestore.collection("meetings")
                    .document(meetingId)
                    .collection("assignedRole")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()
                
                Log.d("GemOfMonthRepository", "Meeting $meetingId: Found ${assignedRolesSnapshot.size()} role documents for user $userId")
                
                val meetingDate = getMeetingDate(meetingId)
                
                for (roleDoc in assignedRolesSnapshot.documents) {
                    // Get roles array from the document
                    val roles = roleDoc.get("roles") as? List<String> ?: emptyList()
                    Log.d("GemOfMonthRepository", "  Role document ${roleDoc.id}: roles = $roles")
                    
                    // Process each role assigned to this user
                    for (roleName in roles) {
                        allRoles.add(
                            GemMemberData.RoleAssignment(
                                meetingId = meetingId,
                                roleName = roleName,
                                meetingDate = meetingDate
                            )
                        )
                        
                        when {
                            roleName.contains("Speaker", ignoreCase = true) -> {
                                speakerCount++
                                Log.d("GemOfMonthRepository", "    Found Speaker role: $roleName")
                            }
                            roleName.contains("Evaluator", ignoreCase = true) -> {
                                evaluatorCount++
                                Log.d("GemOfMonthRepository", "    Found Evaluator role: $roleName")
                            }
                            else -> {
                                otherRolesCount++
                                Log.d("GemOfMonthRepository", "    Found Other role: $roleName")
                            }
                        }
                    }
                }
            }
            
            val recentRoles = allRoles
                .sortedByDescending { it.meetingDate }
                .take(4)
                .map { it.roleName }
            
            Log.d("GemOfMonthRepository", "User $userId role summary: Speaker=$speakerCount, Evaluator=$evaluatorCount, Other=$otherRolesCount, Recent roles=$recentRoles")
            
            GemMemberData.RoleData(
                recentRoles = recentRoles,
                speakerCount = speakerCount,
                evaluatorCount = evaluatorCount,
                otherRolesCount = otherRolesCount,
                allRoles = allRoles
            )
        } catch (e: Exception) {
            Log.e("GemOfMonthRepository", "Error getting role data for member $userId", e)
            GemMemberData.RoleData(
                recentRoles = emptyList(),
                speakerCount = 0,
                evaluatorCount = 0,
                otherRolesCount = 0,
                allRoles = emptyList()
            )
        }
    }

    override suspend fun getAwardsForMember(
        userId: String,
        meetingIds: List<String>
    ): List<GemMemberData.Award> {
        return try {
            val awards = mutableListOf<GemMemberData.Award>()
            
            Log.d("GemOfMonthRepository", "Getting awards for user $userId in ${meetingIds.size} meetings")
            
            // First, get the user's name to match against memberName field
            val userDoc = firestore.collection("users").document(userId).get().await()
            val userName = userDoc.getString("name") ?: ""
            Log.d("GemOfMonthRepository", "User $userId name: $userName")
            
            for (meetingId in meetingIds) {
                Log.d("GemOfMonthRepository", "Checking awards in meeting $meetingId for user $userName")
                
                val winnersSnapshot = firestore.collection("meetings")
                    .document(meetingId)
                    .collection("winners")
                    .whereEqualTo("memberName", userName)
                    .whereEqualTo("member", true)
                    .get()
                    .await()
                
                Log.d("GemOfMonthRepository", "Found ${winnersSnapshot.size()} awards for $userName in meeting $meetingId")
                
                for (winnerDoc in winnersSnapshot.documents) {
                    val categoryString = winnerDoc.getString("category") ?: ""
                    val memberName = winnerDoc.getString("memberName") ?: ""
                    val member = winnerDoc.getBoolean("member") ?: false
                    
                    Log.d("GemOfMonthRepository", "Award: category=$categoryString, memberName=$memberName, member=$member")
                    
                    // Convert string to WinnerCategory enum
                    val winnerCategory = try {
                        com.bntsoft.toastmasters.domain.models.WinnerCategory.valueOf(categoryString)
                    } catch (e: IllegalArgumentException) {
                        Log.w("GemOfMonthRepository", "Unknown winner category: $categoryString")
                        null
                    }
                    
                    if (winnerCategory != null) {
                        awards.add(
                            GemMemberData.Award(
                                category = winnerCategory,
                                meetingId = meetingId,
                                meetingDate = getMeetingDate(meetingId)
                            )
                        )
                    }
                }
            }
            
            Log.d("GemOfMonthRepository", "Total awards found for user $userName: ${awards.size}")
            awards.forEach { award ->
                Log.d("GemOfMonthRepository", "  - ${award.displayName} (${award.category})")
            }
            
            awards
        } catch (e: Exception) {
            Log.e("GemOfMonthRepository", "Error getting awards for member $userId", e)
            emptyList()
        }
    }

    override suspend fun getGemHistoryForMember(
        userId: String
    ): List<String> {
        return try {
            val gemHistorySnapshot = firestore.collection("gemOfTheMonth")
                .whereEqualTo("userId", userId)
                .orderBy("year")
                .orderBy("month")
                .get()
                .await()
            
            gemHistorySnapshot.documents.mapNotNull { doc ->
                val year = doc.getLong("year")?.toInt()
                val month = doc.getLong("month")?.toInt()
                if (year != null && month != null) {
                    val monthNames = arrayOf(
                        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                    )
                    "${monthNames[month - 1]} $year"
                } else null
            }
        } catch (e: Exception) {
            Log.e("GemOfMonthRepository", "Error getting gem history for member $userId", e)
            emptyList()
        }
    }

    override suspend fun saveGemOfTheMonth(
        meetingId: String,
        userId: String,
        year: Int,
        month: Int
    ): Result<Unit> {
        return try {
            // Get member data for the record
            var memberData: GemMemberData? = null
            getMemberPerformanceData(year, month).collect { memberList ->
                memberData = memberList.find { it.user.id == userId }
            }
            
            if (memberData == null) {
                return Result.failure(Exception("Member data not found"))
            }
            
            // Create document ID as "userId_year_month"
            val documentId = "${userId}_${year}_${month}"
            
            val gemRecord = com.bntsoft.toastmasters.data.model.GemOfTheMonth(
                id = documentId,
                userId = userId,
                memberName = memberData!!.user.name,
                year = year,
                month = month,
                performanceScore = memberData!!.performanceScore,
                attendanceData = com.bntsoft.toastmasters.data.model.GemOfTheMonth.AttendanceData(
                    attendedMeetings = memberData!!.attendanceData.attendedMeetings,
                    totalMeetings = memberData!!.attendanceData.totalMeetings
                ),
                roleData = com.bntsoft.toastmasters.data.model.GemOfTheMonth.RoleData(
                    speakerCount = memberData!!.roleData.speakerCount,
                    evaluatorCount = memberData!!.roleData.evaluatorCount,
                    otherRolesCount = memberData!!.roleData.otherRolesCount,
                    recentRoles = memberData!!.roleData.recentRoles
                ),
                awards = memberData!!.awards.map { it.displayName }
            )
            
            firestore.collection("gemOfTheMonth")
                .document(documentId)
                .set(gemRecord)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("GemOfMonthRepository", "Error saving gem of the month", e)
            Result.failure(e)
        }
    }

    private suspend fun getMeetingDate(meetingId: String): String {
        return try {
            val meetingDoc = firestore.collection("meetings")
                .document(meetingId)
                .get()
                .await()
            
            // Try to get date as string first (based on your Firebase structure)
            val dateString = meetingDoc.getString("date")
            if (!dateString.isNullOrEmpty()) {
                return dateString
            }
            
            // Fallback to timestamp if date is stored as timestamp
            val date = meetingDoc.getDate("date")
            date?.let { dateFormat.format(it) } ?: ""
        } catch (e: Exception) {
            Log.e("GemOfMonthRepository", "Error getting meeting date for $meetingId", e)
            ""
        }
    }

    private fun calculatePerformanceScore(
        attendanceData: GemMemberData.AttendanceData,
        roleData: GemMemberData.RoleData,
        awards: List<GemMemberData.Award>
    ): Int {
        var score = 0
        
        score += (attendanceData.attendancePercentage * 0.3).toInt()
        
        score += roleData.speakerCount * 15
        score += roleData.evaluatorCount * 10
        score += roleData.otherRolesCount * 5
        
        score += awards.size * 20
        
        return score
    }
}
