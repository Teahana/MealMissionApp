package com.example.meal_mission_app.DTO

data class Restaurant(
    val id: Long,
    var name: String,
    var address: String,
    val city: String?,
    var description: String,
    var logoUrl: String,
    var latitude: Double,
    var longitude: Double
)
