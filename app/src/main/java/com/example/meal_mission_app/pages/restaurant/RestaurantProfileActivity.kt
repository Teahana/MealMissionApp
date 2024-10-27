package com.example.meal_mission_app.pages.restaurant

import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.meal_mission_app.DTO.Restaurant
import com.example.meal_mission_app.R
import com.example.meal_mission_app.objects.NetworkClient
import com.example.meal_mission_app.objects.OfflineStorageService
import com.example.meal_mission_app.pages.customer.AddressFragment
import com.squareup.picasso.Picasso // Use Picasso to load images
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch



class RestaurantProfileActivity : RestaurantBaseActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var imageViewRestaurantLogo: ImageView

    private lateinit var restaurant: Restaurant
    private val authToken: String by lazy { "Bearer ${OfflineStorageService.getToken(this)}" }
    private val restaurantId: Long by lazy {
        OfflineStorageService.getRestaurantId(this)?.toLongOrNull() ?: 0L
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate your specific layout
        val inflater = layoutInflater
        val contentView = inflater.inflate(R.layout.restaurant_activity_profile, null)

        // Add your contentView to the activity_content FrameLayout
        val contentFrame = findViewById<FrameLayout>(R.id.activity_content)
        contentFrame.addView(contentView)

        // Initialize your views using contentView
        tabLayout = contentView.findViewById(R.id.tabLayout)
        viewPager = contentView.findViewById(R.id.viewPager)
        imageViewRestaurantLogo = contentView.findViewById(R.id.imageViewRestaurantLogo)

        setupTabs()
        fetchRestaurantDetails()
    }

    override fun getSelectedItemId(): Int {
        return R.id.nav_restaurant_profile
    }

    private fun setupTabs() {
        val adapter = RestaurantProfilePagerAdapter(this)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Meals"
                1 -> "Items"
                2 -> "Info"
                else -> "Meals"
            }
        }.attach()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchRestaurantDetails() {
        lifecycleScope.launch {
            try {
                val response = NetworkClient.apiService.getRestaurantDetails(
                    mapOf("restaurantId" to restaurantId.toString()),
                    authToken
                )
                if (response.isSuccessful) {
                    restaurant = response.body()!!
                    populateRestaurantDetails()
                } else {
                    showToast("Failed to fetch restaurant details")
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun populateRestaurantDetails() {
        // Use Picasso to load the logo image
        Picasso.get()
            .load(restaurant.logoUrl)
            .placeholder(R.drawable.placeholder_image) // Optional placeholder image
            .error(R.drawable.error_image) // Optional error image
            .into(imageViewRestaurantLogo)
    }

    private fun showToast(message: String) {
        Toast.makeText(this@RestaurantProfileActivity, message, Toast.LENGTH_SHORT).show()
    }
}




class RestaurantProfilePagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 3 // Meals, Items, and Address

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> MealsFragment()  // Assuming you already have this fragment
            1 -> ItemsFragment()  // Assuming you already have this fragment
            2 -> AddressFragment(isRestaurantContext = true)  // This will be the new fragment to manage address
            else -> MealsFragment()
        }
    }
}
