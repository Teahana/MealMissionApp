package com.example.meal_mission_app.pages
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.meal_mission_app.DTO.RestaurantResponse
import com.example.meal_mission_app.R
import com.example.meal_mission_app.helper.RestaurantAdapter
import com.example.meal_mission_app.objects.NetworkClient
import com.example.meal_mission_app.objects.OfflineStorageService
import com.example.meal_mission_app.pages.RestaurantDetailsActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CustomerHomepageActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RestaurantAdapter
    private lateinit var searchBar: EditText
    private var allRestaurants: List<RestaurantResponse> = listOf()  // Store the original list

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_homepage)

        recyclerView = findViewById(R.id.recyclerView)
        searchBar = findViewById(R.id.searchBar)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = RestaurantAdapter(listOf()) { restaurant ->
            // Handle click on restaurant
            openRestaurantDetails(restaurant)
        }
        recyclerView.adapter = adapter

        searchBar.addTextChangedListener {
            val searchText = it.toString()
            filterRestaurants(searchText)
        }

        fetchRestaurants()
    }

    private fun fetchRestaurants() {
        val token = "Bearer ${OfflineStorageService.getToken(this)}"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = NetworkClient.apiService.getRestaurants(token)
                if (response.isSuccessful) {
                    val restaurants = response.body() ?: emptyList()
                    withContext(Dispatchers.Main) {
                        allRestaurants = restaurants  // Store the original list
                        adapter.updateRestaurants(restaurants)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CustomerHomepageActivity, "Failed to fetch restaurants", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                println("Error: " + e.message)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CustomerHomepageActivity, "Error fetching restaurants", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun filterRestaurants(query: String) {
        val filteredList = if (query.isEmpty()) {
            allRestaurants  // Show all restaurants when the search query is empty
        } else {
            allRestaurants.filter { it.name.contains(query, ignoreCase = true) }
        }
        adapter.updateRestaurants(filteredList)
    }

    private fun openRestaurantDetails(restaurant: RestaurantResponse) {
        val intent = Intent(this, RestaurantDetailsActivity::class.java)
        intent.putExtra("restaurantName", restaurant.name)
        intent.putExtra("restaurantDescription", restaurant.description)
        intent.putExtra("restaurantAddress", restaurant.address)
        intent.putExtra("restaurantId", restaurant.id)
        println("Restaurant ID before starting intent: " +  restaurant.id)
        startActivity(intent)
    }
}

