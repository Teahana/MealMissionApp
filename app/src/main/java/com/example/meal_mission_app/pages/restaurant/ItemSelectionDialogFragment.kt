package com.example.meal_mission_app.pages.restaurant

import android.app.AlertDialog
import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.meal_mission_app.DTO.Item
import com.example.meal_mission_app.R
import com.example.meal_mission_app.objects.NetworkClient
import com.example.meal_mission_app.objects.OfflineStorageService
import com.example.meal_mission_app.pages.restaurant.adapters.ItemSelectionAdapter
import kotlinx.coroutines.launch

// ItemSelectionDialogFragment.kt
class ItemSelectionDialogFragment(private val preselectedItemIds: Set<Long>) : DialogFragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ItemSelectionAdapter
    private val itemsList = mutableListOf<Item>()
    private val selectedItemIds = mutableSetOf<Long>()

    private val authToken: String by lazy { "Bearer ${OfflineStorageService.getToken(requireContext())}" }
    private val restaurantId: Long by lazy {
        OfflineStorageService.getRestaurantId(requireContext())?.toLongOrNull() ?: 0L
    }

    private var onItemsSelectedListener: ((List<Long>) -> Unit)? = null

    fun setOnItemsSelectedListener(listener: (List<Long>) -> Unit) {
        onItemsSelectedListener = listener
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_item_selection, null)

        recyclerView = view.findViewById(R.id.recyclerViewItemSelection)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = ItemSelectionAdapter(itemsList, selectedItemIds)
        recyclerView.adapter = adapter

        selectedItemIds.addAll(preselectedItemIds)

        builder.setView(view)
            .setTitle("Select Items")
            .setPositiveButton("OK") { _, _ ->
                onItemsSelectedListener?.invoke(selectedItemIds.toList())
                dismiss()
            }
            .setNegativeButton("Cancel") { _, _ ->
                dismiss()
            }

        fetchItems()

        return builder.create()
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

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
