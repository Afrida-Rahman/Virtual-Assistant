package org.mmh.virtual_assistant.core

import android.content.Context
import org.mmh.virtual_assistant.SignInActivity
import org.mmh.virtual_assistant.domain.model.APiUrl
import org.mmh.virtual_assistant.domain.model.LogInData

object Utilities {
    fun getUrl(tenant: String): APiUrl {
        return when (tenant.lowercase()) {
            "dev" -> {
                APiUrl(
                    getAssessmentUrl = "https://devvaapi.injurycloud.com",
                    getExerciseUrl = "https://devvaapi.injurycloud.com",
                    getExerciseConstraintsURL = "https://devvaapi.injurycloud.com",
                    saveExerciseTrackingURL = "https://devapi.injurycloud.com"
                )
            }
            "stg" -> {
                APiUrl(
                    getAssessmentUrl = "https://stgvaapi.injurycloud.com",
                    getExerciseUrl = "https://stgvaapi.injurycloud.com",
                    getExerciseConstraintsURL = "https://stgvaapi.injurycloud.com",
                    saveExerciseTrackingURL = "https://stgapi.injurycloud.com"
                )
            }
            else -> {
                APiUrl(
                    getAssessmentUrl = "https://vaapi.injurycloud.com",
                    getExerciseUrl = "https://vaapi.injurycloud.com",
                    getExerciseConstraintsURL = "https://vaapi.injurycloud.com",
                    saveExerciseTrackingURL = "https://api.injurycloud.com"
                )
            }

        }
    }

    fun saveLogInData(context: Context, logInData: LogInData) {
        val preferences = context.getSharedPreferences(
            SignInActivity.LOGIN_PREFERENCE,
            SignInActivity.PREFERENCE_MODE
        )
        preferences.edit().apply {
            putString(SignInActivity.FIRST_NAME, logInData.firstName)
            putString(SignInActivity.LAST_NAME, logInData.lastName)
            putString(SignInActivity.PATIENT_ID, logInData.patientId)
            putString(SignInActivity.TENANT, logInData.tenant)
            apply()
        }
    }

    fun loadLogInData(context: Context): LogInData {
        val preferences = context.getSharedPreferences(
            SignInActivity.LOGIN_PREFERENCE,
            SignInActivity.PREFERENCE_MODE
        )
        return LogInData(
            firstName = preferences.getString(SignInActivity.FIRST_NAME, "") ?: "",
            lastName = preferences.getString(SignInActivity.LAST_NAME, "") ?: "",
            patientId = preferences.getString(SignInActivity.PATIENT_ID, "") ?: "",
            tenant = preferences.getString(SignInActivity.TENANT, "") ?: ""
        )
    }
}