package com.android.pos.di

import android.content.Context
import com.android.pos.data.remote.ApiService2
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiModule2 @Inject constructor(@ApplicationContext context: Context) {

    private val BASE_URL = "http://poslink.com/poslink/ws/process2.asmx/"  // for SNACK POS

    fun getRetrofit2(): ApiService2 =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(
                OkHttpClient.Builder().connectTimeout(50000, TimeUnit.MILLISECONDS)
                    .readTimeout(100000, TimeUnit.MILLISECONDS)
                    .addInterceptor { chain ->
                        chain.proceed(chain.request().newBuilder().also {
                        }.build())
                    }.also { client ->
//                        if (BuildConfig.DEBUG) {
                        val logging = HttpLoggingInterceptor()
                        logging.setLevel(HttpLoggingInterceptor.Level.BODY)
                        client.addInterceptor(logging)
                        // client.addInterceptor(networkConnectionInterceptor)
//                        }
                    }.build()
            )
            .addConverterFactory(SimpleXmlConverterFactory.create())
            .build()
            .create(ApiService2::class.java)
}