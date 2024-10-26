package com.example.meal_mission_app.pages.restaurant

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.meal_mission_app.R
import com.google.android.material.tabs.TabLayoutMediator
import androidx.viewpager2.widget.ViewPager2
import com.example.meal_mission_app.helper.OrderStatus
import java.time.LocalDate
import java.time.LocalTime

class RestaurantOrderListActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var pagerAdapter: OrdersPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set the content view to the layout containing TabLayout and ViewPager2
        setContentView(R.layout.activity_order_list)

        viewPager = findViewById(R.id.viewPager)
        pagerAdapter = OrdersPagerAdapter(this)
        viewPager.adapter = pagerAdapter

        val tabLayout = findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabLayout)

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = if (position == 0) "Live Orders" else "Completed Orders"
        }.attach()
    }
}
class OrdersPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 2 // We have two fragments

    override fun createFragment(position: Int): Fragment {
        return if (position == 0) {
            LiveOrdersFragment()
        } else {
            CompletedOrdersFragment()
        }
    }
}
class OrderAdapter(
    private val orders: List<CustomerLiveOrder>,
    private val onItemClick: (Long, String) -> Unit
) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewOrderId: TextView = itemView.findViewById(R.id.textViewOrderId)
        val textViewOrderStatus: TextView = itemView.findViewById(R.id.textViewOrderStatus)
        val textViewOrderDateTime: TextView = itemView.findViewById(R.id.textViewOrderDateTime)
        val textViewOrderDistance: TextView = itemView.findViewById(R.id.textViewOrderDistance)
        val textViewPrice: TextView = itemView.findViewById(R.id.textViewOrderPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]
        holder.textViewOrderId.text = "Order #${order.orderId}"
        holder.textViewOrderStatus.text = order.orderStatus
        holder.textViewOrderDateTime.text = "Date: ${order.orderDate} \nTime: ${order.orderTime}"
        if (order.orderDistance != null) {
            holder.textViewOrderDistance.text = "${order.orderDistance}"
            holder.textViewOrderDistance.visibility = View.VISIBLE
        } else {
            holder.textViewOrderDistance.visibility = View.GONE
        }
        holder.itemView.setOnClickListener {
            onItemClick(order.orderId, order.orderStatus)
        }
//        if(order.orderStatus == OrderStatus.DELIVERING.toString()
//            || order.orderStatus == OrderStatus.DELIVERED.toString()
//            || order.orderStatus == OrderStatus.READY.toString()){
//            holder.textViewPrice.visibility = View.VISIBLE
//            holder.textViewPrice.text = "$${order.price}"
//        }else{
//            holder.textViewPrice.visibility = View.GONE
//        }
        if(order.price > 0){
            holder.textViewPrice.visibility = View.VISIBLE
            holder.textViewPrice.text = "$${order.price}"
        }else{
            holder.textViewPrice.visibility = View.GONE
        }
    }

    override fun getItemCount() = orders.size
}
data class CustomerOrderResponse(
    val orders: List<CustomerOrderDto>
)

data class CustomerLiveOrder(
    val orderId: Long,
    val orderStatus: String,
    val orderDate: LocalDate,
    val orderTime: LocalTime,
    val orderDistance: String?,
    val price: Double
)