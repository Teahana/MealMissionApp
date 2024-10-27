package com.example.meal_mission_app.pages.customer
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.meal_mission_app.DTO.RestaurantResponse
import com.example.meal_mission_app.R
import com.example.meal_mission_app.objects.NetworkClient
import com.example.meal_mission_app.objects.OfflineStorageService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CustomerHomepageActivityCustomer : CustomerBaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HomepageAdapter
    private lateinit var searchBar: EditText
    private var allRestaurants: List<RestaurantResponse> = listOf()  // Store the original list

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userLatitude: Double? = null
    private var userLongitude: Double? = null

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate the specific layout for CustomerHomepageActivity into the base FrameLayout
        layoutInflater.inflate(R.layout.activity_customer_homepage, findViewById(R.id.activity_content))

        recyclerView = findViewById(R.id.recyclerView)
        searchBar = findViewById(R.id.searchBar)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = HomepageAdapter(listOf()) { restaurant ->
            openRestaurantDetails(restaurant)
        }
        recyclerView.adapter = adapter

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        searchBar.addTextChangedListener {
            val searchText = it.toString()
            filterRestaurants(searchText)
        }

        if (checkLocationPermission()) {
            getUserLocation()
        } else {
            requestLocationPermission()
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getUserLocation() {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 5000
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // If permissions are not granted, request them
            requestLocationPermission()
            return
        }

        // Permissions are granted, proceed to get the location
        fusedLocationClient.getCurrentLocation(
            LocationRequest.PRIORITY_HIGH_ACCURACY,
            null
        ).addOnSuccessListener { location: Location? ->
            if (location != null) {
                userLatitude = location.latitude
                userLongitude = location.longitude
                fetchRestaurants()
            } else {
                Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show()
                fetchRestaurants()  // Proceed without location
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to get location", Toast.LENGTH_SHORT).show()
            fetchRestaurants()  // Proceed without location
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchRestaurants() {
        val token = "Bearer ${OfflineStorageService.getToken(this)}"

        val latitude = userLatitude?.toString() ?: ""
        val longitude = userLongitude?.toString() ?: ""

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = NetworkClient.apiService.getRestaurants(token, latitude, longitude)
                if (response.isSuccessful) {
                    val restaurants = response.body() ?: emptyList()
                    withContext(Dispatchers.Main) {
                        allRestaurants = restaurants
                        adapter.updateRestaurants(restaurants)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CustomerHomepageActivityCustomer, "Failed to fetch restaurants", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                println("Error: " + e.message)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CustomerHomepageActivityCustomer, "Error fetching restaurants", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun filterRestaurants(query: String) {
        val filteredList = if (query.isEmpty()) {
            allRestaurants
        } else {
            allRestaurants.filter { it.name?.contains(query, ignoreCase = true) == true }
        }
        adapter.updateRestaurants(filteredList)
    }

    private fun openRestaurantDetails(restaurant: RestaurantResponse) {
        val intent = Intent(this, RestaurantDetailsActivityCustomer::class.java)
        intent.putExtra("restaurantName", restaurant.name)
        intent.putExtra("restaurantDescription", restaurant.description)
        intent.putExtra("restaurantAddress", restaurant.address)
        intent.putExtra("restaurantId", restaurant.id)
        startActivity(intent)
    }

    override fun getSelectedItemId(): Int {
        return R.id.nav_home  // Indicate that this is the Home page
    }

    // Handle location permission result
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getUserLocation()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
                fetchRestaurants()  // Proceed without location
            }
        }
    }
}
class HomepageAdapter(
    var restaurants: List<RestaurantResponse>,
    private val onItemClick: (RestaurantResponse) -> Unit
) : RecyclerView.Adapter<HomepageAdapter.RestaurantViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RestaurantViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.restaurant_list_item, parent, false)
        return RestaurantViewHolder(view)
    }

    override fun onBindViewHolder(holder: RestaurantViewHolder, position: Int) {
        val restaurant = restaurants[position]
        holder.bind(restaurant)
        holder.itemView.setOnClickListener {
            onItemClick(restaurant)
        }
    }

    override fun getItemCount(): Int {
        return restaurants.size
    }

    fun updateRestaurants(newRestaurants: List<RestaurantResponse>) {
        this.restaurants = newRestaurants
        notifyDataSetChanged()
    }

    class RestaurantViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val cardView: CardView = view.findViewById(R.id.restaurantCard)
        private val logoImageView: ImageView = view.findViewById(R.id.restaurantLogo)
        private val nameTextView: TextView = view.findViewById(R.id.restaurantName)
        private val descriptionTextView: TextView = view.findViewById(R.id.restaurantDescription)
        private val addressTextView: TextView = view.findViewById(R.id.restaurantAddress)
        private val distanceTextView: TextView = view.findViewById(R.id.restaurantDistance)

        fun bind(restaurant: RestaurantResponse) {
            nameTextView.text = restaurant.name
            descriptionTextView.text = restaurant.description
            addressTextView.text = restaurant.address
            distanceTextView.text = restaurant.distance ?: ""

            // Load the logo image using Glide or any image loading library
            Glide.with(logoImageView.context)
                .load(restaurant.imageUrl)
                .placeholder(R.drawable.placeholder_image)  // Add a placeholder image in your drawable
                .error(R.drawable.placeholder_image)
                .into(logoImageView)
        }
    }
}
