package com.android.pos.utils.extensions


import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.android.pos.utils.Event
import com.google.android.material.snackbar.Snackbar
import java.io.IOException

/**
 * Created by Waheed on 04,November,2019
 */

fun View.visible() {
    visibility = View.VISIBLE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

fun View.gone() {
    visibility = View.GONE
}

fun ProgressBar.show() {
    this.visibility = View.VISIBLE
}

fun ProgressBar.hide() {
    this.visibility = View.GONE
}

fun Context.getColorCompat(@ColorRes colorRes: Int) = ContextCompat.getColor(this, colorRes)
fun Fragment.getColor(@ColorRes colorRes: Int) = ContextCompat.getColor(requireContext(), colorRes)

/**
 * Easy toast function for Activity.
 */
fun FragmentActivity.toast(text: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, text, duration).show()
}

/**
 * Inflate the layout specified by [layoutRes].
 */
fun ViewGroup.inflate(layoutRes: Int): View {
    return LayoutInflater.from(context).inflate(layoutRes, this, false)
}

fun Context.getDrawableCompat(@DrawableRes resId: Int, @ColorRes tintColorRes: Int = 0) = when {
    tintColorRes != 0 -> AppCompatResources.getDrawable(this, resId)?.apply {
        setColorFilter(getColorCompat(tintColorRes), PorterDuff.Mode.SRC_ATOP)
    }
    else -> AppCompatResources.getDrawable(this, resId)
}!!

fun View.liveSnackBar(
    lifecycleOwner: LifecycleOwner,
    snackbarEvent: LiveData<Event<Any?>>,
    timeLength: Int
) {

    snackbarEvent.observe(lifecycleOwner, Observer { event ->
        event.getContentIfNotHandled()?.let {
            when (it) {
                is Int -> showSnackbar(context.getString(it), timeLength)
                is String -> showSnackbar(it, timeLength)
            }

        }
    })
}

fun View.showSnackbar(snackbarText: String, timeLength: Int) {
    Snackbar.make(this, snackbarText, timeLength).show()
}

fun Context.getRandomMaterialColor(typeColor: String): Int {
    var returnColor: Int = Color.GRAY
    val arrayId: Int = getResources().getIdentifier(
        "mdcolor_$typeColor",
        "array", packageName
    )
    if (arrayId != 0) {
        val colors: TypedArray = getResources().obtainTypedArray(arrayId)
        val index = (Math.random() * colors.length()).toInt()
        returnColor = colors.getColor(index, Color.GRAY)
        colors.recycle()
    }
    return returnColor
}

class NoInternetException(message: String) : IOException(message)
