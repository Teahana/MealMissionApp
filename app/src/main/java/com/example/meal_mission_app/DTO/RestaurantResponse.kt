package com.example.meal_mission_app.DTO

data class RestaurantResponse(
    val id : Long,
    val name: String,
    val description: String,
    val address: String,
    val imageUrl: String,
    val distance: String?
)
