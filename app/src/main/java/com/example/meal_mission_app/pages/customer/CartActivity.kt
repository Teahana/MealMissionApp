package com.example.meal_mission_app.pages.customer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.meal_mission_app.R
import com.example.meal_mission_app.objects.OfflineStorageService
import com.example.meal_mission_app.pages.BaseActivity
import com.example.meal_mission_app.services.LocationService
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class CartActivity : BaseActivity(), OnMapReadyCallback {

    private lateinit var recyclerView: RecyclerView
    private lateinit var cartAdapter: CartAdapter
    private val cartItems: MutableList<CartItem> = mutableListOf()
    private val cartMeals: MutableList<CartMeal> = mutableListOf()
    private lateinit var map: GoogleMap
    private var userLocation: LatLng? = null
    private lateinit var locationService: LocationService
    private val LOCATION_PERMISSION_REQUEST_CODE = 100
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        recyclerView = findViewById(R.id.recyclerViewCart)
        recyclerView.layoutManager = LinearLayoutManager(this)
        // Enable back arrow in action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // Initialize adapter with both cart items and meals
        cartAdapter = CartAdapter(cartItems, cartMeals)
        recyclerView.adapter = cartAdapter

        locationService = LocationService(this)

        // Initialize the map
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Load cart items and meals from intent or shared preferences
        loadCartItemsAndMeals()

        // Proceed to payment
        findViewById<Button>(R.id.btnProceedToPayment).setOnClickListener {
            val intent = Intent(this, PaymentActivity::class.java)
            // Pass any necessary data to PaymentActivity
            startActivity(intent)
        }

        // Check location permissions and fetch location if granted
        if (checkLocationPermissions()) {
            fetchUserLocation()
        } else {
            requestLocationPermissions()
        }
    }

    // Handle back arrow click in the action bar
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Finish the current activity and go back to the previous one
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    // Check if location permissions are granted
    private fun checkLocationPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Request location permissions if not granted
    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    // Handle the result of the permission request
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted, fetch the user's location
                println("Permission granted")
                fetchUserLocation()
            } else {
                println("Permissionn was denied")
                // Permission was denied, handle accordingly
                // You can show a message to the user explaining why the app needs the permission
            }
        }
    }

    // Fetch user's location from GPS using LocationService
    private fun fetchUserLocation() {
        locationService.getCurrentLocation { location: Location? ->
            if (location != null) {
                userLocation = LatLng(location.latitude, location.longitude)
                updateMapLocation(userLocation!!)
            } else {
                // Handle location error (e.g., show a message to the user)
            }
        }
    }

    private fun loadCartItemsAndMeals() {
        // Retrieve cart items and meals from the Intent
        val itemsList = intent.getParcelableArrayListExtra<CartItem>("cartItems") ?: arrayListOf()
        val mealsList = intent.getParcelableArrayListExtra<CartMeal>("cartMeals") ?: arrayListOf()

        // Clear the existing cart data and add new items/meals
        cartItems.clear()
        cartItems.addAll(itemsList)

        cartMeals.clear()
        cartMeals.addAll(mealsList)

        // Notify adapter of data changes
        cartAdapter.notifyDataSetChanged()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // If location is available, update the map
        userLocation?.let {
            updateMapLocation(it)
        }
    }

    // Update the map with the current user location
    private fun updateMapLocation(location: LatLng) {
        map.clear()
        map.addMarker(MarkerOptions().position(location).title("Your Location"))
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
    }
}
class CartAdapter(
    private val cartItems: List<CartItem>,
    private val cartMeals: List<CartMeal>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_ITEM = 0
        private const val TYPE_MEAL = 1
    }

    class CartItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemName: TextView = itemView.findViewById(R.id.textViewItemName)
        val itemQuantity: TextView = itemView.findViewById(R.id.textViewItemQuantity)
        val itemPrice: TextView = itemView.findViewById(R.id.textViewItemPrice)
    }

    class CartMealViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mealName: TextView = itemView.findViewById(R.id.textViewMealName)
        val mealQuantity: TextView = itemView.findViewById(R.id.textViewMealQuantity)
        val mealPrice: TextView = itemView.findViewById(R.id.textViewMealPrice)
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < cartItems.size) TYPE_ITEM else TYPE_MEAL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_ITEM) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cart, parent, false)
            CartItemViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.meal_cart, parent, false)
            CartMealViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == TYPE_ITEM) {
            val item = cartItems[position]
            val itemHolder = holder as CartItemViewHolder
            itemHolder.itemName.text = "Item ID: ${item.itemId}" // Modify to show actual item name if available
            itemHolder.itemQuantity.text = "Quantity: ${item.quantity}"
            itemHolder.itemPrice.text = "Price: \$${item.quantity * 10.0}" // Replace with actual price logic
        } else {
            val meal = cartMeals[position - cartItems.size]
            val mealHolder = holder as CartMealViewHolder
            mealHolder.mealName.text = "Meal ID: ${meal.mealId}" // Modify to show actual meal name if available
            mealHolder.mealQuantity.text = "Quantity: ${meal.quantity}"
            mealHolder.mealPrice.text = "Price: \$${meal.quantity * 15.0}" // Replace with actual price logic
        }
    }

    override fun getItemCount(): Int {
        return cartItems.size + cartMeals.size
    }
}

// Sample data class for cart items
data class CartItem(val itemId: Long, val quantity: Int) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(itemId)
        parcel.writeInt(quantity)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<CartItem> {
        override fun createFromParcel(parcel: Parcel): CartItem {
            return CartItem(parcel)
        }

        override fun newArray(size: Int): Array<CartItem?> {
            return arrayOfNulls(size)
        }
    }
}

data class CartMeal(val mealId: Long, val quantity: Int) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(mealId)
        parcel.writeInt(quantity)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<CartMeal> {
        override fun createFromParcel(parcel: Parcel): CartMeal {
            return CartMeal(parcel)
        }

        override fun newArray(size: Int): Array<CartMeal?> {
            return arrayOfNulls(size)
        }
    }
}
