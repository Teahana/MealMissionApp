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

    override fun onCreate() {
        super.onCreate()

        println("LocationForegroundService created")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000L
        ).apply {
            setMinUpdateIntervalMillis(5000L)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                location?.let {
                    println("Location fetched: Latitude ${it.latitude}, Longitude ${it.longitude}, Accuracy ${it.accuracy}")
                    sendLocationToServer(it)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService()
        } else {
            startForeground(1, createLegacyNotification())
        }

        startLocationUpdates()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startForegroundService() {
        println("Starting foreground service with notification")
        val channelId = "LocationServiceChannel"
        val channel = NotificationChannel(
            channelId,
            "Location Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Location Service")
            .setContentText("Fetching location in the background")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        startForeground(1, notification)
    }

    private fun createLegacyNotification(): Notification {
        println("Creating legacy notification")
        return NotificationCompat.Builder(this)
            .setContentTitle("Location Service")
            .setContentText("Fetching location in the background")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
    }

    private fun startLocationUpdates() {
        println("Starting location updates")
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            println("Location permissions not granted, stopping service")
            stopSelf()
            return
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private fun sendLocationToServer(location: Location) {
        val userId = OfflineStorageService.getUserId(this) ?: return
        val token = "Bearer ${OfflineStorageService.getToken(this)}"

        println("Sending location to server: Latitude ${location.latitude}, Longitude ${location.longitude}, Accuracy ${location.accuracy}")

        val locationData = mapOf(
            "latitude" to location.latitude.toString(),
            "longitude" to location.longitude.toString(),
            "accuracy" to location.accuracy.toString(),
            "userId" to userId
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                NetworkClient.apiService.postRequest("/api/location/update", locationData, token)
                println("Location successfully posted to server")
            } catch (e: Exception) {
                println("Failed to post location to server: ${e.message}")
                e.printStackTrace() // Handle error
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        println("LocationForegroundService destroyed")
        fusedLocationClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }
}
