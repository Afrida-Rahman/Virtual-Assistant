package org.mmh.virtual_assistant.core

enum class VisualQues(
    val quesId: Int,
    val questionMessage: String,
    val posAnsId: Long,
    val negAnsId: Long,
    val posAnsText: String = "Yes",
    val negAnsText: String = "No"
) {
    DO_YOU_FIND_THIS_EXERCISE_TO_BE_TOO_PAINFUL(
        10001002,
        "Do you find this exercise to be too painful?",
        61894,
        61893
    ),
    WAS_THIS_EXERCISE_TOO_EASY(
        10001001,
        "Was this exercise too easy?",
        61892,
        61891
    ),
    DID_YOU_FIND_THIS_EXERCISE_TO_BE_TOO_DIFFICULT(
        10001000,
        "Did you find this exercise to be too difficult?",
        61890,
        61889
    );

    companion object {
        private val map = values().associateBy(VisualQues::quesId)
        fun fromQuesId(quesId: Int): VisualQues = map.getValue(quesId)
    }
}