package com.example.meal_mission_app.pages

import android.Manifest
import android.content.Context
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
import com.example.meal_mission_app.services.LocationForegroundService
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task

class DriverHomePageActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_LOCATION_PERMISSIONS_CODE = 1001
        private const val REQUEST_CHECK_SETTINGS = 1002
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_homepage)

        println("DriverHomePageActivity created")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val startButton: Button = findViewById(R.id.startButton)
        val stopButton: Button = findViewById(R.id.stopButton)

        startButton.setOnClickListener {
            println("Start button clicked")
            if (checkLocationPermission()) {
                println("Location permission granted")
                checkLocationSettings()
            } else {
                println("Location permission not granted, requesting permission")
                requestLocationPermission()
            }
        }

        stopButton.setOnClickListener {
            println("Stop button clicked")
            stopLocationService()
        }
    }

    private fun checkLocationPermission(): Boolean {
        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        println("Check location permission: $hasPermission")
        return hasPermission
    }

    private fun requestLocationPermission() {
        println("Requesting location permission")
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_LOCATION_PERMISSIONS_CODE
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSIONS_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            println("Location permission granted by user")
            checkLocationSettings()
        } else {
            println("Location permission denied by user")
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkLocationSettings() {
        println("Checking location settings")
        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L
        ).apply {
            setMinUpdateIntervalMillis(5000L)
        }.build()

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true)

        val settingsClient: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = settingsClient.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            println("Location settings are OK, starting location service")
            startLocationService()
        }

        task.addOnFailureListener { exception ->
            println("Location settings check failed")
            if (exception is ResolvableApiException) {
                try {
                    exception.startResolutionForResult(this@DriverHomePageActivity, REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    println("Unable to resolve location settings: ${sendEx.message}")
                    Toast.makeText(this, "Unable to resolve location settings.", Toast.LENGTH_SHORT).show()
                }
            } else {
                println("Location services are not enabled")
                Toast.makeText(this, "Please enable location services to continue.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startLocationService() {
        println("Starting LocationForegroundService")
        val intent = Intent(this, LocationForegroundService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopLocationService() {
        println("Stopping LocationForegroundService")
        val intent = Intent(this, LocationForegroundService::class.java)
        stopService(intent)
    }
}
