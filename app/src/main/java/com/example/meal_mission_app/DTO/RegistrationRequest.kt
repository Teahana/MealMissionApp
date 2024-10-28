package com.example.meal_mission_app.DTO

// RegistrationRequest.kt

data class RegistrationRequest(
    val username: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val phone: String
)
data class ApiResponse(
    val success: Boolean,
    val message: String
)
