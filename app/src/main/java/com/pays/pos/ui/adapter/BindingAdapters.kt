package com.pays.pos.ui.adapter

import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.databinding.BindingAdapter

object BindingAdapters {
    @BindingAdapter("app:srcCompat")
    fun bindSrcCompat(view: AppCompatImageView, @DrawableRes drawable: Int) {
        view.setImageResource(drawable)
    }
}