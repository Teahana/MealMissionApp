package com.example.meal_mission_app.DTO

data class Meal(
    val id: Long,
    var restaurantId: Long,
    var name: String?,
    var description: String?,
    var price: Double,
    var imageUrl: String?,
    var available: Boolean,
    var itemIds: Set<Long>
)

