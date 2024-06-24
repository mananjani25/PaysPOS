package com.pays.pos.utils

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display

fun getCustomerDisplay(context: Context): Display? {
    val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    val displays = displayManager.displays

    if (displays.size <= 1) {
        return null
    }
    // We take the first additional screen
    return displays[1]
}
