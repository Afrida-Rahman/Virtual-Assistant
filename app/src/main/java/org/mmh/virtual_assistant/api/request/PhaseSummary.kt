package org.mmh.virtual_assistant.api.request

data class PhaseSummary(
    val PhaseNumber: Int,
    val Restrictions: List<Restriction>
)
