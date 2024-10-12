package com.example.meal_mission_app.DTO

data class LoginResponse(
    val token: String?,
    val refreshToken: String?,
    val userId: String?,
    var userType: String?,
    var longitude: Double?,
    var latitude: Double?,
    var accuracy: Double?,
    var restaurantId: String?
   // var driverId: Long
)
