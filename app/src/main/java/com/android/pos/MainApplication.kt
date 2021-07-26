package com.android.pos

import android.app.Application
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.AppCompatImageView
import androidx.databinding.BindingAdapter
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }


    @BindingAdapter("app:srcCompat")
    fun bindSrcCompat(imageView: AppCompatImageView?, drawable: Drawable?) {
        // Your setter code goes here, like setDrawable or similar
    }
}