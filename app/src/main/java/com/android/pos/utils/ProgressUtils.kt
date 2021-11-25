package com.android.pos.utils

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import com.android.pos.R
import java.lang.Exception


/**
 * CommonUtils class
 *
 *
 *
 *
 * This is progress dialog utils class which manage show/hide progress dialog
 *
 *
 * @author Vishal Patel
 * @version 1.0
 */
object ProgressUtils {
    private var builder: Dialog? = null

    /***
     * Show progress dialog
     * @param message Message
     * @param processCount Count of total processes.
     * Like if you want to do 2 tasks at a time then just call this showProgressDialog with processCount=2 and then call dismissProgressDialog() method at every task finish
     */
    @JvmOverloads
    fun showProgressDialog(context: Activity) {
        if (builder == null)
            builder = Dialog(context)

        val inflater = LayoutInflater.from(context)

        val dialogView = inflater.inflate(R.layout.view_loading, null)
        builder!!.setContentView(dialogView)

        builder!!.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        builder!!.window?.setBackgroundDrawable(
            ColorDrawable(Color.WHITE)
        )
        builder!!.setCanceledOnTouchOutside(false)
        builder!!.setCancelable(false)
        builder!!.window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        if (!builder!!.isShowing) {
            val activity: Activity = context as Activity
            if (!activity.isFinishing && !activity.isDestroyed) {
                try {
                    builder?.show()
                } catch (e: Exception) {
                }

            }
        }
    }

    @JvmOverloads
    fun showProgressDialog(message: String?, context: Context) {
        if (builder == null)
            builder = Dialog(context)

        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.view_loading, null)
        builder?.setContentView(dialogView)

        builder?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        builder?.window?.setBackgroundDrawable(
            ColorDrawable(Color.WHITE)
        )
        builder?.setCanceledOnTouchOutside(false)
        builder?.window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        if (builder != null) {
            if (!builder!!.isShowing) {
                builder!!.show()
            }
        }
    }

    /***
     * For dismiss progress dialog
     */
    fun dismissProgressDialog() {
        try {
            if (builder != null && builder?.isShowing == true) {
                builder?.dismiss()
            }
        } catch (e: Exception) {
            Log.d("pos", "dismissProgressDialog: " + e.message)
        }

    }


}