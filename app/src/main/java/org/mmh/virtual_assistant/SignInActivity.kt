package org.mmh.virtual_assistant

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.mmh.virtual_assistant.api.ILogInService
import org.mmh.virtual_assistant.api.request.LogInRequest
import org.mmh.virtual_assistant.api.response.LogInResponse
import org.mmh.virtual_assistant.core.Utilities
import org.mmh.virtual_assistant.databinding.ActivitySignInBinding
import org.mmh.virtual_assistant.domain.model.LogInData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SignInActivity : AppCompatActivity() {

    companion object {
        const val LOGIN_PREFERENCE = "LogInPreference"
        const val PREFERENCE_MODE = Context.MODE_PRIVATE
        const val FIRST_NAME = "firstName"
        const val LAST_NAME = "lastName"
        const val PATIENT_ID = "patientId"
        const val TENANT = "tenant"
    }

    private lateinit var binding: ActivitySignInBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val logInData = loadLogInData()

        if (logInData.patientId.isNotEmpty()) gotoMainActivity()

        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.tenant_name, android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinner.adapter = adapter

        binding.signInButton.setOnClickListener {
            binding.progressBar.visibility = View.VISIBLE
            it.isClickable = false
            val email = binding.emailAddressField.text.toString()
            val password = binding.passwordField.text.toString()
            val tenant = binding.spinner.selectedItem.toString().lowercase()

            if (email.isNotEmpty() && password.isNotEmpty() && tenant.isNotEmpty()) userLogin(
                email,
                password,
                tenant
            )
            else {
                Toast.makeText(this, "Tenant, email or password cannot be empty", Toast.LENGTH_LONG)
                    .show()
                binding.progressBar.visibility = View.GONE
                it.isClickable = true
            }
        }


    }

    private fun userLogin(email: String, password: String, tenant: String) {
        val url = Utilities.getUrl(tenant)
        val service = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(url.getAssessmentUrl)
            .build()
            .create(ILogInService::class.java)
        val requestPayload = LogInRequest(
            Email = email,
            Password = password,
            Tenant = tenant
        )
        val response = service.logIn(requestPayload)
        response.enqueue(object : Callback<LogInResponse> {
            override fun onResponse(call: Call<LogInResponse>, response: Response<LogInResponse>) {
                response.body()?.let {
                    if (it.Success) {
                        saveLogInData(
                            LogInData(
                                firstName = it.ContactData.FirstName,
                                lastName = it.ContactData.LastName,
                                patientId = it.ContactData.PatientId,
                                tenant = requestPayload.Tenant
                            )
                        )
                        Toast.makeText(
                            this@SignInActivity,
                            "Successfully logged in",
                            Toast.LENGTH_SHORT
                        ).show()
                        gotoMainActivity()
                    } else {
                        Toast.makeText(
                            this@SignInActivity,
                            "Invalid email address or password",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                binding.progressBar.visibility = View.GONE
                binding.signInButton.isClickable = true
            }

            override fun onFailure(call: Call<LogInResponse>, t: Throwable) {
                println(t.message)
                binding.progressBar.visibility = View.GONE
                binding.signInButton.isClickable = true
                Toast.makeText(
                    this@SignInActivity,
                    "Failed to get login data from API !!!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun gotoMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun saveLogInData(logInData: LogInData) {
        val preferences = getSharedPreferences(LOGIN_PREFERENCE, PREFERENCE_MODE)
        preferences.edit().apply {
            putString(FIRST_NAME, logInData.firstName)
            putString(LAST_NAME, logInData.lastName)
            putString(PATIENT_ID, logInData.patientId)
            putString(TENANT, logInData.tenant)
            apply()
        }
    }

    private fun loadLogInData(): LogInData {
        val preferences = getSharedPreferences(
            LOGIN_PREFERENCE,
            PREFERENCE_MODE
        )
        return LogInData(
            firstName = preferences.getString(FIRST_NAME, "") ?: "",
            lastName = preferences.getString(LAST_NAME, "") ?: "",
            patientId = preferences.getString(PATIENT_ID, "") ?: "",
            tenant = preferences.getString(TENANT, "") ?: ""
        )
    }
}
