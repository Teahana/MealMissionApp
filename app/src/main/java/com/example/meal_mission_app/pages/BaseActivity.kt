package com.example.meal_mission_app.pages

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.meal_mission_app.R
import com.example.meal_mission_app.pages.customer.CartActivity
import com.example.meal_mission_app.pages.customer.CustomerActivity
import com.example.meal_mission_app.pages.customer.CustomerHomepageActivity
import com.example.meal_mission_app.pages.customer.RestaurantDetailsActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.base_activity)

        // Set up BottomNavigationView
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    if (this !is CustomerHomepageActivity) {
                        val intent = Intent(this, CustomerHomepageActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        startActivity(intent)
                        finish()
                    }
                    true
                }
                R.id.nav_cart -> {
                    if (this !is CartActivity) {
                        val intent = Intent(this, CartActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        startActivity(intent)
                        finish()
                    }
                    true
                }
//                R.id.nav_orders -> {
//                    if (this !is RestaurantDetailsActivity) {
//                        val intent = Intent(this, RestaurantDetailsActivity::class.java)
//                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
//                        startActivity(intent)
//                        finish()
//                    }
//                    true
//                }
                R.id.nav_profile -> {
                    if (this !is CustomerActivity) {
                        val intent = Intent(this, CustomerActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        startActivity(intent)
                        finish()
                    }
                    true
                }
                else -> false
            }
        }
    }

    // This will allow child activities to specify which item to highlight
    open fun getSelectedItemId(): Int {
        return R.id.nav_home // Default to Home
    }

    override fun onResume() {
        super.onResume()
        // Ensure correct item is selected
        findViewById<BottomNavigationView>(R.id.bottom_navigation).selectedItemId = getSelectedItemId()
    }
}
