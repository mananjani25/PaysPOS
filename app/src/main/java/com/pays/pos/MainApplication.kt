package com.pays.pos

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import com.github.anrwatchdog.ANRWatchDog
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.pax.poslink.CommSetting
import com.pax.poslink.LogSetting
import com.pax.poslink.POSLinkAndroid
import com.pays.pos.data.remote.Constants
import com.pays.pos.logger.MessageEvent
import com.pays.pos.ui.activities.MainActivity
import com.pays.pos.utils.InternetUtils
import com.pays.pos.utils.paxUtils.Convenience
import com.pays.pos.utils.paxUtils.SettingINI
import com.pays.pos.utils.scanner.helpers.AvailableScanner
import com.pays.pos.utils.scanner.helpers.Barcode
import com.pays.pos.utils.scanner.helpers.Foreground
import com.pays.pos.utils.scanner.helpers.ScannerAppEngine
import dagger.hilt.android.HiltAndroidApp
import org.greenrobot.eventbus.EventBus
import retrofit2.HttpException
import java.io.File

@HiltAndroidApp
class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)

        /*It will not work in builds that are sent for live */
        if (Constants.isPaxInDebugMode) {
            /*ANRWatchDog().start()
            ANRWatchDog().setANRListener { error ->
                // Log or handle the ANR event
                Log.e("ANR-WatchDog", "Application Not Responding detected!", error)

                // You can also send this information to Crashlytics or another logging service
                // FirebaseCrashlytics.getInstance().recordException(error)
            }.start()*/
        }
/*ALL THE SCAN GUN VARIABLES ARE COMMENTED AND MOVED TO MAINACTIVITY(for solving permission issue), PLEASE UNCOMMENT IT AND REMOVE THE VARIABLES FROM MAINACTIVITY*/

//        CoroutineScope(Dispatchers.IO).launch {
//
//            while (true){
//            val count = Thread.getAllStackTraces().count()
//
//                val memoryInfo = Debug.MemoryInfo()
//                Debug.getMemoryInfo(memoryInfo)
//
//                val totalUsedMemoryKB = memoryInfo.totalPrivateDirty
//                val totalUsedMemoryMB = totalUsedMemoryKB / 1024.0
//                Log.e("Pays Thread Tracking","MEMORY = $totalUsedMemoryMB Pays Thread count $count")
//            }
//        }

//
//        Thread.setDefaultUncaughtExceptionHandler { paramThread, paramThrowable ->
//
//          //  Firebase.crashlytics.log("Error" + Thread.currentThread().stackTrace[2])
//            FirebaseCrashlytics.getInstance().log(paramThrowable.message+"")
//            FirebaseCrashlytics.getInstance().recordException(paramThrowable)
//
//            paramThrowable.localizedMessage?.let {
//                Log.e(
//                    "Error" + Thread.currentThread().stackTrace,
//                    it
//                )
//            }
//
//            if(paramThrowable !is com.google.android.gms.dynamite.DynamiteModule.LoadingException)
//            {
//                mainActivity?.finish()
//            }
//        }
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { paramThread, paramThrowable ->

            //  Firebase.crashlytics.log("Error" + Thread.currentThread().stackTrace[2])
            FirebaseCrashlytics.getInstance().log(paramThrowable.message + "")
            FirebaseCrashlytics.getInstance().recordException(paramThrowable)
            Log.e(
                getString(R.string.app_name),
                "Uncaught exception in thread " + paramThread.getName(),
                paramThrowable
            );

            paramThrowable.localizedMessage?.let {
                Log.e(
                    "Error" + Thread.currentThread().stackTrace,
                    it
                )
            }

            if (defaultHandler != null) {
                defaultHandler.uncaughtException(paramThread, paramThrowable);
            }
            EventBus.getDefault().post(MessageEvent("${Constants.LINE_BREAK_TAB} MainApplication.kt = ${paramThrowable.printStackTrace()}"))

            if (paramThrowable !is com.google.android.gms.dynamite.DynamiteModule.LoadingException && paramThrowable !is HttpException) {
                paramThrowable.printStackTrace()
                mainActivity?.finish()
            } else {
                if (paramThrowable is HttpException) {
                    if (!InternetUtils.isServerReachable()){
                        Toast.makeText(applicationContext, getString(R.string.server_not_reachable), Toast.LENGTH_SHORT).show()
                    }
                }
            }

        }
        /*try {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false)
        } catch (e: Exception) {
        }*/
        //bhumit.bhadani@bacancy.com = 10Ce70901@
        //TestFairy.begin(this, "SDK-SrnpgIU9"); // vishal.j.patel+103@bacancy.com/Pos@2022
        instance = this
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        Foreground.init(this)
        createNotificationChannel()
//        sdkHandler = SDKHandler(this, true)
        init()
    }

    fun init() {
        val commSetting: CommSetting = setupSetting(applicationContext)
        POSLinkAndroid.init(applicationContext, commSetting)
        Log.i("DEBUG", "Start Application")
        Convenience.init(applicationContext)
    }

    companion object {
        private var instance: MainApplication? = null
        var mainActivity: MainActivity? = null
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
//        var mScannerInfoList = ArrayList<DCSScannerInfo>()
//        var mDevListDelegates: ArrayList<ScannerAppEngine.IScannerAppEngineDevListDelegate>? =
//            ArrayList<ScannerAppEngine.IScannerAppEngineDevListDelegate>()

        //Barcode data
//        var barcodeData: ArrayList<Barcode> = ArrayList<Barcode>()
//        var currentConnectedScanner: AvailableScanner? = null
//        var lastConnectedScanner: AvailableScanner? = null

        //Instance of SDK Handler
//        var sdkHandler: SDKHandler? = null

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

//        var SCANNER_ID_NONE = -1
//        var currentScannerName = ""
//        var currentScannerAddress = ""
//        var currentScannerId = SCANNER_ID_NONE
//        var currentAutoReconnectionState = true
//        var isAnyScannerConnected = false //True, if currently connected to any scanner
//
//        var currentConnectedScannerID = -1 //Track scannerId of currently connected Scanner
//
//        var isFirmwareUpdateInProgress = false
////        var intentionallyDisconnected = false
//        var virtualTetherHostActivated = false

        //bluetooth mac address
//        var btAddress: String? = ""


        fun clearApplicationData() {
            val cache: File? = getInstance()?.cacheDir
            val appDir = File(cache?.parent)
            if (appDir.exists()) {
                val children: Array<String> = appDir.list()
                for (s in children) {
                    if (s != "lib") {
                        deleteDir(File(appDir, s))
                        Log.i("TAG", "File /data/data/APP_PACKAGE/$s DELETED")
                    }
                }
            }
        }

        private fun deleteDir(dir: File?): Boolean {
            if (dir != null && dir.isDirectory) {
                val children: Array<String> = dir.list()
                for (i in children.indices) {
                    val success = deleteDir(File(dir, children[i]))
                    if (!success) {
                        return false
                    }
                }
            }
            return dir?.delete() ?: false
        }


    }

    /**
     * Create the NotificationChannel to show notification in background mode
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val id = getString(R.string.default_notification_channel_id)
            val name: CharSequence = getString(R.string.virtual_tether_notification_channel_name)
            val description = getString(R.string.virtual_tether_notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(id, name, importance)
            channel.description = description
            val notificationManager: NotificationManager? =
                MainApplication.getInstance()?.applicationContext?.getSystemService<NotificationManager>(
                    NotificationManager::class.java
                )
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun setupSetting(context: Context): CommSetting {
        val settingIniFile = context.filesDir.absolutePath + "/" + SettingINI.FILENAME
        val commSetting: CommSetting = SettingINI.getCommSettingFromFile(context,settingIniFile)
        disableProxyForThisVersion(commSetting, settingIniFile)

        //initialization value  for comsetting's attribute
        commSetting.type = CommSetting.TCP
        commSetting.timeOut = "-1"
        commSetting.baudRate = "9600"
//        commSetting.serialPort = "COM1"
        commSetting.isEnableProxy = false
        commSetting.macAddr = ""
        commSetting.destIP = "127.0.0.1"
        commSetting.destPort = "10009"
        /*val selectedHost = "UNKNOWN"
        Convenience.setHost(context, commSetting, selectedHost)*/
        SettingINI.saveCommSettingToFile(context, settingIniFile, commSetting)

        Log.i(
            "TAG", "coms.CommType = " + commSetting.type + "; coms.TimeOut=" + commSetting.timeOut
                    + "; SerialPort=" + commSetting.serialPort + "; coms.BaudRate=" + commSetting.baudRate
                    + "; coms.DestIP=" + commSetting.destIP + "; coms.DestPort=" + commSetting.destPort + "; coms.MacAddr=" + commSetting.macAddr + "; coms.EnableProxy=" + commSetting.isEnableProxy
        )

        if (!SettingINI.loadSettingFromFile(settingIniFile)) {
            //String LogOutputFile = getApplicationContext().getFilesDir().getAbsolutePath() + "/POSLog.txt";
            val LogOutputFile = context.getExternalFilesDir(null)!!.path
            LogSetting.setLogMode(true)
            LogSetting.setLevel(LogSetting.LOGLEVEL.DEBUG)
            LogSetting.setOutputPath(LogOutputFile)
            SettingINI.saveLogSettingToFile(settingIniFile)
        }
        return SettingINI.getCommSettingFromFile(context!!,settingIniFile)
    }

    protected fun disableProxyForThisVersion(commSetting: CommSetting, settingIniFile: String) {
        commSetting.isEnableProxy = false
        SettingINI.saveCommSettingToFile(applicationContext, settingIniFile, commSetting)
    }

}