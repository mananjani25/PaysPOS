package com.pays.pos.di


import android.content.Context
import android.net.ConnectivityManager
import com.pays.pos.di.ApiModule.BASE_URL
import javax.inject.Singleton
import javax.inject.Inject
import com.pays.pos.di.PrefProvider
import com.pays.pos.utils.InternetUtils
import com.pays.pos.utils.extensions.NoInternetException
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.URISyntaxException
import kotlin.Throws

/*
  Solution from @swankjesse
  Host Selection Retrofit
  More at @link https://github.com/square/retrofit/issues/1404#issuecomment-207408548
*/
@Singleton
class HostSelectionInterceptor @Inject constructor(internal var preferenceHelper: PrefProvider, @ApplicationContext val context: Context) :
    Interceptor {
    internal var host: HttpUrl = BASE_URL.toHttpUrl()

    fun setHostBaseUrl() {
        host = preferenceHelper.getBaseUrl().toHttpUrl()
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        if (!isInternetAvailable()){
            return throw NoInternetException("Network error")
        }
        var request: Request = chain.request()
        var newUrl: HttpUrl? = null
        try {
            newUrl = request.url.newBuilder()
                .scheme(host.scheme)
                .host(host.toUrl().toURI().host)
                .build()
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }
        assert(newUrl != null)
        request = request.newBuilder()
            .url(newUrl!!)
            .build()
        return chain.proceed(request)
    }

    init {
        setHostBaseUrl()
    }


    private fun isInternetAvailable(): Boolean {
//        if in some case it happens that the network is not reachable, then check this. The context must be coming null
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
    }
}