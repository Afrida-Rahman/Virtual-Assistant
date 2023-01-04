package org.mmh.virtual_assistant.api

import org.mmh.virtual_assistant.api.request.AssessmentListRequestPayload
import org.mmh.virtual_assistant.api.request.ExerciseListRequestPayload
import org.mmh.virtual_assistant.api.request.ExerciseRequestPayload
import org.mmh.virtual_assistant.api.request.ExerciseTrackingPayload
import org.mmh.virtual_assistant.api.response.AssessmentListResponse
import org.mmh.virtual_assistant.api.response.ExerciseListResponse
import org.mmh.virtual_assistant.api.response.ExerciseTrackingResponse
import org.mmh.virtual_assistant.api.response.KeyPointRestrictions
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface IExerciseService {

    @POST("/api/exercisekeypoint/GetPatientAssessments")
    fun getAssessmentList(@Body requestPayload: AssessmentListRequestPayload): Call<AssessmentListResponse>

    @POST("/api/exercisekeypoint/GetPatientExercises")
    fun getExerciseList(@Body requestPayload: ExerciseListRequestPayload): Call<ExerciseListResponse>

    @POST("/api/exercisekeypoint/GetPatientExerciseRestrictions")
    fun getExerciseConstraint(@Body requestPayload: ExerciseRequestPayload): Call<KeyPointRestrictions>

    @Headers("Authorization: Bearer YXBpdXNlcjpZV2xoYVlUUmNHbDFjMlZ5T2lRa1RVWVRFUk1ESXc=")
    @POST("/api/exercise/SaveExerciseTracking")
    fun saveExerciseData(@Body requestPayload: ExerciseTrackingPayload): Call<ExerciseTrackingResponse>

}