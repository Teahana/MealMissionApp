package com.example.meal_mission_app.pages.restaurant

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
import kotlinx.coroutines.*

class CompletedOrdersFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var orderAdapter: OrderAdapter
    private val orders = mutableListOf<CustomerLiveOrder>()
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
        fetchCompletedOrders()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchCompletedOrders() {
        CoroutineScope(Dispatchers.Default).launch {
            val token = "Bearer ${OfflineStorageService.getToken(requireContext())}"
            val restaurantId = OfflineStorageService.getRestaurantId(requireContext())
            val requestBody = mapOf("restaurantId" to restaurantId.toString())
            try {
                val response = NetworkClient.apiService.getRestaurantCompletedOrders(token, requestBody)
                if (response.isSuccessful) {
                    val newOrders = response.body() ?: emptyList()
                    updateOrders(newOrders)
                } else if (response.code() == 404) {
                    updateOrders(response.body() ?: emptyList())
                } else {
                    withContext(Dispatchers.Main) {
                        counter++
                        if (counter >= 30) {
                            Toast.makeText(context, "Failed to fetch completed orders", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error fetching completed orders: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                println("Error:" + e.message)
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
