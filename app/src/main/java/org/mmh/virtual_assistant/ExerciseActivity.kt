package org.mmh.virtual_assistant

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class ExerciseActivity : AppCompatActivity() {
    companion object {
        const val ExerciseId = "ExerciseId"
        const val TestId = "TestId"
        const val Name = "Name"
        const val ProtocolId = "ProtocolId"
        const val RepetitionLimit = "RepetitionLimit"
        const val SetLimit = "SetLimit"
        const val ImageUrl = "ImageUrl"
        const val TAG = "ExerciseActivityTag"
        private const val PREVIEW_WIDTH = 640
        private const val PREVIEW_HEIGHT = 480
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise)
    }
}