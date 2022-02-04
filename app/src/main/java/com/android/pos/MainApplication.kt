package com.android.pos

import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Handler
import androidx.appcompat.app.AppCompatDelegate
import com.android.pos.utils.scanner.helpers.AvailableScanner
import com.android.pos.utils.scanner.helpers.Barcode
import com.android.pos.utils.scanner.helpers.Foreground
import com.android.pos.utils.scanner.helpers.ScannerAppEngine
import com.google.firebase.FirebaseApp
import com.testfairy.TestFairy
import com.zebra.scannercontrol.DCSScannerInfo
import com.zebra.scannercontrol.SDKHandler
import dagger.hilt.android.HiltAndroidApp
import java.util.*

@HiltAndroidApp
class MainApplication : Application() {


    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        TestFairy.begin(this, "SDK-TVuIrZk6");
        instance = this
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        Foreground.init(this)
        createNotificationChannel()
        sdkHandler = SDKHandler(this, true)

    }

    companion object {
        private var instance: MainApplication? = null
        fun getInstance(): MainApplication? {
            if (instance == null) {
                synchronized(MainApplication::class.java) {
                    if (instance == null) {
                        instance = MainApplication()
                    }
                }
            }
            return instance
        }


        //Scanners (both available and active)
        var mScannerInfoList = ArrayList<DCSScannerInfo>()
        var mDevListDelegates: ArrayList<ScannerAppEngine.IScannerAppEngineDevListDelegate>? =
            ArrayList<ScannerAppEngine.IScannerAppEngineDevListDelegate>()

        //Barcode data
        var barcodeData: ArrayList<Barcode> = ArrayList<Barcode>()
        var currentConnectedScanner: AvailableScanner? = null
        var lastConnectedScanner: AvailableScanner? = null

        //Instance of SDK Handler
        var sdkHandler: SDKHandler? = null

        //Handler to handle bluetooth events
        var globalMsgHandler: Handler? = null

        //Var to access scanner app engine
        var scannerAppEngine: ScannerAppEngine? = null

        //Settings for notifications
        var MOT_SETTING_OPMODE = 0
        var MOT_SETTING_SCANNER_DETECTION = true
        var MOT_SETTING_EVENT_ACTIVE = true
        var MOT_SETTING_EVENT_AVAILABLE = true
        var MOT_SETTING_EVENT_BARCODE = true
        var MOT_SETTING_EVENT_IMAGE = true
        var MOT_SETTING_EVENT_VIDEO = true
        var MOT_SETTING_EVENT_BINARY_DATA = true

        var MOT_SETTING_NOTIFICATION_ACTIVE = true
        var MOT_SETTING_NOTIFICATION_AVAILABLE = true
        var MOT_SETTING_NOTIFICATION_BARCODE = true
        var MOT_SETTING_NOTIFICATION_IMAGE = true
        var MOT_SETTING_NOTIFICATION_VIDEO = true
        var MOT_SETTING_NOTIFICATION_BINARY_DATA = true

        var SCANNER_ID_NONE = -1
        var currentScannerName = ""
        var currentScannerAddress = ""
        var currentScannerId = SCANNER_ID_NONE
        var currentAutoReconnectionState = true
        var isAnyScannerConnected = false //True, if currently connected to any scanner

        var currentConnectedScannerID = -1 //Track scannerId of currently connected Scanner

        var isFirmwareUpdateInProgress = false
        var intentionallyDisconnected = false
        var virtualTetherHostActivated = false

        //bluetooth mac address
        var btAddress : String? = ""

    }

    /**
     * Create the NotificationChannel to show notification in background mode
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val id = getString(R.string.virtual_tether_notification_channel_id)
            val name: CharSequence = getString(R.string.virtual_tether_notification_channel_name)
            val description = getString(R.string.virtual_tether_notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(id, name, importance)
            channel.description = description
            val notificationManager: NotificationManager? = MainApplication.getInstance()?.applicationContext?.getSystemService<NotificationManager>(
                NotificationManager::class.java
            )
            notificationManager?.createNotificationChannel(channel)
        }
    }

}