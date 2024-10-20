package com.example.meal_mission_app.pages.customer

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.meal_mission_app.R
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
                    if (this !is CustomerHomepageActivity && this !is RestaurantDetailsActivity) {
                        startActivity(Intent(this, CustomerHomepageActivity::class.java))
                        finish()
                    }
                    true
                }
                R.id.nav_cart -> {
                    if (this !is CartActivity) {
                        startActivity(Intent(this, CartActivity::class.java))
                        finish()
                    }
                    true
                }
                R.id.nav_orders -> {
                    if (this !is CustomerOrdersActivity) {
                        startActivity(Intent(this, CustomerOrdersActivity::class.java))
                        finish()
                    }
                    true
                }
                R.id.nav_profile -> {
                    if (this !is ProfileActivity) {
                        startActivity(Intent(this, ProfileActivity::class.java))
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
