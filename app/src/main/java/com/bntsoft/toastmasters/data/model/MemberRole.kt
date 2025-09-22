package com.bntsoft.toastmasters.data.model

data class EvaluatorInfo(
    val name: String = "",
    val role: String = ""
)

data class MemberRole(
    val id: String = "",
    val memberName: String = "",
    val roles: List<String> = emptyList(),
    val evaluators: List<EvaluatorInfo> = emptyList(),
    // Legacy support - will be removed in future
    val evaluator: String = "",
    val evaluatorRole: String = ""
) {
    fun getCombinedEvaluators(): List<EvaluatorInfo> {
        return if (evaluator.isNotBlank()) {
            evaluators + EvaluatorInfo(evaluator, evaluatorRole)
        } else {
            evaluators
        }
    }
}
