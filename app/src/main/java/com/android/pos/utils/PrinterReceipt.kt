package com.android.pos.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import androidx.annotation.Nullable
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.android.pos.data.entities.TbItem
import com.android.pos.data.entities.TbServiceCharge
import com.android.pos.data.model.responseModel.CreateOrderResponse
import com.android.pos.data.model.responseModel.GetOrderDetailsResponse
import com.android.pos.data.model.responseModel.GetTipReponse
import com.android.pos.data.model.responseModel.OpenOrderResponse
import com.android.pos.data.remote.Constants
import com.epson.eposprint.Builder

val TAG = "PrinterReceipt"

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

fun addOrdersForKitchenDineIn(
    builder: Builder,
    list: ArrayList<TbItem>
): Builder {


    list.forEach { obj ->


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

        builder.addText(obj.itemQuantity.toString() + " " + obj.name)

        if (obj.modifiers.isNotEmpty()) {
            for (j in 0 until obj.modifiers.size) {
                val modifierObj = obj.modifiers.get(j)
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
                    Builder.COLOR_2
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
                    Builder.COLOR_2
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

fun addWholeTbItemToGuest(
    builder: Builder,
    list: TbItem,
    font: String,
    showModifiers: Boolean,
    guestCount: Int,
    serviceChargeList: ArrayList<TbServiceCharge>
): Builder {

    val obj = list
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


    val subTotal = (obj.price * obj.itemQuantity).toDouble()
    var WTTaxes = 0.0
    var serviceCharge = 0.0


    obj.taxes?.forEach { tax ->
        if (tax.isActive) {
            WTTaxes += if (tax.taxType == "Percentage") {

                var modifierPrice = 0.0
                val price =
                    (obj.price * obj.itemQuantity) - obj.discountPrice

                obj.modifiers.forEach {
                    modifierPrice += (it.price * it.itemQuantity)
                }

                val totalPrice = price + modifierPrice

                val itemTaxPrice =
                    (tax.rate * totalPrice) / 100
                Log.e("itemTaxPrice", "" + itemTaxPrice)
                String.format("%.2f", itemTaxPrice)
                    .toDouble()
            } else {

                String.format(
                    "%.2f",
                    tax.rate * obj.itemQuantity
                )
                    .toDouble()
            }
        }


    }

    if (serviceChargeList?.isNotEmpty() == true) {
        serviceChargeList?.forEach {
            if (it.isEnabled) {
                serviceCharge += (subTotal * it.percentage) / 100
            }
        }

        Log.e(TAG, "serviceCharge  ${serviceCharge}")
        Log.e(TAG, "serviceWTTaxes  ${WTTaxes}")
        Log.e(TAG, "serviceSubTotal  ${subTotal}")
    }

    var finalAmt = MethodUtils.roundOffAmount((subTotal) / guestCount)
    builder.addText(
        padLineCustomerItem(
            obj.itemQuantity.toString() + "x " + obj.name,
            "" + finalAmt,
            if (font == Constants.LARGE) {
                24
            } else {
                48
            }
        )
    )


    /*if (obj.modifiers.isNotEmpty() && showModifiers) {
        for (j in 0 until obj.modifiers.size) {
            val modifierObj = obj.modifiers.get(j)
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
                    "$" + MethodUtils.roundOffAmountString(modifierObj.price.toDouble() * modifierObj.itemQuantity),
                    if (font == Constants.LARGE) {
                        23
                    } else {
                        47
                    }
                )
            )


        }


    }*/

    return builder
}

fun addOrderItemForDineIn(
    builder: Builder,
    list: TbItem,
    font: String,
    showModifiers: Boolean
): Builder {


    val obj = list
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
            obj.itemQuantity.toString() + "x " + obj.name,
            "$" + MethodUtils.roundOffAmountString(totalPriceDineInItem(obj)),
            if (font == Constants.LARGE) {
                24
            } else {
                48
            }
        )
    )


    if (obj.modifiers.isNotEmpty() && showModifiers) {
        for (j in 0 until obj.modifiers.size) {
            val modifierObj = obj.modifiers.get(j)
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
                    "$" + MethodUtils.roundOffAmountString(modifierObj.price.toDouble() * modifierObj.itemQuantity),
                    if (font == Constants.LARGE) {
                        23
                    } else {
                        47
                    }
                )
            )


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

fun addOrderItemsTransaction(
    builder: Builder,
    list: List<GetOrderDetailsResponse.Data.OrderItem>,
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
                "$" + MethodUtils.roundOffAmountString(totalPriceTransaction(obj)),
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

private fun totalPriceDineInItem(model: TbItem): Double {
    return if (model.modifiers.isNotEmpty()) {

        var totalPrice = 0.0

        val mList = model.modifiers
        mList.forEach { items ->
            totalPrice += items.price * items.itemQuantity
        }

        (model.price * model.itemQuantity) + totalPrice
    } else {

        model.price * model.itemQuantity

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

private fun totalPriceTransaction(model: GetOrderDetailsResponse.Data.OrderItem): Double {

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
