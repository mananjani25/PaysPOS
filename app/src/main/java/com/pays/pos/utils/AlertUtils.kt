package com.pays.pos.utils

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.telephony.PhoneNumberUtils
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.pays.pos.R
import java.nio.file.Files.delete
import java.util.*


/**
 * CommonUtils class
 *
 *
 *
 *
 * This is util class for Alert dialog. All customization regarding alert are managed in this class
 *
 *
 * @author Mohit Kanada
 */
object AlertUtils {
    fun showAlert(
        context: Context,
        message: String?
    ): AlertDialog {
        val builder = AlertDialog.Builder(context)
        builder.setIcon(0)
        setAlertTitle(context, builder)
        //		builder.setTitle(context.getString(R.string.alert));
        builder.setMessage(message)
        builder.setPositiveButton(context.getString(android.R.string.ok), null)
        val dialog = builder.show()
        changeDefaultColor(dialog, context)
        return dialog
    }

    private fun setAlertTitle(
        context: Context,
        builder: AlertDialog.Builder
    ) {
        val title = TextView(context)
        title.text = context.getString(R.string.app_name)
        title.setPadding(15, 10, 15, 0);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30f)
        title.setTypeface(title.getTypeface(), Typeface.BOLD)
        title.setTextColor(context.resources.getColor(R.color.btnColorDark))
        builder.setCustomTitle(title)
    }

    /***
     * Show simple alert dialog with title 'Alert'
     * @param context Activity context
     * @param message Message
     * @return AlertDialog
     */
    fun showSimpleAlert(
        context: Context,
        title: String?,
        message: String?
    ): AlertDialog {
        val builder = AlertDialog.Builder(context)
        builder.setIcon(0)
        setAlertTitle(context, builder)
        //		builder.setTitle(context.getString(R.string.alert));
        builder.setMessage(message)
        builder.setNeutralButton(context.getString(android.R.string.ok), null)
        val dialog = builder.show()
        changeDefaultColor(dialog, context)
        return dialog
    }

    fun showSimpleSingleAlert(
        context: Context, title: String?,
        message: String?, listener: DialogInterface.OnClickListener?
    ): AlertDialog {
        val builder = AlertDialog.Builder(context)
        builder.setIcon(0)
        builder.setTitle(title)
        builder.setMessage(message)
        //builder.setNeutralButton(context.getString(R.string.ok), null);
        builder.setNeutralButton(context.getString(android.R.string.ok), listener)
        val dialog = builder.show()
        changeDefaultColor(dialog, context)
        return dialog
    }

    fun showAlertWithListener(
        context: Context,
        message: String?,
        listener: (Any, Any) -> Unit
    ): AlertDialog {
        val builder = AlertDialog.Builder(context)
        builder.setIcon(0)
        builder.setCancelable(false)
        setAlertTitle(context, builder)
        builder.setMessage(message)
        builder.setPositiveButton(context.getString(android.R.string.ok), listener)
        val dialog = builder.show()
        changeDefaultColor(dialog, context)
        return dialog
    }

    /***
     * Show simple alert dialog
     * @param context Activity context
     * @param title Title
     * @param message Message
     * @param listener DialogInterface.OnClickListener for ok button click
     * @return AlertDialog
     */
    fun showSimpleAlert(
        context: Context, title: String?,
        message: String?, buttontitle: String,
        listener: DialogInterface.OnClickListener?
    ): AlertDialog {
        val builder = AlertDialog.Builder(context)
        builder.setIcon(0)
        builder.setTitle(title)
        builder.setMessage(message)
        if (buttontitle == "") {
            builder.setNeutralButton(context.getString(android.R.string.ok), listener)
        } else {
            builder.setNeutralButton(buttontitle, listener)
        }
        builder.setOnCancelListener { dialog -> listener?.onClick(dialog, 0) }
        val dialog = builder.show()
        changeDefaultColor(dialog, context)
        return dialog
    }

    fun showToast(context: Context?, message: String?) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    /***
     * Show alert dialog for confirm some action
     * @param context Activity context
     * @param title Title
     * @param message Message
     * @param onYesClick DialogInterface.OnClickListener for positive button click
     * @return AlertDialog
     */
    fun showConfirmAlert(
        context: Context,
        message: String?, onYesClick: (Any, Any) -> Unit
    ): AlertDialog {
        val builder = AlertDialog.Builder(context)
        builder.setIcon(0)
        builder.setTitle(context.getString(R.string.app_name))
        builder.setMessage(message)
        builder.setPositiveButton(context.getString(android.R.string.yes), onYesClick)
        builder.setNegativeButton(context.getString(android.R.string.no), null)
        val dialog = builder.show()
        //  changeDefaultColor(dialog)
        return dialog
    }


    fun showUpdateAppDialog(context: Context): AlertDialog {
        val builder = AlertDialog.Builder(context)
        builder.setIcon(0)
        builder.setTitle("Update App")
        builder.setMessage("A new version is available on Play Store. Do you want to update it now?")
        builder.setPositiveButton("Update Now") { dialog, which ->
            val appPackageName =
                context.packageName // getPackageName() from Context or Activity object
            try {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=$appPackageName")
                    )
                )
            } catch (anfe: ActivityNotFoundException) {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")
                    )
                )
            }
        }
        builder.setNegativeButton("Later", null)
        val dialog = builder.show()
        changeDefaultColor(dialog, context)
        return dialog
    }

    /***
     * Show alert dialog with list of options
     * @param context Activity context
     * @param title Title
     * @param items String array of all list options
     * @param onItemClick DialogInterface.OnClickListener for detect every item click
     * @return AlertDialog
     */
    fun showSingleChoiceListAlert(
        context: Context,
        title: String?,
        items: Array<String?>?,
        selected: Int,
        onItemClick: DialogInterface.OnClickListener?
    ): AlertDialog {
        val builder = AlertDialog.Builder(context)
        builder.setIcon(0)
        builder.setTitle(title)
        //        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,android.R.layout.simple_list_item_1,items);
        builder.setSingleChoiceItems(
            items,
            selected
        ) { dialog, which -> // TODO Auto-generated method stub
            dialog?.dismiss()
            onItemClick?.onClick(dialog, which)
        }
        builder.setNegativeButton(context.getString(android.R.string.cancel), null)
        val dialog = builder.show()
        changeDefaultColor(dialog, context)
        return dialog
    }

    /***
     * Show alert dialog with list of options
     * @param context Activity context
     * @param title Title
     * @param items String array of all list options
     * @param onOkClickListener DialogInterface.OnClickListener for detect ok item click
     * @return AlertDialog
     */
    fun showMultiChoiceListAlert(
        context: Context, title: String?,
        items: Array<String?>?, checkeItems: BooleanArray,
        onOkClickListener: DialogInterface.OnClickListener?
    ): AlertDialog {
        val builder = AlertDialog.Builder(context)
        builder.setIcon(0)
        builder.setTitle(title)
        //        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,android.R.layout.simple_list_item_1,items);
        builder.setMultiChoiceItems(
            items,
            checkeItems
        ) { dialog, which, isChecked -> checkeItems[which] = isChecked }
        builder.setNegativeButton(context.getString(android.R.string.cancel), null)
        builder.setPositiveButton(context.getString(android.R.string.ok), onOkClickListener)
        val dialog = builder.show()
        changeDefaultColor(dialog, context)
        return dialog
    }

    /***
     * Show alert dialog with list of options
     * @param context Activity context
     * @param title Title
     * @param items String array of all list options
     * @param onItemClick DialogInterface.OnClickListener for detect every item click
     * @return AlertDialog
     */
    fun showListAlert(
        context: Context, title: String?,
        items: Array<String?>, onItemClick: DialogInterface.OnClickListener?
    ): AlertDialog {
        val builder = AlertDialog.Builder(context)
        builder.setIcon(0)
        builder.setTitle(title)
        val adapter =
            ArrayAdapter(context, android.R.layout.simple_list_item_1, items)
        builder.setAdapter(adapter) { dialog, which -> // TODO Auto-generated method stub
            onItemClick?.onClick(dialog, which)
        }
        builder.setNegativeButton(context.getString(android.R.string.cancel), null)
        val dialog = builder.show()
        changeDefaultColor(dialog, context)
        return dialog
    }

    /***
     * Show alert dialog with list of options
     * @param context Activity context
     * @param title Title
     * @param adapter List adapter for show customized list row
     * @param onItemClick DialogInterface.OnClickListener for detect every item click
     * @return AlertDialog
     */
    fun showListAlert(
        context: Context, title: String?,
        adapter: ListAdapter?, onItemClick: DialogInterface.OnClickListener?
    ): AlertDialog {
        val builder = AlertDialog.Builder(context)
        builder.setIcon(0)
        builder.setTitle(title)
        builder.setAdapter(adapter) { dialog, which -> // TODO Auto-generated method stub
            onItemClick?.onClick(dialog, which)
        }
        builder.setNegativeButton(context.getString(android.R.string.cancel), null)
        val dialog = builder.show()
        changeDefaultColor(dialog, context)
        return dialog
    }

    /***
     * Change default color theme for Alert dialog
     * @param dialog AlertDialog
     */
    fun changeDefaultColor(dialog: AlertDialog, context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // only for gingerbread and newer versions

                val tvMessage = dialog.getWindow()?.findViewById<TextView>(android.R.id.message)
                tvMessage?.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25f)
                tvMessage?.setPadding(15, 0, 15, 0)

                val typeface: Typeface? =
                    ResourcesCompat.getFont(context, R.font.sf_pro_display_regular)

                tvMessage?.setTypeface(typeface)

                var b = dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
                b?.setTextColor(
                    ContextCompat.getColor(
                        dialog.context,
                        R.color.btnColorDark
                    )
                )
                b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30f)
                b?.setPadding(0, 0, 15, 10)
                b = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                b?.setTextColor(
                    ContextCompat.getColor(
                        dialog.context,
                        R.color.btnColorDark
                    )
                )
                b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30f)
                b?.setPadding(0, 0, 15, 10)
                b = dialog.getButton(DialogInterface.BUTTON_NEUTRAL)
                b?.setTextColor(
                    ContextCompat.getColor(
                        dialog.context,
                        R.color.btnColorDark
                    )
                )
                b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30f)
                b?.setPadding(0, 0, 15, 10)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                val decorView = dialog.window
                    ?.decorView as ViewGroup
                val windowContentView = decorView
                    .getChildAt(0) as FrameLayout
                val contentView = windowContentView
                    .getChildAt(0) as FrameLayout
                val parentPanel = contentView
                    .getChildAt(0) as LinearLayout
                val topPanel = parentPanel
                    .getChildAt(0) as LinearLayout
                val titleDivider = topPanel.getChildAt(2)
                val titleTemplate = topPanel
                    .getChildAt(1) as LinearLayout
                val alertTitle =
                    titleTemplate.getChildAt(1) as androidx.appcompat.widget.AppCompatTextView
                val textColor = ContextCompat.getColor(
                    dialog.context,
                    R.color.btnColorDark
                )
                alertTitle.setTextColor(textColor)
                val primaryColor = ContextCompat.getColor(
                    dialog.context,
                    R.color.btnColorDark
                )
                titleDivider.setBackgroundColor(primaryColor)
            }
        } catch (e: Exception) {
            // e.printStackTrace();
        }
    }

    fun isEmailValid(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun usNumberFormat(phone: String): String? {

        if (phone.isEmpty()) return ""

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            PhoneNumberUtils.formatNumber(phone, Locale.getDefault().country);
        } else {
            PhoneNumberUtils.formatNumber(phone);
        }
    }

    fun showCustomAlert(
        context: Context,
        message: String?
    ) {
        if (message == null || message.equals("null", ignoreCase = true)) {
            return
        }
        val dialogView = LayoutInflater.from(context).inflate(R.layout.view_custom_dialog, null)
        val customDialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .show()
        val tvTitle = dialogView.findViewById<AppCompatTextView>(R.id.tvTitle)
        val tvSubTitle = dialogView.findViewById<AppCompatTextView>(R.id.tvMessage)

        tvTitle.text = context.getString(R.string.app_name)
        tvSubTitle.text = message

        val btSave = dialogView.findViewById<AppCompatTextView>(R.id.tvSave)
        btSave.text = context.getString(android.R.string.ok)
        btSave.setBackgroundColor(ContextCompat.getColor(context, R.color.btnColorDark))
        btSave.setOnClickListener {
            customDialog.dismiss()
        }
        val btDismiss = dialogView.findViewById<AppCompatTextView>(R.id.tvCancel)
        btDismiss.visibility = View.GONE

    }

    fun showCustomAlertWithListener(
        context: Context,
        message: String?,
        listener: DialogInterface.OnClickListener?
    ) {
        if (message == null || message.equals("null", ignoreCase = true)) {
            return
        }
        val dialogView = LayoutInflater.from(context).inflate(R.layout.view_custom_dialog, null)
        val customDialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .show()
        val tvTitle = dialogView.findViewById<AppCompatTextView>(R.id.tvTitle)
        val tvSubTitle = dialogView.findViewById<AppCompatTextView>(R.id.tvMessage)

        tvTitle.text = context.getString(R.string.app_name)
        tvSubTitle.text = message

        val btSave = dialogView.findViewById<AppCompatTextView>(R.id.tvSave)
        btSave.text = context.getString(R.string.tv_delete)
        btSave.setBackgroundColor(ContextCompat.getColor(context, R.color.colorRed))
        btSave.setOnClickListener {
            customDialog.dismiss()
            listener?.onClick(customDialog, 0)
        }
        val btDismiss = dialogView.findViewById<AppCompatTextView>(R.id.tvCancel)
        btDismiss.visibility = View.VISIBLE
        btDismiss.setOnClickListener {
            customDialog.dismiss()
        }

    }

    fun showCustomAlertWithYesNoListener(
        context: Context?,
        message: String?,
        listener: DialogInterface.OnClickListener?
    ) {
        if (message == null || message.equals("null", ignoreCase = true)) {
            return
        }
        val dialogView = LayoutInflater.from(context).inflate(R.layout.view_custom_dialog, null)
        val customDialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .show()
        val tvTitle = dialogView.findViewById<AppCompatTextView>(R.id.tvTitle)
        val tvSubTitle = dialogView.findViewById<AppCompatTextView>(R.id.tvMessage)

        tvTitle.text = context?.getString(R.string.app_name)
        tvSubTitle.text = message

        val btSave = dialogView.findViewById<AppCompatTextView>(R.id.tvSave)
        btSave.text = context?.getString(R.string.yes)
        context?.let { ContextCompat.getColor(it, R.color.colorRed) }
            ?.let { btSave.setBackgroundColor(it) }
        btSave.setOnClickListener {
            customDialog.dismiss()
            listener?.onClick(customDialog, 0)
        }
        val btDismiss = dialogView.findViewById<AppCompatTextView>(R.id.tvCancel)
        btDismiss.text = context?.getString(R.string.no)
        btDismiss.visibility = View.VISIBLE
        btDismiss.setOnClickListener {
            customDialog.dismiss()
        }

    }

    fun showCustomAlertWithListenerWithOK(
        context: Context,
        message: String?,
        listener: DialogInterface.OnClickListener?
    ) {
        if (message == null || message.equals("null", ignoreCase = true)) {
            return
        }
        val dialogView = LayoutInflater.from(context).inflate(R.layout.view_custom_dialog_ok, null)
        val customDialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(false)
            .show()
        val tvTitle = dialogView.findViewById<AppCompatTextView>(R.id.tvTitle)
        val tvSubTitle = dialogView.findViewById<AppCompatTextView>(R.id.tvMessage)

        tvTitle.text = context.getString(R.string.app_name)
        tvSubTitle.text = message

        val btSave = dialogView.findViewById<AppCompatTextView>(R.id.tvSave)
        btSave.text = context.getString(android.R.string.ok)
        btSave.setBackgroundColor(ContextCompat.getColor(context, R.color.btnColorDark))
        btSave.setOnClickListener {
            customDialog.dismiss()
            listener?.onClick(customDialog, 0)
        }
        /* val btDismiss = dialogView.findViewById<AppCompatTextView>(R.id.tvCancel)
         btDismiss.visibility = View.VISIBLE
         btDismiss.setOnClickListener {
             customDialog.dismiss()
         }*/

    }

    fun showCustomAlertWithTitleListenerWithOK(
        context: Context,
        title: String? = context.getString(R.string.app_name),
        message: String?,
        listener: DialogInterface.OnClickListener?
    ) {
        if (message == null || message.equals("null", ignoreCase = true)) {
            return
        }
        val dialogView = LayoutInflater.from(context).inflate(R.layout.view_custom_dialog_with_title_and_ok, null)
        val customDialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(false)
            .show()
        val tvTitle = dialogView.findViewById<AppCompatTextView>(R.id.tvTitle)
        val tvSubTitle = dialogView.findViewById<AppCompatTextView>(R.id.tvMessage)

        tvTitle.text = title
        tvSubTitle.text = message

        val btSave = dialogView.findViewById<AppCompatTextView>(R.id.tvSave)
        btSave.text = context.getString(android.R.string.ok)
        btSave.setBackgroundColor(ContextCompat.getColor(context, R.color.btnColorDark))
        btSave.setOnClickListener {
            customDialog.dismiss()
            listener?.onClick(customDialog, 0)
        }
        /* val btDismiss = dialogView.findViewById<AppCompatTextView>(R.id.tvCancel)
         btDismiss.visibility = View.VISIBLE
         btDismiss.setOnClickListener {
             customDialog.dismiss()
         }*/

    }

    fun showCustomAlertClearTableDineIn(
        context: Context,
        title: String? = context.getString(R.string.app_name),
        onButtonClicked: (action: String) -> Unit
    ) {

        val dialogView = LayoutInflater.from(context).inflate(R.layout.view_custom_dialog_clear_table_dine_in, null)
        val customDialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(false)
            .show()

        val tvTitle = dialogView.findViewById<AppCompatTextView>(R.id.tvTitle)
        val tvMessage = dialogView.findViewById<AppCompatTextView>(R.id.tvMessage)
        val btnClearTable = dialogView.findViewById<AppCompatTextView>(R.id.tvSave)
        val btnCancel = dialogView.findViewById<AppCompatTextView>(R.id.tvCancel)

        tvTitle.text = title

        btnClearTable.text = context.getString(R.string.clear_table)
        btnCancel.text = context.getString(android.R.string.cancel)
        tvMessage.text = "Do you want to clear the table ?"

        btnClearTable.setBackgroundColor(ContextCompat.getColor(context, R.color.btnColorDark))
        btnCancel.visibility = View.VISIBLE

        btnClearTable.setOnClickListener {
            customDialog.dismiss()
            onButtonClicked.invoke("Clear Table")
        }

        btnCancel.setOnClickListener {
            customDialog.dismiss()
            onButtonClicked.invoke("Cancel")
        }
    }


    fun showCustomAlertWithTitleListenerWithRefresh(
        context: Context,
        title: String? = context.getString(R.string.app_name),
        message: String?,
        listener: DialogInterface.OnClickListener?
    ) {
        if (message == null || message.equals("null", ignoreCase = true)) {
            return
        }
        val dialogView = LayoutInflater.from(context).inflate(R.layout.view_custom_dialog_with_title_and_ok, null)
        val customDialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(false)
            .show()
        val tvTitle = dialogView.findViewById<AppCompatTextView>(R.id.tvTitle)
        val tvSubTitle = dialogView.findViewById<AppCompatTextView>(R.id.tvMessage)

        tvTitle.text = title
        tvSubTitle.text = message

        tvSubTitle.textSize=15f
        val btSave = dialogView.findViewById<AppCompatTextView>(R.id.tvSave)
        btSave.text = context.getString(R.string.refresh)
        btSave.setBackgroundColor(ContextCompat.getColor(context, R.color.btnColorDark))
        btSave.setOnClickListener {
            customDialog.dismiss()
            listener?.onClick(customDialog, 0)
        }
        /* val btDismiss = dialogView.findViewById<AppCompatTextView>(R.id.tvCancel)
         btDismiss.visibility = View.VISIBLE
         btDismiss.setOnClickListener {
             customDialog.dismiss()
         }*/

    }

    fun showCustomAlertWithListenerWithOKCancel(
        context: Context,
        message: String?,
        okayButtonText: String? = null,
        listener: DialogInterface.OnClickListener?
    ) {
        val dialogView =
            LayoutInflater.from(context).inflate(R.layout.view_custom_dialog_ok_cancel, null)
        val customDialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(false)
            .show()
        val tvTitle = dialogView.findViewById<AppCompatTextView>(R.id.tvTitle)
        val tvSubTitle = dialogView.findViewById<AppCompatTextView>(R.id.tvMessage)

        tvTitle.text = context.getString(R.string.app_name)
        tvSubTitle.text = message

        val btSave = dialogView.findViewById<AppCompatTextView>(R.id.tvSave)
        btSave.text = okayButtonText ?: context.getString(android.R.string.ok)
        btSave.setBackgroundColor(ContextCompat.getColor(context, R.color.btnColorDark))
        btSave.setOnClickListener {
            customDialog.dismiss()
            listener?.onClick(customDialog, 0)
        }
        val btDismiss = dialogView.findViewById<AppCompatTextView>(R.id.tvCancel)
        btDismiss.text = context.getString(R.string.cancel)
        btDismiss.setOnClickListener {
            customDialog.dismiss()
        }

    }

    fun showCustomAlertWithListenerWithOKCancelUpdated(
        context: Context,
        message: String?,
        okayButtonText: String?,
        listener: DialogInterface.OnClickListener?
    ) {
        val dialogView =
            LayoutInflater.from(context).inflate(R.layout.view_custom_dialog_ok_cancel, null)
        val customDialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(false)
            .show()
        val tvTitle = dialogView.findViewById<AppCompatTextView>(R.id.tvTitle)
        val tvSubTitle = dialogView.findViewById<AppCompatTextView>(R.id.tvMessage)

        tvTitle.text = context.getString(R.string.app_name)
        tvSubTitle.text = message

        val btSave = dialogView.findViewById<AppCompatTextView>(R.id.tvSave)
        btSave.text = okayButtonText
        btSave.setBackgroundColor(ContextCompat.getColor(context, R.color.btnColorDark))
        btSave.setOnClickListener {
            customDialog.dismiss()
            listener?.onClick(customDialog, 0)
        }
        val btDismiss = dialogView.findViewById<AppCompatTextView>(R.id.tvCancel)
        btDismiss.text = context.getString(R.string.cancel)
        btDismiss.setOnClickListener {
            customDialog.dismiss()
        }

    }
}