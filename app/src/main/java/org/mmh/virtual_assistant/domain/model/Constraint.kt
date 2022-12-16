package org.mmh.virtual_assistant.domain.model

import android.graphics.Color

data class Constraint(
    val type: ConstraintType,
    val startPointIndex: Int,
    val middlePointIndex: Int,
    val endPointIndex: Int,
    val clockWise: Boolean = false,
    var color: Int = Color.WHITE,
    val minValue: Int,
    val maxValue: Int,
    val uniqueId: Int
)
