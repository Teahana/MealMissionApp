package com.example.meal_mission_app.pages.restaurant

import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.meal_mission_app.R
import com.example.meal_mission_app.helper.OrderStatus
import com.example.meal_mission_app.objects.NetworkClient
import com.example.meal_mission_app.objects.OfflineStorageService
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime

class OrderDetailsActivity : AppCompatActivity() {
    private lateinit var textViewOrderId: TextView
    private lateinit var textViewOrderStatus: TextView
    private lateinit var textViewOrderDateTime: TextView
    private lateinit var textViewCustomerInfo: TextView
    private lateinit var textViewMealPrice: TextView
    private lateinit var textViewOrderItems: TextView
    private lateinit var textViewOrderMeals: TextView
    private lateinit var buttonAcceptOrder: Button
    private lateinit var buttonOrderReady: Button

    private var orderId: Long = -1

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_details)
        initializeViews()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        orderId = intent.getLongExtra("ORDER_ID", -1L)
        val orderStatus = intent.getStringExtra("ORDER_STATUS")

        // Set the initial state of the buttons based on the order status
        updateButtonState(orderStatus ?: "PENDING")

        if (orderId != -1L) {
            lifecycleScope.launch {
                fetchOrderDetails(orderId)
            }
        } else {
            Toast.makeText(this, "Invalid order ID", Toast.LENGTH_SHORT).show()
            finish()
        }

        buttonAcceptOrder.setOnClickListener {
            lifecycleScope.launch {
                updateOrderStatusAccept(orderId)
            }
        }
        buttonOrderReady.setOnClickListener {
            lifecycleScope.launch {
                updateOrderStatusReady(orderId)
            }
        }
    }
    // Handle back button press
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Navigate back to the previous screen
                finish()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
    private fun initializeViews() {
        textViewOrderId = findViewById(R.id.textViewOrderId)
        textViewOrderStatus = findViewById(R.id.textViewOrderStatus)
        textViewOrderDateTime = findViewById(R.id.textViewOrderDateTime)
        textViewCustomerInfo = findViewById(R.id.textViewCustomerInfo)
        textViewMealPrice = findViewById(R.id.textViewMealPrice)
        textViewOrderItems = findViewById(R.id.textViewOrderItems)
        textViewOrderMeals = findViewById(R.id.textViewOrderMeals)
        buttonAcceptOrder = findViewById(R.id.buttonAcceptOrder)
        buttonOrderReady = findViewById(R.id.buttonOrderReady)
    }

    // Method to update the button states based on order status
    private fun updateButtonState(orderStatus: String) {
        when (orderStatus) {
            "PENDING" -> {
                buttonAcceptOrder.isEnabled = true
                buttonOrderReady.isEnabled = false
            }
            "ACCEPTED" -> {
                buttonAcceptOrder.isEnabled = false
                buttonOrderReady.isEnabled = true
            }
            "READY" -> {
                buttonAcceptOrder.isEnabled = false
                buttonOrderReady.isEnabled = false
            }
            else -> {
                buttonAcceptOrder.isEnabled = false
                buttonOrderReady.isEnabled = false
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun fetchOrderDetails(orderId: Long) {
        val token = "Bearer ${OfflineStorageService.getToken(this)}"
        val requestBody = HashMap<String, String?>()
        requestBody.put("orderId", orderId.toString())
        try {
            val response = NetworkClient.apiService.getOrderDetails(requestBody, token)
            if (response.isSuccessful) {
                val orderDetails = response.body()
                orderDetails?.let {
                    withContext(Dispatchers.Main) {
                        displayOrderDetails(it)
                        updateButtonState(it.status) // Update buttons after fetching order details
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@OrderDetailsActivity, "Failed to fetch order details", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@OrderDetailsActivity, "Error fetching order details: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayOrderDetails(orderDetails: CustomerOrderDto) {
        textViewOrderId.text = "Order #${orderDetails.orderId}"
        textViewOrderStatus.text = orderDetails.status
        textViewOrderDateTime.text = "${orderDetails.orderDate} ${orderDetails.orderTime}"
        textViewCustomerInfo.text = "Customer: ${orderDetails.customerName}"
        textViewMealPrice.text = "Meal Price: $${orderDetails.totalPrice}"
        textViewOrderItems.text = orderDetails.items.joinToString("\n") {
            "${it.quantity}x ${it.itemName} ($${it.itemPrice})"
        }
        textViewOrderMeals.text = orderDetails.meals.joinToString("\n") {
            "${it.quantity}x ${it.mealName} ($${it.mealPrice})"
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun updateOrderStatusAccept(orderId: Long) {
        val token = "Bearer ${OfflineStorageService.getToken(this)}"
        val requestBody = JsonObject().apply {
            addProperty("orderId", orderId)
        }

        try {
            val response = NetworkClient.apiService.updateOrderStatus(requestBody, token)
            if (response.isSuccessful) {
                val statusUpdateResponse = response.body()
                if (statusUpdateResponse != null) {
                    withContext(Dispatchers.Main) {
                        when (OrderStatus.valueOf(statusUpdateResponse.status)) {
                            OrderStatus.ACCEPTED -> {
                                textViewOrderStatus.text = "Order status: ${statusUpdateResponse.statusMessage}"
                                Toast.makeText(this@OrderDetailsActivity, "Order status updated: ${statusUpdateResponse.status}", Toast.LENGTH_SHORT).show()
                                buttonAcceptOrder.isEnabled = false
                                buttonOrderReady.isEnabled = true
                            }
                            OrderStatus.DUPLICATE -> {
                                textViewOrderStatus.text = "Order status: ${statusUpdateResponse.statusMessage}"
                                Toast.makeText(this@OrderDetailsActivity, "Order already accepted", Toast.LENGTH_SHORT).show()
                                buttonAcceptOrder.isEnabled = false
                            }
                            else -> {
                                Toast.makeText(this@OrderDetailsActivity, "Unknown status: ${statusUpdateResponse.status}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@OrderDetailsActivity, "Failed to update order status", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@OrderDetailsActivity, "Error updating order status: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun updateOrderStatusReady(orderId: Long) {
        val token = "Bearer ${OfflineStorageService.getToken(this)}"
        val requestBody = JsonObject().apply {
            addProperty("orderId", orderId)
        }

        try {
            val response = NetworkClient.apiService.updateOrderStatusReady(requestBody, token)
            if (response.isSuccessful) {
                val statusUpdateResponse = response.body()
                if (statusUpdateResponse != null) {
                    withContext(Dispatchers.Main) {
                        textViewOrderStatus.text = statusUpdateResponse.statusMessage
                        Toast.makeText(this@OrderDetailsActivity, "Order is ready: ${statusUpdateResponse.status}", Toast.LENGTH_SHORT).show()
                        buttonOrderReady.isEnabled = false
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@OrderDetailsActivity, "Failed to mark order as ready", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@OrderDetailsActivity, "Error marking order as ready: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}


data class CustomerOrderDto(
    val orderId: Long,
    val customerId: Long,
    val totalPrice: Double,
    val customerName: String,
    val restaurantId: Long,
   // val driverId: Long,
    val orderDate: LocalDate,
    val orderTime: LocalTime,
    val status: String,
    val items: List<CustomerOrderItemDto>,
    val meals: List<CustomerOrderMealDto>
)

data class CustomerOrderItemDto(
    val itemId: Long,
    val itemName: String,
    val itemPrice: Double,
    val quantity: Int
)

data class CustomerOrderMealDto(
    val mealId: Long,
    val mealName: String,
    val mealPrice: Double,
    val quantity: Int
)
data class StatusUpdateResponse(
    val status: String,
    val statusMessage: String
)