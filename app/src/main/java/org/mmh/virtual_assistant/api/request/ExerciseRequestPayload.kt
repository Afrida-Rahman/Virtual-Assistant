package org.mmh.virtual_assistant.api.request

data class ExerciseRequestPayload(
    val KeyPointsRestrictions: List<ExerciseData>,
    val Tenant : String
)
