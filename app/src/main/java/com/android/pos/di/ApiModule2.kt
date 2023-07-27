package com.android.pos.di

import android.content.Context
import com.android.pos.BuildConfig
import com.android.pos.MainApplication
import com.android.pos.data.remote.ApiService
import com.android.pos.data.remote.Constants
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
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule2 {

    private const val BASE_URL = "http://poslink.com/poslink/ws/process2.asmx/"  // for SNACK POS

    @Singleton
    @Provides
    fun provideNetworkConnectionInterceptor(
        @ApplicationContext app: Context
    ) = NetworkConnectionInterceptor(app)


    @Provides
    @Singleton
    fun provideHostSelectionInterceptor(preferenceHelper: PrefProvider): HostSelectionInterceptor {
        return HostSelectionInterceptor(preferenceHelper)
    }

    @Provides
    fun getRetrofit(
        networkConnectionInterceptor: NetworkConnectionInterceptor,
        prefProvider: PrefProvider,
        hostSelectionInterceptor: HostSelectionInterceptor,

        ): ApiService =
        Retrofit.Builder()
            .baseUrl(prefProvider.getValue(Constants.BASE_URL_NEW, BASE_URL))
            .client(
                OkHttpClient.Builder().connectTimeout(50000, TimeUnit.MILLISECONDS)
                    .readTimeout(100000, TimeUnit.MILLISECONDS)
                    .addInterceptor(hostSelectionInterceptor)
                    .addInterceptor { chain ->
                        chain.proceed(chain.request().newBuilder().also {
                            val authToken = prefProvider.getValue(Constants.AUTH_TOKEN, "")
                            println("authToken ::  $authToken")
                            println("BASE_URL :: ${prefProvider.getValue(Constants.BASE_URL_NEW, BASE_URL)}")
                            if (MainApplication.getInstance() != null) {
                                MainApplication.getInstance()?.applicationContext?.packageManager?.getPackageInfo(
                                    MainApplication.getInstance()?.applicationInfo?.packageName
                                        ?: "",
                                    0
                                )?.versionName?.let { it1 ->
                                    it.addHeader(
                                        "CURRENTVERSION",
                                        it1
                                    )
                                }
                            }
                            if (authToken.isNotEmpty())
                                it.addHeader("TOKEN", authToken)

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
            .addConverterFactory(SimpleXmlConverterFactory.create())
            .build()
            .create(ApiService::class.java)
}