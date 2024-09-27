package com.example.kiosk02.admin

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface BusinessService {

    @POST("status")
    fun checkBusinessNumber(
        @Query("serviceKey") serviceKey: String,
        @Body requestBody: BusinessRequest
    ): Call<BusinessResponse>
}

data class BusinessRequest(
    val b_no: List<String>
)

data class BusinessResponse(
    val status_code: String,
    val data: List<BusinessData>
)

data class BusinessData(
    val b_no: String,
    val b_stt_cd: String,
    val tax_type: String
)