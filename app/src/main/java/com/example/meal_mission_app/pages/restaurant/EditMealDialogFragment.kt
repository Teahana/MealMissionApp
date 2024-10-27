package com.example.meal_mission_app.pages.restaurant

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.meal_mission_app.DTO.Meal
import com.example.meal_mission_app.R
import com.example.meal_mission_app.objects.FileUtils
import com.example.meal_mission_app.objects.NetworkClient
import com.example.meal_mission_app.objects.OfflineStorageService
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

// EditMealDialogFragment.kt
class EditMealDialogFragment(private val meal: Meal?) : DialogFragment() {

    private lateinit var editTextName: EditText
    private lateinit var editTextPrice: EditText
    private lateinit var editTextDescription: EditText
    private lateinit var imageViewMealImage: ImageView
    private lateinit var buttonSelectImage: Button
    private lateinit var buttonSelectItems: Button
    private lateinit var textViewSelectedItems: TextView
    private lateinit var switchAvailable: SwitchCompat
    private lateinit var buttonSave: Button

    private val authToken: String by lazy { "Bearer ${OfflineStorageService.getToken(requireContext())}" }
    private val restaurantId: Long by lazy {
        OfflineStorageService.getRestaurantId(requireContext())?.toLongOrNull() ?: 0L
    }
    private var onMealUpdatedListener: (() -> Unit)? = null
    private val selectedItemIds = mutableSetOf<Long>()
    private var imageUri: Uri? = null

    fun setOnMealUpdatedListener(listener: () -> Unit) {
        onMealUpdatedListener = listener
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_edit_meal, null)

        editTextName = view.findViewById(R.id.editTextMealName)
        editTextPrice = view.findViewById(R.id.editTextMealPrice)
        editTextDescription = view.findViewById(R.id.editTextMealDescription)
        imageViewMealImage = view.findViewById(R.id.imageViewMealImage)
        buttonSelectImage = view.findViewById(R.id.buttonSelectMealImage)
        buttonSelectItems = view.findViewById(R.id.buttonSelectItems)
        textViewSelectedItems = view.findViewById(R.id.textViewSelectedItems)
        switchAvailable = view.findViewById(R.id.switchMealAvailable)
        buttonSave = view.findViewById(R.id.buttonSaveMeal)

        if (meal != null) {
            // Editing existing meal
            editTextName.setText(meal.name)
            editTextPrice.setText(meal.price.toString())
            editTextDescription.setText(meal.description)
            switchAvailable.isChecked = meal.available
            selectedItemIds.addAll(meal.itemIds)
            updateSelectedItemsText()

            // Load image if available
            if (meal.imageUrl?.isNotEmpty() == true) {
                Picasso.get().load(meal.imageUrl).into(imageViewMealImage)
            }
        } else {
            // Creating new meal
            switchAvailable.isChecked = true
        }

        buttonSelectImage.setOnClickListener {
            selectImage()
        }

        buttonSelectItems.setOnClickListener {
            selectItems()
        }

        buttonSave.setOnClickListener {
            saveMeal()
        }

        builder.setView(view)
        return builder.create()
    }

    private fun selectImage() {
        // Intent to pick image
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    private fun selectItems() {
        // Open ItemSelectionDialogFragment
        val dialog = ItemSelectionDialogFragment(selectedItemIds)
        dialog.setOnItemsSelectedListener { selectedIds ->
            selectedItemIds.clear()
            selectedItemIds.addAll(selectedIds)
            updateSelectedItemsText()
        }
        dialog.show(parentFragmentManager, "ItemSelectionDialog")
    }

    private fun updateSelectedItemsText() {
        if (selectedItemIds.isEmpty()) {
            textViewSelectedItems.text = "Selected Items: None"
        } else {
            textViewSelectedItems.text = "Selected Items: ${selectedItemIds.size}"
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun saveMeal() {
        val name = editTextName.text.toString()
        val priceText = editTextPrice.text.toString()
        val description = editTextDescription.text.toString()
        val isAvailable = switchAvailable.isChecked

        if (name.isBlank() || priceText.isBlank()) {
            showToast("Name and price are required")
            return
        }

        val price = priceText.toDoubleOrNull()
        if (price == null) {
            showToast("Invalid price")
            return
        }

        if (selectedItemIds.isEmpty()) {
            showToast("Please select at least one item")
            return
        }

        val mealToSave = meal ?: Meal(
            id = 0L,
            name = name,
            restaurantId = restaurantId,
            description = description,
            price = price,
            imageUrl = "", // Will be updated after image upload
            available = isAvailable,
            itemIds = selectedItemIds
        )

        mealToSave.name = name
        mealToSave.description = description
        mealToSave.price = price
        mealToSave.available = isAvailable
        mealToSave.itemIds = selectedItemIds

        lifecycleScope.launch {
            try {
                // Handle image upload if imageUri is set
                if (imageUri != null) {
                    val imageUrl = uploadImage(imageUri!!)
                    mealToSave.imageUrl = imageUrl
                }

                val response = if (meal != null) {
                    // Update existing meal
                    println("Updating meal")
                    NetworkClient.apiService.updateMeal(mealToSave, authToken)
                } else {
                    // Create new meal
                    println("Creating meal")
                    mealToSave.restaurantId = restaurantId
                    NetworkClient.apiService.createMeal(mealToSave, authToken)
                }

                if (response.isSuccessful) {
                    showToast("Meal saved successfully")
                    onMealUpdatedListener?.invoke()
                    dismiss()
                } else {
                    val errorBody = response.errorBody()?.string()
                    showToast("Failed to save meal: $errorBody")
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun uploadImage(uri: Uri): String {
        // Convert Uri to File
        val file = FileUtils.getFileFromUri(requireContext(), uri)
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("image", file.name, requestFile)

        val response = NetworkClient.apiService.uploadImage(body, authToken)
        if (response.isSuccessful) {
            // Assuming the server returns the image URL in the response body
            return response.body()?.imageUrl ?: ""
        } else {
            throw Exception("Image upload failed")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    // Handle Image Picker Result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK) {
            imageUri = data?.data
            imageViewMealImage.setImageURI(imageUri)
        }
    }

    companion object {
        private const val REQUEST_IMAGE_PICK = 1001
    }
}


