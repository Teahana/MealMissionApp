package com.example.meal_mission_app.services;

import com.example.meal_mission_app.DTO.Location
import com.example.meal_mission_app.DTO.LoginResponse
import com.example.meal_mission_app.DTO.RestaurantResponse
import com.example.meal_mission_app.pages.customer.RestaurantDetailResponse
import com.example.meal_mission_app.pages.customer.UserLocation
import com.example.meal_mission_app.pages.restaurant.CustomerLiveOrder
import com.example.meal_mission_app.pages.restaurant.CustomerOrderDto
import com.example.meal_mission_app.pages.restaurant.StatusUpdateResponse
import com.google.gson.JsonObject
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
    @POST("/api/saveUserLocation")
    suspend fun saveUserLocation(
        @Body data: Map<String, String?>,
        @Header("Authorization") authToken: String? = null
    ): Response<Map<String, Any>>
    @POST("/api/getUserLocations")
    suspend fun getUserLocations(
        @Body data: Map<String, String?>,
        @Header("Authorization") authToken: String? = null
    ): Response<List<UserLocation>>
    @GET("/api/getRestaurants")
    suspend fun getRestaurants(
        @Header("Authorization") authToken: String
    ): Response<List<RestaurantResponse>>

    @POST("/api/getRestaurant")
    suspend fun getRestaurant(
        @Body data: Map<String, String?>,
        @Header("Authorization") authToken: String? = null
    ): Response<RestaurantDetailResponse>

    @POST("/api/getLiveOrders")
    suspend fun getRestaurantLiveOrders(
       @Header("Authorization") authToken: String,
       @Body requestBody: Map<String, String?>
    ): Response<List<CustomerLiveOrder>>
    @POST("/api/getCustomerLiveOrders")
    suspend fun getCustomerLiveOrders(
        @Header("Authorization") authToken: String,
        @Body requestBody: Map<String, String?>
    ): Response<List<CustomerLiveOrder>>

    @POST("/api/getOrderDetails")
    suspend fun getOrderDetails(
        @Body requestBody: Map<String, String?>,
        @Header("Authorization") authToken: String?
    ): Response<CustomerOrderDto>

    @POST("/api/orderStatusUpdateAccept")
    suspend fun updateOrderStatus(
        @Body requestBody: JsonObject,
        @Header("Authorization") authToken: String
    ): Response<StatusUpdateResponse>

    @POST("/api/orderStatusUpdateReady")
    suspend fun orderStatusUpdateReady(
        @Body requestBody: JsonObject,
        @Header("Authorization") authToken: String
    ): Response<StatusUpdateResponse>

    @POST("/api/orderStatusUpdateDelivering")
    suspend fun orderStatusUpdateDelivering(
        @Body requestBody: MutableMap<String, Any>,
        @Header("Authorization") authToken: String
    ): Response<StatusUpdateResponse>


    @POST("/api/submitOrder")
    suspend fun submitOrder(
        @Body requestBody: MutableMap<String,Any>,
        @Header("Authorization") authToken: String
    ): Response<StatusUpdateResponse>

    @POST("/api/getReadyOrders")
    suspend fun getReadyOrders(
        @Body requestBody: Map<String, String?>,
        @Header("Authorization") authToken: String
    ): Response<List<CustomerLiveOrder>>
    @POST("/api/deleteLocation")
    suspend fun deleteLocation(
        @Body requestData: Map<String, String>,
        @Header("Authorization") token: String
    ): Response<Map<String, Any>>
    @POST("/api/updateDriverLocation")
    suspend fun updateDriverLocation(
        @Body requestData: Map<String, String>,
        @Header("Authorization") token: String
    ): Response<Map<String, Any>>

    @POST("/api/updateFcmToken")
    suspend fun updateFcmToken(
        @Body requestData: Map<String, String>,
        @Header("Authorization") token: String?
    ): Response<Map<String, Any>>

    @POST("/api/getCustomerCompletedOrders")
    suspend fun getCustomerCompletedOrders(
        @Header("Authorization") authToken: String,
        @Body requestBody: Map<String, String?>
    ): Response<List<CustomerLiveOrder>>

    @POST("/api/getDriverLocation")
    suspend fun getDriverLocation(
        @Header("Authorization") authToken: String?,
        @Body requestBody: Map<String, String?>
    ): Response<Location>



}

