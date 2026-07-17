package com.veggiego.rider

import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.firestore.SetOptions

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {

    val context =
        LocalContext.current

    val auth =
        FirebaseAuth.getInstance()

    val db =
        FirebaseFirestore.getInstance()

    var phone by remember {
        mutableStateOf("")
    }

    var otp by remember {
        mutableStateOf("")
    }

    var verificationId by remember {
        mutableStateOf("")
    }

    var loading by remember {
        mutableStateOf(false)
    }

    var otpSent by remember {
        mutableStateOf(false)
    }

    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .padding(24.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center

    ) {

        Text(

            text =
                "🚚 VeggieGo Rider",

            style =
                MaterialTheme
                    .typography
                    .headlineMedium
        )

        Spacer(
            modifier =
                Modifier.height(24.dp)
        )

        OutlinedTextField(

            value = phone,

            onValueChange = {

                if (

                    it.length <= 10 &&
                    it.all { ch -> ch.isDigit() }

                ) {

                    phone = it

                }

            },

            label = {
                Text("Phone Number")
            },

            keyboardOptions =
                KeyboardOptions(
                    keyboardType =
                        KeyboardType.Number
                ),

            modifier =
                Modifier.fillMaxWidth()
        )

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        if (otpSent) {

            OutlinedTextField(

                value = otp,

                onValueChange = {

                    if (

                        it.length <= 6 &&
                        it.all { ch -> ch.isDigit() }

                    ) {

                        otp = it

                    }

                },

                label = {
                    Text("Enter OTP")
                },

                keyboardOptions =
                    KeyboardOptions(
                        keyboardType =
                            KeyboardType.Number
                    ),

                modifier =
                    Modifier.fillMaxWidth()
            )

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            Button(

                enabled = otp.length == 6,

                onClick = {

                    loading = true

                    val credential =

                        PhoneAuthProvider
                            .getCredential(
                                verificationId,
                                otp
                            )

                    auth.signInWithCredential(
                        credential
                    )

                        .addOnCompleteListener {

                            loading = false

                            if (it.isSuccessful) {

                                val uid =
                                    auth.currentUser
                                        ?.uid ?: ""

                                val riderRef =
                                    db.collection("riders")
                                        .document(uid)

                                riderRef.get()

                                    .addOnSuccessListener { document ->


                                        if (!document.exists()) {

                                            riderRef.set(

                                                hashMapOf(

                                                    "uid" to uid,
                                                    "phone" to phone,
                                                    "name" to "",
                                                    "zone" to "",
                                                    "vehicleNumber" to "",
                                                    "licenseNumber" to "",
                                                    "riderCode" to "",
                                                    "status" to "PENDING",
                                                    "online" to false,
                                                    "activeOrderId" to "",
                                                    "totalDeliveries" to 0,
                                                    "earnings" to 0,
                                                    "createdAt" to
                                                            com.google.firebase.firestore.FieldValue.serverTimestamp()

                                                ),

                                                SetOptions.merge()

                                            )

                                                .addOnSuccessListener {

                                                    Toast.makeText(

                                                        context,

                                                        "⏳ Waiting For Admin Approval",

                                                        Toast.LENGTH_LONG

                                                    ).show()

                                                    auth.signOut()

                                                }

                                        } else {

                                            val status =
                                                document.getString("status")
                                                    ?: "PENDING"

                                            when (status) {

                                                "APPROVED" -> {

                                                    onLoginSuccess()

                                                }

                                                "REJECTED" -> {

                                                    Toast.makeText(

                                                        context,

                                                        "❌ Rider Rejected By Admin",

                                                        Toast.LENGTH_LONG

                                                    ).show()

                                                    auth.signOut()

                                                }

                                                else -> {

                                                    Toast.makeText(

                                                        context,

                                                        "⏳ Waiting For Admin Approval",

                                                        Toast.LENGTH_LONG

                                                    ).show()

                                                    auth.signOut()

                                                }

                                            }

                                        }

                                    }
                                    .addOnFailureListener { e ->

                                        Toast.makeText(
                                            context,
                                            e.message,
                                            Toast.LENGTH_LONG
                                        ).show()

                                    }

                            } else {

                                Toast.makeText(

                                    context,

                                    "❌ Invalid OTP",

                                    Toast.LENGTH_SHORT

                                ).show()
                            }
                        }
                },

                modifier =
                    Modifier.fillMaxWidth()

            ) {

                Text("Verify OTP")
            }

        } else {

            Button(

                enabled = phone.length == 10,

                onClick = {

                    loading = true

                    val options =

                        PhoneAuthOptions
                            .newBuilder(auth)

                            .setPhoneNumber(
                                "+91$phone"
                            )

                            .setTimeout(
                                60L,
                                TimeUnit.SECONDS
                            )

                            .setActivity(
                                (context as? MainActivity)
                                    ?: return@Button
                            )

                            .setCallbacks(

                                object :
                                    PhoneAuthProvider
                                    .OnVerificationStateChangedCallbacks() {

                                    override fun onVerificationCompleted(
                                        credential: PhoneAuthCredential
                                    ) {
                                    }

                                    override fun onVerificationFailed(
                                        e: FirebaseException
                                    ) {

                                        loading = false

                                        Toast.makeText(

                                            context,

                                            e.message,

                                            Toast.LENGTH_LONG

                                        ).show()
                                    }

                                    override fun onCodeSent(

                                        id: String,

                                        token:
                                        PhoneAuthProvider
                                        .ForceResendingToken

                                    ) {

                                        loading = false

                                        verificationId =
                                            id

                                        otpSent = true

                                        Toast.makeText(

                                            context,

                                            "✅ OTP Sent",

                                            Toast.LENGTH_SHORT

                                        ).show()
                                    }
                                }
                            )

                            .build()

                    PhoneAuthProvider
                        .verifyPhoneNumber(
                            options
                        )
                },

                modifier =
                    Modifier.fillMaxWidth()

            ) {

                Text("Send OTP")
            }
        }

        if (loading) {

            Spacer(
                modifier =
                    Modifier.height(24.dp)
            )

            Text(
                text = "Loading..."
            )
        }
    }
}

