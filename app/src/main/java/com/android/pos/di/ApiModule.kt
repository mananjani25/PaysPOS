package com.android.pos.di

import android.content.Context
import com.android.pos.BuildConfig
import com.android.pos.data.remote.ApiServie
import com.android.pos.data.remote.NetworkConnectionInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    private const val BASE_URL_Meal = "http://13.56.17.221/api/v1/"

    @Singleton
    @Provides
    fun provideNetworkConnectionInterceptor(
        @ApplicationContext app: Context
    ) = NetworkConnectionInterceptor(app)


    @Singleton
    @Provides
    fun getRetrofit(networkConnectionInterceptor: NetworkConnectionInterceptor): ApiServie =
        Retrofit.Builder()
            .baseUrl(BASE_URL_Meal)
            .client(
                OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        chain.proceed(chain.request().newBuilder().also {
                            //it.addHeader("Authorization", "Bearer $authToken")
                        }.build())
                    }.also { client ->
                        if (BuildConfig.DEBUG) {
                            val logging = HttpLoggingInterceptor()
                            logging.setLevel(HttpLoggingInterceptor.Level.BODY)
                            client.addInterceptor(logging)
                            client.addInterceptor(networkConnectionInterceptor)
                        }
                    }.build()
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiServie::class.java)
}