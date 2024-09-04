package com.example.meal_mission_app.DTO



data class RestaurantDetailResponse(
    val id: Long,
    val name: String,
    val address: String,
    val phone: String?,
    val email: String?,
    val description: String,
    val items: List<ItemResponse>,
    val meals: List<MealResponse>
)

data class MealResponse(
    val id: Long,
    val name: String,
    val price: Double,
    val description: String,
    val items: List<ItemResponse>
)

data class ItemResponse(
    val id: Long,
    val name: String,
    val description: String,
    val price: Double
)

