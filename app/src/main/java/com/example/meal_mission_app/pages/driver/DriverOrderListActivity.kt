package com.example.meal_mission_app.pages.driver

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.meal_mission_app.R
import com.example.meal_mission_app.objects.NetworkClient
import com.example.meal_mission_app.objects.OfflineStorageService
import com.example.meal_mission_app.pages.restaurant.CustomerLiveOrder
import com.example.meal_mission_app.pages.restaurant.OrderAdapter
import kotlinx.coroutines.*

class DriverOrderListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var orderAdapter: OrderAdapter
    private val orders = mutableListOf<CustomerLiveOrder>()
    private var pollingJob: Job? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_list)  // Reuse the same XML

        recyclerView = findViewById(R.id.recyclerViewOrders)
        recyclerView.layoutManager = LinearLayoutManager(this)
        orderAdapter = OrderAdapter(orders) { orderId, orderStatus ->
            val intent = Intent(this, DriverOrderDetailsActivity::class.java)  // Different activity for driver
            intent.putExtra("ORDER_ID", orderId)
            intent.putExtra("ORDER_STATUS", orderStatus)
            startActivity(intent)
        }
        recyclerView.adapter = orderAdapter

        startPolling()
    }

    override fun onStop() {
        super.onStop()
        pollingJob?.cancel()  // Cancel the polling when the app goes to the background
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        startPolling()  // Restart the polling when the app comes back to the foreground
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startPolling() {
        pollingJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                fetchDriverOrders()  // Fetch from the driver-specific endpoint
                delay(10000)  // Polling every 10 seconds
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun fetchDriverOrders() {
        val token = "Bearer ${OfflineStorageService.getToken(this)}"

        try {
            val response = NetworkClient.apiService.getReadyOrders(token)  // Call the driver-specific endpoint
            if (response.isSuccessful) {
                val newOrders = response.body() ?: emptyList()
                withContext(Dispatchers.Main) {
                    updateOrders(newOrders)
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DriverOrderListActivity, "Failed to fetch driver orders", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@DriverOrderListActivity, "Error fetching driver orders: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            println("Error:" + e.message)
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

    override fun onDestroy() {
        super.onDestroy()
        pollingJob?.cancel()
    }
}
