package org.mmh.virtual_assistant.core

import android.content.Context
import android.content.Intent
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import org.mmh.virtual_assistant.ExerciseActivity
import org.mmh.virtual_assistant.ExerciseGuidelineFragment
import org.mmh.virtual_assistant.R
import org.mmh.virtual_assistant.api.IExerciseService
import org.mmh.virtual_assistant.api.request.ExerciseTrackingPayload
import org.mmh.virtual_assistant.api.request.PhaseSummary
import org.mmh.virtual_assistant.api.request.QResponse
import org.mmh.virtual_assistant.api.response.ExerciseTrackingResponse
import org.mmh.virtual_assistant.exercise.home.HomeExercise
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ExerciseListAdapter(
    private val testId: String,
    private val testDate: String,
    private var exerciseList: List<HomeExercise>,
    private val manager: FragmentManager,
    private val patientId: String,
    private val tenant: String
) : RecyclerView.Adapter<ExerciseListAdapter.ExerciseItemViewHolder>() {

    private lateinit var viewGroup: ViewGroup

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseItemViewHolder {
        viewGroup = parent
        return ExerciseItemViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_exercise, parent, false)
        )
    }

    override fun getItemCount(): Int = exerciseList.size

    override fun onBindViewHolder(holder: ExerciseItemViewHolder, position: Int) {
        val exercise = exerciseList[position]
        var gifUrl: String? = null
        holder.apply {
            val context = this.exerciseImageView.context
            val imageUrl = if (exercise.imageUrls.isNotEmpty()) {
                gifUrl = exercise.imageUrls.find { it.endsWith(".gif") }
                gifUrl ?: exercise.imageUrls[0]
            } else {
                R.drawable.exercise.toString()
            }

            Glide.with(context)
                .load(imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                .thumbnail(Glide.with(context).load(R.drawable.gif_loading).centerCrop())
                .transition(DrawableTransitionOptions.withCrossFade(500))
                .into(this.exerciseImageView)

            exerciseNameView.text = exercise.name

            if (exercise.active) {
                exerciseStatus.setImageResource(R.drawable.ic_exercise_active)
                startExerciseButton.setOnClickListener {
                    showExerciseInformation(it.context, exercise, gifUrl)
                }
            } else {
                exerciseStatus.setImageResource(R.drawable.ic_exercise_inactive)
                startExerciseButton.setOnClickListener {
                    Toast.makeText(it.context, "Coming soon", Toast.LENGTH_LONG).show()
                }
            }

            manualTrackingButton.setOnClickListener {
                val alertDialog = AlertDialog.Builder(context)
                val layout = LinearLayout(context)
                layout.orientation = LinearLayout.VERTICAL

                alertDialog.setTitle("Manual Tracking")
                val setInput = EditText(context)
                setInput.setSingleLine()
                setInput.hint = "Enter Set Count"
                setInput.inputType = InputType.TYPE_CLASS_NUMBER
                layout.addView(setInput)

                val repInput = EditText(context)
                repInput.setSingleLine()
                repInput.hint = "Enter Repetition Count"
                repInput.inputType = InputType.TYPE_CLASS_NUMBER
                layout.addView(repInput)

                val wrongInput = EditText(context)
                wrongInput.setSingleLine()
                wrongInput.hint = "Enter Wrong Count"
                wrongInput.inputType = InputType.TYPE_CLASS_NUMBER
                layout.addView(wrongInput)

                layout.setPadding(100, 50, 100, 50)
                alertDialog.setView(layout)

                alertDialog.setPositiveButton("Submit") { _, _ ->
                    val setText = setInput.text.toString().toInt()
                    val repText = repInput.text.toString().toInt()
                    val wrongText = wrongInput.text.toString().toInt()

                    saveManualTrackingData(
                        Tenant = tenant,
                        PatientId = patientId,
                        TestId = testId,
                        ExerciseId = exercise.id,
                        ProtocolId = exercise.protocolId,
                        ExerciseDate = Utilities.currentDate(),
                        NoOfReps = setText,
                        NoOfSets = repText,
                        NoOfWrongCount = wrongText,
                        context = it.context,
                        AssignReps = repText,
                        AssignSets = setText,
                        TotalTime = 0,
                        Phases = exercise.getPhaseSummary(),
                        Responses = listOf()
                    )
                }
                alertDialog.setNegativeButton("Cancel") { alert, _ -> alert.cancel() }

                alertDialog.show()
            }

            guidelineButton.setOnClickListener {
                manager.beginTransaction().apply {
                    replace(
                        R.id.fragment_container,
                        ExerciseGuidelineFragment(
                            testId = testId,
                            testDate = testDate,
                            position = position,
                            exerciseList = exerciseList,
                            patientId = patientId,
                            tenant = tenant
                        )
                    )
                    commit()
                }
            }

            assignedSet.text =
                assignedSet.context.getString(R.string.assigned_set).format(exercise.maxSetCount)

            assignedRepetition.text =
                assignedRepetition.context.getString(R.string.assigned_repetition)
                    .format(exercise.maxRepCount)
        }
    }

    private fun saveManualTrackingData(
        Tenant: String,
        PatientId: String,
        TestId: String,
        ExerciseId: Int,
        ProtocolId: Int,
        ExerciseDate: String,
        AssignSets: Int,
        AssignReps: Int,
        NoOfReps: Int,
        NoOfSets: Int,
        NoOfWrongCount: Int = 0,
        TotalTime: Int = 0,
        Phases: List<PhaseSummary>,
        Responses: List<QResponse>,
        context: Context
    ) {
        val saveExerciseTrackingURL = Utilities.getUrl(tenant).saveExerciseTrackingURL
        val service = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(saveExerciseTrackingURL)
            .build()
            .create(IExerciseService::class.java)

        val requestPayload = ExerciseTrackingPayload(
            Tenant = Tenant,
            PatientId = PatientId,
            TestId = TestId,
            ExerciseId = ExerciseId,
            ProtocolId = ProtocolId,
            AssignSets = AssignSets,
            AssignReps = AssignReps,
            ExerciseDate = ExerciseDate,
            NoOfReps = NoOfReps,
            NoOfSets = NoOfSets,
            NoOfWrongCount = NoOfWrongCount,
            TotalTime = TotalTime,
            Phases = Phases,
            Responses = Responses
        )
        val response = service.saveExerciseData(requestPayload)
        response.enqueue(object : Callback<ExerciseTrackingResponse> {
            override fun onResponse(
                call: Call<ExerciseTrackingResponse>,
                response: Response<ExerciseTrackingResponse>
            ) {
                val responseBody = response.body()
                if (responseBody != null) {
                    if (responseBody.Successful) {
                        Toast.makeText(context, responseBody.Message, Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Could not save exercise data!", Toast.LENGTH_LONG)
                            .show()
                    }
                } else {
                    Toast.makeText(
                        context,
                        "Failed to save and got empty response! ($responseBody)",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(call: Call<ExerciseTrackingResponse>, t: Throwable) {
                Toast.makeText(
                    context,
                    "Failed to track data !!!",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun showExerciseInformation(context: Context, exercise: HomeExercise, gifUrl: String?) {
        val dialogView = LayoutInflater
            .from(context)
            .inflate(R.layout.exercise_info_modal, viewGroup, false)
        val alertDialog = AlertDialog.Builder(context).setView(dialogView)
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
            context.startActivity(intent)
        }
        alertDialog.setNegativeButton("Cancel") { _, _ -> }
        alertDialog.show()
    }

    class ExerciseItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val startExerciseButton: Button = view.findViewById(R.id.btn_start_exercise)
        val manualTrackingButton: Button = view.findViewById(R.id.btn_manual_tracking)
        val exerciseImageView: ImageView = view.findViewById(R.id.item_exercise_image)
        val exerciseNameView: TextView = view.findViewById(R.id.item_exercise_name)
        var exerciseStatus: ImageView = view.findViewById(R.id.exercise_status)
        val guidelineButton: ImageView = view.findViewById(R.id.btn_guideline)
        val assignedSet: TextView = view.findViewById(R.id.assigned_set)
        val assignedRepetition: TextView = view.findViewById(R.id.assigned_repetition)
    }
}