package org.mmh.virtual_assistant.exercise.home.neck

import android.content.Context
import org.mmh.virtual_assistant.core.Point
import org.mmh.virtual_assistant.core.Utilities
import org.mmh.virtual_assistant.core.VisualizationUtils
import org.mmh.virtual_assistant.domain.model.Person
import org.mmh.virtual_assistant.exercise.home.HomeExercise

class IsometricCervicalExtensionInSitting(
    context: Context
) : HomeExercise(
    context = context,
    id = 75
) {
    private var shoulderAngleDownMin = 0f
    private var shoulderAngleDownMax = 30f
    private var shoulderAngleUpMin = 115f
    private var shoulderAngleUpMax = 140f

    private var wrongShoulderAngleDownMin = 0f
    private var wrongShoulderAngleDownMax = 30f
    private var wrongShoulderAngleUpMin = 150f
    private var wrongShoulderAngleUpMax = 190f

    private val totalStates = 3
    private var rightStateIndex = 0
    override var wrongStateIndex = 0

    fun rightExercise(
        person: Person,
        canvasHeight: Int,
        canvasWidth: Int
    ) {
        val leftShoulderPoint = Point(
            person.keyPoints[5].coordinate.x,
            -person.keyPoints[5].coordinate.y
        )
        val rightShoulderPoint = Point(
            person.keyPoints[6].coordinate.x,
            -person.keyPoints[6].coordinate.y
        )
        val leftElbowPoint = Point(
            person.keyPoints[7].coordinate.x,
            -person.keyPoints[7].coordinate.y
        )
        val rightElbowPoint = Point(
            person.keyPoints[8].coordinate.x,
            -person.keyPoints[8].coordinate.y
        )
        val leftHipPoint = Point(
            person.keyPoints[11].coordinate.x,
            -person.keyPoints[11].coordinate.y
        )
        val rightHipPoint = Point(
            person.keyPoints[12].coordinate.x,
            -person.keyPoints[12].coordinate.y
        )
        if (rightCountPhases.size >= 2) {
            shoulderAngleDownMin = rightCountPhases[0].constraints[0].minValue.toFloat()
            shoulderAngleDownMax = rightCountPhases[0].constraints[0].maxValue.toFloat()
            shoulderAngleUpMin = rightCountPhases[1].constraints[0].minValue.toFloat()
            shoulderAngleUpMax = rightCountPhases[1].constraints[0].maxValue.toFloat()
        }

        val rightCountStates: Array<FloatArray> = arrayOf(
            floatArrayOf(
                shoulderAngleDownMin,
                shoulderAngleDownMax,
                shoulderAngleDownMin,
                shoulderAngleDownMax
            ),
            floatArrayOf(
                shoulderAngleUpMin,
                shoulderAngleUpMax,
                shoulderAngleUpMin,
                shoulderAngleUpMax
            ),
            floatArrayOf(
                shoulderAngleDownMin,
                shoulderAngleDownMax,
                shoulderAngleDownMin,
                shoulderAngleDownMax
            )
        )

        val leftShoulderAngle =
            Utilities.angle(leftElbowPoint, leftShoulderPoint, leftHipPoint, false)
        val rightShoulderAngle =
            Utilities.angle(rightElbowPoint, rightShoulderPoint, rightHipPoint, true)
        val insideBox = VisualizationUtils.isInsideBox(person, canvasHeight, canvasWidth)

        if (leftShoulderAngle > rightCountStates[rightStateIndex][0]
            && leftShoulderAngle < rightCountStates[rightStateIndex][1]
            && rightShoulderAngle > rightCountStates[rightStateIndex][2]
            && rightShoulderAngle < rightCountStates[rightStateIndex][3]
            && insideBox
        ) {
            rightStateIndex += 1
            if (rightStateIndex == totalStates) {
                rightStateIndex = 0
                repetitionCount()
            }
        } else {
            if (!insideBox) {
                onEvent(CommonInstructionEvent.OutSideOfBox)
            }
        }
    }

    fun wrongExercise(person: Person, canvasHeight: Int, canvasWidth: Int) {
        val leftShoulderPoint = Point(
            person.keyPoints[5].coordinate.x,
            -person.keyPoints[5].coordinate.y
        )
        val rightShoulderPoint = Point(
            person.keyPoints[6].coordinate.x,
            -person.keyPoints[6].coordinate.y
        )
        val leftElbowPoint = Point(
            person.keyPoints[7].coordinate.x,
            -person.keyPoints[7].coordinate.y
        )
        val rightElbowPoint = Point(
            person.keyPoints[8].coordinate.x,
            -person.keyPoints[8].coordinate.y
        )
        val leftHipPoint = Point(
            person.keyPoints[11].coordinate.x,
            -person.keyPoints[11].coordinate.y
        )
        val rightHipPoint = Point(
            person.keyPoints[12].coordinate.x,
            -person.keyPoints[12].coordinate.y
        )

        wrongShoulderAngleDownMin = shoulderAngleDownMin
        wrongShoulderAngleDownMax = shoulderAngleDownMax
        wrongShoulderAngleUpMin = shoulderAngleUpMin + 35
        wrongShoulderAngleUpMax = shoulderAngleUpMax + 50

        val wrongCountStates1: Array<FloatArray> = arrayOf(
            floatArrayOf(
                wrongShoulderAngleDownMin,
                wrongShoulderAngleDownMax,
                wrongShoulderAngleDownMin,
                wrongShoulderAngleDownMax
            ),
            floatArrayOf(
                wrongShoulderAngleUpMin,
                wrongShoulderAngleUpMax,
                wrongShoulderAngleUpMin,
                wrongShoulderAngleUpMax
            ),
            floatArrayOf(
                wrongShoulderAngleDownMin,
                wrongShoulderAngleDownMax,
                wrongShoulderAngleDownMin,
                wrongShoulderAngleDownMax
            )
        )
        val wrongCountStates2: Array<FloatArray> = arrayOf(
            floatArrayOf(
                wrongShoulderAngleDownMin,
                wrongShoulderAngleDownMax,
                wrongShoulderAngleDownMin,
                wrongShoulderAngleDownMax
            ),
            floatArrayOf(
                wrongShoulderAngleUpMin,
                wrongShoulderAngleUpMax,
                wrongShoulderAngleDownMin,
                wrongShoulderAngleDownMax
            ),
            floatArrayOf(
                wrongShoulderAngleDownMin,
                wrongShoulderAngleDownMax,
                wrongShoulderAngleDownMin,
                wrongShoulderAngleDownMax
            )
        )

        val leftShoulderAngle = Utilities.angle(leftElbowPoint, leftShoulderPoint, leftHipPoint)
        val rightShoulderAngle =
            Utilities.angle(rightElbowPoint, rightShoulderPoint, rightHipPoint, true)
        val insideBox = VisualizationUtils.isInsideBox(person, canvasHeight, canvasWidth)
        if (
            (leftShoulderAngle > wrongCountStates1[wrongStateIndex][0]
                    && leftShoulderAngle < wrongCountStates1[wrongStateIndex][1]
                    && rightShoulderAngle > wrongCountStates1[wrongStateIndex][2]
                    && rightShoulderAngle < wrongCountStates1[wrongStateIndex][3])
            || (leftShoulderAngle > wrongCountStates2[wrongStateIndex][0]
                    && leftShoulderAngle < wrongCountStates2[wrongStateIndex][1]
                    && rightShoulderAngle > wrongCountStates2[wrongStateIndex][2]
                    && rightShoulderAngle < wrongCountStates2[wrongStateIndex][3])
            || (rightShoulderAngle > wrongCountStates2[wrongStateIndex][0]
                    && rightShoulderAngle < wrongCountStates2[wrongStateIndex][1]
                    && leftShoulderAngle > wrongCountStates2[wrongStateIndex][2]
                    && leftShoulderAngle < wrongCountStates2[wrongStateIndex][3])
            && insideBox
        ) {
            wrongStateIndex += 1
            if (wrongStateIndex == wrongCountStates1.size - 1) {
                rightStateIndex = 0
            }
            if (wrongStateIndex == wrongCountStates1.size) {
                wrongStateIndex = 0
                wrongCount()
            }
        }
    }
}