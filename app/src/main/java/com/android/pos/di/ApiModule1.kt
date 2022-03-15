package com.android.pos.di

import android.content.Context
import com.android.pos.BuildConfig
import com.android.pos.data.remote.ApiService1
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ApiModule1 @Inject constructor(@ApplicationContext context: Context) {

    private val BASE_URL = "https://mppg.magensa.net/v4/MPPGv4Service.svc/JSON/"

//    @Singleton
//    @Provides
//    fun provideNetworkConnectionInterceptor(
//        @ApplicationContext app: Context
//    ) = NetworkConnectionInterceptor1(app)
//

    fun getRetrofit1(): ApiService1 =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(
                OkHttpClient.Builder().connectTimeout(1, TimeUnit.MINUTES)
                    .addInterceptor { chain ->
                        chain.proceed(chain.request().newBuilder().also {
                        }.build())
                    }.also { client ->
                        if (BuildConfig.DEBUG) {
                            val logging = HttpLoggingInterceptor()
                            logging.setLevel(HttpLoggingInterceptor.Level.BODY)
                            client.addInterceptor(logging)
                            // client.addInterceptor(networkConnectionInterceptor)
                        }
                    }.build()
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService1::class.java)
}