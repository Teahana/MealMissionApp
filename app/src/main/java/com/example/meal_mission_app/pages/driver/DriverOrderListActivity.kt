package com.example.meal_mission_app.pages.driver

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.meal_mission_app.R
import com.google.android.material.tabs.TabLayoutMediator

class DriverOrderListActivity : DriverBaseActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var pagerAdapter: DriverOrdersPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layoutInflater.inflate(R.layout.activity_order_list, findViewById(R.id.activity_content))


        viewPager = findViewById(R.id.viewPager)
        pagerAdapter = DriverOrdersPagerAdapter(this)
        viewPager.adapter = pagerAdapter

        val tabLayout = findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabLayout)

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = if (position == 0) "Live Orders" else "Completed Orders"
        }.attach()
    }
    override fun getSelectedItemId(): Int {
        return R.id.nav_driver_orders // This makes the "Orders" tab selected when in DriverOrderListActivity
    }
}
class DriverOrdersPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 2 // We have two fragments (Live and Completed Orders)

    override fun createFragment(position: Int): Fragment {
        return if (position == 0) {
            DriverLiveOrdersFragment()
        } else {
            DriverCompletedOrdersFragment()
        }
    }
}
