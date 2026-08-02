package com.veggiego.rider

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DeliveryHistoryItem(
    val id: String,
    val restaurantName: String,
    val area: String,
    val deliveredAt: Long,
    val distanceKm: Double,
    val riderPay: Int,
    val paymentMethod: String,
    val codCollected: Int,
    val settlementStatus: String
)

@Composable
fun HistoryScreen(onBack: () -> Unit = {}) {
    val riderId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val db = FirebaseFirestore.getInstance()
    var orders by remember { mutableStateOf<List<DeliveryHistoryItem>>(emptyList()) }
    var lastDocument by remember { mutableStateOf<DocumentSnapshot?>(null) }
    var loading by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }
    var queryText by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }

    fun loadNextPage(reset: Boolean = false) {
        if (loading || (!hasMore && !reset) || riderId.isBlank()) return
        if (reset) {
            orders = emptyList()
            lastDocument = null
            hasMore = true
        }
        loading = true
        var query: Query = db.collection("orders")
            .whereEqualTo("riderId", riderId)
            .whereEqualTo("deliveryStatus", "DELIVERED")
            .orderBy("deliveredAt", Query.Direction.DESCENDING)
            .limit(20)
        lastDocument?.let { query = query.startAfter(it) }
        query.get().addOnSuccessListener { snap ->
            val page = snap.documents.map { doc ->
                val rawDate = doc.get("deliveredAt")
                val delivered = when (rawDate) {
                    is Timestamp -> rawDate.toDate().time
                    is Number -> rawDate.toLong()
                    else -> doc.getLong("timestamp") ?: 0L
                }
                DeliveryHistoryItem(
                    id = doc.getString("orderId") ?: doc.id,
                    restaurantName = doc.getString("restaurantName") ?: "Restaurant",
                    area = doc.getString("area") ?: doc.getString("city") ?: "-",
                    deliveredAt = delivered,
                    distanceKm = (doc.get("deliveryDistance") as? Number)?.toDouble()
                        ?: (doc.get("distanceKm") as? Number)?.toDouble() ?: 0.0,
                    riderPay = (doc.get("riderPay") as? Number)?.toInt() ?: 0,
                    paymentMethod = doc.getString("paymentMethod") ?: "COD",
                    codCollected = if (doc.getBoolean("cashCollected") == true)
                        (doc.get("total") as? Number)?.toInt() ?: 0 else 0,
                    settlementStatus = doc.getString("riderSettlementStatus") ?: "UNSETTLED"
                )
            }
            orders = (orders + page).distinctBy { it.id }
            lastDocument = snap.documents.lastOrNull()
            hasMore = snap.size() == 20
            loading = false
        }.addOnFailureListener {
            loading = false
            hasMore = false
        }
    }

    LaunchedEffect(riderId) { loadNextPage(reset = true) }

    val now = System.currentTimeMillis()
    val cutoff = when (filter) {
        "Week" -> now - 7L * 24 * 60 * 60 * 1000
        "Month" -> now - 31L * 24 * 60 * 60 * 1000
        else -> 0L
    }
    val visible = orders.filter {
        it.deliveredAt >= cutoff &&
                (queryText.isBlank() || it.id.contains(queryText, true) ||
                        it.restaurantName.contains(queryText, true))
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onBack) { Text("Back") }
            Text("Order History", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        OutlinedTextField(
            value = queryText,
            onValueChange = { queryText = it },
            label = { Text("Search order ID or restaurant") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All", "Week", "Month").forEach { label ->
                AssistChip(onClick = { filter = label }, label = { Text(label) })
            }
        }
        Text("${visible.size} loaded deliveries • Earnings ₹${visible.sumOf { it.riderPay }}")

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(visible, key = { _, item -> item.id }) { index, order ->
                LaunchedEffect(index, hasMore, loading) {
                    if (index >= visible.lastIndex - 3 && hasMore && !loading) loadNextPage()
                }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("#${order.id}", fontWeight = FontWeight.Bold)
                            Text("DELIVERED")
                        }
                        Text(order.restaurantName, fontWeight = FontWeight.Bold)
                        Text(order.area)
                        Text("${formatHistoryDate(order.deliveredAt)} • ${"%.1f".format(order.distanceKm)} km")
                        Text("Earning ₹${order.riderPay}", fontWeight = FontWeight.Bold)
                        Text(
                            if (order.paymentMethod.equals("COD", true))
                                "COD ₹${order.codCollected} collected • ${order.settlementStatus}"
                            else "Online Paid • ${order.settlementStatus}"
                        )
                    }
                }
            }
            if (loading) {
                item {
                    Text(
                        "Loading deliveries…",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            if (!hasMore && orders.isNotEmpty()) item { Text("No more deliveries", Modifier.padding(16.dp)) }
            if (!loading && orders.isEmpty()) item { Text("No deliveries found", Modifier.padding(16.dp)) }
        }
    }
}

private fun formatHistoryDate(value: Long): String =
    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(value))