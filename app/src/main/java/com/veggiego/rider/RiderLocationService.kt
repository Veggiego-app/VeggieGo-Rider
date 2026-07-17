package com.veggiego.rider

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.IBinder
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth

class RiderLocationService : Service() {

    private lateinit var fusedLocationClient:
            FusedLocationProviderClient

    private val db =
        FirebaseFirestore.getInstance()
    private val auth =
        FirebaseAuth.getInstance()

    override fun onStartCommand(

        intent: Intent?,

        flags: Int,

        startId: Int

    ): Int {
        createNotificationChannel()

        val notification =

            androidx.core.app.NotificationCompat.Builder(
                this,
                "rider_tracking"
            )

                .setSmallIcon(
                    android.R.drawable.ic_dialog_map
                )

                .setContentTitle(
                    "🟢 VeggieGo Rider Online"
                )

                .setContentText(
                    "Live location sharing active"
                )

                .setOngoing(true)

                .build()

        startForeground(
            1001,
            notification
        )
        fusedLocationClient =

            LocationServices
                .getFusedLocationProviderClient(
                    this
                )

        startLocationUpdates()

        return START_STICKY
    }

    private fun startLocationUpdates() {

        val locationRequest =

            LocationRequest.Builder(

                Priority.PRIORITY_HIGH_ACCURACY,

                10000

            ).build()

        val callback =

            object : LocationCallback() {

                override fun onLocationResult(

                    result: LocationResult

                ) {

                    for (location in result.locations) {

                        updateLocation(location)
                    }
                }
            }

        if (

            ActivityCompat.checkSelfPermission(

                this,

                Manifest.permission.ACCESS_FINE_LOCATION

            )

            != PackageManager.PERMISSION_GRANTED

        ) {

            return
        }

        fusedLocationClient.requestLocationUpdates(

            locationRequest,

            callback,

            mainLooper
        )
    }

    private fun updateLocation(

        location: Location

    ) {

        db.collection("riders")

            .document(
                auth.currentUser?.uid ?: return
            )

            .set(

                hashMapOf(

                    "lat" to location.latitude,

                    "lng" to location.longitude
                ),

                com.google.firebase.firestore.SetOptions.merge()
            )
    }
    private fun createNotificationChannel() {

        if (

            android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.O

        ) {

            val channel =

                android.app.NotificationChannel(

                    "rider_tracking",

                    "Rider Tracking",

                    android.app.NotificationManager
                        .IMPORTANCE_LOW
                )

            val manager =

                getSystemService(

                    android.app.NotificationManager::class.java

                )

            manager.createNotificationChannel(
                channel
            )
        }
    }
    override fun onBind(
        intent: Intent?
    ): IBinder? {

        return null
    }
}