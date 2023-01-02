package org.mmh.virtual_assistant.api.request

data class ExerciseTrackingPayload(
    val Tenant: String,
    val PatientId: String,
    val TestId: String,
    val ExerciseId: Int,
    val ProtocolId: Int,
    val ExerciseDate: String,
    val AssignSets: Int = 0,
    val AssignReps: Int = 0,
    val NoOfReps: Int,
    val NoOfSets: Int,
    val NoOfWrongCount: Int,
    val TotalTime: Int = 0,
    val Phases: List<PhaseSummary>,
    val Responses: List<QResponse>
)
