package org.mmh.virtual_assistant.core

import android.content.Context
import org.mmh.virtual_assistant.SignInActivity
import org.mmh.virtual_assistant.domain.model.APiUrl
import org.mmh.virtual_assistant.domain.model.BodyPart
import org.mmh.virtual_assistant.domain.model.LogInData
import java.util.*
import kotlin.math.acos
import kotlin.math.sqrt

object Utilities {
    fun angle(
        startPoint: Point,
        middlePoint: Point = Point(0f, 0f),
        endPoint: Point = Point(1f, 0f),
        clockWise: Boolean = false
    ): Float {
        if ((middlePoint != Point(0f, 0f)) && (endPoint != Point(1f, 0f))) {
            val vectorBA = Point(startPoint.x - middlePoint.x, startPoint.y - middlePoint.y)
            val vectorBC = Point(endPoint.x - middlePoint.x, endPoint.y - middlePoint.y)
            val vectorBAAngle = angle(vectorBA)
            val vectorBCAngle = angle(vectorBC)
            var angleValue = if (vectorBAAngle > vectorBCAngle) {
                vectorBAAngle - vectorBCAngle
            } else {
                360 + vectorBAAngle - vectorBCAngle
            }
            if (clockWise) {
                angleValue = 360 - angleValue
            }
            return angleValue
        } else {
            val x = startPoint.x
            val y = startPoint.y
            val magnitude = sqrt((x * x + y * y).toDouble())
            var angleValue = if (magnitude >= 0.0001) {
                acos(x / magnitude)
            } else {
                0
            }
            angleValue = Math.toDegrees(angleValue.toDouble())
            if (y < 0) {
                angleValue = 360 - angleValue
            }
            return angleValue.toFloat()
        }
    }

    fun currentDate(): String {
        val currentDate = Calendar.getInstance()
        val day = currentDate.get(Calendar.DATE)
        val month = currentDate.get(Calendar.MONTH)
        val year = currentDate.get(Calendar.YEAR)
        return "$month/$day/$year"
    }

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

    fun getIndex(name: String): Int {
        return when (name) {
            "NOSE".lowercase() -> BodyPart.NOSE.position
            "LEFT_EYE".lowercase() -> BodyPart.LEFT_EYE.position
            "RIGHT_EYE".lowercase() -> BodyPart.RIGHT_EYE.position
            "LEFT_EAR".lowercase() -> BodyPart.LEFT_EAR.position
            "RIGHT_EAR".lowercase() -> BodyPart.RIGHT_EAR.position
            "LEFT_SHOULDER".lowercase() -> BodyPart.LEFT_SHOULDER.position
            "RIGHT_SHOULDER".lowercase() -> BodyPart.RIGHT_SHOULDER.position
            "LEFT_ELBOW".lowercase() -> BodyPart.LEFT_ELBOW.position
            "RIGHT_ELBOW".lowercase() -> BodyPart.RIGHT_ELBOW.position
            "LEFT_WRIST".lowercase() -> BodyPart.LEFT_WRIST.position
            "RIGHT_WRIST".lowercase() -> BodyPart.RIGHT_WRIST.position
            "LEFT_HIP".lowercase() -> BodyPart.LEFT_HIP.position
            "RIGHT_HIP".lowercase() -> BodyPart.RIGHT_HIP.position
            "LEFT_KNEE".lowercase() -> BodyPart.LEFT_KNEE.position
            "RIGHT_KNEE".lowercase() -> BodyPart.RIGHT_KNEE.position
            "LEFT_ANKLE".lowercase() -> BodyPart.LEFT_ANKLE.position
            "RIGHT_ANKLE".lowercase() -> BodyPart.RIGHT_ANKLE.position
            else -> -1
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