package com.bntsoft.toastmasters.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.bntsoft.toastmasters.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(userEntity: UserEntity)

    @Query("""SELECT * FROM members WHERE (email = :identifier OR number = :numberIdentifier)AND password = :password LIMIT 1""")
    suspend fun login(
        identifier: String,         // for email
        numberIdentifier: Int?,     // for number
        password: String
    ): UserEntity?

    @Query("UPDATE members SET isApproved = 1 WHERE id = :userId")
    suspend fun approveMember(userId: Int)

    @Transaction
    suspend fun assignVpPermissionTo(userId: Int) {
        removeVpPermissionFromCurrent()
        grantVpPermission(userId)
    }

    @Query("UPDATE members SET hasVpPermission = 0 WHERE hasVpPermission = 1")
    suspend fun removeVpPermissionFromCurrent()

    @Query("UPDATE members SET hasVpPermission = 1 WHERE id = :userId")
    suspend fun grantVpPermission(userId: Int)

    @Query("SELECT * FROM members WHERE isApproved = 1")
    fun getApprovedMembers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM members WHERE isApproved = 0")
    fun getPendingMembers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM members WHERE hasVpPermission = 1 LIMIT 1")
    fun getVpPermissionHolder(): Flow<UserEntity?>

    @Query("UPDATE members SET mentor = :mentorName, mentorAssignedBy = :assignedBy, mentorAssignedDate = :assignedDate WHERE id = :userId")
    suspend fun assignMentor(
        userId: Int,
        mentorName: String,
        assignedBy: String,
        assignedDate: Long
    )
}