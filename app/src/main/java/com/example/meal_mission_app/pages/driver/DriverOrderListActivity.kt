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
    private val orders: MutableList<CustomerLiveOrder> = mutableListOf()

    private var pollingJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_list)  // Reuse the same XML

        recyclerView = findViewById(R.id.recyclerViewOrders)
        recyclerView.layoutManager = LinearLayoutManager(this)
        orderAdapter = OrderAdapter(orders) { orderId ->
            val intent = Intent(this, DriverOrderDetailsActivity::class.java)  // Different activity for driver
            intent.putExtra("ORDER_ID", orderId)
            startActivity(intent)
        }
        recyclerView.adapter = orderAdapter

        startPolling()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startPolling() {
        pollingJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                fetchDriverOrders()  // Different endpoint for driver
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
            println("Error:" + e.message)
        }
    }

    private fun updateOrders(newOrders: List<CustomerLiveOrder>) {
        orders.clear()
        orders.addAll(newOrders)
        orderAdapter.notifyDataSetChanged()
    }

    override fun onStop() {
        super.onStop()
        pollingJob?.cancel()  // Stop polling when the activity is stopped
    }
}
