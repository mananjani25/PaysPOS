package com.pays.pos.utils

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.FragmentActivity
import com.pays.pos.R
import com.pays.pos.utils.callback.DeleteOptionCallback
import com.pays.pos.utils.extensions.visible
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

    private var listener: DeleteOptionCallback? = null

    fun setCallback(callback: DeleteOptionCallback) {
        listener = callback
    }

    /***
     * Show progress dialog
     * @param message Message
     * @param processCount Count of total processes.
     * Like if you want to do 2 tasks at a time then just call this showProgressDialog with processCount=2 and then call dismissProgressDialog() method at every task finish
     */
    @JvmOverloads
    fun showProgressDialog(context: Activity) {
        if (context.isFinishing || context.isDestroyed) {
            // Do not attempt to show the dialog if the Activity is not in a valid state
            return
        }

        if (builder == null)
            builder = Dialog(context)

        val inflater = LayoutInflater.from(context)

        val dialogView = inflater.inflate(R.layout.view_loading, null)
        builder?.setContentView(dialogView)

        builder?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
//        builder?.window?.setBackgroundDrawable(
//            ColorDrawable(Color.WHITE)
//        )
        builder?.setCanceledOnTouchOutside(false)
        builder?.setCancelable(false)
        builder?.window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        builder?.isShowing?.let { isShowing ->
            if (!isShowing) {
                val activity: Activity = context as Activity
                if (!activity.isFinishing && !activity.isDestroyed) {
                    try {
                        builder?.show()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    @JvmOverloads
    fun showProgressDialog(message: String?, context: Context, showClose: Int = 0) {
        if (builder == null)
            builder = Dialog(context)

        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.view_loading, null)
        builder?.setContentView(dialogView)

        dialogView.findViewById<AppCompatTextView>(R.id.txtMessage).text = message
        val imgClose = dialogView.findViewById<AppCompatImageView>(R.id.imgClose)
        builder?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
//        builder?.window?.setBackgroundDrawable(
//            ColorDrawable(Color.WHITE)
//        )
        builder?.setCanceledOnTouchOutside(false)
        builder?.window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        imgClose.visible()
        imgClose.visibility = showClose

        imgClose.setOnClickListener {
            listener?.onItemClickListener(0)
        }

        try {
            if (builder != null) {
                if (!builder!!.isShowing) {
                    builder!!.show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Loading...", Toast.LENGTH_SHORT).show()
        }


    }

    /***
     * For dismiss progress dialog
     */
    fun dismissProgressDialog() {
        try {
            if (builder != null && builder?.isShowing == true) {
                builder?.dismiss()
                builder = null
            }
        } catch (e: Exception) {
            Log.d("pos", "dismissProgressDialog: " + e.message)
        }

    }


}