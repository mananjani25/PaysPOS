package com.pays.pos.utils.recievers

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.pays.pos.MainApplication
import com.pays.pos.R
import com.pays.pos.ui.activities.MainActivity
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.scanner.helpers.Constants

/**
 * This class acts as a receiver for the Scanner Events when the app is running in the background.
 * It adds the message as a notification.
 */
class NotificationsReceiver : BroadcastReceiver() {
    private val notificationIcon: Int
        private get() = R.drawable.ic_launcher_foreground

    override fun onReceive(context: Context, intent: Intent) {

        LogUtil.logE("NotificationsReceiver", " called------ ")
        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        //TODO : Add support for stacked notifications(multiple Scanner events occur when the app is in background)
        val mBuilder = NotificationCompat.Builder(context).setSmallIcon(
            notificationIcon
        ).setContentTitle("Scanner Control").setAutoCancel(true)
            .setContentText(intent.getStringExtra(Constants.NOTIFICATIONS_TEXT))
        val notificationText = intent.getStringExtra(Constants.NOTIFICATIONS_TEXT)
        val notificationType = intent.getIntExtra(Constants.NOTIFICATIONS_TYPE, 0)
        var resultIntent = Intent(context, MainActivity::class.java)
        //TODO scanner
        /* if (notificationType == Constants.BARCODE_RECEIVED) {
             resultIntent = Intent(context, ActiveScannerActivity::class.java)
             resultIntent.putExtra(Constants.SHOW_BARCODE_VIEW, true)
         }
         if (notificationType == Constants.SESSION_ESTABLISHED) {
             resultIntent = Intent(context, ActiveScannerActivity::class.java)
             resultIntent.putExtra(Constants.SHOW_BARCODE_VIEW, false)
         }
         if (notificationType == Constants.SESSION_TERMINATED) {
             resultIntent = Intent(context, HomeActivity::class.java)
         }
         if (notificationType == Constants.SCANNER_APPEARED) {
             resultIntent = Intent(context, HomeActivity::class.java)
         }
         if (notificationType == Constants.SCANNER_DISAPPEARED) {
             resultIntent = Intent(context, HomeActivity::class.java)
         }*/
        resultIntent.putExtra(Constants.SCANNER_ID, intent.getIntExtra(Constants.SCANNER_ID, -1))
        resultIntent.putExtra(Constants.SCANNER_NAME, MainApplication.currentScannerName)
        resultIntent.putExtra(Constants.SCANNER_ADDRESS, MainApplication.currentScannerAddress)
        resultIntent.putExtra(Constants.SCANNER_ID, MainApplication.currentScannerId)
        resultIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        resultIntent.putExtra(
            Constants.AUTO_RECONNECTION,
            MainApplication.currentAutoReconnectionState
        )
        val stackBuilder = TaskStackBuilder.create(context)
        // Adds the back stack
        //TODO scanner
        //stackBuilder.addParentStack(ActiveScannerActivity::class.java)
        // Adds the Intent to the top of the stack
        stackBuilder.addNextIntent(resultIntent)
        // Gets a PendingIntent containing the entire back stack
        val resultPendingIntent =
            stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        mBuilder.setContentIntent(resultPendingIntent)

        //Notify with the notification ID
        mgr?.notify(
            intent.getIntExtra(
                Constants.NOTIFICATIONS_ID,
                DEFAULT_NOTIFICATION_ID
            ), mBuilder.build()
        )
    }

    companion object {
        //Default Notification ID
        var DEFAULT_NOTIFICATION_ID = 1
    }
}