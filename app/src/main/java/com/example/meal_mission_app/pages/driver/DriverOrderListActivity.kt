package com.example.meal_mission_app.pages.driver

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.meal_mission_app.R
import com.example.meal_mission_app.objects.NetworkClient
import com.example.meal_mission_app.objects.OfflineStorageService
import com.example.meal_mission_app.pages.restaurant.CustomerLiveOrder
import com.example.meal_mission_app.pages.restaurant.OrderAdapter
import com.example.meal_mission_app.services.LocationService
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.*
import java.time.LocalDate
import java.time.LocalTime

class DriverOrderListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var orderAdapter: OrderAdapter
    private lateinit var locationService: LocationService
    private val orders = mutableListOf<CustomerLiveOrder>()
    private var pollingJob: Job? = null
    private var bestLocation: Location? = null
    private var retryCount = 0
    private val MAX_RETRIES = 5
    private val REQUEST_LOCATION_PERMISSIONS_CODE = 1001
    private val REQUEST_CHECK_SETTINGS = 1002

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_list)

        locationService = LocationService(this)

        recyclerView = findViewById(R.id.recyclerViewOrders)
        recyclerView.layoutManager = LinearLayoutManager(this)
        orderAdapter = OrderAdapter(orders) { orderId, orderStatus ->
            val intent = Intent(this, DriverOrderDetailsActivity::class.java)
            intent.putExtra("ORDER_ID", orderId)
            intent.putExtra("ORDER_STATUS", orderStatus)
            startActivity(intent)
        }
        recyclerView.adapter = orderAdapter

        if (checkLocationPermission()) {
            checkLocationSettings()
        } else {
            requestLocationPermission()
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStart() {
        super.onStart()
        if (checkLocationPermission()) {
            checkLocationSettings()
        }
    }
    override fun onStop() {
        super.onStop()
        stopPolling()  // Stop polling when the activity goes to the background
    }
    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        if (checkLocationPermission()) {
            checkLocationSettings()
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            REQUEST_LOCATION_PERMISSIONS_CODE
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkLocationSettings() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10000L
        ).apply {
            setMinUpdateIntervalMillis(10000L)
        }.build()

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true)

        val settingsClient: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = settingsClient.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            startPolling() // Fetch driver orders if settings are enabled
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    exception.startResolutionForResult(this, REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Toast.makeText(this, "Unable to resolve location settings.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please enable location services to continue.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startPolling() {
        if (pollingJob == null || pollingJob?.isActive == false) {
            pollingJob = CoroutineScope(Dispatchers.Default).launch {
                while (isActive) {
                    getUserLocation()
                    delay(10000)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getUserLocation() {
        locationService.getCurrentLocation { location ->
            if (location != null && location.accuracy <= 30) {
                bestLocation = location
                // Launch the coroutine here
                CoroutineScope(Dispatchers.IO).launch {
                    fetchDriverOrders(location.latitude, location.longitude)
                }
            } else {
                if (retryCount < MAX_RETRIES) {
                    retryCount++
                    Handler(Looper.getMainLooper()).postDelayed({ getUserLocation() }, 2000)
                } else {
                    bestLocation?.let {
                        Toast.makeText(this, "Using best available location", Toast.LENGTH_SHORT).show()
                        CoroutineScope(Dispatchers.IO).launch {
                            fetchDriverOrders(it.latitude, it.longitude)
                        }
                    } ?: run {
                        Toast.makeText(this, "Unable to find accurate location.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun fetchDriverOrders(latitude: Double, longitude: Double) {
        val token = "Bearer ${OfflineStorageService.getToken(this)}"

        val requestData = mapOf(
            "driverLatitude" to latitude.toString(),
            "driverLongitude" to longitude.toString()
        )

        try {
            val response = NetworkClient.apiService.getReadyOrders(requestData, token)
            if (response.isSuccessful) {
                val newOrders = response.body() ?: emptyList()
                updateOrders(newOrders)
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DriverOrderListActivity, "Failed to fetch driver orders", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@DriverOrderListActivity, "Error fetching driver orders: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateOrders(newOrders: List<CustomerLiveOrder>) {
        val updatedOrders = newOrders.toMutableList()

        if (updatedOrders != orders) {
            orders.clear()
            orders.addAll(updatedOrders)
            CoroutineScope(Dispatchers.Main).launch {
                orderAdapter.notifyDataSetChanged()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS && resultCode == Activity.RESULT_OK) {
            startPolling()
        } else {
            Toast.makeText(this, "GPS is required to fetch orders.", Toast.LENGTH_SHORT).show()
        }
    }
}
