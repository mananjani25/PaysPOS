package com.android.pos.ui.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.android.pos.R
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.EMPLOYEE_ID
import com.android.pos.data.remote.Constants.ONLINE_ORDER_GET_NOTIFICATION
import com.android.pos.data.remote.Constants.SEND_CLOCKOUT_NOTIFICATION
import com.android.pos.data.remote.Constants.SYNC_FLOORPLAN
import com.android.pos.data.remote.Constants.SYNC_MARKUP
import com.android.pos.data.remote.Constants.SYNC_NOTIFICATION
import com.android.pos.data.remote.Constants.SYNC_SETTING_NOTIFICATION
import com.android.pos.di.PrefProvider
import com.android.pos.ui.activities.MainActivity
import com.android.pos.utils.LogUtil
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import javax.inject.Inject

class MyFirebaseMessagingService : FirebaseMessagingService() {
    var type = ""

    @Inject
    lateinit var prefProvider: PrefProvider

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.e(TAG, "From: ${remoteMessage.data}")

        prefProvider = PrefProvider(this)
        if (remoteMessage.data.isNotEmpty()) {
            type = remoteMessage.data["type"].toString()
            Log.e(TAG, "onMessageReceived: type : $type")
            if (prefProvider.getValueInt(EMPLOYEE_ID, -1) != -1) {
                if (type == "Clock Out") {
                    val intent = Intent()
                    intent.putExtra("isAuto", false)
                    intent.action = SEND_CLOCKOUT_NOTIFICATION
                    prefProvider.setValueboolean("clockOutFromNoti", true)
                    sendBroadcast(intent)
                } else if (type == "Auto Clockout") {
                    val intent = Intent()
                    intent.putExtra("isAuto", true)
                    intent.putExtra("message", remoteMessage.data["message"].toString())
                    intent.action = SEND_CLOCKOUT_NOTIFICATION
                    prefProvider.setValueboolean("clockOutFromNoti", true)
                    sendBroadcast(intent)
                } else if (type == "onlineorder") {//for online and third-party pending orders count
                    val intent = Intent()
                    intent.putExtra("message", remoteMessage.data["message"].toString())
                    intent.putExtra("count", remoteMessage.data["count"])
                    intent.action = ONLINE_ORDER_GET_NOTIFICATION
                    sendBroadcast(intent)
                    setSoundForOnlineOrder()
                } else if (type == "Sync") {
                    if (prefProvider.getValue(Constants.AUTH_TOKEN, "").isNotEmpty()) {
                        val intent = Intent()
                        intent.action = SYNC_NOTIFICATION
                     //   sendBroadcast(intent)
                    }
                } else if (type == "SettingData") {
                    val intent = Intent()
                    intent.action = SYNC_SETTING_NOTIFICATION
                   // sendBroadcast(intent)
                } else if (type == "MarkupSync") {

                    prefProvider.setValueboolean(Constants.IS_SYNC_MARKUP, true)
                    val intent = Intent()
                    intent.action = SYNC_MARKUP
                    sendBroadcast(intent)
                } else if (type == "DineIn") {
                    val intent = Intent()
                    intent.action = SYNC_FLOORPLAN
                    sendBroadcast(intent)

                } else {
                    /* val intent = Intent()
                     intent.putExtra("printer_queue", "rem")
                     intent.action = "PrinterQueue"
                     sendBroadcast(intent)*/

                }
            }
        }
    }


    private fun setSoundForOnlineOrder() {
        try {
            val resID = resources.getIdentifier("bell", "raw", packageName)
            val mediaPlayer: MediaPlayer = MediaPlayer.create(this, resID)
            mediaPlayer.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onNewToken(token: String) {
        LogUtil.logEN(TAG, "Refreshed token: $token")
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String?) {
        LogUtil.logEN(TAG, "sendRegistrationTokenToServer($token)")
    }

    private fun sendNotification(messageBody: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0 /* Request code */, intent,
            PendingIntent.FLAG_ONE_SHOT
        )

        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_baseline_notifications_24)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
    }

    companion object {

        private const val TAG = "MyFirebaseMsgService"
    }

    fun queuePrinterLogic(context: Context) {
        /*  val data = Data.Builder()
              .putString("kitchenPrinterList", Gson().toJson(kitchenPrinterList))
              .put("kitchenSettingData", Gson().toJson(kitchenSettingModel))
              .put("location_id", prefProvider.getValueInt(Constants.LOCATION_ID, 0))
              .put("base_url", prefProvider.getValue(Constants.BASE_URL_NEW, ""))
              .build()
    */

        /*val uploadWorkRequest =
            OneTimeWorkRequest.Builder(UploadWorker::class.java).setInputData(data).build()*/


    }
}