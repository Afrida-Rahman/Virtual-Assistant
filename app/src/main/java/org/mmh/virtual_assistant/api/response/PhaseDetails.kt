package org.mmh.virtual_assistant.api.response

data class PhaseDetails(
    val HoldInSeconds: Int,
    val Restrictions: List<Restrictions>,
    val PhaseNumber: Int,
    val PhaseDialogue: String,
    val CapturedImage: String
)