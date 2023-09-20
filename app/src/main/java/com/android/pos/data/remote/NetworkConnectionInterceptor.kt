package com.android.pos.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.android.pos.R
import com.android.pos.utils.extensions.NoInternetException
import okhttp3.Interceptor
import okhttp3.Response


class NetworkConnectionInterceptor(
    context: Context
) : Interceptor {

    private val applicationContext = context.applicationContext


    //hidden by zeeshan
    override fun intercept(chain: Interceptor.Chain): Response {
        if (!isInternetAvailable())
            try {

                throw NoInternetException(applicationContext.getString(R.string.no_internet))
            }catch (e:Exception){e.printStackTrace()}
        return chain.proceed(chain.request())
    }

    private fun isInternetAvailable(): Boolean {
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