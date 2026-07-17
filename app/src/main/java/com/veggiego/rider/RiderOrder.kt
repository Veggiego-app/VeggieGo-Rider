package com.veggiego.rider

data class RiderOrder(

    val id: String = "",

    // Customer
    val customerName: String = "",
    val customerPhone: String = "",
    val customerAddress: String = "",

    // Restaurant
    val restaurantName: String = "",
    val restaurantAddress: String = "",

    // Amount
    val total: Int = 0,

    // Rider
    val riderId: String = "",
    val riderName: String = "",

    // Order Status
    val status: String = "",
    val deliveryStatus: String = "",

    // Navigation Flow
    val navigationStage: String = "TO_RESTAURANT",

    // Customer Location
    val customerLat: Double = 0.0,
    val customerLng: Double = 0.0,

    // Restaurant Location
    val restaurantLat: Double = 0.0,
    val restaurantLng: Double = 0.0,

    // Distance
    val pickupDistance: Double = 0.0,
    val dropDistance: Double = 0.0,
    val tripDistance: Double = 0.0,

    // Rider Earnings
    val riderEarning: Int = 0,

    val riderPay: Int = 0,

// Time
    val createdAt: Long = 0L,

// Payment
    val cashCollected: Boolean = false,
    val paymentReceived: Boolean = false
)