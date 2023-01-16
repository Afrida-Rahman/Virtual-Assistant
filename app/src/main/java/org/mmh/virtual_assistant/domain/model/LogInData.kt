package org.mmh.virtual_assistant.domain.model

data class LogInData(
    val firstName: String,
    val lastName: String,
    val patientId: String,
    val tenant: String
)