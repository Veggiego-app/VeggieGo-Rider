package com.veggiego.rider

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        // ✅ LOCATION PERMISSION

        ActivityCompat.requestPermissions(

            this,

            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            ),

            1
        )

        // ✅ NOTIFICATION PERMISSION

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            ActivityCompat.requestPermissions(

                this,

                arrayOf(
                    Manifest.permission.POST_NOTIFICATIONS
                ),

                1
            )
        }

        // ✅ TEST NOTIFICATION

        val channelId =
            "rider_channel"

        val manager =
            getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(

                    channelId,

                    "VeggieGo Rider",

                    NotificationManager
                        .IMPORTANCE_HIGH
                )

            manager.createNotificationChannel(
                channel
            )
        }

        val builder =

            NotificationCompat.Builder(
                this,
                channelId
            )

                .setSmallIcon(
                    android.R.drawable.ic_dialog_info
                )

                .setContentTitle(
                    "🚚 VeggieGo Rider"
                )

                .setContentText(
                    "Rider App Ready"
                )

        manager.notify(
            1,
            builder.build()
        )

        // ✅ UI

        setContent {

            var isLoggedIn by remember {

                mutableStateOf(
                    FirebaseAuth.getInstance().currentUser != null
                )

            }

            if (isLoggedIn) {

                RiderMainScreen()

            } else {

                LoginScreen(

                    onLoginSuccess = {

                        isLoggedIn = true

                    }

                )
            }
        }
    }
}