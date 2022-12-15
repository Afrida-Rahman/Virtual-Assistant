package org.mmh.virtual_assistant.api.response

data class KeyPointsRestriction(
    val ResistanceId: Int,
    val AngleArea: String,
    val CapturedImage: String,
    val Direction: String,
    val EndKeyPosition: String,
    val ExerciseId: Int,
    val Id: Int,
    val LineType: String,
    val MaxValidationValue: Int,
    val MiddleKeyPosition: String,
    val MinValidationValue: Int,
    val NoOfKeyPoints: Int,
    val Phase: Int,
    val Scale: String,
    val StartKeyPosition: String
)
