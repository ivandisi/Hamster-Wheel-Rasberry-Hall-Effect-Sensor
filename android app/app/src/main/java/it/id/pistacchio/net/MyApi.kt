package it.id.pistacchio.net

import it.id.pistacchio.Constants
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object MyApi {
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(Constants.GetAPI.URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val instance: GetService by lazy {
        retrofit.create(GetService::class.java)
    }
}