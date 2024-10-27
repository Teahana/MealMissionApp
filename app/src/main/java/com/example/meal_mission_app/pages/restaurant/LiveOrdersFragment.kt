package com.example.meal_mission_app.pages.restaurant

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.meal_mission_app.R
import com.example.meal_mission_app.objects.NetworkClient
import com.example.meal_mission_app.objects.OfflineStorageService
import kotlinx.coroutines.*

class LiveOrdersFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var orderAdapter: OrderAdapter
    private val orders = mutableListOf<CustomerLiveOrder>()
    private var pollingJob: Job? = null
    var counter = 0

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_orders, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewOrders)
        recyclerView.layoutManager = LinearLayoutManager(context)
        orderAdapter = OrderAdapter(orders) { orderId, orderStatus ->
            val intent = Intent(context, RestaurantOrderDetailsActivity::class.java)
            intent.putExtra("ORDER_ID", orderId)
            intent.putExtra("ORDER_STATUS", orderStatus)
            startActivity(intent)
        }
        recyclerView.adapter = orderAdapter

        return view
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        startPolling()
    }

    override fun onPause() {
        super.onPause()
        stopPolling()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startPolling() {
        if (pollingJob == null || pollingJob?.isCancelled == true) {
            pollingJob = viewLifecycleOwner.lifecycleScope.launch {
                while (isActive) {
                    fetchLiveOrders()
                    delay(10000) // 10 seconds delay
                }
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun fetchLiveOrders() {
        if (!isVisible || !isAdded) return // Check if the fragment is visible and attached
        val token = "Bearer ${OfflineStorageService.getToken(requireContext())}"
        val restaurantId = OfflineStorageService.getRestaurantId(requireContext())
        val requestBody = mapOf("restaurantId" to restaurantId.toString())

        try {
            val response = NetworkClient.apiService.getRestaurantLiveOrders(token, requestBody)
            if (response.isSuccessful) {
                counter = 0
                val newOrders = response.body() ?: emptyList()
                updateOrders(newOrders)
            } else if (response.code() == 404) {
                updateOrders(response.body() ?: emptyList())
            } else {
                counter++
                if (counter >= 30) {
                    withContext(Dispatchers.Main) {
                        if (counter >= 30 && isVisible && isAdded) { // Check if the fragment is still in a valid state
                            Toast.makeText(context, "Failed to fetch live orders", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                if (isVisible && isAdded) { // Check if the fragment is still in a valid state
                    Toast.makeText(context, "Error fetching live orders: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            println("Error: ${e.message}")
        }
    }

    private fun updateOrders(newOrders: List<CustomerLiveOrder>) {
        val updatedOrders = newOrders.toMutableList()

        if (updatedOrders != orders) {
            orders.clear()
            orders.addAll(updatedOrders)
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                if (isVisible && isAdded) { // Ensure the fragment is in a valid state before updating UI
                    orderAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pollingJob?.cancel()
    }
}
