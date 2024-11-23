package com.example.kiosk02.map

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object SearchRepository {

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(AppInterceptor())
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://openapi.naver.com")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .client(okHttpClient)
        .build()

    private val service = retrofit.create(SearchService::class.java)

    fun getStore(query: String): Call<SearchResult> {
        return service.getRestaurant(query = "$query 맛집", display = 5)
    }

    class AppInterceptor: Interceptor{
        override fun intercept(chain: Interceptor.Chain): Response {
            val newRequest = chain.request().newBuilder()
                .addHeader("X-Naver-Client-Id", "TqvWjVyGzkMsMEDThsI2")
                .addHeader("X-Naver-Client-Secret", "VC0hRYg9LS")
                .build()
            return chain.proceed(newRequest)
        }

    }
}