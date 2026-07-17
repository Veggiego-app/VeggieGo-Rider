package com.veggiego.rider

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
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun RiderChatScreen(

    navController: NavController,

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
                    }
                }
            }
    }

    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .imePadding()

    ) {

        TopAppBar(

            title = {

                Text(
                    "Customer Chat"
                )
            }
        )

        LazyColumn(

            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),

            contentPadding =
                PaddingValues(
                    top = 12.dp,
                    bottom = 4.dp
                )
        ) {

            items(messages) { msg ->

                val isMine =

                    msg.senderId ==
                            currentUser

                Row(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                vertical = 4.dp
                            ),

                    horizontalArrangement =

                        if (isMine)

                            Arrangement.End

                        else

                            Arrangement.Start
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

                            text = msg.message,

                            modifier =
                                Modifier.padding(
                                    14.dp
                                ),

                            color =

                                if (isMine)

                                    Color.White

                                else

                                    Color.Black,

                            fontSize = 16.sp
                        )
                    }
                }
            }
        }

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(
                        start = 12.dp,
                        end = 12.dp,
                        top = 2.dp,
                        bottom = 2.dp
                    ),

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
                },

                shape =
                    RoundedCornerShape(
                        30.dp
                    )
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

                        val msg =

                            ChatMessage(

                                senderId =
                                    currentUser,

                                senderType =
                                    "rider",

                                message =
                                    messageText,

                                timestamp =
                                    System.currentTimeMillis()
                            )

                        FirebaseFirestore
                            .getInstance()
                            .collection("chats")
                            .document(orderId)
                            .collection("messages")
                            .add(msg)

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