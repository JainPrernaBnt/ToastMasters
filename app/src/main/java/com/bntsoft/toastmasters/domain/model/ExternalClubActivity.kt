package com.bntsoft.toastmasters.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class ExternalClubActivity(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userProfilePicture: String? = null,
    val clubName: String = "",
    val clubLocation: String? = null,
    val meetingDate: Date = Date(),
    val rolePlayed: String = "",
    val notes: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable
