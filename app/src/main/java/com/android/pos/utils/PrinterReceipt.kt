package com.android.pos.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.Nullable
import com.google.android.gms.common.util.Strings
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.android.pos.data.model.responseModel.CreateOrderResponse
import com.android.pos.data.model.responseModel.GetTipReponse
import com.epson.eposprint.Builder


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

fun addHorizontalLine(builder: Builder): Builder {


    var str: String = ""
    for (i in 0 until 48) {
        str += "-"
    }
    Log.e("strLine", "strLine  $str")
    builder.addText(str)

    return builder
}

fun addHorizontalKitchenLine(builder: Builder): Builder {


    var str: String = ""
    for (i in 0 until 40) {
        str += "-"
    }
    Log.e("strLine", "strLine  $str")
    builder.addText(str)

    return builder
}

fun addTipsList(builder: Builder, list: List<GetTipReponse.Data>, totalAmt: Double): Builder {
    for (i in 0 until list.size) {
        val obj = list.get(i)
        builder.addTextLineSpace(30)
        builder.addFeedUnit(30)
        builder.addTextFont(Builder.FONT_E)
        // builder.addTextAlign(Builder.ALIGN_LEFT)
        builder.addTextLang(Builder.LANG_EN)
        builder.addTextSize(1, 1)
        builder.addTextStyle(
            Builder.FALSE,
            Builder.FALSE,
            Builder.FALSE,
            Builder.COLOR_1
        )

        val tipName = obj.name + "(" + MethodUtils.roundOffAmountString(obj.rate) +"%)"
        val price = "(Tip $"+calculateTipAmt(obj.rate, totalAmt)+" Total $"+MethodUtils.roundOffAmountString((totalAmt - calculateTipAmt(obj.rate, totalAmt)))+")"
        builder.addText(
            padLine(
                tipName,
                price,
                48
            )
        )

    }


    return builder
}

fun addOrderItems(builder: Builder, list: List<CreateOrderResponse.Data.Order.OrderItem>): Builder {
    for (i in 0 until list.size) {
        val obj = list.get(i)
        builder.addTextLineSpace(30)
        builder.addFeedUnit(30)
        builder.addTextFont(Builder.FONT_E)
        // builder.addTextAlign(Builder.ALIGN_LEFT)
        builder.addTextLang(Builder.LANG_EN)
        builder.addTextSize(1, 1)
        builder.addTextStyle(
            Builder.FALSE,
            Builder.FALSE,
            Builder.FALSE,
            Builder.COLOR_1
        )

        builder.addText(
            padLine(
                obj.quantity.toString() + "x " + obj.itemName,
                "$" + MethodUtils.roundOffAmountString(obj.price),
                48
            )
        )

        if (obj.orderItemModifiers.isNotEmpty()) {
            for (j in 0 until obj.orderItemModifiers.size) {
                val modifierObj = obj.orderItemModifiers.get(j)
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                //builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(1, 1)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                builder.addTextPosition(4)
                builder.addText(
                    padLine(
                        "   " + modifierObj.name,
                        "$" + MethodUtils.roundOffAmountString(modifierObj.price.toDouble()),
                        47
                    )
                )


            }

        }
    }


    return builder
}

fun calculateTipAmt(percentage: Double, price: Double): Double {

    return MethodUtils.roundOffAmountDouble((price * percentage) / 100)
}
