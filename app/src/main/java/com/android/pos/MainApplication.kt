package com.android.pos

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MainApplication : Application() {


    override fun onCreate() {
        super.onCreate()
        instance = this
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

    }

    companion object {
        private var instance: MainApplication? = null
        fun getInstance(): MainApplication? {
            if (instance == null) {
                synchronized(MainApplication::class.java) {
                    if (instance == null) {
                        instance = MainApplication()
                    }
                }
            }
            return instance
        }
    }
}