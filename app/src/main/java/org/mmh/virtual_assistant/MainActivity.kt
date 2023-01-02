package org.mmh.virtual_assistant

import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.mmh.virtual_assistant.api.IExerciseService
import org.mmh.virtual_assistant.api.request.AssessmentListRequestPayload
import org.mmh.virtual_assistant.api.response.Assessment
import org.mmh.virtual_assistant.api.response.AssessmentListResponse
import org.mmh.virtual_assistant.core.Utilities
import org.mmh.virtual_assistant.databinding.ActivityMainBinding
import org.mmh.virtual_assistant.domain.model.LogInData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var menuToggle: ActionBarDrawerToggle
    private var assessmentListFragment: AssessmentListFragment? = null
    private var assignedAssessments: List<Assessment> = emptyList()
    private lateinit var logInData: LogInData
    private var width: Int = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        width = displayMetrics.widthPixels

        logInData = Utilities.loadLogInData(this)
        binding.patientName.text =
            getString(R.string.hello_patient_name_i_m_emma).format("${logInData.firstName} ${logInData.lastName}")

        CoroutineScope(IO).launch {
            getAssessmentDetails(logInData.patientId, logInData.tenant)
        }
        menuToggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            R.string.open,
            R.string.close
        )
        binding.drawerLayout.addDrawerListener(menuToggle)
        menuToggle.syncState()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.elevation = 0f

        binding.menuButton.setOnClickListener {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                binding.drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        binding.btnTryAgain.setOnClickListener {
            getAssessmentDetails(patientId = logInData.patientId, tenant = logInData.tenant)
            it.visibility = View.GONE
            binding.progressIndicator.visibility = View.VISIBLE
        }

        binding.navView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.log_out_button -> {
                    Utilities.saveLogInData(
                        this,
                        LogInData(
                            firstName = "",
                            lastName = "",
                            patientId = "",
                            tenant = ""
                        )
                    )
                    Toast.makeText(this, "Successfully logged out", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, SignInActivity::class.java))
                    finish()
                }
            }
            true
        }

        assessmentListFragment?.let {
            changeScreen(it)
        }
    }

    override fun onResume() {
        super.onResume()
        assessmentListFragment =
            AssessmentListFragment(
                assignedAssessments,
                width = width
            )
        assessmentListFragment?.let { changeScreen(it) }
    }

    override fun onBackPressed() {
        if (assessmentListFragment != null) {
            if (assessmentListFragment!!.isVisible) {
                super.onBackPressed()
                finish()
            } else {
                changeScreen(assessmentListFragment!!)
            }
        } else {
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (menuToggle.onOptionsItemSelected(item)) return true
        return super.onOptionsItemSelected(item)
    }

    private fun changeScreen(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            disallowAddToBackStack()
            replace(R.id.fragment_container, fragment)
            commit()
        }
    }

    private fun getAssessmentDetails(patientId: String, tenant: String) {
        val client = OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.MINUTES)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        val service = Retrofit.Builder()
            .baseUrl(Utilities.getUrl(Utilities.loadLogInData(this).tenant).getAssessmentUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(IExerciseService::class.java)
        val requestPayload = AssessmentListRequestPayload(
            PatientId = patientId,
            Tenant = tenant
        )
        val response = service.getAssessmentList(requestPayload)
        response.enqueue(object : Callback<AssessmentListResponse> {
            override fun onResponse(
                call: Call<AssessmentListResponse>,
                response: Response<AssessmentListResponse>
            ) {
                val responseBody = response.body()
                if (responseBody != null) {
                    if (responseBody.Assessments.isNotEmpty()) {
                        binding.progressIndicator.visibility = View.GONE
                        assignedAssessments = responseBody.Assessments
                        assessmentListFragment =
                            AssessmentListFragment(
                                assignedAssessments,
                                width = width
                            )
                        assessmentListFragment?.let { changeScreen(it) }
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "You have not performed any assessment yet. Please perform an assessment first!",
                            Toast.LENGTH_LONG
                        ).show()
                        binding.progressIndicator.visibility = View.GONE
                    }
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Failed to get assessment list from API and got empty response!",
                        Toast.LENGTH_LONG
                    ).show()
                    binding.progressIndicator.visibility = View.GONE
                    binding.btnTryAgain.visibility = View.VISIBLE
                }
            }

            override fun onFailure(call: Call<AssessmentListResponse>, t: Throwable) {
                Toast.makeText(
                    this@MainActivity,
                    "Failed to get assessment list from API.",
                    Toast.LENGTH_LONG
                ).show()
                binding.progressIndicator.visibility = View.GONE
                binding.btnTryAgain.visibility = View.VISIBLE
            }
        })
    }
}