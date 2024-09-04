package com.example.meal_mission_app.services;

import com.example.meal_mission_app.DTO.LoginResponse
import com.example.meal_mission_app.DTO.RestaurantDetailResponse
import com.example.meal_mission_app.DTO.RestaurantResponse
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.GET
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

    @GET("/api/getRestaurants")
    suspend fun getRestaurants(
        @Header("Authorization") authToken: String
    ): Response<List<RestaurantResponse>>

    @POST("/api/getRestaurant")
    suspend fun getRestaurant(
        @Body data: Map<String, String?>,
        @Header("Authorization") authToken: String? = null
    ): Response<RestaurantDetailResponse>
}

