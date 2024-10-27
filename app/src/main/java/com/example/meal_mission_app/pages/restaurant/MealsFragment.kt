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
import com.example.meal_mission_app.DTO.Meal
import com.example.meal_mission_app.R
import com.example.meal_mission_app.objects.NetworkClient
import com.example.meal_mission_app.objects.OfflineStorageService
import com.example.meal_mission_app.pages.restaurant.adapters.MealsAdapter
import kotlinx.coroutines.launch

class MealsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MealsAdapter
    private val mealsList = mutableListOf<Meal>()
    private val authToken: String by lazy { "Bearer ${OfflineStorageService.getToken(requireContext())}" }
    private val restaurantId: Long by lazy {
        OfflineStorageService.getRestaurantId(requireContext())?.toLongOrNull() ?: 0L
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_meals, container, false)
        recyclerView = view.findViewById(R.id.recyclerViewMeals)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = MealsAdapter(mealsList, ::onMealClicked)
        recyclerView.adapter = adapter

        fetchMeals()

        // Enable options menu for adding new meals
        setHasOptionsMenu(true)

        return view
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchMeals() {
        lifecycleScope.launch {
            try {
                val response = NetworkClient.apiService.listMeals(restaurantId, authToken)
                if (response.isSuccessful) {
                    mealsList.clear()
                    mealsList.addAll(response.body()!!)
                    adapter.notifyDataSetChanged()
                } else {
                    showToast("Failed to fetch meals")
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun onMealClicked(meal: Meal) {
        // Open dialog or activity to edit meal
        val dialog = EditMealDialogFragment(meal)
        dialog.setOnMealUpdatedListener {
            fetchMeals()
        }
        dialog.show(parentFragmentManager, "EditMealDialog")
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    // Inflate the menu for adding new meals
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.meals_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    // Handle menu item selection
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add_meal -> {
                val dialog = EditMealDialogFragment(null)
                dialog.setOnMealUpdatedListener {
                    fetchMeals()
                }
                dialog.show(parentFragmentManager, "AddMealDialog")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
