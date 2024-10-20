package com.example.meal_mission_app.pages.driver

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import com.example.meal_mission_app.R
import com.example.meal_mission_app.helper.OrderStatus
import com.example.meal_mission_app.objects.NetworkClient
import com.example.meal_mission_app.objects.OfflineStorageService
import com.example.meal_mission_app.pages.restaurant.CustomerOrderDto
import com.example.meal_mission_app.services.LocationForegroundService
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.Task
import com.google.maps.android.PolyUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONObject
import okhttp3.Request

class DriverOrderDetailsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private lateinit var textViewOrderDetails: TextView
    private lateinit var buttonAccept: Button
    private lateinit var buttonDelivered: Button
    private lateinit var toolbar: Toolbar
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var driverMarker: Marker? = null
    private var orderId: Long = -1
    private var customerLatitude: Double = 0.0
    private var customerLongitude: Double = 0.0

    private val handler = Handler(Looper.getMainLooper())

    private var hasZoomedToFit = false
    private var hasFetchedRoute = false

    companion object {
        private const val REQUEST_LOCATION_PERMISSIONS_CODE = 1001
        private const val REQUEST_CHECK_SETTINGS = 1002
        private const val UPDATE_INTERVAL = 10000L
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_order_detail)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mapView = findViewById(R.id.mapView)
        textViewOrderDetails = findViewById(R.id.textViewOrderDetails)
        buttonAccept = findViewById(R.id.buttonAccept)
        buttonDelivered = findViewById(R.id.buttonDelivered)

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this) // onMapReady will be called when map is ready

        orderId = intent.getLongExtra("ORDER_ID", -1)

        buttonAccept.setOnClickListener { acceptOrder() }
        buttonDelivered.setOnClickListener { deliverOrder() }

        checkPermissionsAndSettings()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true

        // Set the initial zoom level to a broader view (e.g., a country-level view)
        googleMap.moveCamera(CameraUpdateFactory.zoomTo(5f)) // Adjust as needed

        // Now that the map is ready, fetch order details
        fetchOrderDetails()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        handler.removeCallbacksAndMessages(null) // Stop location updates when paused
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    private fun checkPermissionsAndSettings() {
        if (checkLocationPermission()) {
            checkLocationSettings()
        } else {
            requestLocationPermission()
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_LOCATION_PERMISSIONS_CODE
        )
    }
    private fun checkLocationSettings(startForMapUpdates: Boolean = true) {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 10000L
        ).build()

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            // GPS is already enabled
            if (startForMapUpdates) {
                // Start location updates for updating the map
                startLocationUpdates()
            }
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    exception.startResolutionForResult(this, REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    showToast("Unable to resolve GPS issue")
                }
            } else {
                showToast("GPS is required for this feature.")
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                // GPS has been enabled by the user, proceed to start location updates
                startLocationUpdates()
            } else {
                // GPS was not enabled, show a message to the user
                showToast("GPS is required to proceed.")
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchOrderDetails() {
        val token = "Bearer ${OfflineStorageService.getToken(this)}"
        val requestData = mapOf("orderId" to orderId.toString())

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = NetworkClient.apiService.getOrderDetails(requestData, token)
                if (response.isSuccessful) {
                    response.body()?.let { orderDto ->
                        withContext(Dispatchers.Main) {
                            if (orderDto.status == OrderStatus.DELIVERING.toString()) {
                                // Show toast to notify driver and redirect to the previous page
                                showToast("Order already accepted by another driver.")
                                // Redirect to the previous page
                                finish() // This will close the current activity and go back to the previous one
                            } else {
                                handleOrderDetails(orderDto) // Proceed if the order is not already delivering
                            }
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showToast("Failed to fetch order details")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Error: ${e.localizedMessage}")
                }
            }
        }
    }

    private fun handleOrderDetails(order: CustomerOrderDto) {
        val itemsDetails = order.items.joinToString("\n") { item ->
            "${item.quantity} x ${item.itemName} (\$${item.itemPrice})"
        }

        val mealsDetails = order.meals.joinToString("\n") { meal ->
            "${meal.quantity} x ${meal.mealName} (\$${meal.mealPrice})"
        }

        val orderDetailsText = """
        Customer: ${order.customerName}
        Address: ${order.customerAddress}
        Directions: ${order.locationDirections}
        Order Date: ${order.orderDate}
        Order Time: ${order.orderTime}
        
        Items:
        $itemsDetails
        
        Meals:
        $mealsDetails
        
        Total Price: \$${order.totalPrice}
    """.trimIndent()

        // Set text to the TextView
        textViewOrderDetails.text = orderDetailsText

        customerLatitude = order.latitude
        customerLongitude = order.longitude

        val customerLatLng = LatLng(customerLatitude, customerLongitude)

        // Ensure googleMap is initialized
        googleMap.addMarker(
            MarkerOptions().position(customerLatLng).title("Customer Location")
        )

        // Start fetching driver's location after customer location is set
        fetchDriverLocation()
    }

    private fun updateCameraToFitLocations(driverLatLng: LatLng, customerLatLng: LatLng) {
        val builder = LatLngBounds.builder()
        builder.include(driverLatLng)
        builder.include(customerLatLng)

        val bounds = builder.build()
        val padding = 100 // Padding around the bounds

        // Move the camera to show both points
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun acceptOrder() {
        val token = "Bearer ${OfflineStorageService.getToken(this)}"
        val driverId = OfflineStorageService.getUserId(this)
        val requestData: MutableMap<String, Any> = mutableMapOf(
            "orderId" to orderId.toString(),
            "driverId" to driverId.toString()
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Make the API call to update the order status to "Delivering"
                val response = NetworkClient.apiService.orderStatusUpdateDelivering(requestData, token)

                // Check if the response is successful (HTTP 2xx)
                if (response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        // First, check location permission
                        if (checkLocationPermission()) {
                            // Now check if GPS is on, without starting map updates
                            checkLocationSettings(false)

                            // Start the ForegroundService for sending the driver's location to the server
                            val intent = Intent(this@DriverOrderDetailsActivity, LocationForegroundService::class.java)
                            startForegroundService(intent)

                            buttonAccept.isEnabled = false
                            buttonDelivered.isEnabled = true
                            showToast("Order accepted successfully!")
                        } else {
                            requestLocationPermission() // Request location permission if not granted
                        }
                    }
                }
                // Handle the case where the order was already accepted (HTTP 409)
                else if (response.code() == 409) {
                    withContext(Dispatchers.Main) {
                        val statusResponse = response.body() // Parse the response body
                        if (statusResponse?.status == OrderStatus.DUPLICATE.toString()) {
                            showToast("Order already accepted by another driver.")
                            finish() // Go back to the previous screen
                        }
                    }
                }
                // Handle other errors
                else {
                    withContext(Dispatchers.Main) {
                        showToast("Failed to accept order: ${response.message()}")
                    }
                }
            } catch (e: Exception) {
                println("BIGError: ${e.message}")
                // Handle exceptions (e.g., network issues)
                withContext(Dispatchers.Main) {
                    showToast("Error: ${e.localizedMessage}")
                }
            }
        }
    }





    @RequiresApi(Build.VERSION_CODES.O)
    private fun deliverOrder() {
        stopService(Intent(this, LocationForegroundService::class.java))
        showToast("Order delivered")
        finish()
    }

    private fun startLocationUpdates() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                fetchDriverLocation()
                handler.postDelayed(this, UPDATE_INTERVAL)
            }
        }, UPDATE_INTERVAL)
    }

    private fun fetchDriverLocation() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val driverLatLng = LatLng(it.latitude, it.longitude)
                    updateDriverMarker(driverLatLng)

                    // Only proceed if customer location is available
                    if (customerLatitude != 0.0 && customerLongitude != 0.0) {
                        val customerLatLng = LatLng(customerLatitude, customerLongitude)

                        if (!hasFetchedRoute) {
                            fetchRoute(driverLatLng, customerLatLng)
                            hasFetchedRoute = true // Prevent multiple fetches
                        }

                        // Update camera only the first time
                        if (!hasZoomedToFit) {
                            updateCameraToFitLocations(driverLatLng, customerLatLng)
                            hasZoomedToFit = true
                        }
                    }
                }
            }.addOnFailureListener { e ->
                println("Failed to fetch location: ${e.message}")
                showToast("Unable to get location. Make sure GPS is enabled.")
            }
        } else {
            showToast("Location permission not granted")
            requestLocationPermission()
        }
    }
    private fun getGoogleMapsApiKey(): String {
        val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        return appInfo.metaData.getString("com.google.android.geo.API_KEY", "")
    }
    private val client = OkHttpClient()

    private fun fetchRoute(driverLatLng: LatLng, customerLatLng: LatLng) {
        val apiKey = getGoogleMapsApiKey() // Replace with your actual API key
        val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=${driverLatLng.latitude},${driverLatLng.longitude}" +
                "&destination=${customerLatLng.latitude},${customerLatLng.longitude}" +
                "&key=$apiKey"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        showToast("Failed to fetch route: ${response.message}")
                    }
                    return@launch
                }

                val jsonResponse = JSONObject(response.body?.string() ?: "")

                val routes = jsonResponse.getJSONArray("routes")
                if (routes.length() > 0) {
                    val points = routes.getJSONObject(0)
                        .getJSONObject("overview_polyline")
                        .getString("points")

                    withContext(Dispatchers.Main) {
                        drawPolyline(points)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showToast("No route found")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Failed to fetch route: ${e.localizedMessage}")
                }
            }
        }
    }

    private fun drawPolyline(encodedPoints: String) {
        val decodedPath = PolyUtil.decode(encodedPoints)
        googleMap.addPolyline(
            PolylineOptions()
                .addAll(decodedPath)
                .width(10f)
                .color(Color.BLUE) // Use Color.BLUE or any other color
        )
    }

    private fun updateDriverMarker(driverLatLng: LatLng) {
        if (driverMarker == null) {
            driverMarker = googleMap.addMarker(
                MarkerOptions().position(driverLatLng).title("Driver Location")
            )
        } else {
            driverMarker?.position = driverLatLng
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSIONS_CODE &&
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationUpdates()
        } else {
            showToast("Location permission denied")
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle back button in the toolbar
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
