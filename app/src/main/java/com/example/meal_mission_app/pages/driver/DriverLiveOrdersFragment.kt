package com.example.meal_mission_app.pages.driver

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.meal_mission_app.R
import com.example.meal_mission_app.objects.NetworkClient
import com.example.meal_mission_app.objects.OfflineStorageService
import com.example.meal_mission_app.pages.restaurant.CustomerLiveOrder
import com.example.meal_mission_app.pages.restaurant.OrderAdapter
import com.example.meal_mission_app.services.LocationService
import kotlinx.coroutines.*

class DriverLiveOrdersFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var orderAdapter: OrderAdapter
    private lateinit var locationService: LocationService
    private val orders = mutableListOf<CustomerLiveOrder>()
    private var pollingJob: Job? = null
    private var bestLocation: Location? = null
    private var bestLocationSoFar: Location? = null
    private var retryCount = 0
    private val MAX_RETRIES = 5

    private lateinit var progressBarLoading: ProgressBar
    private lateinit var textViewNoData: TextView

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_orders, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewOrders)
        recyclerView.layoutManager = LinearLayoutManager(context)
        orderAdapter = OrderAdapter(orders) { orderId, orderStatus ->
            val intent = Intent(context, DriverOrderDetailsActivity::class.java)
            intent.putExtra("ORDER_ID", orderId)
            intent.putExtra("ORDER_STATUS", orderStatus)
            startActivity(intent)
        }
        recyclerView.adapter = orderAdapter

        progressBarLoading = view.findViewById(R.id.progressBarLoading)
        textViewNoData = view.findViewById(R.id.textViewNoData)

        locationService = LocationService(requireContext())
        return view
    }

    fun stopPollingExplicitly() {
        pollingJob?.cancel()
    }
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        if (checkLocationPermission()) {
            if (orders.isEmpty()) {
                // Show loading indicator
                progressBarLoading.visibility = View.VISIBLE
                textViewNoData.visibility = View.GONE
                recyclerView.visibility = View.GONE
            }
            startPolling()
        } else {
            requestLocationPermission()
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    fun startPollingExplicitly() {
        if (checkLocationPermission()) {
            startPolling()
        } else {
            requestLocationPermission()
        }
    }

    override fun onPause() {
        super.onPause()
        stopPolling()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startPolling() {
        if (pollingJob == null || pollingJob?.isCancelled == true) {
            pollingJob = CoroutineScope(Dispatchers.Default).launch {
                while (isActive) {
                    getUserLocation()
                    delay(10000) // 10 seconds delay
                }
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        requestPermissions(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            1001
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getUserLocation() {
        locationService.getCurrentLocation { location ->
            if (location != null) {
                bestLocation = location
                if (isVisible && isAdded) {  // Ensure fragment is in a valid state
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                        fetchDriverOrders(location.latitude, location.longitude)
                    }
                }
            } else {
                if (retryCount < MAX_RETRIES) {
                    retryCount++
                    Handler(Looper.getMainLooper()).postDelayed({ getUserLocation() }, 2000)
                } else {
                    bestLocationSoFar?.let {
                        if (isVisible && isAdded) {  // Ensure fragment is in a valid state
                            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                                fetchDriverOrders(it.latitude, it.longitude)
                            }
                        }
                    } ?: run {
                        if (isVisible && isAdded) {  // Ensure fragment is in a valid state
                            Toast.makeText(context, "Unable to find accurate location. Using inaccurate location", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchDriverOrders(latitude: Double, longitude: Double) {
        if (!isAdded) return
        val token = "Bearer ${OfflineStorageService.getToken(requireContext())}"

        val requestData = mapOf(
            "driverLatitude" to latitude.toString(),
            "driverLongitude" to longitude.toString()
        )

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = NetworkClient.apiService.getReadyOrders(requestData, token)
                if (response.isSuccessful) {
                    val newOrders = response.body() ?: emptyList()
                    updateOrders(newOrders)
                } else {
                    withContext(Dispatchers.Main) {
                        handleErrorResponse()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    handleErrorResponse()
                    Toast.makeText(context, "Error fetching driver orders: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private fun handleErrorResponse() {
        progressBarLoading.visibility = View.GONE
        recyclerView.visibility = View.GONE
        if (orders.isEmpty()) {
            textViewNoData.visibility = View.VISIBLE
        }
    }

    private fun updateOrders(newOrders: List<CustomerLiveOrder>) {
        val updatedOrders = newOrders.toMutableList()

        if (updatedOrders != orders) {
            orders.clear()
            orders.addAll(updatedOrders)
            lifecycleScope.launch(Dispatchers.Main) {
                progressBarLoading.visibility = View.GONE
                textViewNoData.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                orderAdapter.notifyDataSetChanged()
            }
        } else {
            lifecycleScope.launch(Dispatchers.Main) {
                progressBarLoading.visibility = View.GONE
                if (orders.isEmpty()) {
                    recyclerView.visibility = View.GONE
                    textViewNoData.visibility = View.VISIBLE
                } else {
                    recyclerView.visibility = View.VISIBLE
                    textViewNoData.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pollingJob?.cancel()
    }
}
