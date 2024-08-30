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
import android.util.Log
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

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        println("Entered on create in location service")
        super.onCreate()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Create a LocationRequest using Google Play services API
        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10000L // Interval in milliseconds
        ).apply {
            setMinUpdateIntervalMillis(5000L) // Fastest interval in milliseconds
        }.build()

        // Start the service in the foreground
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService()
        } else {
            startForeground(1, createLegacyNotification())
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start location updates when the service is started
        startLocationUpdates()
        return START_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startForegroundService() {
        println("Entered start foreground service")
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
        return NotificationCompat.Builder(this)
            .setContentTitle("Location Service")
            .setContentText("Fetching location in the background")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
    }

    private fun startLocationUpdates() {
        println("Entered start Location updates")
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            println("Permission refused")
            return
        }
        println("Permission granted continuing to fusedLocationClient")
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            println("Entered onLocationResult")
            locationResult ?: return
            for (location in locationResult.locations) {
                sendLocationToServer(location)
            }
        }
    }

    private fun sendLocationToServer(location: Location) {
        println("Entered Send to server")
        Log.d("LocationService", "Location: ${location.latitude}, ${location.longitude}, Accuracy: ${location.accuracy}")
        val token = OfflineStorageService.getToken(applicationContext)
        val userId = OfflineStorageService.getUserId(applicationContext)
        Log.d("Debug Tokens", "Token: $token, User Id: $userId")

        val locationData = mapOf(
            "latitude" to location.latitude.toString(),
            "longitude" to location.longitude.toString(),
            "accuracy" to location.accuracy.toString(),
            "userId" to userId
        )

        scope.launch {
            try {
                NetworkClient.apiService.postRequest("/api/location/update", locationData, "Bearer $token")
            } catch (e: Exception) {
                // Handle exceptions, e.g., log the error
                Log.e("LocationService", "Failed to send location to server: ${e.message}")
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }
}
