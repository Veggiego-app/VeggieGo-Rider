package com.veggiego.rider

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseService : FirebaseMessagingService() {

    companion object {
        const val ORDER_CHANNEL = "rider_orders_v2"
        const val ACTION_CLOSE_ORDER_POPUP = "com.veggiego.rider.CLOSE_ORDER_POPUP"
    }

    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
            db.collection("riders").document(uid)
                .set(mapOf("fcmToken" to token), SetOptions.merge())
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val type = message.data["type"].orEmpty()
        val orderId = message.data["orderId"].orEmpty()

        if (type == "CANCEL_ORDER_NOTIFICATION") {
            cancelOrderNotification(orderId)
            return
        }

        if (type == "RIDER_ORDER_REQUEST" && orderId.isNotBlank()) {
            verifyAndShowOrderRequest(orderId, message)
            return
        }

        showRegularNotification(
            message.notification?.title ?: message.data["title"] ?: "VeggieGo Rider",
            message.notification?.body ?: message.data["body"] ?: "New update"
        )
    }

    private fun verifyAndShowOrderRequest(orderId: String, message: RemoteMessage) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("riders").document(uid).get().addOnSuccessListener { rider ->
            val online = rider.getBoolean("online") == true
            val activeCount = (rider.getLong("activeOrderCount") ?: 0L).toInt()
            val manual = message.data["manualAssignment"] == "true"
            val capacityAllowed = if (manual) activeCount < 2 else activeCount == 0
            if (!online || !capacityAllowed) return@addOnSuccessListener

            db.collection("orders").document(orderId).get().addOnSuccessListener { order ->
                val status = order.getString("status").orEmpty()
                val assigned = order.getString("riderId").orEmpty()
                val pendingRiderId = order.getString("pendingRiderId").orEmpty()
                val validAutomatic = !manual && status == "READY_FOR_PICKUP" && assigned.isBlank()
                val validManual = manual && pendingRiderId == uid &&
                    order.getString("pendingRiderRequestStatus") == "PENDING"
                if (!validAutomatic && !validManual) return@addOnSuccessListener
                showOrderNotification(
                    orderId,
                    message.data["restaurantName"] ?: order.getString("restaurantName") ?: "Restaurant",
                    message.data["distanceKm"] ?: "Nearby",
                    manual
                )
            }
        }
    }

    private fun showOrderNotification(orderId: String, restaurant: String, distance: String, manual: Boolean) {
        createOrderChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val popupIntent = Intent(this, NewOrderPopupActivity::class.java).apply {
            putExtra("orderId", orderId)
            putExtra("restaurantName", restaurant)
            putExtra("distanceKm", distance)
            putExtra("manualAssignment", manual)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            orderId.hashCode(),
            popupIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val sound = Uri.parse("android.resource://$packageName/raw/new_order")
        val notification = NotificationCompat.Builder(this, ORDER_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("New delivery request")
            .setContentText("$restaurant • $distance km away")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setSound(sound)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .build()

        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(orderId.hashCode(), notification)
    }

    private fun cancelOrderNotification(orderId: String) {
        if (orderId.isBlank()) return
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(orderId.hashCode())
        sendBroadcast(Intent(ACTION_CLOSE_ORDER_POPUP).setPackage(packageName).putExtra("orderId", orderId))
    }

    private fun showRegularNotification(title: String, body: String) {
        createOrderChannel()
        val intent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, ORDER_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(intent)
            .build()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createOrderChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val sound = Uri.parse("android.resource://$packageName/raw/new_order")
            val audio = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build()
            val channel = NotificationChannel(
                ORDER_CHANNEL,
                "Rider order requests",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                setSound(sound, audio)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
