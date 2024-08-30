package com.example.meal_mission_app.pages

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.meal_mission_app.R
import com.example.meal_mission_app.services.LocationService
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task

class DriverHomePageActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_LOCATION_PERMISSIONS_CODE = 1001
        private const val REQUEST_CHECK_SETTINGS = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_homepage)

        val startButton: Button = findViewById(R.id.startButton)
        val stopButton: Button = findViewById(R.id.stopButton)

        startButton.setOnClickListener {
            if (checkLocationPermission()) {
                println("Permission granted")
                checkLocationSettings()
            } else {
                println("Permission not granted")
                requestLocationPermission()
            }
        }

        stopButton.setOnClickListener {
            stopLocationService()
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            REQUEST_LOCATION_PERMISSIONS_CODE
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_LOCATION_PERMISSIONS_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission granted, check if location settings are enabled
                    checkLocationSettings()
                } else {
                    // Permission denied, show a message to the user
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun checkLocationSettings() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L // Interval in milliseconds
        ).apply {
            setMinUpdateIntervalMillis(5000L) // Fastest interval in milliseconds
        }.build()

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true) // Show the dialog even if location settings are off

        val settingsClient: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = settingsClient.checkLocationSettings(builder.build())

        task.addOnSuccessListener { locationSettingsResponse ->
            // All location settings are satisfied. Start the LocationService.
            startLocationService()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed by showing the user a dialog.
                try {
                    exception.startResolutionForResult(this@DriverHomePageActivity, REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                    Toast.makeText(this, "Unable to resolve location settings.", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Show a message to the user that they need to enable location services.
                Toast.makeText(this, "Please enable location services to continue.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CHECK_SETTINGS) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    // User agreed to make required location settings changes
                    startLocationService()
                }
                Activity.RESULT_CANCELED -> {
                    // User chose not to make required location settings changes
                    Toast.makeText(this, "Location services are necessary for this feature.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startLocationService() {
        println("Entered startLocationService")
        val serviceIntent = Intent(this, LocationService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun stopLocationService() {
        val serviceIntent = Intent(this, LocationService::class.java)
        stopService(serviceIntent)
    }
}
