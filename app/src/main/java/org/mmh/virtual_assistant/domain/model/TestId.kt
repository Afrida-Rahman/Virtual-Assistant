package org.mmh.virtual_assistant.domain.model

data class TestId(
    val id: String,
    val bodyRegionId: Int,
    val bodyRegionName: String,
    val providerName: String?,
    val providerId: String?,
    val testDate: String,
    val isReportReady: Boolean,
    val registrationType: String,
    val totalExercises: Int
)
