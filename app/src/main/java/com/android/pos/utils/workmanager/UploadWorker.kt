package com.android.pos.utils.workmanager

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.android.pos.utils.printer.PrinterClass
import com.epson.eposprint.Builder
import com.epson.eposprint.Print
import org.jetbrains.annotations.NotNull


class UploadWorker(@NotNull context: Context, @NotNull params: WorkerParameters) :
    Worker(context, params) {
    private val TAG = UploadWorker::class.java.name
    override fun doWork(): Result {
        val data = inputData.getString("itemName")
        Log.e(TAG, "datadata  ${data}")


        return try {
            if (data.isNullOrEmpty()) {
                throw IllegalArgumentException("Invalid input uri")
            } else {
                connectPrinter(context = applicationContext, data)
            }


            Result.success()
        } catch (throwable: Throwable) {
            Result.failure()
        }
    }

    fun connectPrinter(context: Context, data: String) {
        var printer: Print = Print(context)
        if (printer != null) {
            /*  printer.setStatusChangeEventCallback(this)
              printer.setBatteryStatusChangeEventCallback(this)
              printer.setStatusChangeEventCallback(this)*/
        }

        val enabled = Print.TRUE
        try {
            printer?.openPrinter(
                Print.DEVTYPE_TCP,
                "192.168.3.202",
                enabled,
                10000
            )


        } catch (e: Exception) {

            try {

            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
            Log.e(TAG, "PrinterException: " + e.message)

            return
        }

        if (printer != null) {
            PrinterClass.setPrinter(printer)

            generateKitchenReceipt(data)

        }


    }

    private fun generateKitchenReceipt(data: String) {
        var builder: Builder? = null
        try {

            val pname = "TM-U220"

            builder = Builder(pname, PrinterClass.language, applicationContext)

            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)
            builder.addFeedLine(1)
            builder.addTextFont(Builder.FONT_E)
            builder.addTextAlign(Builder.ALIGN_LEFT)
            //builder.addTextLineSpace(20)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 1)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
            )
            builder.addText(data)


            /*  if (receiptModel?.order?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {
                  builder.addTextLineSpace(30)
                  builder.addFeedUnit(30)
                  builder.addFeedLine(1)
                  builder.addTextFont(Builder.FONT_E)
                  builder.addTextAlign(Builder.ALIGN_LEFT)
                  //builder.addTextLineSpace(20)
                  builder.addTextLang(Builder.LANG_EN)
                  builder.addTextSize(1, 1)
                  builder.addTextStyle(
                      Builder.FALSE,
                      Builder.FALSE,
                      Builder.TRUE,
                      Builder.COLOR_1
                  )
                  builder.addText("Order Note")

                  builder.addTextLineSpace(30)
                  builder.addFeedUnit(30)

                  builder.addTextFont(Builder.FONT_E)
                  builder.addTextAlign(Builder.ALIGN_LEFT)
                  builder.addTextLang(Builder.LANG_EN)
                  builder.addTextSize(1, 1)
                  builder.addTextStyle(
                      Builder.FALSE,
                      Builder.FALSE,
                      Builder.FALSE,
                      Builder.COLOR_1
                  )


                  builder.addText(receiptModel?.order?.note.toString())
              }*/



            builder.addFeedLine(2)

            builder.addCut(Builder.CUT_FEED)

            val status = IntArray(1)
            val battery = IntArray(1)


            try {
                PrinterClass.getPrinter()?.sendData(
                    builder,
                    PrinterClass.TEST_PRINT_LAN_TIME, status, battery
                )

                PrinterClass.closePrinter()


                //PrinterClass.getPrinter()?.sendData(builder, 0, status, battery)
            } catch (e: Exception) {


                PrinterClass.closePrinter()
                e.printStackTrace()


            }


        } catch (e: Exception) {

            e.printStackTrace()
        }


    }
}


