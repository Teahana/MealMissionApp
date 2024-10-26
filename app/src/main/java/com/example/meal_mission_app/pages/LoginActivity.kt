package com.example.meal_mission_app.pages

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.meal_mission_app.R
import com.example.meal_mission_app.objects.NetworkClient
import com.example.meal_mission_app.objects.OfflineStorageService
import com.example.meal_mission_app.pages.customer.CustomerHomepageActivityCustomer
import com.example.meal_mission_app.pages.driver.DriverOrderListActivity
import com.example.meal_mission_app.pages.restaurant.RestaurantOrderListActivity
//import com.example.meal_mission_app.pages.restaurant.RestaurantOrderListActivity
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var customerLoginButton: Button
    private lateinit var driverLoginButton: Button
    private lateinit var restaurantLoginButton: Button

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        usernameEditText = findViewById(R.id.username)
        passwordEditText = findViewById(R.id.password)
        loginButton = findViewById(R.id.login_button)
        customerLoginButton = findViewById(R.id.customer_login_button)
        driverLoginButton = findViewById(R.id.driver_login_button)
        restaurantLoginButton = findViewById(R.id.restaurant_login_button)

        // Request notification permission if required
        checkAndRequestNotificationPermission()

        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()
            fetchFcmTokenAndLogin(username, password)
        }

        customerLoginButton.setOnClickListener {
            fetchFcmTokenAndLogin("customer", "password")
        }

        driverLoginButton.setOnClickListener {
            fetchFcmTokenAndLogin("driver", "password")
        }

        restaurantLoginButton.setOnClickListener {
            fetchFcmTokenAndLogin("restaurant", "password")
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                // Request notification permission
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fetchFcmTokenAndLogin(username: String, password: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("LoginActivity", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get the FCM token
            println("FCM FETCHED SUCCESSFFULLY F")
            val fcmToken = task.result

            // Now, perform the login with FCM token included
            performLogin(username, password, fcmToken)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun performLogin(username: String, password: String, fcmToken: String?) {
        val loginData = mapOf(
            "username" to username,
            "password" to password,
            "fcmToken" to fcmToken // Send the FCM token along with login credentials
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = NetworkClient.apiService.postRequest("/api/login", loginData)
                if (response.isSuccessful) {
                    val responseData = response.body()

                    // Safely access the properties in the responseData
                    val token = responseData?.token
                    val refreshToken = responseData?.refreshToken
                    val userId = responseData?.userId
                    val userType = responseData?.userType
                    val restaurantId = responseData?.restaurantId

                    // Save tokens and userId to shared preferences or secure storage
                    OfflineStorageService.saveCredentials(applicationContext, token, refreshToken, userId, userType, restaurantId)

                    // Determine where to navigate based on userType
                    withContext(Dispatchers.Main) {
                        when (userType) {
                            "DRIVER" -> startActivity(Intent(this@LoginActivity, DriverOrderListActivity::class.java))
                            "RESTAURANT" -> startActivity(Intent(this@LoginActivity, RestaurantOrderListActivity::class.java))
                            "CUSTOMER" -> startActivity(Intent(this@LoginActivity, CustomerHomepageActivityCustomer::class.java))
                            else -> Toast.makeText(this@LoginActivity, "Unknown user type", Toast.LENGTH_SHORT).show()
                        }
                        finish()
                    }
                } else {
                    Log.w("LoginActivity", "Login failed: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("LoginActivity", "Error during login: ${e.localizedMessage}")
            }
        }
    }
}

