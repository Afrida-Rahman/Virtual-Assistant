package org.mmh.virtual_assistant.domain.model

import android.graphics.PointF
import org.mmh.virtual_assistant.core.Point

data class KeyPoint(
    val bodyPart: BodyPart,
    var coordinate: PointF,
    val score: Float
) {
    fun toRealPoint(): Point {
        return Point(
            coordinate.x,
            -coordinate.y
        )
    }

    fun toCanvasPoint(): Point {
        return Point(
            coordinate.x,
            coordinate.y
        )
    }
}
