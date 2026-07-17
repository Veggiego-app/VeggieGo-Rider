package com.veggiego.rider

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseService :

    FirebaseMessagingService() {

    // ✅ SAVE TOKEN

    override fun onNewToken(
        token: String
    ) {

        super.onNewToken(token)

        val uid =

            FirebaseAuth
                .getInstance()
                .currentUser
                ?.uid ?: return

        FirebaseFirestore
            .getInstance()
            .collection("riders")
            .document(uid)

            .set(

                mapOf(
                    "fcmToken" to token
                ),

                com.google.firebase.firestore.SetOptions.merge()
            )
    }

    // ✅ RECEIVE MESSAGE

    override fun onMessageReceived(

        message: RemoteMessage

    ) {

        super.onMessageReceived(message)

        val title =

            message.data["title"]

                ?: message.notification?.title

                ?: "VeggieGo Rider"

        val body =

            message.data["body"]

                ?: message.notification?.body

                ?: "New Message"

        showNotification(
            title,
            body
        )
    }

    private fun showNotification(

        title: String,

        body: String

    ) {

        val channelId =
            "rider_chat"

        val manager =

            getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        // ✅ CHANNEL

        if (

            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O

        ) {

            val soundUri =

                android.net.Uri.parse(
                    "android.resource://$packageName/raw/new_order"
                )

            val channel =

                NotificationChannel(

                    channelId,

                    "Rider Orders",

                    NotificationManager.IMPORTANCE_HIGH
                )

            channel.enableVibration(true)

            channel.vibrationPattern =
                longArrayOf(
                    0,
                    1000,
                    500,
                    1000,
                    500,
                    1000
                )

            channel.setSound(

                soundUri,

                android.media.AudioAttributes
                    .Builder()
                    .setUsage(
                        android.media.AudioAttributes
                            .USAGE_NOTIFICATION_RINGTONE
                    )
                    .build()
            )

            manager.createNotificationChannel(
                channel
            )
        }

        // ✅ OPEN APP

        val intent =

            Intent(
                this,
                MainActivity::class.java
            )

        val pendingIntent =

            PendingIntent.getActivity(

                this,

                0,

                intent,

                PendingIntent.FLAG_IMMUTABLE
            )

        // ✅ NOTIFICATION

        val soundUri =

            android.net.Uri.parse(
                "android.resource://$packageName/raw/new_order"
            )

        val notification =

            NotificationCompat.Builder(

                this,

                channelId
            )

                .setSmallIcon(
                    android.R.drawable.ic_dialog_info
                )

                .setContentTitle(
                    title
                )

                .setContentText(
                    body
                )

                .setPriority(
                    NotificationCompat.PRIORITY_HIGH
                )

                .setDefaults(
                    NotificationCompat.DEFAULT_ALL
                )

                .setVisibility(
                    NotificationCompat.VISIBILITY_PUBLIC
                )

                .setSound(soundUri)

                .setVibrate(

                    longArrayOf(

                        0,
                        1000,
                        500,
                        1000,
                        500,
                        1000
                    )
                )

                .setAutoCancel(true)
                .setContentIntent(
                    pendingIntent
                )

                .build()

        manager.notify(

            System.currentTimeMillis()
                .toInt(),

            notification
        )
    }
}