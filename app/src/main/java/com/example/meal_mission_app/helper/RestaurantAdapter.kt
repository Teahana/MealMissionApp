package com.example.meal_mission_app.helper

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.meal_mission_app.DTO.RestaurantResponse
import com.example.meal_mission_app.R

class RestaurantAdapter(
    var restaurants: List<RestaurantResponse>,
    private val onItemClick: (RestaurantResponse) -> Unit
) : RecyclerView.Adapter<RestaurantAdapter.RestaurantViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RestaurantViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_restaurant, parent, false)
        return RestaurantViewHolder(view)
    }

    override fun onBindViewHolder(holder: RestaurantViewHolder, position: Int) {
        val restaurant = restaurants[position]
        holder.bind(restaurant)
        holder.itemView.setOnClickListener {
            onItemClick(restaurant)
        }
    }

    override fun getItemCount(): Int {
        return restaurants.size
    }

    fun updateRestaurants(newRestaurants: List<RestaurantResponse>) {
        this.restaurants = newRestaurants
        notifyDataSetChanged()
    }

    class RestaurantViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val nameTextView: TextView = view.findViewById(R.id.restaurantName)
        private val descriptionTextView: TextView = view.findViewById(R.id.restaurantDescription)
        private val addressTextView: TextView = view.findViewById(R.id.restaurantAddress)

        fun bind(restaurant: RestaurantResponse) {
            nameTextView.text = restaurant.name
            descriptionTextView.text = restaurant.description
            addressTextView.text = restaurant.address
        }
    }
}
