package com.example.kiosk02.admin

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    fun getInstance(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.odcloud.kr/api/nts-businessman/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}