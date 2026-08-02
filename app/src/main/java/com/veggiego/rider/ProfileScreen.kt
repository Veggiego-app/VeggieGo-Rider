package com.veggiego.rider

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.shape.RoundedCornerShape
import com.google.firebase.Timestamp

@Composable
fun ProfileScreen() {

    val context = LocalContext.current

    val db = FirebaseFirestore.getInstance()

    val riderId =
        FirebaseAuth.getInstance()
            .currentUser
            ?.uid ?: ""

    var riderName by remember {
        mutableStateOf("")
    }

    var riderPhone by remember {
        mutableStateOf("")
    }

    var riderStatus by remember {
        mutableStateOf("")
    }

    var riderOnline by remember {
        mutableStateOf(false)
    }

    var vehicleNumber by remember {
        mutableStateOf("")
    }

    var licenseNumber by remember {
        mutableStateOf("")
    }
    var riderCode by remember {

        mutableStateOf("")

    }

    var riderZone by remember {

        mutableStateOf("")

    }

    var joinedOn by remember {
        mutableStateOf("")
    }
    LaunchedEffect(riderId) {

        if (riderId.isEmpty())
            return@LaunchedEffect

        db.collection("riders")

            .document(riderId)

            .addSnapshotListener { snapshot, _ ->

                if (snapshot == null || !snapshot.exists())
                    return@addSnapshotListener

                riderName =
                    snapshot.getString("name") ?: ""

                riderPhone =
                    snapshot.getString("phone") ?: ""

                riderStatus =
                    snapshot.getString("status") ?: "PENDING"

                riderOnline =
                    snapshot.getBoolean("online") ?: false

                vehicleNumber =
                    snapshot.getString("vehicleNumber") ?: ""

                licenseNumber =
                    snapshot.getString("licenseNumber") ?: ""

                riderCode =

                    snapshot.getString(
                        "riderCode"
                    ) ?: ""

                riderZone =

                    snapshot.getString(
                        "zone"
                    ) ?: ""

                val joinedTimestamp =
                    snapshot.getTimestamp("createdAt")

                if (joinedTimestamp != null) {

                    joinedOn =

                        SimpleDateFormat(

                            "dd MMM yyyy",

                            Locale.getDefault()

                        ).format(joinedTimestamp.toDate())

                } else {

                    joinedOn = "-"

                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Text(
            text = "👤 Rider Profile",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(

            modifier = Modifier.fillMaxWidth(),

            elevation = CardDefaults.cardElevation(8.dp),

            shape = RoundedCornerShape(20.dp)

        ) {

            Column(

                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),

                horizontalAlignment = Alignment.CenterHorizontally

            ) {
                Surface(
                    shape = CircleShape
                ) {

                    Text(
                        text = "🛵",
                        modifier = Modifier.padding(24.dp)
                    )
                }

                Spacer(
                    modifier = Modifier.height(20.dp)
                )

                Text(

                    text = riderName,

                    fontSize = 22.sp,

                    fontWeight = FontWeight.Bold

                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(

                    text = riderPhone,

                    color = MaterialTheme.colorScheme.onSurfaceVariant

                )

                Spacer(
                    modifier = Modifier.height(24.dp)
                )

                Divider()

                Spacer(
                    modifier = Modifier.height(20.dp)
                )

                ProfileRow(

                    title = "🆔 Rider ID",

                    value = riderCode

                )

                ProfileRow(

                    title = "📍 Zone",

                    value = riderZone

                )

                ProfileRow(

                    title = "🚗 Vehicle",

                    value = vehicleNumber

                )

                ProfileRow(

                    title = "🪪 License",

                    value = licenseNumber

                )

                ProfileRow(

                    title = "🟢 Status",

                    value = riderStatus

                )

                ProfileRow(

                    title = "📅 Joined",

                    value = joinedOn

                )

                ProfileRow(

                    title = "📡 Availability",

                    value = if (riderOnline)
                        "ONLINE"
                    else
                        "OFFLINE"

                )

                Spacer(
                    modifier = Modifier.height(28.dp)
                )

                Button(

                    onClick = {
                        val auth = FirebaseAuth.getInstance()
                        val uid = auth.currentUser?.uid
                        val activity = context as MainActivity
                        activity.stopRiderLocationService()

                        if (uid == null) {
                            auth.signOut()
                            activity.recreate()
                        } else {
                            FirebaseFirestore.getInstance()
                                .collection("riders")
                                .document(uid)
                                .update(
                                    mapOf(
                                        "online" to false,
                                        "availableForOrders" to false
                                    )
                                )
                                .addOnCompleteListener {
                                    auth.signOut()
                                    activity.recreate()
                                }
                        }
                    },

                    modifier =
                        Modifier.fillMaxWidth()

                ) {

                    Text("🚪 Logout")
                }
            }
        }
    }
}
@Composable
fun ProfileRow(

    title: String,

    value: String

) {

    Row(

        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),

        horizontalArrangement =
            Arrangement.SpaceBetween

    ) {

        Text(

            text = title,

            fontWeight = FontWeight.SemiBold

        )

        Text(

            text = value

        )

    }

}
