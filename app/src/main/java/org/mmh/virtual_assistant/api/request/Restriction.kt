package org.mmh.virtual_assistant.api.request

data class Restriction(
    val StartKeyPosition: String,
    val MiddleKeyPosition: String,
    val EndKeyPosition: String,
    val AverageMin: Int,
    val AverageMax: Int
)
