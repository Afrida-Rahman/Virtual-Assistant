package org.mmh.virtual_assistant.api

import org.mmh.virtual_assistant.api.request.LogInRequest
import org.mmh.virtual_assistant.api.response.LogInResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ILogInService {

    @POST("/api/Account/GetCrmContact")
    fun logIn(@Body requestPayload: LogInRequest): Call<LogInResponse>
}