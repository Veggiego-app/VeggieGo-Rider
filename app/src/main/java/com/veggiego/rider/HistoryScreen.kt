package com.veggiego.rider

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HistoryOrder(

    val orderId: String = "",

    val restaurantName: String = "",

    val customerName: String = "",

    val area: String = "",

    val city: String = "",

    val riderEarning: Int = 0,

    val timestamp: Long = 0L
)

@Composable
fun HistoryScreen() {

    val riderId =
        FirebaseAuth
            .getInstance()
            .currentUser
            ?.uid ?: ""

    val db =
        FirebaseFirestore.getInstance()

    var orders by remember {
        mutableStateOf(
            listOf<HistoryOrder>()
        )
    }

    var totalEarnings by remember {
        mutableStateOf(0)
    }
    var expandedWeek by remember {
        mutableStateOf("")
    }

    LaunchedEffect(Unit) {

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

                if (value != null) {

                    totalEarnings = 0

                    orders =

                        value.documents

                            .sortedByDescending {

                                it.getLong("timestamp")
                                    ?: 0L
                            }

                            .map {

                                val earning =
                                    it.getLong("riderPay")
                                        ?.toInt()
                                        ?: 0

                            totalEarnings += earning

                            HistoryOrder(

                                orderId =
                                    it.getString("orderId")
                                        ?: it.id,

                                restaurantName =
                                    it.getString("restaurantName")
                                        ?: "",

                                customerName =
                                    it.getString("customerName")
                                        ?: "",

                                area =
                                    it.getString("area")
                                        ?: "",

                                city =
                                    it.getString("city")
                                        ?: "",

                                riderEarning =
                                    it.getLong("riderPay")
                                        ?.toInt()
                                        ?: 0,

                                timestamp =
                                    it.getLong("timestamp")
                                        ?: 0L
                            )
                        }
                }
            }
    }

    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)

    ) {

        Text(

            text =
                "📜 Delivery History",

            fontSize = 26.sp,

            fontWeight =
                FontWeight.Bold
        )

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        Card(

            modifier =
                Modifier.fillMaxWidth()

        ) {

            Column(

                modifier =
                    Modifier.padding(16.dp)

            ) {

                Text(
                    "💰 Total Earnings"
                )

                Text(

                    text =
                        "₹$totalEarnings",

                    fontSize = 24.sp,

                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                Text(
                    "📦 Deliveries : ${orders.size}"
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )
        val currentWeekOrders = orders.take(50)

        val currentWeekEarnings =
            currentWeekOrders.sumOf {
                it.riderEarning
            }

        LazyColumn {

            item {

                Card(

                    modifier =
                        Modifier.fillMaxWidth()

                ) {

                    Column(

                        modifier =
                            Modifier.padding(16.dp)

                    ) {

                        Button(

                            onClick = {

                                expandedWeek =

                                    if (
                                        expandedWeek ==
                                        "CURRENT"
                                    )

                                        ""

                                    else

                                        "CURRENT"
                            },

                            modifier =
                                Modifier.fillMaxWidth()

                        ) {

                            Text(
                                "📅 Current Week"
                            )
                        }
                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )

                        Text(
                            "💰 Total Earnings ₹$currentWeekEarnings"
                        )

                        Text(
                            "📦 Total Deliveries ${currentWeekOrders.size}"
                        )

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )

                        Text(

                            if (
                                expandedWeek ==
                                "CURRENT"
                            )

                                "🔼 Hide Orders"

                            else

                                "🔽 Show Orders"
                        )
                    }
                }
            }

            if (expandedWeek == "CURRENT") {

                items(currentWeekOrders) { order ->

                    Card(

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    vertical = 4.dp
                                )

                    ) {

                        Column(

                            modifier =
                                Modifier.padding(16.dp)

                        ) {

                            val dateText =

                                SimpleDateFormat(

                                    "dd MMM yyyy • hh:mm a",

                                    Locale.getDefault()

                                ).format(

                                    Date(
                                        order.timestamp
                                    )
                                )

                            Text(
                                "🆔 ${order.orderId}",
                                fontWeight =
                                    FontWeight.Bold
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(4.dp)
                            )

                            Text(
                                "🕒 $dateText"
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(4.dp)
                            )

                            Text(
                                "🍔 ${order.restaurantName}"
                            )

                            Text(
                                "👤 ${order.customerName}"
                            )

                            Text(
                                "📍 ${order.area}, ${order.city}"
                            )

                            Text(
                                "💰 Rider Earning ₹${order.riderEarning}"
                            )
                        }
                    }
                }
            }
        }
    }
}