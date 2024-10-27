package com.example.meal_mission_app.pages.restaurant



import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.meal_mission_app.DTO.Item
import com.example.meal_mission_app.R
import com.example.meal_mission_app.objects.NetworkClient
import com.example.meal_mission_app.objects.OfflineStorageService
import com.example.meal_mission_app.pages.restaurant.adapters.ItemsAdapter
import kotlinx.coroutines.launch

class ItemsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ItemsAdapter
    private val itemsList = mutableListOf<Item>()
    private val authToken: String by lazy { "Bearer ${OfflineStorageService.getToken(requireContext())}" }
    private val restaurantId: Long by lazy {
        OfflineStorageService.getRestaurantId(requireContext())?.toLongOrNull() ?: 0L
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_items, container, false)
        recyclerView = view.findViewById(R.id.recyclerViewItems)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = ItemsAdapter(itemsList, ::onItemClicked)
        recyclerView.adapter = adapter

        fetchItems()

        // Enable options menu for adding new items
        setHasOptionsMenu(true)

        return view
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchItems() {
        lifecycleScope.launch {
            try {
                val response = NetworkClient.apiService.listItems(restaurantId, authToken)
                if (response.isSuccessful) {
                    itemsList.clear()
                    itemsList.addAll(response.body()!!)
                    adapter.notifyDataSetChanged()
                } else {
                    showToast("Failed to fetch items")
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun onItemClicked(item: Item) {
        // Open dialog or activity to edit item
        val dialog = EditItemDialogFragment(item)
        dialog.setOnItemUpdatedListener {
            fetchItems()
        }
        dialog.show(parentFragmentManager, "EditItemDialog")
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    // Inflate the menu for adding new items
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.items_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    // Handle menu item selection
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add_item -> {
                val dialog = EditItemDialogFragment(null)
                dialog.setOnItemUpdatedListener {
                    fetchItems()
                }
                dialog.show(parentFragmentManager, "AddItemDialog")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}
