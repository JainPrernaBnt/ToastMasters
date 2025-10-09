package com.bntsoft.toastmasters.data.remote.dto

data class MemberResponseDto(
    val id: String = "",
    val meetingId: String = "",
    val memberId: String = "",
    val userId: String = "",
    val availability: String = "",
    val preferredRoles: List<String> = emptyList(),
    val notes: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
) {
    companion object {
        fun empty() = MemberResponseDto()
    }
}
