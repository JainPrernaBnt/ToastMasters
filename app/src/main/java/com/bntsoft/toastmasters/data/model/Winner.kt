package com.bntsoft.toastmasters.data.model

import com.bntsoft.toastmasters.domain.models.WinnerCategory
import com.google.firebase.Timestamp

data class Winner(
    val id: String = "",
    val meetingId: String = "",
    val category: WinnerCategory = WinnerCategory.BEST_SPEAKER, // Default category
    val isMember: Boolean = true,            // True if winner is a member, false if guest
    val winnerUserId: Int? = null,           // Member ID (nullable for guest)
    val memberName: String? = null,          // Member name
    val guestName: String? = null,            // Guest name (filled if isMember = false)
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

