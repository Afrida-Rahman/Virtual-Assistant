package org.mmh.virtual_assistant.api.request

data class ExerciseTrackingPayload(
    val ExerciseId: Int,
    val TestId: String,
    val ProtocolId: Int,
    val PatientId: String,
    val ExerciseDate: String,
    val NoOfReps: Int,
    val NoOfSets: Int,
    val NoOfWrongCount: Int,
    val Tenant: String
)
