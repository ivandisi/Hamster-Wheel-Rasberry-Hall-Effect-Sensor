package it.id.pistacchio.net

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.annotation.RequiresPermission
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class MyInterceptor (
        private val context: Context
    ) : Interceptor {

        @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
        override fun intercept(chain: Interceptor.Chain): Response {
            if (!isNetworkAvailable(context)) {
                throw NoInternetException("No Network")
            }

            return chain.proceed(chain.request())
        }

        @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
        private fun isNetworkAvailable(context: Context): Boolean {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
    }

class NoInternetException(message: String) : IOException(message)