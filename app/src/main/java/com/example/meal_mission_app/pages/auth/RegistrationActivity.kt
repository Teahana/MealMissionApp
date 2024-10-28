package com.example.meal_mission_app.pages.auth

import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.meal_mission_app.DTO.RegistrationRequest
import com.example.meal_mission_app.R
import com.example.meal_mission_app.objects.NetworkClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegistrationActivity : AppCompatActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var firstNameEditText: EditText
    private lateinit var lastNameEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var registerButton: Button
    private lateinit var loginLink: TextView

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set the content view to the registration layout
        setContentView(R.layout.activity_registration)

        // Initialize the views
        usernameEditText = findViewById(R.id.username)
        passwordEditText = findViewById(R.id.password)
        confirmPasswordEditText = findViewById(R.id.confirm_password)
        firstNameEditText = findViewById(R.id.first_name)
        lastNameEditText = findViewById(R.id.last_name)
        phoneEditText = findViewById(R.id.phone)
        registerButton = findViewById(R.id.register_button)
        loginLink = findViewById(R.id.login_link)

        // Set click listener for register button
        registerButton.setOnClickListener {
            performRegistration()
        }

        // Set click listener for login link
        loginLink.setOnClickListener {
            // Finish the registration activity to go back to login
            finish()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun performRegistration() {
        // Get the values from the EditTexts
        val username = usernameEditText.text.toString().trim()
        val password = passwordEditText.text.toString()
        val confirmPassword = confirmPasswordEditText.text.toString()
        val firstName = firstNameEditText.text.toString().trim()
        val lastName = lastNameEditText.text.toString().trim()
        val phone = phoneEditText.text.toString().trim()

        // Validate inputs
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() ||
            firstName.isEmpty() || lastName.isEmpty() || phone.isEmpty()
        ) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        // Create the registration request object
        val registrationRequest = RegistrationRequest(
            username = username,
            password = password,
            firstName = firstName,
            lastName = lastName,
            phone = phone
        )

        // Perform the API call
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = NetworkClient.apiService.registerCustomer(registrationRequest)
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    withContext(Dispatchers.Main) {
                        if (apiResponse != null && apiResponse.success) {
                            Toast.makeText(this@RegistrationActivity, "Registration successful", Toast.LENGTH_SHORT).show()
                            // Go back to login screen
                            finish()
                        } else {
                            Toast.makeText(this@RegistrationActivity, apiResponse?.message ?: "Registration failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        val errorMessage = response.errorBody()?.string() ?: "Registration failed"
                        Toast.makeText(this@RegistrationActivity, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@RegistrationActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
