package com.example.meal_mission_app.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.meal_mission_app.R
import com.example.meal_mission_app.objects.NetworkClient
import com.example.meal_mission_app.objects.OfflineStorageService
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LocationForegroundService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    companion object {
        const val CHANNEL_ID = "LocationForegroundServiceChannel"
    }

    override fun onCreate() {
        super.onCreate()

        println("LocationForegroundService created")

        // Start the service in the foreground with a notification
        createNotificationChannel()
        val notification = createNotification()
        startForeground(1, notification)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10000L // Requesting updates every 10 seconds
        ).apply {
            setMinUpdateIntervalMillis(10000L)
        }.build()

        locationCallback = object : LocationCallback() {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { gps ->
                    // Just print the location without sending to the server
                    println("Location fetched: Latitude ${gps.latitude}, Longitude ${gps.longitude}, Accuracy ${gps.accuracy}")

                    // Commented out sending to the server
                    sendLocationToServer(gps)
                }
            }
        }

        startLocationUpdates()
    }

    fun startLocationUpdates() {
        println("Starting location updates")
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest, locationCallback, Looper.getMainLooper()
            )
        } else {
            println("Location permissions not granted, stopping service")
            stopSelf()
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendLocationToServer(location: Location) {
        val userId = OfflineStorageService.getUserId(this) ?: return
        val token = "Bearer ${OfflineStorageService.getToken(this)}"

        val locationData = mapOf(
            "latitude" to location.latitude.toString(),
            "longitude" to location.longitude.toString(),
            "accuracy" to location.accuracy.toString(),
            "driverId" to userId
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                NetworkClient.apiService.updateDriverLocation(locationData, token)
                println("Location successfully posted to server")
            } catch (e: Exception) {
                println("Failed to post location to server: ${e.message}")
            }
        }
    }
    fun stopLocationUpdates() {
        println("Stopping location updates")
        fusedLocationClient.removeLocationUpdates(locationCallback)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        println("LocationForegroundService destroyed")
    }

    // Create a persistent notification for the foreground service
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Location Tracking Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Meal Mission")
            .setContentText("Tracking your location...")
            .setSmallIcon(R.drawable.ic_location)
            .build()
    }
}

