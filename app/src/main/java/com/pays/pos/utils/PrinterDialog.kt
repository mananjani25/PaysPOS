package com.pays.pos.utils

import android.app.Dialog
import android.content.Context
import android.view.Window
import com.pays.pos.R

class PrinterDialog() {
    var dialog: Dialog?=null

    /*fun getInstance(): PrinterDialog? {
        return ourInstance
    }*/


    fun show(context: Context) {
        if (dialog != null && dialog?.isShowing() == true) {
            return
        }
        dialog = Dialog(context)
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog?.setContentView(R.layout.layout_progress_dialog)
        dialog?.setCancelable(true)
        dialog?.show()
    }

    fun dismiss() {
        if (dialog != null && dialog?.isShowing() == true) {
            dialog?.dismiss()
        }
    }
}