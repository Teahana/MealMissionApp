package com.example.meal_mission_app.pages

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.meal_mission_app.DTO.RestaurantDetailResponse
import com.example.meal_mission_app.R
import com.example.meal_mission_app.helper.ItemAdapter
import com.example.meal_mission_app.helper.MealAdapter
import com.example.meal_mission_app.objects.NetworkClient
import com.example.meal_mission_app.objects.OfflineStorageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RestaurantDetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restaurant_details)

        val restaurantId = intent.getLongExtra("restaurantId", -1L)
        println("Restaurant id: $restaurantId")
        if (restaurantId != -1L) {
            fetchRestaurantDetails(restaurantId)
        }
    }

    private fun fetchRestaurantDetails(restaurantId: Long) {
        val token = "Bearer ${OfflineStorageService.getToken(this)}"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = NetworkClient.apiService.getRestaurant(
                    data = mapOf("restaurantId" to restaurantId.toString()),
                    authToken = token
                )
                if (response.isSuccessful) {
                    val restaurantDetails = response.body()
                    println("Restaurant details: " + restaurantDetails.toString())
                    withContext(Dispatchers.Main) {
                        if (restaurantDetails != null) {
                            displayRestaurantDetails(restaurantDetails)
                        } else {
                            Toast.makeText(this@RestaurantDetailsActivity, "Failed to load restaurant details", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@RestaurantDetailsActivity, "Failed to fetch restaurant details", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                println("Error: " + e.message)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@RestaurantDetailsActivity, "Error fetching restaurant details", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun displayRestaurantDetails(restaurantDetails: RestaurantDetailResponse) {
        findViewById<TextView>(R.id.restaurantName).text = restaurantDetails.name
        findViewById<TextView>(R.id.restaurantDescription).text = restaurantDetails.description
        findViewById<TextView>(R.id.restaurantAddress).text = restaurantDetails.address
        findViewById<TextView>(R.id.restaurantPhone).text = restaurantDetails.phone ?: "N/A"
        findViewById<TextView>(R.id.restaurantEmail).text = restaurantDetails.email ?: "N/A"

        // You can also set up RecyclerViews to display meals and items
        // Set up adapter for meals
        val mealsRecyclerView: RecyclerView = findViewById(R.id.mealsRecyclerView)
        mealsRecyclerView.layoutManager = LinearLayoutManager(this)
        mealsRecyclerView.adapter = MealAdapter(restaurantDetails.meals)

        // Set up adapter for items
        val itemsRecyclerView: RecyclerView = findViewById(R.id.itemsRecyclerView)
        itemsRecyclerView.layoutManager = LinearLayoutManager(this)
        itemsRecyclerView.adapter = ItemAdapter(restaurantDetails.items)
    }
}
