package com.pays.pos.utils.landi

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.pays.pos.data.model.DineInModel
import com.pays.pos.utils.*
import java.io.IOException
import java.io.OutputStream
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
    val FONT_SIZE_3X = byteArrayOf(0x1B, 0x21, 0x11)
    val FONT_SIZE_4X = byteArrayOf(0x1B, 0x21, 0x22) // Width: 4, Height: 4
    val FONT_B = byteArrayOf(0x1D, 0x21, 0x11) // Font B0x1B, 0x4D, 0x01
    val FONT_SIZE_10X = byteArrayOf(0x1B, 0x21, 0xAA.toByte()) // Width: 10, Height: 10
    val FONT_SIZE_DOUBLE_HEIGHT = byteArrayOf(0x1B, 0x21, 0x31) // Double height
    val FONT_SIZE_DOUBLE_BOTH = byteArrayOf(0x1B, 0x21, 0x11) // Double width and height
    val FONT_SIZE_5X = byteArrayOf(0x1B, 0x21, 0x32) // Width: 4, Height: 4


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

    fun print(string: String,fontSize:ByteArray = NORMAL_SIZE, isBold:Boolean = false ,printOnNewLine:Boolean = false,align: ByteArray = LEFT_ALIGN){
        outputStream?.apply {

            if(isBold)
                write(BOLD_ON)
            else
                write(BOLD_OFF)

            write(fontSize)
            write(align)

            val stringToPrint = if(printOnNewLine) "\n${string.trim()}\n".toByteArray() else string.trim().toByteArray()
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