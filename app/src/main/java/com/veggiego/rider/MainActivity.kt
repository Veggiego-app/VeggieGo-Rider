package com.veggiego.rider

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestRuntimePermissions()

        setContent {
            var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }

            if (isLoggedIn) {
                LaunchedEffect(Unit) { ensureTokenAndTrackingState() }
                RiderMainScreen()
            } else {
                LoginScreen(
                    onLoginSuccess = {
                        isLoggedIn = true
                        ensureTokenAndTrackingState()
                    }
                )
            }
        }
    }

    fun startRiderLocationService() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val intent = Intent(this, RiderLocationService::class.java).apply {
            action = RiderLocationService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
        else startService(intent)
    }

    fun stopRiderLocationService() {
        stopService(Intent(this, RiderLocationService::class.java))
    }

    private fun ensureTokenAndTrackingState() {
        val uid = auth.currentUser?.uid ?: return
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            db.collection("riders").document(uid).set(
                mapOf("fcmToken" to token),
                SetOptions.merge()
            )
        }

        db.collection("riders").document(uid).get().addOnSuccessListener { rider ->
            val online = rider.getBoolean("online") == true
            val activeCount = (rider.getLong("activeOrderCount") ?: 0L).toInt()
            val hasActive = activeCount > 0 || !rider.getString("activeOrderId").isNullOrBlank()
            if (online || hasActive) startRiderLocationService() else stopRiderLocationService()
        }
    }

    private fun requestRuntimePermissions() {
        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions += Manifest.permission.POST_NOTIFICATIONS
        }
        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 100)
    }
}
