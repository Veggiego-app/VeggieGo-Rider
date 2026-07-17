package com.veggiego.rider

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*

@Composable
fun RiderMainScreen() {

    val navController = rememberNavController()

    LaunchedEffect(Unit) {
        navController.navigate("home")
    }

    val currentRoute =
        navController
            .currentBackStackEntryAsState()
            .value
            ?.destination
            ?.route ?: "home"

    Scaffold(

        bottomBar = {

            Row(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(
                            horizontal = 8.dp,
                            vertical = 10.dp
                        ),

                horizontalArrangement =
                    Arrangement.SpaceEvenly

            ) {

                BottomItem(
                    title = "🏠 Home",
                    selected =
                        currentRoute == "home"
                ) {
                    navController.navigate("home")
                }

                BottomItem(
                    title = "📦 Orders",
                    selected =
                        currentRoute == "orders"
                ) {
                    navController.navigate("orders")
                }

                BottomItem(
                    title = "📜 History",
                    selected =
                        currentRoute == "history"
                ) {
                    navController.navigate("history")
                }

                BottomItem(
                    title = "👤 ",
                    selected =
                        currentRoute == "profile"
                ) {
                    navController.navigate("profile")
                }
            }
        }

    ) { padding ->

        NavHost(

            navController =
                navController,

            startDestination =
                "home",

            modifier =
                Modifier.padding(padding)

        ) {

            composable("home") {
                HomeScreen()
            }

            composable("orders") {
                NewOrderScreen()
            }

            composable("history") {
                HistoryScreen()
            }

            composable("profile") {
                ProfileScreen()
            }
        }
    }
}

@Composable
fun BottomItem(

    title: String,

    selected: Boolean,

    onClick: () -> Unit

) {

    Box(

        modifier =

            Modifier

                .clip(
                    RoundedCornerShape(50)
                )

                .background(

                    if (selected)
                        Color(0xFF6C4DFF)
                    else
                        Color.Transparent
                )

                .clickable {
                    onClick()
                }

                .padding(
                    horizontal = 16.dp,
                    vertical = 10.dp
                )

    ) {

        Text(

            text = title,

            color =

                if (selected)
                    Color.White
                else
                    Color.Black,

            fontWeight =

                if (selected)
                    FontWeight.Bold
                else
                    FontWeight.Normal
        )
    }
}