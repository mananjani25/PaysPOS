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
import com.android.pos.data.model.responseModel.*
import com.android.pos.data.model.responseModel.report.KeyValue
import com.android.pos.data.remote.Constants
import com.android.pos.utils.MethodUtils.Companion.roundOffAmountString
import com.android.pos.di.PrefProvider
import com.epson.epos2.printer.Printer
import com.epson.eposprint.Builder
import com.sunmi.externalprinterlibrary.api.SunmiPrinterApi

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


fun addPaymentDetailsHeader(builder: Builder): Builder {
    builder.addTextLineSpace(30)
    builder.addFeedUnit(30)
    builder.addTextFont(Builder.FONT_E)
    // builder.addTextAlign(Builder.ALIGN_LEFT)
    builder.addTextLang(Builder.LANG_EN)
    addCustomerTextSize(builder, Constants.SMALL)
    builder.addTextStyle(
        Builder.FALSE,
        Builder.FALSE,
        Builder.TRUE,
        Builder.COLOR_1
    )
    builder.addText("Details" + repeat(" ", 20) + "Refund" + repeat(" ", 9) + "Amount")


    return builder
}

fun addPaymentDetailsHeader() {

    PrintSunmiUtils.orderTime("Details" + repeat(" ", 20) + "Refund" + repeat(" ", 9) + "Amount")


}

fun addPaymentDetailsThreeData(builder: Builder, keyValue: java.util.ArrayList<KeyValue>): Builder {
    builder.addTextLineSpace(30)
    builder.addFeedUnit(30)
    builder.addTextFont(Builder.FONT_E)
    // builder.addTextAlign(Builder.ALIGN_LEFT)
    builder.addTextLang(Builder.LANG_EN)
    addCustomerTextSize(builder, Constants.SMALL)
    builder.addTextStyle(
        Builder.FALSE,
        Builder.FALSE,
        Builder.FALSE,
        Builder.COLOR_1
    )
    var title = ""
    var refund = ""
    var amount = ""


    keyValue.forEach {


        if (it.key.toString().toLowerCase().contains("Refund".toLowerCase())) {
            refund = "$" + it.value
        } else {
            title = it.key.toString()
            amount = MethodUtils.roundOffAmount(it.value.toString().toDouble())
        }
    }

    var fPart = title + repeat(" ", 27 - title.length) + refund
    var spaceLastPart = 48 - fPart.length
    var spaceLast = 0
    if (spaceLastPart > 1 && amount.length < spaceLastPart) {
        spaceLast = spaceLastPart - amount.length
    }

    fPart += repeat(" ", spaceLast) + amount

    builder.addText(fPart)

    return builder
}

fun addPaymentDetailsThreeData(keyValue: java.util.ArrayList<KeyValue>) {

    var title = ""
    var refund = ""
    var amount = ""


    keyValue.forEach {


        if (it.key.toString().toLowerCase().contains("Refund".toLowerCase())) {
            refund = "$" + it.value
        } else {
            title = it.key.toString()
            amount = MethodUtils.roundOffAmount(it.value.toString().toDouble())
        }
    }

    var fPart = title + repeat(" ", 27 - title.length) + refund
    var spaceLastPart = 48 - fPart.length
    var spaceLast = 0
    if (spaceLastPart > 1 && amount.length < spaceLastPart) {
        spaceLast = spaceLastPart - amount.length
    }

    fPart += repeat(" ", spaceLast) + amount

    PrintSunmiUtils.orderTime(fPart)

}

fun employeeGuestDetailsData(builder: Builder, keyValue: KeyValue): Builder {
    builder.addTextLineSpace(30)
    builder.addFeedUnit(30)
    builder.addTextFont(Builder.FONT_E)
    // builder.addTextAlign(Builder.ALIGN_LEFT)
    builder.addTextLang(Builder.LANG_EN)
    addCustomerTextSize(builder, Constants.SMALL)
    builder.addTextStyle(
        Builder.FALSE,
        Builder.FALSE,
        Builder.FALSE,
        Builder.COLOR_1
    )
    var sPart = if (keyValue.key?.contains("Served", true) == true) {
        keyValue.value.toString()
    } else {
        MethodUtils.roundOffAmount(keyValue.value?.toDouble() ?: 0.0)
    }
    builder.addText(
        padLine(
            keyValue.key,
            sPart,
            48
        )
    )

    return builder

}

fun employeeGuestDetailsData(keyValue: KeyValue) {

    var sPart = if (keyValue.key?.contains("Served", true) == true) {
        keyValue.value.toString()
    } else {
        MethodUtils.roundOffAmount(keyValue.value?.toDouble() ?: 0.0)
    }
    PrintSunmiUtils.orderTime(
        padLine(
            keyValue.key,
            sPart,
            48
        ).toString()
    )


}

fun addPaymentDetailsTwoData(builder: Builder, keyValue: KeyValue): Builder {
    builder.addTextLineSpace(30)
    builder.addFeedUnit(30)
    builder.addTextFont(Builder.FONT_E)
    // builder.addTextAlign(Builder.ALIGN_LEFT)
    builder.addTextLang(Builder.LANG_EN)
    addCustomerTextSize(builder, Constants.SMALL)
    builder.addTextStyle(
        Builder.FALSE,
        Builder.FALSE,
        Builder.FALSE,
        Builder.COLOR_1
    )
    builder.addText(
        padLine(
            keyValue.key,
            MethodUtils.roundOffAmount(keyValue.value.toString().toDouble() ?: 0.0),
            48
        )
    )

    return builder
}

fun addPaymentDetailsTwoData(keyValue: KeyValue) {

    PrintSunmiUtils.orderTime(
        padLine(
            keyValue.key,
            MethodUtils.roundOffAmount(keyValue.value.toString().toDouble() ?: 0.0),
            48
        ).toString()
    )


}

fun addRefundVoidsMultiple(builder: Builder, keyValue: java.util.ArrayList<KeyValue>): Builder {
    builder.addTextLineSpace(30)
    builder.addFeedUnit(30)
    builder.addTextFont(Builder.FONT_E)
    // builder.addTextAlign(Builder.ALIGN_LEFT)
    builder.addTextLang(Builder.LANG_EN)
    addCustomerTextSize(builder, Constants.SMALL)
    builder.addTextStyle(
        Builder.FALSE,
        Builder.FALSE,
        Builder.FALSE,
        Builder.COLOR_1
    )
    keyValue.forEach {
        if (!it.key?.trim().equals("Item Count".trim(), true)) {
            builder.addText(
                padLine(
                    it.key,
                    MethodUtils.roundOffAmount(it.value.toString().toDouble() ?: 0.0),
                    48
                )
            )
        }
    }


    return builder

}

fun addRefundVoidsMultiple(keyValue: java.util.ArrayList<KeyValue>) {

    keyValue.forEach {
        if (!it.key?.trim().equals("Item Count".trim(), true)) {
            PrintSunmiUtils.orderTime(
                padLine(
                    it.key,
                    MethodUtils.roundOffAmount(it.value.toString().toDouble() ?: 0.0),
                    48
                ).toString()
            )
        }
    }


}

fun addSixHeaderForOrderSaleDetails(builder: Builder): Builder {


    builder.addTextLineSpace(30)
    builder.addFeedUnit(30)
    builder.addTextFont(Builder.FONT_E)
    // builder.addTextAlign(Builder.ALIGN_LEFT)
    builder.addTextLang(Builder.LANG_EN)
    addCustomerTextSize(builder, Constants.SMALL)
    builder.addTextStyle(
        Builder.FALSE,
        Builder.FALSE,
        Builder.TRUE,
        Builder.COLOR_1
    )

    builder.addText("OrderId    Tip      SC     PayType     Amount   ")

    return builder
}

fun addSixHeaderForOrderSaleDetailsSunmi() {
    PrintSunmiUtils.orderTime("OrderId    Tip      SC     PayType     Amount   ")
}

fun addCreditTipAuditHeader(builder: Builder): Builder {
    builder.addTextLineSpace(30)
    builder.addFeedUnit(30)
    builder.addTextFont(Builder.FONT_E)
    builder.addTextAlign(Builder.ALIGN_LEFT)
    builder.addTextLang(Builder.LANG_EN)
    addCustomerTextSize(builder, Constants.SMALL)
    builder.addTextStyle(
        Builder.FALSE,
        Builder.FALSE,
        Builder.FALSE,
        Builder.COLOR_1
    )

    builder.addText(
        "PaymentId" + repeat(" ", 4) + "SubTotal" + repeat(" ", 6) + "Tip" + repeat(
            " ",
            8
        ) + "Total"
    )

    return builder

}

fun addCreditTipAuditHeader() {

    PrintSunmiUtils.orderTime(
        "PaymentId" + repeat(" ", 4) + "SubTotal" + repeat(" ", 6) + "Tip" + repeat(
            " ",
            8
        ) + "Total"
    )


}

fun addCreditTipAuditData(
    builder: Builder,
    fPArt: String,
    sPart: String,
    TPArt: String,
    lPart: String
): Builder {
    builder.addTextLineSpace(30)
    builder.addFeedUnit(30)
    builder.addTextFont(Builder.FONT_E)
    builder.addTextAlign(Builder.ALIGN_LEFT)
    builder.addTextLang(Builder.LANG_EN)
    addCustomerTextSize(builder, Constants.SMALL)
    builder.addTextStyle(
        Builder.FALSE,
        Builder.FALSE,
        Builder.FALSE,
        Builder.COLOR_1
    )


    var pOne = TPArt + repeat(" ", 13 - TPArt.length) + fPArt

    pOne += repeat(" ", 27 - pOne.length) + sPart
    pOne += repeat(" ", 38 - pOne.length) + lPart




    builder.addText(pOne)
    return builder
}

fun addCreditTipAuditData(
    fPArt: String,
    sPart: String,
    TPArt: String,
    lPart: String
) {


    var pOne = TPArt + repeat(" ", 13 - TPArt.length) + fPArt

    pOne += repeat(" ", 27 - pOne.length) + sPart
    pOne += repeat(" ", 38 - pOne.length) + lPart


    PrintSunmiUtils.orderTime(pOne)

}

fun addCreditCardBreakDown(builder: Builder): Builder {
    builder.addTextLineSpace(30)
    builder.addFeedUnit(30)
    builder.addTextFont(Builder.FONT_E)
    // builder.addTextAlign(Builder.ALIGN_LEFT)
    builder.addTextLang(Builder.LANG_EN)
    addCustomerTextSize(builder, Constants.SMALL)
    builder.addTextStyle(
        Builder.FALSE,
        Builder.FALSE,
        Builder.FALSE,
        Builder.COLOR_1
    )

    builder.addText("CardName" + repeat(" ", 20) + "Tip" + repeat(" ", 11) + "Amount")

    return builder
}

fun addCreditCardBreakDown() {


    PrintSunmiUtils.orderTime("CardName" + repeat(" ", 20) + "Tip" + repeat(" ", 11) + "Amount")

}

fun addCreditCardBreakDownData(
    builder: Builder,
    creditCardBreakdown: EodReportResponse.Data.CreditCardBreakdown
): Builder {
    builder.addTextLineSpace(30)
    builder.addFeedUnit(30)
    builder.addTextFont(Builder.FONT_E)
    // builder.addTextAlign(Builder.ALIGN_LEFT)
    builder.addTextLang(Builder.LANG_EN)
    addCustomerTextSize(builder, Constants.SMALL)
    builder.addTextStyle(
        Builder.FALSE,
        Builder.FALSE,
        Builder.FALSE,
        Builder.COLOR_1
    )
    var pOne = creditCardBreakdown.key + repeat(
        " ",
        28 - creditCardBreakdown.key.length
    ) + MethodUtils.roundOffAmount(creditCardBreakdown.tips)
    var lastPart = 48 - pOne.length
    var amount = MethodUtils.roundOffAmount(creditCardBreakdown.value)
    var spaceLast = 0
    if (lastPart > 1 && amount.length < lastPart) {
        spaceLast = lastPart - amount.length
    }
    pOne += repeat(" ", spaceLast) + amount

    builder.addText(pOne)
    return builder
}


fun addCreditCardBreakDownData(
    creditCardBreakdown: EodReportResponse.Data.CreditCardBreakdown
) {

    var pOne = creditCardBreakdown.key + repeat(
        " ",
        28 - creditCardBreakdown.key.length
    ) + MethodUtils.roundOffAmount(creditCardBreakdown.tips)
    var lastPart = 48 - pOne.length
    var amount = MethodUtils.roundOffAmount(creditCardBreakdown.value)
    var spaceLast = 0
    if (lastPart > 1 && amount.length < lastPart) {
        spaceLast = lastPart - amount.length
    }
    pOne += repeat(" ", spaceLast) + amount

    PrintSunmiUtils.orderTime(pOne)

}

fun addItemsInOrderSalesDetails(
    builder: Builder,
    details: EodReportResponse.Data.OrderSalesDetails.Details
): Builder {


    builder.addTextLineSpace(30)
    builder.addFeedUnit(30)
    builder.addTextFont(Builder.FONT_E)
    builder.addTextAlign(Builder.ALIGN_LEFT)
    builder.addTextLang(Builder.LANG_EN)
    addCustomerTextSize(builder, Constants.SMALL)
    builder.addTextStyle(
        Builder.FALSE,
        Builder.FALSE,
        Builder.FALSE,
        Builder.COLOR_1
    )

    var data = details.orderId
    data += repeat(" ", 10 - details.orderId.length) + MethodUtils.roundOffAmount(details.tip)
    data += repeat(" ", 18 - data.length) + MethodUtils.roundOffAmount(details.serviceCharge)
    data += repeat(" ", 27 - data.length) + details.payType
    data += repeat(" ", 39 - data.length) + MethodUtils.roundOffAmount(details.amount)

    builder.addText(data)
    return builder
}

fun addItemsInOrderSalesDetails(
    details: EodReportResponse.Data.OrderSalesDetails.Details
) {

    var data = details.orderId
    data += repeat(" ", 10 - details.orderId.length) + MethodUtils.roundOffAmount(details.tip)
    data += repeat(" ", 18 - data.length) + MethodUtils.roundOffAmount(details.serviceCharge)
    data += repeat(" ", 27 - data.length) + details.payType
    data += repeat(" ", 39 - data.length) + MethodUtils.roundOffAmount(details.amount)

    PrintSunmiUtils.orderTime(data)
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
        var tempPadding = 0
        if (((columnsPerLine - tempPartOne.length) - partTwo.length) < 0) {
            tempPadding = partTwo.length
        } else {
            tempPadding = (columnsPerLine - tempPartOne.length) - partTwo.length
        }
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

fun addHorizontalLineNew(printer:Printer):Printer{
    var str: String = ""
    for (i in 0 until 48) {
        str += "-"
    }
    Log.e("strLine", "strLine  $str")
    printer.addText(str)

    return printer
}

fun orderSalesDetails(builder: Builder): Builder {
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

fun addHorizontalKitchenLineSunmi(fontSize: String): String {

    var int = 48
    when (fontSize) {
        Constants.LARGE -> {
            int = 23
        }
    }

    var str: String = ""
    for (i in 0 until int) {
        str += "-"
    }



    return str
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

        val tipName = obj.name + "(" + roundOffAmountString(obj.rate) + "%)"
        val price = "(Tip $" + calculateTipAmt(
            obj.rate,
            totalAmt
        ) + " Total $" + roundOffAmountString(
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

//fun addTipsList(
//    list: List<GetTipReponse.Data>,
//    totalAmt: Double,
//    font: String
//) {
//    for (i in 0 until list.size) {
//        val obj = list.get(i)
//
//
//        val tipName = obj.name + "(" + roundOffAmountString(obj.rate) + "%)"
//
//        val price = "(Tip $" + calculateTipAmt(
//            obj.rate,
//            totalAmt
//        ) + " Total $" + roundOffAmountString(
//            (totalAmt + calculateTipAmt(
//                obj.rate,
//                totalAmt
//            ))
//        ) + ")"
//
//        val str = padLine(
//            tipName, price, if (font == Constants.LARGE) {
//                23
//            } else {
//                48
//            }
//        ).toString()
//
//        PrintSunmiUtils.orderTime(str)
//
//    }
//
//
//}

fun addOrdersForKitchenCustoemrPrinter(
    builder: Builder,
    list: ArrayList<TbItem>,
    fontSizeH: Int = 1,
    fontSizeW: Int = 1
): Builder {
    list.forEach { obj ->


        builder.addTextLineSpace(30)
        builder.addFeedUnit(30)
        builder.addTextFont(Builder.FONT_E)
        builder.addTextLang(Builder.LANG_EN)
        builder.addTextAlign(Builder.ALIGN_LEFT)
        builder.addTextSize(fontSizeH, fontSizeW)
        builder.addTextStyle(
            Builder.FALSE,
            Builder.FALSE,
            Builder.FALSE,
            Builder.COLOR_1
        )

        builder.addText(obj.itemQuantity.toString() + " " + obj.name)

        if (obj.modifiers.isNotEmpty()) {
            for (j in 0 until obj.modifiers.size) {
                val modifierObj = obj.modifiers.get(j)
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                //builder.addTextLineSpace(20)
                builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(fontSizeH, fontSizeW)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_2
                )
                //builder.addTextPosition(1)


                builder.addText("  " + modifierObj.name)


            }
        }
        if (obj.note.isNotEmpty()) {
            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)
            builder.addTextFont(Builder.FONT_E)
            //builder.addTextLineSpace(20)
            builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(fontSizeH, fontSizeW)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )
            builder.addText("  Note:" + obj.note)

        }
    }


    return builder

}

fun addTipsList(
    list: List<GetTipReponse.Data>,
    totalAmt: Double,
    font: String
) {
    for (i in 0 until list.size) {
        val obj = list.get(i)


        val tipName = obj.name + "(" + roundOffAmountString(obj.rate) + "%)"

        val price = "(Tip $" + calculateTipAmt(
            obj.rate,
            totalAmt
        ) + " Total $" + roundOffAmountString(
            (totalAmt + calculateTipAmt(
                obj.rate,
                totalAmt
            ))
        ) + ")"

        val str = padLine(
            tipName, price, if (font == Constants.LARGE) {
                23
            } else {
                48
            }
        ).toString()

        PrintSunmiUtils.orderTime(str)

    }


}

fun addTipsListInner(
    list: List<GetTipReponse.Data>,
    totalAmt: Double,
    font: String
) {
    for (i in 0 until list.size) {
        val obj = list.get(i)


        val tipName = obj.name + "(" + roundOffAmountString(obj.rate) + "%)"

        val price = "(Tip $" + calculateTipAmt(
            obj.rate,
            totalAmt
        ) + " Total $" + roundOffAmountString(
            (totalAmt + calculateTipAmt(
                obj.rate,
                totalAmt
            ))
        ) + ")"

        val str = padLine(
            tipName, price, if (font == Constants.LARGE) {
                23
            } else {
                48
            }
        ).toString()

        PrintSunmiUtils.normalText(str)

    }


}

fun addOrdersForKitchenDineIn(
    builder: Builder,
    list: ArrayList<TbItem>,
    fontSizeH: Int = 1,
    fontSizeW: Int = 1
): Builder {


    list.forEach { obj ->


        builder.addTextLineSpace(30)
        builder.addFeedUnit(30)
        builder.addTextFont(Builder.FONT_C)
        builder.addTextLang(Builder.LANG_EN)
        builder.addTextAlign(Builder.ALIGN_LEFT)
        builder.addTextSize(fontSizeH, fontSizeW)
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
                builder.addTextSize(fontSizeH, fontSizeW)
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
            builder.addTextSize(fontSizeH, fontSizeW)
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

fun addOrdersForKitchenOnlineOrder(
    builder: Builder,
    list: List<OnlineOrderResponseModel.Data.OrderItem>,
    fontSizeH: Int = 1,
    fontSizeW: Int = 1
): Builder {
    for (i in 0 until list.size) {
        val obj = list.get(i)
        builder.addTextLineSpace(30)
        builder.addFeedUnit(30)
        builder.addTextFont(Builder.FONT_C)
        builder.addTextLang(Builder.LANG_EN)
        builder.addTextAlign(Builder.ALIGN_LEFT)
        builder.addTextSize(fontSizeH, fontSizeW)
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
                builder.addTextSize(fontSizeH, fontSizeW)
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
            builder.addTextSize(fontSizeH, fontSizeW)
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

fun addOrdersForKitchenDineIn(

    list: ArrayList<TbItem>
) {

    list.forEach { obj ->


        PrintSunmiUtils.orderTime(obj.itemQuantity.toString() + " " + obj.name)

        if (obj.modifiers.isNotEmpty()) {
            for (j in 0 until obj.modifiers.size) {
                val modifierObj = obj.modifiers.get(j)

                PrintSunmiUtils.orderTime("  " + modifierObj.name)


            }
        }
        if (obj.note.isNotEmpty()) {

            PrintSunmiUtils.orderTime("  Note:" + obj.note)

        }
    }

}

fun addOrdersForKitchenCustomer(
    builder: Builder,
    list: List<CreateOrderResponse.Data.Order.OrderItem>,
    fontSizeH: Int = 1,
    fontSizeW: Int = 1
): Builder {
    for (i in 0 until list.size) {
        val obj = list.get(i)
        builder.addTextLineSpace(30)
        builder.addFeedUnit(30)
        builder.addTextFont(Builder.FONT_E)
        builder.addTextLang(Builder.LANG_EN)
        builder.addTextAlign(Builder.ALIGN_LEFT)
        builder.addTextSize(fontSizeH, fontSizeW)
        builder.addTextStyle(
            Builder.FALSE,
            Builder.FALSE,
            Builder.FALSE,
            Builder.COLOR_1
        )

        builder.addText(obj.quantity.toString() + " " + obj.itemName)

        if (obj.orderItemModifiers.isNotEmpty()) {
            for (j in 0 until obj.orderItemModifiers.size) {
                val modifierObj = obj.orderItemModifiers.get(j)
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                //builder.addTextLineSpace(20)
                builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(fontSizeH, fontSizeW)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_2
                )
                //builder.addTextPosition(1)


                builder.addText("  " + modifierObj.name)


            }
        }
        if (obj.note.isNotEmpty()) {
            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)
            builder.addTextFont(Builder.FONT_E)
            //builder.addTextLineSpace(20)
            builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(fontSizeH, fontSizeW)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )
            builder.addText("  Note:" + obj.note)

        }


    }

    return builder
}

fun addOrdersForKitchenCustomerNewPrinter(
    builder: Printer,
    list: List<CreateOrderResponse.Data.Order.OrderItem>,
    fontSizeH: Int = 1,
    fontSizeW: Int = 1
): Printer {
    for (i in 0 until list.size) {
        val obj = list.get(i)
        builder.addFeedUnit(30)
        builder.addTextFont(Builder.FONT_E)
        builder.addTextLang(Builder.LANG_EN)
        builder.addTextAlign(Builder.ALIGN_LEFT)
        builder.addTextSize(fontSizeH, fontSizeW)
        builder.addTextStyle(
            Builder.FALSE,
            Builder.FALSE,
            Builder.FALSE,
            Builder.COLOR_1
        )

        builder.addText(obj.quantity.toString() + " " + obj.itemName)

        if (obj.orderItemModifiers.isNotEmpty()) {
            for (j in 0 until obj.orderItemModifiers.size) {
                val modifierObj = obj.orderItemModifiers.get(j)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                //builder.addTextLineSpace(20)
                builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(fontSizeH, fontSizeW)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_2
                )
                //builder.addTextPosition(1)


                builder.addText("  " + modifierObj.name)


            }
        }
        if (obj.note.isNotEmpty()) {
            builder.addFeedUnit(30)
            builder.addTextFont(Builder.FONT_E)
            //builder.addTextLineSpace(20)
            builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(fontSizeH, fontSizeW)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )
            builder.addText("  Note:" + obj.note)

        }


    }

    return builder
}


//fun addOrdersForKitchenDineIn(
//
//    list: ArrayList<TbItem>
//) {
//
//    list.forEach { obj ->
//
//
//        PrintSunmiUtils.orderTime(obj.itemQuantity.toString() + " " + obj.name)
//
//        if (obj.modifiers.isNotEmpty()) {
//            for (j in 0 until obj.modifiers.size) {
//                val modifierObj = obj.modifiers.get(j)
//
//                PrintSunmiUtils.orderTime("  " + modifierObj.name)
//
//
//            }
//        }
//        if (obj.note.isNotEmpty()) {
//
//            PrintSunmiUtils.orderTime("  Note:" + obj.note)
//
//        }
//    }
//
//}

fun addOrdersForKitchen(
    builder: Builder,
    list: List<CreateOrderResponse.Data.Order.OrderItem>,
    fontSizeH: Int = 1,
    fontSizeW: Int = 1
): Builder {
    for (i in 0 until list.size) {
        val obj = list.get(i)
        builder.addTextLineSpace(30)
        builder.addFeedUnit(30)
        builder.addTextFont(Builder.FONT_C)
        builder.addTextLang(Builder.LANG_EN)
        builder.addTextAlign(Builder.ALIGN_LEFT)
        builder.addTextSize(fontSizeH, fontSizeW)
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
                builder.addTextSize(fontSizeH, fontSizeW)
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
            builder.addTextSize(fontSizeH, fontSizeW)
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
    list: List<CreateOrderResponse.Data.Order.OrderItem>
) {
    for (i in 0 until list.size) {
        val obj = list.get(i)


        PrintSunmiUtils.orderTime(obj.quantity.toString() + " " + obj.itemName)

        if (obj.orderItemModifiers.isNotEmpty()) {
            for (j in 0 until obj.orderItemModifiers.size) {
                val modifierObj = obj.orderItemModifiers.get(j)

                PrintSunmiUtils.orderTime("  " + modifierObj.name)


            }
        }
        if (obj.note.isNotEmpty()) {
            PrintSunmiUtils.orderTime("  Note:" + obj.note)
        }


    }

}

fun addOrdersForKitchenInner(
    list: List<CreateOrderResponse.Data.Order.OrderItem>
) {
    for (i in 0 until list.size) {
        val obj = list.get(i)


        PrintSunmiUtils.normalText(obj.quantity.toString() + " " + obj.itemName)

        if (obj.orderItemModifiers.isNotEmpty()) {
            for (j in 0 until obj.orderItemModifiers.size) {
                val modifierObj = obj.orderItemModifiers.get(j)

                PrintSunmiUtils.normalText("  " + modifierObj.name)


            }
        }
        if (obj.note.isNotEmpty()) {
            PrintSunmiUtils.normalText("  Note:" + obj.note)
        }


    }

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
                "$" + roundOffAmountString(totalPriceOpenOrder(obj)),
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
                        "$" + roundOffAmountString(modifierObj.price.toDouble() * modifierObj.quantity),
                        if (font == Constants.LARGE) {
                            23
                        } else {
                            47
                        }
                    )
                )


            }

        }

        if (obj.note.isNotEmpty()) {
            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)
            builder.addTextFont(Builder.FONT_E)
            builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            addCustomerTextSize(builder, font)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )
            builder.addText("   Note: " + obj.note)
            builder.addFeedLine(1)


        }
    }


    return builder


}

fun addOrderItemOpenOrderSunmi(
    list: List<OpenOrderResponse.Data.Order.OrderItem>,
    font: String,
    showModifiers: Boolean
) {
    for (i in 0 until list.size) {
        val obj = list.get(i)

        PrintSunmiUtils.orderTime(
            padLineCustomerItem(
                obj.quantity.toString() + "x " + obj.itemName,
                "$" + roundOffAmountString(totalPriceOpenOrder(obj)),
                if (font == Constants.LARGE) {
                    23
                } else {
                    48
                }
            ).toString()
        )



        if (obj.orderItemModifiers.isNotEmpty() && showModifiers) {
            for (j in 0 until obj.orderItemModifiers.size) {
                val modifierObj = obj.orderItemModifiers.get(j)

                PrintSunmiUtils.orderTime(
                    padLineCustomerItem(
                        "   " + modifierObj.name,
                        "$" + roundOffAmountString(modifierObj.price.toDouble() * modifierObj.quantity),
                        if (font == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                )

            }

        }

        if (obj.note.isNotEmpty()) {

            PrintSunmiUtils.orderTime("   Note: " + obj.note)

        }
    }


}

fun addWholeTbItemToGuest(
    builder: Builder,
    list: TbItem,
    font: String,
    showModifiers: Boolean,
    guestCount: Int,
    serviceChargeList: ArrayList<TbServiceCharge>,
    prefProvider: PrefProvider
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


    var subTotal = (obj.price * obj.itemQuantity).toDouble()

    if (obj.modifiers.isNotEmpty()) {
        obj.modifiers.forEach {
            subTotal += it.price * it.itemQuantity
        }
    }
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
        if (prefProvider.getValueboolean(Constants.SERVICECHARGE_DINEIN_ORDER, false)) {
            var isApplied = false
            serviceChargeList.forEach {
                if (it.order_type == Constants.SERVICECHARGE_DINEIN_ORDER) {
                    if (isInRange(it.min_guest_count!!, it.max_guest_count!!, guestCount)) {
                        isApplied = true
                        serviceCharge += (subTotal * it.percentage) / 100
                        return@forEach
                    }
                }
            }
            if (!isApplied) {
                serviceChargeList.forEach { service ->
                    if (service.id == checkMaxGuestCountId(serviceChargeList)) {
                        serviceCharge += (subTotal * service.percentage) / 100
                        return@forEach
                    }
                }
            }
        }


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


    return builder
}


fun addWholeTbItemToGuest(
    list: TbItem,
    font: String,
    showModifiers: Boolean,
    guestCount: Int,
    serviceChargeList: ArrayList<TbServiceCharge>
) {

    val obj = list

    var subTotal = (obj.price * obj.itemQuantity).toDouble()

    if (obj.modifiers.isNotEmpty()) {
        obj.modifiers.forEach {
            subTotal += it.price * it.itemQuantity
        }
    }
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

    if (serviceChargeList.isNotEmpty()) {
        serviceChargeList.forEach {
            if (it.isEnabled) {
                serviceCharge += (subTotal * it.percentage) / 100
            }
        }
    }


    val finalAmt = MethodUtils.roundOffAmount((subTotal) / guestCount)


    PrintSunmiUtils.orderTime(
        padLineCustomerItem(
            obj.itemQuantity.toString() + "x " + obj.name,
            "" + finalAmt,
            if (font == Constants.LARGE) 23 else 48
        ).toString()
    )

}


fun addWholeTbItemToGuestInner(
    list: TbItem,
    font: String,
    showModifiers: Boolean,
    guestCount: Int,
    serviceChargeList: ArrayList<TbServiceCharge>
) {

    val obj = list

    var subTotal = (obj.price * obj.itemQuantity).toDouble()

    if (obj.modifiers.isNotEmpty()) {
        obj.modifiers.forEach {
            subTotal += it.price * it.itemQuantity
        }
    }
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

    if (serviceChargeList.isNotEmpty()) {
        serviceChargeList.forEach {
            if (it.isEnabled) {
                serviceCharge += (subTotal * it.percentage) / 100
            }
        }
    }


    val finalAmt = MethodUtils.roundOffAmount((subTotal) / guestCount)


    PrintSunmiUtils.normalText(
        padLineCustomerItem(
            obj.itemQuantity.toString() + "x " + obj.name,
            "" + finalAmt,
            if (font == Constants.LARGE) 23 else 48
        ).toString()
    )

}

fun isInRange(minn: Int, maxx: Int, value: Int): Boolean {
    return (minn <= value && value <= maxx)
}

fun checkMaxGuestCountId(serviceChargeList: ArrayList<TbServiceCharge>): Int {
    var maxValue = 0
    var serviceChargeId = 0
    serviceChargeList.forEach { serviceCharge ->
        if (serviceCharge.order_type == Constants.SERVICECHARGE_DINEIN_ORDER) {
            if (serviceCharge.max_guest_count!! >= maxValue) {
                maxValue = serviceCharge.max_guest_count
                serviceChargeId = serviceCharge.id
            }
        }
    }
    return serviceChargeId
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
            "$" + roundOffAmountString(totalPriceDineInItem(obj)),
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
                    "$" + roundOffAmountString(modifierObj.price.toDouble() * modifierObj.itemQuantity),
                    if (font == Constants.LARGE) {
                        23
                    } else {
                        47
                    }
                )
            )


        }


    }

    if (obj.note.isNotEmpty()) {
        builder.addTextLineSpace(30)
        builder.addFeedUnit(30)
        builder.addTextFont(Builder.FONT_E)
        builder.addTextAlign(Builder.ALIGN_LEFT)
        builder.addTextLang(Builder.LANG_EN)
        addCustomerTextSize(builder, font)
        builder.addTextStyle(
            Builder.FALSE,
            Builder.FALSE,
            Builder.FALSE,
            Builder.COLOR_1
        )
        builder.addText("   Note: " + obj.note)
        builder.addFeedLine(1)

    }


    return builder
}


fun addOrderItemForDineIn(
    list: TbItem,
    font: String,
    showModifiers: Boolean
) {


    val obj = list


    PrintSunmiUtils.orderTime(
        padLineCustomerItem(
            obj.itemQuantity.toString() + "x " + obj.name,
            "$" + roundOffAmountString(totalPriceDineInItem(obj)),
            if (font == Constants.LARGE) 23 else 48
        ).toString()
    )



    if (obj.modifiers.isNotEmpty() && showModifiers) {
        for (j in 0 until obj.modifiers.size) {
            val modifierObj = obj.modifiers.get(j)


            PrintSunmiUtils.orderTime(
                padLineCustomerItem(
                    "   " + modifierObj.name,
                    "$" + roundOffAmountString(modifierObj.price.toDouble() * modifierObj.itemQuantity),
                    if (font == Constants.LARGE) 23 else 48
                ).toString()
            )


        }


    }

    if (obj.note.isNotEmpty()) {
        PrintSunmiUtils.orderTime("   Note: " + obj.note)
    }


}

fun addOrderItemForDineInInner(
    list: TbItem,
    font: String,
    showModifiers: Boolean
) {


    val obj = list


    PrintSunmiUtils.normalText(
        padLineCustomerItem(
            obj.itemQuantity.toString() + "x " + obj.name,
            "$" + roundOffAmountString(totalPriceDineInItem(obj)),
            if (font == Constants.LARGE) 23 else 48
        ).toString()
    )



    if (obj.modifiers.isNotEmpty() && showModifiers) {
        for (j in 0 until obj.modifiers.size) {
            val modifierObj = obj.modifiers.get(j)


            PrintSunmiUtils.normalText(
                padLineCustomerItem(
                    "   " + modifierObj.name,
                    "$" + roundOffAmountString(modifierObj.price.toDouble() * modifierObj.itemQuantity),
                    if (font == Constants.LARGE) 23 else 48
                ).toString()
            )


        }


    }

    if (obj.note.isNotEmpty()) {
        PrintSunmiUtils.normalText("   Note: " + obj.note)
    }


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
                "$" + roundOffAmountString(totalPrice(obj)),
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
                        "$" + roundOffAmountString(modifierObj.price.toDouble() * modifierObj.quantity),
                        if (font == Constants.LARGE) {
                            23
                        } else {
                            47
                        }
                    )
                )


            }

        }

        if (obj.note.isNotEmpty()) {
            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)
            builder.addTextFont(Builder.FONT_E)
            builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            addCustomerTextSize(builder, font)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )
            builder.addText("   Note: " + obj.note)
            builder.addFeedLine(1)

        }


    }


    return builder
}

fun addOrderItems(
    list: List<CreateOrderResponse.Data.Order.OrderItem>,
    showModifiers: Boolean,
    font: String,
) {
    for (i in 0 until list.size) {
        val obj = list[i]


        val item = padLineCustomerItem(
            obj.quantity.toString() + "x " + obj.itemName,
            "$" + roundOffAmountString(totalPrice(obj)),
            if (font == Constants.LARGE) 23 else 48
        )

        PrintSunmiUtils.orderTime(item.toString())



        if (obj.orderItemModifiers.isNotEmpty()) {
            for (j in 0 until obj.orderItemModifiers.size) {
                val modifierObj = obj.orderItemModifiers.get(j)

                val modifier = padLineCustomerItem(
                    "   " + modifierObj.name,
                    "$" + roundOffAmountString(modifierObj.price.toDouble() * modifierObj.quantity),
                    if (font == Constants.LARGE) 22 else 48
                )

                PrintSunmiUtils.orderTime(modifier.toString())

            }

        }

        if (obj.note.isNotEmpty()) {

            PrintSunmiUtils.orderTime("   Note: " + obj.note)

        }


    }


}

fun addOrderItemsInner(
    list: List<CreateOrderResponse.Data.Order.OrderItem>,
    showModifiers: Boolean,
    font: String,
) {
    for (i in 0 until list.size) {
        val obj = list[i]


        val item = padLineCustomerItem(
            obj.quantity.toString() + "x " + obj.itemName,
            "$" + roundOffAmountString(totalPrice(obj)),
            if (font == Constants.LARGE) 23 else 48
        )

        PrintSunmiUtils.normalText(item.toString())



        if (obj.orderItemModifiers.isNotEmpty()) {
            for (j in 0 until obj.orderItemModifiers.size) {
                val modifierObj = obj.orderItemModifiers.get(j)

                val modifier = padLineCustomerItem(
                    "   " + modifierObj.name,
                    "$" + roundOffAmountString(modifierObj.price.toDouble() * modifierObj.quantity),
                    if (font == Constants.LARGE) 22 else 48
                )

                PrintSunmiUtils.normalText(modifier.toString())

            }

        }

        if (obj.note.isNotEmpty()) {

            PrintSunmiUtils.normalText("   Note: " + obj.note)

        }


    }


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
                "$" + roundOffAmountString(totalPriceTransaction(obj)),
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
                        "$" + roundOffAmountString(modifierObj.price.toDouble() * modifierObj.quantity),
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
    list: List<GetOrderDetailsResponse.Data.OrderItem>,
    font: String,
    showModifiers: Boolean
) {
    for (i in 0 until list.size) {
        val obj = list.get(i)

        val item = padLineCustomerItem(
            obj.quantity.toString() + "x " + obj.itemName,
            "$" + roundOffAmountString(totalPriceTransaction(obj)),
            if (font == Constants.LARGE) 23 else 48
        )

        PrintSunmiUtils.orderTime(item.toString())



        if (obj.orderItemModifiers.isNotEmpty() && showModifiers) {
            for (j in 0 until obj.orderItemModifiers.size) {
                val modifierObj = obj.orderItemModifiers.get(j)


                val modifier = padLineCustomerItem(
                    "   " + modifierObj.name,
                    "$" + roundOffAmountString(modifierObj.price.toDouble() * modifierObj.quantity),
                    if (font == Constants.LARGE) 23 else 48
                )
                PrintSunmiUtils.orderTime(modifier.toString())


            }

        }
    }
}

fun addOrderItemsTransactionInner(
    list: List<GetOrderDetailsResponse.Data.OrderItem>,
    font: String,
    showModifiers: Boolean
) {
    for (i in 0 until list.size) {
        val obj = list.get(i)

        val item = padLineCustomerItem(
            obj.quantity.toString() + "x " + obj.itemName,
            "$" + roundOffAmountString(totalPriceTransaction(obj)),
            if (font == Constants.LARGE) 23 else 48
        )

        PrintSunmiUtils.normalText(item.toString())



        if (obj.orderItemModifiers.isNotEmpty() && showModifiers) {
            for (j in 0 until obj.orderItemModifiers.size) {
                val modifierObj = obj.orderItemModifiers.get(j)


                val modifier = padLineCustomerItem(
                    "   " + modifierObj.name,
                    "$" + roundOffAmountString(modifierObj.price.toDouble() * modifierObj.quantity),
                    if (font == Constants.LARGE) 23 else 48
                )
                PrintSunmiUtils.normalText(modifier.toString())


            }

        }
    }
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
