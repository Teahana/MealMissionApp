package com.example.meal_mission_app.pages

import android.Manifest
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.meal_mission_app.R
import com.example.meal_mission_app.services.LocationService
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.Task
import com.google.maps.android.PolyUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import retrofit2.HttpException
import com.example.meal_mission_app.objects.NetworkClient
import com.example.meal_mission_app.objects.OfflineStorageService

class CustomerActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        private const val REQUEST_LOCATION_PERMISSIONS_CODE = 1001
        private const val REQUEST_CHECK_SETTINGS = 1002
        private const val MAX_RETRIES = 5
    }

    private lateinit var mMap: GoogleMap
    private var retryCount = 0
   // private lateinit var backgroundService: BackgroundService
    private var destinationMarker: Marker? = null // Customer's location
    private var originMarker: Marker? = null // Driver's location
    private var currentPolyline: Polyline? = null // Route polyline
    private lateinit var apiKey: String
    private lateinit var locationService: LocationService
    private var handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer)

        apiKey = getApiKeyFromManifest()
        //backgroundService = BackgroundService(this)
        locationService = LocationService(this)

        val startProcessButton: Button = findViewById(R.id.getDriverLocationButton)
        val cancelProcessButton: Button = findViewById(R.id.cancelProcessButton)

        startProcessButton.setOnClickListener {
            startFetchingDriverLocation()
        }

        cancelProcessButton.setOnClickListener {
            stopFetchingDriverLocation()
        }

        // Initialize the map
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        if (checkLocationPermission()) {
            checkLocationSettings()
        } else {
            requestLocationPermission()
        }
    }

    private fun checkLocationSettings() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L
        ).apply {
            setMinUpdateIntervalMillis(5000L)
        }.build()

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true)

        val settingsClient: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = settingsClient.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            getUserLocation() // Proceed to fetch the user location if settings are enabled
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            // Check again if location settings are correct and start location fetching if so
            if (checkLocationPermission()) {
                Log.d(TAG, "Location settings changed, trying to fetch user location again.")
                getUserLocation()
            } else {
                requestLocationPermission()
            }
        }
    }

    private fun getUserLocation() {
        Log.d(TAG, "Fetching user location, retry count: $retryCount")
        fetchCurrentLocation { location ->
            if (location != null) {
                Log.d(TAG, "Location fetched: ${location.latitude}, ${location.longitude}, accuracy: ${location.accuracy}")
                if (location.accuracy <= 30) {
                    val customerLatLng = LatLng(location.latitude, location.longitude)
                    updateDestinationMarker(customerLatLng)
                } else if (retryCount < MAX_RETRIES) {
                    retryCount++
                    Log.d(TAG, "Location accuracy too low, retrying... Retry count: $retryCount")
                    handler.postDelayed({ getUserLocation() }, 2000)
                } else {
                    Log.d(TAG, "Max retries reached, stopping location fetch.")
                    Toast.makeText(this, "Unable to get accurate location. Please try again later.", Toast.LENGTH_LONG).show()
                }
            } else {
                Log.d(TAG, "Location fetch failed, retrying... Retry count: $retryCount")
                if (retryCount < MAX_RETRIES) {
                    retryCount++
                    handler.postDelayed({ getUserLocation() }, 2000)
                } else {
                    Log.d(TAG, "Max retries reached, stopping location fetch.")
                    Toast.makeText(this, "Unable to get accurate location. Please try again later.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun fetchCurrentLocation(callback: (Location?) -> Unit) {
        locationService.getCurrentLocation { location ->
            callback(location)
        }
    }

    private fun updateDestinationMarker(customerLatLng: LatLng) {
        if (::mMap.isInitialized) {
            Log.d(TAG, "Updating destination marker on the map")
            mMap.clear() // Clear previous markers if any
            destinationMarker = mMap.addMarker(
                MarkerOptions()
                    .position(customerLatLng)
                    .title("Your Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(customerLatLng, 15f))
        }
    }

    private fun startFetchingDriverLocation() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                Log.d(TAG, "Fetching driver's location from server")
                fetchOriginAndDrawRoute()
                handler.postDelayed(this, 5000) // Fetch every 5 seconds
            }
        }, 0)
    }

    private fun stopFetchingDriverLocation() {
        Log.d(TAG, "Stopping driver location fetching")
        handler.removeCallbacksAndMessages(null) // Stops the fetching process
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
                        Log.d(TAG, "Driver location fetched: $originLat, $originLng")
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
            Log.d(TAG, "Updating driver's location marker on the map")
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

            // move the camera to include both markers
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
                        Log.d(TAG, "Drawing route on the map")
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSIONS_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkLocationSettings()
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        stopFetchingDriverLocation()
        super.onDestroy()
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
}
