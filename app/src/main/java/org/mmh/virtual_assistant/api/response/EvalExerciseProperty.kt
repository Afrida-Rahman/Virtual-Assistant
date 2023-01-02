package org.mmh.virtual_assistant.api.response

data class EvalExerciseProperty(
    val Id: Int,
    val DayNumber: Int,
    val DayName: String,
    val HoldInSeconds: Int,
    val RepetitionInCount: Int,
    val SetInCount: Int,
    val FrequencyInDay: Int
)