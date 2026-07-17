package com.veggiego.rider

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    val api: RouteApi by lazy {

        Retrofit.Builder()

            .baseUrl(
                "https://maps.googleapis.com/"
            )

            .addConverterFactory(
                GsonConverterFactory.create()
            )

            .build()

            .create(RouteApi::class.java)
    }
}