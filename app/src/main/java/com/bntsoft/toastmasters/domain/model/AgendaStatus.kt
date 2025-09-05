package com.bntsoft.toastmasters.domain.model

enum class AgendaStatus {
    DRAFT,
    FINALIZED;

    companion object {
        fun fromString(value: String): AgendaStatus {
            return values().find { it.name.equals(value, ignoreCase = true) } ?: DRAFT
        }
    }
}
