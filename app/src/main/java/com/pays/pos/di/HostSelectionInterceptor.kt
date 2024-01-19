package com.pays.pos.di


import com.pays.pos.di.ApiModule.BASE_URL
import javax.inject.Singleton
import javax.inject.Inject
import com.pays.pos.di.PrefProvider
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
class HostSelectionInterceptor @Inject constructor(internal var preferenceHelper: PrefProvider) :
    Interceptor {
    internal var host: HttpUrl = BASE_URL.toHttpUrl()

    fun setHostBaseUrl() {
        host = preferenceHelper.getBaseUrl().toHttpUrl()
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
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
}