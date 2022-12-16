package org.mmh.virtual_assistant.exercise

import org.mmh.virtual_assistant.core.Utilities
import org.mmh.virtual_assistant.domain.model.BodyPart
import org.mmh.virtual_assistant.domain.model.Constraint
import org.mmh.virtual_assistant.domain.model.Person

object CommonInstructions {

    private const val LEFT_ELBOW_MIN = 150f
    private const val LEFT_ELBOW_MAX = 190f
    private const val RIGHT_ELBOW_MIN = 150f
    private const val RIGHT_ELBOW_MAX = 190f

    private val LEFT_HAND_KEY_POINTS = setOf(
        BodyPart.LEFT_WRIST.position,
        BodyPart.LEFT_ELBOW.position,
        BodyPart.LEFT_SHOULDER.position
    )
    private val RIGHT_HAND_KEY_POINTS = setOf(
        BodyPart.RIGHT_WRIST.position,
        BodyPart.RIGHT_ELBOW.position,
        BodyPart.RIGHT_SHOULDER.position
    )

    fun isBothHandStraight(person: Person, constraints: List<Constraint>): Boolean {
        return true
    }

    fun isLeftHandStraight(person: Person, constraint: Constraint): Boolean {
        val keyPoints = setOf(
            constraint.startPointIndex,
            constraint.middlePointIndex,
            constraint.endPointIndex
        )
        if (keyPoints == LEFT_HAND_KEY_POINTS) {
            if (constraint.minValue >= LEFT_ELBOW_MIN && constraint.maxValue <= LEFT_ELBOW_MAX) {
                val angle = Utilities.angle(
                    startPoint = person.keyPoints[constraint.startPointIndex].toRealPoint(),
                    middlePoint = person.keyPoints[constraint.middlePointIndex].toRealPoint(),
                    endPoint = person.keyPoints[constraint.endPointIndex].toRealPoint(),
                    clockWise = constraint.clockWise
                )
                if (angle < constraint.minValue || angle > constraint.maxValue) {
                    return false
                }
            }
        }
        return true
    }

    fun isRightHandStraight(person: Person, constraint: Constraint): Boolean {
        val keyPoints = setOf(
            constraint.startPointIndex,
            constraint.middlePointIndex,
            constraint.endPointIndex
        )
        if (keyPoints == RIGHT_HAND_KEY_POINTS) {
            if (constraint.minValue >= RIGHT_ELBOW_MIN && constraint.maxValue <= RIGHT_ELBOW_MAX) {
                val angle = Utilities.angle(
                    startPoint = person.keyPoints[constraint.startPointIndex].toRealPoint(),
                    middlePoint = person.keyPoints[constraint.middlePointIndex].toRealPoint(),
                    endPoint = person.keyPoints[constraint.endPointIndex].toRealPoint(),
                    clockWise = constraint.clockWise
                )
                if (angle < constraint.minValue || angle > constraint.maxValue) {
                    return false
                }
            }
        }
        return true
    }
}