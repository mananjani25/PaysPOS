package com.android.pos.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.android.pos.R
import com.android.pos.di.ApiModule
import com.android.pos.utils.extensions.NoInternetException
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.net.URISyntaxException


class NetworkConnectionInterceptor(
    context: Context
) : Interceptor {

    private val applicationContext = context.applicationContext

    private var host ="https://boldpos.site/api/v1/".toHttpUrlOrNull()

    //hidden by zeeshan
    override fun intercept(chain: Interceptor.Chain): Response {
        if (!isInternetAvailable())

            throw NoInternetException(applicationContext.getString(R.string.no_internet))
        return chain.proceed(chain.request())
    }

    //added by zeeshan
/*
    override fun intercept(chain: Interceptor.Chain): Response {
        if (!isInternetAvailable())
            throw NoInternetException(applicationContext.getString(R.string.no_internet))
        else {
            var request: Request = chain.request()
            if (host != null) {
                var newUrl: HttpUrl? = null
                try {
                    newUrl = host?.scheme?.let {
                        host?.toUrl()?.toURI()?.host?.let { it1 ->
                            request.url.newBuilder()
                                .scheme(it)
                                .host(it1)
                                .build()
                        }
                    }
                } catch (e: URISyntaxException) {
                    e.printStackTrace()
                }
                assert(newUrl != null)
                request = request.newBuilder()
                    .url(newUrl!!)
                    .build()
            }
            return chain.proceed(request)
        }
    }
*/

    //added by zeeshan
    fun setHostBaseUrl(host: String) {
        this.host = host.toHttpUrlOrNull()
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