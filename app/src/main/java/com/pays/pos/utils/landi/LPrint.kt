package com.pays.pos.utils.landi

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import java.io.IOException
import java.io.OutputStream
import java.util.*

final object LPrint {
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bluetoothSocket: BluetoothSocket? = null

    private var outputStream: OutputStream? = null

    /*------------------------Formatting Parameters--------------------------*/
    public val FONT_SIZE = byteArrayOf(0x1D, 0x21, 0x11)
    public var NORMAL_SIZE = byteArrayOf(0x1B, 0x21, 0x00) // ESC ! 0
    public var DOUBLE_HEIGHT_WIDTH = byteArrayOf(0x1B, 0x21, 0x11) // ESC ! 17 (0x11)
    val FONT_SIZE_3X = byteArrayOf(0x1B, 0x21, 0x11)
    val FONT_SIZE_4X = byteArrayOf(0x1B, 0x21, 0x22) // Width: 4, Height: 4
    val FONT_B = byteArrayOf(0x1B, 0x4B, 0x01) // Font B
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


    public val LINE_FEED = "\n".toByteArray()

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
            return null
            e.printStackTrace()
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


}