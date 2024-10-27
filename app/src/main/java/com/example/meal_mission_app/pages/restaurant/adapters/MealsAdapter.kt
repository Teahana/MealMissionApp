package com.example.meal_mission_app.pages.restaurant.adapters


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.meal_mission_app.DTO.Meal
import com.example.meal_mission_app.R
import com.squareup.picasso.Picasso

class MealsAdapter(
    private val mealsList: List<Meal>,
    private val onMealClicked: (Meal) -> Unit
) : RecyclerView.Adapter<MealsAdapter.MealViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_meal_profile, parent, false)
        return MealViewHolder(view)
    }

    override fun onBindViewHolder(holder: MealViewHolder, position: Int) {
        val meal = mealsList[position]
        holder.bind(meal)
    }

    override fun getItemCount(): Int = mealsList.size

    inner class MealViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageViewMeal: ImageView = itemView.findViewById(R.id.imageViewMeal)
        private val textViewMealName: TextView = itemView.findViewById(R.id.textViewMealName)
        private val textViewMealPrice: TextView = itemView.findViewById(R.id.textViewMealPrice)
        private val imageViewAvailability: ImageView = itemView.findViewById(R.id.imageViewAvailability)

        fun bind(meal: Meal) {
            if (meal.name.isNullOrBlank()) {
                // Handle the case where name is null or blank
                textViewMealName.text = "Unnamed Meal"
            } else {
                textViewMealName.text = meal.name
            }

            textViewMealPrice.text = "$${meal.price ?: 0.0}"

            // Load image using Picasso or any other image loading library
            if (!meal.imageUrl.isNullOrEmpty()) {
                Picasso.get().load(meal.imageUrl).into(imageViewMeal)
            } else {
                imageViewMeal.setImageResource(R.drawable.placeholder_image_item) // Placeholder image
            }

            // Change appearance based on availability
            if (meal.available) {
                imageViewAvailability.setImageResource(R.drawable.ic_available)
                itemView.alpha = 1f // Fully opaque
            } else {
                imageViewAvailability.setImageResource(R.drawable.ic_unavailable)
                itemView.alpha = 0.5f // Semi-transparent to indicate unavailability
            }

            itemView.setOnClickListener {
                onMealClicked(meal)
            }
        }

    }
}
