package org.mmh.virtual_assistant.exercise.home.back

import android.content.Context
import org.mmh.virtual_assistant.core.Point
import org.mmh.virtual_assistant.core.Utilities
import org.mmh.virtual_assistant.core.VisualizationUtils
import org.mmh.virtual_assistant.domain.model.Person
import org.mmh.virtual_assistant.exercise.home.HomeExercise

class PronePressUpLumbar(
    context: Context
) : HomeExercise(
    context = context,
    id = 158
) {
    private var downElbowAngleMin = 30f
    private var downElbowAngleMax = 200f
    private var downHipAngleMin = 170f
    private var downHipAngleMax = 190f

    private var upElbowAngleMin = 60f
    private var upElbowAngleMax = 100f
    private var upHipAngleMin = 130f
    private var upHipAngleMax = 180f

    private var rightStateIndex = 0

    override var wrongStateIndex = 0
    private var wrongFrameCount = 0
    private val maxWrongCountFrame = 3
    override fun rightExerciseCount(
        person: Person,
        canvasHeight: Int,
        canvasWidth: Int
    ) {
        val leftWristPoint = Point(
            person.keyPoints[9].coordinate.x,
            -person.keyPoints[9].coordinate.y
        )
        val leftElbowPoint = Point(
            person.keyPoints[7].coordinate.x,
            -person.keyPoints[7].coordinate.y
        )
        val leftShoulderPoint = Point(
            person.keyPoints[5].coordinate.x,
            -person.keyPoints[5].coordinate.y
        )
        val leftHipPoint = Point(
            person.keyPoints[11].coordinate.x,
            -person.keyPoints[11].coordinate.y
        )
        val leftKneePoint = Point(
            person.keyPoints[13].coordinate.x,
            -person.keyPoints[13].coordinate.y
        )
        if (rightCountPhases.size >= 2) {
            if (rightCountPhases[0].constraints.size > 1) {
                downElbowAngleMin = rightCountPhases[0].constraints[0].minValue.toFloat()
                downElbowAngleMax = rightCountPhases[0].constraints[0].maxValue.toFloat()
                downHipAngleMin = rightCountPhases[0].constraints[1].minValue.toFloat()
                downHipAngleMax = rightCountPhases[0].constraints[1].maxValue.toFloat()

                upElbowAngleMin = rightCountPhases[1].constraints[0].minValue.toFloat()
                upElbowAngleMax = rightCountPhases[1].constraints[0].maxValue.toFloat()
                upHipAngleMin = rightCountPhases[1].constraints[1].minValue.toFloat()
                upHipAngleMax = rightCountPhases[1].constraints[1].maxValue.toFloat()
            }
        }

        val insideBox = VisualizationUtils.isInsideBox(person, canvasHeight, canvasWidth)
        val elbowAngle = Utilities.angle(leftWristPoint, leftElbowPoint, leftShoulderPoint, false)
        val hipAngle = Utilities.angle(leftShoulderPoint, leftHipPoint, leftKneePoint, false)
        val rightCountStates: Array<FloatArray> = arrayOf(
            floatArrayOf(
                downElbowAngleMin,
                downElbowAngleMax,
                downHipAngleMin,
                downHipAngleMax
            ),
            floatArrayOf(
                upElbowAngleMin,
                upElbowAngleMax,
                upHipAngleMin,
                upHipAngleMax
            ),
            floatArrayOf(
                downElbowAngleMin,
                downElbowAngleMax,
                downHipAngleMin,
                downHipAngleMax
            )
        )
        if (elbowAngle > rightCountStates[rightStateIndex][0] && elbowAngle < rightCountStates[rightStateIndex][1]
            && hipAngle > rightCountStates[rightStateIndex][2] && hipAngle < rightCountStates[rightStateIndex][3]
            && insideBox
        ) {
            rightStateIndex += 1
            if (rightStateIndex == rightCountStates.size - 1) {
                wrongStateIndex = 0
            }
            if (rightStateIndex == rightCountStates.size) {
                rightStateIndex = 0
                repetitionCount()
            }
        } else {
            if (!insideBox) {
                onEvent(CommonInstructionEvent.OutSideOfBox)
            } else if (wrongFrameCount >= maxWrongCountFrame) {
                wrongFrameCount = 0
            }
        }
    }

    override fun wrongExerciseCount(person: Person, canvasHeight: Int, canvasWidth: Int) {

    }
}