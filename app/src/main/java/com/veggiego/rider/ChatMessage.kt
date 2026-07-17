package com.veggiego.rider

data class ChatMessage(

    val id: String = "",

    val senderId: String = "",

    val senderType: String = "",

    val message: String = "",

    val timestamp: Long = 0L,

    val seen: Boolean = false
)