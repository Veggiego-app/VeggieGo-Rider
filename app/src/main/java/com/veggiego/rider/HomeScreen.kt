package com.veggiego.rider

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.firestore.FieldValue
import java.util.Calendar

data class HomeActiveOrder(

    val orderId: String = "",

    val restaurantName: String = "",

    val restaurantAddress: String = "",

    val customerName: String = "",

    val customerPhone: String = "",

    val customerArea: String = "",

    val house: String = "",

    val landmark: String = "",

    val city: String = "",

    val pincode: String = "",

    val status: String = "",

    val riderRequestStatus: String = "",

    val isRiderChangeRequest: Boolean = false,

    val oldRiderId: String = "",

    val pendingRiderRequestStatus: String = "",

    val navigationStage: String = "TO_RESTAURANT",

    val restaurantLat: Double = 0.0,

    val restaurantLng: Double = 0.0,

    val customerLat: Double = 0.0,

    val customerLng: Double = 0.0,

    val total: Int = 0,

    val paymentReceived: Boolean = false,

    val cashCollected: Boolean = false,

    val items: List<String> = emptyList()
)

@Composable
fun HomeScreen() {

    val db =
        FirebaseFirestore.getInstance()

    val riderId =
        FirebaseAuth
            .getInstance()
            .currentUser
            ?.uid ?: ""
    val context =
        LocalContext.current

    var earnings by remember {
        mutableStateOf(0)
    }

    var deliveries by remember {
        mutableStateOf(0)
    }

    var todayCash by remember {
        mutableStateOf(0)
    }

    var activeOrderId by remember {
        mutableStateOf("")
    }
    var activeStatus by remember {
        mutableStateOf("")
    }

    var activePhone by remember {
        mutableStateOf("")
    }
    var activeCustomerName by remember {
        mutableStateOf("")
    }

    var activeRestaurantName by remember {
        mutableStateOf("")
    }

    var activeItems by remember {
        mutableStateOf(listOf<String>())
    }

    var activeArea by remember {
        mutableStateOf("")
    }
    var restaurantAddress by remember {
        mutableStateOf("")
    }

    var navigationStage by remember {
        mutableStateOf("TO_RESTAURANT")
    }

    var restaurantLat by remember {
        mutableStateOf(0.0)
    }

    var restaurantLng by remember {
        mutableStateOf(0.0)
    }

    var house by remember {
        mutableStateOf("")
    }

    var city by remember {
        mutableStateOf("")
    }

    var landmark by remember {
        mutableStateOf("")
    }

    var pincode by remember {
        mutableStateOf("")
    }

    var activeTotal by remember {
        mutableStateOf(0)
    }
    var paymentReceived by remember {
        mutableStateOf(false)
    }

    var cashCollected by remember {
        mutableStateOf(false)
    }
    var customerLat by remember {
        mutableStateOf(0.0)
    }

    var customerLng by remember {
        mutableStateOf(0.0)
    }
    var isOnline by remember {
        mutableStateOf(true)
    }

    var activeOrders by remember {
        mutableStateOf<List<HomeActiveOrder>>(
            emptyList()
        )
    }

    var pendingOrders by remember {
        mutableStateOf<List<HomeActiveOrder>>(
            emptyList()
        )
    }

    var riderChangeRequests by remember {

        mutableStateOf<List<HomeActiveOrder>>(
            emptyList()
        )
    }

    LaunchedEffect(Unit) {

        db.collection("riders")

            .document(riderId)

            .addSnapshotListener { value, _ ->

                if (value != null) {
                    isOnline =
                        value.getBoolean(
                            "online"
                        ) ?: true

                    activeOrderId =
                        value.getString(
                            "activeOrderId"
                        ) ?: ""
                    if (activeOrderId.isNotEmpty()) {

                        db.collection("orders")
                            .document(activeOrderId)
                            .get()
                            .addOnSuccessListener {

                                val orderRiderId =
                                    it.getString("riderId")
                                        ?: ""

                                if (
                                    orderRiderId != riderId
                                ) {

                                    db.collection("riders")
                                        .document(riderId)
                                        .update(
                                            "activeOrderId",
                                            ""
                                        )

                                    activeOrderId = ""

                                    return@addOnSuccessListener
                                }
                            }
                    }
                    if (activeOrderId.isNotEmpty()) {

                        db.collection("orders")
                            .document(activeOrderId)
                            .addSnapshotListener { it, _ ->

                                if (it != null) {

                                    val status =
                                        it.getString("status")
                                            ?: ""

                                    if (
                                        status == "DELIVERED"
                                    ) {

                                        db.collection("riders")
                                            .document(riderId)
                                            .update(
                                                "activeOrderId",
                                                ""
                                            )

                                        activeOrderId = ""

                                        return@addSnapshotListener
                                    }

                                    activeStatus =
                                        it.getString("status")
                                            ?: ""

                                    activePhone =
                                        it.getString("customerPhone")
                                            ?: ""

                                    activeCustomerName =
                                        it.getString("customerName")
                                            ?: ""

                                    activeRestaurantName =
                                        it.getString("restaurantName")
                                            ?: ""
                                    activeItems =

                                        (it.get("items") as? List<Map<String, Any>>)
                                            ?.map { item ->

                                                val name =
                                                    item["name"]?.toString()
                                                        ?: ""

                                                val qty =
                                                    item["quantity"]?.toString()
                                                        ?: "1"

                                                "• $name × $qty"
                                            }

                                            ?: emptyList()

                                    restaurantAddress =
                                        it.getString("restaurantAddress")
                                            ?: ""

                                    navigationStage =
                                        it.getString("navigationStage")
                                            ?: "TO_RESTAURANT"

                                    restaurantLat =
                                        it.getDouble("restaurantLat")
                                            ?: 0.0

                                    restaurantLng =
                                        it.getDouble("restaurantLng")
                                            ?: 0.0

                                    house =
                                        it.getString("house")
                                            ?: ""

                                    city =
                                        it.getString("city")
                                            ?: ""

                                    landmark =
                                        it.getString("landmark")
                                            ?: ""

                                    pincode =
                                        it.getString("pincode")
                                            ?: ""

                                    activeArea =
                                        it.getString("area")
                                            ?: ""

                                    activeTotal =
                                        it.getLong("total")
                                            ?.toInt()
                                            ?: 0

                                    paymentReceived =
                                        it.getBoolean(
                                            "paymentReceived"
                                        ) ?: false

                                    cashCollected =
                                        it.getBoolean(
                                            "cashCollected"
                                        ) ?: false

                                    customerLat =
                                        it.getDouble("customerLat")
                                            ?: 0.0

                                    customerLng =
                                        it.getDouble("customerLng")
                                            ?: 0.0
                                }
                            }
                    }
                }
            }

        db.collection("orders")

            .whereEqualTo(
                "riderId",
                riderId
            )

            .addSnapshotListener { value, error ->

                if (error != null) {

                    android.util.Log.e(
                        "HOME_RIDER_ORDERS",
                        "Rider orders error",
                        error
                    )

                    return@addSnapshotListener
                }

                val mappedOrders =

                    value
                        ?.documents
                        ?.map { document ->

                            HomeActiveOrder(

                                orderId =
                                    document.id,

                                restaurantName =
                                    document.getString(
                                        "restaurantName"
                                    ) ?: "Restaurant",

                                restaurantAddress =
                                    document.getString(
                                        "restaurantAddress"
                                    ) ?: "",

                                customerName =
                                    document.getString(
                                        "customerName"
                                    ) ?: "Customer",

                                customerArea =
                                    document.getString(
                                        "area"
                                    ) ?: "",

                                status =
                                    document.getString(
                                        "deliveryStatus"
                                    )
                                        ?: document.getString(
                                            "status"
                                        )
                                        ?: "",

                                riderRequestStatus =
                                    document.getString(
                                        "riderRequestStatus"
                                    ) ?: "",

                                navigationStage =
                                    document.getString(
                                        "navigationStage"
                                    ) ?: "TO_RESTAURANT",

                                restaurantLat =
                                    document.getDouble(
                                        "restaurantLat"
                                    ) ?: 0.0,

                                restaurantLng =
                                    document.getDouble(
                                        "restaurantLng"
                                    ) ?: 0.0,

                                customerLat =
                                    document.getDouble(
                                        "customerLat"
                                    ) ?: 0.0,

                                customerLng =
                                    document.getDouble(
                                        "customerLng"
                                    ) ?: 0.0,

                                customerPhone =
                                    document.getString(
                                        "customerPhone"
                                    ) ?: "",

                                house =
                                    document.getString(
                                        "house"
                                    ) ?: "",

                                landmark =
                                    document.getString(
                                        "landmark"
                                    ) ?: "",

                                city =
                                    document.getString(
                                        "city"
                                    ) ?: "",

                                pincode =
                                    document.getString(
                                        "pincode"
                                    ) ?: "",

                                total =
                                    document.getLong(
                                        "total"
                                    )?.toInt() ?: 0,

                                paymentReceived =
                                    document.getBoolean(
                                        "paymentReceived"
                                    ) ?: false,

                                cashCollected =
                                    document.getBoolean(
                                        "cashCollected"
                                    ) ?: false,

                                items =
                                    (document.get("items")
                                            as? List<Map<String, Any>>)
                                        ?.map { item ->

                                            val name =
                                                item["name"]
                                                    ?.toString()
                                                    ?: ""

                                            val qty =
                                                item["quantity"]
                                                    ?.toString()
                                                    ?: "1"

                                            "• $name × $qty"
                                        }
                                        ?: emptyList()
                            )
                        }

                        ?: emptyList()

                activeOrders =

                    mappedOrders
                        .filter { order ->

                            order.riderRequestStatus !=
                                    "PENDING"

                                    &&

                                    order.riderRequestStatus !=
                                    "REJECTED"

                                    &&

                                    order.status !=
                                    "DELIVERED"

                                    &&

                                    order.status !=
                                    "CANCELLED"
                        }
                        .sortedBy {
                            it.orderId
                        }
                        .take(2)

                pendingOrders =

                    mappedOrders
                        .filter { order ->

                            order.riderRequestStatus ==
                                    "PENDING"
                        }
            }

        // =====================================================
// RIDER CHANGE REQUEST LISTENER
// =====================================================

        db.collection("orders")

            .whereEqualTo(
                "pendingRiderId",
                riderId
            )

            .addSnapshotListener { value, error ->

                if (error != null) {

                    android.util.Log.e(
                        "RIDER_CHANGE_REQUEST",
                        "Rider change request error",
                        error
                    )

                    return@addSnapshotListener
                }

                riderChangeRequests =

                    value
                        ?.documents
                        ?.filter { document ->

                            val requestStatus =

                                document.getString(
                                    "pendingRiderRequestStatus"
                                ) ?: ""

                            val changePending =

                                document.getBoolean(
                                    "riderChangePending"
                                ) ?: false

                            requestStatus == "PENDING"

                                    &&

                                    changePending

                        }
                        ?.map { document ->

                            HomeActiveOrder(

                                orderId =
                                    document.id,

                                restaurantName =
                                    document.getString(
                                        "restaurantName"
                                    ) ?: "Restaurant",

                                restaurantAddress =
                                    document.getString(
                                        "restaurantAddress"
                                    ) ?: "",

                                customerName =
                                    document.getString(
                                        "customerName"
                                    ) ?: "Customer",

                                customerPhone =
                                    document.getString(
                                        "customerPhone"
                                    ) ?: "",

                                customerArea =
                                    document.getString(
                                        "area"
                                    ) ?: "",

                                house =
                                    document.getString(
                                        "house"
                                    ) ?: "",

                                landmark =
                                    document.getString(
                                        "landmark"
                                    ) ?: "",

                                city =
                                    document.getString(
                                        "city"
                                    ) ?: "",

                                pincode =
                                    document.getString(
                                        "pincode"
                                    ) ?: "",

                                status =
                                    document.getString(
                                        "deliveryStatus"
                                    )
                                        ?: document.getString(
                                            "status"
                                        )
                                        ?: "",

                                riderRequestStatus =
                                    document.getString(
                                        "riderRequestStatus"
                                    ) ?: "",

                                pendingRiderRequestStatus =
                                    document.getString(
                                        "pendingRiderRequestStatus"
                                    ) ?: "",

                                isRiderChangeRequest =
                                    true,

                                oldRiderId =
                                    document.getString(
                                        "riderId"
                                    ) ?: "",

                                navigationStage =
                                    document.getString(
                                        "navigationStage"
                                    ) ?: "TO_RESTAURANT",

                                restaurantLat =
                                    document.getDouble(
                                        "restaurantLat"
                                    ) ?: 0.0,

                                restaurantLng =
                                    document.getDouble(
                                        "restaurantLng"
                                    ) ?: 0.0,

                                customerLat =
                                    document.getDouble(
                                        "customerLat"
                                    ) ?: 0.0,

                                customerLng =
                                    document.getDouble(
                                        "customerLng"
                                    ) ?: 0.0,

                                total =
                                    document.getLong(
                                        "total"
                                    )?.toInt() ?: 0,

                                paymentReceived =
                                    document.getBoolean(
                                        "paymentReceived"
                                    ) ?: false,

                                cashCollected =
                                    document.getBoolean(
                                        "cashCollected"
                                    ) ?: false,

                                items =

                                    (document.get("items")
                                            as? List<Map<String, Any>>)

                                        ?.map { item ->

                                            val name =

                                                item["name"]
                                                    ?.toString()
                                                    ?: ""

                                            val qty =

                                                item["quantity"]
                                                    ?.toString()
                                                    ?: "1"

                                            "• $name × $qty"
                                        }

                                        ?: emptyList()
                            )
                        }

                        ?: emptyList()
            }

        db.collection("orders")

            .whereEqualTo(
                "riderId",
                riderId
            )

            .whereEqualTo(
                "deliveryStatus",
                "DELIVERED"
            )

            .addSnapshotListener { value, _ ->

                earnings = 0
                deliveries = 0
                todayCash = 0

                val calendar =
                    Calendar.getInstance()

                calendar.set(
                    Calendar.HOUR_OF_DAY,
                    0
                )

                calendar.set(
                    Calendar.MINUTE,
                    0
                )

                calendar.set(
                    Calendar.SECOND,
                    0
                )

                calendar.set(
                    Calendar.MILLISECOND,
                    0
                )

                val todayStart =
                    calendar.timeInMillis

                value?.documents?.forEach {

                    val deliveredTime =

                        it.getTimestamp(
                            "deliveredAt"
                        )
                            ?.toDate()
                            ?.time

                            ?: it.getLong(
                                "deliveredAt"
                            )

                            ?: it.getLong(
                                "timestamp"
                            )

                            ?: 0L

                    if (
                        deliveredTime >= todayStart
                    ) {

                        earnings +=

                            it.getLong("riderPay")
                                ?.toInt()
                                ?: 0

                        deliveries++

                        if (
                            it.getString(
                                "paymentMethod"
                            ) == "COD"
                        ) {

                            todayCash +=

                                it.getLong("total")
                                    ?.toInt()
                                    ?: 0
                        }
                    }
                }
            }

    }

    val allAcceptedOrdersPickedUp =

        activeOrders.isNotEmpty()

                &&

                activeOrders.all { order ->

                    order.navigationStage ==
                            "TO_CUSTOMER"

                            ||

                            order.navigationStage ==
                            "OUT_FOR_DELIVERY"
                }

    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(16.dp)

    ) {

        Text(

            text =
                "🛵 VeggieGo Rider",

            fontSize = 28.sp,

            fontWeight =
                FontWeight.Bold
        )

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        Card(

            colors =
                CardDefaults.cardColors(

                    containerColor =

                        if (isOnline)

                            Color(0xFF00C853)

                        else

                            Color(0xFFD32F2F)
                ),

            modifier =
                Modifier.fillMaxWidth()

        ) {

            Column(

                modifier =
                    Modifier.padding(16.dp)

            ) {

                Text(

                    text =

                        if (isOnline)

                            "🟢 ONLINE"

                        else

                            "🔴 OFFLINE",

                    color =
                        Color.White,

                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                Text(

                    text =

                        if (isOnline)

                            "Ready To Deliver 🚚"

                        else

                            "Offline Mode 😴",

                    color =
                        Color.White
                )
                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                Button(

                    onClick = {

                        if (

                            isOnline

                            &&

                            activeOrders.isNotEmpty()

                        ) {

                            android.widget.Toast.makeText(

                                context,

                                "Complete current delivery first",

                                android.widget.Toast.LENGTH_SHORT

                            ).show()

                            return@Button
                        }

                        db.collection("riders")
                            .document(riderId)
                            .update(

                                "online",

                                !isOnline
                            )
                    }

                ) {

                    Text(

                        if (isOnline)

                            "GO OFFLINE"

                        else

                            "GO ONLINE"
                    )
                }
            }
        }

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        Row(

            modifier =
                Modifier.fillMaxWidth()

        ) {

            Card(

                modifier =
                    Modifier.weight(1f),

                shape =
                    RoundedCornerShape(16.dp)

            ) {

                Column(

                    modifier =
                        Modifier.padding(16.dp)

                ) {

                    Text(" Today Earnings")

                    Text(

                        "₹$earnings",

                        fontWeight =
                            FontWeight.Bold,

                        fontSize = 22.sp
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.width(12.dp)
            )

            Card(

                modifier =
                    Modifier.weight(1f),

                shape =
                    RoundedCornerShape(16.dp)

            ) {

                Column(

                    modifier =
                        Modifier.padding(16.dp)

                ) {

                    Text(" Today Deliveries")

                    Text(

                        "$deliveries",

                        fontWeight =
                            FontWeight.Bold,

                        fontSize = 22.sp
                    )
                }
            }
        }
        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        Card(

            modifier =
                Modifier.fillMaxWidth(),

            shape =
                RoundedCornerShape(16.dp)

        ) {

            Column(

                modifier =
                    Modifier.padding(16.dp)

            ) {

                Text("💵 Today Cash")

                Text(

                    "₹$todayCash",

                    fontWeight =
                        FontWeight.Bold,

                    fontSize = 22.sp
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )
        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        // =====================================================
// RIDER CHANGE REQUEST UI
// =====================================================

        if (
            riderChangeRequests.isNotEmpty()
        ) {

            Card(

                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(16.dp),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            Color(0xFFFFEBEE)
                    )
            ) {

                Column(

                    modifier =
                        Modifier.padding(16.dp)
                ) {

                    Text(

                        text =
                            "🔄 Rider Change Requests (${riderChangeRequests.size})",

                        fontWeight =
                            FontWeight.Bold,

                        fontSize =
                            20.sp,

                        color =
                            Color(0xFFC62828)
                    )

                    Spacer(
                        modifier =
                            Modifier.height(10.dp)
                    )

                    riderChangeRequests.forEach { order ->

                        Card(

                            modifier =
                                Modifier.fillMaxWidth(),

                            colors =
                                CardDefaults.cardColors(
                                    containerColor =
                                        Color.White
                                )
                        ) {

                            Column(

                                modifier =
                                    Modifier.padding(12.dp)
                            ) {

                                Text(

                                    text =
                                        "⚠️ Delivery Transfer Request",

                                    fontWeight =
                                        FontWeight.Bold,

                                    color =
                                        Color(0xFFC62828)
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(8.dp)
                                )

                                Text(

                                    text =
                                        "🍽 ${order.restaurantName}",

                                    fontWeight =
                                        FontWeight.Bold
                                )

                                if (
                                    order.restaurantAddress
                                        .isNotBlank()
                                ) {

                                    Text(
                                        text =
                                            "📍 ${order.restaurantAddress}"
                                    )
                                }

                                Text(
                                    text =
                                        "👤 ${order.customerName}"
                                )

                                Text(

                                    text =
                                        "📦 Current Stage: ${order.status.replace("_", " ")}",

                                    fontWeight =
                                        FontWeight.SemiBold
                                )

                                Text(
                                    text =
                                        "🆔 ${order.orderId}",
                                    fontSize =
                                        12.sp
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(10.dp)
                                )

                                Row(

                                    modifier =
                                        Modifier.fillMaxWidth(),

                                    horizontalArrangement =
                                        Arrangement.spacedBy(
                                            8.dp
                                        )
                                ) {

                                    Button(

                                        onClick = {

                                            if (
                                                activeOrders.size >= 2
                                            ) {

                                                android.widget.Toast
                                                    .makeText(
                                                        context,
                                                        "Maximum 2 active orders allowed",
                                                        android.widget.Toast.LENGTH_SHORT
                                                    )
                                                    .show()

                                                return@Button
                                            }

                                            val orderRef =

                                                db.collection(
                                                    "orders"
                                                )
                                                    .document(
                                                        order.orderId
                                                    )

                                            orderRef
                                                .get()
                                                .addOnSuccessListener { snapshot ->

                                                    val currentPendingRiderId =

                                                        snapshot.getString(
                                                            "pendingRiderId"
                                                        ) ?: ""

                                                    val pendingStatus =

                                                        snapshot.getString(
                                                            "pendingRiderRequestStatus"
                                                        ) ?: ""

                                                    val oldRiderId =

                                                        snapshot.getString(
                                                            "riderId"
                                                        ) ?: ""

                                                    if (
                                                        currentPendingRiderId != riderId

                                                        ||

                                                        pendingStatus != "PENDING"
                                                    ) {

                                                        android.widget.Toast
                                                            .makeText(
                                                                context,
                                                                "This request is no longer available",
                                                                android.widget.Toast.LENGTH_SHORT
                                                            )
                                                            .show()

                                                        return@addOnSuccessListener
                                                    }

                                                    db.collection(
                                                        "riders"
                                                    )
                                                        .document(
                                                            riderId
                                                        )
                                                        .get()
                                                        .addOnSuccessListener { riderSnapshot ->

                                                            val riderName =

                                                                riderSnapshot.getString(
                                                                    "name"
                                                                ) ?: "Rider"

                                                            val riderPhone =

                                                                riderSnapshot.getString(
                                                                    "phone"
                                                                )
                                                                    ?: riderSnapshot.getString(
                                                                        "mobile"
                                                                    )
                                                                    ?: ""

                                                            db.runBatch { batch ->

                                                                batch.update(

                                                                    orderRef,

                                                                    mapOf(

                                                                        "riderId" to
                                                                                riderId,

                                                                        "riderName" to
                                                                                riderName,

                                                                        "riderPhone" to
                                                                                riderPhone,

                                                                        "riderAssigned" to
                                                                                true,

                                                                        "riderRequestStatus" to
                                                                                "ACCEPTED",

                                                                        "pendingRiderRequestStatus" to
                                                                                "ACCEPTED",

                                                                        "riderChangePending" to
                                                                                false,

                                                                        "pendingRiderId" to
                                                                                "",

                                                                        "pendingRiderName" to
                                                                                "",

                                                                        "pendingRiderPhone" to
                                                                                "",

                                                                        "previousRiderId" to
                                                                                oldRiderId,

                                                                        "riderChangedAt" to
                                                                                FieldValue.serverTimestamp(),

                                                                        "riderAcceptedAt" to
                                                                                FieldValue.serverTimestamp()
                                                                    )
                                                                )

                                                                if (
                                                                    activeOrderId.isBlank()
                                                                ) {

                                                                    batch.update(

                                                                        db.collection(
                                                                            "riders"
                                                                        )
                                                                            .document(
                                                                                riderId
                                                                            ),

                                                                        mapOf(

                                                                            "activeOrderId" to
                                                                                    order.orderId
                                                                        )
                                                                    )
                                                                }
                                                            }
                                                                .addOnSuccessListener {

                                                                    android.widget.Toast
                                                                        .makeText(
                                                                            context,
                                                                            "Order transferred successfully",
                                                                            android.widget.Toast.LENGTH_SHORT
                                                                        )
                                                                        .show()
                                                                }

                                                                .addOnFailureListener {

                                                                    android.widget.Toast
                                                                        .makeText(
                                                                            context,
                                                                            "Unable to accept transfer",
                                                                            android.widget.Toast.LENGTH_SHORT
                                                                        )
                                                                        .show()
                                                                }
                                                        }
                                                }
                                        },

                                        enabled =
                                            activeOrders.size < 2,

                                        modifier =
                                            Modifier.weight(1f)
                                    ) {

                                        Text("ACCEPT")
                                    }

                                    OutlinedButton(

                                        onClick = {

                                            val orderRef =

                                                db.collection(
                                                    "orders"
                                                )
                                                    .document(
                                                        order.orderId
                                                    )

                                            orderRef
                                                .get()
                                                .addOnSuccessListener { snapshot ->

                                                    val currentPendingRiderId =

                                                        snapshot.getString(
                                                            "pendingRiderId"
                                                        ) ?: ""

                                                    if (
                                                        currentPendingRiderId != riderId
                                                    ) {

                                                        return@addOnSuccessListener
                                                    }

                                                    orderRef.update(

                                                        mapOf(

                                                            "pendingRiderRequestStatus" to
                                                                    "REJECTED",

                                                            "riderChangePending" to
                                                                    false,

                                                            "pendingRiderId" to
                                                                    "",

                                                            "pendingRiderName" to
                                                                    "",

                                                            "pendingRiderPhone" to
                                                                    "",

                                                            "riderChangeRejectedAt" to
                                                                    FieldValue.serverTimestamp()
                                                        )
                                                    )
                                                        .addOnSuccessListener {

                                                            android.widget.Toast
                                                                .makeText(
                                                                    context,
                                                                    "Transfer request rejected",
                                                                    android.widget.Toast.LENGTH_SHORT
                                                                )
                                                                .show()
                                                        }
                                                }
                                        },

                                        modifier =
                                            Modifier.weight(1f)
                                    ) {

                                        Text("REJECT")
                                    }
                                }
                            }
                        }

                        Spacer(
                            modifier =
                                Modifier.height(10.dp)
                        )
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )
        }
        if (
            pendingOrders.isNotEmpty()
        ) {

            Card(

                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(16.dp),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            Color(0xFFFFF8E1)
                    )
            ) {

                Column(
                    modifier =
                        Modifier.padding(16.dp)
                ) {

                    Text(
                        text =
                            "🆕 Pending Orders (${pendingOrders.size})",
                        fontWeight =
                            FontWeight.Bold,
                        fontSize =
                            20.sp
                    )

                    Spacer(
                        modifier =
                            Modifier.height(10.dp)
                    )

                    pendingOrders.forEach { order ->

                        Card(
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {

                            Column(
                                modifier =
                                    Modifier.padding(12.dp)
                            ) {

                                Text(
                                    text =
                                        "🍽 ${order.restaurantName}",
                                    fontWeight =
                                        FontWeight.Bold
                                )

                                Text(
                                    text =
                                        "📍 ${order.restaurantAddress}"
                                )

                                Text(
                                    text =
                                        "👤 ${order.customerName}"
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(8.dp)
                                )

                                Row(
                                    modifier =
                                        Modifier.fillMaxWidth(),
                                    horizontalArrangement =
                                        Arrangement.spacedBy(
                                            8.dp
                                        )
                                ) {

                                    Button(

                                        onClick = {

                                            if (
                                                activeOrders.size >= 2
                                            ) {

                                                android.widget.Toast
                                                    .makeText(
                                                        context,
                                                        "Maximum 2 active orders allowed",
                                                        android.widget.Toast.LENGTH_SHORT
                                                    )
                                                    .show()

                                                return@Button
                                            }

                                            db.collection("orders")
                                                .document(
                                                    order.orderId
                                                )
                                                .update(

                                                    mapOf(

                                                        "riderRequestStatus" to
                                                                "ACCEPTED",

                                                        "riderAssigned" to
                                                                true,

                                                        "navigationStage" to
                                                                "TO_RESTAURANT",

                                                        "riderAcceptedAt" to
                                                                FieldValue.serverTimestamp()
                                                    )
                                                )
                                                .addOnSuccessListener {

                                                    if (
                                                        activeOrderId
                                                            .isBlank()
                                                    ) {

                                                        db.collection(
                                                            "riders"
                                                        )
                                                            .document(
                                                                riderId
                                                            )
                                                            .update(
                                                                "activeOrderId",
                                                                order.orderId
                                                            )
                                                    }
                                                }
                                        },

                                        enabled =
                                            activeOrders.size < 2,

                                        modifier =
                                            Modifier.weight(1f)
                                    ) {

                                        Text("ACCEPT")
                                    }

                                    OutlinedButton(

                                        onClick = {

                                            db.collection("orders")
                                                .document(
                                                    order.orderId
                                                )
                                                .update(

                                                    mapOf(

                                                        "riderRequestStatus" to
                                                                "REJECTED",

                                                        "riderAssigned" to
                                                                false,

                                                        "riderId" to
                                                                "",

                                                        "riderName" to
                                                                "",

                                                        "riderPhone" to
                                                                "",

                                                        "riderRejectedAt" to
                                                                FieldValue.serverTimestamp()
                                                    )
                                                )
                                        },

                                        modifier =
                                            Modifier.weight(1f)
                                    ) {

                                        Text("REJECT")
                                    }
                                }
                            }
                        }

                        Spacer(
                            modifier =
                                Modifier.height(10.dp)
                        )
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )
        }

        Card(

            modifier =
                Modifier.fillMaxWidth(),

            shape =
                RoundedCornerShape(16.dp),

            colors =
                CardDefaults.cardColors(

                    containerColor =
                        if (
                            activeOrders.size >= 2
                        )

                            Color(0xFFFFF3E0)

                        else

                            Color(0xFFE8F5E9)
                )

        ) {

            Row(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),

                horizontalArrangement =
                    Arrangement.SpaceBetween

            ) {

                Column {

                    Text(

                        text =
                            "🚚 Active Orders",

                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )

                    Text(

                        text =
                            when (
                                activeOrders.size
                            ) {

                                0 ->
                                    "No active delivery"

                                1 ->
                                    "1 order in progress"

                                else ->
                                    "Maximum capacity reached"
                            },

                        fontSize =
                            13.sp
                    )
                }

                Text(

                    text =
                        "${activeOrders.size}/2",

                    fontSize =
                        24.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        if (
                            activeOrders.size >= 2
                        )

                            Color(0xFFE65100)

                        else

                            Color(0xFF2E7D32)
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        if (
            activeOrderId.isNotEmpty()
        ) {

            Card(

                colors =
                    CardDefaults.cardColors(

                        containerColor =
                            Color(0xFFFFF3E0)
                    ),

                modifier =
                    Modifier.fillMaxWidth()

            ) {

                Column(

                    modifier =
                        Modifier.padding(16.dp)

                ) {

                    Text(

                        text =
                            "🚴 LIVE ORDERS (${if (activeOrderId.isNotEmpty()) 1 else 0})",

                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(
                        text = "🆔 $activeOrderId"
                    )

                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )

                    Text(
                        text = "🍔 $activeRestaurantName",
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )

                    Text(
                        text = "📦 Items",
                        fontWeight = FontWeight.Bold
                    )

                    activeItems.forEach {

                        Text(it)
                    }

                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )

                    Text(
                        text = "👤 $activeCustomerName",
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text =
                            "📍 $house, $landmark, $activeArea, $city - $pincode"
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Button(

                        onClick = {

                            val lat =
                                if (navigationStage == "TO_RESTAURANT")
                                    restaurantLat
                                else
                                    customerLat

                            val lng =
                                if (navigationStage == "TO_RESTAURANT")
                                    restaurantLng
                                else
                                    customerLng

                            val uri =
                                Uri.parse(
                                    "google.navigation:q=$lat,$lng"
                                )

                            val intent =
                                Intent(
                                    Intent.ACTION_VIEW,
                                    uri
                                )

                            intent.setPackage(
                                "com.google.android.apps.maps"
                            )

                            context.startActivity(
                                intent
                            )
                        },

                        modifier =
                            Modifier.fillMaxWidth()

                    ) {

                        Text(

                            if (
                                navigationStage ==
                                "TO_RESTAURANT"
                            )

                                "🗺 Navigate To Restaurant"
                            else

                                "🗺 Navigate To Customer"
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Button(

                        onClick = {

                            if (activePhone.isNotEmpty()) {

                                val intent =
                                    Intent(
                                        Intent.ACTION_DIAL,
                                        Uri.parse(
                                            "tel:$activePhone"
                                        )
                                    )

                                context.startActivity(
                                    intent
                                )
                            }
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
                    Button(

                        onClick = {

                            val intent =
                                Intent(

                                    context,

                                    RiderChatActivity::class.java
                                )

                            intent.putExtra(
                                "orderId",
                                activeOrderId
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

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    if (
                        navigationStage == "TO_RESTAURANT"
                    ) {

                        Button(

                            onClick = {

                                db.collection("orders")
                                    .document(activeOrderId)
                                    .get()
                                    .addOnSuccessListener {

                                        val orderRiderId =
                                            it.getString("riderId")
                                                ?: ""

                                        if (
                                            orderRiderId ==
                                            riderId
                                        ) {

                                            db.collection("orders")
                                                .document(activeOrderId)
                                                .update(
                                                    "navigationStage",
                                                    "REACHED_RESTAURANT"
                                                )
                                        }
                                    }
                            },

                            modifier =
                                Modifier.fillMaxWidth()

                        ) {

                            Text(
                                "✅ Reached Restaurant"
                            )
                        }
                    }

                    if (
                        navigationStage ==
                        "REACHED_RESTAURANT"
                    ) {

                        Button(

                            onClick = {

                                db.collection("orders")
                                    .document(activeOrderId)
                                    .get()
                                    .addOnSuccessListener {

                                        val orderRiderId =
                                            it.getString("riderId")
                                                ?: ""

                                        if (
                                            orderRiderId ==
                                            riderId
                                        ) {

                                            db.collection("orders")
                                                .document(activeOrderId)
                                                .update(

                                                    mapOf(

                                                        "navigationStage" to
                                                                "TO_CUSTOMER",

                                                        "status" to
                                                                "PICKED_UP",

                                                        "deliveryStatus" to
                                                                "PICKED_UP"
                                                    )
                                                )
                                        }
                                    }
                            },

                            modifier =
                                Modifier.fillMaxWidth()

                        ) {

                            Text(
                                "📦 Picked Up"
                            )
                        }
                    }

                    if (
                        navigationStage ==
                        "TO_CUSTOMER"
                    ) {

                        Button(

                            onClick = {

                                if (
                                    !allAcceptedOrdersPickedUp
                                ) {

                                    android.widget.Toast
                                        .makeText(
                                            context,
                                            "Please collect all accepted orders first",
                                            android.widget.Toast.LENGTH_SHORT
                                        )
                                        .show()

                                    return@Button
                                }

                                db.collection("orders")
                                    .document(activeOrderId)
                                    .get()
                                    .addOnSuccessListener {

                                        val orderRiderId =
                                            it.getString("riderId")
                                                ?: ""

                                        if (
                                            orderRiderId ==
                                            riderId
                                        ) {

                                            db.collection("orders")
                                                .document(activeOrderId)
                                                .update(

                                                    mapOf(

                                                        "status" to
                                                                "OUT_FOR_DELIVERY",

                                                        "deliveryStatus" to
                                                                "OUT_FOR_DELIVERY",

                                                        "navigationStage" to
                                                                "OUT_FOR_DELIVERY"
                                                    )
                                                )
                                        }
                                    }
                            },

                            enabled =
                                allAcceptedOrdersPickedUp,

                            modifier =
                                Modifier.fillMaxWidth()

                        ) {

                            Text(
                                "🚚 Start Delivery"
                            )
                        }
                    }
                    if (
                        navigationStage ==
                        "OUT_FOR_DELIVERY"
                    ) {

                        Text(
                            "🚚 Delivery In Progress",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )
                    }
                    if (
                        activeStatus ==
                        "OUT_FOR_DELIVERY"
                    ) {

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )

                        if (paymentReceived) {

                            Text(
                                "✅ PAYMENT RECEIVED",
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(8.dp)
                            )
                        }

                        if (!paymentReceived) {

                            Text(

                                text =
                                    "💰 Customer Amount ₹$activeTotal",

                                fontWeight =
                                    FontWeight.Bold,

                                color =
                                    Color(0xFFD84315)

                            )

                            Spacer(
                                modifier =
                                    Modifier.height(8.dp)
                            )

                            Row {

                                Checkbox(

                                    checked =
                                        cashCollected,

                                    onCheckedChange = {

                                        db.collection("orders")
                                            .document(activeOrderId)
                                            .get()
                                            .addOnSuccessListener {

                                                val orderRiderId =
                                                    it.getString("riderId")
                                                        ?: ""

                                                if (
                                                    orderRiderId ==
                                                    riderId
                                                ) {

                                                    cashCollected = !cashCollected

                                                    db.collection("orders")
                                                        .document(activeOrderId)
                                                        .update(
                                                            "cashCollected",
                                                            cashCollected
                                                        )
                                                }
                                            }
                                    }
                                )

                                Text(
                                    "💵 Collect Cash ₹$activeTotal",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(
                                modifier =
                                    Modifier.height(8.dp)
                            )
                        }

                        Button(

                            onClick = {
                                db.collection("orders")
                                    .document(activeOrderId)
                                    .get()
                                    .addOnSuccessListener {

                                        val orderRiderId =
                                            it.getString("riderId")
                                                ?: ""

                                        if (
                                            orderRiderId !=
                                            riderId
                                        ) return@addOnSuccessListener

                                        db.collection("orders")
                                            .document(activeOrderId)
                                            .update(
                                                mapOf(

                                                    "status" to
                                                            "DELIVERED",

                                                    "deliveryStatus" to
                                                            "DELIVERED",

                                                    "navigationStage" to
                                                            "DELIVERED",

                                                    "riderAssigned" to
                                                            false,

                                                    "deliveredAt" to
                                                            FieldValue.serverTimestamp()
                                                )
                                            )

                                            .addOnSuccessListener {

                                                db.collection("orders")

                                                    .whereEqualTo(
                                                        "riderId",
                                                        riderId
                                                    )

                                                    .get()

                                                    .addOnSuccessListener { orders ->
                                                        val nextOrder =

                                                            orders.documents.firstOrNull {

                                                                val status =
                                                                    it.getString(
                                                                        "deliveryStatus"
                                                                    ) ?: ""

                                                                it.id != activeOrderId

                                                                        &&

                                                                        status != "DELIVERED"

                                                                        &&

                                                                        status != "CANCELLED"
                                                            }
                                                        android.util.Log.d(
                                                            "VEGGIEGO",
                                                            "TOTAL ORDERS = ${orders.documents.size}"
                                                        )

                                                        orders.documents.forEach {

                                                            android.util.Log.d(
                                                                "VEGGIEGO",
                                                                "ORDER = ${it.id} | STATUS = ${
                                                                    it.getString("deliveryStatus")
                                                                } | ASSIGNED = ${
                                                                    it.getBoolean("riderAssigned")
                                                                }"
                                                            )
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
                                                                            FieldValue.increment(1)
                                                                )
                                                            )
                                                    }
                                            }
                                    }
                            },

                            enabled =

                                paymentReceived
                                        ||
                                        cashCollected,

                            modifier =
                                Modifier.fillMaxWidth()

                        ) {

                            Text(
                                "✅ Delivered"
                            )
                        }
                    }
                }
            }
        }
        if (
            activeOrders.size > 1
        ) {

            val secondOrder =
                activeOrders.firstOrNull {
                    it.orderId != activeOrderId
                } ?: activeOrders[1]

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            Card(

                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(16.dp),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            Color(0xFFE3F2FD)
                    )
            ) {

                Column(
                    modifier =
                        Modifier.padding(16.dp)
                ) {

                    Row(

                        modifier =
                            Modifier.fillMaxWidth(),

                        horizontalArrangement =
                            Arrangement.SpaceBetween
                    ) {

                        Text(
                            text = "📦 ADDITIONAL ORDER",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1565C0)
                        )

                        Text(
                            text = "2 of 2",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(10.dp)
                    )

                    Text(
                        text = "🆔 ${secondOrder.orderId}",
                        fontSize = 13.sp
                    )

                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )

                    Text(
                        text = "🍽 ${secondOrder.restaurantName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    if (
                        secondOrder.restaurantAddress
                            .isNotBlank()
                    ) {

                        Spacer(
                            modifier =
                                Modifier.height(4.dp)
                        )

                        Text(
                            text =
                                "📍 ${secondOrder.restaurantAddress}",
                            fontSize = 14.sp
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(
                        text = "📦 Items",
                        fontWeight = FontWeight.Bold
                    )

                    secondOrder.items.forEach {
                        Text(it)
                    }

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            "👤 ${secondOrder.customerName}",
                        fontWeight =
                            FontWeight.SemiBold
                    )

                    Text(
                        text =
                            "🏠 ${secondOrder.house}, ${secondOrder.landmark}, ${secondOrder.customerArea}, ${secondOrder.city} - ${secondOrder.pincode}",
                        fontSize = 14.sp
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(

                        text =
                            when (
                                secondOrder.navigationStage
                            ) {

                                "TO_RESTAURANT" ->
                                    "Stage: Going to restaurant"

                                "REACHED_RESTAURANT" ->
                                    "Stage: Reached restaurant"

                                "TO_CUSTOMER" ->
                                    "Stage: Picked up"

                                "OUT_FOR_DELIVERY" ->
                                    "Stage: Out for delivery"

                                else ->
                                    "Status: ${secondOrder.status}"
                            },

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            Color(0xFF1565C0)
                    )

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    Button(

                        onClick = {

                            val goToCustomer =

                                secondOrder.navigationStage ==
                                        "TO_CUSTOMER"

                                        ||

                                        secondOrder.navigationStage ==
                                        "OUT_FOR_DELIVERY"

                            val lat =
                                if (goToCustomer)
                                    secondOrder.customerLat
                                else
                                    secondOrder.restaurantLat

                            val lng =
                                if (goToCustomer)
                                    secondOrder.customerLng
                                else
                                    secondOrder.restaurantLng

                            if (
                                lat == 0.0 ||
                                lng == 0.0
                            ) {

                                android.widget.Toast
                                    .makeText(
                                        context,
                                        "Location not available",
                                        android.widget.Toast.LENGTH_SHORT
                                    )
                                    .show()

                            } else {

                                val uri =
                                    Uri.parse(
                                        "google.navigation:q=$lat,$lng"
                                    )

                                val intent =
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        uri
                                    )

                                intent.setPackage(
                                    "com.google.android.apps.maps"
                                )

                                context.startActivity(
                                    intent
                                )
                            }
                        },

                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Text(

                            text =
                                if (
                                    secondOrder.navigationStage ==
                                    "TO_CUSTOMER"

                                    ||

                                    secondOrder.navigationStage ==
                                    "OUT_FOR_DELIVERY"
                                )

                                    "🗺 Navigate To Customer"

                                else

                                    "🗺 Navigate To Restaurant"
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Button(

                        onClick = {

                            if (
                                secondOrder.customerPhone
                                    .isNotBlank()
                            ) {

                                val intent =
                                    Intent(
                                        Intent.ACTION_DIAL,
                                        Uri.parse(
                                            "tel:${secondOrder.customerPhone}"
                                        )
                                    )

                                context.startActivity(
                                    intent
                                )
                            }
                        },

                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Text("📞 Call Customer")
                    }

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
                                secondOrder.orderId
                            )

                            context.startActivity(
                                intent
                            )
                        },

                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Text("💬 Chat")
                    }

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    if (
                        secondOrder.navigationStage ==
                        "TO_RESTAURANT"
                    ) {

                        Button(

                            onClick = {

                                db.collection("orders")
                                    .document(
                                        secondOrder.orderId
                                    )
                                    .get()
                                    .addOnSuccessListener {

                                        val orderRiderId =
                                            it.getString(
                                                "riderId"
                                            ) ?: ""

                                        if (
                                            orderRiderId ==
                                            riderId
                                        ) {

                                            db.collection(
                                                "orders"
                                            )
                                                .document(
                                                    secondOrder.orderId
                                                )
                                                .update(
                                                    "navigationStage",
                                                    "REACHED_RESTAURANT"
                                                )
                                        }
                                    }
                            },

                            modifier =
                                Modifier.fillMaxWidth()
                        ) {

                            Text(
                                "✅ Reached Restaurant"
                            )
                        }
                    }

                    if (
                        secondOrder.navigationStage ==
                        "REACHED_RESTAURANT"
                    ) {

                        Button(

                            onClick = {

                                db.collection("orders")
                                    .document(
                                        secondOrder.orderId
                                    )
                                    .get()
                                    .addOnSuccessListener {

                                        val orderRiderId =
                                            it.getString(
                                                "riderId"
                                            ) ?: ""

                                        if (
                                            orderRiderId ==
                                            riderId
                                        ) {

                                            db.collection(
                                                "orders"
                                            )
                                                .document(
                                                    secondOrder.orderId
                                                )
                                                .update(

                                                    mapOf(

                                                        "navigationStage" to
                                                                "TO_CUSTOMER",

                                                        "status" to
                                                                "PICKED_UP",

                                                        "deliveryStatus" to
                                                                "PICKED_UP"
                                                    )
                                                )
                                        }
                                    }
                            },

                            modifier =
                                Modifier.fillMaxWidth()
                        ) {

                            Text("📦 Picked Up")
                        }
                    }

                    if (
                        secondOrder.navigationStage ==
                        "TO_CUSTOMER"
                    ) {

                        Button(

                            onClick = {

                                if (
                                    !allAcceptedOrdersPickedUp
                                ) {

                                    android.widget.Toast
                                        .makeText(
                                            context,
                                            "Please collect all accepted orders first",
                                            android.widget.Toast.LENGTH_SHORT
                                        )
                                        .show()

                                    return@Button
                                }

                                db.collection("orders")
                                    .document(
                                        secondOrder.orderId
                                    )
                                    .get()
                                    .addOnSuccessListener {

                                        val orderRiderId =
                                            it.getString(
                                                "riderId"
                                            ) ?: ""

                                        if (
                                            orderRiderId ==
                                            riderId
                                        ) {

                                            db.collection(
                                                "orders"
                                            )
                                                .document(
                                                    secondOrder.orderId
                                                )
                                                .update(

                                                    mapOf(

                                                        "status" to
                                                                "OUT_FOR_DELIVERY",

                                                        "deliveryStatus" to
                                                                "OUT_FOR_DELIVERY",

                                                        "navigationStage" to
                                                                "OUT_FOR_DELIVERY"
                                                    )
                                                )
                                        }
                                    }
                            },

                            enabled =
                                allAcceptedOrdersPickedUp,

                            modifier =
                                Modifier.fillMaxWidth()
                        ) {

                            Text(
                                "🚚 Start Delivery"
                            )
                        }
                    }

                    if (
                        secondOrder.navigationStage ==
                        "OUT_FOR_DELIVERY"
                    ) {

                        Text(
                            text =
                                "🚚 Delivery In Progress",
                            fontWeight =
                                FontWeight.Bold,
                            color =
                                Color(0xFF2E7D32)
                        )

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )

                        if (
                            secondOrder.paymentReceived
                        ) {

                            Text(
                                text =
                                    "✅ PAYMENT RECEIVED",
                                color =
                                    Color(0xFF2E7D32),
                                fontWeight =
                                    FontWeight.Bold
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(8.dp)
                            )
                        }

                        if (
                            !secondOrder.paymentReceived
                        ) {

                            Text(
                                text =
                                    "💰 Customer Amount ₹${secondOrder.total}",
                                fontWeight =
                                    FontWeight.Bold,
                                color =
                                    Color(0xFFD84315)
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(8.dp)
                            )

                            Row {

                                Checkbox(

                                    checked =
                                        secondOrder.cashCollected,

                                    onCheckedChange = {

                                        db.collection("orders")
                                            .document(
                                                secondOrder.orderId
                                            )
                                            .update(
                                                "cashCollected",
                                                !secondOrder.cashCollected
                                            )
                                    }
                                )

                                Text(
                                    text =
                                        "💵 Collect Cash ₹${secondOrder.total}",
                                    fontWeight =
                                        FontWeight.Bold
                                )
                            }

                            Spacer(
                                modifier =
                                    Modifier.height(8.dp)
                            )
                        }

                        Button(

                            onClick = {

                                db.collection("orders")
                                    .document(
                                        secondOrder.orderId
                                    )
                                    .get()
                                    .addOnSuccessListener {

                                        val orderRiderId =
                                            it.getString(
                                                "riderId"
                                            ) ?: ""

                                        if (
                                            orderRiderId !=
                                            riderId
                                        ) {

                                            return@addOnSuccessListener
                                        }

                                        db.collection("orders")
                                            .document(
                                                secondOrder.orderId
                                            )
                                            .update(

                                                mapOf(

                                                    "status" to
                                                            "DELIVERED",

                                                    "deliveryStatus" to
                                                            "DELIVERED",

                                                    "navigationStage" to
                                                            "DELIVERED",

                                                    "riderAssigned" to
                                                            false,

                                                    "deliveredAt" to
                                                            FieldValue.serverTimestamp()
                                                )
                                            )
                                            .addOnSuccessListener {

                                                db.collection("orders")

                                                    .whereEqualTo(
                                                        "riderId",
                                                        riderId
                                                    )

                                                    .get()

                                                    .addOnSuccessListener { orders ->

                                                        val nextOrder =

                                                            orders.documents
                                                                .firstOrNull {

                                                                    val status =
                                                                        it.getString(
                                                                            "deliveryStatus"
                                                                        ) ?: ""

                                                                    it.id !=
                                                                            secondOrder.orderId

                                                                            &&

                                                                            status !=
                                                                            "DELIVERED"

                                                                            &&

                                                                            status !=
                                                                            "CANCELLED"
                                                                }

                                                        db.collection(
                                                            "riders"
                                                        )
                                                            .document(
                                                                riderId
                                                            )
                                                            .update(

                                                                mapOf(

                                                                    "activeOrderId" to
                                                                            (
                                                                                    nextOrder?.id
                                                                                        ?: ""
                                                                                    ),

                                                                    "totalDeliveries" to
                                                                            FieldValue.increment(
                                                                                1
                                                                            )
                                                                )
                                                            )
                                                    }
                                            }
                                    }
                            },

                            enabled =
                                secondOrder.paymentReceived
                                        ||
                                        secondOrder.cashCollected,

                            modifier =
                                Modifier.fillMaxWidth()
                        ) {

                            Text("✅ Delivered")
                        }
                    }
                }
            }
        }
    }
}