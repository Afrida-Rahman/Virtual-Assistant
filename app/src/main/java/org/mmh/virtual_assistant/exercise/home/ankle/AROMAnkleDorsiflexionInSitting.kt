package org.mmh.virtual_assistant.exercise.home.ankle

import android.content.Context
import org.mmh.virtual_assistant.domain.model.Person
import org.mmh.virtual_assistant.exercise.home.HomeExercise

class AROMAnkleDorsiflexionInSitting(
    context: Context
) : HomeExercise(context = context, id = 50) {
    override fun wrongExerciseCount(person: Person, canvasHeight: Int, canvasWidth: Int) {

    }
}