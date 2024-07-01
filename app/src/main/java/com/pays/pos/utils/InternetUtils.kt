package com.pays.pos.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.pays.pos.MainApplication
import com.pays.pos.di.ApiModule
import java.net.InetAddress

object InternetUtils {

    fun isServerReachable(): Boolean // To check if server is reachable
    {
        return try {
            InetAddress.getByName(ApiModule.BASE_URL).isReachable(3000) //Replace with your name
            true
        } catch (e: java.lang.Exception) {
            false
        }
    }

    fun isInternetAvailable(applicationContext: Context): Boolean {
        var result: Boolean
        val connectivityManager =
            applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        connectivityManager.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                it.getNetworkCapabilities(connectivityManager.activeNetwork)?.apply {
                    result = when {
                        hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                        hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                        hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                        else -> false
                    }
                    return result
                }
            } else {
                connectivityManager.activeNetworkInfo.also {
                    return it != null && it.isConnected
                }
            }
        }
        return false
    }
}