package com.example.meal_mission_app.services;

import com.example.meal_mission_app.DTO.ApiResponse
import com.example.meal_mission_app.DTO.ChangePasswordRequest
import com.example.meal_mission_app.DTO.ChangePasswordResponse
import com.example.meal_mission_app.DTO.ImageUploadResponse
import com.example.meal_mission_app.DTO.Item
import com.example.meal_mission_app.DTO.Location
import com.example.meal_mission_app.DTO.LoginResponse
import com.example.meal_mission_app.DTO.Meal
import com.example.meal_mission_app.DTO.RegistrationRequest
import com.example.meal_mission_app.DTO.Restaurant
import com.example.meal_mission_app.DTO.RestaurantResponse
import com.example.meal_mission_app.DTO.UserDetailsResponse
import com.example.meal_mission_app.pages.customer.RestaurantDetailResponse
import com.example.meal_mission_app.pages.customer.UserLocation
import com.example.meal_mission_app.pages.restaurant.CustomerLiveOrder
import com.example.meal_mission_app.pages.restaurant.CustomerOrderDto
import com.example.meal_mission_app.pages.restaurant.StatusUpdateResponse
import com.google.gson.JsonObject
import okhttp3.MultipartBody
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.GET
import retrofit2.http.Header;
import retrofit2.http.Multipart
import retrofit2.http.POST;
import retrofit2.http.Part
import retrofit2.http.Query
import retrofit2.http.Url;

interface ApiService {
    // Items
    @POST("/api/items/create")
    suspend fun createItem(
        @Body item: Item,
        @Header("Authorization") authToken: String
    ): Response<Map<String,String>>

    @POST("/api/items/update")
    suspend fun updateItem(
        @Body item: Item,
        @Header("Authorization") authToken: String
    ): Response<Map<String,String>>

    @GET("/api/items/list")
    suspend fun listItems(
        @Query("restaurantId") restaurantId: Long,
        @Header("Authorization") authToken: String
    ): Response<List<Item>>

    @POST("/api/items/setAvailability")
    suspend fun setItemAvailability(
        @Body request: Map<String, Any>,
        @Header("Authorization") authToken: String
    ): Response<String>

    // Meals
    @POST("/api/meals/create")
    suspend fun createMeal(
        @Body meal: Meal,
        @Header("Authorization") authToken: String
    ): Response<Map<String,String>>

    @POST("/api/meals/update")
    suspend fun updateMeal(
        @Body meal: Meal,
        @Header("Authorization") authToken: String
    ): Response<Map<String,String>>

    @GET("/api/meals/list")
    suspend fun listMeals(
        @Query("restaurantId") restaurantId: Long,
        @Header("Authorization") authToken: String
    ): Response<List<Meal>>

    @POST("/api/meals/setAvailability")
    suspend fun setMealAvailability(
        @Body request: Map<String, Any>,
        @Header("Authorization") authToken: String
    ): Response<String>

    // Restaurant
    @POST("/api/restaurants/update")
    suspend fun updateRestaurant(
        @Body restaurant: Restaurant,
        @Header("Authorization") authToken: String
    ): Response<String>
    @POST("/api/restaurants/getRestaurantDetails")
    suspend fun getRestaurantDetails(
        @Body requestBody: Map<String, String?>,
        @Header("Authorization") authToken: String?
    ): Response<Restaurant>


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
    // In your API interface
    @GET("/api/getRestaurants")
    suspend fun getRestaurants(
        @Header("Authorization") authToken: String,
        @Query("latitude") latitude: String,
        @Query("longitude") longitude: String
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
    @POST("/api/getCompletedOrders")
    suspend fun getRestaurantCompletedOrders(
        @Header("Authorization") token: String,
        @Body requestBody: Map<String, String>
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

    @POST("/api/orderStatusUpdateDelivered")
    suspend fun orderStatusUpdateDelivered(
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

    @POST("/api/getCompletedOrdersDriver")
    suspend fun getDriverCompletedOrders(
        @Body requestBody: Map<String, String?>,
        @Header("Authorization") authToken: String
    ): Response<List<CustomerLiveOrder>>

    @POST("/api/getDriverProfile")
    suspend fun getDriverProfile(
        @Body requestBody: Map<String, String?>,
        @Header("Authorization") authToken: String
    ): Response<Map<String,Any>>

    @Multipart
    @POST("/api/uploadImage")
    suspend fun uploadImage(
        @Part image: MultipartBody.Part,
        @Header("Authorization") authToken: String
    ): Response<ImageUploadResponse>

    @POST("/api/restaurants/saveLocation")
    suspend fun saveRestaurantLocation(
        @Body requestBody: Map<String, String?>,
        @Header("Authorization") authToken: String
    ): Response<Map<String,Any>>


    @POST("/api/notifyCustomer")
    suspend fun notifyCustomer(
        @Body data: Map<String, String>,
        @Header("Authorization") authToken: String
    ): Response<Map<String, Any>>


    @GET("/api/user/details")
    suspend fun getUserDetails(
        @Query("userId") userId: Long,
        @Header("Authorization") authToken: String
    ): Response<UserDetailsResponse>


    @POST("/api/user/changePassword")
    suspend fun changePassword(
        @Body changePasswordRequest: ChangePasswordRequest?,
        @Header("Authorization") authToken: String
    ): Response<ChangePasswordResponse>



    @POST("/api/register/customer")
    suspend fun registerCustomer(
        @Body registrationRequest: RegistrationRequest
    ): Response<ApiResponse>



}

