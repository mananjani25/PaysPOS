package com.android.pos.utils

import android.graphics.Bitmap
import com.android.pos.data.model.responseModel.GetTipReponse
import com.android.pos.data.remote.Constants
import com.sunmi.externalprinterlibrary.api.SunmiPrinterApi


class PrintSunmiUtils {


    companion object {

        var fontSize = ""

        fun printLogo(newBitmap: Bitmap?) {

            SunmiPrinterApi.getInstance().printerInit()
            SunmiPrinterApi.getInstance().setAlignMode(1)
            if (newBitmap != null) {
                SunmiPrinterApi.getInstance().printBitmap(newBitmap, 0)
            }
            SunmiPrinterApi.getInstance().lineWrap(1)
        }

        fun addLable(value: String) {

            SunmiPrinterApi.getInstance().setAlignMode(1)
            SunmiPrinterApi.getInstance().enableBold(true)
            SunmiPrinterApi.getInstance().setFontZoom(1, 2)
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(2)
            addHorizontal()

        }

        fun printBusinessDetails(value: String, value1: String, value2: String) {

            SunmiPrinterApi.getInstance().setAlignMode(1)
            SunmiPrinterApi.getInstance().enableBold(true)
            SunmiPrinterApi.getInstance().setFontZoom(2, 2)
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)

            SunmiPrinterApi.getInstance().setAlignMode(1)
            SunmiPrinterApi.getInstance().enableBold(false)
            setFontSize()
            SunmiPrinterApi.getInstance().printText(value1)
            SunmiPrinterApi.getInstance().lineWrap(1)

            SunmiPrinterApi.getInstance().setAlignMode(1)
            SunmiPrinterApi.getInstance().enableBold(false)
            setFontSize()
            SunmiPrinterApi.getInstance().printText(value2)
            SunmiPrinterApi.getInstance().lineWrap(1)


        }


        fun venueWebsite(value: String) {
            SunmiPrinterApi.getInstance().setAlignMode(1)
            SunmiPrinterApi.getInstance().enableBold(false)
            setFontSize()
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)
        }

        fun paymentType(value: String) {
            SunmiPrinterApi.getInstance().setAlignMode(1)
            SunmiPrinterApi.getInstance().enableBold(false)
            SunmiPrinterApi.getInstance().setFontZoom(2, 2)
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)
        }

        fun orderId(value: String) {
            SunmiPrinterApi.getInstance().setAlignMode(0)
            SunmiPrinterApi.getInstance().enableBold(false)
            setFontSize()
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)
        }

        fun orderTime(value: String) {
            SunmiPrinterApi.getInstance().setAlignMode(0)
            SunmiPrinterApi.getInstance().enableBold(false)
            setFontSize()
            SunmiPrinterApi.getInstance().printText(
                value
            )
            SunmiPrinterApi.getInstance().lineWrap(1)

        }

        fun addValue(value: String) {
            SunmiPrinterApi.getInstance().setAlignMode(1)
            SunmiPrinterApi.getInstance().enableBold(false)
             setFontSize()
            SunmiPrinterApi.getInstance().printText(
                value
            )
            SunmiPrinterApi.getInstance().lineWrap(1)

        }

        fun employee(value: String) {
            SunmiPrinterApi.getInstance().setAlignMode(0)
            SunmiPrinterApi.getInstance().enableBold(false)
            setFontSize()
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)
        }

        fun receiptID(value: String) {
            SunmiPrinterApi.getInstance().setAlignMode(0)
            SunmiPrinterApi.getInstance().enableBold(false)
            setFontSize()
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)
        }

        fun printOrderType(value: String) {

            SunmiPrinterApi.getInstance().setAlignMode(1)
            SunmiPrinterApi.getInstance().enableBold(true)
            SunmiPrinterApi.getInstance().setFontZoom(2, 2)
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)

        }

        fun addHorizontal() {



            val st = addHorizontalKitchenLineSunmi(fontSize)
            SunmiPrinterApi.getInstance().enableUnderline(true)
            SunmiPrinterApi.getInstance().enableBold(false)
             setFontSize()
            SunmiPrinterApi.getInstance().printText(st)
            SunmiPrinterApi.getInstance().lineWrap(1)

        }

        fun totalDiscount(value: String) {
            SunmiPrinterApi.getInstance().enableBold(false)
             setFontSize()
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)

        }

        fun subTotal(value: String) {
            SunmiPrinterApi.getInstance().enableBold(false)
             setFontSize()
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)


        }

        fun tax(value: String) {
            SunmiPrinterApi.getInstance().enableBold(false)
             setFontSize()
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)

        }

        fun serviceCharge(value: String) {
            SunmiPrinterApi.getInstance().enableBold(false)
            setFontSize()
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)


        }

        fun tip(value: String) {
            SunmiPrinterApi.getInstance().enableBold(true)
            setFontSize()
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)

        }

        fun surCharge(value: String) {
            SunmiPrinterApi.getInstance().enableBold(true)
            setFontSize()
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)
        }

        fun cashDiscount(value: String) {
            SunmiPrinterApi.getInstance().enableBold(true)
            setFontSize()
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)
        }

        fun loyaltyAmount(value: String) {
            SunmiPrinterApi.getInstance().enableBold(true)
            setFontSize()
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)

        }

        fun loyaltyPoint(value: String) {
            SunmiPrinterApi.getInstance().enableBold(true)
            setFontSize()
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)
        }

        fun totalPrice(value: String) {

            SunmiPrinterApi.getInstance().enableBold(true)
            setFontSize()
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)

        }

        fun changeAmount(value: String) {

            SunmiPrinterApi.getInstance().enableBold(true)
            setFontSize()
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)

        }

        fun refundAmount(value: String) {

            SunmiPrinterApi.getInstance().enableBold(true)
            setFontSize()
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)

        }

        fun tips(value: String) {

            SunmiPrinterApi.getInstance().setAlignMode(0)
            SunmiPrinterApi.getInstance().enableBold(true)
            setFontSize()
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(2)
        }

        fun additionalTips() {

            SunmiPrinterApi.getInstance().setAlignMode(0)
            SunmiPrinterApi.getInstance().enableBold(true)
            setFontSize()
            SunmiPrinterApi.getInstance().printText("Additional Tips")
            SunmiPrinterApi.getInstance().lineWrap(1)
            addHorizontal()

        }

        fun addTipList(tipsList: List<GetTipReponse.Data>, totalAmount: Double, fonts: String) {

            addTipsList(
                tipsList,
                totalAmount,
                fonts
            )
        }

        fun transactionId(value: String) {

            SunmiPrinterApi.getInstance().setAlignMode(0)
            SunmiPrinterApi.getInstance().enableBold(true)
            setFontSize()
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)
        }

        fun transactionType(value: String) {

            SunmiPrinterApi.getInstance().enableBold(true)
            setFontSize()
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(2)

        }

        fun cardDetails(cardName: String, cardType: String, cardNumber: String) {

            for (i in 1..3) {

                SunmiPrinterApi.getInstance().setAlignMode(2)
                SunmiPrinterApi.getInstance().enableBold(true)
                setFontSize()
                when (i) {
                    1 -> {
                        SunmiPrinterApi.getInstance().printText(cardName)
                    }
                    2 -> {
                        SunmiPrinterApi.getInstance().printText(cardType)
                    }
                    3 -> {
                        SunmiPrinterApi.getInstance().printText(cardNumber)
                    }
                }

                SunmiPrinterApi.getInstance().lineWrap(1)
            }
        }

        fun customerDetails() {

            SunmiPrinterApi.getInstance().enableBold(true)
            setFontSize()
            SunmiPrinterApi.getInstance().printText("Customer Details")
            SunmiPrinterApi.getInstance().lineWrap(1)

            addHorizontal()

        }

        fun customerName(value: String) {

            SunmiPrinterApi.getInstance().setAlignMode(0)
            SunmiPrinterApi.getInstance().enableBold(false)
            setFontSize()
            SunmiPrinterApi.getInstance()
                .printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)
        }

        fun customerPhone(value: String) {
            SunmiPrinterApi.getInstance().setAlignMode(0)
            SunmiPrinterApi.getInstance().enableBold(false)
            setFontSize()
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)
        }

        fun customerAddress(value: String) {
            SunmiPrinterApi.getInstance().setAlignMode(0)
            SunmiPrinterApi.getInstance().enableBold(false)
            setFontSize()
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)
        }

        fun orderNote(value: String) {

            SunmiPrinterApi.getInstance().setAlignMode(1)
            SunmiPrinterApi.getInstance().enableBold(true)
            setFontSize()
            SunmiPrinterApi.getInstance().printText("Order Note")
            SunmiPrinterApi.getInstance().lineWrap(1)

            SunmiPrinterApi.getInstance().setAlignMode(1)
            SunmiPrinterApi.getInstance().enableBold(false)
            setFontSize()
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)
        }

        fun qrCode(value: String) {
            SunmiPrinterApi.getInstance().setAlignMode(1)
            value.let {
                SunmiPrinterApi.getInstance().printQrCode(
                    it, 6, 0
                )
            }
        }

        fun cutPaper() {

            SunmiPrinterApi.getInstance().lineWrap(5)
            SunmiPrinterApi.getInstance().cutPaper(1, 1)
        }

        fun printTextCenter(value: String) {

            SunmiPrinterApi.getInstance().setAlignMode(1)
            SunmiPrinterApi.getInstance().enableBold(false)
            setFontSize()
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)
        }

        fun deliveryType(value: String) {
            SunmiPrinterApi.getInstance().setAlignMode(1)
            SunmiPrinterApi.getInstance().enableBold(true)
            SunmiPrinterApi.getInstance().setFontZoom(2, 2)
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(2)

        }

        fun paidStatus(value: String) {
            SunmiPrinterApi.getInstance().setAlignMode(1)
            SunmiPrinterApi.getInstance().enableBold(true)
            SunmiPrinterApi.getInstance().setFontZoom(2, 2)
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)

        }

        fun fontSize(fonts: String) {
            this.fontSize = fonts
        }

        private fun setFontSize() {
            when (fontSize) {
                Constants.SMALL -> {
                    SunmiPrinterApi.getInstance().setFontZoom(1, 1)
                }
                Constants.MEDIUM -> {
                    SunmiPrinterApi.getInstance().setFontZoom(1, 2)
                }
                Constants.LARGE -> {
                    SunmiPrinterApi.getInstance().setFontZoom(2, 2)
                }
                else -> SunmiPrinterApi.getInstance().setFontZoom(1, 1)
            }
        }


    }


}

