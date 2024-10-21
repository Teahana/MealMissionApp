package com.example.meal_mission_app.pages.customer

import com.example.meal_mission_app.R
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.meal_mission_app.DTO.Location
import com.example.meal_mission_app.objects.NetworkClient
import com.example.meal_mission_app.objects.OfflineStorageService
import com.example.meal_mission_app.pages.restaurant.CustomerOrderDto
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import kotlin.properties.Delegates

class CustomerOrderDetailsActivity : AppCompatActivity(), OnMapReadyCallback {

   // private lateinit var authToken: String

    // UI elements
    private lateinit var statusTextView: TextView
    private lateinit var driverNameTextView: TextView
    private lateinit var restaurantNameTextView: TextView
    private lateinit var itemsLayout: LinearLayout
    private lateinit var mealsLayout: LinearLayout
    private lateinit var totalPriceTextView: TextView
    private lateinit var mapContainer: FrameLayout
    private lateinit var mapFragment: SupportMapFragment
    private var orderId by Delegates.notNull<Long>()

    private lateinit var googleMap: GoogleMap
    private var orderDetails: CustomerOrderDto? = null
    private var driverLocationMarker: Marker? = null
    private var polyline: Polyline? = null

    private var pollingHandler: Handler? = null
    private val pollingRunnable = object : Runnable {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun run() {
            fetchDriverLocation()
            pollingHandler?.postDelayed(this, 10000)
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_order_details)

        // Initialize UI elements
        statusTextView = findViewById(R.id.statusTextView)
        driverNameTextView = findViewById(R.id.driverNameTextView)
        restaurantNameTextView = findViewById(R.id.restaurantNameTextView)
        itemsLayout = findViewById(R.id.itemsLayout)
        mealsLayout = findViewById(R.id.mealsLayout)
        totalPriceTextView = findViewById(R.id.totalPriceTextView)
        mapContainer = findViewById(R.id.mapContainer)

        // Initialize the map fragment
        mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Initialize authToken (replace with actual token retrieval)
        //authToken = OfflineStorageService.getToken(this)

        // Fetch order details
        fetchOrderDetails()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchOrderDetails() {
        orderId = intent.getLongExtra("orderId", -1)
        if (orderId == -1L) {
            Toast.makeText(this, "Invalid Order ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch {
            val authToken = OfflineStorageService.getToken(this@CustomerOrderDetailsActivity)
            val requestBody = mapOf("orderId" to orderId.toString())
            try {
                val response = NetworkClient.apiService.getOrderDetails(requestBody, authToken)
                if (response.isSuccessful) {
                    orderDetails = response.body()
                    orderDetails?.let {
                        updateUI(it)
                    }
                } else {
                    Toast.makeText(this@CustomerOrderDetailsActivity, "Failed to fetch order details", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@CustomerOrderDetailsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUI(order: CustomerOrderDto) {
        // Update order information
        statusTextView.text = "Status: ${order.status}"
        restaurantNameTextView.text = "Restaurant: ${order.restaurantName}"
        totalPriceTextView.text = "Total Price: \$${order.totalPrice}"

        // Driver name
        if (order.status == "DELIVERING" || order.status == "DELIVERED") {
            driverNameTextView.visibility = View.VISIBLE
            driverNameTextView.text = "Driver: ${order.driverName}"
        } else {
            driverNameTextView.visibility = View.GONE
        }

        // Update items
        itemsLayout.removeAllViews()
        for (item in order.items) {
            val itemView = TextView(this)
            itemView.text = "${item.itemName} x${item.quantity} - \$${item.itemPrice}"
            itemsLayout.addView(itemView)
        }

        // Update meals
        mealsLayout.removeAllViews()
        for (meal in order.meals) {
            val mealView = TextView(this)
            mealView.text = "${meal.mealName} x${meal.quantity} - \$${meal.mealPrice}"
            mealsLayout.addView(mealView)
        }

        // Handle map visibility and setup based on status
        when (order.status) {
            "DELIVERED" -> {
                // Hide map
                mapContainer.visibility = View.GONE
                stopPolling()
            }
            "PENDING", "ACCEPTED", "READY" -> {
                // Show map with polyline between customer and restaurant
                mapContainer.visibility = View.VISIBLE
                if (::googleMap.isInitialized) {
                    drawRouteBetweenCustomerAndRestaurant(order)
                }
            }
            "DELIVERING" -> {
                // Show map and start polling driver location
                mapContainer.visibility = View.VISIBLE
                if (::googleMap.isInitialized) {
                    startPolling()
                }
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        orderDetails?.let {
            when (it.status) {
                "PENDING", "ACCEPTED", "READY" -> {
                    drawRouteBetweenCustomerAndRestaurant(it)
                }
                "DELIVERING" -> {
                    startPolling()
                }
            }
        }
    }

    private fun drawRouteBetweenCustomerAndRestaurant(order: CustomerOrderDto) {
        val customerLatLng = LatLng(order.latitude, order.longitude)
        val restaurantLatLng = LatLng(order.restaurantLat, order.restaurantLong)

        // Clear existing markers and polylines
        googleMap.clear()

        // Add markers
        googleMap.addMarker(MarkerOptions().position(customerLatLng).title("Customer"))
        googleMap.addMarker(MarkerOptions().position(restaurantLatLng).title("Restaurant"))

        // Move camera
        val bounds = LatLngBounds.Builder()
            .include(customerLatLng)
            .include(restaurantLatLng)
            .build()
        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))

        // Draw polyline along road
        drawPolyline(restaurantLatLng, customerLatLng)
    }

    private fun drawPolyline(startLatLng: LatLng, endLatLng: LatLng) {
        val url = getDirectionsUrl(startLatLng, endLatLng)
        lifecycleScope.launch {
            try {
                val data = downloadUrl(url)
                val result = parseDirectionsJson(data)
                withContext(Dispatchers.Main) {
                    val polylineOptions = PolylineOptions()
                        .addAll(result)
                        .width(5f)
                        .color(Color.BLUE)
                    polyline = googleMap.addPolyline(polylineOptions)
                }
            } catch (e: Exception) {
                // Handle exception
            }
        }
    }

    private fun getDirectionsUrl(origin: LatLng, dest: LatLng): String {
        val originLatLng = "${origin.latitude},${origin.longitude}"
        val destLatLng = "${dest.latitude},${dest.longitude}"
        val apiKey = getString(R.string.google_api_key)
        return "https://maps.googleapis.com/maps/api/directions/json?origin=$originLatLng&destination=$destLatLng&key=$apiKey"
    }

    private suspend fun downloadUrl(strUrl: String): String {
        return withContext(Dispatchers.IO) {
            var data = ""
            try {
                val url = URL(strUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connect()
                val inputStream = conn.inputStream
                val br = BufferedReader(InputStreamReader(inputStream))
                val sb = StringBuilder()
                var line: String?
                while (br.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                data = sb.toString()
                br.close()
                inputStream.close()
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            data
        }
    }

    private fun parseDirectionsJson(jsonData: String): List<LatLng> {
        val result = mutableListOf<LatLng>()
        try {
            val jsonObject = JSONObject(jsonData)
            val routes = jsonObject.getJSONArray("routes")
            if (routes.length() > 0) {
                val legs = routes.getJSONObject(0).getJSONArray("legs")
                val steps = legs.getJSONObject(0).getJSONArray("steps")
                for (i in 0 until steps.length()) {
                    val polyline = steps.getJSONObject(i).getJSONObject("polyline").getString("points")
                    result.addAll(decodePolyline(polyline))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if ((result and 1) != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if ((result and 1) != 0) (result shr 1).inv() else result shr 1
            lng += dlng
            val p = LatLng(lat / 1E5, lng / 1E5)
            poly.add(p)
        }
        return poly
    }

    private fun startPolling() {
        pollingHandler = Handler(Looper.getMainLooper())
        pollingHandler?.post(pollingRunnable)
    }

    private fun stopPolling() {
        pollingHandler?.removeCallbacks(pollingRunnable)
        pollingHandler = null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchDriverLocation() {
        orderDetails?.let { order ->
            val requestBody = mapOf(
                "driverId" to order.driverId.toString(),
                "orderId" to order.orderId.toString()
            )
            val authToken = OfflineStorageService.getToken(this@CustomerOrderDetailsActivity)
            lifecycleScope.launch {
                try {
                    val response = NetworkClient.apiService.getDriverLocation(authToken, requestBody)
                    if (response.isSuccessful) {
                        val driverLocation = response.body()
                        driverLocation?.let {
                            // Check accuracy
                            if (it.accuracy <= 50) {
                                updateDriverLocationOnMap(it)
                            }
                            // Check orderStatus
                            if (it.orderStatus == "DELIVERED") {
                                // Switch UI to DELIVERED status
                                stopPolling()
                                order.status = "DELIVERED"
                                runOnUiThread {
                                    updateUI(order)
                                }
                            }
                        }
                    } else {
                        // Handle error
                    }
                } catch (e: Exception) {
                    // Handle exception
                }
            }
        }
    }

    private fun updateDriverLocationOnMap(location: Location) {
        val driverLatLng = LatLng(location.latitude, location.longitude)
        val customerLatLng = LatLng(orderDetails!!.latitude, orderDetails!!.longitude)

        runOnUiThread {
            // Remove previous marker if exists
            driverLocationMarker?.remove()
            // Add driver marker
            driverLocationMarker = googleMap.addMarker(MarkerOptions().position(driverLatLng).title("Driver"))

            // Remove previous polyline
            polyline?.remove()

            // Draw polyline between driver and customer
            drawPolyline(driverLatLng, customerLatLng)

            // Move camera to include both markers
            val bounds = LatLngBounds.Builder()
                .include(driverLatLng)
                .include(customerLatLng)
                .build()
            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPolling()
    }
}




