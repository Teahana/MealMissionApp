package com.example.meal_mission_app.pages

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.meal_mission_app.R
import com.example.meal_mission_app.objects.NetworkClient
import com.example.meal_mission_app.objects.OfflineStorageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.example.meal_mission_app.pages.customer.CustomerHomepageActivity
import com.example.meal_mission_app.pages.driver.DriverHomePageActivity
import com.example.meal_mission_app.pages.driver.DriverOrderListActivity
import com.example.meal_mission_app.pages.restaurant.OrderListActivity

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

        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()
            performLogin(username, password)
        }

        customerLoginButton.setOnClickListener {
            performLogin("customer", "password")
        }

        driverLoginButton.setOnClickListener {
            performLogin("driver", "password")
        }

        restaurantLoginButton.setOnClickListener {
            performLogin("restaurant", "password")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun performLogin(username: String, password: String) {
        val loginData = mapOf("username" to username, "password" to password)

        // Tag for logging
        val TAG = "LoginActivity"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Attempting login with username: $username") // Log before making the request
                val response = NetworkClient.apiService.postRequest("/api/login", loginData)
                if (response.isSuccessful) {
                    val responseData = response.body()

                    Log.d(TAG, "Login successful: $responseData") // Log the success and response data

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
                            "DRIVER" -> {
                                Log.i(TAG, "Navigating to Driver Home Page") // Log navigation
                                startActivity(Intent(this@LoginActivity, DriverOrderListActivity::class.java))
                            }
                            "RESTAURANT" -> {
                                Log.i(TAG, "Navigating to Restaurant Home Page") // Log navigation
                                startActivity(Intent(this@LoginActivity, OrderListActivity::class.java))
                            }
                            "CUSTOMER" -> {
                                Log.i(TAG, "Navigating to Customer Page") // Log navigation
                                startActivity(Intent(this@LoginActivity, CustomerHomepageActivity::class.java))
                            }
                            else -> {
                                Log.w(TAG, "Unknown user type: $userType") // Log unexpected user type
                                Toast.makeText(this@LoginActivity, "Unknown user type", Toast.LENGTH_SHORT).show()
                            }
                        }
                        finish()
                    }
                } else {
                    Log.w(TAG, "Login failed: ${response.errorBody()?.string()}") // Log the failure
                }
            } catch (e: HttpException) {
                Log.e(TAG, "HttpException during login: ${e.message()}", e) // Log exceptions
            } catch (e: Exception) {
                Log.e(TAG, "Exception during login: ${e.message}", e) // Log unexpected exceptions
            }
        }
    }
}

