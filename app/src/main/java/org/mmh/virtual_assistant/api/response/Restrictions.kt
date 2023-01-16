package org.mmh.virtual_assistant.api.response

data class Restrictions(
    val Scale: String,
    val LineType: String,
    val NoOfKeyPoints: Int,
    val Direction: String,
    val StartKeyPosition: String,
    val MiddleKeyPosition: String,
    val EndKeyPosition: String,
    val MinValidationValue: Int,
    val MaxValidationValue: Int,
    val LowestMinValidationValue: Int,
    val LowestMaxValidationValue: Int,
    val AngleArea: String,
    val DrawExtensionFlexion: Boolean
)
