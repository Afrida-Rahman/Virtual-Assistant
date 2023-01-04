package org.mmh.virtual_assistant.exercise.home

import android.content.Context

class GeneralExercise(
    context: Context,
    exerciseId: Int,
    active: Boolean = false
) : HomeExercise(
    context = context,
    id = exerciseId,
    active = active
)