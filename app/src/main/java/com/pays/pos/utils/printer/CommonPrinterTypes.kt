package com.pays.pos.utils.printer

import com.pays.pos.data.entities.TbCartItem
import com.pays.pos.ui.fragments.settings.hardware.printer.SunmiPrintHelper
import com.pays.pos.utils.PrintSunmiUtils
import com.pays.pos.utils.addOrdersForKitchenDineIn
import com.pays.pos.utils.addOrdersForKitchenDineInInner
import com.pays.pos.utils.landi.LPrint
import com.sunmi.externalprinterlibrary.api.SunmiPrinterApi
import java.io.OutputStream


enum class CommonPrinterTypes {
    SunmiInnerPrinter,
    LandiInnerPrinter,
    SunmiCloudPrinter
}


class SunmiInnerPrinter(var isOldVersion:Boolean = false)
class LandiInnerPrinter(outputStream: OutputStream? = null)
class SunmiCloudPrinter()

//
//import com.pays.pos.data.entities.TbCartItem
//import com.pays.pos.ui.fragments.settings.hardware.printer.SunmiPrintHelper
//import com.pays.pos.utils.PrintSunmiUtils
//import com.pays.pos.utils.addOrdersForKitchenDineIn
//import com.pays.pos.utils.addOrdersForKitchenDineInInner
//import com.pays.pos.utils.landi.LPrint
//import com.sunmi.externalprinterlibrary.api.SunmiPrinterApi
//import java.io.OutputStream

//// Enum class defining printer types
//enum class CommonPrinterTypes {
//    SunmiInnerPrinter {
//        override fun createPrinter(): Printer {
//            return SunmiInnerPrinter()
//        }
//    },
//    LandiInnerPrinter {
//        override fun createPrinter(): Printer {
//            return LandiInnerPrinter()
//        }
//    },
//    SunmiCloudPrinter {
//        override fun createPrinter(): Printer {
//            return SunmiCloudPrinter()
//        }
//    };
//
//    abstract fun createPrinter(): Printer
//}
//
//// Data class for order details
//data class OrderDetails(
//    val orderId: String,
//    val orderType: String,
//    val tableInfo: String,
//    val receiptId: String,
//    val employee: String,
//    val orderTime: String,
//    val note: String,
//    val isUpdated: Boolean,
//    val fontSize: String,
//    val showTeamMember: Boolean
//)
//
//// Printer interface defining printing and cutting functionality
//interface Printer {
//    fun printOrder(orderDetails: OrderDetails, items: List<TbCartItem>, listItemWithGuest: HashMap<String, ArrayList<TbCartItem>>)
//    fun cutPaper()
//}
//
//// Implementation for SunmiInnerPrinter
//class SunmiInnerPrinter(var isOldVersion: Boolean = false) : Printer {
//    override fun printOrder(orderDetails: OrderDetails, items: List<TbCartItem>, listItemWithGuest: HashMap<String, ArrayList<TbCartItem>>) {
//        SunmiPrintHelper.getInstance().initPrinter()
//        SunmiPrintHelper.getInstance().lineWrap(1)
//
//        PrintSunmiUtils.apply {
//            headerText(orderDetails.orderId)
//            headerText(orderDetails.orderType)
//            SunmiPrintHelper.getInstance().lineWrap(1)
//
//            if (orderDetails.isUpdated) headerText("*** Updated ***")
//            normalTextCenterLarge(orderDetails.tableInfo)
//            SunmiPrintHelper.getInstance().lineWrap(1)
//            normalTextLarge(orderDetails.receiptId)
//            normalTextLarge(orderDetails.employee)
//            normalTextLarge(orderDetails.orderTime)
//
//            printHorizontalInnerNew(isOldVersion)
//            // Optionally include item printing logic
//            // addOrdersForKitchenDineInInner(items, listItemWithGuest, isOldVersion)
//
//            SunmiPrintHelper.getInstance().lineWrap(1)
//            orderNoteInnerLarge(orderDetails.note)
//        }
//    }
//
//    override fun cutPaper() {
//        PrintSunmiUtils.cutPaperInner()
//    }
//}
//
//// Implementation for LandiInnerPrinter
//class LandiInnerPrinter(var outputStream: OutputStream? = null) : Printer {
//    override fun printOrder(orderDetails: OrderDetails, items: List<TbCartItem>, listItemWithGuest: HashMap<String, ArrayList<TbCartItem>>) {
//        LPrint.apply {
//            printCenter(orderDetails.orderId, isBold = true, fontSize = FONT_B)
//            lineBreak()
//            printCenter(orderDetails.orderType, isBold = true, fontSize = FONT_B)
//            lineBreak()
//
//            if (orderDetails.isUpdated) {
//                printCenter("*** Updated ***", isBold = true, fontSize = FONT_B)
//                lineBreak()
//            }
//
//            printCenter(orderDetails.tableInfo, isBold = true, fontSize = FONT_B)
//            lineBreak()
//            printLeft(orderDetails.receiptId)
//            lineBreak()
//            printLeft(orderDetails.employee)
//            lineBreak()
//            printLeft(orderDetails.orderTime)
//            lineBreak()
//            printDashedLineAndBreak()
//
//            // Optionally include item printing logic
//            // addOrdersForKitchenDineInLandi(items, listItemWithGuest)
//            lineBreak()
//            printCenter(orderDetails.note)
//            lineBreak()
//        }
//    }
//
//    override fun cutPaper() {
//        LPrint.paperCut()
//    }
//}
//
//// Implementation for SunmiCloudPrinter
//class SunmiCloudPrinter : Printer {
//    override fun printOrder(orderDetails: OrderDetails, items: List<TbCartItem>, listItemWithGuest: HashMap<String, ArrayList<TbCartItem>>) {
//        PrintSunmiUtils.apply {
//            fontSize(orderDetails.fontSize.toString())
//            SunmiPrinterApi.getInstance().printerInit()
//            SunmiPrinterApi.getInstance().lineWrap(1)
//
//            orderIdLarge(orderDetails.orderId)
//            printOrderType(orderDetails.orderType)
//
//            SunmiPrinterApi.getInstance().lineWrap(1)
//            if (orderDetails.isUpdated) addValue("*** Updated ***")
//
//            SunmiPrinterApi.getInstance().lineWrap(1)
//            addValue(orderDetails.tableInfo)
//
//            SunmiPrinterApi.getInstance().lineWrap(1)
//            receiptID(orderDetails.receiptId)
//            SunmiPrinterApi.getInstance().lineWrap(1)
//
//            if (orderDetails.showTeamMember) {
//                addValue(orderDetails.employee)
//                SunmiPrinterApi.getInstance().lineWrap(1)
//            }
//
//            orderTime(orderDetails.orderTime)
//            SunmiPrinterApi.getInstance().lineWrap(1)
//
//            printHorizontalInnerNew(false)
//            // Optionally include item printing logic
//            // addOrdersForKitchenDineIn(items, listItemWithGuest)
//            SunmiPrinterApi.getInstance().lineWrap(1)
//
//            orderNote(orderDetails.note)
//        }
//    }
//
//    override fun cutPaper() {
//        PrintSunmiUtils.cutPaper()
//    }
//}
//
