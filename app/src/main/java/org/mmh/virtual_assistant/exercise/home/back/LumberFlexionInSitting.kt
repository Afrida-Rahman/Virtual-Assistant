package org.mmh.virtual_assistant.exercise.home.back

import android.content.Context
import org.mmh.virtual_assistant.core.Point
import org.mmh.virtual_assistant.core.Utilities
import org.mmh.virtual_assistant.core.VisualizationUtils
import org.mmh.virtual_assistant.domain.model.Person
import org.mmh.virtual_assistant.exercise.home.HomeExercise

class LumberFlexionInSitting(
    context: Context
) : HomeExercise(
    context = context,
    id = 341
) {
    private var sittingHipAngleMin = 200f
    private var sittingHipAngleMax = 250f
    private var sittingShoulderAngleMin = 120f
    private var sittingShoulderAngleMax = 190f

    private var downHipAngleMin = 260f
    private var downHipAngleMax = 320f
    private var downShoulderAngleMin = 80f
    private var downShoulderAngleMax = 140f

    private var wrongUpHipAngleMin = 160f
    private var wrongUpHipAngleMax = 190f
    private var wrongUpKneeAngleMin = 160f
    private var wrongUpKneeAngleMax = 190f

    private var wrongDownHipAngleMin = 100f
    private var wrongDownHipAngleMax = 160f
    private var wrongDownKneeAngleMin = 100f
    private var wrongDownKneeAngleMax = 160f

    override var wrongStateIndex = 0

    override fun wrongExerciseCount(person: Person, canvasHeight: Int, canvasWidth: Int) {
        val leftElbowPoint = Point(
            person.keyPoints[7].coordinate.x,
            -person.keyPoints[7].coordinate.y
        )
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

        wrongUpHipAngleMin = sittingHipAngleMin
        wrongUpHipAngleMax = sittingHipAngleMax
        wrongUpKneeAngleMin = sittingShoulderAngleMin
        wrongUpKneeAngleMax = sittingShoulderAngleMax
        wrongDownHipAngleMin = downHipAngleMin + 40
        wrongDownHipAngleMax = downHipAngleMax + 70
        wrongDownKneeAngleMin = downShoulderAngleMin + 40
        wrongDownKneeAngleMax = downShoulderAngleMax + 70

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
        val hipAngle = Utilities.angle(shoulderPoint, hipPoint, kneePoint, false)
        val shoulderAngle = Utilities.angle(leftElbowPoint, shoulderPoint, hipPoint)

        if (hipAngle > wrongCountStates[wrongStateIndex][0] && hipAngle < wrongCountStates[wrongStateIndex][1] &&
            shoulderAngle > wrongCountStates[wrongStateIndex][2] && shoulderAngle < wrongCountStates[wrongStateIndex][3] &&
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
