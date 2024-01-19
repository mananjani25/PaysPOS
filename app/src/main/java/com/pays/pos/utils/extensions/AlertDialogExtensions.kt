package com.pays.pos.utils.extensions

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.pays.pos.R

inline fun Activity.alert(
    title: CharSequence? = null,
    message: CharSequence? = null,
    func: AlertDialogHelper.() -> Unit
): AlertDialog {
    return AlertDialogHelper(this, title, message).apply {
        func()
    }.create()
}

inline fun Activity.alert(
    titleResource: Int = 0,
    messageResource: Int = 0,
    func: AlertDialogHelper.() -> Unit
): AlertDialog {
    val title = if (titleResource == 0) null else getString(titleResource)
    val message = if (messageResource == 0) null else getString(messageResource)
    return AlertDialogHelper(this, title, message).apply {
        func()
    }.create()
}

inline fun Fragment.alert(
    title: CharSequence? = null,
    message: CharSequence? = null,
    func: AlertDialogHelper.() -> Unit
): AlertDialog {
    return AlertDialogHelper(context, "", message).apply {
        func()
    }.create()
}

inline fun Fragment.alert(
    titleResource: Int = 0,
    messageResource: Int = 0,
    func: AlertDialogHelper.() -> Unit
): AlertDialog {
    val title = if (titleResource == 0) null else getString(titleResource)
    val message = if (messageResource == 0) null else getString(messageResource)
    return AlertDialogHelper(context, title, message).apply {
        func()
    }.create()
}


class AlertDialogHelper(context: Context?, title: CharSequence?, message: CharSequence?) {

    private val dialogView: View by lazy {
        LayoutInflater.from(context).inflate(R.layout.view_custom_dialog, null)
    }

    private val builder: AlertDialog.Builder = AlertDialog.Builder(context!!)
        .setView(dialogView)

    private val title: AppCompatTextView by lazy {
        dialogView.findViewById(R.id.tvTitle)
    }

    private val message: AppCompatTextView by lazy {
        dialogView.findViewById(R.id.tvMessage)
    }

    private val positiveButton: AppCompatTextView by lazy {
        dialogView.findViewById(R.id.tvSave)
    }

    private val negativeButton: AppCompatTextView by lazy {
        dialogView.findViewById(R.id.tvCancel)
    }

    private var dialog: AlertDialog? = null

    var cancelable: Boolean = true

    init {
        this.title.text = title
        this.message.text = message
    }

    fun positiveButton(@StringRes textResource: Int, func: (() -> Unit)? = null) {
        with(positiveButton) {
            text = builder.context.getString(textResource)
            setClickListenerToDialogButton(func)
        }
    }

    fun positiveButton(text: CharSequence, func: (() -> Unit)? = null) {
        with(positiveButton) {
            this.text = text
            setClickListenerToDialogButton(func)
        }
    }

    fun negativeButton(@StringRes textResource: Int, func: (() -> Unit)? = null) {
        with(negativeButton) {
            text = builder.context.getString(textResource)
            setClickListenerToDialogButton(func)
        }
    }

    fun negativeButton(text: CharSequence, func: (() -> Unit)? = null) {
        with(negativeButton) {
            this.text = text
            setClickListenerToDialogButton(func)
        }
    }

    fun onCancel(func: () -> Unit) {
        builder.setOnCancelListener { func() }
    }

    fun create(): AlertDialog {
        title.goneIfTextEmpty()
        message.goneIfTextEmpty()
        positiveButton.goneIfTextEmpty()
        negativeButton.goneIfTextEmpty()

        dialog = builder
            .setCancelable(cancelable)
            .show()
        return dialog!!
    }

    private fun AppCompatTextView.goneIfTextEmpty() {
        visibility = if (text.isNullOrEmpty()) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    private fun AppCompatTextView.setClickListenerToDialogButton(func: (() -> Unit)?) {
        setOnClickListener {
            func?.invoke()
            dialog?.dismiss()
        }
    }


    /**
     * Call this method (in onActivityCreated or later)
     * to make the dialog near-full screen.
     */
    fun DialogFragment.setFullScreen() {
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}