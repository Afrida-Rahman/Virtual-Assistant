package org.mmh.virtual_assistant.exercise.home.hip

import android.content.Context
import org.mmh.virtual_assistant.exercise.home.HomeExercise

class BodyWeightSquats(
    context: Context
) : HomeExercise(
    context = context,
    id = 458
)
//) {
//    private var wrongUpHipAngleMin = 145f
//    private var wrongUpHipAngleMax = 160f
//
//    private var wrongDownHipAngleMin = 110f
//    private var wrongDownHipAngleMax = 130f
//
//    override var wrongStateIndex = 0
//
//    override fun wrongExerciseCount(person: Person, canvasHeight: Int, canvasWidth: Int) {
//        val shoulderPoint = Point(
//            person.keyPoints[5].coordinate.x,
//            -person.keyPoints[5].coordinate.y
//        )
//        val hipPoint = Point(
//            person.keyPoints[11].coordinate.x,
//            -person.keyPoints[11].coordinate.y
//        )
//        val kneePoint = Point(
//            person.keyPoints[13].coordinate.x,
//            -person.keyPoints[13].coordinate.y
//        )
//        val anklePoint = Point(
//            person.keyPoints[15].coordinate.x,
//            -person.keyPoints[15].coordinate.y
//        )
//
//        val wrongCountStates: Array<FloatArray> = arrayOf(
//            floatArrayOf(
//                wrongUpHipAngleMin,
//                wrongUpHipAngleMax,
//            ),
//            floatArrayOf(
//                wrongDownHipAngleMin,
//                wrongDownHipAngleMax,
//            ),
//            floatArrayOf(
//                wrongUpHipAngleMin,
//                wrongUpHipAngleMax,
//            )
//        )
//
//        val insideBox = VisualizationUtils.isInsideBox(person, canvasHeight, canvasWidth)
//        val hipAngle = Utilities.angle(shoulderPoint, hipPoint, kneePoint, true)
//
//
//        if (hipAngle > wrongCountStates[wrongStateIndex][0] && hipAngle < wrongCountStates[wrongStateIndex][1] &&
//            insideBox
//        ) {
//            if (insideBox) {
//                wrongStateIndex += 1
//                if (wrongStateIndex == wrongCountStates.size) {
//                    wrongStateIndex = 0
//                    wrongCount()
//                    phaseIndex = 0
//                    phaseEntered = false
//                }
//            }
//        }
//    }
//}
