package com.bntsoft.toastmasters.data.model

sealed class MeetingRoleItem {
    data class MemberRoleItem(val memberRole: MemberRole) : MeetingRoleItem()
    
    data class GrammarianDetails(
        val wordOfTheDay: String,
        val wordMeaning: List<String>,
        val wordExamples: List<String>,
        val idiomOfTheDay: String,
        val idiomMeaning: String,
        val idiomExamples: List<String>
    ) : MeetingRoleItem()
    
    data class SpeakerDetails(
        val level: Int,
        val pathwaysTrack: String,
        val projectNumber: String,
        val projectTitle: String,
        val speechObjectives: String,
        val speechTime: String,
        val speechTitle: String
    ) : MeetingRoleItem()
}
