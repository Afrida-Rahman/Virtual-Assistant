package org.mmh.virtual_assistant.api.request

data class QResponse(
    val QuestionId: Int,
    val AnswerId: Long,
    val AnswerValue: String
)
