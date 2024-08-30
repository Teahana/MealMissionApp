package com.example.meal_mission_app.pages

import android.Manifest
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
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
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
    private var currentPolyline: Polyline? = null
    private var originMarker: Marker? = null // Keep track of the origin marker
    private var destinationMarker: Marker? = null // Keep track of the destination marker

    private lateinit var apiKey: String

    // FusedLocationProviderClient for getting the customer's GPS location
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Flag to toggle between dummy data and real data
    private val useDummyData = false

    // Dummy origin data
    private val dummyOriginPoints = listOf(
        LatLng(-18.147629, 178.447076),
        LatLng(-18.145983, 178.446858),
        LatLng(-18.143944, 178.448124),
        LatLng(-18.140436, 178.449070),
        LatLng(-18.138677, 178.449131)
    )
    private var currentDummyIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer)

        // Retrieve API key from AndroidManifest.xml
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
        // Check for location permissions and fetch location if granted
        checkLocationPermissionAndFetchLocation()
    }

    private fun checkLocationPermissionAndFetchLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Permission granted, fetch location
            fetchCustomerLocation { customerLocation ->
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(customerLocation, 15f))
                if (destinationMarker == null) {
                    destinationMarker = mMap.addMarker(MarkerOptions().position(customerLocation).title("Your Location"))
                }
            }
        } else {
            // Request permission
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Permission granted, fetch location
                checkLocationPermissionAndFetchLocation()
            } else {
                // Permission denied, show a message to the user
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
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

    private fun startProcess() {
        if (isRunning) return
        isRunning = true

        handler = Handler(Looper.getMainLooper())
        runnable = object : Runnable {
            override fun run() {
                // Fetch the origin from the server or use dummy data based on the flag
                if (useDummyData) {
                    fetchDummyOriginAndDrawRoute(destinationMarker?.position ?: LatLng(0.0, 0.0))
                } else {
                    fetchOriginAndDrawRoute(destinationMarker?.position ?: LatLng(0.0, 0.0))
                }

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

    private fun fetchCustomerLocation(onLocationRetrieved: (LatLng) -> Unit) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                println("Accuracy: " + location.accuracy)
                if (location != null && location.accuracy <= 30) {
                    val customerLatLng = LatLng(location.latitude, location.longitude)
                    // Update the destination marker
                    if (destinationMarker == null) {
                        destinationMarker = mMap.addMarker(MarkerOptions().position(customerLatLng).title("Your Location"))
                    } else {
                        destinationMarker?.position = customerLatLng
                    }
                    onLocationRetrieved(customerLatLng)
                } else {
                    println("Accuracy above 30")
                    // Retry fetching the location after a short delay
                    Handler(Looper.getMainLooper()).postDelayed({
                        fetchCustomerLocation(onLocationRetrieved)
                    }, 2000) // Retry every 2 seconds
                }
            }
        }
    }

    private fun fetchOriginAndDrawRoute(destination: LatLng) {
        val userId = "3" // Use stored userId
        val token = OfflineStorageService.getToken(applicationContext)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = NetworkClient.apiService.postRequest("/api/location/retrieve", mapOf("userId" to userId), "Bearer $token")
                if (response.isSuccessful) {
                    val responseData = response.body()
                    val originLat = responseData?.latitude ?: 0.0
                    val originLng = responseData?.longitude ?: 0.0
                    val origin = LatLng(originLat, originLng)

                    // Draw route on the main thread
                    withContext(Dispatchers.Main) {
                        drawRoute(origin, destination)
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

    private fun fetchDummyOriginAndDrawRoute(destination: LatLng) {
        // Use dummy data for the origin
        val origin = dummyOriginPoints[currentDummyIndex]
        currentDummyIndex = (currentDummyIndex + 1) % dummyOriginPoints.size

        // Draw the route with dummy data
        drawRoute(origin, destination)
    }

    private fun drawRoute(origin: LatLng, destination: LatLng) {
        // Update the origin marker
        if (originMarker == null) {
            originMarker = mMap.addMarker(MarkerOptions().position(origin).title("Origin"))
        } else {
            originMarker?.position = origin
        }

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
                        val newPolyline = mMap.addPolyline(PolylineOptions().addAll(decodedPath))

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
