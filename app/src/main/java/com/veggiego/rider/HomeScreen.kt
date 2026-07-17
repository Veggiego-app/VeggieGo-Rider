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

                    val timestamp =
                        it.getLong("timestamp")
                            ?: 0L

                    if (
                        timestamp >= todayStart
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

                            activeOrderId.isNotEmpty()

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
                                                    "status" to "DELIVERED",
                                                    "deliveryStatus" to "DELIVERED",
                                                    "navigationStage" to "DELIVERED",
                                                    "riderAssigned" to false
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
    }
}