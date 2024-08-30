package com.example.meal_mission_app.services;

import com.example.meal_mission_app.DTO.LoginResponse
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Url;

interface ApiService {

    @POST
    suspend fun postRequest(
        @Url url: String,
        @Body data: Map<String, String?>,
        @Header("Authorization") authToken: String? = null
    ): Response<LoginResponse>
}

