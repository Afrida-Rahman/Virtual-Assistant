package org.mmh.virtual_assistant.domain.model

data class APiUrl(
    val getAssessmentUrl: String,
    val getExerciseUrl: String,
    val getExerciseConstraintsURL: String,
    val saveExerciseTrackingURL: String
)
