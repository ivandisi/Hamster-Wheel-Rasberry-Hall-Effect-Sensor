package it.id.pistacchio.net

import android.content.Context
import it.id.pistacchio.Constants
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object MyApi {
    private lateinit var context: Context

    fun init(appContext: Context) {
        context = appContext.applicationContext
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(MyInterceptor(context))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(Constants.GetAPI.URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val instance: GetService by lazy {
        retrofit.create(GetService::class.java)
    }
}
