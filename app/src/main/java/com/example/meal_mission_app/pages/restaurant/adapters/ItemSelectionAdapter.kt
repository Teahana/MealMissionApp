package com.example.meal_mission_app.pages.restaurant.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.meal_mission_app.DTO.Item
import com.example.meal_mission_app.R

// ItemSelectionAdapter.kt
class ItemSelectionAdapter(
    private val itemsList: List<Item>,
    private val selectedItemIds: MutableSet<Long>
) : RecyclerView.Adapter<ItemSelectionAdapter.ItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_item_selection, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = itemsList[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = itemsList.size

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkBoxItem: CheckBox = itemView.findViewById(R.id.checkBoxItem)
        private val textViewItemName: TextView = itemView.findViewById(R.id.textViewItemName)

        fun bind(item: Item) {
            textViewItemName.text = item.name
            checkBoxItem.isChecked = selectedItemIds.contains(item.id)

            checkBoxItem.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedItemIds.add(item.id)
                } else {
                    selectedItemIds.remove(item.id)
                }
            }
        }
    }
}
