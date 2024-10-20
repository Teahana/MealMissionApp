package com.example.meal_mission_app.pages.customer

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.meal_mission_app.R
import com.example.meal_mission_app.objects.NetworkClient
import com.example.meal_mission_app.objects.OfflineStorageService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CartActivity : BaseActivity() {

    private lateinit var locationSpinner: Spinner
    private lateinit var locationAdapter: ArrayAdapter<String>
    private lateinit var cartRecyclerView: RecyclerView
    private lateinit var cartAdapter: CartAdapter
    private var userLocations = listOf<UserLocation>()
    private var selectedLocationId: Long? = null
    private var cartList = listOf<Cart>()
    private val successfullySubmittedOrders = mutableListOf<String>()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("CartActivity", "CartActivity created")

        super.onCreate(savedInstanceState)
        layoutInflater.inflate(R.layout.activity_cart, findViewById(R.id.activity_content))

        locationSpinner = findViewById(R.id.locationSpinner)
        cartRecyclerView = findViewById(R.id.cartRecyclerView)
        val submitOrderButton: Button = findViewById(R.id.submitOrderButton)

        // Fetch the list of carts from SharedPreferences
        cartList = OfflineStorageService.getCartList(this)

        if (cartList.isEmpty()) {
            Toast.makeText(this, "No carts found.", Toast.LENGTH_SHORT).show()
            return
        }

        // Set up the RecyclerView
        cartAdapter = CartAdapter(cartList) { cart ->
            OfflineStorageService.removeCartByOfflineId(this, cart.offlineId)
            cartList = OfflineStorageService.getCartList(this) // Update the cart list
            cartAdapter.updateCartList(cartList) // Refresh the adapter's data
        }
        cartRecyclerView.layoutManager = LinearLayoutManager(this)
        cartRecyclerView.adapter = cartAdapter

        // Fetch user locations and populate the spinner
        fetchUserLocations()

        // Set up the submit button
        submitOrderButton.setOnClickListener {
            if (selectedLocationId == null) {
                Toast.makeText(this, "Please select a location.", Toast.LENGTH_SHORT).show()
            } else {
                submitOrders()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun submitOrders() {
        val token = "Bearer ${OfflineStorageService.getToken(this)}"

        GlobalScope.launch(Dispatchers.IO) {
            cartList.forEach { cart ->
                val orderData = mutableMapOf(
                    "restaurantId" to cart.restaurantId.toString(),
                    "customerId" to cart.customerId.toString(),
                    "locationId" to selectedLocationId.toString(),
                    "totalPrice" to cart.totalPrice.toString(),
                    "items" to cart.items.map { mapOf("itemId" to it.id.toString(), "quantity" to it.quantity.toString()) },
                    "meals" to cart.meals.map { mapOf("mealId" to it.id.toString(), "quantity" to it.quantity.toString()) }
                )
                println("Order data below")
                println(orderData)
                try {
                    val response = NetworkClient.apiService.submitOrder(orderData, token)
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            successfullySubmittedOrders.add(cart.offlineId)
                            Toast.makeText(this@CartActivity, "Order ${cart.offlineId} submitted successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@CartActivity, "Failed to submit order: ${cart.offlineId}.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CartActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // After processing all orders, delete the successfully submitted orders
            withContext(Dispatchers.Main) {
                successfullySubmittedOrders.forEach { offlineId ->
                    OfflineStorageService.removeCartByOfflineId(this@CartActivity, offlineId)
                }
                // Refresh the cart list and notify adapter
                cartList = OfflineStorageService.getCartList(this@CartActivity)
                cartAdapter.updateCartList(cartList)
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchUserLocations() {
        val userId = OfflineStorageService.getUserId(this)
        val token = "Bearer ${OfflineStorageService.getToken(this)}"

        val requestData = mapOf(
            "userId" to userId.toString()
        )

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val response = NetworkClient.apiService.getUserLocations(requestData, token)
                if (response.isSuccessful) {
                    response.body()?.let { locations ->
                        userLocations = locations
                        withContext(Dispatchers.Main) {
                            setupLocationSpinner(locations)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CartActivity, "Failed to fetch locations.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CartActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupLocationSpinner(locations: List<UserLocation>) {
        val locationNames = locations.map { "${it.address}, ${it.city}" }
        locationAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, locationNames)
        locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        locationSpinner.adapter = locationAdapter

        locationSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedLocationId = userLocations[position].id
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedLocationId = null
            }
        }
    }

    override fun getSelectedItemId(): Int {
        return R.id.nav_cart  // Ensure the "Cart" icon is highlighted in the bottom nav
    }
}



class CartAdapter(
    private var cartList: List<Cart>,
    private val onDeleteClick: (Cart) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    inner class CartViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val orderIdTextView: TextView = view.findViewById(R.id.orderIdTextView)
        val restaurantNameTextView: TextView = view.findViewById(R.id.restaurantNameTextView)
        val itemsTextView: TextView = view.findViewById(R.id.itemsTextView)
        val mealsTextView: TextView = view.findViewById(R.id.mealsTextView)
        val totalPriceTextView: TextView = view.findViewById(R.id.totalPriceTextView)
        val deleteButton: Button = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.cart_item, parent, false)
        return CartViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val cart = cartList[position]

        holder.orderIdTextView.text = "Order ID: ${cart.offlineId}"
        holder.restaurantNameTextView.text = "Restaurant: ${cart.restaurantName}"

        // Display Items
        val itemsText = cart.items.joinToString("\n") { item ->
            "- ${item.name}: ${item.quantity} x $${item.price}"
        }
        holder.itemsTextView.text = "Items:\n$itemsText"

        // Display Meals
        val mealsText = cart.meals.joinToString("\n") { meal ->
            "- ${meal.name}: ${meal.quantity} x $${meal.price}"
        }
        holder.mealsTextView.text = "Meals:\n$mealsText"

        holder.totalPriceTextView.text = "Total Price: $${cart.totalPrice}"

        // Set up the delete button
        holder.deleteButton.setOnClickListener {
            onDeleteClick(cart)
        }
    }

    override fun getItemCount() = cartList.size

    fun updateCartList(newCartList: List<Cart>) {
        this.cartList = newCartList
        notifyDataSetChanged()
    }
}

