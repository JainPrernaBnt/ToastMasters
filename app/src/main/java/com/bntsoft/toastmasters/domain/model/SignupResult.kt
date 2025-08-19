package com.bntsoft.toastmasters.domain.model

data class SignupResult(
    val user: User,
    val requiresApproval: Boolean
)
