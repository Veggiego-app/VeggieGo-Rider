package com.veggiego.rider

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class RiderSettlementItem(
    val id: String,
    val period: String,
    val deliveryCount: Int,
    val grossEarning: Double,
    val codCollected: Double,
    val netPayable: Double,
    val status: String,
    val paymentMode: String,
    val paymentReference: String,
    val paidDate: String,
    val adminRemark: String
)

private data class RiderWeek(
    val start: Long,
    val endExclusive: Long,
    val shortLabel: String,
    val fullLabel: String,
    val isCurrent: Boolean
)

private data class WeeklyDelivery(
    val orderTime: Long,
    val riderPay: Double,
    val codAmount: Double,
    val settlementStatus: String
)

@Composable
fun RiderEarningsScreen() {
    val riderId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val db = FirebaseFirestore.getInstance()
    val weeks = remember { buildLastFiveWeeks() }
    var selectedWeek by remember { mutableStateOf(weeks.first()) }
    var deliveries by remember { mutableStateOf<List<WeeklyDelivery>>(emptyList()) }
    var settlements by remember { mutableStateOf<List<RiderSettlementItem>>(emptyList()) }
    var ordersListener: ListenerRegistration? by remember { mutableStateOf(null) }
    var settlementsListener: ListenerRegistration? by remember { mutableStateOf(null) }
    var loadMessage by remember { mutableStateOf("Loading weekly earnings…") }

    DisposableEffect(riderId) {
        if (riderId.isBlank()) {
            loadMessage = "Please login again"
            onDispose { }
        } else {
            // Earning/COD come from delivered orders, not from settlement documents.
            ordersListener = db.collection("orders")
                .whereEqualTo("riderId", riderId)
                .addSnapshotListener { value, error ->
                    if (error != null) {
                        loadMessage = "Unable to load earnings"
                        return@addSnapshotListener
                    }
                    deliveries = value?.documents?.mapNotNull { doc ->
                        val status = doc.getString("deliveryStatus")
                            ?: doc.getString("status")
                            ?: ""
                        if (status.uppercase(Locale.US) != "DELIVERED") return@mapNotNull null
                        WeeklyDelivery(
                            // Business rule: attribute earning to the time the order arrived.
                            orderTime = orderArrivalTime(doc),
                            riderPay = firstNumber(doc, "riderPay", "riderEarning", "estimatedRiderEarning"),
                            codAmount = if (doc.getString("paymentMethod").equals("COD", true))
                                firstNumber(doc, "total", "grandTotal", "orderTotal") else 0.0,
                            settlementStatus = doc.getString("riderSettlementStatus")
                                ?: doc.getString("settlementStatus")
                                ?: "UNSETTLED"
                        )
                    } ?: emptyList()
                    loadMessage = if (deliveries.isEmpty()) "No delivered orders yet" else ""
                }

            settlementsListener = db.collection("rider_settlements")
                .whereEqualTo("riderId", riderId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { value, _ ->
                    settlements = value?.documents?.map { doc ->
                        RiderSettlementItem(
                            id = doc.id,
                            period = doc.getString("period") ?: "Settlement",
                            deliveryCount = (doc.getLong("deliveryCount") ?: 0).toInt(),
                            grossEarning = firstNumber(doc, "grossEarning"),
                            codCollected = firstNumber(doc, "codCollected"),
                            netPayable = firstNumber(doc, "netPayable"),
                            status = doc.getString("status") ?: "PROCESSING",
                            paymentMode = doc.getString("paymentMode") ?: "",
                            paymentReference = doc.getString("paymentReference") ?: "",
                            paidDate = doc.getString("paidDateText") ?: "",
                            adminRemark = doc.getString("adminRemark") ?: ""
                        )
                    } ?: emptyList()
                }

            onDispose {
                ordersListener?.remove()
                settlementsListener?.remove()
            }
        }
    }

    val selectedDeliveries = deliveries.filter {
        it.orderTime >= selectedWeek.start && it.orderTime < selectedWeek.endExclusive
    }
    val totalEarning = selectedDeliveries.sumOf { it.riderPay }
    val availableEarning = selectedDeliveries
        .filter { !it.settlementStatus.equals("PAID", true) && !it.settlementStatus.equals("SETTLED", true) }
        .sumOf { it.riderPay }
    val codTotal = selectedDeliveries.sumOf { it.codAmount }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Earnings & Settlements",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (selectedWeek.isCurrent) "This Week • ${selectedWeek.fullLabel}"
                else selectedWeek.fullLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(weeks) { week ->
                    Card(
                        modifier = Modifier.clickable { selectedWeek = week },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedWeek == week)
                                Color(0xFF6C4DFF) else Color(0xFFF0EDF8)
                        )
                    ) {
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Text(
                                if (week.isCurrent) "This Week" else "Previous Week",
                                color = if (selectedWeek == week) Color.White else Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                week.shortLabel,
                                color = if (selectedWeek == week) Color.White else Color.DarkGray
                            )
                        }
                    }
                }
            }
        }

        item {
            EarningsSummaryCard(
                title = if (selectedWeek.isCurrent) "Available Earnings • This Week" else "Available Earnings",
                amount = availableEarning,
                subtitle = "Total earning ₹${money(totalEarning)} • ${selectedDeliveries.size} deliveries"
            )
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    EarningsSummaryCard("COD Collected", codTotal, "Cash collected for selected week")
                }
                Column(Modifier.weight(1f)) {
                    EarningsSummaryCard(
                        "Deliveries",
                        selectedDeliveries.size.toDouble(),
                        "Completed orders",
                        showRupee = false
                    )
                }
            }
        }

        if (loadMessage.isNotBlank()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Text(loadMessage, Modifier.padding(16.dp))
                }
            }
        }

        item { Text("Settlements", fontWeight = FontWeight.Bold) }
        if (settlements.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Text("No settlements available yet", Modifier.padding(18.dp))
                }
            }
        } else {
            items(settlements, key = { it.id }) { settlement ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(settlement.period, fontWeight = FontWeight.Bold)
                            Text(settlement.status)
                        }
                        Text("${settlement.deliveryCount} deliveries • Earnings ₹${money(settlement.grossEarning)}")
                        Text("COD ₹${money(settlement.codCollected)} • Net payable ₹${money(settlement.netPayable)}")
                        if (settlement.paymentMode.isNotBlank()) {
                            Text("${settlement.paymentMode} • ${settlement.paidDate}")
                        }
                        if (settlement.paymentReference.isNotBlank()) {
                            Text("Reference: ${settlement.paymentReference}")
                        }
                        if (settlement.adminRemark.isNotBlank()) {
                            Text("Admin note: ${settlement.adminRemark}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EarningsSummaryCard(
    title: String,
    amount: Double,
    subtitle: String,
    showRupee: Boolean = true
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title)
            Text(
                if (showRupee) "₹${money(amount)}" else money(amount),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun buildLastFiveWeeks(): List<RiderWeek> {
    val startOfThisWeek = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val shortFormat = SimpleDateFormat("d MMM", Locale.getDefault())
    val fullFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    return (0 until 5).map { offset ->
        val startCalendar = (startOfThisWeek.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -7 * offset)
        }
        val endExclusiveCalendar = (startCalendar.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 7)
        }
        val sundayCalendar = (endExclusiveCalendar.clone() as Calendar).apply {
            add(Calendar.MILLISECOND, -1)
        }
        RiderWeek(
            start = startCalendar.timeInMillis,
            endExclusive = endExclusiveCalendar.timeInMillis,
            shortLabel = "${shortFormat.format(startCalendar.time)} - ${shortFormat.format(sundayCalendar.time)}",
            fullLabel = "${fullFormat.format(startCalendar.time)} - ${fullFormat.format(sundayCalendar.time)}",
            isCurrent = offset == 0
        )
    }
}

private fun orderArrivalTime(doc: DocumentSnapshot): Long {
    val fields = listOf("timestamp", "createdAt", "orderPlacedAt", "placedAt", "deliveredAt")
    for (field in fields) {
        when (val value = doc.get(field)) {
            is Timestamp -> return value.toDate().time
            is Date -> return value.time
            is Number -> return value.toLong()
        }
    }
    return 0L
}

private fun firstNumber(doc: DocumentSnapshot, vararg fields: String): Double {
    for (field in fields) {
        val value = doc.get(field)
        if (value is Number) return value.toDouble()
        value?.toString()?.toDoubleOrNull()?.let { return it }
    }
    return 0.0
}

private fun money(value: Double): String = if (value % 1.0 == 0.0) {
    value.toLong().toString()
} else {
    String.format(Locale.US, "%.2f", value)
}