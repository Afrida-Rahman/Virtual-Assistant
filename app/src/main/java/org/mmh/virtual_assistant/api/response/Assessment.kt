package org.mmh.virtual_assistant.api.response

data class Assessment(
    val TestId: String,
    val BodyRegionId: Int,
    val BodyRegionName: String,
    val ProviderName: String,
    val ProviderId: String,
    val CreatedOnUtc: String,
    val IsReportReady: Boolean,
    val RegistrationType: String,
    val TotalExercise: Int
)
