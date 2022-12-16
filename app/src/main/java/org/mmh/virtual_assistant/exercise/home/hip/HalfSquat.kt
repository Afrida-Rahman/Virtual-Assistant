package org.mmh.virtual_assistant.exercise.home.hip

import android.content.Context
import org.mmh.virtual_assistant.core.Point
import org.mmh.virtual_assistant.core.Utilities
import org.mmh.virtual_assistant.core.VisualizationUtils
import org.mmh.virtual_assistant.domain.model.Person
import org.mmh.virtual_assistant.exercise.home.HomeExercise

class HalfSquat(
    context: Context
) : HomeExercise(
    context = context,
    id = 495
) {
    private var upHipAngleMin = 160f
    private var upHipAngleMax = 190f
    private var upKneeAngleMin = 160f
    private var upKneeAngleMax = 190f

    private var downHipAngleMin = 80f
    private var downHipAngleMax = 120f
    private var downKneeAngleMin = 80f
    private var downKneeAngleMax = 120f

    private var wrongUpHipAngleMin = 160f
    private var wrongUpHipAngleMax = 190f
    private var wrongUpKneeAngleMin = 160f
    private var wrongUpKneeAngleMax = 190f

    private var wrongDownHipAngleMin = 120f
    private var wrongDownHipAngleMax = 160f
    private var wrongDownKneeAngleMin = 120f
    private var wrongDownKneeAngleMax = 160f

    private val totalStates = 3
    private var rightStateIndex = 0

    override var wrongStateIndex = 0
    private var wrongFrameCount = 0
    private val maxWrongCountFrame = 3

    fun rightExercise(
        person: Person,
        canvasHeight: Int,
        canvasWidth: Int
    ) {
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
        val leftAnklePoint = Point(
            person.keyPoints[15].coordinate.x,
            -person.keyPoints[15].coordinate.y
        )
        if (rightCountPhases.size >= 2) {
            upHipAngleMin = rightCountPhases[0].constraints[0].minValue.toFloat()
            upHipAngleMax = rightCountPhases[0].constraints[0].maxValue.toFloat()
            downHipAngleMin = rightCountPhases[1].constraints[0].minValue.toFloat()
            downHipAngleMax = rightCountPhases[1].constraints[0].maxValue.toFloat()

            upKneeAngleMin = rightCountPhases[0].constraints[0].minValue.toFloat()
            upKneeAngleMax = rightCountPhases[0].constraints[0].maxValue.toFloat()
            downKneeAngleMin = rightCountPhases[1].constraints[0].minValue.toFloat()
            downKneeAngleMax = rightCountPhases[1].constraints[0].maxValue.toFloat()
        }

        val insideBox = VisualizationUtils.isInsideBox(person, canvasHeight, canvasWidth)
        val hipAngle = Utilities.angle(leftShoulderPoint, leftHipPoint, leftKneePoint, true)
        val kneeAngle = Utilities.angle(leftHipPoint, leftKneePoint, leftAnklePoint)

        val rightCountStates: Array<FloatArray> = arrayOf(
            floatArrayOf(
                upHipAngleMin,
                upHipAngleMax,
                upKneeAngleMin,
                upKneeAngleMax
            ),
            floatArrayOf(
                downHipAngleMin,
                downHipAngleMax,
                downKneeAngleMin,
                downKneeAngleMax
            ),
            floatArrayOf(
                upHipAngleMin,
                upHipAngleMax,
                upKneeAngleMin,
                upKneeAngleMax
            )
        )
        if (hipAngle > rightCountStates[rightStateIndex][0] && hipAngle < rightCountStates[rightStateIndex][1]
            && kneeAngle > rightCountStates[rightStateIndex][2] && kneeAngle < rightCountStates[rightStateIndex][3]
            && insideBox
        ) {
            rightStateIndex += 1
            if (rightStateIndex == rightCountStates.size - 1) {
                wrongStateIndex = 0
            }
            if (rightStateIndex == totalStates) {
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

    fun wrongExercise(person: Person, canvasHeight: Int, canvasWidth: Int) {
        val shoulderPoint = Point(
            person.keyPoints[5].coordinate.x,
            -person.keyPoints[5].coordinate.y
        )
        val hipPoint = Point(
            person.keyPoints[11].coordinate.x,
            -person.keyPoints[11].coordinate.y
        )
        val kneePoint = Point(
            person.keyPoints[13].coordinate.x,
            -person.keyPoints[13].coordinate.y
        )
        val anklePoint = Point(
            person.keyPoints[15].coordinate.x,
            -person.keyPoints[15].coordinate.y
        )

        wrongUpHipAngleMin = upHipAngleMin
        wrongUpHipAngleMax = upHipAngleMax
        wrongUpKneeAngleMin = upKneeAngleMin
        wrongUpKneeAngleMax = upKneeAngleMax
        wrongDownHipAngleMin = downHipAngleMin + 40
        wrongDownHipAngleMax = downHipAngleMax + 40
        wrongDownKneeAngleMin = downKneeAngleMin + 40
        wrongDownKneeAngleMax = downKneeAngleMax + 40

        val wrongCountStates: Array<FloatArray> = arrayOf(
            floatArrayOf(
                wrongUpHipAngleMin,
                wrongUpHipAngleMax,
                wrongUpKneeAngleMin,
                wrongUpKneeAngleMax
            ),
            floatArrayOf(
                wrongDownHipAngleMin,
                wrongDownHipAngleMax,
                wrongDownKneeAngleMin,
                wrongDownKneeAngleMax
            ),
            floatArrayOf(
                wrongUpHipAngleMin,
                wrongUpHipAngleMax,
                wrongUpKneeAngleMin,
                wrongUpKneeAngleMax
            )
        )

        val insideBox = VisualizationUtils.isInsideBox(person, canvasHeight, canvasWidth)
        val hipAngle = Utilities.angle(shoulderPoint, hipPoint, kneePoint, true)
        val kneeAngle = Utilities.angle(hipPoint, kneePoint, anklePoint)


        if (hipAngle > wrongCountStates[wrongStateIndex][0] && hipAngle < wrongCountStates[wrongStateIndex][1] &&
            kneeAngle > wrongCountStates[wrongStateIndex][2] && kneeAngle < wrongCountStates[wrongStateIndex][3] &&
            insideBox
        ) {
            if (insideBox) {
                wrongStateIndex += 1
                if (wrongStateIndex == wrongCountStates.size) {
                    wrongStateIndex = 0
                    wrongCount()
                }
            }
        }
    }
}
