package org.mmh.virtual_assistant


import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import org.mmh.virtual_assistant.core.ExerciseInfoAdapter
import org.mmh.virtual_assistant.core.ImageSliderAdapter
import org.mmh.virtual_assistant.exercise.home.HomeExercise


class ExerciseGuidelineFragment(
    private val testId: String,
    private val testDate: String,
    private val position: Int,
    private val exerciseList: List<HomeExercise>,
    private val patientId: String,
    private val tenant: String,
    private val width: Int = 0
) : Fragment() {
    private lateinit var mediaSource: MediaSource
    private lateinit var exoplayer: ExoPlayer

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_exercise_guideline, container, false)
        val exerciseNameView: TextView = view.findViewById(R.id.exercise_name_guideline)
        val backButton: ImageButton = view.findViewById(R.id.back_button)
        val playVideo: PlayerView = view.findViewById(R.id.video_view)
        val startWorkoutButton: Button = view.findViewById(R.id.btn_start_workout_guideline)

        val exercise = exerciseList[position]
        var instruction = exercise.instruction
        val imageUrls = exercise.imageUrls
        val videoUrls = exercise.videoUrls


        exoplayer = ExoPlayer.Builder(requireContext()).build()
        playVideo.player = exoplayer
        exoplayer.setMediaSource(buildMediaSource(videoUrls))
        exoplayer.prepare()
        exoplayer.volume = 0f
        exoplayer.playWhenReady = false
        exoplayer.play()

        var gifUrl = ""
        if (imageUrls.isNotEmpty()) {
            imageUrls.forEach { url ->
                gifUrl = if (url.endsWith(".gif")) {
                    url
                } else {
                    ""
                }
            }
        }

        backButton.setOnClickListener {
            parentFragmentManager.beginTransaction().apply {
                replace(
                    R.id.fragment_container,
                    ExerciseListFragment(testId, testDate, patientId, tenant, exerciseList)
                )
                commit()
            }
            exoplayer.pause()
        }
        startWorkoutButton.setOnClickListener {
            val dialogView = LayoutInflater
                .from(context)
                .inflate(R.layout.exercise_info_modal, container, false)
            val alertDialog = AlertDialog.Builder(view.context).setView(dialogView)
            val imageSlider: ViewPager2 = dialogView.findViewById(R.id.exercise_image_slide)
            imageSlider.adapter = ExerciseInfoAdapter(exercise.imageUrls)
            alertDialog.setPositiveButton("Let's Start") { _, _ ->
                val intent = Intent(context, ExerciseActivity::class.java).apply {
                    putExtra(ExerciseActivity.ExerciseId, exercise.id)
                    putExtra(ExerciseActivity.TestId, testId)
                    putExtra(ExerciseActivity.Name, exercise.name)
                    putExtra(ExerciseActivity.RepetitionLimit, exercise.maxRepCount)
                    putExtra(ExerciseActivity.SetLimit, exercise.maxSetCount)
                    putExtra(ExerciseActivity.ImageUrl, gifUrl)
                    putExtra(ExerciseActivity.ProtocolId, exercise.protocolId)
                }
                view.context.startActivity(intent)
            }
            alertDialog.setNegativeButton("Cancel") { _, _ -> }
            alertDialog.show()
        }

        exerciseNameView.text = exercise.name
        val exerciseInstructionView: TextView =
            view.findViewById(R.id.exercise_instruction_guideline)
        val htmlTagRegex = Regex("<[^>]*>|&nbsp|;")
        instruction = instruction ?: ""
        instruction = instruction.let { htmlTagRegex.replace(it, "").replace("\n", " ") }
        exerciseInstructionView.text = instruction


        if (imageUrls.isEmpty()) {
            Toast.makeText(context, "No image is available now!", Toast.LENGTH_SHORT).show()
        }

        val imageAdapter = view.findViewById<ViewPager2>(R.id.image_slide_guideline)
//        val indicator = view.findViewById<CircleIndicator3>(R.id.slide_indicator)

        imageAdapter.adapter = ImageSliderAdapter(
            view.context,
//            listOf("Phase 1", "Phase 2", "Phase 3"),
//            listOf("phase 1 Description", "phase 2 Description", "phase 3 Description"),
            imageUrls
        )

//        indicator.setViewPager(imageAdapter)

        return view
    }

    override fun onDestroy() {
        super.onDestroy()
        exoplayer.pause()
    }

    override fun onPause() {
        super.onPause()
        exoplayer.pause()
    }

    private fun buildMediaSource(videoURL: String): MediaSource {
        val dataSourceFactory: DataSource.Factory = DefaultHttpDataSource.Factory()
        mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(videoURL))

        return mediaSource
    }

    private fun addToList(number: String, description: String, image: Int) {
    }

//    private fun postToList() {
//        for (i in 0..2) {
//            addToList("1", "Hello", R.id.action_image)
//        }
//    }
}