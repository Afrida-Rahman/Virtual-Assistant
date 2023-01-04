package org.mmh.virtual_assistant


import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import me.relex.circleindicator.CircleIndicator3
import org.mmh.virtual_assistant.core.ImageSliderAdapter
import org.mmh.virtual_assistant.exercise.home.HomeExercise


class ExerciseGuidelineFragment(
    private val testId: String,
    private val testDate: String,
    private val exercise: HomeExercise,
    private val exerciseList: List<HomeExercise>,
    private val patientId: String,
    private val tenant: String,
    private val active: Boolean = false
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
        val playVideo: StyledPlayerView = view.findViewById(R.id.video_view)
        val startWorkoutButton: Button = view.findViewById(R.id.btn_start_workout_guideline)
        val imageAdapter = view.findViewById<ViewPager2>(R.id.image_slide_guideline)
        val sliderIndicator = view.findViewById<CircleIndicator3>(R.id.slide_indicator)
        val exerciseInstructionView: TextView =
            view.findViewById(R.id.exercise_instruction_guideline)

        //Log.d("SliderIndicatorIssue", "Indicator: $sliderIndicator")

        val htmlTagRegex = Regex("<[^>]*>|&nbsp|;")
        var instruction = exercise.instruction
        val imageUrls = exercise.imageUrls
        val videoUrls = exercise.videoUrls
        val gifUrl = imageUrls.find { it.endsWith(".gif") }

        exoplayer = ExoPlayer.Builder(requireContext()).build()
        playVideo.player = exoplayer
        exoplayer.setMediaSource(buildMediaSource(videoUrls))
        exoplayer.prepare()
        exoplayer.volume = 0f
        exoplayer.playWhenReady = false
        exoplayer.play()

        exerciseNameView.text = exercise.name
        instruction = instruction ?: ""
        instruction = instruction.let { htmlTagRegex.replace(it, "").replace("\n", " ") }
        exerciseInstructionView.text = instruction

        if (imageUrls.isEmpty()) {
            Toast.makeText(context, "No image is available now!", Toast.LENGTH_SHORT).show()
        } else {
            imageAdapter.adapter = ImageSliderAdapter(
                view.context,
                exercise.phaseList.sortedBy { it.PhaseNumber }
            )
            sliderIndicator.setViewPager(imageAdapter)
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
        if (active) {
            startWorkoutButton.visibility = View.VISIBLE
            startWorkoutButton.setOnClickListener {
                val intent = Intent(context, ExerciseActivity::class.java).apply {
                    putExtra(ExerciseActivity.ExerciseId, exercise.id)
                    putExtra(ExerciseActivity.TestId, testId)
                    putExtra(ExerciseActivity.Name, exercise.name)
                    putExtra(ExerciseActivity.RepetitionLimit, exercise.maxRepCount)
                    putExtra(ExerciseActivity.SetLimit, exercise.maxSetCount)
                    putExtra(ExerciseActivity.ImageUrl, gifUrl)
                    putExtra(ExerciseActivity.ProtocolId, exercise.protocolId)
                }
                if (exercise.maxSetCount == 0) {
                    Toast.makeText(
                        context,
                        "Assigned set is zero. Please reset it from VA portal",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    view.context.startActivity(intent)
                }
            }
        } else {
            startWorkoutButton.visibility = View.GONE
            startWorkoutButton.setOnClickListener {
                Toast.makeText(context, "Coming soon!", Toast.LENGTH_SHORT).show()
            }
        }
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
}