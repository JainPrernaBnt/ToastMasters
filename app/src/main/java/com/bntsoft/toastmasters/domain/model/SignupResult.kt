package com.bntsoft.toastmasters.domain.model

import com.bntsoft.toastmasters.domain.model.User

data class SignupResult(
    val user: User,
    val requiresApproval: Boolean
)
