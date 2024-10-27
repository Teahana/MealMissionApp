package com.example.meal_mission_app.pages.restaurant

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.meal_mission_app.DTO.Item
import com.example.meal_mission_app.R
import com.example.meal_mission_app.objects.FileUtils
import com.example.meal_mission_app.objects.NetworkClient
import com.example.meal_mission_app.objects.OfflineStorageService
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

// EditItemDialogFragment.kt
class EditItemDialogFragment(private val item: Item?) : DialogFragment() {

    private lateinit var editTextName: EditText
    private lateinit var editTextPrice: EditText
    private lateinit var editTextDescription: EditText
    private lateinit var imageViewItemImage: ImageView
    private lateinit var buttonSelectImage: Button
    private lateinit var switchAvailable: SwitchCompat
    private lateinit var buttonSave: Button

    private val authToken: String by lazy { "Bearer ${OfflineStorageService.getToken(requireContext())}" }
    private val restaurantId: Long by lazy {
        OfflineStorageService.getRestaurantId(requireContext())?.toLongOrNull() ?: 0L
    }
    private var onItemUpdatedListener: (() -> Unit)? = null
    private var imageUri: Uri? = null

    fun setOnItemUpdatedListener(listener: () -> Unit) {
        onItemUpdatedListener = listener
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_edit_item, null)

        editTextName = view.findViewById(R.id.editTextItemName)
        editTextPrice = view.findViewById(R.id.editTextItemPrice)
        editTextDescription = view.findViewById(R.id.editTextItemDescription)
        imageViewItemImage = view.findViewById(R.id.imageViewItemImage)
        buttonSelectImage = view.findViewById(R.id.buttonSelectItemImage)
        switchAvailable = view.findViewById(R.id.switchItemAvailable)
        buttonSave = view.findViewById(R.id.buttonSaveItem)

        if (item != null) {
            // Editing existing item
            editTextName.setText(item.name)
            editTextPrice.setText(item.price.toString())
            editTextDescription.setText(item.description)
            switchAvailable.isChecked = item.available

            // Load image if available
            if (item.imageUrl?.isNotEmpty() == true) {
                Picasso.get().load(item.imageUrl).into(imageViewItemImage)
            }
        } else {
            // Creating new item
            switchAvailable.isChecked = true
        }

        buttonSelectImage.setOnClickListener {
            selectImage()
        }

        buttonSave.setOnClickListener {
            saveItem()
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun saveItem() {
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

        val itemToSave = item ?: Item(
            id = 0L,
            restaurantId = 0L,
            name = name,
            description = description,
            price = price,
            imageUrl = "", // Will be updated after image upload
            available = isAvailable
        )

        itemToSave.name = name
        itemToSave.description = description
        itemToSave.price = price
        itemToSave.restaurantId = restaurantId
        itemToSave.available = isAvailable

        lifecycleScope.launch {
            try {
                // Handle image upload if imageUri is set
                if (imageUri != null) {
                    val imageUrl = uploadImage(imageUri!!)
                    itemToSave.imageUrl = imageUrl
                }
                println("Item to save: $itemToSave")
                val response = if (item != null) {
                    // Update existing item
                    NetworkClient.apiService.updateItem(itemToSave, authToken)
                } else {
                    // Create new item
                    itemToSave.restaurantId = restaurantId
                    NetworkClient.apiService.createItem(itemToSave, authToken)
                }

                if (response.isSuccessful) {
                    showToast("Item saved successfully")
                    onItemUpdatedListener?.invoke()
                    dismiss()
                } else {
                    val errorBody = response.errorBody()?.string()
                    showToast("Failed to save item: $errorBody")
                    println("Error: $errorBody")
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
                println("Error: ${e.message}")
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
            imageViewItemImage.setImageURI(imageUri)
        }
    }

    companion object {
        private const val REQUEST_IMAGE_PICK = 1001
    }
}

