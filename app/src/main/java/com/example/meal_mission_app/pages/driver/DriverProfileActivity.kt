package com.example.meal_mission_app.pages.driver

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.example.meal_mission_app.R
import com.example.meal_mission_app.objects.NetworkClient
import com.example.meal_mission_app.objects.OfflineStorageService
import com.example.meal_mission_app.pages.LoginActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DriverProfileActivity : DriverBaseActivity() {

    private lateinit var textViewDriverName: TextView
    private lateinit var textViewEmail: TextView
    private lateinit var textViewPhone: TextView
    private lateinit var textViewLicenseNumber: TextView
    private lateinit var textViewPlateNumber: TextView
    private lateinit var textViewRating: TextView
    private lateinit var textViewOrdersDelivered: TextView
    private lateinit var buttonLogout: Button

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layoutInflater.inflate(R.layout.activity_driver_profile, findViewById(R.id.activity_content))

        // Initialize UI elements
        textViewDriverName = findViewById(R.id.textViewDriverName)
        textViewEmail = findViewById(R.id.textViewEmail)
        textViewPhone = findViewById(R.id.textViewPhone)
        textViewLicenseNumber = findViewById(R.id.textViewLicenseNumber)
        textViewPlateNumber = findViewById(R.id.textViewPlateNumber)
        textViewRating = findViewById(R.id.textViewRating)
        textViewOrdersDelivered = findViewById(R.id.textViewOrdersDelivered)
        buttonLogout = findViewById(R.id.buttonLogout)

        // Fetch and display driver profile data
        fetchDriverProfile()

        // Logout button functionality
        buttonLogout.setOnClickListener {
            performLogout()
        }
    }
    override fun getSelectedItemId(): Int {
        return R.id.nav_driver_profile
    }
    // Function to fetch driver profile from server
    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchDriverProfile() {
        val driverId = OfflineStorageService.getUserId(this)
        val token = "Bearer ${OfflineStorageService.getToken(this)}"
        val requestData = mapOf("driverId" to driverId.toString())

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = NetworkClient.apiService.getDriverProfile(requestData, token)
                if (response.isSuccessful) {
                    val profileData = response.body()
                    profileData?.let {
                        withContext(Dispatchers.Main) {
                            updateUI(it)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@DriverProfileActivity, "Failed to fetch profile", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DriverProfileActivity, "Error fetching profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Function to update the UI with driver profile data
    private fun updateUI(profileData: Map<String, Any>) {
        val firstName = profileData["firstName"] as String
        val lastName = profileData["lastName"] as String
        textViewDriverName.text = "$firstName $lastName"
        textViewEmail.text = "Email: ${profileData["email"]}"
        textViewPhone.text = "Phone: ${profileData["phone"]}"
        textViewLicenseNumber.text = "License Number: ${profileData["licenseNumber"]}"
        textViewPlateNumber.text = "Plate Number: ${profileData["plateNumber"]}"
        textViewRating.text = "Rating: ${profileData["rating"]}"
        textViewOrdersDelivered.text = "Orders Delivered: ${profileData["ordersDelivered"]}"
    }

    // Function to handle logout
    private fun performLogout() {
        // Notify the fragment to stop any ongoing tasks or cleanup
        supportFragmentManager.findFragmentByTag("DriverLiveOrdersFragment")?.let { fragment ->
            if (fragment is DriverLiveOrdersFragment) {
                fragment.stopPollingExplicitly() // Ensure polling is stopped
            }
        }

        // Clear local storage or any persisted data
        OfflineStorageService.clearUserCredentials(this)

        // Redirect to LoginActivity (or any other appropriate screen)
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
