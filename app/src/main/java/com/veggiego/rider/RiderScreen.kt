package com.veggiego.rider

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.delay
import com.google.firebase.firestore.FieldValue
import android.location.Location

@Composable
fun RiderScreen() {

    val db =
        FirebaseFirestore.getInstance()

    val context =
        LocalContext.current

    val auth =
        FirebaseAuth.getInstance()

    val riderId =
        auth.currentUser?.uid ?: ""
    var riderName by remember {
        mutableStateOf("")
    }

    var riderPhone by remember {
        mutableStateOf("")
    }

    var riderZone by remember {
        mutableStateOf("")
    }

    var orders by remember {

        mutableStateOf(
            listOf<RiderOrder>()
        )
    }

    var todayEarnings by remember {
        mutableStateOf(0)
    }

    var deliveredOrders by remember {
        mutableStateOf(0)
    }
    var paymentReceived by remember {
        mutableStateOf(false)
    }

    var cashCollected by remember {
        mutableStateOf(false)
    }

    var onlineSeconds by remember {
        mutableStateOf(0)
    }

    var isOnline by remember {
        mutableStateOf(true)
    }

    var ordersListener by remember {
        mutableStateOf<ListenerRegistration?>(null)
    }

    var riderListener by remember {
        mutableStateOf<ListenerRegistration?>(null)
    }

    var forceLogout by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(riderId) {

        if (riderId.isNotEmpty()) {

            db.collection("riders")
                .document(riderId)
                .get()
                .addOnSuccessListener {

                    riderName =
                        it.getString("name") ?: ""

                    riderPhone =
                        it.getString("phone") ?: ""

                    riderZone =
                        it.getString("zone") ?: ""
                }
        }
    }

    // ✅ TIMER

    LaunchedEffect(Unit) {

        while (true) {

            delay(1000)

            onlineSeconds++
        }
    }

    // ✅ REALTIME ORDERS

    LaunchedEffect(Unit) {
        riderListener =

            db.collection("riders")

                .document(riderId)

                .addSnapshotListener { riderDoc, _ ->

                if (riderDoc == null)
                    return@addSnapshotListener

                val status =

                    riderDoc.getString("status") ?: ""
                ordersListener?.remove()
                ordersListener = null

                if (status != "APPROVED") {
                    if (forceLogout)
                        return@addSnapshotListener

                    forceLogout = true

                    ordersListener?.remove()
                    ordersListener = null

                    Toast.makeText(

                        context,

                        "❌ Your Account Is Not Approved",

                        Toast.LENGTH_LONG

                    ).show()

                    auth.signOut()

                    val intent =

                        Intent(

                            context,

                            MainActivity::class.java

                        )

                    intent.flags =

                        Intent.FLAG_ACTIVITY_NEW_TASK or

                                Intent.FLAG_ACTIVITY_CLEAR_TASK

                    context.startActivity(intent)

                    return@addSnapshotListener

                }

                ordersListener =

                    db.collection("orders")

                        .orderBy(
                            "timestamp",
                            Query.Direction.DESCENDING
                        )

                        .addSnapshotListener { value, error ->

                if (error != null) {

                    return@addSnapshotListener
                }

                if (value != null) {

                    orders =

                        value.documents.mapNotNull {

                            try {

                                val firestoreRiderId =

                                    it.getString(
                                        "riderId"
                                    ) ?: ""

                                val firestoreStatus =

                                    it.getString(
                                        "status"
                                    ) ?: ""

                                val restaurantZone =
                                    it.getString("restaurantZone") ?: ""

                                // ✅ SHOW ORDER IF:
                                // READY_FOR_PICKUP
                                // OR assigned to this rider

                                if (

                                    (
                                            firestoreStatus == "READY_FOR_PICKUP"
                                                    &&
                                                    firestoreRiderId.isEmpty()
                                                    &&
                                                    restaurantZone == riderZone
                                            )

                                    ||

                                    (
                                            firestoreRiderId ==
                                                    riderId

                                                    &&

                                                    firestoreStatus !=
                                                    "DELIVERED"

                                                    &&

                                                    firestoreStatus !=
                                                    "CANCELLED"
                                            )

                                ) {

                                    RiderOrder(

                                        id = it.id,

                                        customerName =
                                            it.getString(
                                                "customerName"
                                            ) ?: "",

                                        customerAddress =
                                            buildString {

                                                append(
                                                    it.getString("house") ?: ""
                                                )

                                                append(", ")

                                                append(
                                                    it.getString("area") ?: ""
                                                )

                                                append(", ")

                                                append(
                                                    it.getString("city") ?: ""
                                                )
                                            },
                                        total =
                                            it.getLong("total")
                                                ?.toInt()
                                                ?: 0,

                                        customerPhone =
                                            it.getString(
                                                "customerPhone"
                                            ) ?: "",

                                        status =
                                            firestoreStatus,

                                        riderId =
                                            firestoreRiderId,

                                        riderName =
                                            it.getString(
                                                "riderName"
                                            ) ?: "",

                                        restaurantName =
                                            it.getString(
                                                "restaurantName"
                                            ) ?: "",

                                        deliveryStatus =
                                            firestoreStatus,
                                        customerLat =
                                            it.getDouble(
                                                "customerLat"
                                            ) ?: 0.0,

                                        customerLng =
                                            it.getDouble(
                                                "customerLng"
                                            ) ?: 0.0,

                                        restaurantLat =
                                            it.getDouble(
                                                "restaurantLat"
                                            ) ?: 0.0,

                                        restaurantLng =
                                            it.getDouble(
                                                "restaurantLng"
                                            ) ?: 0.0,
                                        cashCollected =
                                            it.getBoolean(
                                                "cashCollected"
                                            ) ?: false,

                                        paymentReceived =
                                            it.getBoolean(
                                                "paymentReceived"
                                            ) ?: false,

                                        riderPay =
                                            it.getLong("riderPay")
                                                ?.toInt()
                                                ?: 0,
                                    )


                                } else {

                                    null
                                }

                            } catch (e: Exception) {

                                null
                            }
                        }.sortedByDescending { order ->

                            when {

                                order.riderId == riderId &&
                                        order.deliveryStatus == "OUT_FOR_DELIVERY" -> 100

                                order.riderId == riderId &&
                                        order.deliveryStatus == "PICKED_UP" -> 90

                                order.riderId == riderId &&
                                        order.deliveryStatus == "RIDER_ASSIGNED" -> 80

                                else -> 10
                            }
                        }
                    }
                }
            }


        // ✅ EARNINGS

        db.collection("orders")

            .whereEqualTo(
                "deliveryStatus",
                "DELIVERED"
            )

            .whereEqualTo(
                "riderId",
                riderId
            )

            .addSnapshotListener { value, _ ->

                if (value != null) {

                    deliveredOrders =
                        value.documents.size

                    todayEarnings = 0

                    value.documents.forEach {

                        todayEarnings +=

                            it.getLong("riderPay")
                                ?.toInt()
                                ?: 0
                    }
                }
            }
    }
    DisposableEffect(Unit) {

        onDispose {

            ordersListener?.remove()
            ordersListener = null

            riderListener?.remove()
            riderListener = null
        }
    }
    LazyColumn(

        modifier =
            Modifier.fillMaxSize()

    ) {

        // ✅ ORDERS

        items(orders) { order ->

            Card(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp)

            ) {

                Column(

                    modifier =
                        Modifier.padding(16.dp)

                ) {

                    Text(

                        text =
                            "🧾 Order ID: ${order.id}",

                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )
                    var pickupDistanceKm = 0.0
                    var dropDistanceKm = 0.0
                    var tripDistanceKm = 0.0
                    var riderEarning = 25

                    if (
                        order.restaurantLat != 0.0 &&
                        order.restaurantLng != 0.0 &&
                        order.customerLat != 0.0 &&
                        order.customerLng != 0.0
                    ) {

                        val result = FloatArray(1)

                        Location.distanceBetween(
                            order.restaurantLat,
                            order.restaurantLng,
                            order.customerLat,
                            order.customerLng,
                            result
                        )

                        dropDistanceKm =
                            result[0] / 1000.0

                        pickupDistanceKm = 0.5

                        tripDistanceKm =
                            pickupDistanceKm + dropDistanceKm
                    }
                    riderEarning =
                        order.riderPay

                    if (order.status == "READY_FOR_PICKUP") {

                        Text(
                            text = "🆕 New Order!",
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Text(
                            "🛣 Trip Distance : %.1f km"
                                .format(tripDistanceKm)
                        )

                        Text(
                            "💰 Rider Earning : ₹$riderEarning"
                        )

                        Text(
                            "📍 Pickup Distance : %.1f km"
                                .format(pickupDistanceKm)
                        )

                        Text(
                            "🏁 Drop Distance : %.1f km"
                                .format(dropDistanceKm)
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Text(
                            text = "🍴 PICKUP FROM",
                            fontWeight = FontWeight.Bold
                        )

                        Text(order.restaurantName)

                    } else {

                        Text("👤 ${order.customerName}")

                        Text("📍 ${order.customerAddress}")

                        Text("📞 ${order.customerPhone}")

                        Text("📌 ${order.deliveryStatus}")
                    }

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    // ✅ CALL

                    if (
                        order.deliveryStatus != "READY_FOR_PICKUP" &&

                        order.deliveryStatus != "DELIVERED" &&

                            order.deliveryStatus != "CANCELLED"
                    ) {

                        Button(

                            onClick = {

                                val intent =

                                    Intent(

                                        Intent.ACTION_DIAL,

                                        Uri.parse(
                                            "tel:${order.customerPhone}"
                                        )
                                    )

                                context.startActivity(
                                    intent
                                )
                            },

                            modifier =
                                Modifier.fillMaxWidth()

                        ) {

                            Text(
                                "📞 Call Customer"
                            )
                        }

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )
                    }

                    // ✅ ACCEPT

                    Button(

                        onClick = {

                            db.collection("orders")

                                .document(order.id)

                                .get()

                                .addOnSuccessListener {

                                    val currentRiderId =

                                        it.getString(
                                            "riderId"
                                        ) ?: ""

                                    if (

                                        currentRiderId.isEmpty()

                                        &&

                                        (
                                                it.getString("status")
                                                        ==
                                                        "READY_FOR_PICKUP"
                                                )

                                    ) {

                                        db.collection("orders")
                                            .document(order.id)

                                            .update(

                                                mapOf(

                                                    "status" to "RIDER_ASSIGNED",

                                                    "riderAssigned" to true,

                                                    "riderId" to riderId,

                                                    "riderName" to riderName,

                                                    "riderPhone" to riderPhone,

                                                    "navigationStage" to "TO_RESTAURANT"
                                                )
                                            )
                                        db.collection("riders")
                                            .document(riderId)
                                            .get()
                                            .addOnSuccessListener { riderDoc ->

                                                val currentActiveOrder =

                                                    riderDoc.getString(
                                                        "activeOrderId"
                                                    ) ?: ""

                                                if (
                                                    currentActiveOrder.isEmpty()
                                                ) {

                                                    db.collection("riders")
                                                        .document(riderId)
                                                        .update(

                                                            mapOf(

                                                                "activeOrderId"
                                                                        to
                                                                        order.id,

                                                                "online"
                                                                        to
                                                                        true
                                                            )
                                                        )
                                                }
                                            }

                                        Toast.makeText(

                                            context,

                                            "✅ Order Accepted",

                                            Toast.LENGTH_SHORT

                                        ).show()

                                    } else {

                                        Toast.makeText(

                                            context,

                                            "❌ Already Accepted By Another Rider",

                                            Toast.LENGTH_LONG

                                        ).show()
                                    }
                                }
                        },

                        enabled =
                            order.status ==
                                    "READY_FOR_PICKUP",

                        modifier =
                            Modifier.fillMaxWidth()

                    ) {

                        Text(
                            "✅ Accept Order"
                        )

                    }
                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    OutlinedButton(

                        onClick = {

                            db.collection("orders")
                                .document(order.id)
                                .update(

                                    mapOf(

                                        "status"
                                                to
                                                "READY_FOR_PICKUP",

                                        "riderId"
                                                to
                                                "",

                                        "riderName"
                                                to
                                                "",

                                        "riderPhone"
                                                to
                                                ""
                                    )
                                )

                            Toast.makeText(

                                context,

                                "❌ Order Rejected",

                                Toast.LENGTH_SHORT

                            ).show()
                        },

                        enabled = false,

                        modifier =
                            Modifier.fillMaxWidth()

                    ) {

                        Text(
                            "❌ Reject Order"
                        )
                    }
                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    // ✅ PICKED UP

                    Button(

                        onClick = {

                            db.collection("orders")
                                .document(order.id)
                                .update(

                                    mapOf(

                                        "deliveryStatus" to "PICKED_UP",

                                        "status" to "PICKED_UP",

                                        "navigationStage" to "TO_CUSTOMER"
                                    )
                                )

                            Toast.makeText(

                                context,

                                "📦 Picked Up",

                                Toast.LENGTH_SHORT

                            ).show()
                        },

                        enabled =

                            order.deliveryStatus ==
                                    "RIDER_ASSIGNED"

                                    &&

                                    order.riderId ==
                                    riderId,

                        modifier =
                            Modifier.fillMaxWidth()

                    ) {

                        Text(
                            "📦 Picked Up"
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    // ✅ START DELIVERY

                    Button(

                        onClick = {

                            db.collection("orders")
                                .document(order.id)
                                .update(

                                    mapOf(

                                        "deliveryStatus" to "OUT_FOR_DELIVERY",

                                        "status" to "OUT_FOR_DELIVERY",

                                        "navigationStage" to "OUT_FOR_DELIVERY"
                                    )
                                )

                            Toast.makeText(

                                context,

                                "🚚 Out For Delivery",

                                Toast.LENGTH_SHORT

                            ).show()
                        },

                        enabled =

                            order.deliveryStatus ==
                                    "PICKED_UP"

                                    &&

                                    order.riderId ==
                                    riderId,

                        modifier =
                            Modifier.fillMaxWidth()

                    ) {

                        Text(
                            "🚚 Start Delivery"
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )
                    if (
                        order.deliveryStatus ==
                        "OUT_FOR_DELIVERY"
                    ) {

                        if (

                            order.paymentReceived

                            ||

                            order.cashCollected

                        ){

                            Text(
                                "✅ PAYMENT RECEIVED",
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold
                            )

                        } else {
                            Text(
                                text =
                                    "💰 Customer Amount ₹${order.total}",
                                fontWeight =
                                    FontWeight.Bold,
                                color =
                                    Color(0xFFD84315)
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(8.dp)
                            )

                            Row(
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Checkbox(

                                    checked =
                                        order.cashCollected,

                                    onCheckedChange = { checked ->

                                        if (
                                            order.riderId != riderId
                                        ) return@Checkbox

                                        if (checked) {

                                            db.collection("orders")
                                                .document(order.id)
                                                .update(

                                                    mapOf(

                                                        "cashCollected" to true,

                                                        "cashCollectedAt" to
                                                                System.currentTimeMillis(),

                                                        "cashCollectedBy" to
                                                                riderId
                                                    )
                                                )

                                        } else {

                                            db.collection("orders")
                                                .document(order.id)
                                                .update(

                                                    mapOf(

                                                        "cashCollected" to false,

                                                        "cashCollectedAt" to null,

                                                        "cashCollectedBy" to ""
                                                    )
                                                )
                                        }
                                    }
                                )
                                Text(
                                    "💵 Cash Collected"
                                )
                            }
                        }

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )
                    }

                    // ✅ DELIVERED

                    Button(

                        onClick = {

                            db.collection("orders")
                                .document(order.id)
                                .update(
                                    mapOf(
                                        "deliveryStatus" to "DELIVERED",
                                        "status" to "DELIVERED",
                                        "riderAssigned" to false
                                    )
                                )

                            db.collection("orders")

                                .whereEqualTo(
                                    "riderId",
                                    riderId
                                )

                                .get()

                                .addOnSuccessListener { result ->

                                    val nextOrder =

                                        result.documents.firstOrNull {

                                            val status =
                                                it.getString(
                                                    "deliveryStatus"
                                                ) ?: ""

                                            it.id != order.id

                                                    &&

                                                    status != "DELIVERED"

                                                    &&

                                                    status != "CANCELLED"
                                        }

                                    db.collection("riders")
                                        .document(riderId)
                                        .update(

                                            mapOf(

                                                "activeOrderId" to
                                                        (
                                                                nextOrder?.id
                                                                    ?: ""
                                                                ),

                                                "totalDeliveries" to
                                                        FieldValue.increment(1),

                                                "earnings" to
                                                        FieldValue.increment(
                                                            riderEarning.toLong()
                                                        )
                                            )
                                        )
                                }

                            Toast.makeText(

                                context,

                                "✅ Delivered",

                                Toast.LENGTH_SHORT

                            ).show()
                        },

                        enabled =

                            order.deliveryStatus ==
                                    "OUT_FOR_DELIVERY"

                                    &&

                                    order.riderId ==
                                    riderId

                                    &&

                                    (

                                            order.paymentReceived

                                                    ||

                                                    order.cashCollected

                                            ),

                        modifier =
                            Modifier.fillMaxWidth()

                    ) {

                        Text(
                            "✅ Delivered"
                        )
                    }

                    // ✅ CHAT

                    if (

                        order.deliveryStatus != "READY_FOR_PICKUP" &&

                        order.deliveryStatus != "DELIVERED" &&

                        order.deliveryStatus != "CANCELLED"

                    ) {

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )

                        Button(

                            onClick = {

                                val intent =

                                    Intent(

                                        context,

                                        RiderChatActivity::class.java
                                    )

                                intent.putExtra(
                                    "orderId",
                                    order.id
                                )

                                context.startActivity(
                                    intent
                                )
                            },

                            modifier =
                                Modifier.fillMaxWidth()

                        ) {

                            Text(
                                "💬 Chat"
                            )
                        }
                    }
                }
            }
        }
    }
}