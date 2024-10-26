package com.example.meal_mission_app.pages.customer

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.meal_mission_app.R
import com.google.android.material.bottomnavigation.BottomNavigationView

open class CustomerBaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.base_activity_customer)

        // Set up BottomNavigationView
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    if (this !is CustomerHomepageActivityCustomer && this !is RestaurantDetailsActivityCustomer) {
                        startActivity(Intent(this, CustomerHomepageActivityCustomer::class.java))
                        finish()
                    }
                    true
                }
                R.id.nav_cart -> {
                    if (this !is CartActivityCustomer) {
                        startActivity(Intent(this, CartActivityCustomer::class.java))
                        finish()
                    }
                    true
                }
                R.id.nav_orders -> {
                    if (this !is CustomerOrdersActivityCustomer) {
                        startActivity(Intent(this, CustomerOrdersActivityCustomer::class.java))
                        finish()
                    }
                    true
                }
                R.id.nav_profile -> {
                    if (this !is ProfileActivityCustomer) {
                        startActivity(Intent(this, ProfileActivityCustomer::class.java))
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
