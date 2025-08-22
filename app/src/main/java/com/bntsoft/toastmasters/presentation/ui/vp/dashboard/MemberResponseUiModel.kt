package com.bntsoft.toastmasters.presentation.ui.vp.dashboard

import com.bntsoft.toastmasters.domain.model.MemberResponse
import com.bntsoft.toastmasters.domain.model.User

data class MemberResponseUiModel(
    val user: User,
    val response: MemberResponse?
) {
    val availability: MemberResponse.AvailabilityStatus
        get() = response?.availability ?: MemberResponse.AvailabilityStatus.NOT_RESPONDED

    companion object {
        fun fromUserAndResponse(user: User, response: MemberResponse?): MemberResponseUiModel {
            return MemberResponseUiModel(user, response)
        }
    }
}
