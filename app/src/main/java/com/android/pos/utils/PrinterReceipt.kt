package com.android.pos.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.annotation.Nullable
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.android.pos.data.model.responseModel.CreateOrderResponse
import com.android.pos.data.model.responseModel.GetTipReponse
import com.android.pos.data.model.responseModel.OpenOrderResponse
import com.android.pos.data.remote.Constants
import com.epson.eposprint.Builder

private val TAG = "PrinterReceipt"

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


        partOne + " " + partTwo
    } else {
        val padding = columnsPerLine - (partOne.length + partTwo.length)
        partOne + repeat(" ", padding) + partTwo
    }
    return concat
}


fun padLineCustomerItem(
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
        val strBuffer = StringBuffer()

        strBuffer.append(
            partOne.substring(0, columnsPerLine - 8) + repeat(
                " ",
                8 - partTwo.length
            ) + partTwo
        )
        strBuffer.append("\n")
        var tempStr = ""
        var tempPartOne = partOne.substring(columnsPerLine - 8, partOne.length)
        val tempPadding = (columnsPerLine - tempPartOne.length) - partTwo.length
        tempStr = tempPartOne + repeat(" ", tempPadding)

        strBuffer.append(tempStr)

        return strBuffer.toString()

        //partOne + " " + partTwo
    } else {
        val padding = columnsPerLine - (partOne.length + partTwo.length)
        partOne + repeat(" ", padding) + partTwo
    }
    return concat
}


fun addCustomerTextSize(builder: Builder, font: String): Builder {
    when (font) {
        Constants.SMALL -> {
            builder.addTextSize(1, 1)
        }
        Constants.LARGE -> {
            builder.addTextSize(2, 2)
        }
        Constants.MEDIUM -> {
            builder.addTextSize(1, 2)

        }
        else -> {
            builder.addTextSize(1, 1)

        }

    }
    return builder

}

fun padLineForItem(
    @Nullable partOne: String?,
    @Nullable partTwo: String?,
    columnsPerLine: Int,
    builder: Builder
): Builder {
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
        val padding = 8
        val strBuffer = StringBuffer()
        if (partOne.length > columnsPerLine - 6) {
            strBuffer.append(partOne.substring(0, columnsPerLine - 6) + "\n")
            if (columnsPerLine - 6 > (partOne.length - partOne.substring(
                    0,
                    columnsPerLine - 6
                ).length)
            ) {
                strBuffer.append(
                    "   " + partOne.substring(
                        (partOne.length - partOne.substring(
                            0,
                            columnsPerLine - 6
                        ).length), partOne.length
                    )
                )
            } else {

                strBuffer.append(
                    "   " + partOne.substring(
                        partOne.length - partOne.substring(
                            0,
                            columnsPerLine - 6
                        ).length, partOne.length
                    ) + "\n"
                )

            }
            //  strBuffer.append(partOne.substring())
        }
        Log.e(TAG, "GeneratePartOne: ${strBuffer.toString()}")
        partOne = ""
        partOne = strBuffer.toString()
        partOne + partTwo
        Log.e(TAG, "FinalString : $partOne + partTwo")
        builder.addText(partOne + partTwo)
        return builder
    } else {
        val padding = columnsPerLine - (partOne.length + partTwo.length)
        partOne + partTwo
        Log.e(TAG, "FinalElseString: $partOne + partTwo")
        builder.addText(partOne + partTwo)
        return builder

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

fun addHorizontalLargeLine(builder: Builder): Builder {
    var str: String = ""
    for (i in 0 until 24) {
        str += "-"
    }
    Log.e("strLine", "strLine  $str")
    builder.addText(str)

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

fun addTipsList(
    builder: Builder,
    list: List<GetTipReponse.Data>,
    totalAmt: Double,
    font: String
): Builder {
    for (i in 0 until list.size) {
        val obj = list.get(i)
        builder.addTextLineSpace(30)
        builder.addFeedUnit(30)
        builder.addTextFont(Builder.FONT_E)
        // builder.addTextAlign(Builder.ALIGN_LEFT)
        builder.addTextLang(Builder.LANG_EN)
        addCustomerTextSize(builder, font)
        builder.addTextStyle(
            Builder.FALSE,
            Builder.FALSE,
            Builder.FALSE,
            Builder.COLOR_1
        )

        val tipName = obj.name + "(" + MethodUtils.roundOffAmountString(obj.rate) + "%)"
        val price = "(Tip $" + calculateTipAmt(
            obj.rate,
            totalAmt
        ) + " Total $" + MethodUtils.roundOffAmountString(
            (totalAmt + calculateTipAmt(
                obj.rate,
                totalAmt
            ))
        ) + ")"
        builder.addText(
            padLine(
                tipName,
                price,
                if (font == Constants.LARGE) {
                    24
                } else {
                    48
                }
            )
        )

    }


    return builder
}


fun addOrdersForKitchen(
    builder: Builder,
    list: List<CreateOrderResponse.Data.Order.OrderItem>
): Builder {
    for (i in 0 until list.size) {
        val obj = list.get(i)
        builder.addTextLineSpace(30)
        builder.addFeedUnit(30)
        builder.addTextFont(Builder.FONT_C)
        builder.addTextLang(Builder.LANG_EN)
        builder.addTextAlign(Builder.ALIGN_LEFT)
        builder.addTextSize(1, 2)
        builder.addTextStyle(
            Builder.FALSE,
            Builder.FALSE,
            Builder.TRUE,
            Builder.COLOR_1
        )

        builder.addText(obj.quantity.toString() + " " + obj.itemName)

        if (obj.orderItemModifiers.isNotEmpty()) {
            for (j in 0 until obj.orderItemModifiers.size) {
                val modifierObj = obj.orderItemModifiers.get(j)
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_C)
                //builder.addTextLineSpace(20)
                builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(1, 2)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )
                //builder.addTextPosition(1)


                builder.addText("  " + modifierObj.name)


            }
        }
        if (obj.note.isNotEmpty()) {
            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)
            builder.addTextFont(Builder.FONT_C)
            //builder.addTextLineSpace(20)
            builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 2)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
            )
            builder.addText("  Note:" + obj.note)

        }


    }


    return builder
}

fun addOrderItemOpenOrder(
    builder: Builder,
    list: List<OpenOrderResponse.Data.Order.OrderItem>,
    font: String,
    showModifiers: Boolean
): Builder {
    for (i in 0 until list.size) {
        val obj = list.get(i)
        builder.addTextLineSpace(30)
        builder.addFeedUnit(30)
        builder.addTextFont(Builder.FONT_E)
        // builder.addTextAlign(Builder.ALIGN_LEFT)
        builder.addTextLang(Builder.LANG_EN)
        addCustomerTextSize(builder, font)
        builder.addTextStyle(
            Builder.FALSE,
            Builder.FALSE,
            Builder.FALSE,
            Builder.COLOR_1
        )



        builder.addText(
            padLineCustomerItem(
                obj.quantity.toString() + "x " + obj.itemName,
                "$" + MethodUtils.roundOffAmountString(totalPriceOpenOrder(obj)),
                if (font == Constants.LARGE) {
                    24
                } else {
                    48
                }
            )
        )


        if (obj.orderItemModifiers.isNotEmpty() && showModifiers) {
            for (j in 0 until obj.orderItemModifiers.size) {
                val modifierObj = obj.orderItemModifiers.get(j)
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                //builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, font)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                builder.addTextPosition(4)
                builder.addText(
                    padLineCustomerItem(
                        "   " + modifierObj.name,
                        "$" + MethodUtils.roundOffAmountString(modifierObj.price.toDouble() * modifierObj.quantity),
                        if (font == Constants.LARGE) {
                            23
                        } else {
                            47
                        }
                    )
                )


            }

        }
    }


    return builder


}


fun addOrderItems(
    builder: Builder,
    list: List<CreateOrderResponse.Data.Order.OrderItem>,
    font: String,
    showModifiers: Boolean
): Builder {
    for (i in 0 until list.size) {
        val obj = list.get(i)
        builder.addTextLineSpace(30)
        builder.addFeedUnit(30)
        builder.addTextFont(Builder.FONT_E)
        // builder.addTextAlign(Builder.ALIGN_LEFT)
        builder.addTextLang(Builder.LANG_EN)
        addCustomerTextSize(builder, font)
        builder.addTextStyle(
            Builder.FALSE,
            Builder.FALSE,
            Builder.FALSE,
            Builder.COLOR_1
        )



        builder.addText(
            padLineCustomerItem(
                obj.quantity.toString() + "x " + obj.itemName,
                "$" + MethodUtils.roundOffAmountString(totalPrice(obj)),
                if (font == Constants.LARGE) {
                    24
                } else {
                    48
                }
            )
        )


        if (obj.orderItemModifiers.isNotEmpty() && showModifiers) {
            for (j in 0 until obj.orderItemModifiers.size) {
                val modifierObj = obj.orderItemModifiers.get(j)
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                //builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, font)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                builder.addTextPosition(4)
                builder.addText(
                    padLineCustomerItem(
                        "   " + modifierObj.name,
                        "$" + MethodUtils.roundOffAmountString(modifierObj.price.toDouble() * modifierObj.quantity),
                        if (font == Constants.LARGE) {
                            23
                        } else {
                            47
                        }
                    )
                )


            }

        }
    }


    return builder
}

private fun totalPriceOpenOrder(model: OpenOrderResponse.Data.Order.OrderItem): Double {
    return if (model.orderItemModifiers.isNotEmpty()) {

        var totalPrice = 0.0

        val mList = model.orderItemModifiers
        mList.forEach { items ->
            totalPrice += items.price * items.quantity
        }

        (model.price * model.quantity) + totalPrice
    } else {

        model.price * model.quantity

    }

}

private fun totalPrice(model: CreateOrderResponse.Data.Order.OrderItem): Double {

    return if (model.orderItemModifiers.isNotEmpty()) {

        var totalPrice = 0.0

        val mList = model.orderItemModifiers
        mList.forEach { items ->
            totalPrice += items.price * items.quantity
        }

        (model.price * model.quantity) + totalPrice
    } else {

        model.price * model.quantity

    }
}


fun calculateTipAmt(percentage: Double, price: Double): Double {

    return MethodUtils.roundOffAmountDouble((price * percentage) / 100)
}
