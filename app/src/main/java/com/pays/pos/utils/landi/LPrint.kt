package com.pays.pos.utils.landi

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.util.Base64
import android.util.Log
import com.pays.pos.data.entities.TbCartItem
import com.pays.pos.data.model.DineInModel
import com.pays.pos.data.model.responseModel.GetKitchenReceiptSettingsResponse
import com.pays.pos.data.model.responseModel.GetOrderDetailsResponse
import com.pays.pos.data.model.responseModel.PrinterResponse
import com.pays.pos.data.remote.Constants
import com.pays.pos.di.PrefProvider
import com.pays.pos.ui.fragments.dineInNew.DineInOrderTablePays
import com.pays.pos.ui.fragments.settings.hardware.printer.SunmiPrintHelper
import com.pays.pos.utils.*
import com.sdksuite.omnidriver.aidl.printer.Align
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URL
import java.util.*

final object LPrint {
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bluetoothSocket: BluetoothSocket? = null

    private var outputStream: OutputStream? = null

    /*------------------------Formatting Parameters--------------------------*/
    public val FONT_SIZE = byteArrayOf(0x1D, 0x21, 0x11)
    public val SMALL_SIZE  = byteArrayOf(0x1B, 0x21, 0x11)
    public val MEDIUM_SIZE = byteArrayOf(0x1B, 0x21, 0x12)
    public var NORMAL_SIZE = byteArrayOf(0x1B, 0x21, 0x00) // ESC ! 0
    public var DOUBLE_HEIGHT_WIDTH = byteArrayOf(0x1B, 0x21, 0x11) // ESC ! 17 (0x11)
    val FONT_SIZE_3X = byteArrayOf(0x1B, 0x21, 0x22)
    val FONT_SIZE_4X = byteArrayOf(0x1B, 0x21, 0x22) // Width: 4, Height: 4
    val FONT_B = byteArrayOf(0x1D, 0x21, 0x11) // Font B0x1B, 0x4D, 0x01
    val FONT_SIZE_10X = byteArrayOf(0x1B, 0x21, 0xAA.toByte()) // Width: 10, Height: 10
    val FONT_SIZE_DOUBLE_HEIGHT = byteArrayOf(0x1B, 0x21, 0x31) // Double height
    val FONT_SIZE_DOUBLE_BOTH = byteArrayOf(0x1B, 0x21, 0x11) // Double width and height
    val FONT_SIZE_5X = byteArrayOf(0x1B, 0x21, 0x32) // Width: 5, Height: 5


    public var LEFT_ALIGN = byteArrayOf(0x1B, 0x61, 0x00) // ESC a 0
    public var CENTER_ALIGN = byteArrayOf(0x1B, 0x61, 0x01) // ESC a 1
    public var RIGHT_ALIGN = byteArrayOf(0x1B, 0x61, 0x02)

    public val RESET_FONT_SIZE = byteArrayOf(0x1D, 0x21, 0x00)
    public val BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)
    public val BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)
    public val UNDERLINE_ON = byteArrayOf(0x1B, 0x2D, 0x01)
    public val UNDERLINE_OFF = byteArrayOf(0x1B, 0x2D, 0x00)
    public val CUT_PAPER = byteArrayOf(0x1D, 0x56, 0x42, 0x00)

    var TAB = byteArrayOf(0x1B, 0x44, 0x08, 0x18, 0x28, 0x00) // Tab stops at columns 8, 24, 40

    public val LINE_FEED = "\n".toByteArray()
    public val DASHED_LINE_FEED = "------------------------------------------------\n".toByteArray()

    /*------------------------Formatting Parameters--------------------------*/

    public fun connectLandiInnerPrinter(macAddress: String): OutputStream? {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val printerMacAddress = macAddress // Replace with your printer's MAC address

        val bluetoothDevice: BluetoothDevice =
            bluetoothAdapter.getRemoteDevice(printerMacAddress)

        try {
            bluetoothSocket =
                bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
            bluetoothSocket?.connect()

            if (bluetoothSocket == null) {
                return null
            }

        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
        outputStream = bluetoothSocket?.outputStream
        return outputStream
    }

    public fun disconnectLandiPrinter() {
        outputStream?.let {
            it.flush()
            it.close()
        }
        bluetoothSocket?.let {
            it.close()
        }
    }

    fun setOutputStream(outputStream: OutputStream){
        this.outputStream = outputStream
    }

    fun getOutputStream() = outputStream

    fun printImage(ba:ByteArray){
        outputStream?.apply {
            try {

//                addImage(ba, Align.CENTER, 0)
//                feedLine(2)
            } catch (ex: java.lang.Exception) {
                Log.d("DMJ", "Error getting image bytes to print")
            }
        }
    }

    fun print(string: String,fontSize:ByteArray = NORMAL_SIZE, isBold:Boolean = false ,printOnNewLine:Boolean = false,align: ByteArray = LEFT_ALIGN, trim:Boolean = true){
        outputStream?.apply {

            if(isBold)
                write(BOLD_ON)
            else
                write(BOLD_OFF)

            write(fontSize)
            write(align)

            val stringToPrint = if(printOnNewLine) "\n${if (trim == true) string.trim() else string}\n".toByteArray() else { if (trim == true) string.trim().toByteArray() else string.toByteArray()}
            write(stringToPrint)

            if(isBold)
                write(BOLD_OFF)
        }
    }

    fun printCenter(string: String,fontSize:ByteArray = NORMAL_SIZE, isBold:Boolean = false ,printOnNewLine:Boolean = false){
        outputStream?.apply {



            if(isBold)
                write(BOLD_ON)
            else
                write(BOLD_OFF)
            write(fontSize)
            write(CENTER_ALIGN)

            val stringToPrint = if(printOnNewLine) "\n${string}\n".toByteArray() else string.toByteArray()
            write(stringToPrint)

            if(isBold)
                write(BOLD_OFF)
        }
    }

    fun printLeft(string: String,fontSize:ByteArray = NORMAL_SIZE, isBold:Boolean = false ,printOnNewLine:Boolean = false){
        outputStream?.apply {

            if(isBold)
                write(BOLD_ON)
            else
                write(BOLD_OFF)

            write(LEFT_ALIGN)
            write(fontSize)


            val stringToPrint = if(printOnNewLine) "\n${string}\n".toByteArray() else string.toByteArray()
            write(stringToPrint)

            if(isBold)
                write(BOLD_OFF)
        }
    }

    fun printBoldLeft(string:String) {
        outputStream?.apply {
            write(BOLD_ON)
            write(string.toByteArray())

        }
    }

    fun printDashedLineAndBreak(fontSize: ByteArray = NORMAL_SIZE) {
        outputStream?.write(fontSize)
        outputStream?.write(DASHED_LINE_FEED)
    }



    public fun bold_On() {
        outputStream?.write(BOLD_ON)
    }

    public fun bold_Off() {
        outputStream?.write(BOLD_OFF)
    }

    public fun centerAlign_On() {
        outputStream?.write(CENTER_ALIGN)
    }

    public fun centerAlign_Off() {
        outputStream?.write(LEFT_ALIGN)
    }


    public fun rightAlign_On() {
        outputStream?.write(RIGHT_ALIGN)
    }

    public fun rightAlign_Off() {
        outputStream?.write(LEFT_ALIGN)
    }

    public fun underline_On() {
        outputStream?.write(UNDERLINE_ON)
    }

    public fun underline_Off() {
        outputStream?.write(UNDERLINE_OFF)
    }

    public fun bigFont_On() {
        outputStream?.write(DOUBLE_HEIGHT_WIDTH)
    }

    public fun bigFont_Off() {
        outputStream?.write(NORMAL_SIZE)
    }

    public fun printText(text: String) {
        outputStream?.write(text.toByteArray())
    }

    public fun printWithFontSize(text: String,fontSize: ByteArray) {

        outputStream?.write(fontSize)
        outputStream?.write(text.toByteArray())
    }

    public fun lineBreak() {
        outputStream?.write(LINE_FEED)
    }

    public fun paperCut() {
        outputStream?.write(CUT_PAPER)
    }

    public fun centerText(writer: OutputStream,text:String) {
        writer.apply {
            write(CENTER_ALIGN)
            write(text.toByteArray())
            write(LINE_FEED)
        }

    }

    fun cardDetailsInner(cardName: String, cardType: String, cardNumber: String, font: String) {

        for (i in 1..3) {

            when (i) {
                /*1 -> {
                    if (!cardName.isNullOrBlank()) {
                        val strCardName = padLine(
                            "",
                            cardName,
                            if (font == Constants.LARGE) 23 else 48
                        ).toString()
                        normalText(strCardName)

                    }
                }*/

                2 -> {
                    if (!cardType.isNullOrBlank()) {
                        val strCardType = padLine(
                            "",
                            cardType,
                           /* if (font == Constants.LARGE) 23 else */48
                        ).toString()
                        print(strCardType, align = RIGHT_ALIGN)
                        lineBreak()
                    }

                }

                3 -> {
                    if (!cardNumber.isNullOrBlank()) {
                        val strCardNumber = padLine(
                            "",
                            cardNumber,
                           /* if (font == Constants.LARGE) 23 else*/ 48
                        ).toString()
                        print(strCardNumber, align = RIGHT_ALIGN)
                    }
                }
            }
        }
    }

    fun downloadImage(url: String): Bitmap? {
        return try {
            val inputStream = URL(url).openStream()
            BitmapFactory.decodeStream(inputStream) // Convert URL to Bitmap
        } catch (e: Exception) {
            println("Error downloading image: ${e.message}")
            null
        }
    }

    fun scaleBitmap(bitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val scaleWidth = newWidth.toFloat() / width
        val scaleHeight = newHeight.toFloat() / height

        val matrix = Matrix()
        matrix.postScale(scaleWidth, scaleHeight) // Apply scaling transformation

        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
    }

    fun processImageForPrinting(url: String, newWidth: Int, newHeight: Int): ByteArray? {
        val bitmap = downloadImage(url) ?: return null
        val scaledBitmap = scaleBitmap(bitmap, newWidth, newHeight) // Proper scaling
        return bitmapToByteArray(scaledBitmap)
    }

    fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream) // Convert Bitmap to PNG byte array
        return outputStream.toByteArray()
    }


    public fun printQRCode(writer: OutputStream, data: String,align :ByteArray= CENTER_ALIGN) {
        try {
            val outputStream = writer

            outputStream.write(align)

            // Set QR code model
            outputStream.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, 0x32, 0x00))

            // Set QR code size (n: 1-16)
            val qrCodeSize = 8 // Adjust this for desired size
            outputStream.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, qrCodeSize.toByte()))

            // Set error correction level (n: 0-3, 0 is lowest, 3 is highest)
            val errorCorrectionLevel = 2
            outputStream.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, errorCorrectionLevel.toByte()))

            // Store QR code data
            val qrDataBytes = data.toByteArray()
            val length = qrDataBytes.size + 3
            val pL = length and 0xFF
            val pH = (length shr 8) and 0xFF
            outputStream.write(byteArrayOf(0x1D, 0x28, 0x6B, pL.toByte(), pH.toByte(), 0x31, 0x50, 0x30))
            outputStream.write(qrDataBytes)

            // Print QR code
            outputStream.write(byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30))

            // Add a line break for better formatting
            outputStream.write("\n".toByteArray())

            outputStream.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun printDineInItemData(dineInList:List<DineInModel>,isShowModifier:Boolean) {
        for (i in dineInList.indices) {
            if (dineInList[i].isHeader == 0) {

                if (i != (dineInList.size - 1) && dineInList[i + 1].isHeader == 1) {

                    if (dineInList[i]?.customer == null) {

                        dineInList[i]?.title?.let {
                            printCenter(it)
                            lineBreak()
                        }

                    } else {

                        val tableName =
                            dineInList[i]?.customer?.first_name + " " +
                                    if (dineInList[i]?.customer?.last_name != null) {
                                        dineInList[i].customer?.last_name
                                    } else {
                                        ""
                                    }
                        printCenter(tableName)
                        lineBreak()

                    }
                }


            } else {
                LogUtil.logE("addDineInInner", "22222222")
                dineInList.get(i).item?.let { item ->


                    val itemDataToPrint =
                        padLineCustomerItem(
                            item.itemQuantity.toString() + "  " + getItemNameToShow(
                                item.name
                            ),
                            getItemPriceToShow(item.price * item.itemQuantity),
                            48
                        )


                    printLeft(itemDataToPrint)
                    lineBreak()


                    if (item.modifiers.isNotEmpty() && isShowModifier) {
                        for (j in 0 until item.modifiers.size) {
                            val modifierObj = item.modifiers.get(j)


                            val modifiersToPrint =
                                padLineCustomerItem(
                                    if (modifierObj.modifier_quantity == 1) {
                                        "     " + getItemNameToShow(
                                            modifierObj.name
                                        )
                                    } else {
                                        "  " + modifierObj.modifier_quantity + "x " + getItemNameToShow(
                                            modifierObj.name
                                        )
                                    },
                                    getModifierItemPriceToShow(
                                        modifierObj.price,
                                        modifierObj.itemQuantity
                                    ),
                                    48
                                )


                            printLeft(modifiersToPrint)
                            lineBreak()

                        }


                    }


                    if (item.note.isNotEmpty()) {
                        lineBreak()
                        printLeft("   Note: " + item.note)
                        lineBreak()
                    }

                }


            }

        }
    }

    fun printKitchenReceiptDineInLandi(
        context: Context,
        outputStream: OutputStream,
        customerReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String,
        item: ArrayList<TbCartItem>,
        listItemWithGuest: HashMap<String, ArrayList<TbCartItem>> = hashMapOf(),
        kitchenSettingModel: GetKitchenReceiptSettingsResponse.Data,
        prefProvider: PrefProvider,
        getOrderDetailsResponse: GetOrderDetailsResponse.Data?
    ) {
        try {
            //ProgressUtils.showProgressDialog(requireActivity())

            lineBreak()

            if (prefProvider.getValueboolean(Constants.ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                printCenter("OrderID:" + getOrderDetailsResponse?.custom_order_id, isBold = true, fontSize = FONT_B)
            } else {
                printCenter("OrderID:" + getOrderDetailsResponse?.id, isBold = true, fontSize = FONT_B)
            }



            lineBreak()

            if (kitchenSettingModel.showOrderType) {

                printCenter(getOrderDetailsResponse?.orderTypeName.toString(), isBold = true, fontSize = FONT_B)

                lineBreak()
            }

            if(prefProvider.getValueboolean(Constants.DINE_IN_UPDATE,false)) {
                lineBreak()
                printCenter("*** Updated ***", fontSize = FONT_B)
            }

            lineBreak()
            lineBreak()

            printCenter(getOrderDetailsResponse?.floorPlanTable?.tableName + " (" + getOrderDetailsResponse?.floorPlanTable?.tableNumber + ")", fontSize = FONT_B)

            lineBreak()
            lineBreak()

            if (kitchenSettingModel.showTeamMember) {


                printLeft(
                    padLine(
                        "Employee:" + getOrderDetailsResponse?.employee?.name, "",
                        if (kitchenSettingModel.fonts == Constants.LARGE) 23 else 48
                    ).toString()
                )

            }

            lineBreak()

            printLeft(
                padLine(
                    Constants.getReceiptFormatDateFromUTCServer(
                        context,
                        getOrderDetailsResponse?.createdAt.toString()
                    ),
                    "",
                    48
                ).toString()
            )

            printDashedLineAndBreak()
            lineBreak()


            addOrdersForKitchenDineInLandi(
                item, customerReceiptPrinters.printerCategories.toCollection(
                    arrayListOf()
                ), listItemWithGuest
            )


            if (getOrderDetailsResponse?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {

                lineBreak()
                printCenter(getOrderDetailsResponse.note)
                lineBreak()
            }

            paperCut()

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun addOrdersForKitchenDineInLandi(
        list: ArrayList<TbCartItem>,
        printerCat: ArrayList<PrinterResponse.Data.PrinterCategories>? = null,
        listItemWithGuest: HashMap<String, ArrayList<TbCartItem>> = hashMapOf(),
    ):List<String> {


        val builder = mutableListOf<String>()

        listItemWithGuest.forEach { guest ->
            var isGuestNamePrinted = false
            guest.value.forEach { obj ->

                printerCat?.forEach {
                    if (it.id == obj.categoryId && it.printerEnable && it.categoryActive) {

                        if (!isGuestNamePrinted) {
                            lineBreak()
                            printDashedLineAndBreak()

                            //printText(guest.key + "\n")
                            printWithFontSize(guest.key + "\n", FONT_B)

                            printDashedLineAndBreak()
                            lineBreak()

                            isGuestNamePrinted = true
                        }

                        printWithFontSize(
                            obj.itemQuantity.toString() + " " + obj.name.uppercase(),
                            FONT_B
                        )

                        if (obj.modifiers.isNotEmpty()) {
                            for (j in 0 until obj.modifiers.size) {
                                val modifierObj = obj.modifiers.get(j)


                                lineBreak()
                                printText(
                                    "  " + if (modifierObj.modifier_quantity == 1) {
                                        "   "
                                    } else {
                                        "" + modifierObj.modifier_quantity + "x "
                                    } + modifierObj.name.uppercase()
                                )


                            }
                        }
                        if (obj.note.isNotEmpty()) {
                            lineBreak()
                            printText("  Note:" + obj.note)
                        }

                        lineBreak()

                        builder.add(obj.orderItemId.toString())
                    }
                }
            }

        }

       return builder

    }


    fun printTab(){
        outputStream?.apply {
            write(TAB)
        }
    }

    fun printTableRow(writer:OutputStream,columns: Array<String>, widths: IntArray) {
        val outputStream = writer
        val row = StringBuilder()
        for (i in columns.indices) {
            row.append(String.format("%-" + widths[i] + "s", columns[i]))
        }
        outputStream?.write(row.toString().toByteArray())
    }

}