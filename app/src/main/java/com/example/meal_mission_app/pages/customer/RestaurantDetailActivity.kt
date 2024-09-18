package com.example.meal_mission_app.pages.customer

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.meal_mission_app.R
import com.example.meal_mission_app.objects.NetworkClient
import com.example.meal_mission_app.objects.OfflineStorageService
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class RestaurantDetailsActivity : AppCompatActivity() {
    private lateinit var selectedItems: MutableList<Pair<ItemResponse, Int>>
    private lateinit var selectedMeals: MutableList<Pair<MealResponse, Int>>
    private lateinit var totalPriceTextView: TextView

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restaurant_details)

        // Enable the back button in the action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        selectedItems = mutableListOf()
        selectedMeals = mutableListOf()
        totalPriceTextView = findViewById(R.id.totalPriceTextView)  // Assuming this is in your XML

        val restaurantId = intent.getLongExtra("restaurantId", -1L)
        println("Restaurant id: $restaurantId")
        if (restaurantId != -1L) {
            fetchRestaurantDetails(restaurantId)
        }

        val submitOrderButton: Button = findViewById(R.id.submitOrderButton)
        submitOrderButton.setOnClickListener {
            submitOrder()
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchRestaurantDetails(restaurantId: Long) {
        val token = "Bearer ${OfflineStorageService.getToken(this)}"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = NetworkClient.apiService.getRestaurant(
                    data = mapOf("restaurantId" to restaurantId.toString()),
                    authToken = token
                )
                if (response.isSuccessful) {
                    val restaurantDetails = response.body()
                    println("Restaurant details: " + restaurantDetails.toString())
                    withContext(Dispatchers.Main) {
                        if (restaurantDetails != null) {
                            displayRestaurantDetails(restaurantDetails)
                        } else {
                            Toast.makeText(this@RestaurantDetailsActivity, "Failed to load restaurant details", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@RestaurantDetailsActivity, "Failed to fetch restaurant details", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                println("Error: " + e.message)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@RestaurantDetailsActivity, "Error fetching restaurant details", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun displayRestaurantDetails(restaurantDetails: RestaurantDetailResponse) {
        // Assuming TextViews for restaurant details
        findViewById<TextView>(R.id.restaurantName).text = restaurantDetails.name
        findViewById<TextView>(R.id.restaurantDescription).text = restaurantDetails.description

        // Set up RecyclerView for items
        val itemsRecyclerView: RecyclerView = findViewById(R.id.itemsRecyclerView)
        itemsRecyclerView.layoutManager = LinearLayoutManager(this)
        itemsRecyclerView.adapter = ItemAdapter(restaurantDetails.items, { item, quantity ->
            handleItemSelection(item, quantity)
        }, { item ->
            handleItemRemoval(item)
        })

        // Set up RecyclerView for meals
        val mealsRecyclerView: RecyclerView = findViewById(R.id.mealsRecyclerView)
        mealsRecyclerView.layoutManager = LinearLayoutManager(this)
        mealsRecyclerView.adapter = MealAdapter(restaurantDetails.meals, { meal, quantity ->
            handleMealSelection(meal, quantity)
        }, { meal ->
            handleMealRemoval(meal)
        })
    }
    private fun handleMealSelection(meal: MealResponse, quantity: Int) {
        selectedMeals.removeIf { it.first.id == meal.id }  // Remove the meal if it already exists
        selectedMeals.add(Pair(meal, quantity))  // Add the updated quantity
        calculateTotalPrice()
    }

    private fun handleMealRemoval(meal: MealResponse) {
        selectedMeals.removeIf { it.first.id == meal.id }  // Remove the meal from selected meals
        calculateTotalPrice()
    }
    private fun handleItemSelection(item: ItemResponse, quantity: Int) {
        selectedItems.removeIf { it.first.id == item.id }  // Remove the item if it already exists
        selectedItems.add(Pair(item, quantity))  // Add the updated quantity
        calculateTotalPrice()
    }

    private fun handleItemRemoval(item: ItemResponse) {
        selectedItems.removeIf { it.first.id == item.id }  // Remove the item from selected items
        calculateTotalPrice()
    }

    private fun calculateTotalPrice() {
        var totalPrice = 0.0
        selectedItems.forEach { (item, quantity) ->
            totalPrice += item.price * quantity
        }
        selectedMeals.forEach { (meal, quantity) ->
            totalPrice += meal.price * quantity
        }
        totalPriceTextView.text = "Total Price: $$totalPrice"
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun submitOrder() {
        val customerId = OfflineStorageService.getUserId(this)
        val restaurantId = intent.getLongExtra("restaurantId", -1)

        // Adjusting items to match the structure to be passed to CartActivity
        val itemsList = selectedItems.map {
            CartItem(it.first.id, it.second)  // Using CartItem data class
        }

        // Adjusting meals to match the structure to be passed to CartActivity
        val mealsList = selectedMeals.map {
            CartMeal(it.first.id, it.second)  // Using CartMeal data class
        }

        // Create intent to navigate to CartActivity
        val intent = Intent(this, CartActivity::class.java)
        intent.putParcelableArrayListExtra("cartItems", ArrayList(itemsList))  // Pass items list
        intent.putParcelableArrayListExtra("cartMeals", ArrayList(mealsList))  // Pass meals list
        intent.putExtra("restaurantId", restaurantId)  // Pass restaurant ID if needed
        intent.putExtra("customerId", customerId.toString())  // Pass customer ID

        // Start CartActivity
        startActivity(intent)
    }

}

class MealAdapter(
    private val meals: List<MealResponse>,
    private val onQuantityChanged: (MealResponse, Int) -> Unit,
    private val onMealRemoved: (MealResponse) -> Unit
) : RecyclerView.Adapter<MealAdapter.MealViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_meal, parent, false)
        return MealViewHolder(view, onQuantityChanged, onMealRemoved)
    }

    override fun onBindViewHolder(holder: MealViewHolder, position: Int) {
        holder.bind(meals[position])
    }

    override fun getItemCount() = meals.size

    class MealViewHolder(
        view: View,
        private val onQuantityChanged: (MealResponse, Int) -> Unit,
        private val onMealRemoved: (MealResponse) -> Unit
    ) : RecyclerView.ViewHolder(view) {
        private val mealNameTextView: TextView = view.findViewById(R.id.mealName)
        private val mealDescriptionTextView: TextView = view.findViewById(R.id.mealDescription)
        private val mealPriceTextView: TextView = view.findViewById(R.id.mealPrice)
        private val addButton: Button = view.findViewById(R.id.addButton)
        private val quantityLayout: LinearLayout = view.findViewById(R.id.quantityLayout)
        private val decreaseQuantityButton: Button = view.findViewById(R.id.decreaseQuantityButton)
        private val increaseQuantityButton: Button = view.findViewById(R.id.increaseQuantityButton)
        private val mealQuantityText: TextView = view.findViewById(R.id.itemQuantityText)
        private val removeButton: Button = view.findViewById(R.id.removeButton)

        private var quantity: Int = 1

        fun bind(meal: MealResponse) {
            mealNameTextView.text = meal.name
            mealDescriptionTextView.text = meal.description
            mealPriceTextView.text = "${meal.price} USD"

            // Initially show "ADD" button and hide the quantity layout
            addButton.visibility = View.VISIBLE
            quantityLayout.visibility = View.GONE

            // Handle "ADD" button click
            addButton.setOnClickListener {
                addButton.visibility = View.GONE
                quantityLayout.visibility = View.VISIBLE
                onQuantityChanged(meal, quantity)
            }

            // Handle "+" button click
            increaseQuantityButton.setOnClickListener {
                if (quantity < 10) {  // Limit the quantity to 10
                    quantity++
                    mealQuantityText.text = quantity.toString()
                    onQuantityChanged(meal, quantity)
                }
            }

            // Handle "-" button click
            decreaseQuantityButton.setOnClickListener {
                if (quantity > 1) {  // Prevent quantity from going below 1
                    quantity--
                    mealQuantityText.text = quantity.toString()
                    onQuantityChanged(meal, quantity)
                }
            }

            // Handle "REMOVE" button click
            removeButton.setOnClickListener {
                addButton.visibility = View.VISIBLE
                quantityLayout.visibility = View.GONE
                quantity = 1  // Reset quantity to 1
                mealQuantityText.text = quantity.toString()
                onMealRemoved(meal)
            }
        }
    }
}


class ItemAdapter(
    private val items: List<ItemResponse>,
    private val onQuantityChanged: (ItemResponse, Int) -> Unit,
    private val onItemRemoved: (ItemResponse) -> Unit
) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_item, parent, false)
        return ItemViewHolder(view, onQuantityChanged, onItemRemoved)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    class ItemViewHolder(
        view: View,
        private val onQuantityChanged: (ItemResponse, Int) -> Unit,
        private val onItemRemoved: (ItemResponse) -> Unit
    ) : RecyclerView.ViewHolder(view) {
        private val itemNameTextView: TextView = view.findViewById(R.id.itemName)
        private val itemDescriptionTextView: TextView = view.findViewById(R.id.itemDescription)
        private val itemPriceTextView: TextView = view.findViewById(R.id.itemPrice)
        private val addButton: Button = view.findViewById(R.id.addButton)
        private val quantityLayout: LinearLayout = view.findViewById(R.id.quantityLayout)
        private val decreaseQuantityButton: Button = view.findViewById(R.id.decreaseQuantityButton)
        private val increaseQuantityButton: Button = view.findViewById(R.id.increaseQuantityButton)
        private val itemQuantityText: TextView = view.findViewById(R.id.itemQuantityText)
        private val removeButton: Button = view.findViewById(R.id.removeButton)

        private var quantity: Int = 1

        fun bind(item: ItemResponse) {
            itemNameTextView.text = item.name
            itemDescriptionTextView.text = item.description
            itemPriceTextView.text = "${item.price} USD"

            // Initially show "ADD" button and hide the quantity layout
            addButton.visibility = View.VISIBLE
            quantityLayout.visibility = View.GONE

            // Handle "ADD" button click
            addButton.setOnClickListener {
                addButton.visibility = View.GONE
                quantityLayout.visibility = View.VISIBLE
                onQuantityChanged(item, quantity)
            }

            // Handle "+" button click
            increaseQuantityButton.setOnClickListener {
                if (quantity < 10) {  // Limit the quantity to 10
                    quantity++
                    itemQuantityText.text = quantity.toString()
                    onQuantityChanged(item, quantity)
                }
            }

            // Handle "-" button click
            decreaseQuantityButton.setOnClickListener {
                if (quantity > 1) {  // Prevent quantity from going below 1
                    quantity--
                    itemQuantityText.text = quantity.toString()
                    onQuantityChanged(item, quantity)
                }
            }

            // Handle "REMOVE" button click
            removeButton.setOnClickListener {
                addButton.visibility = View.VISIBLE
                quantityLayout.visibility = View.GONE
                quantity = 1  // Reset quantity to 1
                itemQuantityText.text = quantity.toString()
                onItemRemoved(item)
            }
        }
    }
}

data class Cart(
    val offlineId: Long,
    val restaurantName: String,
    val items: List<ItemResponse>,
    val meals: List<MealResponse>
)
data class OfflineCartItem(
    val id: Long,
    val quantity: Int,
    val price: Double,
    val name: String,
    val description: String
    )
data class OfflineCartMeal(
    val id: Long,
    val quantity: Int,
    val price: Double,
    val name: String,
    val description: String
)
data class RestaurantDetailResponse(
    val id: Long,
    val name: String,
    val address: String,
    val phone: String?,
    val email: String?,
    val description: String?,  // Make this nullable
    val items: List<ItemResponse>,
    val meals: List<MealResponse>
)

data class MealResponse(
    val id: Long,
    val name: String,
    val price: Double,
    val description: String,
    val items: List<ItemResponse>
)

data class ItemResponse(
    val id: Long,
    val name: String,
    val description: String,
    val price: Double
)



//    @RequiresApi(Build.VERSION_CODES.O)
//    private fun submitOrder() {
//        val customerId = OfflineStorageService.getUserId(this)
//        val token = "Bearer ${OfflineStorageService.getToken(this)}"
//        val restaurantId = intent.getLongExtra("restaurantId", -1)
//
//        // Adjusting items to match the DTO structure
//        val itemsList = selectedItems.map {
//            mapOf("itemId" to it.first.id, "quantity" to it.second)
//        }
//
//        // Adjusting meals to match the DTO structure
//        val mealsList = selectedMeals.map {
//            mapOf("mealId" to it.first.id, "quantity" to it.second)
//        }
//
//        val orderDetails = mutableMapOf<String, Any>(
//            "restaurantId" to restaurantId,
//            "customerId" to customerId.toString(),
//            "items" to itemsList,
//            "meals" to mealsList
//        )
//
//        // Print the order details to Logcat
//        println("Order Details: ")
//        println("Restaurant ID: $restaurantId")
//        println("Customer ID: $customerId")
//        println("Items: $itemsList")
//        println("Meals: $mealsList")
//        println(orderDetails.toString())
////        val gson = Gson()
////        val json = gson.toJson(orderDetails)  // Properly convert the map to JSON string
////        val requestBody = json.toRequestBody("application/json".toMediaType())
//     //   return;
//        // Post the order details to the API
//        CoroutineScope(Dispatchers.IO).launch {
//            try {
//                val response = NetworkClient.apiService.submitOrder(orderDetails, token)
//                withContext(Dispatchers.Main) {
//                    if (response.isSuccessful) {
//                        Toast.makeText(this@RestaurantDetailsActivity, "Order submitted successfully", Toast.LENGTH_SHORT).show()
//                    } else {
//                        Toast.makeText(this@RestaurantDetailsActivity, "Failed to submit order", Toast.LENGTH_SHORT).show()
//                    }
//                }
//            } catch (e: Exception) {
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(this@RestaurantDetailsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
//                }
//            }
//        }
//    }