package com.example.meal_mission_app.helper

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.meal_mission_app.DTO.ItemResponse
import com.example.meal_mission_app.DTO.MealResponse
import com.example.meal_mission_app.R

class MealAdapter(private val meals: List<MealResponse>) : RecyclerView.Adapter<MealAdapter.MealViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_meal, parent, false)
        return MealViewHolder(view)
    }

    override fun onBindViewHolder(holder: MealViewHolder, position: Int) {
        holder.bind(meals[position])
    }

    override fun getItemCount() = meals.size

    class MealViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val mealNameTextView: TextView = view.findViewById(R.id.mealName)
        private val mealDescriptionTextView: TextView = view.findViewById(R.id.mealDescription)
        private val mealPriceTextView: TextView = view.findViewById(R.id.mealPrice)

        fun bind(meal: MealResponse) {
            mealNameTextView.text = meal.name
            mealDescriptionTextView.text = meal.description
            mealPriceTextView.text = "${meal.price} USD"
        }
    }
}

class ItemAdapter(private val items: List<ItemResponse>) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_item, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val itemNameTextView: TextView = view.findViewById(R.id.itemName)
        private val itemDescriptionTextView: TextView = view.findViewById(R.id.itemDescription)
        private val itemPriceTextView: TextView = view.findViewById(R.id.itemPrice)

        fun bind(item: ItemResponse) {
            itemNameTextView.text = item.name
            itemDescriptionTextView.text = item.description
            itemPriceTextView.text = "${item.price} USD"
        }
    }
}
