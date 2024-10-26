package com.example.meal_mission_app.pages.driver

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.meal_mission_app.R
import com.google.android.material.bottomnavigation.BottomNavigationView

open class DriverBaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.base_activity_driver)

        // Set up BottomNavigationView
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.driver_bottom_navigation)

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_driver_orders -> {
                    if (this !is DriverOrderListActivity) {
                        startActivity(Intent(this, DriverOrderListActivity::class.java))
                        finish()
                    }
                    true
                }
                R.id.nav_driver_profile -> {
                    if (this !is DriverProfileActivity) {
                        startActivity(Intent(this, DriverProfileActivity::class.java))
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
        return R.id.nav_driver_orders // Default to Orders
    }

    override fun onResume() {
        super.onResume()
        // Ensure the correct item is selected
        findViewById<BottomNavigationView>(R.id.driver_bottom_navigation).selectedItemId = getSelectedItemId()
    }
}
