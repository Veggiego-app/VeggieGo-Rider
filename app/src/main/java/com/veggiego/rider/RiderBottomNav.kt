package com.veggiego.rider

sealed class RiderBottomNav(

    val route: String,

    val title: String

) {

    object Home :
        RiderBottomNav(
            "home",
            "Home"
        )

    object Orders :
        RiderBottomNav(
            "orders",
            "Orders"
        )

    object History :
        RiderBottomNav(
            "history",
            "History"
        )

    object Profile :
        RiderBottomNav(
            "profile",
            "Profile"
        )
}