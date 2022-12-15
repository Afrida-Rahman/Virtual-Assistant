package org.mmh.virtual_assistant.api.request

data class LogInRequest(
    val Email: String,
    val Password: String,
    val Tenant: String
)