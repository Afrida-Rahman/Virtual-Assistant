package org.mmh.virtual_assistant.api.response

data class KeyPointRestrictionsItem(
    val ExerciseMedia: String,
    val IsPhaseFinished: Boolean,
    val ExerciseId: Int,
    val KeyPointsRestrictionGroup: List<KeyPointsRestrictionGroup>,
    val Tenant: String
)
