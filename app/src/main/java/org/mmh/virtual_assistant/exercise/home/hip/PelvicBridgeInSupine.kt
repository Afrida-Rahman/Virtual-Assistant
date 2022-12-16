package org.mmh.virtual_assistant.exercise.home.hip

import android.content.Context
import org.mmh.virtual_assistant.R
import org.mmh.virtual_assistant.core.Utilities
import org.mmh.virtual_assistant.domain.model.BodyPart
import org.mmh.virtual_assistant.domain.model.Person
import org.mmh.virtual_assistant.exercise.home.HomeExercise

class PelvicBridgeInSupine(
    context: Context
) : HomeExercise(
    context = context, id = 122
) {
    override fun instruction(person: Person) {
        this.getPhase()?.let {
            if (it.phaseNumber == 2) {
                it.constraints.forEach { constraint ->
                    if (constraint.startPointIndex == BodyPart.LEFT_SHOULDER.position && constraint.middlePointIndex == BodyPart.LEFT_HIP.position && constraint.endPointIndex == BodyPart.LEFT_KNEE.position) {
                        val angle = Utilities.angle(
                            startPoint = person.keyPoints[constraint.startPointIndex].toRealPoint(),
                            middlePoint = person.keyPoints[constraint.middlePointIndex].toRealPoint(),
                            endPoint = person.keyPoints[constraint.endPointIndex].toRealPoint(),
                            clockWise = constraint.clockWise
                        )
                        if (angle < constraint.minValue) {
                            this.playAudio(R.raw.raise_your_hip)
                        }
                    }
                }
            }
        }
    }
}