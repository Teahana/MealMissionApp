package com.example.meal_mission_app.pages

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.meal_mission_app.R
import com.example.meal_mission_app.objects.NetworkClient
import com.example.meal_mission_app.objects.OfflineStorageService
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.HttpException
import android.util.Log
import com.google.maps.android.PolyUtil
import okhttp3.OkHttpClient
import okhttp3.Request

class CustomerActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private val TAG = "CustomerActivity"

    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private var isRunning = false
    private var destinationMarker: Marker? = null // Customer's location
    private var originMarker: Marker? = null // Driver's location
    private var currentPolyline: Polyline? = null // Route polyline

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var apiKey: String

    companion object {
        private const val REQUEST_LOCATION_PERMISSIONS_CODE = 1001
        private const val REQUEST_CHECK_SETTINGS = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer)
        apiKey = getApiKeyFromManifest()

        // Initialize the map
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val startProcessButton: Button = findViewById(R.id.getDirectionsButton)
        val cancelProcessButton: Button = findViewById(R.id.cancelProcessButton)

        startProcessButton.setOnClickListener {
            startProcess()
        }

        cancelProcessButton.setOnClickListener {
            stopProcess()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        // Enable MyLocation layer if permissions are granted
        if (checkLocationPermission()) {
            mMap.isMyLocationEnabled = true
        }
        // Check for location permissions and fetch location if granted
        checkLocationPermissionAndSettings()
    }

    private fun checkLocationPermissionAndSettings() {
        if (checkLocationPermission()) {
            // Permission granted, check if location settings are enabled
            checkLocationSettings()
        } else {
            // Request permission
            requestLocationPermission()
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
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            REQUEST_LOCATION_PERMISSIONS_CODE
        )
    }

    private fun checkLocationSettings() {
        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L // Interval in milliseconds
        ).apply {
            setMinUpdateIntervalMillis(5000L) // Fastest interval in milliseconds
        }.build()

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true) // Show the dialog only if location settings are off

        val settingsClient: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = settingsClient.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            // All location settings are satisfied. Fetch the customer's location.
            fetchCustomerLocation()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    exception.startResolutionForResult(this@CustomerActivity, REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Toast.makeText(this, "Unable to resolve location settings.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please enable location services to continue.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getApiKeyFromManifest(): String {
        try {
            val applicationInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            val bundle = applicationInfo.metaData
            return bundle.getString("com.google.android.geo.API_KEY") ?: ""
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Failed to load API key from manifest", e)
            return ""
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_LOCATION_PERMISSIONS_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission granted, enable MyLocation layer and check settings
                    if (::mMap.isInitialized) {
                        if (ActivityCompat.checkSelfPermission(
                                this,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                                this,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            mMap.isMyLocationEnabled = true
                        }
                    }
                    // Check if location settings are enabled
                    checkLocationSettings()
                } else {
                    // Permission denied, show a message to the user
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CHECK_SETTINGS) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    // User agreed to make required location settings changes
                    fetchCustomerLocation()
                }
                Activity.RESULT_CANCELED -> {
                    // User chose not to make required location settings changes
                    Toast.makeText(this, "Location services are necessary for this feature.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun fetchCustomerLocation(retryCount: Int = 0, maxRetries: Int = 5) {
        println("FETCHING LOCATION - Attempt: ${retryCount + 1}")

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null && location.accuracy <= 30) {
                        println("Location fetched successfully with accuracy: ${location.accuracy}")
                        val customerLatLng = LatLng(location.latitude, location.longitude)
                        // Update the destination marker and stop further location updates
                        updateDestinationMarker(customerLatLng)
                    } else {
                        println("Location is either null or accuracy is above 30 meters")
                        handleLocationRetry(retryCount, maxRetries)
                    }
                }.addOnFailureListener { exception ->
                    println("Failed to fetch location: ${exception.message}")
                    handleLocationRetry(retryCount, maxRetries)
                }
            } catch (e: Exception) {
                println("Error while fetching location: ${e.message}")
                handleLocationRetry(retryCount, maxRetries)
            }
        } else {
            println("Location permission not granted")
        }
    }

    private fun handleLocationRetry(retryCount: Int, maxRetries: Int) {
        if (retryCount < maxRetries) {
            val delay = 2000L * (retryCount + 1) // Exponential backoff
            println("Retrying to fetch location in ${delay / 1000} seconds... (Attempt ${retryCount + 1})")
            Handler(Looper.getMainLooper()).postDelayed({
                fetchCustomerLocation(retryCount + 1, maxRetries)
            }, delay)
        } else {
            println("Max retries reached. Unable to determine location.")
            Toast.makeText(this, "Unable to determine location. Please try again later.", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateDestinationMarker(customerLatLng: LatLng) {
        if (::mMap.isInitialized) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(customerLatLng, 15f))
            if (destinationMarker == null) {
                destinationMarker = mMap.addMarker(
                    MarkerOptions()
                        .position(customerLatLng)
                        .title("Your Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                )
            } else {
                destinationMarker?.position = customerLatLng
            }
        } else {
            Toast.makeText(this, "Map is not initialized yet.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startProcess() {
        if (isRunning) return
        isRunning = true

        handler = Handler(Looper.getMainLooper())
        runnable = object : Runnable {
            override fun run() {
                // Fetch the driver's location from the server and draw the route
                fetchOriginAndDrawRoute()

                // Schedule the next update
                handler.postDelayed(this, 5000) // Update every 5 seconds
            }
        }
        handler.post(runnable)
    }

    private fun stopProcess() {
        if (::handler.isInitialized && isRunning) {
            handler.removeCallbacks(runnable)
            isRunning = false
        }
    }

    private fun fetchOriginAndDrawRoute() {
        val userId = "3" // Use stored userId
        val token = "Bearer ${OfflineStorageService.getToken(applicationContext)}"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = NetworkClient.apiService.postRequest(
                    "/api/location/retrieve",
                    mapOf("userId" to userId),
                    token
                )
                if (response.isSuccessful) {
                    val responseData = response.body()
                    val originLat = responseData?.latitude ?: 0.0
                    val originLng = responseData?.longitude ?: 0.0
                    val origin = LatLng(originLat, originLng)

                    // Draw route and update driver's marker on the main thread
                    withContext(Dispatchers.Main) {
                        updateOriginMarker(origin)
                        drawRoute(origin)
                    }
                } else {
                    Log.e(TAG, "Failed to retrieve location: ${response.errorBody()?.string()}")
                }
            } catch (e: HttpException) {
                Log.e(TAG, "HttpException during location retrieval: ${e.message()}", e)
            } catch (e: Exception) {
                Log.e(TAG, "Exception during location retrieval: ${e.message}", e)
            }
        }
    }

    private fun updateOriginMarker(originLatLng: LatLng) {
        if (::mMap.isInitialized) {
            if (originMarker == null) {
                originMarker = mMap.addMarker(
                    MarkerOptions()
                        .position(originLatLng)
                        .title("Driver's Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                )
            } else {
                originMarker?.position = originLatLng
            }

            // Optionally, move the camera to include both markers
            if (destinationMarker != null) {
                val bounds = LatLngBounds.Builder()
                    .include(destinationMarker!!.position)
                    .include(originLatLng)
                    .build()
                val padding = 100 // offset from edges of the map in pixels
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
            }
        } else {
            Toast.makeText(this, "Map is not initialized yet.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun drawRoute(origin: LatLng) {
        val destination = destinationMarker?.position ?: return

        // Use Google Maps Directions API to get the route
        val directionsUrl = getDirectionsUrl(origin, destination)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(directionsUrl).build()
                val response = client.newCall(request).execute()
                val responseData = response.body?.string()
                val jsonResponse = JSONObject(responseData ?: "")
                val routes = jsonResponse.getJSONArray("routes")
                if (routes.length() > 0) {
                    val overviewPolyline = routes.getJSONObject(0).getJSONObject("overview_polyline")
                    val points = overviewPolyline.getString("points")
                    val decodedPath = PolyUtil.decode(points)

                    withContext(Dispatchers.Main) {
                        // Draw the polyline on the map
                        val newPolyline = mMap.addPolyline(
                            PolylineOptions()
                                .addAll(decodedPath)
                                .color(ContextCompat.getColor(this@CustomerActivity, R.color.teal_700))
                                .width(10f)
                        )

                        // Remove the old polyline if exists after the new one is drawn
                        currentPolyline?.remove()
                        currentPolyline = newPolyline
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during route drawing: ${e.message}", e)
            }
        }
    }

    private fun getDirectionsUrl(origin: LatLng, dest: LatLng): String {
        val originStr = "origin=${origin.latitude},${origin.longitude}"
        val destStr = "destination=${dest.latitude},${dest.longitude}"
        return "https://maps.googleapis.com/maps/api/directions/json?$originStr&$destStr&key=$apiKey"
    }
}
