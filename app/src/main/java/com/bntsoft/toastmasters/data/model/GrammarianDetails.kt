package com.bntsoft.toastmasters.data.model

data class GrammarianDetails(
    val meetingID: String = "",
    val userId: String = "",
    val wordOfTheDay: String = "",
    val wordMeaning: List<String> = emptyList(),
    val wordExamples: List<String> = emptyList(),
    val idiomOfTheDay: String = "",
    val idiomMeaning: String = "",
    val idiomExamples: List<String> = emptyList()
)
