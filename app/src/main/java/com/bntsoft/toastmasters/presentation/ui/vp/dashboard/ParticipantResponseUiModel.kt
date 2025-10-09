package com.bntsoft.toastmasters.presentation.ui.vp.dashboard

import com.bntsoft.toastmasters.domain.model.Guest
import com.bntsoft.toastmasters.domain.model.MemberResponse
import com.bntsoft.toastmasters.domain.model.User

data class ParticipantResponseUiModel(
    val user: User?,
    val guest: Guest?,
    val response: MemberResponse?
) {
    val availability: MemberResponse.AvailabilityStatus
        get() = response?.availability ?: MemberResponse.AvailabilityStatus.NOT_RESPONDED

    val name: String
        get() = user?.name ?: guest?.name ?: "Unknown"

    val isGuest: Boolean
        get() = guest != null

    companion object {
        fun fromUserAndResponse(user: User, response: MemberResponse?): ParticipantResponseUiModel {
            return ParticipantResponseUiModel(user, null, response)
        }

        fun fromGuestAndResponse(guest: Guest, response: MemberResponse?): ParticipantResponseUiModel {
            return ParticipantResponseUiModel(null, guest, response)
        }
    }
}
