package com.bntsoft.toastmasters.data.model

data class MemberRole(
    val id: String = "",
    val memberName: String = "",
    val roles: List<String> = emptyList(),
    val evaluator: String = "", // evaluator name
    val evaluatorRole: String = ""  // evaluator role (e.g., "Evaluator 1")
)
