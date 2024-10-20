package com.example.meal_mission_app.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.meal_mission_app.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.example.meal_mission_app.objects.NetworkClient
import com.example.meal_mission_app.objects.OfflineStorageService
import com.example.meal_mission_app.pages.LoginActivity
import com.example.meal_mission_app.pages.driver.DriverOrderListActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "MyFirebaseMsgService"
        private const val NOTIFICATION_REQUEST_CODE = 1001
    }

    // Called when a new token is generated or refreshed
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")

        // Retrieve userId and send the new token to the server
        val userId = OfflineStorageService.getUserId(this)
        val jwtToken = OfflineStorageService.getToken(this)

        sendNewFcmTokenToServer(userId, token, jwtToken)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendNewFcmTokenToServer(userId: String?, fcmToken: String, jwtToken: String?) {
        if (userId == null || jwtToken == null) {
            Log.w(TAG, "User not logged in, skipping token update.")
            return
        }

        val tokenData = mapOf("userId" to userId, "fcmToken" to fcmToken)
        val bearerToken = "Bearer $jwtToken"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = NetworkClient.apiService.updateFcmToken(tokenData, bearerToken)
                if (response.isSuccessful) {
                    Log.d(TAG, "FCM token updated successfully on server")
                } else {
                    Log.w(TAG, "Failed to update FCM token on server: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating FCM token on server: ${e.localizedMessage}")
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "Message received from: ${remoteMessage.from}")

        // Check if the message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Title: ${it.title}")
            Log.d(TAG, "Message Notification Body: ${it.body}")

            // Handle the notification payload (title, body)
            showNotification(it.title, it.body)
        }

        // Check if the message contains data payload.
        remoteMessage.data?.let { payload ->
            Log.d(TAG, "Message Data Payload: $payload")

            // Extract custom data and perform the desired action
//            val activityToLaunch = payload["activity"]
//            val orderId = payload["orderId"]
//
//            if (activityToLaunch != null && orderId != null) {
//                // Handle navigation to a specific activity based on the received data
//                navigateToActivity(activityToLaunch, orderId)
//            }
        }
    }
    private fun navigateToActivity(activity: String, orderId: String) {
        when (activity) {
            "DriverOrderActivity" -> {
                val intent = Intent(this, DriverOrderListActivity::class.java).apply {
                    putExtra("ORDER_ID", orderId)
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
            // Handle other activities if necessary
        }
    }

    // Function to display a notification
    private fun showNotification(title: String?, message: String?) {
        val notificationId = System.currentTimeMillis().toInt() // Unique notification ID

        // Create an intent to launch the app when the notification is tapped
        val intent = Intent(this, LoginActivity::class.java) // Change to the activity you want to open
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Notification channel ID (required for Android 8.0 and above)
        val channelId = "fcm_default_channel"

        // Create a NotificationManager instance
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create a notification channel for Android 8.0 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "FCM Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for FCM notifications"
                enableLights(true)
                lightColor = Color.RED
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Build the notification
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification) // Replace with your app's notification icon
            .setContentTitle(title ?: "Notification")
            .setContentText(message ?: "You have a new message")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        // Show the notification, ensuring we have POST_NOTIFICATIONS permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(this).notify(notificationId, notificationBuilder.build())
            } else {
                Log.w(TAG, "Notification permission not granted.")
            }
        } else {
            NotificationManagerCompat.from(this).notify(notificationId, notificationBuilder.build())
        }
    }
}
