package com.veggiego.rider

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class RiderLocationService : Service() {

    companion object {
        const val ACTION_START = "com.veggiego.rider.START_TRACKING"
        const val ACTION_STOP = "com.veggiego.rider.STOP_TRACKING"
        private const val CHANNEL_ID = "rider_tracking"
        private const val NOTIFICATION_ID = 1001
        private const val ACTIVE_INTERVAL_MS = 15_000L
        private const val IDLE_INTERVAL_MS = 30_000L
        private const val ACTIVE_MIN_DISTANCE_M = 10f
        private const val IDLE_MIN_DISTANCE_M = 25f
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private var locationCallback: LocationCallback? = null
    private var lastUploadedLocation: Location? = null
    private var lastUploadedAt = 0L
    private var activeDelivery = false
    private var trackingStarted = false

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopTrackingAndSelf()
            return START_NOT_STICKY
        }

        val uid = auth.currentUser?.uid
        if (uid == null) {
            stopTrackingAndSelf()
            return START_NOT_STICKY
        }

        // Verify availability every time Android or the UI asks the service to start.
        db.collection("riders").document(uid).get()
            .addOnSuccessListener { rider ->
                val online = rider.getBoolean("online") == true
                val count = (rider.getLong("activeOrderCount") ?: 0L).toInt()
                activeDelivery = count > 0 || !rider.getString("activeOrderId").isNullOrBlank()
                if (!online && !activeDelivery) {
                    stopTrackingAndSelf()
                } else {
                    startForeground(NOTIFICATION_ID, trackingNotification(activeDelivery))
                    startLocationUpdates()
                }
            }
            .addOnFailureListener { stopTrackingAndSelf() }

        return START_STICKY
    }

    private fun startLocationUpdates() {
        if (trackingStarted || locationCallback != null) return
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            stopTrackingAndSelf()
            return
        }

        val interval = if (activeDelivery) ACTIVE_INTERVAL_MS else IDLE_INTERVAL_MS
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, interval)
            .setMinUpdateIntervalMillis(interval)
            .setMinUpdateDistanceMeters(
                if (activeDelivery) ACTIVE_MIN_DISTANCE_M else IDLE_MIN_DISTANCE_M
            )
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let(::maybeUploadLocation)
            }
        }
        fusedLocationClient.requestLocationUpdates(request, locationCallback!!, mainLooper)
        trackingStarted = true
    }

    private fun maybeUploadLocation(location: Location) {
        val uid = auth.currentUser?.uid ?: return
        val now = System.currentTimeMillis()
        val minInterval = if (activeDelivery) ACTIVE_INTERVAL_MS else IDLE_INTERVAL_MS
        val minDistance = if (activeDelivery) ACTIVE_MIN_DISTANCE_M else IDLE_MIN_DISTANCE_M
        val previous = lastUploadedLocation

        if (previous != null && now - lastUploadedAt < minInterval) return
        if (previous != null && previous.distanceTo(location) < minDistance) return

        lastUploadedLocation = Location(location)
        lastUploadedAt = now
        db.collection("riders").document(uid).set(
            mapOf(
                "lat" to location.latitude,
                "lng" to location.longitude,
                "locationUpdatedAt" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        )
    }

    private fun stopTrackingAndSelf() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        locationCallback = null
        trackingStarted = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun trackingNotification(active: Boolean) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentTitle("VeggieGo Rider Online")
            .setContentText(if (active) "Active delivery tracking" else "Nearby orders enabled")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Rider Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        locationCallback = null
        trackingStarted = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
