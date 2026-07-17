package com.veggiego.rider

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import com.google.maps.android.compose.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun RiderMapScreen(

    order: RiderOrder

) {

    val context =
        LocalContext.current

    // ✅ RIDER LOCATION

    val riderLocation =

        LatLng(
            23.0753,
            70.1337
        )

    // ✅ REAL CUSTOMER LOCATION

    val customerLocation =

        LatLng(

            order.customerLat,

            order.customerLng
        )

    // ✅ ROUTE POINTS

    var routePoints by remember {

        mutableStateOf<List<LatLng>>(
            emptyList()
        )
    }

    // ✅ LOAD REAL ROAD ROUTE

    LaunchedEffect(Unit) {

        val origin =
            "${riderLocation.latitude},${riderLocation.longitude}"

        val destination =
            "${customerLocation.latitude},${customerLocation.longitude}"

        RetrofitClient.api.getDirections(

            origin,

            destination,

            "AIzaSyB7-In-M9L3o9BdUxAMcQiad7VfMj74gCI"

        ).enqueue(

            object : Callback<DirectionResponse> {

                override fun onResponse(

                    call: Call<DirectionResponse>,

                    response: Response<DirectionResponse>

                ) {

                    if (response.isSuccessful) {

                        val points =

                            response.body()
                                ?.routes
                                ?.firstOrNull()
                                ?.overview_polyline
                                ?.points

                        if (points != null) {

                            routePoints =

                                PolyUtil.decode(points)
                        }
                    }
                }

                override fun onFailure(

                    call: Call<DirectionResponse>,

                    t: Throwable

                ) {

                }
            }
        )
    }

    // ✅ CAMERA

    val cameraPositionState =

        rememberCameraPositionState {

            position =

                CameraPosition
                    .fromLatLngZoom(

                        riderLocation,

                        14f
                    )
        }

    // ✅ MAP UI

    Box(

        modifier =
            Modifier.fillMaxSize()

    ) {

        GoogleMap(

            modifier =
                Modifier.fillMaxSize(),

            cameraPositionState =
                cameraPositionState

        ) {

            // ✅ RIDER MARKER

            Marker(

                state =
                    MarkerState(
                        position =
                            riderLocation
                    ),

                title =
                    "🚚 Rider"
            )

            // ✅ CUSTOMER MARKER

            Marker(

                state =
                    MarkerState(
                        position =
                            customerLocation
                    ),

                title =
                    "🏠 Customer"
            )

            // ✅ ROUTE POLYLINE

            if (routePoints.isNotEmpty()) {

                Polyline(

                    points =
                        routePoints,

                    width = 14f
                )
            }
        }

        // ✅ NAVIGATION BUTTON

        Button(

            onClick = {

                val uri = Uri.parse(

                    "google.navigation:q=" +

                            "${customerLocation.latitude}," +

                            customerLocation.longitude
                )

                val intent =

                    Intent(

                        Intent.ACTION_VIEW,

                        uri
                    )

                intent.setPackage(
                    "com.google.android.apps.maps"
                )

                context.startActivity(intent)
            },

            modifier =
                Modifier.align(
                    Alignment.BottomCenter
                )

        ) {

            Text(
                "🗺 Navigate"
            )
        }
    }
}