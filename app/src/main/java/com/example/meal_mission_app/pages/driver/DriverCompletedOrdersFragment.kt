package com.example.meal_mission_app.pages.driver

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.meal_mission_app.R
import com.example.meal_mission_app.objects.NetworkClient
import com.example.meal_mission_app.objects.OfflineStorageService
import com.example.meal_mission_app.pages.restaurant.CustomerLiveOrder
import com.example.meal_mission_app.pages.restaurant.OrderAdapter
import kotlinx.coroutines.*

class DriverCompletedOrdersFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var orderAdapter: OrderAdapter
    private val orders = mutableListOf<CustomerLiveOrder>()

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

        return view
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        fetchCompletedOrders()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchCompletedOrders() {
        CoroutineScope(Dispatchers.Default).launch {
            val token = "Bearer ${OfflineStorageService.getToken(requireContext())}"
            val driverId = OfflineStorageService.getUserId(requireContext())
            val requestBody = mapOf("driverId" to driverId.toString())
            try {
                val response = NetworkClient.apiService.getDriverCompletedOrders(requestBody,token)
                if (response.isSuccessful) {
                    val newOrders = response.body() ?: emptyList()
                    updateOrders(newOrders)
                }
                else if(response.code() == 404){
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "No completed orders found", Toast.LENGTH_SHORT).show()
                    }
                }

                else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to fetch completed orders", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error fetching completed orders: ${e.message}", Toast.LENGTH_SHORT).show()
                }
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
}
