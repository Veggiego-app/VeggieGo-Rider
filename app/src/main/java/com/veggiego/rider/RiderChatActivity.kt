package com.veggiego.rider

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.launch

class RiderChatActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        val orderId =
            intent.getStringExtra("orderId") ?: ""

        setContent {

            RiderChatScreen(orderId)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun RiderChatScreen(
    orderId: String
) {

    val currentUser =

        FirebaseAuth
            .getInstance()
            .currentUser
            ?.uid ?: ""

    var messageText by remember {

        mutableStateOf("")
    }

    val messages = remember {

        mutableStateListOf<ChatMessage>()
    }
    val listState =

        rememberLazyListState()

    val coroutineScope =

        rememberCoroutineScope()

    // REALTIME CHAT

    LaunchedEffect(Unit) {

        FirebaseFirestore
            .getInstance()
            .collection("chats")
            .document(orderId)
            .collection("messages")
            .orderBy("timestamp")

            .addSnapshotListener { value, _ ->

                messages.clear()

                value?.documents?.forEach {

                    val msg =
                        it.toObject(
                            ChatMessage::class.java
                        )

                    if (msg != null) {

                        messages.add(msg)
                        coroutineScope.launch {

                            listState.animateScrollToItem(

                                messages.size
                            )
                        }
                    }
                }
            }
    }

    Column(

        modifier =
            Modifier.fillMaxSize()
    ) {

        TopAppBar(

            title = {

                Text(
                    "💬 Customer Chat"
                )
            }
        )

        LazyColumn(

            state = listState,

            modifier =
                Modifier
                    .weight(1f)
                    .padding(12.dp),

            contentPadding =
                PaddingValues(
                    bottom = 90.dp
                )
        ) {

            items(messages) { msg ->
                LaunchedEffect(msg.id) {

                    if (

                        msg.senderType == "customer"

                        &&

                        !msg.seen

                        &&

                        msg.id.isNotEmpty()
                    ) {

                        FirebaseFirestore
                            .getInstance()
                            .collection("chats")
                            .document(orderId)
                            .collection("messages")
                            .document(msg.id)

                            .update(
                                "seen",
                                true
                            )
                    }
                }
                val isMine =

                    msg.senderId ==
                            currentUser

                Row(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),

                    horizontalArrangement =

                        if (isMine)

                            Arrangement.End

                        else

                            Arrangement.Start
                ) {

                    Column(

                        horizontalAlignment =

                            if (isMine)

                                Alignment.End

                            else

                                Alignment.Start
                    ) {

                        Card(

                            colors =
                                CardDefaults.cardColors(

                                    containerColor =

                                        if (isMine)

                                            Color(
                                                0xFF2962FF
                                            )

                                        else

                                            Color.LightGray
                                ),

                            shape =
                                RoundedCornerShape(
                                    18.dp
                                )
                        ) {

                            Text(

                                text =
                                    msg.message,

                                modifier =
                                    Modifier.padding(
                                        14.dp
                                    ),

                                color =

                                    if (isMine)

                                        Color.White

                                    else

                                        Color.Black,

                                fontSize =
                                    16.sp
                            )
                        }

                        if (isMine) {

                            Text(

                                text =

                                    if (msg.seen)

                                        "✔✔ Seen"

                                    else

                                        "✔ Sent",

                                fontSize = 11.sp,

                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            OutlinedTextField(

                value = messageText,

                onValueChange = {

                    messageText = it
                },

                modifier =
                    Modifier.weight(1f),

                placeholder = {

                    Text(
                        "Reply..."
                    )
                }
            )

            Spacer(
                modifier =
                    Modifier.width(8.dp)
            )

            FloatingActionButton(

                onClick = {

                    if (
                        messageText.isNotEmpty()
                    ) {

                        val messageId =

                            FirebaseFirestore
                                .getInstance()
                                .collection("temp")
                                .document()
                                .id

                        val msg =

                            ChatMessage(

                                id = messageId,

                                senderId =
                                    currentUser,

                                senderType =
                                    "rider",

                                message =
                                    messageText,

                                timestamp =
                                    System.currentTimeMillis(),

                                seen = false
                            )

                        FirebaseFirestore
                            .getInstance()
                            .collection("chats")
                            .document(orderId)
                            .collection("messages")
                            .document(messageId)
                            .set(msg)

                        messageText = ""
                    }
                }

            ) {

                Icon(

                    Icons.Default.Send,

                    contentDescription =
                        null
                )
            }
        }
    }
}