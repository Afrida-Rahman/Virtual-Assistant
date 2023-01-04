package org.mmh.virtual_assistant.domain.model

data class Phase(
    val phaseNumber: Int,
    val constraints: List<Constraint>,
    val holdTime: Int,
    val phaseDialogue: String?
)