package com.android.pos.utils.printer

import com.epson.eposprint.Print
import java.lang.Exception

object PrinterClass {

    const val PRINTER_INTERVAL = 1
    private var printer: Print? = null
    fun setPrinter(obj: Print) {
        printer = obj

    }

    fun getPrinter(): Print? {
        return printer
    }

    fun closePrinter() {
        try {
            printer?.closePrinter()
            printer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


}