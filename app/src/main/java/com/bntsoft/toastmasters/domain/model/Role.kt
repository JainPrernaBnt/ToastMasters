package com.bntsoft.toastmasters.domain.model

data class Role(
    val id: String,
    val name: String,
    val description: String = "",
    val isRequired: Boolean = false
) {
    companion object {
        // Common Toastmasters roles
        val TOASTMASTER = Role("toastmaster", "Toastmaster", "The meeting's director and host")
        val SPEAKER = Role("speaker", "Speaker", "Delivers a prepared speech")
        val TABLE_TOPICS_MASTER = Role(
            "table_topics_master",
            "Table Topics Master",
            "Leads the impromptu speaking session"
        )
        val GENERAL_EVALUATOR =
            Role("general_evaluator", "General Evaluator", "Leads the evaluation team")
        val EVALUATOR = Role("evaluator", "Evaluator", "Provides feedback to a speaker")
        val GRAMMARIAN = Role("grammarian", "Grammarian", "Monitors language and grammar usage")
        val AH_COUNTER = Role("ah_counter", "Ah-Counter", "Notes filler words and sounds")
        val TIMER = Role("timer", "Timer", "Tracks time for each segment")
        val VOTE_COUNTER =
            Role("vote_counter", "Vote Counter", "Tracks votes and announces winners")
        val LISTENER =
            Role("listener", "Listener", "Listens and provides feedback on meeting content")

        // Add more roles as needed
        val ALL_ROLES = listOf(
            TOASTMASTER,
            SPEAKER,
            TABLE_TOPICS_MASTER,
            GENERAL_EVALUATOR,
            EVALUATOR,
            GRAMMARIAN,
            AH_COUNTER,
            TIMER,
            VOTE_COUNTER,
            LISTENER
        )

        fun fromId(id: String): Role? = ALL_ROLES.find { it.id == id }
    }

    override fun toString(): String = name
}
