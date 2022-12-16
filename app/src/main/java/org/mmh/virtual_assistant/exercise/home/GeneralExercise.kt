package org.mmh.virtual_assistant.exercise.home

import android.content.Context
import org.mmh.virtual_assistant.domain.model.Person

class GeneralExercise(
    context: Context,
    exerciseId: Int,
    active: Boolean = false
) : HomeExercise(
    context = context,
    id = exerciseId,
    active = active
) {
    override fun wrongExerciseCount(person: Person, canvasHeight: Int, canvasWidth: Int) {
    }
}