package com.example.meal_mission_app.DTO

// UserDetailsResponse.kt

data class UserDetailsResponse(
    val id: Long,
    val name: String,
    val email: String,
    val phoneNumber: String
)
data class ChangePasswordRequest(
    val userId: Long,
    val currentPassword: String,
    val newPassword: String
)

// ChangePasswordResponse.kt

data class ChangePasswordResponse(
    val success: Boolean,
    val message: String
)
