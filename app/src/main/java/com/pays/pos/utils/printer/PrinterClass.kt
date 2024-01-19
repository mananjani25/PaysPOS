package com.pays.pos.utils.printer

import android.util.Log
import com.epson.epos2.printer.Printer
import com.epson.eposprint.Builder
import com.epson.eposprint.Print
import javax.inject.Singleton

@Singleton
object PrinterClass {

    const val language = Builder.LANG_EN
    const val SEND_TIMEOUT = 1
    const val TEST_PRINT_LAN_TIME = 10 * 1000
    const val IMAGE_WIDTH_MAX = 512 * 2
    const val BLUETOOTH_TIMEOUT = 10 * 1000
    const val PRINTER_INTERVAL = 1
    private var printer: Print? = null
    private var mPrinter: Printer? = null

    init {
        if (mPrinter != null) {

            mPrinter?.setReceiveEventListener { printer, i, printerStatusInfo, s ->

                Log.e("cehckGlobalEvebt", "gotPrinterInside:")
                if (printerStatusInfo.online == 1) {
                    try {
                        printer.disconnect()

                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }


    fun setPrinter(obj: Print?) {
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
            printer = null
            e.printStackTrace()
        }
    }


    fun initPrinter(mPri: Printer) {
        mPrinter = mPri
        mPrinter?.startMonitor()
    }

    fun getInitPrinter(): Printer? {
        if (mPrinter != null)
            return mPrinter
        else
            return null
    }


}