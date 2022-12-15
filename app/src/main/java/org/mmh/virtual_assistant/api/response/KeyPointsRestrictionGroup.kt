package org.mmh.virtual_assistant.api.response

data class KeyPointsRestrictionGroup(
    val KeyPointsRestriction: List<KeyPointsRestriction>,
    val HoldInSeconds: Int,
    val PhaseDialogue: String,
    val Phase: Int
)
