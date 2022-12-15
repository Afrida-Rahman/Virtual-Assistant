package org.mmh.virtual_assistant.exercise.home.back

import android.content.Context
import org.mmh.virtual_assistant.domain.model.Person
import org.mmh.virtual_assistant.exercise.home.HomeExercise

class PlankOnKneesInProne(
    context: Context
) : HomeExercise(
    context = context,
    id = 194
) {
    override fun wrongExerciseCount(person: Person, canvasHeight: Int, canvasWidth: Int) {

    }
}