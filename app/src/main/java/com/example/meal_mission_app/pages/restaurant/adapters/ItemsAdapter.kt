package com.example.meal_mission_app.pages.restaurant.adapters


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.meal_mission_app.DTO.Item
import com.example.meal_mission_app.R
import com.squareup.picasso.Picasso

class ItemsAdapter(
    private val itemsList: List<Item>,
    private val onItemClicked: (Item) -> Unit
) : RecyclerView.Adapter<ItemsAdapter.ItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_item_profile, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = itemsList[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = itemsList.size

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageViewItem: ImageView = itemView.findViewById(R.id.imageViewItem)
        private val textViewItemName: TextView = itemView.findViewById(R.id.textViewItemName)
        private val textViewItemPrice: TextView = itemView.findViewById(R.id.textViewItemPrice)
        private val imageViewAvailability: ImageView = itemView.findViewById(R.id.imageViewAvailability)

        fun bind(item: Item) {
            textViewItemName.text = item.name
            textViewItemPrice.text = "$${item.price}"

            // Load image using Picasso or any other image loading library
            if (item.imageUrl?.isNotEmpty() == true) {
                Picasso.get().load(item.imageUrl).into(imageViewItem)
            } else {
                imageViewItem.setImageResource(R.drawable.placeholder_image_item) // Placeholder image
            }

            // Change appearance based on availability
            if (item.available) {
                imageViewAvailability.setImageResource(R.drawable.ic_available)
                itemView.alpha = 1f // Fully opaque
            } else {
                imageViewAvailability.setImageResource(R.drawable.ic_unavailable)
                itemView.alpha = 0.5f // Semi-transparent to indicate unavailability
            }

            itemView.setOnClickListener {
                onItemClicked(item)
            }
        }
    }
}
