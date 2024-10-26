package com.example.meal_mission_app.pages.customer

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.meal_mission_app.R
import com.example.meal_mission_app.objects.NetworkClient
import com.example.meal_mission_app.objects.OfflineStorageService
import com.example.meal_mission_app.pages.restaurant.CustomerLiveOrder
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CustomerOrdersActivityCustomer : CustomerBaseActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var orderAdapter: CustomerOrderAdapter
    private val liveOrders = mutableListOf<CustomerLiveOrder>()
    private val completedOrders = mutableListOf<CustomerLiveOrder>()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layoutInflater.inflate(R.layout.activity_customer_orders, findViewById(R.id.activity_content))
        tabLayout = findViewById(R.id.tab_layout)
        recyclerView = findViewById(R.id.customerRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize with empty adapter
        orderAdapter = CustomerOrderAdapter(emptyList()) { orderId ->
            onOrderClick(orderId)
        }
        recyclerView.adapter = orderAdapter

        // Handle tab changes
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        // Clear the adapter immediately when switching to live orders
                        clearAdapter()
                        loadLiveOrders()
                    }
                    1 -> {
                        // Clear the adapter immediately when switching to completed orders
                        clearAdapter()
                        loadCompletedOrders()
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Load live orders initially
        loadLiveOrders()
    }

    override fun getSelectedItemId(): Int {
        return R.id.nav_orders // This makes the "Orders" tab selected when in CustomerOrdersActivity
    }

    private fun onOrderClick(orderId: Long) {
        val intent = Intent(this, CustomerOrderDetailsActivity::class.java)
        intent.putExtra("orderId", orderId)
        startActivity(intent)
    }

    private fun clearAdapter() {
        // Clear the adapter and update the RecyclerView with an empty list
        orderAdapter.updateOrders(emptyList())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadLiveOrders() {
        val token = "Bearer ${OfflineStorageService.getToken(this)}"
        val userId = OfflineStorageService.getUserId(this)
        val requestData = mapOf("customerId" to userId.toString())

        // Clear the live orders list before fetching new data
        liveOrders.clear()

        // Fetch live orders from the API
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = NetworkClient.apiService.getCustomerLiveOrders(token, requestData)
                if (response.isSuccessful) {
                    val orders = response.body() ?: emptyList()
                    liveOrders.addAll(orders)

                    runOnUiThread {
                        orderAdapter.updateOrders(liveOrders)
                    }
                }
            } catch (e: Exception) {
                Log.e("CustomerOrdersActivity", "Error fetching live orders", e)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadCompletedOrders() {
        val token = "Bearer ${OfflineStorageService.getToken(this)}"
        val userId = OfflineStorageService.getUserId(this)
        val requestData = mapOf("customerId" to userId.toString())

        // Clear the completed orders list before fetching new data
        completedOrders.clear()

        // Fetch completed orders from the API
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = NetworkClient.apiService.getCustomerCompletedOrders(token, requestData)
                if (response.isSuccessful) {
                    val orders = response.body() ?: emptyList()
                    completedOrders.addAll(orders)

                    runOnUiThread {
                        orderAdapter.updateOrders(completedOrders)
                    }
                }
            } catch (e: Exception) {
                Log.e("CustomerOrdersActivity", "Error fetching completed orders", e)
            }
        }
    }

    // Adapter class inside the same file
    class CustomerOrderAdapter(
        private var orders: List<CustomerLiveOrder>,
        private val onOrderClick: (Long) -> Unit
    ) : RecyclerView.Adapter<CustomerOrderAdapter.OrderViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.customer_order_item, parent, false)
            return OrderViewHolder(view)
        }

        override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
            val order = orders[position]
            holder.bind(order)
        }

        override fun getItemCount(): Int = orders.size

        fun updateOrders(newOrders: List<CustomerLiveOrder>) {
            orders = newOrders
            notifyDataSetChanged()
        }

        inner class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val orderIdTextView: TextView = itemView.findViewById(R.id.text_order_id)
            private val orderStatusTextView: TextView = itemView.findViewById(R.id.text_order_status)

            fun bind(order: CustomerLiveOrder) {
                orderIdTextView.text = "Order ID: ${order.orderId}"
                orderStatusTextView.text = "Status: ${order.orderStatus}"
                itemView.setOnClickListener { onOrderClick(order.orderId) }
            }
        }
    }
}
