package com.veggiego.rider

data class DirectionResponse(

    val routes:
    List<RouteData>
)

data class RouteData(

    val overview_polyline:
    OverviewPolylineData
)

data class OverviewPolylineData(

    val points: String
)