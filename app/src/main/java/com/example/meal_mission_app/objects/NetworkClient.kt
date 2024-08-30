package com.example.meal_mission_app.objects

import com.example.meal_mission_app.services.ApiService
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkClient {  // This is a singleton object

    private const val BASE_URL = "https://mealsmission.com"

    // Lazy initialization of Retrofit
    private val retrofit: Retrofit by lazy {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .build()
    }

    // Lazy initialization of ApiService
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}
