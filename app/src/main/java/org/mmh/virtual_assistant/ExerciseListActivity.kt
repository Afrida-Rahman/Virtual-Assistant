package org.mmh.virtual_assistant

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.mmh.virtual_assistant.api.IExerciseService
import org.mmh.virtual_assistant.api.request.ExerciseListRequestPayload
import org.mmh.virtual_assistant.api.response.ExerciseListResponse
import org.mmh.virtual_assistant.core.Exercises
import org.mmh.virtual_assistant.core.Utilities
import org.mmh.virtual_assistant.databinding.ActivityExerciseListBinding
import org.mmh.virtual_assistant.domain.model.LogInData
import org.mmh.virtual_assistant.exercise.home.GeneralExercise
import org.mmh.virtual_assistant.exercise.home.HomeExercise
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ExerciseListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityExerciseListBinding
    private var assessmentDate: String? = null
    private lateinit var logInData: LogInData
    private lateinit var patientNameDisplay: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tryAgainButton: Button
    private lateinit var fragmentContainer: FrameLayout

    companion object {
        const val TEST_ID = "testId"
        const val TEST_DATE = "testDate"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExerciseListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val testId = intent.getStringExtra(TEST_ID)
        assessmentDate = intent.getStringExtra(TEST_DATE)

        patientNameDisplay = findViewById(R.id.patient_name)
        progressBar = findViewById(R.id.progress_indicator)
        tryAgainButton = findViewById(R.id.btn_try_again)
        fragmentContainer = findViewById(R.id.fragment_container)

        logInData = Utilities.loadLogInData(this)

        patientNameDisplay.text = getString(
            R.string.hello_patient_name_i_m_emma,
            "${logInData.firstName} ${logInData.lastName}"
        )

        if (testId == null) {
            Toast.makeText(this, "You have to pass a test ID", Toast.LENGTH_LONG).show()
            finish()
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                getExerciseList(tenant = logInData.tenant, testId = testId)
            }
        }

        tryAgainButton.setOnClickListener {
            it.visibility = View.GONE
            testId?.let { id ->
                CoroutineScope(Dispatchers.IO).launch {
                    getExerciseList(tenant = logInData.tenant, testId = id)
                }
            }
        }
    }

    private fun changeScreen(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            disallowAddToBackStack()
            replace(R.id.fragment_container, fragment)
            commit()
        }
    }

    private fun getExerciseList(tenant: String, testId: String) {
        val client = OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.MINUTES)
            .readTimeout(4, TimeUnit.MINUTES)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
        val service = Retrofit.Builder()
            .baseUrl(Utilities.getUrl(tenant).getExerciseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(IExerciseService::class.java)
        val requestPayload = ExerciseListRequestPayload(
            Tenant = tenant,
            TestId = testId
        )
        val response = service.getExerciseList(requestPayload)
        response.enqueue(object : Callback<ExerciseListResponse> {
            override fun onResponse(
                call: Call<ExerciseListResponse>,
                response: Response<ExerciseListResponse>
            ) {
                progressBar.visibility = View.GONE
                val responseBody = response.body()
                if (responseBody != null) {
                    if (responseBody.Exercises.isNotEmpty()) {
                        val implementedExerciseList = Exercises.get(this@ExerciseListActivity)
                        var parsedExercises = mutableListOf<HomeExercise>()
                        responseBody.Exercises.forEach { exercise ->
                            val implementedExercise =
                                implementedExerciseList.find { it.id == exercise.ExerciseId }

                            if (implementedExercise != null) {
                                implementedExercise.setExercise(
                                    exerciseName = exercise.ExerciseName,
                                    exerciseInstruction = exercise.Instructions,
                                    exerciseImageUrls = exercise.ImageURLs,
                                    exerciseVideoUrls = exercise.ExerciseMedia,
                                    repetitionLimit = exercise.RepetitionInCount,
                                    setLimit = exercise.SetInCount,
                                    protoId = exercise.ProtocolId
                                )
                                parsedExercises.add(implementedExercise)
                            } else {
                                val notImplementedExercise = GeneralExercise(
                                    context = this@ExerciseListActivity,
                                    exerciseId = exercise.ExerciseId,
                                    active = false
                                )
                                notImplementedExercise.setExercise(
                                    exerciseName = exercise.ExerciseName,
                                    exerciseInstruction = exercise.Instructions,
                                    exerciseImageUrls = exercise.ImageURLs,
                                    exerciseVideoUrls = exercise.ExerciseMedia,
                                    repetitionLimit = exercise.RepetitionInCount,
                                    setLimit = exercise.SetInCount,
                                    protoId = exercise.ProtocolId
                                )
                                parsedExercises.add(notImplementedExercise)
                            }
                        }
                        parsedExercises =
                            parsedExercises.sortedBy { it.active }.reversed().toMutableList()
                        changeScreen(
                            ExerciseListFragment(
                                assessmentId = testId,
                                assessmentDate = assessmentDate ?: "",
                                patientId = logInData.patientId,
                                tenant = tenant,
                                exerciseList = parsedExercises
                            )
                        )
                    } else {
                        tryAgainButton.visibility = View.VISIBLE
                        Toast.makeText(
                            this@ExerciseListActivity,
                            "No exercise is assigned!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    tryAgainButton.visibility = View.VISIBLE
                    Toast.makeText(
                        this@ExerciseListActivity,
                        "Got empty response",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(call: Call<ExerciseListResponse>, t: Throwable) {
                progressBar.visibility = View.GONE
                tryAgainButton.visibility = View.VISIBLE
                Toast.makeText(
                    this@ExerciseListActivity,
                    t.message
                        ?: "Unknown error",// "Failed to get exercise list from API. Try again later.",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }
}