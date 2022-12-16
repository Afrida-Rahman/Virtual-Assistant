package org.mmh.virtual_assistant.exercise.home.neck

import android.content.Context
import org.mmh.virtual_assistant.domain.model.Person
import org.mmh.virtual_assistant.exercise.home.HomeExercise

class ProneOnElbows(
    context: Context
) : HomeExercise(
    context = context,
    id = 167
) {
    override var wrongStateIndex = 0
    override fun wrongExerciseCount(person: Person, canvasHeight: Int, canvasWidth: Int) {}
}