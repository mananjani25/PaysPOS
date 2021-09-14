package com.android.pos.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.Nullable
import com.google.android.gms.common.util.Strings
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat


fun padLine(
    @Nullable partOne: String?,
    @Nullable partTwo: String?,
    columnsPerLine: Int
): String? {
    var partOne = partOne
    var partTwo = partTwo
    if (partOne == null) {
        partOne = ""
    }
    if (partTwo == null) {
        partTwo = ""
    }
    val concat: String
    concat = if (partOne.length + partTwo.length > columnsPerLine) {
        "$partOne $partTwo"
    } else {
        val padding = columnsPerLine - (partOne.length + partTwo.length)
        partOne + repeat(" ", padding) + partTwo
    }
    return concat
}


/** utility: string repeat  */
fun repeat(str: String?, i: Int): String? {
    return String(CharArray(i)).replace("\u0000", str!!)
}

fun getBitmapFromVectorDrawable(context: Context?, drawableId: Int): Bitmap {
    var drawable: Drawable? = context?.let { ContextCompat.getDrawable(it, drawableId) }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
        drawable = drawable?.let { DrawableCompat.wrap(it).mutate() }
    }
    val bitmap = drawable?.let {
        Bitmap.createBitmap(
            it.intrinsicWidth,
            drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
        )
    }
    val canvas = bitmap?.let { Canvas(it) }
    drawable?.setBounds(0, 0, canvas!!.getWidth(), canvas.getHeight())
    if (canvas != null) {
        drawable?.draw(canvas)
    }
    return bitmap!!
}

    fun addBuilderText(
    builder: com.epson.eposprint.Builder,
    text: String
): com.epson.eposprint.Builder {
    builder.addText(text)
    return builder
}
