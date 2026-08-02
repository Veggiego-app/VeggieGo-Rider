package com.veggiego.rider

import android.app.Activity
import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Space
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import java.util.Locale
import kotlin.math.ceil

class NewOrderPopupActivity : Activity() {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private var player: MediaPlayer? = null
    private var orderListener: ListenerRegistration? = null
    private var orderId = ""
    private var manualAssignment = false
    private lateinit var acceptButton: Button
    private lateinit var rejectButton: Button
    private lateinit var requestTitleText: TextView
    private lateinit var earningText: TextView
    private lateinit var distanceText: TextView
    private lateinit var restaurantText: TextView
    private lateinit var addressText: TextView
    private lateinit var etaText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        orderId = intent.getStringExtra("orderId").orEmpty()
        manualAssignment = intent.getBooleanExtra("manualAssignment", false)
        if (orderId.isBlank() || auth.currentUser == null) {
            finish()
            return
        }

        showOverLockScreen()
        buildScreen()
        updateRequestTitle()
        startSound()
        watchOrder()
    }

    private fun showOverLockScreen() {
        window.statusBarColor = Color.WHITE
        window.navigationBarColor = Color.WHITE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager)
                .requestDismissKeyguard(this, null)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }

        val header = FrameLayout(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(18, 211, 103), Color.rgb(5, 151, 76))
            )
        }
        val headerText = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(32), dp(20), dp(14))
        }
        requestTitleText = label("New Order", 31f, Typeface.BOLD, Gravity.CENTER).apply {
            setTextColor(Color.WHITE)
        }
        headerText.addView(requestTitleText)
        headerText.addView(label("Delivery request", 16f, Typeface.NORMAL, Gravity.CENTER).apply {
            setTextColor(Color.WHITE)
            alpha = 0.92f
            setPadding(0, dp(5), 0, 0)
        })
        header.addView(
            headerText,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        rejectButton = Button(this).apply {
            text = "✕  Reject"
            textSize = 17f
            isAllCaps = false
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            background = rounded(Color.BLACK, 32f)
            setPadding(dp(22), 0, dp(22), 0)
            setOnClickListener { rejectOrder() }
        }
        header.addView(
            rejectButton,
            FrameLayout.LayoutParams(dp(128), dp(52), Gravity.TOP or Gravity.END).apply {
                topMargin = dp(14)
                marginEnd = dp(14)
            }
        )
        root.addView(
            header,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 0.22f)
        )

        val detailsScroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
            setBackgroundColor(Color.WHITE)
        }
        val details = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(0), dp(22), dp(20))
        }

        details.addView(label("Estimated earnings", 19f, Typeface.BOLD, Gravity.CENTER).apply {
            setPadding(0, dp(22), 0, 0)
        })
        earningText = label("₹--", 43f, Typeface.BOLD, Gravity.CENTER)
        details.addView(earningText)
        distanceText = label("Pickup: -- km  |  Drop: -- km", 17f, Typeface.NORMAL, Gravity.CENTER)
        details.addView(distanceText)

        details.addView(divider(), LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply {
            topMargin = dp(20)
            bottomMargin = dp(20)
        })

        details.addView(TextView(this).apply {
            text = "Pick up"
            textSize = 14f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            background = rounded(Color.rgb(80, 80, 80), 5f)
            setPadding(dp(10), dp(6), dp(10), dp(6))
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        restaurantText = label(
            intent.getStringExtra("restaurantName") ?: "Restaurant",
            21f,
            Typeface.BOLD,
            Gravity.START
        ).apply { setPadding(0, dp(12), 0, 0) }
        details.addView(restaurantText)

        addressText = label("Loading pickup address…", 17f, Typeface.NORMAL, Gravity.START).apply {
            setTextColor(Color.DKGRAY)
            setPadding(0, dp(8), 0, 0)
        }
        details.addView(addressText)

        etaText = label("◷ Calculating pickup time…", 16f, Typeface.NORMAL, Gravity.START).apply {
            setTextColor(Color.DKGRAY)
            setPadding(0, dp(12), 0, 0)
        }
        details.addView(etaText)
        details.addView(Space(this), LinearLayout.LayoutParams(1, 0, 1f))

        acceptButton = Button(this).apply {
            text = "Accept order"
            textSize = 19f
            isAllCaps = false
            setTextColor(Color.BLACK)
            typeface = Typeface.DEFAULT_BOLD
            background = rounded(Color.rgb(18, 211, 103), 34f)
            setOnClickListener { acceptOrder() }
        }
        details.addView(
            acceptButton,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(68)).apply {
                topMargin = dp(22)
            }
        )

        detailsScroll.addView(
            details,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        root.addView(
            detailsScroll,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 0.78f)
        )
        setContentView(root)
    }

    private fun watchOrder() {
        val uid = auth.currentUser?.uid ?: return
        orderListener = db.collection("orders").document(orderId)
            .addSnapshotListener { snap, error ->
                if (error != null || snap == null || !snap.exists()) {
                    if (error != null) closePopup()
                    return@addSnapshotListener
                }

                val status = snap.getString("status").orEmpty()
                val assigned = snap.getString("riderId").orEmpty()
                val rejected = (snap.get("rejectedRiderIds") as? List<*>)?.contains(uid) == true
                val manualStillPending = manualAssignment &&
                        snap.getString("pendingRiderId") == uid &&
                        snap.getString("pendingRiderRequestStatus") == "PENDING"
                val automaticStillOpen = !manualAssignment &&
                        status == "READY_FOR_PICKUP" && assigned.isBlank()

                if (rejected || (!manualStillPending && !automaticStillOpen)) {
                    closePopup()
                    return@addSnapshotListener
                }

                updateOrderDetails(snap)
            }
    }

    private fun updateRequestTitle() {
        if (!manualAssignment) {
            requestTitleText.text = "New Order"
            return
        }
        val uid = auth.currentUser?.uid ?: return
        db.collection("riders").document(uid).get().addOnSuccessListener { rider ->
            val activeIds = (rider.get("activeOrderIds") as? List<*>)
                ?.mapNotNull { it as? String } ?: emptyList()
            val activeCount = maxOf(
                activeIds.size,
                (rider.getLong("activeOrderCount") ?: 0L).toInt(),
                if (rider.getString("activeOrderId").isNullOrBlank()) 0 else 1
            )
            requestTitleText.text = if (activeCount > 0) "Additional Order" else "New Order"
        }
    }

    private fun updateOrderDetails(order: DocumentSnapshot) {
        val pickupKm = firstNumber(
            order,
            "pickupDistance",
            "pickupDistanceKm"
        ).takeIf { it > 0.0 }
            ?: intent.getStringExtra("distanceKm")?.toDoubleOrNull()
            ?: 0.0
        var dropKm = firstNumber(order, "dropDistance", "deliveryDistance", "dropDistanceKm")
        if (dropKm <= 0.0) {
            val restaurantLat = firstNumber(order, "restaurantLat", "restaurantLatitude")
            val restaurantLng = firstNumber(order, "restaurantLng", "restaurantLongitude")
            val customerLat = firstNumber(order, "customerLat", "deliveryLat", "latitude")
            val customerLng = firstNumber(order, "customerLng", "deliveryLng", "longitude")
            if (restaurantLat != 0.0 && restaurantLng != 0.0 &&
                customerLat != 0.0 && customerLng != 0.0
            ) {
                val result = FloatArray(1)
                Location.distanceBetween(
                    restaurantLat,
                    restaurantLng,
                    customerLat,
                    customerLng,
                    result
                )
                dropKm = result[0] / 1000.0
            }
        }
        val earning = firstNumber(order, "riderEarning", "riderPay", "estimatedRiderEarning")
        val restaurant = order.getString("restaurantName")
            ?: intent.getStringExtra("restaurantName")
            ?: "Restaurant"
        val address = listOfNotNull(
            order.getString("restaurantAddress"),
            order.getString("restaurantArea")
        ).firstOrNull { it.isNotBlank() }.orEmpty()
        val etaMinutes = if (pickupKm > 0.0) ceil((pickupKm / 25.0) * 60.0).toInt().coerceAtLeast(1) else 0

        earningText.text = "₹${formatAmount(earning)}"
        distanceText.text = "Pickup: ${formatKm(pickupKm)} km  |  Drop: ${formatKm(dropKm)} km"
        restaurantText.text = restaurant
        addressText.text = if (address.isNotBlank()) address else "Loading restaurant address…"
        etaText.text = if (etaMinutes > 0) "◷ $etaMinutes mins away" else "◷ Nearby pickup"
        if (address.isBlank()) loadRestaurantAddress(order)
    }

    private fun loadRestaurantAddress(order: DocumentSnapshot) {
        val restaurantId = order.getString("restaurantId").orEmpty()
        if (restaurantId.isBlank()) {
            addressText.text = "Restaurant address not available"
            return
        }
        db.collection("restaurants").document(restaurantId).get().addOnSuccessListener { restaurant ->
            if (isFinishing) return@addOnSuccessListener
            val savedAddress = listOfNotNull(
                restaurant.getString("address"),
                restaurant.getString("restaurantAddress")
            ).firstOrNull { it.isNotBlank() }.orEmpty()
            val fullAddress = savedAddress.ifBlank {
                listOfNotNull(
                    restaurant.getString("area"),
                    restaurant.getString("city")
                ).map { it.trim() }.filter { it.isNotBlank() }.distinct().joinToString(", ")
            }
            addressText.text = fullAddress.ifBlank { "Restaurant address not available" }
        }
    }

    private fun acceptOrder() {
        val uid = auth.currentUser?.uid ?: return
        stopSound()
        acceptButton.isEnabled = false
        rejectButton.isEnabled = false
        val orderRef = db.collection("orders").document(orderId)
        val riderRef = db.collection("riders").document(uid)

        db.runTransaction { transaction ->
            val order = transaction.get(orderRef)
            val rider = transaction.get(riderRef)
            val status = order.getString("status").orEmpty()
            val assigned = order.getString("riderId").orEmpty()
            val online = rider.getBoolean("online") == true
            val activeIds = (rider.get("activeOrderIds") as? List<*>)
                ?.mapNotNull { it as? String }?.toMutableList() ?: mutableListOf()
            val activeCount = maxOf(activeIds.size, (rider.getLong("activeOrderCount") ?: 0L).toInt())
            val manualForThisRider = order.getString("pendingRiderId") == uid
            val notifiedRiders = (order.get("notifiedRiderIds") as? List<*>)
                ?.mapNotNull { it as? String } ?: emptyList()

            val previousRiderRef = assigned.takeIf { it.isNotBlank() && it != uid }
                ?.let { db.collection("riders").document(it) }
            val previousRider = previousRiderRef?.let { transaction.get(it) }

            check(status == "READY_FOR_PICKUP" || manualForThisRider) { "Order is no longer available" }
            check(assigned.isBlank() || manualForThisRider) { "Order already accepted" }
            check(online) { "You are offline" }
            check(if (manualForThisRider) activeCount < 2 else activeCount == 0) {
                "Complete your current delivery first"
            }
            check(manualForThisRider || notifiedRiders.contains(uid)) {
                "This request is not available for you"
            }

            if (!activeIds.contains(orderId)) activeIds.add(orderId)
            transaction.update(orderRef, mapOf(
                "riderId" to uid,
                "riderName" to (rider.getString("name") ?: "Rider"),
                "riderPhone" to (rider.getString("phone") ?: ""),
                "riderAssigned" to true,
                "riderRequestStatus" to "ACCEPTED",
                "riderAcceptedAt" to FieldValue.serverTimestamp(),
                "dispatchStatus" to "ACCEPTED",
                "pendingRiderId" to "",
                "pendingRiderRequestStatus" to ""
            ))
            transaction.update(riderRef, mapOf(
                "activeOrderId" to (rider.getString("activeOrderId").takeUnless { it.isNullOrBlank() } ?: orderId),
                "activeOrderIds" to activeIds,
                "activeOrderCount" to activeIds.size,
                "availableForOrders" to false
            ))
            if (previousRiderRef != null && previousRider != null && previousRider.exists()) {
                val oldIds = (previousRider.get("activeOrderIds") as? List<*>)
                    ?.mapNotNull { it as? String }?.filter { it != orderId } ?: emptyList()
                transaction.set(previousRiderRef, mapOf(
                    "activeOrderIds" to oldIds,
                    "activeOrderCount" to oldIds.size,
                    "activeOrderId" to (oldIds.firstOrNull() ?: ""),
                    "availableForOrders" to
                            (previousRider.getBoolean("online") == true && oldIds.isEmpty())
                ), SetOptions.merge())
            }
            true
        }.addOnSuccessListener {
            Toast.makeText(this, "Order accepted", Toast.LENGTH_SHORT).show()
            closePopup()
        }.addOnFailureListener { error ->
            Toast.makeText(this, error.message ?: "Unable to accept order", Toast.LENGTH_LONG).show()
            closePopup()
        }
    }

    private fun rejectOrder() {
        val uid = auth.currentUser?.uid ?: return
        stopSound()
        acceptButton.isEnabled = false
        rejectButton.isEnabled = false
        db.collection("orders").document(orderId).update(
            mapOf(
                "rejectedRiderIds" to FieldValue.arrayUnion(uid),
                "lastRejectedAt" to FieldValue.serverTimestamp()
            )
        ).addOnCompleteListener { closePopup() }
    }

    private fun startSound() {
        player = MediaPlayer.create(this, R.raw.new_order)?.apply {
            isLooping = true
            start()
        }
    }

    private fun closePopup() {
        stopSound()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(orderId.hashCode())
        if (!isFinishing) finish()
    }

    private fun stopSound() {
        player?.runCatching { stop() }
        player?.release()
        player = null
    }

    private fun label(textValue: String, size: Float, style: Int, textGravity: Int) =
        TextView(this).apply {
            text = textValue
            textSize = size
            gravity = textGravity
            setTextColor(Color.BLACK)
            typeface = Typeface.create(Typeface.DEFAULT, style)
        }

    private fun divider() = View(this).apply { setBackgroundColor(Color.rgb(232, 232, 232)) }

    private fun rounded(
        fillColor: Int,
        radiusDp: Float,
        strokeColor: Int? = null,
        strokeWidthDp: Int = 0
    ) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(fillColor)
        cornerRadius = dp(radiusDp).toFloat()
        if (strokeColor != null && strokeWidthDp > 0) setStroke(dp(strokeWidthDp), strokeColor)
    }

    private fun firstNumber(doc: DocumentSnapshot, vararg fields: String): Double {
        for (field in fields) {
            val value = doc.get(field)
            if (value is Number) return value.toDouble()
            value?.toString()?.toDoubleOrNull()?.let { return it }
        }
        return 0.0
    }

    private fun formatKm(value: Double): String = String.format(Locale.US, "%.1f", value)

    private fun formatAmount(value: Double): String = if (value % 1.0 == 0.0) {
        value.toLong().toString()
    } else {
        String.format(Locale.US, "%.2f", value)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun dp(value: Float): Int = (value * resources.displayMetrics.density).toInt()

    override fun onBackPressed() {
        // Rider must explicitly Accept or Reject the request.
    }

    override fun onDestroy() {
        orderListener?.remove()
        stopSound()
        super.onDestroy()
    }
}