package com.example.meal_mission_app.pages.restaurant

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.meal_mission_app.R
import com.example.meal_mission_app.objects.NetworkClient
import com.example.meal_mission_app.objects.OfflineStorageService
import kotlinx.coroutines.*
import java.sql.SQLOutput
import java.time.LocalDate
import java.time.LocalTime

data class CustomerOrderResponse(
    val orders: List<CustomerOrderDto>
)
data class CustomerLiveOrder(
    val orderId: Long,
    val orderStatus: String,
    val orderDate: LocalDate,
    val orderTime: LocalTime
)

class OrderListActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var orderAdapter: OrderAdapter
    private val orders = mutableListOf<CustomerLiveOrder>()
    private var pollingJob: Job? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_list)

        recyclerView = findViewById(R.id.recyclerViewOrders)
        recyclerView.layoutManager = LinearLayoutManager(this)
        orderAdapter = OrderAdapter(orders) { orderId, orderStatus ->
            val intent = Intent(this, OrderDetailsActivity::class.java)
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
                fetchLiveOrders()
                delay(10000) // 10 seconds delay
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun fetchLiveOrders() {
        val token = "Bearer ${OfflineStorageService.getToken(this)}"
        val requestBody = emptyMap<String, String?>()
        try {
            val response = NetworkClient.apiService.getLiveOrders(token,requestBody)
            if (response.isSuccessful) {
                val newOrders = response.body() ?: emptyList()
                updateOrders(newOrders)
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@OrderListActivity, "Failed to fetch live orders", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@OrderListActivity, "Error fetching live orders: ${e.message}", Toast.LENGTH_SHORT).show()
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

class OrderAdapter(
    private val orders: List<CustomerLiveOrder>,
    private val onItemClick: (Long, String) -> Unit
) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    class OrderViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val textViewOrderId: TextView = itemView.findViewById(R.id.textViewOrderId)
        val textViewOrderStatus: TextView = itemView.findViewById(R.id.textViewOrderStatus)
        val textViewOrderDateTime: TextView = itemView.findViewById(R.id.textViewOrderDateTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]
        holder.textViewOrderId.text = "Order #${order.orderId}"
        holder.textViewOrderStatus.text = order.orderStatus
        holder.textViewOrderDateTime.text = "${order.orderDate} ${order.orderTime}"
        holder.itemView.setOnClickListener {
            onItemClick(order.orderId, order.orderStatus)
        }
    }

    override fun getItemCount() = orders.size
}