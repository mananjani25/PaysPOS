package com.pays.pos.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import androidx.annotation.Nullable
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.pays.pos.data.entities.TbCartItem
import com.pays.pos.data.entities.TbItem
import com.pays.pos.data.entities.TbServiceCharge
import com.pays.pos.data.model.GetPaymentOrderDetailsResponse
import com.pays.pos.data.model.GuestAttrQueue
import com.pays.pos.data.model.responseModel.CreateOrderResponse
import com.pays.pos.data.model.responseModel.EodReportResponse
import com.pays.pos.data.model.responseModel.GetOrderDetailsResponse
import com.pays.pos.data.model.responseModel.GetTipReponse
import com.pays.pos.data.model.responseModel.OnlineOrderResponseModel
import com.pays.pos.data.model.responseModel.OpenOrderResponse
import com.pays.pos.data.model.responseModel.PrinterResponse
import com.pays.pos.data.model.responseModel.employeeTipSummary.EmployeeTipSummaryResponse
import com.pays.pos.data.model.responseModel.report.KeyValue
import com.pays.pos.data.remote.Constants
import com.pays.pos.di.PrefProvider
import com.pays.pos.ui.fragments.settings.hardware.printer.SunmiPrintHelper
import com.epson.epos2.printer.Printer
import com.epson.eposprint.Builder
import com.sunmi.externalprinterlibrary.api.SunmiPrinterApi
import com.sunmi.externalprinterlibrary2.printer.CloudPrinter

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


fun addPaymentDetailsHeader() {

    PrintSunmiUtils.orderTime("Details" + repeat(" ", 20) + "Refund" + repeat(" ", 9) + "Amount")


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

fun addItemWiseSalesHeader(builder: Builder): Builder {
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
    builder.addText("Item Name" + repeat(" ", 18) + "Quantity" + repeat(" ", 7) + "Amount")


    return builder
}


fun addPaymentDetailsHeaderEODP(builder: Printer): Printer {
    builder.addFeedUnit(30)
    builder.addTextFont(Builder.FONT_E)
    // builder.addTextAlign(Builder.ALIGN_LEFT)
    builder.addTextLang(Builder.LANG_EN)
    addCustomerTextSizeEODP(builder, Constants.SMALL)
    builder.addTextStyle(
        Builder.FALSE,
        Builder.FALSE,
        Builder.TRUE,
        Builder.COLOR_1
    )
    builder.addText("Details" + repeat(" ", 20) + "Refund" + repeat(" ", 9) + "Amount")


    return builder
}

fun addItemWiseSalesHeader() {

    val header = "Item Name" + repeat(" ", 18) + "Quantity" + repeat(" ", 7) + "Amount"
    PrintSunmiUtils.orderTime(header)

}

fun addItemWiseSalesHeaderSunmiInner() {

    val header = "Item Name" + repeat(" ", 18) + "Quantity" + repeat(" ", 7) + "Amount"
    Log.e("addItemWiseSalesHeader", "$header")
    PrintSunmiUtils.normalText(header)

}

fun employeeTipSummaryHeader() {

    val header = "Employee Name   Cash Tips  Card Tips  Total Tips"

    PrintSunmiUtils.normalText(header)

}

fun addPaymentDetailsHeaderInner() {

    PrintSunmiUtils.normalText("Details" + repeat(" ", 20) + "Refund" + repeat(" ", 9) + "Amount")


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
            refund = if (it.value?.isNotEmpty() == true) {
                it.showData()
            } else {
                "$0.00"
            }
        } else {
            title = it.key.toString()
            amount = if (it.value?.isNotEmpty() == true) {
                it.showData()
            } else {
                "$0.00"
            }
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

fun addPaymentDetailsThreeDataEODP(
    builder: Printer,
    keyValue: java.util.ArrayList<KeyValue>
): Printer {
    builder.addFeedUnit(30)
    builder.addTextFont(Builder.FONT_E)
    // builder.addTextAlign(Builder.ALIGN_LEFT)
    builder.addTextLang(Builder.LANG_EN)
    addCustomerTextSizeEODP(builder, Constants.SMALL)
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
            refund = if (it.value?.isNotEmpty() == true) {
                MethodUtils.roundOffAmount(it.value.toString().toDouble())
            } else {
                "$0.00"
            }
        } else {
            title = it.key.toString()
            amount = if (it.value?.isNotEmpty() == true) {
                MethodUtils.roundOffAmount(it.value.toString().toDouble())
            } else {
                "$0.00"
            }
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
            refund = it.showData()
        } else {
            title = it.key.toString()
            amount = if (it.value?.isNotEmpty() == true) {
                it.showData()
            } else {
                "$0.00"
            }
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

fun addPaymentDetailsThreeDataInner(keyValue: java.util.ArrayList<KeyValue>) {

    var title = ""
    var refund = ""
    var amount = ""


    keyValue.forEach {


        if (it.key.toString().toLowerCase().contains("Refund".toLowerCase())) {
            refund = it.showData()
        } else {
            title = it.key.toString()
            amount = if (it.value?.isNotEmpty() == true) {
                it.showData()
            } else {
                "$0.00"
            }
        }
    }

    var fPart = title + repeat(" ", 27 - title.length) + refund
    var spaceLastPart = 48 - fPart.length
    var spaceLast = 0
    if (spaceLastPart > 1 && amount.length < spaceLastPart) {
        spaceLast = spaceLastPart - amount.length
    }

    fPart += repeat(" ", spaceLast) + amount

    PrintSunmiUtils.normalText(fPart)

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

fun employeeGuestDetailsDataEODP(builder: Printer, keyValue: KeyValue): Printer {
    builder.addFeedUnit(30)
    builder.addTextFont(Builder.FONT_E)
    // builder.addTextAlign(Builder.ALIGN_LEFT)
    builder.addTextLang(Builder.LANG_EN)
    addCustomerTextSizeEODP(builder, Constants.SMALL)
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

fun employeeGuestDetailsDataInner(keyValue: KeyValue) {

    var sPart = if (keyValue.key?.contains("Served", true) == true) {
        keyValue.value.toString()
    } else {
        MethodUtils.roundOffAmount(keyValue.value?.toDouble() ?: 0.0)
    }
    PrintSunmiUtils.normalText(
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
            keyValue.showData(),
            48
        )
    )

    return builder
}

fun addPaymentDetailsTwoDataEODP(builder: Printer, keyValue: KeyValue): Printer {
    builder.addFeedUnit(30)
    builder.addTextFont(Builder.FONT_E)
    // builder.addTextAlign(Builder.ALIGN_LEFT)
    builder.addTextLang(Builder.LANG_EN)
    addCustomerTextSizeEODP(builder, Constants.SMALL)
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
            keyValue.showData(),
            48
        ).toString()
    )


}


fun addPaymentDetailsTwoDataInner(keyValue: KeyValue) {

    PrintSunmiUtils.normalText(
        padLine(
            keyValue.key,
            keyValue.showData(),
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

fun addRefundVoidsMultipleEODP(builder: Printer, keyValue: java.util.ArrayList<KeyValue>): Printer {
    builder.addFeedUnit(30)
    builder.addTextFont(Builder.FONT_E)
    // builder.addTextAlign(Builder.ALIGN_LEFT)
    builder.addTextLang(Builder.LANG_EN)
    addCustomerTextSizeEODP(builder, Constants.SMALL)
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

fun addRefundVoidsMultipleInner(keyValue: java.util.ArrayList<KeyValue>) {

    keyValue.forEach {
        if (!it.key?.trim().equals("Item Count".trim(), true)) {
            PrintSunmiUtils.normalText(
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

fun addSixHeaderForEmployeeTipSummary(builder: Builder) {


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

    builder.addText("Employee Name   Cash Tips  Card Tips  Total Tips")

}

fun addSixHeaderForOrderSaleDetailsEODP(builder: Printer): Printer {


    builder.addFeedUnit(30)
    builder.addTextFont(Builder.FONT_E)
    // builder.addTextAlign(Builder.ALIGN_LEFT)
    builder.addTextLang(Builder.LANG_EN)
    addCustomerTextSizeEODP(builder, Constants.SMALL)
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

fun addSixHeaderForEmployeeTipSummarySunmi() {
    PrintSunmiUtils.orderTime("Employee Name   Cash Tips  Card Tips  Total Tips")
}

fun addSixHeaderForOrderSaleDetailsSunmiInner() {
    PrintSunmiUtils.normalText("OrderId    Tip      SC     PayType     Amount   ")
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

fun addCreditTipAuditHeaderEODP(builder: Printer): Printer {
    builder.addFeedUnit(30)
    builder.addTextFont(Builder.FONT_E)
    builder.addTextAlign(Builder.ALIGN_LEFT)
    builder.addTextLang(Builder.LANG_EN)
    addCustomerTextSizeEODP(builder, Constants.SMALL)
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


fun addCreditTipAuditHeaderInner() {

    PrintSunmiUtils.normalText(
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

fun addCreditTipAuditDataEODP(
    builder: Printer,
    fPArt: String,
    sPart: String,
    TPArt: String,
    lPart: String
): Printer {
    builder.addFeedUnit(30)
    builder.addTextFont(Builder.FONT_E)
    builder.addTextAlign(Builder.ALIGN_LEFT)
    builder.addTextLang(Builder.LANG_EN)
    addCustomerTextSizeEODP(builder, Constants.SMALL)
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


fun addCreditTipAuditDataInner(
    fPArt: String,
    sPart: String,
    TPArt: String,
    lPart: String
) {


    var pOne = TPArt + repeat(" ", 13 - TPArt.length) + fPArt

    pOne += repeat(" ", 27 - pOne.length) + sPart
    pOne += repeat(" ", 38 - pOne.length) + lPart


    PrintSunmiUtils.normalText(pOne)

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

fun addCreditCardBreakDownEODP(builder: Printer): Printer {
    builder.addFeedUnit(30)
    builder.addTextFont(Builder.FONT_E)
    // builder.addTextAlign(Builder.ALIGN_LEFT)
    builder.addTextLang(Builder.LANG_EN)
    addCustomerTextSizeEODP(builder, Constants.SMALL)
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


try{
    PrintSunmiUtils.orderTime("CardName" + repeat(" ", 20) + "Tip" + repeat(" ", 11) + "Amount")

}catch (e:java.lang.Exception){

}
}

fun addCreditCardBreakDownInner() {


    PrintSunmiUtils.normalText("CardName" + repeat(" ", 20) + "Tip" + repeat(" ", 11) + "Amount")

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
    val lastPart = 48 - pOne.length
    val amount = creditCardBreakdown.showData()
    var spaceLast = 0
    if (lastPart > 1 && amount.length < lastPart) {
        spaceLast = lastPart - amount.length
    }
    pOne += repeat(" ", spaceLast) + amount

    builder.addText(pOne)
    return builder
}


fun addCreditCardBreakDownDataEODP(
    builder: Printer,
    creditCardBreakdown: EodReportResponse.Data.CreditCardBreakdown
): Printer {
    builder.addFeedUnit(30)
    builder.addTextFont(Builder.FONT_E)
    // builder.addTextAlign(Builder.ALIGN_LEFT)
    builder.addTextLang(Builder.LANG_EN)
    addCustomerTextSizeEODP(builder, Constants.SMALL)
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
    var amount = creditCardBreakdown.showData()
    var spaceLast = 0
    if (lastPart > 1 && amount.length < lastPart) {
        spaceLast = lastPart - amount.length
    }
    pOne += repeat(" ", spaceLast) + amount

    PrintSunmiUtils.orderTime(pOne)

}

fun addCreditCardBreakDownDataInner(
    creditCardBreakdown: EodReportResponse.Data.CreditCardBreakdown
) {

    var pOne = creditCardBreakdown.key + repeat(
        " ",
        28 - creditCardBreakdown.key.length
    ) + MethodUtils.roundOffAmount(creditCardBreakdown.tips)
    var lastPart = 48 - pOne.length
    var amount = creditCardBreakdown.showData()
    var spaceLast = 0
    if (lastPart > 1 && amount.length < lastPart) {
        spaceLast = lastPart - amount.length
    }
    pOne += repeat(" ", spaceLast) + amount

    PrintSunmiUtils.normalText(pOne)

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

fun addItemsInOrderSalesDetailsEODP(
    builder: Printer,
    details: EodReportResponse.Data.OrderSalesDetails.Details
): Printer {


    builder.addFeedUnit(30)
    builder.addTextFont(Builder.FONT_E)
    builder.addTextAlign(Builder.ALIGN_LEFT)
    builder.addTextLang(Builder.LANG_EN)
    addCustomerTextSizeEODP(builder, Constants.SMALL)
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

fun addItemWiseSales(it: EodReportResponse.Data.ItemWiseSalesData) {

    var itemName = ""
    var quantity = ""
    var amount = ""


    itemName = it.itemName
    quantity = it.quantity
    amount = MethodUtils.roundOffAmount(it.amount)

    var fPart = itemName + repeat(" ", 27 - itemName.length) + quantity
    var spaceLastPart = 48 - fPart.length
    var spaceLast = 0
    if (spaceLastPart > 1 && amount.length < spaceLastPart) {
        spaceLast = spaceLastPart - amount.length
    }

    fPart += repeat(" ", spaceLast) + amount

    PrintSunmiUtils.orderTime(fPart)

}

fun addItemWiseSalesSunmiInnerPrinter(it: EodReportResponse.Data.ItemWiseSalesData) {

    var itemName = ""
    var quantity = ""
    var amount = ""


    itemName = it.itemName
    quantity = it.quantity
    amount = MethodUtils.roundOffAmount(it.amount)

    var fPart = itemName + repeat(" ", 27 - itemName.length) + quantity
    var spaceLastPart = 48 - fPart.length
    var spaceLast = 0
    if (spaceLastPart > 1 && amount.length < spaceLastPart) {
        spaceLast = spaceLastPart - amount.length
    }

    fPart += repeat(" ", spaceLast) + amount
    Log.e("addItemWiseSalesHeader", "$fPart")
    PrintSunmiUtils.normalText(fPart)

}

fun addItemsInEmployeeTipsSummary(data: EmployeeTipSummaryResponse.Data) {

    var items = ""
    var emName = data.employee_name
    if (data.employee_name.length >= 13) {
        emName = data.employee_name.substring(0, 11).plus("...")
    }
    items += repeat(" ", 0 - data.employee_name.length) + emName
    items += repeat(" ", 17 - items.length) + MethodUtils.roundOffAmount(data.total_cash_tips)
    items += repeat(" ", 28 - items.length) + MethodUtils.roundOffAmount(data.total_card_tips)
    items += repeat(" ", 39 - items.length) + MethodUtils.roundOffAmount(data.total_tips)

    Log.e("addItemsInEmployeeTip", "$items")
    PrintSunmiUtils.orderTime(items)

}

fun addItemsInEmployeeTipsSummaryInnerPrinter(data: EmployeeTipSummaryResponse.Data) {

    var items = ""
    var emName = data.employee_name
    if (data.employee_name.length >= 13) {
        emName = data.employee_name.substring(0, 11).plus("...")
    }
    items += repeat(" ", 0 - data.employee_name.length) + emName
    items += repeat(" ", 17 - items.length) + MethodUtils.roundOffAmount(data.total_cash_tips)
    items += repeat(" ", 28 - items.length) + MethodUtils.roundOffAmount(data.total_card_tips)
    items += repeat(" ", 39 - items.length) + MethodUtils.roundOffAmount(data.total_tips)

    Log.e("addItemsInEmployeeTip", "$items")
    PrintSunmiUtils.normalText(items)

}

fun itemWiseSalesM30Print(it: EodReportResponse.Data.ItemWiseSalesData, builder: Builder) {

    var itemName = ""
    var quantity = ""
    var amount = ""


    itemName = it.itemName
    quantity = it.quantity
    amount = MethodUtils.roundOffAmount(it.amount)

    var fPart = itemName + repeat(" ", 27 - itemName.length) + quantity
    var spaceLastPart = 48 - fPart.length
    var spaceLast = 0
    if (spaceLastPart > 1 && amount.length < spaceLastPart) {
        spaceLast = spaceLastPart - amount.length
    }

    fPart += repeat(" ", spaceLast) + amount

    builder.addText(fPart)

}

fun addItemsInEmployeeTipsSummaryM30(data: EmployeeTipSummaryResponse.Data, builder: Builder) {

    var items = ""
    var emName = data.employee_name
    if (data.employee_name.length >= 13) {
        emName = data.employee_name.substring(0, 11).plus("...")
    }
    items += repeat(" ", 0 - data.employee_name.length) + emName
    items += repeat(" ", 17 - items.length) + MethodUtils.roundOffAmount(data.total_cash_tips)
    items += repeat(" ", 28 - items.length) + MethodUtils.roundOffAmount(data.total_card_tips)
    items += repeat(" ", 39 - items.length) + MethodUtils.roundOffAmount(data.total_tips)

    Log.e("addItemsInEmployeeTip", "$items")
    //  PrintSunmiUtils.orderTime(items)

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
    builder.addText(items)

}


/** utility: string repeat  */
fun repeat(str: String?, i: Int): String? {
    if (i > -1)
        return String(CharArray(i)).replace("\u0000", str!!)

    return ""
}


fun addItemsInOrderSalesDetailsInner(
    details: EodReportResponse.Data.OrderSalesDetails.Details
) {

    var data = details.orderId
    data += repeat(" ", 10 - details.orderId.length) + MethodUtils.roundOffAmount(details.tip)
    data += repeat(" ", 18 - data.length) + MethodUtils.roundOffAmount(details.serviceCharge)
    data += repeat(" ", 27 - data.length) + details.payType
    data += repeat(" ", 39 - data.length) + MethodUtils.roundOffAmount(details.amount)

    PrintSunmiUtils.normalText(data)
}

fun padLineCustomerItem(
    @Nullable partOne: String?,
    @Nullable partTwo: String?,
    columnsPerLine: Int
): String {
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


fun addCustomerTextSizeEODP(builder: Printer, font: String): Printer {
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
        LogUtil.logE(TAG, "GeneratePartOne: ${strBuffer.toString()}")
        partOne = ""
        partOne = strBuffer.toString()
        partOne + partTwo
        LogUtil.logE(TAG, "FinalString : $partOne + partTwo")
        builder.addText(partOne + partTwo)
        return builder
    } else {
        val padding = columnsPerLine - (partOne.length + partTwo.length)
        partOne + partTwo
        LogUtil.logE(TAG, "FinalElseString: $partOne + partTwo")
        builder.addText(partOne + partTwo)
        return builder

    }

    return concat
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

fun addBuilderTextEODP(
    builder: Printer,
    text: String
): Printer {
    builder.addText(text)
    return builder
}

fun addBuilderTextForU220(
    builder: Printer,
    text: String
): Printer {
    builder.addText(text)
    return builder
}

fun addHorizontalLargeLine(builder: Builder): Builder {
    var str: String = ""
    for (i in 0 until 24) {
        str += "-"
    }
    LogUtil.logE("strLine", "strLine  $str")
    builder.addText(str)

    return builder

}

fun addHorizontalLine(builder: Builder): Builder {


    var str: String = ""
    for (i in 0 until 48) {
        str += "-"
    }
    LogUtil.logE("strLine", "strLine  $str")
    builder.addText(str)

    return builder
}

fun addHorizontalLineForTM30(builder: Builder, fontSize: String): Builder {
    var int = 48
    when (fontSize) {
        Constants.LARGE -> {
            int = 24
        }
    }

    var str: String = ""
    for (i in 0 until int) {
        str += "-"
    }
    LogUtil.logE("strLine", "strLine  $str")
    builder.addText(str)

    return builder
}

fun addHorizontalLineEODP(builder: Printer): Printer {


    var str: String = ""
    for (i in 0 until 48) {
        str += "-"
    }
    LogUtil.logE("strLine", "strLine  $str")
    builder.addText(str)

    return builder
}

fun addHorizontalLineNew(printer: Printer): Printer {
    var str: String = ""
    for (i in 0 until 48) {
        str += "-"
    }
    LogUtil.logE("strLine", "strLine  $str")
    printer.addText(str)

    return printer
}


fun printGuestByItemForQueue(guestAttributes: List<GuestAttrQueue>, printer: Printer): Printer {

    guestAttributes.forEach {
        printer.addFeedLine(1)
        printer.addTextFont(Builder.FONT_C)
        printer.addTextAlign(Builder.ALIGN_LEFT)
        printer.addTextLang(Builder.LANG_EN)
        printer.addTextSize(1, 1)
        printer.addTextStyle(
            Builder.FALSE,
            Builder.FALSE,
            Builder.TRUE,
            Builder.COLOR_1
        )

        printer.addText(it.name)


        it.listOfItems.forEach { obj ->

            printer.addFeedLine(1)
            printer.addTextFont(Builder.FONT_E)
            printer.addTextAlign(Builder.ALIGN_LEFT)
            printer.addTextLang(Builder.LANG_EN)
            printer.addTextSize(1, 1)
            printer.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
            )
            if (obj.timestamp.isNotEmpty()) {
                var msg = "(" + obj.timestamp + ")"
                printer.addText("" + obj.quantity + " " + obj.itemName + "  " + msg)

            } else {

                printer.addText("" + obj.quantity + " " + obj.itemName)
            }

            if (obj.orderItemModifiers.isNotEmpty()) {
                obj.orderItemModifiers.forEach { mod ->

                    printer.addFeedLine(1)
                    printer.addTextFont(Builder.FONT_E)
                    printer.addTextAlign(Builder.ALIGN_LEFT)
                    printer.addTextLang(Builder.LANG_EN)
                    printer.addTextSize(1, 1)
                    printer.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )

                    printer.addText(
                        "  " + if (mod.modifierQuantity == 1) {
                            "   "
                        } else {
                            "" + mod.modifierQuantity + "x "
                        } + mod.name
                    )


                }


            }


        }

    }

    return printer


}

fun printGuestByItemForSunmiQueue(
    guestAttributes: List<GuestAttrQueue>,
    printer: CloudPrinter
): CloudPrinter {

    guestAttributes.forEach {
        printer.lineFeed(1)
        addDotLineForSunmiQueue(printer)
        printer.setCharacterSize(2, 2)
        printer.setBoldMode(true)

        printer.printText(it.name)


        printer.setBoldMode(false)
        addDotLineForSunmiQueue(printer)
        it.listOfItems.forEach { obj ->

            // printer.lineFeed(1)
            printer.setCharacterSize(2, 2)
            printer.setBoldMode(false)

            if (obj.timestamp.isNotEmpty()) {
                var msg = "(" + obj.timestamp + ")"
                printer.printText("" + obj.quantity + " " + obj.itemName + "  " + msg)

            } else {

                printer.printText("" + obj.quantity + " " + obj.itemName)
            }

            if (obj.orderItemModifiers.isNotEmpty()) {
                obj.orderItemModifiers.forEach { mod ->

                    //printer.lineFeed(1)
                    printer.setCharacterSize(2, 2)


                    printer.printText(
                        "  " + if (mod.modifierQuantity == 1) {
                            "   "
                        } else {
                            "" + mod.modifierQuantity + "x "
                        } + mod.name
                    )


                }


            }


        }

    }

    return printer


}

fun addDotLineForSunmiQueue(cloudPrinter: CloudPrinter): CloudPrinter {
    cloudPrinter.setCharacterSize(1, 1)
    cloudPrinter.setBoldMode(true)
    var str: String = ""
    for (i in 0 until 48) {
        str += "-"
    }
    cloudPrinter.printText(str)
    return cloudPrinter
}

fun addDoubleDotLineForSunmiQueue(cloudPrinter: CloudPrinter): CloudPrinter {
    cloudPrinter.setCharacterSize(2, 1)
    cloudPrinter.setBoldMode(true)
    var str: String = ""
    for (i in 0 until 48) {
        str += "-"
    }
    cloudPrinter.printText(str)
    return cloudPrinter
}


fun addHorizontalLineNewU220(printer: Printer): Printer {
    var str: String = ""
    for (i in 0 until 30) {
        str += "-"
    }
    LogUtil.logE("strLine", "strLine  $str")
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


fun addHorizontalKitchenLineForU220(builder: Printer): Printer {


    var str: String = ""
    for (i in 0 until 40) {
        str += "-"
    }

    builder.addText(str)

    return builder
}

fun addHorizontalHalfCustomerReceiptLine(fontSize: String): String {

    var int = 24
    when (fontSize) {
        Constants.LARGE -> {
            int = 12
        }
    }

    var str: String = ""
    for (i in 0 until int) {
        str += "_"
    }



    return str
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

fun addHorizontalKitchenLineSunmi24(fontSize: String): String {

    var int = 48
    when (fontSize) {
        Constants.LARGE -> {
            int = 24
        }
    }

    var str: String = ""
    for (i in 0 until int) {
        str += "-"
    }



    return str
}

fun addHorizontalKitchenLineSunmiForLastHeaderLine(fontSize: String): String {

    var int = 48
    when (fontSize) {
        Constants.LARGE -> {
            int = 25
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
    list: ArrayList<TbCartItem>,
    fontSizeH: Int = 1,
    fontSizeW: Int = 1,
    printerCat: ArrayList<PrinterResponse.Data.PrinterCategories>? = null,
    fontSize: String = Constants.LARGE,
    listItemWithGuest: HashMap<String, ArrayList<TbCartItem>> = hashMapOf()
): Builder {
    listItemWithGuest.forEach {
        builder.addFeedLine(1)
        addHorizontalLineForTM30(builder, fontSize)
        builder.addFeedUnit(30)
        builder.addTextFont(Builder.FONT_B)
        builder.addTextLang(Builder.LANG_EN)
        builder.addTextAlign(Builder.ALIGN_LEFT)
        builder.addTextSize(fontSizeH, fontSizeW)
        builder.addTextStyle(
            Builder.FALSE,
            Builder.FALSE,
            Builder.FALSE,
            Builder.COLOR_1
        )
        builder.addText(it.key)
        builder.addFeedLine(1)
        addHorizontalLineForTM30(builder, fontSize)


        var list = it.value

        list.forEach { obj ->
            printerCat?.forEach {
                if (it.id == obj.categoryId && it.printerEnable && it.categoryActive) {


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


                            builder.addText(
                                "  " + if (modifierObj.modifier_quantity == 1) {
                                    "   "
                                } else {
                                    "" + modifierObj.modifier_quantity + "x "
                                } + modifierObj.name.uppercase()
                            )


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
            }
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
    list: ArrayList<TbCartItem>,
    fontSizeH: Int = 1,
    fontSizeW: Int = 1,
    printerCat: ArrayList<PrinterResponse.Data.PrinterCategories>? = null,
    listItemWithGuest: HashMap<String, ArrayList<TbCartItem>> = hashMapOf()
): Builder {

    listItemWithGuest.forEach {

        builder.addTextLineSpace(30)
        builder.addFeedUnit(30)
        addHorizontalKitchenLine(builder)


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
        builder.addText(it.key.toString())
        builder.addTextLineSpace(30)
        builder.addFeedUnit(30)
        addHorizontalKitchenLine(builder)

        it.value.forEach { obj ->
            printerCat?.forEach {
                if (it.id == obj.categoryId && it.printerEnable && it.categoryActive) {

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

                    builder.addText(obj.itemQuantity.toString() + " " + obj.name.uppercase())

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

                            builder.addText(
                                "  " + if (modifierObj.modifier_quantity == 1) {
                                    "   "
                                } else {
                                    "" + modifierObj.modifier_quantity + "x "
                                } + modifierObj.name.uppercase()
                            )


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
            }
        }

    }



    return builder
}

fun addOrdersForKitchenDineInU220(
    builder: Printer,
    list: ArrayList<TbCartItem>,
    fontSizeH: Int = 1,
    fontSizeW: Int = 1,
    printerCat: ArrayList<PrinterResponse.Data.PrinterCategories>? = null
): Printer {

    list.forEach { obj ->
        printerCat?.forEach {
            if (it.id == obj.categoryId && it.printerEnable && it.categoryActive) {

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

                builder.addText(obj.itemQuantity.toString() + " " + obj.name.uppercase())

                if (obj.modifiers.isNotEmpty()) {
                    for (j in 0 until obj.modifiers.size) {
                        val modifierObj = obj.modifiers.get(j)
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


                        builder.addText(
                            "  " + if (modifierObj.modifier_quantity == 1) {
                                "   "
                            } else {
                                "" + modifierObj.modifier_quantity + "x "
                            } + modifierObj.name.uppercase()
                        )


                    }
                }
                if (obj.note.isNotEmpty()) {
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
        }
    }


    return builder
}

fun addOrdersForKitchenOnlineOrder(
    builder: Builder,
    list: List<OnlineOrderResponseModel.Data.OrderItem>,
    fontSizeH: Int = 1,
    fontSizeW: Int = 1,
    printerCat: ArrayList<PrinterResponse.Data.PrinterCategories>? = null
): Builder {
    for (i in 0 until list.size) {
        printerCat?.forEach {
            if (it.id == list[i].categoryId && it.printerEnable && it.categoryActive) {

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

                builder.addText(obj.quantity.toString() + " " + obj.itemName.uppercase())

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


                        builder.addText(
                            "  " + if (modifierObj.modifier_quantity == 1) {
                                "   "
                            } else {
                                "" + modifierObj.modifier_quantity + "x "
                            } + modifierObj.name.uppercase()
                        )


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
        }
    }


    return builder
}


fun addOrdersForKitchenOnlineOrderU220(
    builder: Printer,
    list: List<OnlineOrderResponseModel.Data.OrderItem>,
    fontSizeH: Int = 1,
    fontSizeW: Int = 1,
    printerCat: ArrayList<PrinterResponse.Data.PrinterCategories>? = null
): Printer {
    for (i in 0 until list.size) {
        printerCat?.forEach {
            if (it.id == list[i].categoryId && it.printerEnable && it.categoryActive) {

                val obj = list.get(i)
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

                builder.addText(obj.quantity.toString() + " " + obj.itemName.uppercase())

                if (obj.orderItemModifiers.isNotEmpty()) {
                    for (j in 0 until obj.orderItemModifiers.size) {
                        val modifierObj = obj.orderItemModifiers.get(j)
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


                        builder.addText(
                            "  " + if (modifierObj.modifier_quantity == 1) {
                                "   "
                            } else {
                                "" + modifierObj.modifier_quantity + "x "
                            } + modifierObj.name.uppercase()
                        )

                    }
                }
                if (obj.note.isNotEmpty()) {
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
        }
    }


    return builder
}

fun addOrdersForKitchenOnlineOrderSunmi(
    list: List<OnlineOrderResponseModel.Data.OrderItem>,
    printerCat: ArrayList<PrinterResponse.Data.PrinterCategories>? = null
) {
    for (i in 0 until list.size) {
        printerCat?.forEach {
            if (it.id == list[i].categoryId && it.printerEnable && it.categoryActive) {
                val obj = list.get(i)
                PrintSunmiUtils.orderTime(obj.quantity.toString() + " " + obj.itemName.uppercase())

                if (obj.orderItemModifiers.isNotEmpty()) {
                    for (j in 0 until obj.orderItemModifiers.size) {
                        val modifierObj = obj.orderItemModifiers.get(j)
                        PrintSunmiUtils.orderTime(
                            "  " + if (modifierObj.modifier_quantity == 1) {
                                "   "
                            } else {
                                "" + modifierObj.modifier_quantity + "x "
                            } + modifierObj.name.uppercase()
                        )
                    }
                }
                if (obj.note.isNotEmpty()) {
                    PrintSunmiUtils.orderTime("  Note:" + obj.note)
                }
            }
        }
    }
}


fun addOrdersForKitchenOnlineOrderSunmiInner(
    list: List<OnlineOrderResponseModel.Data.OrderItem>,
    printerCat: ArrayList<PrinterResponse.Data.PrinterCategories>? = null
) {

    for (i in 0 until list.size) {
        printerCat?.forEach {
            if (it.id == list[i].categoryId && it.printerEnable && it.categoryActive) {
                val obj = list.get(i)
                PrintSunmiUtils.normalTextLarge(obj.quantity.toString() + " " + obj.itemName.uppercase())

                if (obj.orderItemModifiers.isNotEmpty()) {
                    for (j in 0 until obj.orderItemModifiers.size) {
                        val modifierObj = obj.orderItemModifiers.get(j)
                        PrintSunmiUtils.normalTextLarge(
                            "  " + if (modifierObj.modifier_quantity == 1) {
                                "   "
                            } else {
                                "" + modifierObj.modifier_quantity + "x "
                            } + modifierObj.name.uppercase()
                        )
                    }
                }
                if (obj.note.isNotEmpty()) {
                    PrintSunmiUtils.normalTextLarge("  Note:" + obj.note)
                }
            }
        }
    }
}

fun addOrdersForKitchenDineIn(
    list: ArrayList<TbCartItem>,
    printerCat: ArrayList<PrinterResponse.Data.PrinterCategories>? = null,
    listItemWithGuest: HashMap<String, ArrayList<TbCartItem>> = hashMapOf()
) {

    listItemWithGuest.forEach {

        SunmiPrinterApi.getInstance()
            .printText(addHorizontalKitchenLineSunmi24(PrintSunmiUtils.fontSize))
        SunmiPrinterApi.getInstance().enableUnderline(false)
        SunmiPrinterApi.getInstance().enableBold(false)

        SunmiPrinterApi.getInstance().setAlignMode(0)
        SunmiPrinterApi.getInstance()
            .printText(it.key + "\n")

        SunmiPrinterApi.getInstance()
            .printText(addHorizontalKitchenLineSunmi24(PrintSunmiUtils.fontSize))
        SunmiPrinterApi.getInstance().lineWrap(1)

        it.value.forEach { obj ->

            printerCat?.forEach {
                if (it.id == obj.categoryId && it.printerEnable && it.categoryActive) {

                    PrintSunmiUtils.orderTime(obj.itemQuantity.toString() + " " + obj.name.uppercase())

                    if (obj.modifiers.isNotEmpty()) {
                        for (j in 0 until obj.modifiers.size) {
                            val modifierObj = obj.modifiers.get(j)


                            PrintSunmiUtils.orderTime(
                                "  " + if (modifierObj.modifier_quantity == 1) {
                                    "   "
                                } else {
                                    "" + modifierObj.modifier_quantity + "x "
                                } + modifierObj.name.uppercase()
                            )


                        }
                    }
                    if (obj.note.isNotEmpty()) {

                        PrintSunmiUtils.orderTime("  Note:" + obj.note)

                    }
                }
            }
        }

    }
}

fun addOrdersForKitchenDineInInner(
    list: ArrayList<TbCartItem>,
    listItemWithGuest: HashMap<String, ArrayList<TbCartItem>>
) {

    listItemWithGuest.forEach {

        PrintSunmiUtils.addHorizontalInner()

        PrintSunmiUtils.normalTextLarge(it.key.toString())
        PrintSunmiUtils.addHorizontalInner()


        it.value.forEach { obj ->
            PrintSunmiUtils.normalTextLarge(obj.itemQuantity.toString() + " " + obj.name.uppercase())

            if (obj.modifiers.isNotEmpty()) {
                for (j in 0 until obj.modifiers.size) {
                    val modifierObj = obj.modifiers.get(j)



                    PrintSunmiUtils.normalTextLarge(
                        "  " + if (modifierObj.modifier_quantity == 1) {
                            "   "
                        } else {
                            "" + modifierObj.modifier_quantity + "x "
                        } + modifierObj.name.uppercase()
                    )


                }
            }
            if (obj.note.isNotEmpty()) {

                PrintSunmiUtils.normalTextLarge("  Note:" + obj.note)

            }
        }

    }
}

fun addOrdersForKitchenCustomer(
    builder: Builder,
    list: List<CreateOrderResponse.Data.Order.OrderItem>,
    fontSizeH: Int = 1,
    fontSizeW: Int = 1,
    printerCat: ArrayList<PrinterResponse.Data.PrinterCategories>? = null
): Builder {
    for (i in 0 until list.size) {

        printerCat?.forEach {
            if (it?.id == list[i].categoryId) {
                if (it.categoryActive && it.printerEnable) {
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

                    builder.addText(obj.quantity.toString() + " " + obj.itemName.uppercase())

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


                            builder.addText(
                                "  " + if (modifierObj.modifierQuantity == 1) {
                                    "   "
                                } else {
                                    "" + modifierObj.modifierQuantity + "x "
                                } + modifierObj.name.uppercase()
                            )


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
            }
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

        builder.addText(obj.quantity.toString() + " " + obj.itemName.uppercase())

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


                builder.addText(
                    "  " + if (modifierObj.modifierQuantity == 1) {
                        "   "
                    } else {
                        "" + modifierObj.modifierQuantity + "x "
                    } + modifierObj.name.uppercase()
                )


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
fun checkItemsforPrinterDineIn(
    list: List<TbCartItem>,
    printerCat: ArrayList<PrinterResponse.Data.PrinterCategories?>? = null
): Boolean {
    var flag = false
    for (i in 0 until list.size) {
        printerCat?.forEach {
            if (it?.id == list[i].categoryId && it.categoryActive && it.printerEnable) {
                flag = true
            }
        }
    }

    return flag
}

fun checkItemsforTransactionPrinter(
    list: List<GetOrderDetailsResponse.Data.OrderItem>,
    printerCat: ArrayList<PrinterResponse.Data.PrinterCategories?>? = null
): Boolean {
    var flag = false
    for (i in 0 until list.size) {
        printerCat?.forEach {
            if (it?.id == list[i].categoryId && it.categoryActive && it.printerEnable) {
                flag = true
            }
        }
    }

    return flag
}


fun checkItemsforPrinter(
    list: List<CreateOrderResponse.Data.Order.OrderItem>,
    printerCat: ArrayList<PrinterResponse.Data.PrinterCategories?>? = null
): Boolean {
    var flag = false
    for (i in 0 until list.size) {
        printerCat?.forEach {
            if (it?.id == list[i].categoryId && it.categoryActive && it.printerEnable) {
                flag = true
            }
        }
    }

    return flag
}

fun checkItemsforPrinterOnlineOrder(
    list: List<OnlineOrderResponseModel.Data.OrderItem>,
    printerCat: ArrayList<PrinterResponse.Data.PrinterCategories?>? = null
): Boolean {
    var flag = false
    for (i in 0 until list.size) {
        printerCat?.forEach {
            if (it?.id == list[i].categoryId && it.categoryActive && it.printerEnable) {
                flag = true
            }
        }
    }

    return flag
}

fun addOrdersForKitchen(
    builder: Builder,
    list: List<CreateOrderResponse.Data.Order.OrderItem>,
    fontSizeH: Int = 1,
    fontSizeW: Int = 1,
    printerCat: ArrayList<PrinterResponse.Data.PrinterCategories?>? = null,
    guestAttributes: ArrayList<CreateOrderResponse.Data.Order.GuestAttributes>? = null
): Builder {
    for (i in 0 until list.size) {
        printerCat?.forEach {
            Log.e("PrinterReceipt", "checkPrinterItemN:   ${list.get(i).itemName}")
            if (it?.id == list[i].categoryId) {
                if (it.categoryActive && it.printerEnable) {

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

                    if (obj.isEdited){
                        builder.addText("(U) "+obj.quantity.toString() + " " + obj.itemName.uppercase())
                    }else{
                        builder.addText(obj.quantity.toString() + " " + obj.itemName.uppercase())
                    }

                    builder.addFeedLine(1)
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


                            builder.addText(
                                "  " + if (modifierObj.modifierQuantity == 1) {
                                    "   "
                                } else {
                                    "" + modifierObj.modifierQuantity + "x "
                                } + modifierObj.name.uppercase()
                            )

                            builder.addFeedLine(1)
                        }
                        builder.addFeedLine(1)
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

                        builder.addFeedLine(1)
                    }


                }
            }
        }


    }


    return builder
}


fun addOrdersForStarKitchen(
    list: List<CreateOrderResponse.Data.Order.OrderItem>,
    printerCat: ArrayList<PrinterResponse.Data.PrinterCategories?>? = null
): String {

    var items: String = ""
    if (list.isNullOrEmpty()){
        return items
    }
    for (i in 0 until list.size) {
        printerCat?.forEach {
            Log.e("PrinterReceipt", "checkPrinterItemN:   ${list.get(i).itemName}")
            if (it?.id == list[i].categoryId) {
                if (it.categoryActive && it.printerEnable) {

                    val obj = list.get(i)
                    if (obj.isEdited){
                        items += "(U) "+obj.quantity.toString() + " " + obj.itemName.uppercase()
                    }else{
                        items += obj.quantity.toString() + " " + obj.itemName.uppercase()
                    }

                    items += "\n"
                    if (obj.orderItemModifiers.isNotEmpty()) {
                        for (j in 0 until obj.orderItemModifiers.size) {
                            val modifierObj = obj.orderItemModifiers.get(j)
                            items += " "
                            items += " " + if (modifierObj.modifierQuantity == 1) {
                                " "
                            } else {
                                "" + modifierObj.modifierQuantity + "x "
                            } + modifierObj.name.uppercase()

                            items += "\n"
                        }
                    }

                    if (obj.note.isNotEmpty()) {
//                        builder.addTextLineSpace(30)
                        items += " "
                        items += "  Note:${obj.note}"
                        items += "\n"
                    }


                }
            }
        }


    }


    return items
}

fun addReprintOrdersForStarKitchen(
    list: List<OnlineOrderResponseModel.Data.OrderItem>,
    printerCat: ArrayList<PrinterResponse.Data.PrinterCategories?>? = null
): String {

    var items: String = ""
    if (list.isNullOrEmpty()){
        return items
    }
    for (i in 0 until list.size) {
        printerCat?.forEach {
            Log.e("PrinterReceipt", "checkPrinterItemN:   ${list.get(i).itemName}")
            if (it?.id == list[i].categoryId) {
                if (it.categoryActive && it.printerEnable) {

                    val obj = list.get(i)
                    items += obj.quantity.toString() + " " + obj.itemName.uppercase()
                    items += "\n"
                    if (obj.orderItemModifiers.isNotEmpty()) {
                        for (j in 0 until obj.orderItemModifiers.size) {
                            val modifierObj = obj.orderItemModifiers.get(j)
                            items += " "
                            items += " " + if (modifierObj.modifier_quantity == 1) {
                                " "
                            } else {
                                "" + modifierObj.modifier_quantity + "x "
                            } + modifierObj.name.uppercase()

                            items += "\n"
                        }
                    }

                    if (obj.note.isNotEmpty()) {
//                        builder.addTextLineSpace(30)
                        items += " "
                        items += "  Note:${obj.note}"
                        items += "\n"
                    }


                }
            }
        }


    }


    return items
}


fun addOrdersForKitchenU220(
    builder: Printer,
    list: List<CreateOrderResponse.Data.Order.OrderItem>,
    fontSizeH: Int = 1,
    fontSizeW: Int = 1,
    printerCat: ArrayList<PrinterResponse.Data.PrinterCategories?>? = null,
    guestAttributes: ArrayList<CreateOrderResponse.Data.Order.GuestAttributes>? = null
): Printer {
    for (i in 0 until list.size) {
        printerCat?.forEach {
            if (it?.id == list[i].categoryId) {
                if (it.categoryActive && it.printerEnable) {

                    val obj = list.get(i)
                    builder.addFeedLine(1)
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

                    if (obj.isEdited){
                        builder.addText("(U) "+obj.quantity.toString() + " " + obj.itemName.uppercase())
                    }else{
                        builder.addText(obj.quantity.toString() + " " + obj.itemName.uppercase())
                    }

                    if (obj.orderItemModifiers.isNotEmpty()) {
                        for (j in 0 until obj.orderItemModifiers.size) {
                            val modifierObj = obj.orderItemModifiers.get(j)
                            builder.addFeedLine(1)
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


                            builder.addText(
                                "  " + if (modifierObj.modifierQuantity == 1) {
                                    "   "
                                } else {
                                    "" + modifierObj.modifierQuantity + "x "
                                } + modifierObj.name.uppercase()
                            )


                        }
                    }
                    if (obj.note.isNotEmpty()) {
                        builder.addFeedLine(1)
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
            }
        }


    }


    return builder
}


fun addOrdersForKitchen(
    list: List<CreateOrderResponse.Data.Order.OrderItem>,
    printerCat: ArrayList<PrinterResponse.Data.PrinterCategories>? = null
) {
    for (i in 0 until list.size) {

        printerCat?.forEach {
            if (it.id == list[i].categoryId && it.categoryActive && it.printerEnable) {

                val obj = list.get(i)


                if (obj.isEdited){
                    PrintSunmiUtils.orderTime("(U)"+obj.quantity.toString() + " " + obj.itemName.uppercase())
                }
                else{
                    PrintSunmiUtils.orderTime(obj.quantity.toString() + " " + obj.itemName.uppercase())
                }

                if (obj.orderItemModifiers.isNotEmpty()) {
                    for (j in 0 until obj.orderItemModifiers.size) {
                        val modifierObj = obj.orderItemModifiers.get(j)

                        PrintSunmiUtils.orderTime(
                            if (modifierObj.modifierQuantity == 1) {
                                "      " + modifierObj.name.uppercase()
                            } else {
                                "  " + modifierObj.modifierQuantity + "x  " + modifierObj.name.uppercase()
                            }
                        )


                    }
                }
                if (obj.note.isNotEmpty()) {
                    PrintSunmiUtils.orderTime("  Note:" + obj.note)
                }

                SunmiPrinterApi.getInstance().lineWrap(1)

            }
        }
    }

}

fun addOrdersForKitchenTransition(
    builder: Builder,
    list: List<GetOrderDetailsResponse.Data.OrderItem>,
    fontSizeH: Int = 1,
    fontSizeW: Int = 1,
    printerCat: ArrayList<PrinterResponse.Data.PrinterCategories?>? = null,
    guestAttributes: ArrayList<CreateOrderResponse.Data.Order.GuestAttributes>? = null
): Builder {
    for (i in 0 until list.size) {
        printerCat?.forEach {
            Log.e("PrinterReceipt", "checkPrinterItemN:   ${list.get(i).itemName}")
            if (it?.id == list[i].categoryId) {
                if (it.categoryActive && it.printerEnable) {
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

                    builder.addText(obj.quantity.toString() + " " + obj.itemName.uppercase())
                    builder.addFeedLine(1)
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
                            builder.addText(
                                "  " + if (modifierObj.modifier_quantity == 1) {
                                    "   "
                                } else {
                                    "" + modifierObj.modifier_quantity + "x "
                                } + modifierObj.name.uppercase()
                            )
                            builder.addFeedLine(1)
                        }
                        builder.addFeedLine(1)
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
                        builder.addFeedLine(1)
                    }
                }
            }
        }
    }
    return builder
}

fun addOrdersForKitchenTransition(
    list: List<GetOrderDetailsResponse.Data.OrderItem>,
    printerCat: ArrayList<PrinterResponse.Data.PrinterCategories>? = null
) {
    for (i in 0 until list.size) {
        printerCat?.forEach {
            if (it.id == list[i].categoryId && it.categoryActive && it.printerEnable) {
                val obj = list.get(i)
                PrintSunmiUtils.orderTime(obj.quantity.toString() + " " + obj.itemName.uppercase())
                if (obj.orderItemModifiers.isNotEmpty()) {
                    for (j in 0 until obj.orderItemModifiers.size) {
                        val modifierObj = obj.orderItemModifiers.get(j)

                        PrintSunmiUtils.orderTime(
                            if (modifierObj.modifier_quantity == 1) {
                                "      " + modifierObj.name.uppercase()
                            } else {
                                "  " + modifierObj.modifier_quantity + "x  " + modifierObj.name.uppercase()
                            }
                        )
                    }
                }
                if (obj.note.isNotEmpty()) {
                    PrintSunmiUtils.orderTime("  Note:" + obj.note)
                }
                SunmiPrinterApi.getInstance().lineWrap(1)
            }
        }
    }
}

fun addOrdersForKitchenTransitionU220(
    builder: Printer,
    list: List<GetOrderDetailsResponse.Data.OrderItem>,
    fontSizeH: Int = 1,
    fontSizeW: Int = 1,
    printerCat: ArrayList<PrinterResponse.Data.PrinterCategories?>? = null,
    guestAttributes: ArrayList<CreateOrderResponse.Data.Order.GuestAttributes>? = null
): Printer {
    for (i in 0 until list.size) {
        printerCat?.forEach {
            if (it?.id == list[i].categoryId) {
                if (it.categoryActive && it.printerEnable) {
                    val obj = list.get(i)
                    builder.addFeedLine(1)
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
                    builder.addText(obj.quantity.toString() + " " + obj.itemName.uppercase())

                    if (obj.orderItemModifiers.isNotEmpty()) {
                        for (j in 0 until obj.orderItemModifiers.size) {
                            val modifierObj = obj.orderItemModifiers.get(j)
                            builder.addFeedLine(1)
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
                            builder.addText(
                                "  " + if (modifierObj.modifier_quantity == 1) {
                                    "   "
                                } else {
                                    "" + modifierObj.modifier_quantity + "x "
                                } + modifierObj.name.uppercase()
                            )
                        }
                    }
                    if (obj.note.isNotEmpty()) {
                        builder.addFeedLine(1)
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
            }
        }
    }
    return builder
}

fun addOrdersForKitchenTransitionInner(
    list: List<GetOrderDetailsResponse.Data.OrderItem>,
    printerCat: ArrayList<PrinterResponse.Data.PrinterCategories>? = null
) {
    for (i in 0 until list.size) {

        printerCat?.forEach {
            if (it.id == list[i].categoryId && it.categoryActive && it.printerEnable) {

                val obj = list.get(i)

                PrintSunmiUtils.normalTextLarge(obj.quantity.toString() + " " + obj.itemName.uppercase())

                if (obj.orderItemModifiers.isNotEmpty()) {
                    for (j in 0 until obj.orderItemModifiers.size) {
                        val modifierObj = obj.orderItemModifiers.get(j)
                        PrintSunmiUtils.normalTextLarge(
                            if (modifierObj.modifier_quantity == 1) {
                                "     " + modifierObj.name.uppercase()
                            } else {
                                "  " + modifierObj.modifier_quantity + "x " + modifierObj.name.uppercase()
                            }
                        )

                    }
                }
                if (obj.note.isNotEmpty()) {
                    PrintSunmiUtils.normalTextLarge("  Note:" + obj.note)
                }

                SunmiPrintHelper.getInstance().lineWrap(1)
            }
        }
    }

}

fun addOrdersForKitchenInner(
    list: List<CreateOrderResponse.Data.Order.OrderItem>,
    printerCat: ArrayList<PrinterResponse.Data.PrinterCategories>? = null
) {
    for (i in 0 until list.size) {

        printerCat?.forEach {
            if (it.id == list[i].categoryId && it.categoryActive && it.printerEnable) {

                val obj = list.get(i)

                if (obj.isEdited){
                    PrintSunmiUtils.normalTextLarge("(U)"+obj.quantity.toString() + " " + obj.itemName.uppercase())
                }else{
                    PrintSunmiUtils.normalTextLarge(obj.quantity.toString() + " " + obj.itemName.uppercase())
                }

//                PrintSunmiUtils.normalTextLarge(obj.quantity.toString() + " " + obj.itemName.uppercase())

                if (obj.orderItemModifiers.isNotEmpty()) {
                    for (j in 0 until obj.orderItemModifiers.size) {
                        val modifierObj = obj.orderItemModifiers.get(j)
                        PrintSunmiUtils.normalTextLarge(
                            if (modifierObj.modifierQuantity == 1) {
                                "     " + modifierObj.name.uppercase()
                            } else {
                                "  " + modifierObj.modifierQuantity + "x " + modifierObj.name.uppercase()
                            }
                        )

                    }
                }
                if (obj.note.isNotEmpty()) {
                    PrintSunmiUtils.normalTextLarge("  Note:" + obj.note)
                }

                SunmiPrintHelper.getInstance().lineWrap(1)
            }
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
                obj.quantity.toString() + "  " + getItemNameToShow(obj.itemName),
                getItemPriceToShow(totalPriceOpenOrder(obj)),
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
                // builder.addTextPosition(4)

                builder.addText(
                    padLineCustomerItem(
                        "   " + if (modifierObj.modifier_quantity == 1) {
                            "   "
                        } else {
                            "" + modifierObj.modifier_quantity + "x "
                        } + getItemNameToShow(modifierObj.name),
                        getModifierItemPriceToShow(modifierObj.price, modifierObj.quantity),
                        if (font == Constants.LARGE) {
                            23
                        } else {
                            48
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

/**
 * This method is created by Dharmesh Basapati.
 * It is created for printing order items of an open order from all orders screen in TM-m30 Printer.
 * Note: As we are getting the response model of an online order in "all orders" api,
 * we are using OnlineOrderResponseModel for printing open order customer receipts.
 */
fun addOrderItemOnlineOrder(
    builder: Builder,
    list: List<OnlineOrderResponseModel.Data.OrderItem>,
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
                obj.quantity.toString() + "  " + getItemNameToShow(obj.itemName),
                getItemPriceToShow(totalPriceOnlineOrder(obj)),
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
                // builder.addTextPosition(4)

                builder.addText(
                    padLineCustomerItem(
                        "   " + if (modifierObj.modifier_quantity == 1) {
                            "   "
                        } else {
                            "" + modifierObj.modifier_quantity + "x "
                        } + getItemNameToShow(modifierObj.name),
                        getModifierItemPriceToShow(modifierObj.price, modifierObj.quantity),
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

fun totalPriceOnlineOrder(model: OnlineOrderResponseModel.Data.OrderItem): Double {
    return model.price * model.quantity
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
                obj.quantity.toString() + "  " + getItemNameToShow(obj.itemName),
                getItemPriceToShow(totalPriceOpenOrder(obj)),
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
                var part1 = "   " + if (modifierObj.modifier_quantity == 1) {
                    "   "
                } else {
                    "" + modifierObj.modifier_quantity + "x "
                } + getItemNameToShow(modifierObj.name)
                var part2 =
                    getModifierItemPriceToShow(modifierObj.price, modifierObj.quantity)

                Log.e("CheckPartFM", "part1 ${part1.length}")
                Log.e("CheckPartFM", "part2 ${part2.length}")

                PrintSunmiUtils.orderTime(
                    padLineCustomerItem(
                        part1,
                        part2,
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

/**
 * This method is created by Dharmesh Basapati.
 * It is created for printing order items of an open order from all orders screen in Sunmi Cloud Printer.
 * Note: As we are getting the response model of an online order in "all orders" api,
 * we are using OnlineOrderResponseModel for printing open order customer receipts.
 */
fun addOrderItemOnlineOrderSunmi(
    list: List<OnlineOrderResponseModel.Data.OrderItem>,
    font: String,
    showModifiers: Boolean
) {
    for (i in 0 until list.size) {
        val obj = list.get(i)

        PrintSunmiUtils.orderTime(
            padLineCustomerItem(
                obj.quantity.toString() + "  " + getItemNameToShow(obj.itemName),
                getItemPriceToShow(totalPriceOnlineOrder(obj)),
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
                var part1 = "   " + if (modifierObj.modifier_quantity == 1) {
                    "   "
                } else {
                    "" + modifierObj.modifier_quantity + "x "
                } + getItemNameToShow(modifierObj.name)
                var part2 =
                    getModifierItemPriceToShow(modifierObj.price, modifierObj.quantity)

                Log.e("CheckPartFM", "part1 ${part1.length}")
                Log.e("CheckPartFM", "part2 ${part2.length}")

                PrintSunmiUtils.orderTime(
                    padLineCustomerItem(
                        part1,
                        part2,
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

fun addOrderItemOpenOrderSunmiInner(
    list: List<OpenOrderResponse.Data.Order.OrderItem>,
    font: String,
    showModifiers: Boolean
) {
    for (i in 0 until list.size) {
        val obj = list.get(i)

        PrintSunmiUtils.normalText(
            padLineCustomerItem(
                obj.quantity.toString() + "  " + getItemNameToShow(obj.itemName),
                getItemPriceToShow(totalPriceOpenOrder(obj)),
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

                var part1 = "   " + if (modifierObj.modifier_quantity == 1) {
                    "   "
                } else {
                    "" + modifierObj.modifier_quantity + "x "
                } + getItemNameToShow(modifierObj.name)

                var part2 =
                    getModifierItemPriceToShow(modifierObj.price, modifierObj.quantity)
                Log.e("CheckPartF", "part1 ${part1.length}")
                Log.e("CheckPartF", "part2 ${part2.length}")

                PrintSunmiUtils.normalText(
                    padLineCustomerItem(
                        part1,
                        part2,
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

            PrintSunmiUtils.normalText("   Note: " + obj.note)

        }
    }


}

/**
 * This method is created by Dharmesh Basapati.
 * It is created for printing order items of an open order from all orders screen in Inner Printer.
 * Note: As we are getting the response model of an online order in "all orders" api,
 * we are using OnlineOrderResponseModel for printing open order customer receipts.
 */
fun addOrderItemOnlineOrderSunmiInner(
    list: List<OnlineOrderResponseModel.Data.OrderItem>,
    font: String,
    showModifiers: Boolean
) {
    for (i in 0 until list.size) {
        val obj = list.get(i)

        PrintSunmiUtils.normalText(
            padLineCustomerItem(
                obj.quantity.toString() + " " + getItemNameToShow(obj.itemName),
                getItemPriceToShow(totalPriceOnlineOrder(obj)),
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

                var part1 = "   " + if (modifierObj.modifier_quantity == 1) {
                    "   "
                } else {
                    "" + modifierObj.modifier_quantity + "x "
                } + getItemNameToShow(modifierObj.name)

                var part2 =
                    getModifierItemPriceToShow(modifierObj.price, modifierObj.quantity)
                Log.e("CheckPartF", "part1 ${part1.length}")
                Log.e("CheckPartF", "part2 ${part2.length}")

                PrintSunmiUtils.normalText(
                    padLineCustomerItem(
                        part1,
                        part2,
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

            PrintSunmiUtils.normalText("   Note: " + obj.note)

        }
    }


}

fun addWholeTbItemToGuest(
    builder: Builder,
    list: TbCartItem,
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

    if (obj.modifiers.isNotEmpty() && showModifiers) {
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
                LogUtil.logE("itemTaxPrice", "" + itemTaxPrice)
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

    val price = (subTotal) / guestCount

    var priceToShow = ""
    if (price > 0.0) {
        priceToShow = MethodUtils.roundOffAmount(price)
    }

    //var finalAmt = MethodUtils.roundOffAmount((subTotal) / guestCount)

    builder.addText(
        padLineCustomerItem(
            obj.itemQuantity.toString() + "  " + getItemNameToShow(obj.name),
            "" + priceToShow,
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
    list: TbCartItem,
    font: String,
    showModifiers: Boolean,
    guestCount: Int,
    serviceChargeList: ArrayList<TbServiceCharge>
) {

    val obj = list

    var subTotal = (obj.price * obj.itemQuantity).toDouble()

    if (obj.modifiers.isNotEmpty() && showModifiers) {
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
                LogUtil.logE("itemTaxPrice", "" + itemTaxPrice)
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

    val price = (subTotal) / guestCount

    var priceToShow = ""
    if (price > 0.0) {
        priceToShow = MethodUtils.roundOffAmount(price)
    }

    //val finalAmt = MethodUtils.roundOffAmount((subTotal) / guestCount)

    PrintSunmiUtils.orderTime(
        padLineCustomerItem(
            obj.itemQuantity.toString() + "  " + getItemNameToShow(obj.name),
            "" + priceToShow,
            if (font == Constants.LARGE) 23 else 48
        ).toString()
    )

}


fun addWholeTbItemToGuestInner(
    list: TbCartItem,
    font: String,
    showModifiers: Boolean,
    guestCount: Int,
    serviceChargeList: ArrayList<TbServiceCharge>
) {

    val obj = list

    var subTotal = (obj.price * obj.itemQuantity).toDouble()

    if (obj.modifiers.isNotEmpty() && showModifiers) {
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
                LogUtil.logE("itemTaxPrice", "" + itemTaxPrice)
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

    val price = (subTotal) / guestCount

    var priceToShow = ""
    if (price > 0.0) {
        priceToShow = MethodUtils.roundOffAmount(price)
    }

    //val finalAmt = MethodUtils.roundOffAmount((subTotal) / guestCount)

    PrintSunmiUtils.normalText(
        padLineCustomerItem(
            obj.itemQuantity.toString() + "  " + getItemNameToShow(obj.name),
            "" + priceToShow,
            if (font == Constants.LARGE) 23 else 48
        ).toString()
    )

    if (obj.modifiers.isNotEmpty()) {

        obj.modifiers.forEach {
            PrintSunmiUtils.normalText(
                padLineCustomerItem(
                    if (it.modifier_quantity == 1) {
                        "      " + it.name
                    } else {
                        "   " + it.modifier_quantity + "x " + it.name
                    },
                    "" + MethodUtils.roundOffAmount(it.price * it.itemQuantity),
                    if (font == Constants.LARGE) 23 else 48
                ).toString()
            )
        }


    }

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
    list: TbCartItem,
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
            obj.itemQuantity.toString() + "  " + getItemNameToShow(obj.name),
            getItemPriceToShow(totalPriceDineInItem(obj)),
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
            //builder.addTextPosition(4)
            builder.addText(
                padLineCustomerItem(
                    "   " + getItemNameToShow(modifierObj.name),
                    getModifierItemPriceToShow(modifierObj.price, modifierObj.itemQuantity),
                    if (font == Constants.LARGE) {
                        23
                    } else {
                        48
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
    list: TbCartItem,
    font: String,
    showModifiers: Boolean
) {


    val obj = list


    PrintSunmiUtils.orderTime(
        padLineCustomerItem(
            obj.itemQuantity.toString() + "  " + getItemNameToShow(obj.name),
            getItemPriceToShow(totalPriceDineInItem(obj)),
            if (font == Constants.LARGE) 23 else 48
        ).toString()
    )



    if (obj.modifiers.isNotEmpty() && showModifiers) {
        for (j in 0 until obj.modifiers.size) {
            val modifierObj = obj.modifiers.get(j)


            PrintSunmiUtils.orderTime(
                padLineCustomerItem(
                    if (modifierObj.modifier_quantity == 1) {
                        "     " + getItemNameToShow(modifierObj.name)
                    } else {
                        "  " + modifierObj.modifier_quantity + "x " + getItemNameToShow(modifierObj.name)
                    },
                    getModifierItemPriceToShow(modifierObj.price, modifierObj.itemQuantity),
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
    list: TbCartItem,
    font: String,
    showModifiers: Boolean
) {


    val obj = list


    PrintSunmiUtils.normalTextDineInItem(
        padLineCustomerItem(
            obj.itemQuantity.toString() + "  " + getItemNameToShow(obj.name),
            getItemPriceToShow(totalPriceDineInItem(obj)),
            if (font == Constants.LARGE) 23 else 48
        ).toString()
    )



    if (obj.modifiers.isNotEmpty() && showModifiers) {
        for (j in 0 until obj.modifiers.size) {
            val modifierObj = obj.modifiers.get(j)


            PrintSunmiUtils.normalText(
                padLineCustomerItem(
                    if (modifierObj.modifier_quantity == 1) {
                        "     " + getItemNameToShow(modifierObj.name)
                    } else {
                        "  " + modifierObj.modifier_quantity + "x " + getItemNameToShow(modifierObj.name)
                    },
                    getModifierItemPriceToShow(modifierObj.price, modifierObj.itemQuantity),
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
                obj.quantity.toString() + "  " + getItemNameToShow(obj.itemName),
                getItemPriceToShow(totalPrice(obj)),
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


                builder.addText(
                    padLineCustomerItem(
                        if (modifierObj.modifierQuantity == 1) {
                            "     "
                        } else {
                            "   " + modifierObj.modifierQuantity.toString() + "x"
                        } + "  " + getItemNameToShow(modifierObj.name),
                        getModifierItemPriceToShow(modifierObj.price, modifierObj.quantity),
                        if (font == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )


                /*builder.addText(
                    padLineCustomerItem(
                         modifierObj.quantity.toString() + " " + "    " + modifierObj.name + " x" + modifierObj.modifierQuantity ,
                        "$" + MethodUtils.roundOffAmountString(modifierObj.price.toDouble() * modifierObj.quantity),
                        if (font == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
*/

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


        Log.e(TAG, "quantity:  ${obj.quantity}")
        val item = padLineCustomerItem(
            obj.quantity.toString() + "  " + getItemNameToShow(obj.itemName),
            getItemPriceToShow(totalPrice(obj)),
            if (font == Constants.LARGE) 23 else 48
        )

        PrintSunmiUtils.orderTime(item.toString())



        if (obj.orderItemModifiers.isNotEmpty() && showModifiers) {
            for (j in 0 until obj.orderItemModifiers.size) {
                val modifierObj = obj.orderItemModifiers.get(j)

                val modifier = padLineCustomerItem(
                    if (modifierObj.modifierQuantity == 1) {
                        "     "
                    } else {
                        "   " + modifierObj.modifierQuantity.toString() + "x"
                    } + "  " + getItemNameToShow(modifierObj.name),
                    getModifierItemPriceToShow(modifierObj.price, modifierObj.quantity),
                    if (font == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
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
            obj.quantity.toString() + "  " + getItemNameToShow(obj.itemName),
            getItemPriceToShow(totalPrice(obj)),
            if (font == Constants.LARGE) 23 else 48
        )

        PrintSunmiUtils.normalText(item.toString())



        if (obj.orderItemModifiers.isNotEmpty() && showModifiers) {
            for (j in 0 until obj.orderItemModifiers.size) {
                val modifierObj = obj.orderItemModifiers.get(j)

                val modifier = padLineCustomerItem(
                    /*if (modifierObj.modifierQuantity == 1) {
                        "     "
                    } else {
                        "   " + modifierObj.modifierQuantity.toString() + "x"
                    }*/"   " + modifierObj.modifierQuantity.toString() + "x" + "  " + getItemNameToShow(
                        modifierObj.name
                    ),
                    getModifierItemPriceToShow(modifierObj.price, modifierObj.quantity),
                    if (font == Constants.LARGE) {
                        22
                    } else {
                        48
                    }
                )

                /*val modifier = padLineCustomerItem(
                    "   " + modifierObj.name,
                    "$" + MethodUtils.roundOffAmountString(modifierObj.price.toDouble() * modifierObj.quantity),
                    if (font == Constants.LARGE) 22 else 48
                )
*/
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
                obj.quantity.toString() + "  " + getItemNameToShow(obj.itemName),
                getItemPriceToShow(totalPriceTransaction(obj)),
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
                //builder.addTextPosition(4)
                var part1 = if (modifierObj.modifier_quantity == 1) {
                    "       " + getItemNameToShow(modifierObj.name)
                } else {
                    "   " + modifierObj.modifier_quantity + "x" + "  " + getItemNameToShow(
                        modifierObj.name
                    )
                }
                builder.addText(
                    padLineCustomerItem(
                        part1,
                        getModifierItemPriceToShow(modifierObj.price, modifierObj.quantity),
                        if (font == Constants.LARGE) {
                            23
                        } else {
                            48
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


fun addOrderItemsTransaction(
    list: List<GetOrderDetailsResponse.Data.OrderItem>,
    font: String,
    showModifiers: Boolean
) {
    for (i in 0 until list.size) {
        val obj = list.get(i)

        val item = padLineCustomerItem(
            obj.quantity.toString() + "  " + getItemNameToShow(obj.itemName),
            getItemPriceToShow(totalPriceTransaction(obj)),
            if (font == Constants.LARGE) 23 else 48
        )

        PrintSunmiUtils.orderTime(item.toString())



        if (obj.orderItemModifiers.isNotEmpty() && showModifiers) {
            for (j in 0 until obj.orderItemModifiers.size) {
                val modifierObj = obj.orderItemModifiers.get(j)


                var part1 =
                    if (modifierObj.modifier_quantity == 1) {
                        "       " + getItemNameToShow(modifierObj.name)
                    } else {
                        "   " + modifierObj.modifier_quantity.toString() + "x" + "  " + getItemNameToShow(
                            modifierObj.name
                        )

                    }
                val modifier = padLineCustomerItem(
                    part1,
                    getModifierItemPriceToShow(modifierObj.price, modifierObj.quantity),
                    if (font == Constants.LARGE) 23 else 48
                )
                PrintSunmiUtils.orderTime(modifier.toString())


            }

        }

        if (obj.note.isNotEmpty()) {
            PrintSunmiUtils.orderTime("   Note: " + obj.note)
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
            obj.quantity.toString() + "  " + getItemNameToShow(obj.itemName),
            getItemPriceToShow(totalPriceTransaction(obj)),
            if (font == Constants.LARGE) 23 else 48
        )

        PrintSunmiUtils.normalText(item.toString())



        if (obj.orderItemModifiers.isNotEmpty() && showModifiers) {
            for (j in 0 until obj.orderItemModifiers.size) {
                val modifierObj = obj.orderItemModifiers.get(j)

                /*   var part1 = if (modifierObj.modifier_quantity == 1) {
                       "       " + getItemNameToShow(modifierObj.name)
                   } else {
                       "   " + modifierObj.modifier_quantity.toString() + "x" + "  " + getItemNameToShow(
                           modifierObj.name
                       )
                   }*/

                var part1 =
                    "   " + modifierObj.modifier_quantity.toString() + "x" + "  " + getItemNameToShow(
                        modifierObj.name
                    )


                val modifier = padLineCustomerItem(
                    part1,
                    getModifierItemPriceToShow(modifierObj.price, modifierObj.quantity),
                    if (font == Constants.LARGE) 23 else 48
                )
                PrintSunmiUtils.normalText(modifier.toString())


            }

        }
        if (obj.note.isNotEmpty()) {
            PrintSunmiUtils.normalText("   Note: " + obj.note)
        }
    }
}

private fun totalPriceOpenOrder(model: OpenOrderResponse.Data.Order.OrderItem): Double {
    return model.price * model.quantity

}

private fun totalPriceDineInItem(model: TbCartItem): Double {
    return model.price * model.itemQuantity
}

private fun totalPrice(model: CreateOrderResponse.Data.Order.OrderItem): Double {
    return model.price * model.quantity
}

private fun totalPriceTransaction(model: GetOrderDetailsResponse.Data.OrderItem): Double {

    return model.price * model.quantity
}


fun calculateTipAmt(percentage: Double, price: Double): Double {

    return MethodUtils.roundOffAmountDouble((price * percentage) / 100)
}

fun getItemPriceToShow(price: Double): String {
    var priceToShow = ""
    if (price > 0.0) {
        priceToShow = "$" + MethodUtils.roundOffAmountString(price)
    }
    return priceToShow
}

fun getModifierItemPriceToShow(modifierPrice: Double, modifierQty: Int): String {
    var modPriceToShow = ""
    if (modifierPrice > 0.0) {
        modPriceToShow = "$" + MethodUtils.roundOffAmountString(modifierPrice * modifierQty)
    }
    return modPriceToShow
}

fun getItemNameToShow(itemName: String): String {
//    Commented below code to hide 15 char limit for item/modifier names to prevent receipt disruption with big names.
    var updatedItemName = itemName
    if (updatedItemName.length > 15) {
        updatedItemName = updatedItemName.substring(0, 15) + "..."
    }
    return updatedItemName
}