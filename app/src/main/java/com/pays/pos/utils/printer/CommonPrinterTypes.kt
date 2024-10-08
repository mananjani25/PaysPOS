package com.pays.pos.utils.printer

import com.pays.pos.data.remote.Constants.BUSINESS_ADDRESS
import com.pays.pos.data.remote.Constants.BUSINESS_NAME
import com.pays.pos.data.remote.Constants.BUSINESS_PHONE_NO
import com.pays.pos.data.remote.Constants.ORDER_ID
import com.pays.pos.data.remote.Constants.ORDER_TYPE
import com.pays.pos.data.remote.EMPLOYEE_NAME
import com.pays.pos.data.remote.PRINT_GUEST_NAME
import com.pays.pos.data.remote.ORDER_TIME
import com.pays.pos.data.remote.PRINTER_ITEM_NOTE
import com.pays.pos.data.remote.PRINT_ADDITIONAL_TIPS_LABEL
import com.pays.pos.data.remote.PRINT_CARD_TYPE
import com.pays.pos.data.remote.PRINT_CASH_DISCOUNT
import com.pays.pos.data.remote.PRINT_CHANGE_AMOUNT
import com.pays.pos.data.remote.PRINT_CUSTOMER_SIGNATURE
import com.pays.pos.data.remote.PRINT_ITEM
import com.pays.pos.data.remote.PRINT_MASKED_CARD_NO
import com.pays.pos.data.remote.PRINT_MODIFIER
import com.pays.pos.data.remote.PRINT_ORDER_NOTE
import com.pays.pos.data.remote.PRINT_PAID_AMOUNT
import com.pays.pos.data.remote.PRINT_PAYMENT_HISTORY_LABEL
import com.pays.pos.data.remote.PRINT_QR_CODE
import com.pays.pos.data.remote.PRINT_REMAINING_AMOUNT
import com.pays.pos.data.remote.PRINT_SERVICE_CHARGES
import com.pays.pos.data.remote.PRINT_SINGLE_PAYMENT
import com.pays.pos.data.remote.PRINT_SUB_TOTAL
import com.pays.pos.data.remote.PRINT_SURCHARGE
import com.pays.pos.data.remote.PRINT_TAX
import com.pays.pos.data.remote.PRINT_TIME
import com.pays.pos.data.remote.PRINT_TIPS
import com.pays.pos.data.remote.PRINT_TIPS_CUSTOM
import com.pays.pos.data.remote.PRINT_TOTAL_CUSTOM
import com.pays.pos.data.remote.PRINT_TOTAL_DISCOUNT
import com.pays.pos.data.remote.PRINT_TOTAL_PRICE
import com.pays.pos.data.remote.PRINT_TRANSACTION_ID
import com.pays.pos.data.remote.PRINT_TRANSACTION_TYPE
import com.pays.pos.data.remote.RECEIPT_ID
import com.pays.pos.ui.fragments.settings.hardware.printer.SunmiPrintHelper
import com.pays.pos.utils.PrintSunmiUtils
import com.pays.pos.utils.landi.LPrint
import com.pays.pos.utils.landi.LPrint.FONT_SIZE_5X
import com.pays.pos.utils.landi.LPrint.SMALL_SIZE
import com.pays.pos.utils.landi.LPrint.lineBreak
import com.pays.pos.utils.landi.LPrint.printBoldLeft
import com.pays.pos.utils.landi.LPrint.printCenter
import com.pays.pos.utils.landi.LPrint.printDashedLineAndBreak
import com.pays.pos.utils.landi.LPrint.printLeft
import com.pays.pos.utils.landi.LPrint.printQRCode
import java.io.OutputStream


enum class CommonPrinterTypes {
    SunmiInnerPrinter,
    LandiInnerPrinter,
    SunmiCloudPrinter
}

class SunmiInnerPrinterPays() {

    var isOldSunmiVersion = false
    var _dataMap = mutableListOf<Pair<String,String>>()


    fun setDataMap(dataMap :MutableList<Pair<String,String>>) {
        this._dataMap = dataMap
    }

    fun isOldSunmiPrinterVersion(isOldSunmiVersion :Boolean = false) {
        this.isOldSunmiVersion = isOldSunmiVersion
    }

    fun initPrinter(font: String) {
        PrintSunmiUtils.fontSizeInner(font)
        SunmiPrintHelper.getInstance().initPrinter()
    }

    fun printOrderId() {
        _dataMap.find { it.first == ORDER_ID }.let { it?.second?.let { it1 ->
            PrintSunmiUtils.headerText(it1)
        } }
    }

    fun breakLine(){
        SunmiPrintHelper.getInstance().lineWrap(1)
    }

    fun printBusinessName() {
        _dataMap.find { it.first == BUSINESS_NAME }.let { it?.second?.let { it1 ->
            PrintSunmiUtils.headerText(it1)
        } }
    }

    fun printBusinessDetails() {
        _dataMap.let {
            val businessName = _dataMap.find { it.first == BUSINESS_NAME }?.second
            val businessAddress = _dataMap.find { it.first == BUSINESS_ADDRESS }?.second
            val businessPhone = _dataMap.find { it.first == BUSINESS_PHONE_NO }?.second

            if (businessName != null && businessAddress != null && businessPhone != null) {
                PrintSunmiUtils.printBusinessDetailsInner(businessName, businessAddress, businessPhone)
            }
        }
    }

    fun printOrderType() {
        _dataMap.find { it.first == ORDER_TYPE }.let { it?.second?.let { it1 ->
            PrintSunmiUtils.headerText(it1)
        } }
    }

    fun printReceiptId() {
        _dataMap.find { it.first == RECEIPT_ID }.let { it?.second?.let { it1 ->
            PrintSunmiUtils.printNormalText(isOldSunmiVersion, it1)
        } }
    }

    fun printEmployeeName() {
        _dataMap.find { it.first == EMPLOYEE_NAME }.let { it?.second?.let { it1 ->
            PrintSunmiUtils.printNormalText(isOldSunmiVersion, it1)
        } }
    }

    fun printOrderTime() {
        _dataMap.find { it.first == ORDER_TIME }.let { it?.second?.let { it1 ->
            PrintSunmiUtils.printNormalText(isOldSunmiVersion, it1)
        } }
    }

    fun printHorizontalLine() {
        PrintSunmiUtils.printHorizontalInnerNew(isOldSunmiVersion)
    }

    fun printTime() {
        _dataMap.find { it.first == PRINT_TIME }.let { it?.second?.let { it1 ->
            PrintSunmiUtils.printNormalText(isOldSunmiVersion, it1)
        } }
    }

    fun printGuest() {
        _dataMap.find { it.first == PRINT_GUEST_NAME }.let { it?.second?.let { it1 ->
            PrintSunmiUtils.printTextCenter(it1)
        } }
    }

    fun printGuest(guestName:String) {
        PrintSunmiUtils.printTextCenter(guestName)
    }

    fun printItem() {
        _dataMap.find { it.first == PRINT_ITEM }.let { it?.second?.let { it1 ->
            PrintSunmiUtils.printNormalText(isOldSunmiVersion,it1)
        } }
    }

    fun printItem(item:String) {
        PrintSunmiUtils.printNormalText(isOldSunmiVersion,item)
    }

    fun printModifier() {
        _dataMap.find { it.first == PRINT_MODIFIER }.let { it?.second?.let { it1 ->
            PrintSunmiUtils.printNormalText(isOldSunmiVersion,it1)
        } }
    }

    fun printModifier(modifiers: String) {
        PrintSunmiUtils.printNormalText(isOldSunmiVersion,modifiers)
    }

    fun printItemNote() {
        _dataMap.find { it.first == PRINTER_ITEM_NOTE }.let { it?.second?.let { it1 ->
            PrintSunmiUtils.printNormalText(isOldSunmiVersion,it1)
        } }
    }

    fun printItemNote(itemNote:String) {
            PrintSunmiUtils.printNormalText(isOldSunmiVersion,itemNote)
    }

    fun printTotalDiscount() {
        _dataMap.find { it.first == PRINT_TOTAL_DISCOUNT }.let { it?.second?.let { it1 ->
            PrintSunmiUtils.printNormalText(isOldSunmiVersion, it1)
        } }
    }

    fun printTotalTax() {
        _dataMap.find { it.first == PRINT_TAX }.let { it?.second?.let { it1 ->
            PrintSunmiUtils.printNormalText(isOldSunmiVersion, it1)
        } }
    }

    fun printSubTotal() {
        _dataMap.find { it.first == PRINT_SUB_TOTAL }.let { it?.second?.let { it1 ->
            PrintSunmiUtils.printNormalText(isOldSunmiVersion, it1)
        } }
    }

    fun printTotalServiceCharges() {
        _dataMap.find { it.first == PRINT_SERVICE_CHARGES }.let { it?.second?.let { it1 ->
            PrintSunmiUtils.printNormalText(isOldSunmiVersion, it1)
        } }
    }

    fun printTips() {
        _dataMap.find { it.first == PRINT_TIPS }.let { it?.second?.let { it1 ->
            PrintSunmiUtils.printNormalText(isOldSunmiVersion, it1)
        } }
    }

    fun printSurCharges() {
        _dataMap.find { it.first == PRINT_SURCHARGE }.let { it?.second?.let { it1 ->
            PrintSunmiUtils.printNormalText(isOldSunmiVersion, it1)
        } }
    }

    fun printCashDiscount() {
        _dataMap.find { it.first == PRINT_CASH_DISCOUNT }.let { it?.second?.let { it1 ->
            PrintSunmiUtils.printNormalText(isOldSunmiVersion, it1)
        } }
    }

    fun printTotalPrice() {
        _dataMap.find { it.first == PRINT_TOTAL_PRICE }.let { it?.second?.let { it1 ->
            PrintSunmiUtils.printBoldText(isOldSunmiVersion,it1)
        } }
    }

    fun printPaidAmount() {
        _dataMap.find { it.first == PRINT_PAID_AMOUNT }.let { it?.second?.let { it1 ->
            PrintSunmiUtils.printBoldText(isOldSunmiVersion,it1)
        } }
    }

    fun printChangeAmount() {
        _dataMap.find { it.first == PRINT_CHANGE_AMOUNT }.let { it?.second?.let { it1 ->
            PrintSunmiUtils.printBoldText(isOldSunmiVersion,it1)
        } }
    }

    fun printRemainingAmount() {
        _dataMap.find { it.first == PRINT_REMAINING_AMOUNT }.let { it?.second?.let { it1 ->
            PrintSunmiUtils.printBoldText(isOldSunmiVersion,it1)
        } }
    }

    fun printTipCustom() {
        _dataMap.find { it.first == PRINT_TIPS_CUSTOM }.let { it?.second?.let { it1 ->
            PrintSunmiUtils.printBoldText(isOldSunmiVersion,it1)
        } }
    }

    fun printTotalCustoms() {
        _dataMap.find { it.first == PRINT_TOTAL_CUSTOM }.let { it?.second?.let { it1 ->
            PrintSunmiUtils.printBoldText(isOldSunmiVersion,it1)
        } }
    }

    fun printPaymentsHistoryLabel() {
        breakLine()
       PrintSunmiUtils.printTextCenter( PRINT_PAYMENT_HISTORY_LABEL)
    }

    fun printSinglePayment(payment:String) {
        PrintSunmiUtils.printBoldText(isOldSunmiVersion,payment)
    }

    fun printAdditionalTipsLabel() {
        PrintSunmiUtils.additionalTipsInner()
    }

    fun printSingleAdditionalTip(singleTip:String) {
        PrintSunmiUtils.printNormalText(isOldSunmiVersion, singleTip)
    }

    fun printTransactionId() {
        _dataMap.find { it.first == PRINT_TRANSACTION_ID }.let { it?.second?.let { it1 ->
            PrintSunmiUtils.printBoldText(isOldSunmiVersion,it1)
        } }
    }

    fun printTransactionType() {
        _dataMap.find { it.first == PRINT_TRANSACTION_TYPE }.let { it?.second?.let { it1 ->
            PrintSunmiUtils.printBoldText(isOldSunmiVersion,it1)
        } }
    }

    fun printCardMaskedNo() {
        _dataMap.find { it.first == PRINT_MASKED_CARD_NO }.let { it?.second?.let { it1 ->
            PrintSunmiUtils.printBoldText(isOldSunmiVersion,it1)
        } }
    }

    fun printCardType() {
        _dataMap.find { it.first == PRINT_CARD_TYPE }.let { it?.second?.let { it1 ->
            PrintSunmiUtils.printBoldText(isOldSunmiVersion,it1)
        } }
    }

    fun printData(data:String) {
        PrintSunmiUtils.printNormalText(isOldSunmiVersion,data)
    }

    fun printCustomerSignature() {
        _dataMap.find { it.first == PRINT_CUSTOMER_SIGNATURE }.let { it?.second?.let { it1 ->
            PrintSunmiUtils.printBoldText(isOldSunmiVersion,it1)
        } }
    }

    fun printOrderNote() {
        _dataMap.find { it.first == PRINT_ORDER_NOTE }.let { it?.second?.let { it1 ->
            PrintSunmiUtils.orderNoteInner(it1)
        } }
    }

    fun printQRCode() {
        _dataMap.find { it.first == PRINT_QR_CODE }.let { it?.second?.let { it1 ->
            PrintSunmiUtils.qrCodeInner(it1)
        } }
    }
}
class LandiInnerPrinterPays() {

    var _dataMap = mutableListOf<Pair<String,String>>()


    fun setDataMap(dataMap :MutableList<Pair<String,String>>) {
        this._dataMap = dataMap
    }

    fun initPrinter(font: String) {

    }

    fun printOrderId() {
        _dataMap.find { it.first == ORDER_ID }.let { it?.second?.let { it1 ->
            LPrint.printCenter(it1,FONT_SIZE_5X,isBold = true)
        } }

        lineBreak()
    }

    fun breakLine(){
       LPrint.lineBreak()
    }


    fun printBusinessDetails() {
        _dataMap.let {
            val businessName = _dataMap.find { it.first == BUSINESS_NAME }?.second
            val businessAddress = _dataMap.find { it.first == BUSINESS_ADDRESS }?.second
            val businessPhone = _dataMap.find { it.first == BUSINESS_PHONE_NO }?.second

            if (businessName != null && businessAddress != null && businessPhone != null) {

                printCenter(businessName)
                lineBreak()

                printCenter(businessAddress, fontSize = SMALL_SIZE)
                lineBreak()


                printCenter(businessPhone, fontSize = SMALL_SIZE)
                lineBreak()
            }
        }
    }

    fun printOrderType() {
        _dataMap.find { it.first == ORDER_TYPE }.let { it?.second?.let { it1 ->
            LPrint.printCenter(
                it1,
                isBold = true,
                fontSize = FONT_SIZE_5X
            )
            lineBreak()
        } }

    }

    fun printReceiptId() {
        _dataMap.find { it.first == RECEIPT_ID }.let { it?.second?.let { it1 ->
            printLeft(it1)
            lineBreak()
        } }

    }

    fun printEmployeeName() {
        _dataMap.find { it.first == EMPLOYEE_NAME }.let { it?.second?.let { it1 ->
            printLeft(it1)
            lineBreak()
        } }

    }

    fun printOrderTime() {
        _dataMap.find { it.first == ORDER_TIME }.let { it?.second?.let { it1 ->
            printLeft(it1)
            lineBreak()
        } }

    }

    fun printHorizontalLine() {
        printDashedLineAndBreak()
    }

    fun printTime() {
        _dataMap.find { it.first == PRINT_TIME }.let { it?.second?.let { it1 ->
            printLeft(it1)
            lineBreak()
        } }
    }

    fun printGuest() {
        _dataMap.find { it.first == PRINT_GUEST_NAME }.let { it?.second?.let { it1 ->
            printCenter(it1)
            lineBreak()
        } }
    }

    fun printGuest(guestName:String) {
        printCenter(guestName)
        lineBreak()
    }

    fun printItem() {
        _dataMap.find { it.first == PRINT_ITEM }.let { it?.second?.let { it1 ->
            printLeft(it1)
        } }
    }

    fun printItem(item:String) {
        printLeft(item)
    }

    fun printModifier() {
        _dataMap.find { it.first == PRINT_MODIFIER }.let { it?.second?.let { it1 ->
            printLeft(it1)
        } }
    }

    fun printModifier(modifiers: String) {
        printLeft(modifiers)
    }

    fun printItemNote() {
        _dataMap.find { it.first == PRINTER_ITEM_NOTE }.let { it?.second?.let { it1 ->
            printLeft(it1)
        } }
    }

    fun printItemNote(itemNote:String) {
        printLeft(itemNote)
    }

    fun printTotalDiscount() {
        _dataMap.find { it.first == PRINT_TOTAL_DISCOUNT }.let { it?.second?.let { it1 ->
            lineBreak()
            printLeft(it1)
        } }
    }

    fun printTotalTax() {
        _dataMap.find { it.first == PRINT_TAX }.let { it?.second?.let { it1 ->
           printLeft(it1)
        } }
    }

    fun printSubTotal() {
        _dataMap.find { it.first == PRINT_SUB_TOTAL }.let { it?.second?.let { it1 ->
            printLeft(it1)
        } }
    }

    fun printTotalServiceCharges() {
        _dataMap.find { it.first == PRINT_SERVICE_CHARGES }.let { it?.second?.let { it1 ->
            printLeft(it1)
        } }
    }

    fun printTips() {
        _dataMap.find { it.first == PRINT_TIPS }.let { it?.second?.let { it1 ->
            printLeft(it1)
        } }
    }

    fun printSurCharges() {
        _dataMap.find { it.first == PRINT_SURCHARGE }.let { it?.second?.let { it1 ->
           printLeft(it1)
        } }
    }

    fun printCashDiscount() {
        _dataMap.find { it.first == PRINT_CASH_DISCOUNT }.let { it?.second?.let { it1 ->
            printLeft(it1)
        } }
    }

    fun printTotalPrice() {
        _dataMap.find { it.first == PRINT_TOTAL_PRICE }.let { it?.second?.let { it1 ->
            lineBreak()
            printBoldLeft(it1)
        } }
    }

    fun printPaidAmount() {
        _dataMap.find { it.first == PRINT_PAID_AMOUNT }.let { it?.second?.let { it1 ->
            printBoldLeft(it1)
        } }
    }

    fun printChangeAmount() {
        _dataMap.find { it.first == PRINT_CHANGE_AMOUNT }.let { it?.second?.let { it1 ->
            printBoldLeft(it1)
        } }
    }

    fun printRemainingAmount() {
        _dataMap.find { it.first == PRINT_REMAINING_AMOUNT }.let { it?.second?.let { it1 ->
            printBoldLeft(it1)
        } }
    }

    fun printTipCustom() {
        _dataMap.find { it.first == PRINT_TIPS_CUSTOM }.let { it?.second?.let { it1 ->
            lineBreak()
            printBoldLeft(it1)
        } }
    }

    fun printTotalCustoms() {
        _dataMap.find { it.first == PRINT_TOTAL_CUSTOM }.let { it?.second?.let { it1 ->
            lineBreak()
            printBoldLeft(it1)
            lineBreak()
        } }
    }

    fun printPaymentsHistoryLabel() {
        breakLine()
        printCenter( PRINT_PAYMENT_HISTORY_LABEL)
        lineBreak()
    }

    fun printSinglePayment(payment:String) {
        printBoldLeft(payment)
        lineBreak()
    }

    fun printAdditionalTipsLabel() {
        lineBreak()
        printBoldLeft(PRINT_ADDITIONAL_TIPS_LABEL)
        lineBreak()
    }

    fun printSingleAdditionalTip(singleTip:String) {
        printLeft(singleTip, isBold = false)
    }

    fun printTransactionId() {
        _dataMap.find { it.first == PRINT_TRANSACTION_ID }.let { it?.second?.let { it1 ->
            lineBreak()
            lineBreak()
            printBoldLeft(it1)
        } }
    }

    fun printTransactionType() {
        _dataMap.find { it.first == PRINT_TRANSACTION_TYPE }.let { it?.second?.let { it1 ->
            printBoldLeft(it1)
        } }
    }

    fun printCardMaskedNo() {
        _dataMap.find { it.first == PRINT_MASKED_CARD_NO }.let { it?.second?.let { it1 ->
            printBoldLeft(it1)
        } }
    }

    fun printCardType() {
        _dataMap.find { it.first == PRINT_CARD_TYPE }.let { it?.second?.let { it1 ->
            printBoldLeft(it1)
        } }
    }

    fun printData(data:String) {
        lineBreak()
        printLeft(data)
    }

    fun printCustomerSignature() {
        _dataMap.find { it.first == PRINT_CUSTOMER_SIGNATURE }.let { it?.second?.let { it1 ->
            lineBreak()
            lineBreak()
            printBoldLeft(it1)
        } }
    }

    fun printOrderNote() {
        _dataMap.find { it.first == PRINT_ORDER_NOTE }.let { it?.second?.let { it1 ->
            lineBreak()

            printCenter("Order Note")
            lineBreak()
            printCenter( it1)
            lineBreak()
        } }
    }

    fun printQRCode() {
        _dataMap.find { it.first == PRINT_QR_CODE }.let { it?.second?.let { it1 ->
            LPrint.printQRCode(LPrint.getOutputStream()!!,it1,LPrint.CENTER_ALIGN)
        } }
    }


}
class SunmiCloudPrinter()
