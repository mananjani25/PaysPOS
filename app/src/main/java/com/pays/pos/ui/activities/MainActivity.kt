package com.pays.pos.ui.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.epson.epos2.ConnectionListener
import com.epson.epos2.printer.Printer
import com.epson.epos2.printer.PrinterStatusInfo
import com.epson.epos2.printer.ReceiveListener
import com.epson.epos2.printer.StatusChangeListener
import com.epson.eposprint.Builder
import com.felhr.usbserial.BuildConfig.APPLICATION_ID
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.hosopy.actioncable.ActionCable
import com.hosopy.actioncable.Channel
import com.hosopy.actioncable.Consumer
import com.hosopy.actioncable.Subscription
import com.pays.pos.MainApplication
import com.pays.pos.R
import com.pays.pos.data.model.GuestAttrQueue
import com.pays.pos.data.model.PrinterJSONElementData
import com.pays.pos.data.model.PrinterQueueModel
import com.pays.pos.data.model.TmpPrinterModel
import com.pays.pos.data.model.responseModel.CreateOrderResponse
import com.pays.pos.data.model.responseModel.GetKitchenReceiptSettingsResponse
import com.pays.pos.data.model.responseModel.PrinterResponse
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.IS_MASTER_TERMINAL
import com.pays.pos.data.remote.Constants.IS_PRINTER_QUEUE_ENABLE
import com.pays.pos.data.remote.Constants.LOCATION_ID
import com.pays.pos.data.remote.Constants.PRINTER_QUEUE_BACKGROUND
import com.pays.pos.data.remote.Constants.UNIQUE_ID
import com.pays.pos.data.remote.Constants.checkUploadWorker
import com.pays.pos.data.repositories.UserRepository
import com.pays.pos.databinding.ParentActivityBinding
import com.pays.pos.di.ApiModule.BASE_URL
import com.pays.pos.di.HostSelectionInterceptor
import com.pays.pos.di.PrefProvider
import com.pays.pos.di.RolePermission
import com.pays.pos.service.KioskService
import com.pays.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.pays.pos.ui.fragments.dashboard.bolddashboard.CustomDisplay
import com.pays.pos.ui.fragments.dashboard.bolddashboard.DashboardCategoryBoldPOS
import com.pays.pos.ui.fragments.dinein.DineInOrderTableViewModel
import com.pays.pos.ui.fragments.loginscreen.PasscodeViewModel
import com.pays.pos.ui.fragments.payment.OrderCompleteViewModel
import com.pays.pos.ui.fragments.settings.hardware.Hardware
import com.pays.pos.ui.fragments.settings.hardware.printer.UpdatePrinters
import com.pays.pos.utils.*
import com.pays.pos.utils.FileUtils
import com.pays.pos.utils.extensions.alert
import com.pays.pos.utils.statusUtils.Status
import com.pays.pos.utils.workmanager.ThreadPoolManager
import com.pays.pos.utils.workmanager.UploadWorker2
import com.sunmi.externalprinterlibrary2.ConnectCallback
import com.sunmi.externalprinterlibrary2.ResultCallback
import com.sunmi.externalprinterlibrary2.printer.CloudPrinter
import com.sunmi.externalprinterlibrary2.printer.CloudPrinterBuilder
import com.sunmi.externalprinterlibrary2.style.AlignStyle
import com.sunmi.externalprinterlibrary2.style.CloudPrinterStatus
import com.sunmi.externalprinterlibrary2.style.UnderlineStyle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.net.URI
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : BaseScannerActivity(), ReceiveListener, ConnectionListener,
    StatusChangeListener, UpdatePrinters, ResultCallback, ComponentCallbacks2 {
    private var isLocalMasterFlag: Boolean = false
    var currentPrinterIndex = 0
    var currentOrderIndex = 0
    var lastSyncTime: Long = 0L
    private var currentCloudPrinter: CloudPrinter? = null
    private var localCallConnect: Boolean = false
    private var previousPrinterAddress = ""
    private var previousPrinterName = ""
    private var connectionCounter: Int = 0
    private var disconnectSize0: Boolean = false
    private var globalPrinterQueue: JsonElement? = null
    var listOfPrintersData: ArrayList<PrinterJSONElementData> = arrayListOf()
    var isQueueRunning: Boolean = false
    private var isPrinterRunning: Boolean = false
    private var reConnectCount: Int = 0
    private var printerBGRunning: Boolean = false
    private var isCancelWork: Boolean = true
    private var printerList: List<PrinterResponse.Data.KitchenReceiptPrinters> = arrayListOf()
    private val dashboardViewModel: DashBoardCategoryViewModel by viewModels()
    private val passcodeViewModel: PasscodeViewModel by viewModels()
    private var globalListPrinters: ArrayList<TmpPrinterModel> = arrayListOf()
    private var printerQueueModelGlobal: PrinterQueueModel? = null
    private var isPrinterQueueRun: Boolean = false
    private var kitchenPrinterList: List<PrinterResponse.Data.KitchenReceiptPrinters> = emptyList()
    private var cameraUri: Uri? = null
    private var selectedFilePath: String? = ""
    private var builder: Dialog? = null
    private val dineInViewModel by viewModels<DineInOrderTableViewModel>()
    private lateinit var binding: ParentActivityBinding
    private var navController: NavController? = null
    private lateinit var listner: NavController.OnDestinationChangedListener
    private val viewModel by viewModels<MainViewModel>()
    var activityResultCallBack: ActivityResultCallBack? = null
    private val TAG = "MainActivity"
    private val viewModelPrinter by viewModels<OrderCompleteViewModel>()
    private var customerSettingModel = GetKitchenReceiptSettingsResponse.Data()
    private var subscription: Subscription? = null
    private var consumer: Consumer? = null
    private var kitchenSettingModel = GetKitchenReceiptSettingsResponse.Data()
    private var queueOrderList: HashMap<String, ArrayList<String>> = hashMapOf()
    private var queueInProgressList: HashMap<String, ArrayList<String>> = hashMapOf()
    var mPrinter: Printer? = null
    var arrayItems: ArrayList<PrinterQueueModel> = arrayListOf()
    var isKitchenFlag: Boolean = false
    var isPrinterOnline = false

    private val dashBoardCategoryViewModel by viewModels<DashBoardCategoryViewModel>()

    @set:Inject
    internal var prefProvider: PrefProvider? = null

    private var doubleBackToExitPressedOnce:Boolean = false

    @set:Inject
    var hostSelectionInterceptor: HostSelectionInterceptor? = null

    @Inject
    lateinit var rolePermission: RolePermission
    var currentIndex: Int = 0

    private var baseUrl = ""
    private var locationId: Int = 0
    private var mContext: Context = this
    private val TAG2 = "SYNCSETTINGS"

    companion object {
        var obj: MainActivity? = null
        fun getInstance(): MainActivity {
            if (obj == null) {
                obj = MainActivity()
                return obj as MainActivity
            } else {
                return obj as MainActivity
            }

        }

        var updatePrinter: UpdatePrinters? = null

        private var subscription: Subscription? = null
        private var subscription2: Subscription? = null
        private var consumer: Consumer? = null
        private var consumer2: Consumer? = null

        fun workerDisconnect() {
            try {
                consumer?.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @Inject
    lateinit var repo: UserRepository
    private lateinit var mFirebaseAnalytics: FirebaseAnalytics
    var broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            LogUtil.logE(TAG, "getDataFirebaseNot")
            var message = intent?.getStringExtra("message")
            var isAuto = intent?.getBooleanExtra("isAuto", false)
            if (isAuto == true) {
                AlertUtils.showCustomAlertWithYesNoListener(
                    context, message
                ) { _, _ ->
                    clockoutFromSystem()
                }
            } else {
                AlertUtils.showCustomAlertWithYesNoListener(
                    context,
                    "You are clocked out in different System.\n Do you want to clock out forcefully in your System."
                ) { _, _ ->
                    clockoutFromSystem()
                }
            }
        }

    }

    var broadCastReceiverPrinterQueueSuccess = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {

            val data = p1?.getStringExtra(Constants.DATA)
            if (data?.isNotEmpty() == true) {
                var printerQueueModel: PrinterQueueModel =
                    Gson().fromJson(data, PrinterQueueModel::class.java)
                LogUtil.logE(TAG, "printerQueueModel:  ${Gson().toJson(printerQueueModel)}")
                lifecycleScope.launch {
                    var flag = viewModelPrinter.checkDataisExistOrNot(printerQueueModel)
                    LogUtil.logE(TAG, "UpdateGetloag ${flag}")
                    if (!flag) {
                        // viewModelPrinter.updateStatusPrinterQueue(printerQueueModel)
                    }
                }

            }
        }

    }


    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)


        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE, ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW, ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                System.gc()
                cacheDir.delete()
                Log.e("Cache Clear", "Cleared cache")
            }

            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND, ComponentCallbacks2.TRIM_MEMORY_MODERATE, ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                System.gc()
                // val lruCache = LruCache(100,10)
                cacheDir.delete()

                Log.e("Cache Clear", "Cleared cache")
            }

            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {

            }
        }
    }

    fun addObserver() {
        /*
                dashboardViewModel.orderCompleted.observe(this){
                    if(it){

                        val memoryInfo = Debug.MemoryInfo()
                        Debug.getMemoryInfo(memoryInfo)

                        val totalUsedMemoryKB = memoryInfo.totalPrivateDirty
                        val totalUsedMemoryMB = totalUsedMemoryKB / 1024.0


                        if (totalUsedMemoryMB > 600) {
                            dashboardViewModel.orderCompleted.value = false

                            Handler().postDelayed({
                                val intent = Intent(applicationContext, MainActivity::class.java)
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(intent)

                                Process.killProcess(Process.myPid())
                            }, 0)
                        }


        //                if(dashboardViewModel.orderCompletedCount.value==1) {
        //                    dashboardViewModel.orderCompleted.value = false
        //
        //                    Handler().postDelayed({
        //                        val intent = Intent(applicationContext, MainActivity::class.java)
        //                        intent.addFlags(Intent.FLAG_RECEIVER_NO_ABORT)
        //                        startActivity(intent)
        //
        //                        // Delay the process kill to allow time for the new activity to start
        //                        Process.killProcess(Process.myPid())
        //                    }, 2000)
        //                }
                    }
                }
        */
    }


    private var syncReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {

            try {
                if (prefProvider?.getValue(Constants.AUTH_TOKEN, "")?.isNotEmpty() == true) {
                    dashBoardCategoryViewModel.syncInventoryModule(false)
                }
            } catch (e: Exception) {
                Log.d("syncReceiver", "dashBoardCategoryViewModel create exception")

            }
        }

    }

    private var masterTerminal = object : BroadcastReceiver() {
        @SuppressLint("RestrictedApi")
        override fun onReceive(p0: Context?, p1: Intent?) {

            Log.e(
                TAG, "checkPrinterQueueWorker:  ${
                    checkUploadWorker(
                        PRINTER_QUEUE_BACKGROUND, this@MainActivity
                    )
                }"
            )


            if (prefProvider?.getValueboolean(
                    IS_MASTER_TERMINAL, false
                ) == true && prefProvider?.getValueboolean(
                    Constants.IS_PRINTER_QUEUE_STARTS, false
                ) == false && prefProvider?.getValueboolean(
                    IS_PRINTER_QUEUE_ENABLE, false
                ) == true
            ) {
                prefProvider?.setValueboolean(Constants.IS_PRINTER_QUEUE_STARTS, true)
                prefProvider?.setValueboolean(Constants.CHECK_QUEUE_CANCEL, false)
                getKitOne()

            } else if (prefProvider?.getValueboolean(IS_MASTER_TERMINAL, false) == false) {
                WorkManager.getInstance(this@MainActivity).cancelAllWork()
                val data = Data.Builder()
                    //.putString("kitchenPrinterList", Gson().toJson(kitchenPrinterList))
                    // .put("kitchenSettingData", Gson().toJson(kitchenSettingModel))
                    .put("location_id", prefProvider?.getValueInt(LOCATION_ID, 0))
                    .put("base_url", prefProvider?.getValue(Constants.BASE_URL_NEW, "")).put(
                        IS_PRINTER_QUEUE_ENABLE,
                        prefProvider?.getValueboolean(IS_PRINTER_QUEUE_ENABLE, false)
                    ).put("is_cancel_work", true).build()
                prefProvider?.setValueboolean(Constants.CHECK_QUEUE_CANCEL, true)
                val uploadWorkRequest = OneTimeWorkRequest.Builder(
                    UploadWorker2::class.java
                ).addTag(Constants.PRINTER_QUEUE_BACKGROUND).setInputData(data).build()


                val workManager = WorkManager.getInstance(this@MainActivity)

                try {

                    workManager.enqueueUniqueWork(
                        Constants.PRINTER_QUEUE_BACKGROUND,
                        ExistingWorkPolicy.KEEP,
                        uploadWorkRequest
                    )

                } catch (e: java.lang.Exception) {
                    LogUtil.logE(TAG, "printerQueueLog  ${e.message.toString()}")
                    e.printStackTrace()
                }
            } else {
                if (prefProvider?.getValueboolean(IS_PRINTER_QUEUE_ENABLE, false) == true) {
                    val data = Data.Builder()
                        //.putString("kitchenPrinterList", Gson().toJson(kitchenPrinterList))
                        // .put("kitchenSettingData", Gson().toJson(kitchenSettingModel))
                        .put("location_id", prefProvider?.getValueInt(LOCATION_ID, 0))
                        .put("base_url", prefProvider?.getValue(Constants.BASE_URL_NEW, "")).put(
                            IS_PRINTER_QUEUE_ENABLE,
                            prefProvider?.getValueboolean(IS_PRINTER_QUEUE_ENABLE, false)
                        ).put("is_cancel_work", false).build()
                    prefProvider?.setValueboolean(Constants.CHECK_QUEUE_CANCEL, false)
                    val uploadWorkRequest = OneTimeWorkRequest.Builder(
                        UploadWorker2::class.java
                    ).addTag(Constants.PRINTER_QUEUE_BACKGROUND).setInputData(data).build()


                    val workManager = WorkManager.getInstance(this@MainActivity)

                    try {

                        workManager.enqueueUniqueWork(
                            Constants.PRINTER_QUEUE_BACKGROUND,
                            ExistingWorkPolicy.REPLACE,
                            uploadWorkRequest
                        )

                    } catch (e: java.lang.Exception) {
                        LogUtil.logE(TAG, "printerQueueLog  ${e.message.toString()}")
                        e.printStackTrace()
                    }
                } else if (prefProvider?.getValueboolean(IS_PRINTER_QUEUE_ENABLE, false) == false) {
                    WorkManager.getInstance(this@MainActivity).cancelAllWork()
                    val data = Data.Builder()
                        //.putString("kitchenPrinterList", Gson().toJson(kitchenPrinterList))
                        // .put("kitchenSettingData", Gson().toJson(kitchenSettingModel))
                        .put("location_id", prefProvider?.getValueInt(LOCATION_ID, 0))
                        .put("base_url", prefProvider?.getValue(Constants.BASE_URL_NEW, "")).put(
                            IS_PRINTER_QUEUE_ENABLE,
                            prefProvider?.getValueboolean(IS_PRINTER_QUEUE_ENABLE, false)
                        ).put("is_cancel_work", true).build()
                    prefProvider?.setValueboolean(Constants.CHECK_QUEUE_CANCEL, true)
                    val uploadWorkRequest = OneTimeWorkRequest.Builder(
                        UploadWorker2::class.java
                    ).addTag(Constants.PRINTER_QUEUE_BACKGROUND).setInputData(data).build()


                    val workManager = WorkManager.getInstance(this@MainActivity)

                    try {

                        workManager.enqueueUniqueWork(
                            Constants.PRINTER_QUEUE_BACKGROUND,
                            ExistingWorkPolicy.KEEP,
                            uploadWorkRequest
                        )

                    } catch (e: java.lang.Exception) {
                        LogUtil.logE(TAG, "printerQueueLog  ${e.message.toString()}")
                        e.printStackTrace()
                    }

                }
            }


            if (prefProvider?.getValueboolean(Constants.IS_FIRST_TIME_LOGIN, false) == true) {
                val data = Data.Builder()
                    //.putString("kitchenPrinterList", Gson().toJson(kitchenPrinterList))
                    // .put("kitchenSettingData", Gson().toJson(kitchenSettingModel))
                    .put("location_id", prefProvider?.getValueInt(LOCATION_ID, 0))
                    .put("base_url", prefProvider?.getValue(Constants.BASE_URL_NEW, "")).put(
                        IS_PRINTER_QUEUE_ENABLE,
                        prefProvider?.getValueboolean(IS_PRINTER_QUEUE_ENABLE, false)
                    ).put("is_cancel_work", false).build()
                prefProvider?.setValueboolean(Constants.CHECK_QUEUE_CANCEL, false)
                val uploadWorkRequest = OneTimeWorkRequest.Builder(
                    UploadWorker2::class.java
                ).addTag(Constants.PRINTER_QUEUE_BACKGROUND).setInputData(data).build()


                val workManager = WorkManager.getInstance(this@MainActivity)

                try {

                    workManager.enqueueUniqueWork(
                        Constants.PRINTER_QUEUE_BACKGROUND,
                        ExistingWorkPolicy.KEEP,
                        uploadWorkRequest
                    )

                } catch (e: java.lang.Exception) {
                    LogUtil.logE(TAG, "printerQueueLog  ${e.message.toString()}")
                    e.printStackTrace()
                }


            }
            Log.e(
                TAG, "checkQUeue: ${
                    prefProvider?.getValueboolean(
                        IS_PRINTER_QUEUE_ENABLE, false
                    )
                }  checkMAsterRermi: ${
                    prefProvider?.getValueboolean(
                        IS_MASTER_TERMINAL, false
                    )
                }  consumer: ${consumer}"
            )

            if (prefProvider?.getValueboolean(
                    IS_PRINTER_QUEUE_ENABLE, false
                ) == true && prefProvider?.getValueboolean(
                    IS_MASTER_TERMINAL, false
                ) == true && consumer == null && isLocalMasterFlag == false
            ) {
                isLocalMasterFlag = true

                Log.e(TAG, "YesIN ACtionConnect")
                connectActionCable()

            } else if ((prefProvider?.getValueboolean(
                    IS_PRINTER_QUEUE_ENABLE, false
                ) == false || prefProvider?.getValueboolean(
                    IS_MASTER_TERMINAL, false
                ) == false) && consumer != null
            ) {

                consumer?.disconnect()


            }

        }

    }

    private var syncFloorPlan = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            Log.e("SyncFloorPlan", "onReceiveSync")
            if (findNavController(R.id.navHostFrag).currentDestination?.id == R.id.dineInFragment) {

                navController?.popBackStack(R.id.dineInFragment, true)
                navController?.navigate(R.id.dineInFragment)

            }
        }

    }

    private var syncSettingReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {

            LogUtil.logEN("onReceive", "" + p1?.action)
            if (prefProvider?.getValue(Constants.AUTH_TOKEN, "")?.isNotEmpty() == true) {
                dashBoardCategoryViewModel.syncSettingModule()
            }

        }

    }

    private var syncMarkupReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {

            LogUtil.logEN("syncMarkupReceiver", "" + p1?.action)
            dashBoardCategoryViewModel.markupInventory()

        }

    }


    var broadCastReceiverPrinterQueueDataGet = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            val serializedObject: String = p1?.getStringExtra(Constants.DATA).toString()
            if (serializedObject.isNotEmpty()) {
                val gson = Gson()
                val type = object : TypeToken<List<PrinterQueueModel?>?>() {}.type
                arrayItems = gson.fromJson<Any>(
                    serializedObject, type
                ) as ArrayList<PrinterQueueModel>


                LogUtil.logE(TAG, "printerQueueDataReceived  ${Gson().toJson(arrayItems)}")


                arrayItems.forEach { data ->
                    data.id?.let { it1 ->
                        viewModelPrinter.checkQueueExist(data.id!!).observe(this@MainActivity) {
                            if (it.status == Status.SUCCESS) {
                                if (it.data == null) {
                                    lifecycleScope.launch {
                                        viewModelPrinter.addPrinterQueueData(data)
                                    }
                                }
                            }


                        }

                    }
                }


            } else {
                getPrinterQueueData()
            }


        }

    }
    var broadcastReceiveronlineOrder = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            LogUtil.logE(TAG, "GetOnlineOrderDataNoti")
            var count = intent?.getStringExtra("count")
            count?.toInt()
                ?.let { DashboardCategoryBoldPOS.newInstance().onlineOrderBadgeDisplay(it) }
            if (navController?.currentDestination?.id == R.id.allOrdersFragment) {
                var intent = Intent()
                intent.putExtra("refresh", true)
                intent.action = Constants.ONLINE_ORDER_REFRESH
                sendBroadcast(intent)
            }
        }
    }

    private fun getPrinterQueueData() {
        viewModelPrinter.getPrinterQueueData().observe(this) {
            if (it != null && it.isNotEmpty()) {
                LogUtil.logE(TAG, "getPrinterQueueData " + isPrinterQueueRun)
                if (!isPrinterQueueRun) {
                    isPrinterQueueRun = true


                    newKitchenPrinterInit(
                        it[it.size - 1], it.size - 1, it.toCollection(arrayListOf())
                    )

                }
                /* prefProvider?.setValue(Constants.PRINTER_QUEUE_DATA, "")
                 prefProvider?.setValue(Constants.PRINTER_QUEUE_DATA, Gson().toJson(it))*/


            } else {
                LogUtil.logE(TAG, "getPrinterNull")
            }
        }
    }

    private fun masterTerminalObserver() {
        dashBoardCategoryViewModel.masterTeminalLiveData.observe(this) {
            Log.e(TAG, "masterTerminalObseOutside ${it}")
            it.getContentIfNotHandled()?.let {
                Log.e(TAG, "masterTerminalObserver ${it}")
                if (it) {
                    sendBroadCast()
                }
            }


        }
    }


    private fun newKitchenPrinterInit(
        printerQueueModel: PrinterQueueModel, index: Int, arrayItems: ArrayList<PrinterQueueModel>
    ) {
        LogUtil.logE(TAG, "kitchenPrinters  ${kitchenPrinterList.size}")
        printerQueueModelGlobal = printerQueueModel
        if (kitchenPrinterList.isEmpty()) {
            isPrinterQueueRun = false
        }


        for (i in 0 until kitchenPrinterList.size) {
            var modelName = -1
            if (kitchenPrinterList[i].modalName.equals("TM-M30", true)) {
                modelName = Printer.TM_M30
            } else if (kitchenPrinterList[i].modalName.equals("TM-U220", true)) {
                modelName = Printer.TM_U220
            } else if (kitchenPrinterList[i].name.substring(0, 6).toString()
                    .equals("TM-m30", true)
            ) {
                modelName = Printer.TM_M30
            }

            LogUtil.logE(TAG, "modelName  ${modelName}")
            if (modelName != -1) {

                mPrinter = null
                mPrinter = com.epson.epos2.printer.Printer(modelName, Printer.MODEL_ANK, this)
                //mPrinter?.startMonitor()


                var containsFlag: Boolean = true
                LogUtil.logE(
                    TAG, "printerSuccessData  ${Gson().toJson(printerQueueModel.printSuccessData)}"
                )
                LogUtil.logE(TAG, "PrinterID ${kitchenPrinterList[i].id}")
                if (printerQueueModel.printSuccessData.isNotEmpty()) {
                    /*    for (k in 0 until printerQueueModel.printSuccessData.size) {

                            if (printerQueueModel.printSuccessData[k].toInt() == kitchenPrinterList[i].id) {
                                containsFlag = true
                                break
                            } else {
                                containsFlag = false
                            }

                        }*/

                    /* if (printerQueueModel.printSuccessData.contains(kitchenPrinterList[i].id)) {
                         containsFlag = true
                     } else {
                         containsFlag = false
                     }*/
                } else {
                    containsFlag = false
                }
                LogUtil.logE(TAG, "containsFlag:  ${containsFlag}  ${mPrinter}")
                if (!containsFlag && mPrinter != null) {

                    try {
                        LogUtil.logE(TAG, "isPrinterQueueRun  ${isPrinterQueueRun}")



                        isPrinterQueueRun = true
                        lifecycleScope.executeAsyncTask(onPostExecute = {
                            if (mPrinter != null) {
                                LogUtil.logE(
                                    TAG, "statusInfo  ${Gson().toJson(mPrinter?.status)}"
                                )

                                var fontSizeH = 1
                                var fontSizeW = 1
                                when (kitchenSettingModel.fonts) {
                                    Constants.SMALL -> {
                                        fontSizeH = 1
                                        fontSizeW = 1
                                    }

                                    Constants.MEDIUM -> {
                                        fontSizeH = 1
                                        fontSizeW = 2
                                    }

                                    Constants.LARGE -> {
                                        fontSizeH = 2
                                        fontSizeW = 2
                                    }


                                }
                                mPrinter?.addFeedLine(2)
                                if (kitchenSettingModel.showOrderType) {


                                    mPrinter?.addFeedLine(0)
                                    mPrinter?.addTextFont(Builder.FONT_E)
                                    mPrinter?.addTextLang(Builder.LANG_EN)
                                    mPrinter?.addTextSize(fontSizeH, fontSizeW)
                                    mPrinter?.addTextStyle(
                                        Builder.FALSE,
                                        Builder.FALSE,
                                        Builder.TRUE,
                                        Builder.COLOR_1
                                    )
                                    mPrinter?.addTextAlign(Builder.ALIGN_CENTER)
                                    mPrinter?.addText(printerQueueModel.orderType)

                                }

                                mPrinter?.addFeedLine(2)
                                mPrinter?.addTextFont(Builder.FONT_E)
                                //  builder.addTextAlign(Builder.ALIGN_LEFT)
                                mPrinter?.addTextLang(Builder.LANG_EN)
                                mPrinter?.addTextSize(1, 1)
                                mPrinter?.addTextStyle(
                                    Builder.FALSE, Builder.FALSE, Builder.FALSE, Builder.COLOR_1
                                )

                                mPrinter?.addText(
                                    padLine(
                                        "OrderID:" + printerQueueModel.orderID, "", 48
                                    )
                                )

                                mPrinter?.addFeedUnit(30)
                                mPrinter?.addTextFont(Builder.FONT_E)
                                //  builder.addTextAlign(Builder.ALIGN_LEFT)
                                mPrinter?.addTextLang(Builder.LANG_EN)
                                mPrinter?.addTextSize(1, 1)
                                mPrinter?.addTextStyle(
                                    Builder.FALSE, Builder.FALSE, Builder.FALSE, Builder.COLOR_1
                                )


                                mPrinter?.addText(
                                    padLine(
                                        "ReceiptID:" + printerQueueModel.offlineId, "", 48
                                    )
                                )

                                mPrinter?.addFeedUnit(30)
                                mPrinter?.addTextFont(Builder.FONT_E)
                                mPrinter?.addTextAlign(Builder.ALIGN_LEFT)
                                mPrinter?.addTextLang(Builder.LANG_EN)
                                mPrinter?.addTextSize(fontSizeH, fontSizeW)
                                mPrinter?.addTextStyle(
                                    Builder.FALSE, Builder.FALSE, Builder.FALSE, Builder.COLOR_1
                                )

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    val current = LocalDateTime.now()
                                    val formatter =
                                        DateTimeFormatter.ofPattern("MMM-dd-yyyy hh:mm:a")
                                    val formatted = current.format(formatter)
                                    mPrinter?.addText(
                                        "Print Time:" + Constants.getCurrentTimeFromTimeZone(
                                            this, formatted
                                        )
                                    )
                                }
                                mPrinter?.addFeedLine(1)
                                mPrinter?.let {
                                    addHorizontalLineNew(it)
                                }

                                printerQueueModel.orderItems.let {
                                    addOrdersForKitchenCustomerNewPrinter(
                                        mPrinter!!, it, fontSizeH, fontSizeW
                                    )
                                }

                                if (kitchenSettingModel.showCustomerAddress != false || kitchenSettingModel.showCustomerPhone != false || kitchenSettingModel.showCustomerName != false) {
                                    if (printerQueueModel.customerName.isNotEmpty()) {

                                        mPrinter?.addFeedUnit(30)
                                        mPrinter?.addFeedLine(1)
                                        mPrinter?.addTextFont(Builder.FONT_E)
                                        //builder.addTextLineSpace(20)
                                        mPrinter?.addTextAlign(Builder.ALIGN_LEFT)
                                        mPrinter?.addTextLang(Builder.LANG_EN)
                                        mPrinter?.addTextSize(fontSizeH, fontSizeW)
                                        mPrinter?.addTextStyle(
                                            Builder.FALSE,
                                            Builder.FALSE,
                                            Builder.TRUE,
                                            Builder.COLOR_1
                                        )
                                        mPrinter?.addText("Customer Details" + "\n")

                                        mPrinter?.addTextFont(Builder.FONT_B)
                                        //builder.addTextLineSpace(20)
                                        mPrinter?.addTextLang(Builder.LANG_EN)
                                        mPrinter?.addTextSize(fontSizeH, fontSizeW)
                                        mPrinter?.addTextStyle(
                                            Builder.FALSE,
                                            Builder.FALSE,
                                            Builder.FALSE,
                                            Builder.COLOR_1
                                        )
                                        addHorizontalLineNew(mPrinter!!)

                                        if (kitchenSettingModel.showCustomerName) {

                                            mPrinter?.addFeedUnit(30)
                                            mPrinter?.addTextFont(Builder.FONT_E)
                                            mPrinter?.addTextAlign(Builder.ALIGN_LEFT)
                                            //builder.addTextLineSpace(20)
                                            mPrinter?.addTextLang(Builder.LANG_EN)
                                            mPrinter?.addTextSize(fontSizeH, fontSizeW)
                                            mPrinter?.addTextStyle(
                                                Builder.FALSE,
                                                Builder.FALSE,
                                                Builder.TRUE,
                                                Builder.COLOR_1
                                            )
                                            mPrinter?.addText(printerQueueModel.customerName)

                                        }


                                        if (kitchenSettingModel.showCustomerPhone) {

                                            if (printerQueueModel?.customerPhoneNo.isNotEmpty()) {
                                                mPrinter?.addFeedUnit(30)
                                                mPrinter?.addTextFont(Builder.FONT_E)
                                                mPrinter?.addTextAlign(Builder.ALIGN_LEFT)
                                                //builder.addTextLineSpace(20)
                                                mPrinter?.addTextLang(Builder.LANG_EN)
                                                mPrinter?.addTextSize(fontSizeH, fontSizeW)
                                                mPrinter?.addTextStyle(
                                                    Builder.FALSE,
                                                    Builder.FALSE,
                                                    Builder.TRUE,
                                                    Builder.COLOR_1
                                                )
                                                mPrinter?.addText(printerQueueModel.customerPhoneNo)
                                            }

                                        }

                                        if (kitchenSettingModel.showCustomerAddress) {


                                            if (printerQueueModel.customerAddress.isNotEmpty()) {

                                                mPrinter?.addFeedUnit(30)
                                                mPrinter?.addTextFont(Builder.FONT_E)
                                                mPrinter?.addTextAlign(Builder.ALIGN_LEFT)
                                                //builder.addTextLineSpace(20)
                                                mPrinter?.addTextLang(Builder.LANG_EN)
                                                mPrinter?.addTextSize(fontSizeH, fontSizeW)
                                                mPrinter?.addTextStyle(
                                                    Builder.FALSE,
                                                    Builder.FALSE,
                                                    Builder.TRUE,
                                                    Builder.COLOR_1
                                                )

                                                mPrinter?.addText(printerQueueModel.customerAddress)
                                            }

                                        }

                                    }
                                }

                                mPrinter?.addFeedLine(2)
                                mPrinter?.addCut(Builder.CUT_FEED)

                                if (mPrinter?.status?.connection != 0) {
                                    mPrinter?.beginTransaction()
                                    mPrinter?.sendData(Printer.PARAM_DEFAULT)

                                    isPrinterQueueRun = false
                                }


                            }

                            lifecycleScope.launch {
                                delay(5000)
                                getPrinterQueueData()
                            }

                        }, doInBackground = {


                            LogUtil.logE(
                                TAG, "getIpAddress  ${kitchenPrinterList[i].ipAddress}"
                            )
                            LogUtil.logE(
                                TAG, "connectionPrinter   ${mPrinter?.status?.connection}"
                            )
                            /*
                                                                try {
                                                                    mPrinter?.disconnect()
                                                                } catch (e: Exception) {
                                                                    e.printStackTrace()
                                                                }*/
                            try {

                                lifecycleScope.launch {

                                    for (m in 0 until 3) {
                                        try {

                                            mPrinter?.connect(
                                                kitchenPrinterList[i].ipAddress,
                                                Printer.PARAM_DEFAULT
                                            )

                                            mPrinter?.startMonitor()
                                        } catch (e: Exception) {
                                            isPrinterQueueRun = false
                                            try {
                                                if (mPrinter?.status?.connection == 1) {
                                                    mPrinter?.disconnect()
                                                }
                                            } catch (e: java.lang.Exception) {
                                                e.printStackTrace()
                                            }
                                            e.printStackTrace()

                                        }

                                    }
                                }
                                mPrinter?.setReceiveEventListener(this)
                                mPrinter?.setConnectionEventListener(this)
                                mPrinter?.setStatusChangeEventListener(this)


                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                        }, onPreExecute = {
                            isPrinterQueueRun = true
                        })


                    } catch (e: java.lang.Exception) {

                        LogUtil.logE(TAG, "connectException  ${e.message}")


                        isPrinterQueueRun = false

                        e.printStackTrace()
                    }


                } else {

                    isPrinterQueueRun = false
                    //printerBGRunning = false
                }
            } else {
                isPrinterQueueRun = false
                // printerBGRunning = false
            }
        }


    }

    private fun getCustomerReceiptSettings() {
        viewModelPrinter.getKitchenReceiptSettings().observe(this) {
            if (it != null) {
                customerSettingModel = it


            }

        }

    }

    @SuppressLint("RestrictedApi")
    private fun getKitOne() {

        /*prefProvider?.setValue(
            Constants.KITCHEN_PRINTER_LIST_PREF,
            Gson().toJson(it.data).toString()
        )*/

        Log.e(
            TAG, "checkPrinterQueueWorker:  ${
                checkUploadWorker(
                    Constants.PRINTER_QUEUE_BACKGROUND, this@MainActivity
                )
            }"
        )
        Log.e(TAG, "CheckHere DAta:")
        val data = Data.Builder()
            //.putString("kitchenPrinterList", Gson().toJson(kitchenPrinterList))
            // .put("kitchenSettingData", Gson().toJson(kitchenSettingModel))
            .put("location_id", prefProvider?.getValueInt(LOCATION_ID, 0))
            .put("base_url", prefProvider?.getValue(Constants.BASE_URL_NEW, "")).put(
                IS_PRINTER_QUEUE_ENABLE,
                prefProvider?.getValueboolean(IS_PRINTER_QUEUE_ENABLE, false)
            ).put("is_cancel_work", false).build()

        prefProvider?.setValueboolean(Constants.CHECK_QUEUE_CANCEL, false)

        val uploadWorkRequest = OneTimeWorkRequest.Builder(
            UploadWorker2::class.java
        ).addTag(Constants.PRINTER_QUEUE_BACKGROUND).setInputData(data).build()


        val workManager = WorkManager.getInstance(this)

        try {


            workManager.enqueueUniqueWork(
                Constants.PRINTER_QUEUE_BACKGROUND, ExistingWorkPolicy.REPLACE, uploadWorkRequest
            )

        } catch (e: java.lang.Exception) {
            LogUtil.logE(TAG, "printerQueueLog  ${e.message.toString()}")
            e.printStackTrace()
        }


    }

    private fun getKitchenPrinters() {
        viewModelPrinter.getKitchenPrinterList().observe(this) {
            when (it.status) {
                Status.SUCCESS -> {
                    ProgressUtils.dismissProgressDialog()
                    if (it.data != null) {
                        kitchenPrinterList = emptyList()
                        kitchenPrinterList = it.data
                        if (prefProvider?.getValueboolean(
                                Constants.IS_MASTER_TERMINAL, false
                            ) == true
                        ) {
                            //  getKitOne()
                        }

                        LogUtil.logE("getCustomerPrinters", Gson().toJson(kitchenPrinterList))
                    }

                }

                Status.ERROR -> {
                    ProgressUtils.dismissProgressDialog()

                }

                Status.LOADING -> {
                    ProgressUtils.showProgressDialog(this)
                }

            }
        }
    }

    private fun clockoutFromSystem() {
        prefProvider?.setValueInt(Constants.EMPLOYEE_ID, 0)
        prefProvider?.setValue(Constants.EMPLOYEE_NAME, "")
        prefProvider?.setValue(
            Constants.EMPLOYEE_ROLE, ""
        )
        prefProvider?.setValueInt(
            Constants.EMPLOYEE_ROLE_ID, 0
        )
        prefProvider?.setValue(Constants.PASSCODE, "")
        var bundle: Bundle = Bundle()
        bundle.putBoolean("isSwap", true)
        bundle.putBoolean("isDashboard", false)
        bundle.putBoolean("isExit", true)
        navController?.navigate(R.id.action_global_login, bundle)


    }

    override fun onDestroy() {
        super.onDestroy()

        if (prefProvider?.getValueboolean(
                IS_MASTER_TERMINAL, false
            ) == true && prefProvider?.getValueboolean(
                Constants.IS_PRINTER_QUEUE_ENABLE, false
            ) == true
        ) {

            disconnectSocket()

        }
        updatePrinter = null
        unregisterReceiver(broadcastReceiver)
        unregisterReceiver(broadcastReceiveronlineOrder)


        //dashboardViewModel.cartOrderUpdated.value?.let { prefProvider?.setOrderStatusSaveOrUpdate(it) }

    }

    private lateinit var presentation: CustomDisplay

    private fun initCustomerDisplay() {
        getCustomerDisplay(this)?.let { display ->
            presentation = CustomDisplay(
                display, this, this, dashboardViewModel, passcodeViewModel, dineInViewModel
            )
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            if (consumer != null) {
                consumer?.disconnect()
                consumer = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        Log.e(TAG, "checkActivityStop:")


    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MainApplication.mainActivity = this
        permissionCheck()

        if (!checkServiceRunning(applicationContext, KioskService::class.java)) {
            startForegroundService(Intent(this, KioskService::class.java))
        }

        Log.e(TAG, "checkConsumerNullorNot  ${consumer}")
        if (consumer != null) {
            consumer = null
        }
        updatePrinter = this
        queueOrderList = hashMapOf()

        addObserver()


//        throw RuntimeException("Test Crash") // Force a crash

        Log.e(TAG, "currentTimeInMilis:   ${System.currentTimeMillis()}")
        /*try {
            val field: Field = CursorWindow::class.java.getDeclaredField("sCursorWindowSize")
            field.setAccessible(true)
            field.set(null, 10 * 1024 * 1024) //the 100MB is the new size
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }*/
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)


        //demoPrinterQueue()
        getKitOne()
        masterTerminalObserver()
        Log.e(
            TAG, "checkPrinterQueue ${
                prefProvider?.getValueboolean(
                    IS_PRINTER_QUEUE_ENABLE, false
                )
            }  checkMaster: ${
                prefProvider?.getValueboolean(
                    IS_MASTER_TERMINAL, false
                )
            }"
        )

        if (prefProvider?.getValueboolean(
                IS_PRINTER_QUEUE_ENABLE, false
            ) == true && prefProvider?.getValueboolean(
                IS_MASTER_TERMINAL, false
            ) == true && consumer == null
        ) {

            //   connectActionCable()
        }


        // connectionActionCable()
        val intentFilter = IntentFilter("PrinterQueue")
        registerReceiver(wifiStateReceiver, intentFilter)
        getCustomerReceiptSettings()
        if (Build.VERSION.SDK_INT > 9) {
            val policy: StrictMode.ThreadPolicy =
                StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)
        }
        var requestURL =
            prefProvider?.getValue(Constants.BASE_URL_NEW, "") + Constants.CREATE_QUEUE_PRINTER
//        val uri = URI("wss://hugepos.com/cable")
        val uri = URI("wss://pays.app/cable")
        consumer = ActionCable.createConsumer(uri)
        getPrinterQueueData()

        getKitchenPrinters()


        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        registerReceiver(
            broadCastReceiverPrinterQueueSuccess, IntentFilter(Constants.PRITNER_QUEUE_DATA_DELETE)
        )
        registerReceiver(
            broadcastReceiver, IntentFilter(Constants.SEND_CLOCKOUT_NOTIFICATION)
        )

        registerReceiver(
            broadcastReceiveronlineOrder, IntentFilter(Constants.ONLINE_ORDER_GET_NOTIFICATION)
        )
        registerReceiver(
            broadCastReceiverPrinterQueueDataGet,
            IntentFilter(Constants.PRINTER_QUEUE_DATA_RECEIVED)
        )

        registerReceiver(
            syncReceiver, IntentFilter(Constants.SYNC_NOTIFICATION)
        )
        registerReceiver(
            syncFloorPlan, IntentFilter(Constants.SYNC_FLOORPLAN)
        )

        registerReceiver(
            masterTerminal, IntentFilter(Constants.MASTER_TEMINAL_CHANGED)
        )

        registerReceiver(
            syncSettingReceiver, IntentFilter(Constants.SYNC_SETTING_NOTIFICATION)
        )

        registerReceiver(
            syncMarkupReceiver, IntentFilter(Constants.SYNC_MARKUP)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            window.statusBarColor = getColor(R.color.txtColorGray)
        }
        binding = DataBindingUtil.setContentView(this, R.layout.parent_activity)
        initCustomerDisplay()
        supportActionBar?.hide()
        binding.lifecycleOwner = this


        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFrag) as NavHostFragment
        navController = navHostFragment.navController

//        navController = findNavController(R.id.navHostFrag) as NavHostFragment

        listner = NavController.OnDestinationChangedListener { controller, destination, arguments ->

            if (destination.id == R.id.dashboard || destination.id == R.id.dashboardCategory || destination.id == R.id.teamList || destination.id == R.id.settings || destination.id == R.id.inventory || destination.id == R.id.reports || destination.id == R.id.customer) {
                //drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            } else {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

            }

            if (destination.id == R.id.login) {
                Log.e(TAG, "YESIT IS LOGIN")
                if (consumer != null) {
                    try {
                        consumer?.disconnect()
                    } catch (e: Exception) {
                        consumer = null
                    }

                }
            }
        }
        binding.navView.setupWithNavController(navController!!)
        val imgBack = binding.navView.getHeaderView(0).findViewById<ImageView>(R.id.imgBack)
        imgBack.setOnClickListener {
            disableDrawer()

        }

        binding.navView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.menuHome -> {
                    disableDrawer()
                    navController?.navigate(R.id.action_global_dashboardCategory)
                    return@setNavigationItemSelectedListener true
                }

                R.id.menuHardware -> {
                    disableDrawer()
                    navController?.navigate(R.id.hardware)
                    return@setNavigationItemSelectedListener true
                }

                R.id.menuOrders -> {
                    disableDrawer()
                    navController?.navigate(R.id.action_global_orders)
                    return@setNavigationItemSelectedListener true
                }

                R.id.menuTransactions -> {
                    disableDrawer()
                    if (rolePermission.hasTransactionPermission(binding.root)) {
                        navController?.navigate(R.id.action_global_transactionFragment)
                        return@setNavigationItemSelectedListener true
                    }
                }

                R.id.menuCashLog -> {
                    disableDrawer()
                    if (rolePermission.hasCashLogPermission(binding.root)) {
                        navController?.navigate(R.id.action_global_cashLogFragment)
                        return@setNavigationItemSelectedListener true
                    }
                }

                R.id.menuReports -> {
                    disableDrawer()
                    navController?.navigate(R.id.action_global_reports)
                    return@setNavigationItemSelectedListener true
                }

                R.id.menuCustomers -> {
                    disableDrawer()
                    if (rolePermission.hasCustomerPermission(binding.root)) {
                        navController?.navigate(R.id.action_global_customer)
                        return@setNavigationItemSelectedListener true
                    }
                }

                R.id.menuTeam -> {
                    disableDrawer()
                    navController?.navigate(R.id.action_global_teamList)
                    return@setNavigationItemSelectedListener true
                }

                R.id.memuInventory -> {
                    disableDrawer()
                    if (rolePermission.hasInventoryPermission(binding.root)) {
                        navController?.navigate(R.id.action_global_inventory)
                        return@setNavigationItemSelectedListener true
                    }
                }

                R.id.menuSettings -> {
                    disableDrawer()
                    navController?.navigate(R.id.action_global_settings)
                    return@setNavigationItemSelectedListener true
                }

                R.id.menuSupport -> {
                    disableDrawer()
                    return@setNavigationItemSelectedListener true
                }

                R.id.menuLogout -> {

                    alertLogout()
                    return@setNavigationItemSelectedListener true

                }

            }

            return@setNavigationItemSelectedListener false
        }

        observeShowProgress()

    }

    fun checkServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun permissionCheck() {
        ActivityCompat.requestPermissions(
            this@MainActivity, arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH
            ), 1515
        )


    }

    private fun reConnectPrinterQueue() {
        Log.e(
            TAG, "checkAll Data: ${
                prefProvider?.getValueboolean(
                    IS_MASTER_TERMINAL, false
                )
            }  printerQueue:  ${
                prefProvider?.getValueboolean(
                    IS_PRINTER_QUEUE_ENABLE, false
                )
            }"
        )

        reConnectCount = 0
        if (prefProvider?.getValueboolean(
                IS_MASTER_TERMINAL, false
            ) == true && prefProvider?.getValueboolean(
                IS_PRINTER_QUEUE_ENABLE, false
            ) == true
        ) {
            lifecycleScope.launch {
                delay(2500)
                connectActionCable()

            }


        }
    }

    fun connectActionCable() {

        var requestURL = prefProvider?.getValue(
            Constants.BASE_URL_NEW, ""
        ) + Constants.CREATE_QUEUE_PRINTER_PHASE3
        Log.e(TAG, "checkrequestURL  ${requestURL}")

        val uri = URI(Constants.PRINTER_QUEUE_CONNECTION_URL_SNACKPOS)

        Log.e(TAG, "checkConsumer ${consumer}")
        if (consumer != null) {

            lifecycleScope.launch(Dispatchers.Main) {
                consumer?.disconnect()
                delay(1500)
                connectActionCable()

            }
        } else {
            consumer = ActionCable.createConsumer(uri)
        }


        // 2. Create subscription
        val appearanceChannel = Channel("PrinterQueueV4Channel")
        Log.e(TAG, "locationID: ${prefProvider?.getValueInt(LOCATION_ID, 0)}")
        appearanceChannel.addParam("id", prefProvider?.getValueInt(LOCATION_ID, 0))
        // appearanceChannel.addParam("id",prefProvider.getValueInt(LOCATION_ID,0))
        subscription = consumer?.subscriptions?.create(appearanceChannel)

        if (subscription != null) {
            subscription?.onConnected {
                isLocalMasterFlag = false

                prefProvider?.setValueboolean(Constants.WORKER_QUEUE_IN_PROGRESS, true)

                Log.e(TAG, "onActionConnected")
                val params = JsonObject()
                params.addProperty("id", prefProvider?.getValueInt(LOCATION_ID, 0))
                params.addProperty("url", requestURL)
                Log.e(
                    TAG, "checkID 8: ${
                        prefProvider?.getValueInt(
                            LOCATION_ID, 0
                        )
                    }  checkURL 8:  ${
                        prefProvider?.getValue(
                            Constants.BASE_URL_NEW, ""
                        ) + Constants.CREATE_QUEUE_PRINTER_PHASE3
                    }"
                )
                if (navController?.currentDestination?.id == R.id.login) {
                    Log.e(TAG, "IN_CONNECTION_CONDITION")
//                        consumer?.disconnect()
                } else {
                    subscription?.perform("received", params)
                }

            }?.onRejected {
                isLocalMasterFlag = false
                currentOrderIndex = 0
                currentPrinterIndex = 0

                prefProvider?.setValueboolean(Constants.WORKER_QUEUE_IN_PROGRESS, false)
                Log.e(TAG, "onRejected ")
                Handler(Looper.getMainLooper()).postDelayed(Runnable {
                    if (isInternetAvailable()) {
                        consumer?.connect()
                    } else {
                        sendNotification("Please check your Network Connectivity.")
                    }
                }, 10000)

            }?.onReceived {
                Log.e(TAG, "onActionReceived:  ${Gson().toJson(it)}")
                Log.e(
                    TAG, "onActionReceived checkCancelWeok:  ${isCancelWork}"
                )

                isLocalMasterFlag = false

                this@MainActivity.getSharedPreferences(
                    this@MainActivity.resources.getString(R.string.app_name), Context.MODE_PRIVATE
                ).edit().putBoolean(Constants.WORKER_QUEUE_IN_PROGRESS, true)

                Log.e(TAG, "checkCancelWork")
                if (navController?.currentDestination?.id == R.id.login) {
                    Log.e(TAG, "IN_CONNECTION_CONDITION")
                    consumer?.disconnect()
                }
//                        consumer?.disconnect()
                else {

                    Log.e("listOfPrintersData", "onReceived ${isQueueRunning}")
                    if (it != null && isQueueRunning == false) {
                        isQueueRunning = true
                        listOfPrintersData.clear()
                        currentOrderIndex = 0
                        currentPrinterIndex = 0
                        listOfPrintersData = arrayListOf()

                        if (it.asJsonObject.has("printer_queue")) {
                            isPrinterRunning = true
                            globalPrinterQueue = it.asJsonObject.get("printer_queue")

                            getQueueDataResponse(it.asJsonObject.get("printer_queue"))


                        } else {


                            Log.e(TAG, "callActionCalledRun 3")


                            currentOrderIndex = 0
                            currentPrinterIndex = 0


                            val params = JsonObject()
                            params.addProperty(
                                "id", prefProvider?.getValueInt(LOCATION_ID, 0)
                            )
                            params.addProperty("url", requestURL)
                            Log.e(
                                TAG, "checkID: ${
                                    prefProvider?.getValueInt(
                                        LOCATION_ID, 0
                                    )
                                }  checkURL:  ${requestURL}"
                            )

                            if (reConnectCount >= 10) {
                                consumer?.disconnect()

                                reConnectPrinterQueue()

                            } else {

                                reConnectCount += 1
                                isQueueRunning = false
                                subscription?.perform("received", params)
                            }


                            /*val intent = Intent()
                    intent.putExtra(Constants.DATA, "")
                    intent.action = PRINTER_QUEUE_DATA_RECEIVED
                    mContext.sendBroadcast(intent)*/


                        }

                    }
                }


            }?.onDisconnected {
                isLocalMasterFlag = false

                currentOrderIndex = 0
                currentPrinterIndex = 0

                this@MainActivity.getSharedPreferences(
                    this@MainActivity.resources.getString(R.string.app_name), Context.MODE_PRIVATE
                ).edit().putBoolean(Constants.WORKER_QUEUE_IN_PROGRESS, false)
                Log.e(TAG, "onDisconnected")
                localCallConnect = false

                if (subscription != null) {
                    try {
                        consumer?.subscriptions?.remove(subscription)
                        consumer = null
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }


                consumer = null


                /*    if (isInternetAvailable()) {


                        if (this@MainActivity.getSharedPreferences(
                                this@MainActivity.resources.getString(R.string.app_name),
                                Context.MODE_PRIVATE
                            ).getBoolean(Constants.CHECK_QUEUE_CANCEL, false) == false
                        ) {
                            //consumer?.connect()
                        } else {
                            consumer?.subscriptions?.remove(subscription)
                            consumer = null
                        }
                    } else {
                        sendNotification("Please check your Network Connectivity.")
                    }
*/

                /* if (isFromParent == true){
                     isFromParent = false
                 }*/


            }?.onFailed {

                isLocalMasterFlag = false
                localCallConnect = false
                currentOrderIndex = 0
                currentPrinterIndex = 0

                this@MainActivity.getSharedPreferences(
                    this@MainActivity.resources.getString(R.string.app_name), Context.MODE_PRIVATE
                ).edit().putBoolean(Constants.WORKER_QUEUE_IN_PROGRESS, false)
                Log.e(TAG, "onFailed")

                if (isInternetAvailable()) {
                    if (prefProvider?.getValueboolean(
                            IS_MASTER_TERMINAL, false
                        ) == true && prefProvider?.getValueboolean(
                            IS_PRINTER_QUEUE_ENABLE, false
                        ) == true
                    ) {
                        /* Handler(Looper.getMainLooper()).postDelayed(object:Runnable{
                             override fun run() {
                                 consumer?.connect()
                             }

                         },5000)*/
                    }
                } else {
                    sendNotification("Please check your Network Connectivity.")
                }


            }

        }


        // 3. Establish connection

        /* if (localCallConnect == false) {*/
        localCallConnect = true
        Log.e(TAG, "consumerConnect  ${consumer}")
        this@MainActivity.getSharedPreferences(
            this@MainActivity.resources.getString(R.string.app_name), Context.MODE_PRIVATE
        ).edit().putBoolean(Constants.WORKER_QUEUE_IN_PROGRESS, true)

        isQueueRunning = false
        isPrinterRunning = false
        consumer?.connect()

        /*}*/


    }

    private fun sendReceiptToPrintSunmi(cloudPrinter: CloudPrinter) {
        Log.d("checkCommitResultF", "sendReceiptToPrintSunmi 5")

        try {
            var obj = listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.get(
                currentOrderIndex
            )


            cloudPrinter.lineFeed(1)
            cloudPrinter.setUnderlineMode(UnderlineStyle.EMPTY)
            cloudPrinter.setBoldMode(true)
            cloudPrinter.setAlignment(AlignStyle.CENTER)
            cloudPrinter.setCharacterSize(2, 2)
            cloudPrinter.printText("Order ID:" + obj.orderID)

            cloudPrinter.lineFeed(1)

            cloudPrinter.setAlignment(AlignStyle.CENTER)
            cloudPrinter.printText(obj.orderType)


            if (obj.deliveryType.isNotEmpty()) {
                cloudPrinter.lineFeed(1)
                cloudPrinter.setAlignment(AlignStyle.CENTER)
                cloudPrinter.printText(obj.deliveryType)
            }


            cloudPrinter.lineFeed(1)
            cloudPrinter.setBoldMode(false)
            cloudPrinter.setCharacterSize(1, 1)
            cloudPrinter.setAlignment(AlignStyle.LEFT)
            if (obj.employeeName != null) {
                cloudPrinter.printText("Employee:" + obj.employeeName)
            }

            cloudPrinter.setCharacterSize(1, 1)

            cloudPrinter.setAlignment(AlignStyle.LEFT)
            cloudPrinter.printText(obj.dateAndTime)


            cloudPrinter.let { addDoubleDotLineForSunmiQueue(it) }

            if (obj.orderType.equals(
                    Constants.DINE_IN, true
                ) || obj.orderType.equals(Constants.DINE_IN_SPACE, true)
            ) {

                Log.e(TAG, "checkInside DINEIN")
                cloudPrinter.let { printGuestByItemForSunmiQueue(obj.guestAttributes, it) }


            } else {
                Log.e(TAG, "checkInside TAKEOUT")
                for (i in 0 until obj.orderItems.size) {

                    cloudPrinter.setBoldMode(false)
                    cloudPrinter.setCharacterSize(2, 2)
                    cloudPrinter.setAlignment(AlignStyle.LEFT)

                    if (obj.orderItems[i].timestamp.isNotEmpty()) {
                        var msg = "(" + obj.orderItems[i].timestamp + ")"
                        cloudPrinter.printText("" + obj.orderItems[i].quantity + " " + obj.orderItems[i].itemName + "  " + msg)

                    } else {

                        cloudPrinter.printText("" + obj.orderItems[i].quantity + " " + obj.orderItems[i].itemName)
                    }

                    if (obj.orderItems[i].orderItemModifiers.isNotEmpty()) {
                        obj.orderItems[i].orderItemModifiers.forEach { mod ->


                            cloudPrinter.printText(
                                /*"  " + if (mod.modifierQuantity == 1) {
                                "   "
                            } else {
                                "" + mod.modifierQuantity + "x "
                            }*/ "   " + mod.modifierQuantity.toString() + "x " + mod.name
                            )


                        }


                    }
                    if (obj.orderItems.get(i).note != null && obj.orderItems.get(i).note.isNotEmpty()) {
                        cloudPrinter.printText("  Note:" + obj.orderItems.get(i).note)
                    }

                }
            }

            if (obj.orderNote.isNotEmpty()) {

                cloudPrinter?.lineFeed(1)
                cloudPrinter?.setCharacterSize(2, 1)
                cloudPrinter?.setBoldMode(true)
                cloudPrinter?.printText("Order Note:-" + obj.orderNote)
            }

            Log.e(TAG, "customerName:  ${obj.customerName}")
            if (obj.orderType != Constants.DINE_IN && obj.orderType != Constants.DINE_IN_SPACE && obj.customerName != null && obj.customerName.isNotEmpty()) {
                cloudPrinter.let { addDoubleDotLineForSunmiQueue(it) }
                cloudPrinter.lineFeed(1)
                cloudPrinter.printText("Customer Details:")
                cloudPrinter.setBoldMode(false)
                cloudPrinter.setCharacterSize(1, 1)
                cloudPrinter.setAlignment(AlignStyle.LEFT)
                cloudPrinter.printText(obj.customerName)
                if (obj.customerPhoneNo.isNotEmpty()) {
                    cloudPrinter.printText(obj.customerPhoneNo)
                }
                if (obj.customerAddress.isNotEmpty()) {
                    cloudPrinter.printText(obj.customerAddress)
                }


            }


            cloudPrinter.lineFeed(3)
            cloudPrinter.cutPaper(true)

            Log.d("checkCommitResultFo", "sendReceiptToPrintSunmi cutPaper")

            var flagIsComplete: Boolean = false

            if (checkConnect(cloudPrinter)) {
                Log.e(
                    "checkCommitResultFo",
                    "checkCommitResultForCloud flagIsComplete 1 = $flagIsComplete " + "checkOrderNot  ${
                        checkOrderIsProceedOrNot(
                            obj.orderID, cloudPrinter?.cloudPrinterInfo?.mac ?: ""
                        )
                    }  orderUpdateOrNot  ${obj.isOrderUpdated}"
                )



                if (obj.isOrderUpdated == false && checkOrderIsProceedOrNot(
                        obj.orderID, cloudPrinter.cloudPrinterInfo.name ?: ""
                    )
                ) {

                    flagIsComplete = true


                    val params = JsonObject()

                    var deleteUrl = ""
                    try {
                        if (listOfPrintersData.get(
                                currentPrinterIndex
                            ).printerQueueModelList.isNotEmpty()
                        ) {

                            deleteUrl = prefProvider?.getValue(
                                Constants.BASE_URL_NEW, ""
                            ) + Constants.DELETE_QUEUE_ORDER_PHASE3 + listOfPrintersData.get(
                                currentPrinterIndex
                            ).printerQueueModelList.get(currentOrderIndex).id
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    Log.e(TAG, "DeleteUrl ${deleteUrl}")
                    Log.e(
                        TAG,
                        "checkDeleteSunmi  ${listOfPrintersData.get(currentPrinterIndex).printerName}"
                    )
                    params.addProperty("url", deleteUrl)
                    params.addProperty(
                        "name", listOfPrintersData.get(currentPrinterIndex).printerName
                    )
                    subscription?.perform("delete_order", params)


                    // delay(400)
                    checkForNextOrder()
                } else {
                    cloudPrinter.commitTransBuffer(object : ResultCallback {
                        override fun onComplete() {
                            if (queueInProgressList.isNotEmpty() && queueInProgressList.containsKey(
                                    cloudPrinter.cloudPrinterInfo.name
                                )
                            ) {
                                var list =
                                    queueInProgressList.get(cloudPrinter.cloudPrinterInfo.name)
                                        ?: arrayListOf()
                                if (list.isNotEmpty() && list.contains(obj.orderID)) {
                                    list.remove(obj.orderID)
                                    queueInProgressList.set(
                                        cloudPrinter.cloudPrinterInfo.name, list
                                    )

                                }
                            }


                            if (queueOrderList.containsKey(cloudPrinter.cloudPrinterInfo.name)) {
                                var list = queueOrderList.get(cloudPrinter.cloudPrinterInfo.name)
                                    ?: arrayListOf()
                                list.add(obj.orderID)
                                cloudPrinter.cloudPrinterInfo.name?.let {
                                    queueOrderList.set(it, list)
                                }

                            } else {
                                var list: ArrayList<String> = arrayListOf()
                                list.add(obj.orderID)

                                cloudPrinter.cloudPrinterInfo?.name?.let {
                                    queueOrderList.put(
                                        it, list
                                    )
                                }

                            }
                            //isQueueRunning = false
                            Log.e(
                                "onComplete2nd()",
                                "currentOrderIndex = $currentOrderIndex :: currentPrinterIndex=$currentPrinterIndex"
                            )
                            if (flagIsComplete == false) {
                                flagIsComplete = true


                                val params = JsonObject()

                                var deleteUrl = ""
                                try {
                                    if (listOfPrintersData.get(
                                            currentPrinterIndex
                                        ).printerQueueModelList.isNotEmpty()
                                    ) {

                                        deleteUrl = prefProvider?.getValue(
                                            Constants.BASE_URL_NEW, ""
                                        ) + Constants.DELETE_QUEUE_ORDER_PHASE3 + listOfPrintersData.get(
                                            currentPrinterIndex
                                        ).printerQueueModelList.get(currentOrderIndex).id
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }

                                Log.e(TAG, "DeleteUrl ${deleteUrl}")
                                Log.e(
                                    TAG,
                                    "checkDeleteSunmi  ${listOfPrintersData.get(currentPrinterIndex).printerName}"
                                )
                                params.addProperty("url", deleteUrl)
                                params.addProperty(
                                    "name", listOfPrintersData.get(currentPrinterIndex).printerName
                                )
                                subscription?.perform("delete_order", params)


                                // delay(400)
                                checkForNextOrder()


                            }

                        }

                        override fun onFailed(p0: CloudPrinterStatus?) {

                            queueInProgressList.get(p0?.name)?.remove(
                                listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.get(
                                    currentOrderIndex
                                ).id.toString()
                            )

                            Log.e(
                                TAG, "getOrderDetailsID: ${
                                    listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.get(
                                        currentOrderIndex
                                    ).id.toString()
                                }"
                            )
                            Log.e(TAG, "getOrderDetailsList: ${Gson().toJson(queueInProgressList)}")

                            if (p0?.name.equals("RUNNING", true) == false) {
                                //isQueueRunning = false
                                if (p0?.name.equals("OUT_PAPER", true)) {

                                    sendNotification("Please fill the Paper in ${cloudPrinter.cloudPrinterInfo?.name}")
                                } else if (p0?.name.equals("COVER", true)) {
                                    sendNotification("Please close the cover of ${cloudPrinter.cloudPrinterInfo?.name}")

                                } else if (p0?.name.equals("UNKNOWN", true)) {
                                    sendNotification(
                                        "Printer - ${
                                            cloudPrinter.cloudPrinterInfo?.name
                                        } is Offline."
                                    )

                                }

                                checkForNextOrder()
                            }
                        }

                    })
                }

            } else {
                checkForNextOrder()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun checkOrderIsProceedOrNot(orderID: String, macAddress: String): Boolean {
        var list = queueOrderList.get(macAddress) ?: arrayListOf()

        if (list.isEmpty()) {
            return false
        } else if (list.contains(orderID)) {
            return true
        } else {
            return false
        }


    }

    private fun getQueueDataResponse(model: JsonElement) {

        if (model.asJsonObject.has("data")) {
            var dataList: JsonArray = model.asJsonObject.get("data").asJsonArray
            Log.e("listOfPrintersData", "data available}")

            if (dataList.size() != 0) {
                isQueueRunning = true
                var isDataAvailable = true
                dataList.forEach { it1 ->
                    var listofPrinterOrders: ArrayList<PrinterQueueModel> = arrayListOf()

                    if (it1.asJsonObject.has("orders")) {
                        disconnectSize0 = false

                        var ordersArray = it1.asJsonObject.get("orders").asJsonArray

                        /*   for (i in dataList) {
                               var orders = i.asJsonObject.get("orders").asJsonArray
                               isDataAvailable = orders.size() != 0
                           }

                           isCount -= 1
                           if (isCount == 0){
                               isDataAvailable = false
                               disconnectSize0 = true
                               Log.e(TAG,"onDisconnectSIZE = $isCount")
                               consumer?.disconnect()
                               isCount = 5
                             //  return
                           }*/

                        ordersArray.forEach {
                            Log.e(
                                TAG, "checkOrderInPRogress:  ${
                                    checkOrderIsInProgressOrNot(
                                        it1.asJsonObject.get("printer_name").asString,
                                        it.asJsonObject.get("id").asInt.toString()
                                    )
                                }"
                            )

                            if (!checkOrderIsInProgressOrNot(
                                    it1.asJsonObject.get("printer_name").asString,
                                    it.asJsonObject.get("id").asInt.toString()
                                )
                            ) {
                                if (queueInProgressList.containsKey(it1.asJsonObject.get("printer_name").asString)) {
                                    var list =
                                        queueInProgressList.get(it1.asJsonObject.get("printer_name").asString)
                                            ?: arrayListOf()
                                    list.add(it.asJsonObject.get("id").asInt.toString())

                                    queueInProgressList.set(
                                        it1.asJsonObject.get("printer_name").asString, list
                                    )


                                } else {
                                    var list: ArrayList<String> = arrayListOf()
                                    list.add(it.asJsonObject.get("id").asInt.toString())


                                    queueInProgressList.put(
                                        it1.asJsonObject.get("printer_name").asString, list
                                    )


                                }


                                var modelOrder = PrinterQueueModel()

                                modelOrder.id = it.asJsonObject.get("id").asInt
                                modelOrder.orderType = it.asJsonObject.get("order_type").asString

                                try {
                                    if (it.asJsonObject.has("delivery_type")) {
                                        if (it.asJsonObject.get("order_type").asString == Constants.PHONE_ORDER_) modelOrder.deliveryType =
                                            it.asJsonObject.get("delivery_type").asString
                                    }
                                } catch (e: Exception) {
                                    Log.d("KeyNotFound", "Exception")
                                }

                                modelOrder.dateAndTime =
                                    it.asJsonObject.get("date_and_time").asString
                                modelOrder.employeeName =
                                    if (it.asJsonObject.has("employee_name") == true) {
                                        /*it.asJsonObject.get("employee_name").asString ?:*/ ""
                                    } else {
                                        ""
                                    }
                                modelOrder.orderNote = it.asJsonObject.get("order_note").asString
                                modelOrder.isOrderUpdated =
                                    it.asJsonObject.get("is_updated").asBoolean


                                if (it.asJsonObject.get("order_type").asString.equals(
                                        Constants.DINE_IN, true
                                    ) || it.asJsonObject.get("order_type").asString.equals(
                                        Constants.DINE_IN_SPACE, true
                                    )
                                ) {

                                    if (it.asJsonObject.has("guest_attributes")) {
                                        var guestAttributes: ArrayList<GuestAttrQueue> =
                                            arrayListOf()
                                        var guestAtr =
                                            it.asJsonObject.get("guest_attributes").asJsonArray



                                        guestAtr.forEach { it5 ->
                                            if (it5.asJsonObject.has("order_items")) {

                                                var orderItems: ArrayList<CreateOrderResponse.Data.Order.OrderItem> =
                                                    arrayListOf()

                                                var itemsArray =
                                                    it5.asJsonObject.get("order_items").asJsonArray

                                                if (itemsArray.size() != 0) {

                                                    var listOfMod: ArrayList<CreateOrderResponse.Data.Order.OrderItem.OrderItemModifiers> =
                                                        arrayListOf()
                                                    itemsArray.forEach { it9 ->
                                                        if (it9.asJsonObject.has("modifiers")) {

                                                            var iteMod =
                                                                it9.asJsonObject.get("modifiers").asJsonArray


                                                            iteMod.forEach {
                                                                listOfMod.add(
                                                                    CreateOrderResponse.Data.Order.OrderItem.OrderItemModifiers(
                                                                        id = it.asJsonObject.get("id").asInt,
                                                                        orderItemId = it.asJsonObject.get(
                                                                            "order_item_id"
                                                                        ).asInt,
                                                                        name = it.asJsonObject.get("name").asString,
                                                                        quantity = it.asJsonObject.get(
                                                                            "quantity"
                                                                        ).asInt,
                                                                        modifierSetId = 0,
                                                                        isModifier = true,
                                                                        modifierQuantity = it.asJsonObject.get(
                                                                            "modifier_quantity"
                                                                        ).asInt,
                                                                        createdAt = "",
                                                                        updatedAt = "",
                                                                        totalPrice = 0.0,
                                                                        orderId = 0,
                                                                        price = 0.0
                                                                    )
                                                                )

                                                            }


                                                        }


                                                        orderItems.add(
                                                            CreateOrderResponse.Data.Order.OrderItem(
                                                                categoryId = it9.asJsonObject.get("category_id").asInt,
                                                                completedInKitchen = false,
                                                                discountType = "",
                                                                discountAmount = 0.0,
                                                                discountId = 0,
                                                                employeeId = 0,
                                                                float = 0.0,
                                                                id = it9.asJsonObject.get("order_item_id").asInt,
                                                                isPrinted = false,
                                                                isPaid = false,
                                                                itemId = it9.asJsonObject.get("item_id").asInt,
                                                                orderId = it.asJsonObject.get("id").asInt,
                                                                itemName = it9.asJsonObject.get("name").asString,
                                                                totalPrice = 0.0,
                                                                timestamp = if (it9.asJsonObject.has(
                                                                        "message"
                                                                    )
                                                                ) {
                                                                    it9.asJsonObject.get("message").asString
                                                                } else {
                                                                    ""
                                                                },
                                                                quantity = it9.asJsonObject.get("quantity").asInt,
                                                                price = 0.0,
                                                                orderItemModifiers = listOfMod,
                                                                note = it9.asJsonObject.get("item_note").asString


                                                            )
                                                        )


                                                    }

                                                    guestAttributes.add(
                                                        GuestAttrQueue(
                                                            id = 0,
                                                            name = it5.asJsonObject.get("name").asString,
                                                            listOfItems = orderItems


                                                        )
                                                    )
                                                }

                                            }


                                        }

                                        modelOrder.guestAttributes = guestAttributes
                                        modelOrder.orderID =
                                            it.asJsonObject.get("id").asInt.toString()
                                    }


                                    listofPrinterOrders.add(modelOrder)

                                } else {
                                    var orderItems: ArrayList<CreateOrderResponse.Data.Order.OrderItem> =
                                        arrayListOf()

                                    if (it.asJsonObject.has("order_items")) {
                                        var orderItemsArray =
                                            it.asJsonObject.get("order_items").asJsonArray

                                        orderItemsArray.forEach { it1 ->
                                            var listOfMod: ArrayList<CreateOrderResponse.Data.Order.OrderItem.OrderItemModifiers> =
                                                arrayListOf()

                                            if (it1.asJsonObject.has("modifiers")) {
                                                var modList =
                                                    it1.asJsonObject.get("modifiers").asJsonArray
                                                if (modList.size() != 0) {
                                                    modList.forEach {
                                                        listOfMod.add(
                                                            CreateOrderResponse.Data.Order.OrderItem.OrderItemModifiers(
                                                                id = it.asJsonObject.get("id").asInt,
                                                                orderItemId = it.asJsonObject.get("order_item_id").asInt,
                                                                name = it.asJsonObject.get("name").asString,
                                                                quantity = it.asJsonObject.get("quantity").asInt,
                                                                modifierSetId = 0,
                                                                isModifier = true,
                                                                modifierQuantity = it.asJsonObject.get(
                                                                    "modifier_quantity"
                                                                ).asInt,
                                                                createdAt = "",
                                                                updatedAt = "",
                                                                totalPrice = 0.0,
                                                                orderId = 0,
                                                                price = 0.0
                                                            )
                                                        )

                                                    }

                                                }


                                            }

                                            orderItems.add(
                                                CreateOrderResponse.Data.Order.OrderItem(
                                                    categoryId = it1.asJsonObject.get("category_id").asInt,
                                                    completedInKitchen = false,
                                                    discountType = "",
                                                    discountAmount = 0.0,
                                                    discountId = 0,
                                                    employeeId = 0,
                                                    float = 0.0,
                                                    id = it1.asJsonObject.get("order_item_id").asInt,
                                                    isPrinted = false,
                                                    isPaid = false,
                                                    itemId = it1.asJsonObject.get("item_id").asInt,
                                                    orderId = it.asJsonObject.get("id").asInt,
                                                    itemName = it1.asJsonObject.get("name").asString,
                                                    totalPrice = 0.0,
                                                    timestamp = it1.asJsonObject.get("message").asString,
                                                    quantity = it1.asJsonObject.get("quantity").asInt,
                                                    price = 0.0,
                                                    orderItemModifiers = listOfMod,
                                                    note = it1.asJsonObject.get("item_note").asString


                                                )
                                            )
                                        }

                                        modelOrder.orderItems = orderItems
                                        modelOrder.orderID =
                                            it.asJsonObject.get("id").asInt.toString()


                                    }
                                    if (it.asJsonObject.has("customer_details")) {

                                        if (it.asJsonObject.get("customer_details").isJsonObject) {
                                            var objCustomer =
                                                it.asJsonObject.get("customer_details").asJsonObject
                                            modelOrder.customerName =
                                                objCustomer.get("first_name").asString + " " + objCustomer.get(
                                                    "last_name"
                                                ).asString
                                            modelOrder.customerPhoneNo =
                                                objCustomer.get("mobile_number").asString
                                            modelOrder.customerAddress =
                                                objCustomer.get("address").asString

                                        }

                                    }

                                    listofPrinterOrders.add(modelOrder)
                                }

                            }
                        }

                    }

                    listOfPrintersData.add(
                        PrinterJSONElementData(
                            printerName = it1.asJsonObject.get("printer_name").asString,
                            macAddress = it1.asJsonObject.get("mac_address").asString,
                            ipAddress = it1.asJsonObject.get("ip_address").asString,
                            modelName = it1.asJsonObject.get("modal_name").asString,
                            printerQueueModelList = listofPrinterOrders,
                            portNo = it1.asJsonObject.get("port_no").asInt

                        )
                    )
                    Log.e("listOfPrintersData", "listOfPrintersData ${listOfPrintersData.size}")
                }
                var isDataGot = false

                for (i in 0 until listOfPrintersData.size) {

                    if (listOfPrintersData.get(i).printerQueueModelList.isNotEmpty()) {
                        isDataGot = true
                        currentPrinterIndex = i
                        currentOrderIndex = 0
                        break
                    }
                }

                Log.e(TAG, "isDataGot: ${isDataGot}")
                if (isDataGot == true) {

                    if (listOfPrintersData.get(0).printerName.contains("CloudPrint_", true)) {
                        Log.e("checkCommitResult", "checkCommitResultForCloud 88")
                        sendDataToPrintToSunmi()

                    } else {
                        //call action cable here


                        currentOrderIndex = 0
                        currentPrinterIndex = 0
                        Log.e(TAG, "callActionCalledRun 4")


                        val params = JsonObject()
                        params.addProperty("id", prefProvider?.getValueInt(LOCATION_ID, 0))
                        params.addProperty(
                            "url", prefProvider?.getValue(
                                Constants.BASE_URL_NEW, ""
                            ) + Constants.CREATE_QUEUE_PRINTER_PHASE3
                        )
                        Log.e(
                            TAG, "checkID 2: ${
                                prefProvider?.getValueInt(
                                    LOCATION_ID, 0
                                )
                            }  checkURL 2:  ${
                                prefProvider?.getValue(
                                    Constants.BASE_URL_NEW, ""
                                ) + Constants.CREATE_QUEUE_PRINTER_PHASE3
                            }"
                        )

                        if (reConnectCount >= 10) {
                            consumer?.disconnect()
                            reConnectPrinterQueue()

                        } else {
                            reConnectCount += 1
                            isQueueRunning = false
                            subscription?.perform("received", params)
                        }


                    }

                } else {
                    //call action cable again here


                    Log.e(TAG, "callActionCalledRun 5")

                    currentOrderIndex = 0
                    currentPrinterIndex = 0


                    val params = JsonObject()
                    params.addProperty("id", prefProvider?.getValueInt(LOCATION_ID, 0))
                    params.addProperty(
                        "url", prefProvider?.getValue(
                            Constants.BASE_URL_NEW, ""
                        ) + Constants.CREATE_QUEUE_PRINTER_PHASE3
                    )
                    Log.e(
                        TAG, "checkID 3: ${
                            prefProvider?.getValueInt(
                                LOCATION_ID, 0
                            )
                        }  checkURL 3:  ${
                            prefProvider?.getValue(
                                Constants.BASE_URL_NEW, ""
                            ) + Constants.CREATE_QUEUE_PRINTER_PHASE3
                        }"
                    )
                    if (reConnectCount >= 10) {
                        consumer?.disconnect()

                        reConnectPrinterQueue()

                    } else {
                        reConnectCount += 1
                        Log.e(TAG, "checkAfterCountIncreased:")
                        lifecycleScope.launch(Dispatchers.IO) {
                            delay(1500)

                            isQueueRunning = false
                            subscription?.perform("received", params)
                        }
                    }


                }


            } else {


                Log.e(TAG, "callActionCalledRun 6")

                currentOrderIndex = 0
                currentPrinterIndex = 0

                val params = JsonObject()
                params.addProperty("id", prefProvider?.getValueInt(LOCATION_ID, 0))
                params.addProperty(
                    "url", prefProvider?.getValue(
                        Constants.BASE_URL_NEW, ""
                    ) + Constants.CREATE_QUEUE_PRINTER_PHASE3
                )

                if (reConnectCount >= 10) {
                    consumer?.disconnect()

                    reConnectPrinterQueue()

                } else {
                    reConnectCount += 1
                    lifecycleScope.launch {
                        delay(3000)
                        isQueueRunning = false
                        subscription?.perform("received", params)

                    }
                }


            }

        } else {

            Log.e(TAG, "callActionCalledRun 6")

            currentOrderIndex = 0
            currentPrinterIndex = 0

            val params = JsonObject()
            params.addProperty("id", prefProvider?.getValueInt(LOCATION_ID, 0))
            params.addProperty(
                "url", prefProvider?.getValue(
                    Constants.BASE_URL_NEW, ""
                ) + Constants.CREATE_QUEUE_PRINTER_PHASE3
            )
            if (reConnectCount >= 10) {
                consumer?.disconnect()
                reConnectPrinterQueue()

            } else {
                reConnectCount += 1
                lifecycleScope.launch {
                    delay(3000)
                    isQueueRunning = false
                    subscription?.perform("received", params)
                }
            }


        }


    }

    private fun checkOrderIsInProgressOrNot(printerName: String, orderId: String): Boolean {
        var listOfInProg = queueInProgressList.get(printerName) ?: arrayListOf()
        if (listOfInProg.isEmpty()) {
            return false

        } else if (listOfInProg.contains(orderId)) {
            return true
        } else {
            return false
        }


    }

    private fun sendDataToPrintToSunmi() {
        Log.e("checkCommitResultF", "checkCommitResultForCloud flagIsComplete 2 ")

        var cloudPrinter: CloudPrinter = CloudPrinterBuilder.buildPrinter(
            listOfPrintersData.get(currentPrinterIndex).printerName,
            listOfPrintersData.get(currentPrinterIndex).ipAddress,
            listOfPrintersData.get(currentPrinterIndex).portNo
        )

        var checkPassOrder = false
        cloudPrinter.connect(this@MainActivity, object : ConnectCallback {
            override fun onConnect() {
                Log.e("checkCommitResultFor", "checkCommitResultForCloud  33 cloudPrinter.")
                Log.e(
                    "checkCommitResultFo",
                    "checkCommitResultForCloud  name = original ${cloudPrinter.cloudPrinterInfo.name}"
                )

                Log.e(
                    "checkCommitResultFo",
                    "checkCommitResultForCloud  address =  ${cloudPrinter.cloudPrinterInfo.address}"
                )
                Log.e(
                    "checkCommitResultFo",
                    "checkCommitResultForCloud  mac =  ${cloudPrinter.cloudPrinterInfo.mac}"
                )
                Log.e(TAG, "onConnected 11")
                currentCloudPrinter = cloudPrinter
                sendReceiptToPrintSunmi(cloudPrinter)

                if (previousPrinterAddress == cloudPrinter.cloudPrinterInfo.address && previousPrinterName == cloudPrinter.cloudPrinterInfo.name) {
                    Log.e("checkCommitResultFo", "duplicate order")

                } else {


                }

                previousPrinterAddress = cloudPrinter.cloudPrinterInfo.address
                previousPrinterName = cloudPrinter.cloudPrinterInfo.name


            }

            override fun onFailed(p0: String?) {
                Log.e(TAG, "connectionFailed 11  ${p0}")

                Log.e(
                    TAG, "getOrderDetailsID: ${
                        Gson().toJson(listOfPrintersData)
                    }"
                )
                Log.e(TAG, "getOrderDetailsList: ${Gson().toJson(queueInProgressList)}")


                try {


                    queueInProgressList.set(
                        cloudPrinter?.cloudPrinterInfo?.name ?: "", arrayListOf()
                    )
                    /*if (queueInProgressList.isNotEmpty() && queueInProgressList.get(cloudPrinter?.cloudPrinterInfo?.name)
                            ?.contains(
                                listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.get(
                                    currentOrderIndex
                                ).id.toString()
                            ) == true && queueInProgressList.get(cloudPrinter?.cloudPrinterInfo?.name)
                            ?.isNotEmpty() == true
                    ) {
                        queueInProgressList.get(cloudPrinter?.cloudPrinterInfo?.name)?.remove(
                            listOfPrintersData?.get(currentPrinterIndex)?.printerQueueModelList?.get(
                                currentOrderIndex
                            )?.id?.toString()
                        )

                    }*/

                } catch (e: Exception) {
                    e.printStackTrace()
                }
                if (checkPassOrder == false) {
                    checkPassOrder = true
                    sendNotification("Printer - ${cloudPrinter.cloudPrinterInfo.name}  " + p0)
                    checkForNextOrder(true)
                }
            }

            override fun onDisConnect() {

                Log.e(TAG, "disconnected 11")
                try {

                    queueInProgressList.set(
                        cloudPrinter?.cloudPrinterInfo?.name ?: "", arrayListOf()
                    )
                    /*queueInProgressList.get(cloudPrinter?.cloudPrinterInfo?.name)?.forEach {
                        queueInProgressList.get(cloudPrinter?.cloudPrinterInfo?.name)?.remove(it)

                    }*/
                    /* if (queueInProgressList.isNotEmpty() && queueInProgressList.get(cloudPrinter?.cloudPrinterInfo?.name)
                             ?.isNotEmpty() == true
                     ) {
                         queueInProgressList.get(cloudPrinter?.cloudPrinterInfo?.name)?.remove(
                             listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.get(
                                 currentOrderIndex
                             ).id.toString()
                         )

                         Log.e(
                             TAG,
                             "getOrderDetailsID: ${
                                 listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.get(
                                     currentOrderIndex
                                 ).id.toString()
                             }"
                         )
                         Log.e(TAG, "getOrderDetailsList: ${Gson().toJson(queueInProgressList)}")

                     }*/
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                if (checkPassOrder == false) {
                    checkPassOrder = true
                    checkForNextOrder(true)
                }

            }

        })


    }

    private fun checkForNextOrder(isCurrentPrinterFailed: Boolean = false) {

        if (listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.isNotEmpty() && listOfPrintersData.get(
                currentPrinterIndex
            ).printerQueueModelList.size - 1 > currentOrderIndex && isCurrentPrinterFailed == false
        ) {
            currentOrderIndex += 1
            Log.e("checkCommitResultFo", "checkCommitResultForCloud 89")
            sendDataToPrintToSunmi()

        } else {
            Log.e(
                TAG,
                "checkELseLAs currentPrinterIn: ${currentPrinterIndex} currentOrderIn:  ${currentOrderIndex}"
            )
            if (listOfPrintersData.size - 1 > currentPrinterIndex) {
                var isBreakIn = false
                for (i in currentPrinterIndex + 1 until listOfPrintersData.size) {
                    Log.e(TAG, "checkNextIValue  ${i}")
                    if (listOfPrintersData.get(i).printerQueueModelList.isNotEmpty()) {
                        isBreakIn = true
                        currentPrinterIndex = i
                        currentOrderIndex = 0
                        break
                    }

                }

                if (isBreakIn) {
                    Log.e("checkCommitResultFo", "checkCommitResultForCloud 90")
                    sendDataToPrintToSunmi()
                } else {
                    //call action cable here


                    Log.e(TAG, "callActionCalledRun 1")

                    currentOrderIndex = 0
                    currentPrinterIndex = 0


                    val params = JsonObject()
                    params.addProperty("id", prefProvider?.getValueInt(LOCATION_ID, 0))
                    params.addProperty(
                        "url", prefProvider?.getValue(
                            Constants.BASE_URL_NEW, ""
                        ) + Constants.CREATE_QUEUE_PRINTER_PHASE3
                    )

                    if (reConnectCount >= 10) {
                        consumer?.disconnect()
                        reConnectPrinterQueue()

                    } else {
                        reConnectCount += 1
                        Log.e(TAG, "CheckNewReceived Method")
                        isQueueRunning = false
                        subscription?.perform("received", params)
                    }


                }

            } else {

                //call action cable here


                Log.e(TAG, "callActionCalledRun 2")

                currentOrderIndex = 0
                currentPrinterIndex = 0


                val params = JsonObject()
                params.addProperty("id", prefProvider?.getValueInt(LOCATION_ID, 0))
                params.addProperty(
                    "url", prefProvider?.getValue(
                        Constants.BASE_URL_NEW, ""
                    ) + Constants.CREATE_QUEUE_PRINTER_PHASE3
                )
                if (reConnectCount >= 10) {
                    consumer?.disconnect()
                    reConnectPrinterQueue()

                } else {
                    reConnectCount += 1
                    isQueueRunning = false
                    subscription?.perform("received", params)
                }


            }
        }

    }

    private fun demoPrinterQueue() {

        viewModelPrinter.getKitchenPrinterList().observe(this) {
            when (it.status) {
                Status.SUCCESS -> {
                    if (isKitchenFlag == false) {
                        isKitchenFlag = true


                        globalListPrinters = arrayListOf()

                        globalListPrinters.add(TmpPrinterModel("Chicken Masala", 0))
                        globalListPrinters.add(TmpPrinterModel("Chicken Masala", 0))
                        globalListPrinters.add(TmpPrinterModel("Chicken Masala", 0))
                        globalListPrinters.add(TmpPrinterModel("Chicken Masala", 0))
                        globalListPrinters.add(TmpPrinterModel("Chicken Masala", 0))
                        globalListPrinters.add(TmpPrinterModel("Chicken Masala", 0))
                        globalListPrinters.add(TmpPrinterModel("Chicken Masala", 0))
                        globalListPrinters.add(TmpPrinterModel("Chicken Masala", 0))
                        globalListPrinters.add(TmpPrinterModel("Chicken Masala", 0))
                        globalListPrinters.add(TmpPrinterModel("Chicken Masala", 0))
                        globalListPrinters.add(TmpPrinterModel("Chicken Masala", 0))
                        globalListPrinters.add(TmpPrinterModel("Chicken Masala", 0))
                        globalListPrinters.add(TmpPrinterModel("Chicken Masala", 0))

                        //list.add(TmpPrinterModel("Chicken Paneer", 0))
                        /* list.add(TmpPrinterModel("Chicken Cheese", 1))
                         list.add(TmpPrinterModel("Chicken Naan", 1))
                         list.add(TmpPrinterModel("Chicken Lababdar", 1))*/


                        printerList = it.data ?: arrayListOf()

                        Log.e("checkPrinterListSize", "printerListSize  ${printerList.size}")
                        Log.e("checkPrinterListSize", "datalist size  ${globalListPrinters.size}")

                        val printer = Printer(Printer.TM_U220, Printer.MODEL_ANK, this)
                        if (globalListPrinters.isNotEmpty() && printerList.isNotEmpty()) {
                            generateReceipt(
                                printer,
                                printerList[printerList.size - 1],
                                globalListPrinters[globalListPrinters.size - 1],
                                globalListPrinters.size - 1
                            )
                        }

                        /* globalListPrinters.forEach {

                             generateReceipt(
                                 printer,
                                 printerList[printerList.size - 1],
                                 it,
                                 0
                             )
                         }*/
//                        Thread.sleep(5000)

                        /*  for (i in 0 until list.size) {
                              for (j in 0 until printerList.size) {
                                  Log.e("checkPrinterListSize","dataentryPrint ${i}  and new ${j}")





                              }


                          }*/
                    }


                }

                Status.ERROR -> {


                }

                Status.LOADING -> {

                }


            }
        }

    }

    private fun generateReceipt(
        printer: Printer,
        kitchenReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        tmpPrinterModel: TmpPrinterModel,
        orderId: Int
    ) {
        Log.e("checkId", "orderId:  ${orderId}")




        try {
            Log.e(
                "checkIpaddress",
                "receivedIpAddress  ${kitchenReceiptPrinters.ipAddress}  and checkaddress ${isPrinterOnline}"
            )

            var printerAdd =
                if (kitchenReceiptPrinters.printer_type == Constants.BLUETOOTH) "BT:" + kitchenReceiptPrinters.macAddress else "TCP:" + kitchenReceiptPrinters.ipAddress


            Log.e("checkPrinterConnection", "checkStatus  ${Gson().toJson(printer.status)}")
            if (isPrinterOnline == false) {


                printer.connect(
                    printerAdd, Printer.PARAM_DEFAULT
                )

            }
            printer.startMonitor()




            printer.setReceiveEventListener { printer, i, printerStatusInfo, s ->

                Log.e(
                    TAG,
                    "PrinterEvent  ${Gson().toJson(printerStatusInfo)} other1 ${s}  other2 ${i}"
                )


                if (printerStatusInfo.errorStatus == 0) {
                    printer.endTransaction()
                    printer.clearCommandBuffer()
                    globalListPrinters.removeAt(orderId)
                    makeCallReumeQueuePrint(printer)
                }

                /*if (printerStatusInfo.online == 1) {
                    try {
                        printer.disconnect()

                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                    }
                }*/
            }


            isPrinterOnline = true



            printer.addFeedUnit(30)
            printer.addFeedLine(2)

            printer.addTextFont(Builder.FONT_E)
            printer.addTextAlign(Builder.ALIGN_CENTER)
            printer.addTextLang(Builder.LANG_EN)
            printer.addTextSize(2, 2)
            printer.addTextStyle(
                Builder.FALSE, Builder.FALSE, Builder.TRUE, Builder.COLOR_1
            )

            printer.addText("OrderID:" + orderId + 1)
            printer.addFeedLine(1)
            printer.addFeedUnit(30)
            printer.addFeedLine(1)


            printer.addTextFont(Builder.FONT_E)
            printer.addTextAlign(Builder.ALIGN_LEFT)
            printer.addTextLang(Builder.LANG_EN)
            printer.addTextSize(2, 2)
            printer.addTextStyle(
                Builder.FALSE, Builder.FALSE, Builder.TRUE, Builder.COLOR_1
            )


            printer.addText(tmpPrinterModel.ItemName)
            printer.addFeedLine(1)
            printer.addFeedUnit(30)
            printer.addFeedLine(1)
            printer.addCut(Builder.CUT_FEED)

            printer.beginTransaction()
            try {
                Log.e("SendDataHowMuchTime", "checkTimePrin")
                printer.sendData(Printer.PARAM_DEFAULT)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }


        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

    }

    private fun makeCallReumeQueuePrint(printer: Printer) {
        if (globalListPrinters.isNotEmpty() && printerList.isNotEmpty()) {
            generateReceipt(
                printer,
                printerList[printerList.size - 1],
                globalListPrinters[globalListPrinters.size - 1],
                globalListPrinters.size - 1
            )
        }


    }

    private fun connectionActionCable() {
//        val uri = URI("wss://hugepos.com/cable")
        val uri = URI("wss://pays.app/cable")
        consumer = ActionCable.createConsumer(uri)

        val appearanceChannel = Channel("KitchenChannel")
        subscription = consumer?.subscriptions?.create(appearanceChannel)

        if (subscription != null) {
            subscription?.onConnected {

                Log.e(TAG, "onActionConnected")
                val params = JsonObject()
                params.addProperty("id", prefProvider?.getValueInt(LOCATION_ID, 0))
                params.addProperty("url", BASE_URL + Constants.CREATE_QUEUE_PRINTER)
                isQueueRunning = false
                subscription?.perform("received", params)


            }?.onRejected {
                Log.e(TAG, "onActiononRejected")

            }?.onReceived {
                Log.e(TAG, "onActiononReceived  " + Gson().toJson(it))


            }?.onDisconnected {
                Log.e(TAG, "onActiononDisconnected")

            }?.onFailed {
                Log.e(TAG, "onActiononFailed")
                //subscription = consumer?.subscriptions?.create(appearanceChannel)
                try {

                    /*subscription = consumer?.subscriptions?.create(appearanceChannel)
                    val params = JsonObject()
                    params.addProperty("id", locationId)
                    subscription?.perform("received", params)*/
                    consumer?.connect()

                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }

            }
        } else {
            Log.e(TAG, "SubscriptionNull")
        }

        if (consumer != null) {
            consumer?.connect()
        } else {
            Log.e(TAG, "ConsumerNull")
        }

    }


    @SuppressLint("HardwareIds")
    fun getDeviceId(): String {
        return Settings.Secure.getString(
            contentResolver, Settings.Secure.ANDROID_ID
        )
    }

    fun alertLogout() {
        alert("", "Are you sure you want to Logout?") {
            this.positiveButton("Logout") {
                viewModel.logoutAPI()
            }
            this.negativeButton("Cancel") {}

        }
    }

    private val wifiStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressLint("RestrictedApi")
        override fun onReceive(context: Context, intent: Intent) {
//            LogUtil.logE(TAG,"customerPrinterList  ${Gson().toJson(customerPrinterList)}")

            /*kitchenPrinterList.forEach {
                println("customerPrinterList " + it.name)
            }


            val data = Data.Builder()
                .putString("kitchenPrinterList", Gson().toJson(kitchenPrinterList))
                .put("kitchenSettingData", Gson().toJson(customerSettingModel))
                .put("location_id", prefProvider?.getValueInt(Constants.LOCATION_ID, 0))
                .put("base_url", prefProvider?.getValue(Constants.BASE_URL_NEW, ""))
                .build()

            val uploadWorkRequest =
                PeriodicWorkRequest.Builder(UploadWorker::class.java, 5, TimeUnit.SECONDS)
                    .setInputData(data)
                    .build()

            val workManager = WorkManager.getInstance(applicationContext)
            try {


                workManager.enqueueUniquePeriodicWork(
                    "demo",
                    ExistingPeriodicWorkPolicy.REPLACE,
                    uploadWorkRequest
                )
            } catch (e: java.lang.Exception) {
                LogUtil.logE(TAG, "printerQueueLog  ${e.message.toString()}")
                e.printStackTrace()
            }
        */
        }
    }


    private fun logout() {
        prefProvider?.setValue(Constants.AUTH_TOKEN, "")
        navController?.navigate(R.id.action_global_login)
    }

    override fun onResume() {
        super.onResume()
        if (prefProvider?.getValueboolean(
                IS_PRINTER_QUEUE_ENABLE, false
            ) == true && prefProvider?.getValueboolean(
                IS_MASTER_TERMINAL, false
            ) == true && consumer == null
        ) {

            connectActionCable()
        }

        updatePrinter = this
        if (this::presentation.isInitialized) {
            presentation.show()
            presentation.onDisplayChanged()
        }
        prefProvider?.setValue(UNIQUE_ID, getDeviceId())

        navController?.addOnDestinationChangedListener(listner)

        //Connecting Dynamic sync functionality

        if (isInternetAvailable()) {
            connectActionCableSYNCSETTINGS()
            try {

                dashboardViewModel.autoSyncEnabled.value = false
                dashboardViewModel.syncInventoryModule(false)
            } catch (e: Exception) {
            }
        } else {
            sendNotification("Please check your Network Connectivity.")
        }

//        locationId = inputData.getInt("location_id", 0)
//        baseUrl = inputData.getString("base_url").toString()
        locationId = PrefProvider(baseContext).getLocationId()
        baseUrl = PrefProvider(baseContext).getBaseUrl()


        dashboardViewModel.allInventoryItems.observe(this) { it ->
            if (it.data?.isEmpty() == true && navController?.currentDestination?.id == R.id.dashboardCategoryBoldPOS) {

                try {
                    val dialog = Dialog(this)
                    dialog.setContentView(R.layout.new_loading)
                    dialog.setCancelable(false)
                    dialog.setCanceledOnTouchOutside(false)
                    dialog.show()

                        Handler(mainLooper).postDelayed({ dialog.dismiss() }, 15000)

                } catch (e: Exception) {

                }


                runBlocking {

                    dashboardViewModel.apply {

                        syncInventoryModule(true, isMigrationOn = true)
                        syncSettingModule()


                        val orderTypes =
                            CoroutineScope(Dispatchers.IO).async { fetchOrderTypesFromServer() }
                                .await()

                        orderTypes.data?.data?.let { orderTypes ->
                            addOrderTypesToDatabase(orderTypes)
                        }


                        allOrderCounts("", "")
                    }
                    // allInventoryItems.removeObserver {  }
                }


            }
        }
    }

    //Dynamic SYNC

    private fun sendNotification(messageBody: String) {
        Log.d("sendNotification", "message = $messageBody")


        val channelId = mContext.getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(mContext, channelId)
            .setSmallIcon(R.drawable.ic_baseline_notifications_24)
            .setContentTitle(mContext.getString(R.string.app_name)).setContentText(messageBody)
            .setAutoCancel(true).setSound(defaultSoundUri)

        val notificationManager =
            mContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Channel human readable title", NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(
            System.currentTimeMillis().toInt()/* ID of notification */, notificationBuilder.build()
        )
    }


    fun connectActionCableSYNCSETTINGS() {
        // 1. Setup
        var requestURL = baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3

        Log.e("PrinterRefreshWorker", "requestURL = $requestURL")

        val uri = URI(Constants.PRINTER_QUEUE_CONNECTION_URL_SNACKPOS)
        MainActivity.consumer2 = ActionCable.createConsumer(uri)

        Log.d("PrinterRefreshWorker", "uri = $uri")

        // 2. Create subscription
        val appearanceChannel = Channel("SyncChannel")
        appearanceChannel.addParam("id", locationId)
        // appearanceChannel.addParam("id",prefProvider.getValueInt(LOCATION_ID,0))
        MainActivity.subscription2 =
            MainActivity.consumer2?.subscriptions?.create(appearanceChannel)

        if (MainActivity.subscription2 != null) {
            MainActivity.subscription2?.onConnected {
                Log.e(TAG2, "onActionConnected")
                val params = JsonObject()
                params.addProperty("location_id", locationId)
                //  params.addProperty("url", requestURL)
                MainActivity.subscription2?.perform("received", params)
            }?.onRejected {
                Log.e(TAG2, "onRejected")
                if (isInternetAvailable()) {
                    MainActivity.consumer2?.connect()
                } else {
                    sendNotification("Please check your Network Connectivity.")
                }

            }?.onReceived {
                Log.e(TAG2, "onReceived  MAIN ACTIVITY" + Gson().toJson(it))
                if (lastSyncTime == 0L || System.currentTimeMillis() - lastSyncTime > 900) {
                    lastSyncTime = System.currentTimeMillis()

                    if (it.asJsonObject.has("location_id"))
                        if (!it.asJsonObject.has("new_order"))
                            if (PrefProvider(baseContext).getLocationId() == it.asJsonObject.get(
                                    "location_id"
                                ).asInt
                            ) {
                                Log.e(TAG2, "onReceived  Inside" + Gson().toJson(it))
                                handleUpdatedData(it)
                            }
                    // handleUpdatedData(it)

                }


            }?.onDisconnected {


                Log.e(TAG2, "onDisconnected")
                if (isInternetAvailable()) {
                    MainActivity.consumer2?.connect()
                } else {
                    sendNotification("Please check your Network Connectivity.")
                }

            }?.onFailed {
                Log.e(TAG2, "onFailed")
                if (isInternetAvailable()) {
                    Handler(Looper.getMainLooper()).postDelayed(Runnable {
                        try {
                            MainActivity.consumer2?.connect()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, 25000)

                } else {
                    sendNotification("Please check your Network Connectivity.")
                }

            }
        }


        // 3. Establish connection
        MainActivity.consumer2?.connect()


    }

    private fun handleUpdatedData(it: JsonElement) {

        try {
            if (it.asJsonObject.has("setting_data")) {

                val setting_data = it.asJsonObject.get("setting_data")
                Log.e(TAG2, "call setting_data API")
                try {

                    if (setting_data.toString() == "true") {
                        Handler(mainLooper).post(object : Runnable {
                            override fun run() {
                                dashboardViewModel.autoSyncEnabled.value = false
                            }
                        })

                        //  PrefProvider(mContext).setValueInt(Constants.CAT_ID_SELECTED, 0)
                        dashboardViewModel.syncSettingModule()

                        if (com.pays.pos.ui.fragments.settings.hardware.printer.Printer.updatePrinter == null) {

                            updatePrinter?.updatePrinters()

                            Log.e(TAG2, "Setting DATA TRUE")


                        } else {
                            com.pays.pos.ui.fragments.settings.hardware.printer.Printer.updatePrinter?.updatePrinters()
                        }

                    } else {

                        if (it.asJsonObject.has("message")) {

                            sendNotification(it.asJsonObject.get("message").asString)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (it.asJsonObject.has("inventory_sync")) {
                val inventory_sync_data = it.asJsonObject.get("inventory_sync")

                if (inventory_sync_data.toString() == "true") {
                    val intent2 = Intent()
                    intent2.action = Constants.SYNC_NOTIFICATION
                    mContext.sendBroadcast(intent2)

//                    Handler(mainLooper).post(object:Runnable{
//                        override fun run() {
//                            dashboardViewModel.autoSyncEnabled.value = false
//                        }
//                    })

                    try {
                        dashboardViewModel.autoSyncEnabled.value = false
                    } catch (_: Exception) {
                    }

                    PrefProvider(mContext).setValueInt(Constants.CAT_ID_SELECTED, 0)
                    dashboardViewModel.syncInventoryModule(false)

//                    DashboardCategoryBoldPOS.syncDataCallback?.syncNotification()
                } else {

                    if (it.asJsonObject.has("message")) {

                        sendNotification(it.asJsonObject.get("message").asString)
                    }
                }
            }

            if (it.asJsonObject.has(Constants.SYNC_NOTIFICATION)) {
                val sync_data = it.asJsonObject.get(Constants.SYNC_NOTIFICATION)

                if (sync_data.toString() == "true") {
                    DashboardCategoryBoldPOS.syncDataCallback?.syncNotification()
                } else {

                    if (it.asJsonObject.has("message")) {

                        sendNotification(it.asJsonObject.get("message").asString)
                    }
                }
            }


            // Added to refresh online orders
            if (it.asJsonObject.has("cancelled_order") || it.asJsonObject.has("new_order")) {


                if (it.asJsonObject.has("new_order")) setSoundForOnlineOrder()

                val intent = Intent()
                intent.putExtra("message", "refresh")
                intent.action = Constants.ONLINE_ORDER_GET_NOTIFICATION
                sendBroadcast(intent)
            } else {
                dashboardViewModel.syncTaxes()
            }
        } catch (e: Exception) {
            Log.e(TAG2, "Exception ${e.message}")
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

    private fun isInternetAvailable(): Boolean {
        var result: Boolean
        val connectivityManager =
            applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        connectivityManager.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                it.getNetworkCapabilities(connectivityManager.activeNetwork)?.apply {
                    result = when {
                        hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                        hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                        hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                        else -> false
                    }
                    return result
                }
            } else {
                connectivityManager.activeNetworkInfo.also {
                    return it != null && it.isConnected
                }
            }
        }
        return false
    }

    override fun onPause() {
        super.onPause()
        try {
            if (consumer != null) {
                consumer?.disconnect()
                consumer = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // disconnectSocket()


        if (this::presentation.isInitialized) {
//            presentation.hide()
            presentation.onDisplayChanged()
        }
        navController?.removeOnDestinationChangedListener(listner)
    }

    fun enableDrawer() {
        binding.drawerLayout.openDrawer(GravityCompat.START)

    }

    private fun disableDrawer() {
        binding.drawerLayout.closeDrawer(GravityCompat.START)
    }

   /* override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }*/

    override fun onBackPressed() {
        /*if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }*/

        try {
            if (navController?.backStack?.last?.destination?.displayName?.contains("phoneOrderFragment") as Boolean) {
                prefProvider!!.setValueInt(Constants.CAT_ID_SELECTED, 0)
                prefProvider!!.setValue(Constants.REDIRECT_FROM, "")
                prefProvider!!.setValue(Constants.ORDER_TYPE, "")
                prefProvider!!.setValue(Constants.ORDER_TYPE_NAME, "")
                prefProvider!!.setValue(Constants.CUSTOMER_NAME, "")
                prefProvider!!.setValueboolean(Constants.LOYALTY_ADDED, false)

                //                navController?.popBackStack()
                try {
                    navController?.popBackStack()
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }

            } else {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    if (navController!!.currentDestination?.id?.equals(R.id.dashboardCategoryBoldPOS) == true) {
                        if (doubleBackToExitPressedOnce) {
                            super.onBackPressed()
                            return
                        }

                        this.doubleBackToExitPressedOnce = true
                        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT)
                            .show()

                        Handler(Looper.getMainLooper()).postDelayed(Runnable {
                            doubleBackToExitPressedOnce = false
                        }, 2000)
                    } else {
                        super.onBackPressed()
                    }
                }
            }
        } catch (e: java.lang.Exception) {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                super.onBackPressed()
            }
        }
    }

    private fun clearPreferences() {
        prefProvider?.setClear()
    }

    private fun observeShowProgress() {

        viewModel.showProgress.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(this)
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        }


        viewModel.logout.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    clearPreferences()
                    disableDrawer()
                    logout()
                    viewModel.clearTable()

                    prefProvider?.deleteValue(Constants.IS_PAX_CONNECTED)

                    prefProvider?.setValue(Constants.BASE_URL_NEW, BASE_URL)
                    prefProvider?.setValue(UNIQUE_ID, getDeviceId())
                    hostSelectionInterceptor?.setHostBaseUrl()


                }
            }
        }

    }

    //Capture Photo
    public fun capturePhoto() {
        selectedFilePath = null
        //Create a file to store the image
        var photoFile: File? = null
        try {
            photoFile = FileUtils.createImageOrVideoFile(this, Constants.MEDIA_TYPE_IMAGE)
        } catch (ex: IOException) {
            ex.printStackTrace()
            Toast.makeText(this, R.string.error_something_wrong, Toast.LENGTH_SHORT).show()
        }

        photoFile?.let { photo ->
            selectedFilePath = photo.absolutePath
            cameraUri = FileProvider.getUriForFile(
                this, "$APPLICATION_ID.provider", photo
            )
            val pictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            pictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
                pictureIntent.clipData = ClipData.newRawUri("", cameraUri)
                pictureIntent.addFlags(
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            if (pictureIntent.resolveActivity(this.packageManager) != null) {
                this.startActivityForResult(pictureIntent, Constants.REQUEST_GET_IMAGE_CAMERA)
            } else {
                Toast.makeText(
                    this, R.string.error_camera_app_not_found, Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == Constants.REQUEST_GET_IMAGE_CAMERA && cameraUri != null) {
                if (selectedFilePath == null && cameraUri != null) {
                    selectedFilePath = FileUtils.getPath(this, cameraUri!!)
                }
                //received new file path
                activityResultCallBack?.onReceivedCameraCapturedPath(
                    mediaType = Constants.MEDIA_TYPE_IMAGE, mediaPath = selectedFilePath
                )
            }
        }
    }

    interface ActivityResultCallBack {
        fun onReceivedCameraCapturedPath(mediaType: Int, mediaPath: String?)
    }

    fun requestLocationPermissions(): Boolean {
        val permissionsLocation = arrayOf<String>(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        return if ((ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED)
        ) {
            true
        } else {
            ActivityCompat.requestPermissions(
                this, permissionsLocation, Constants.REQUEST_LOCATION_PERMISSION
            )
            false
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        LogUtil.logE("!_@_", "$requestCode")
        when (requestCode) {
            Constants.REQUEST_LOCATION_PERMISSION -> if (permissions.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //permission with request code 1 granted
                LogUtil.logE("!_@_", "Permission Granted")
                requestCallBack?.invoke()
            } else {
                //permission with request code 1 was not granted
                LogUtil.logE("!_@_", "Permission not granted")
            }

            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    fun getRequestCallBack(requestGrantedCallBack: (() -> Unit)) {
        this.requestCallBack = requestGrantedCallBack
    }

    private var requestCallBack: (() -> Unit)? = null

    override fun onSupportNavigateUp(): Boolean {
        navController?.navigateUp()
        return super.onSupportNavigateUp()
    }

    fun getSpecificFragment(fragmentTag: Int): Fragment? {
        val navHostFragment: Fragment? = supportFragmentManager.findFragmentById(R.id.navHostFrag)
        if (navHostFragment?.childFragmentManager != null) {
            val fragmentList: List<Fragment> = navHostFragment.childFragmentManager.fragments
            for (fragment in fragmentList) {
                if (Constants.FRAGMENT_HARDWARE == fragmentTag && fragment is Hardware) {
                    return (fragment as Hardware)
                }
            }
        }
        return null
    }

    public var fragmentCallBack: ((Fragment?) -> Unit)? = null
    fun loadFragmentInSettings(fragment: Fragment?) {
        fragmentCallBack?.invoke(fragment)
    }

    override fun onPtrReceive(p0: Printer?, p1: Int, p2: PrinterStatusInfo?, p3: String?) {

        LogUtil.logE(TAG, "onPrintReceived")
        lifecycleScope.launch {
            var flag = printerQueueModelGlobal?.let { viewModelPrinter.checkDataisExistOrNot(it) }
            LogUtil.logE(TAG, "UpdateGetloag ${flag}")
            if (flag == false) {
                var listIds: ArrayList<Int> = arrayListOf()
                listIds.add(kitchenPrinterList[currentIndex].id)

                LogUtil.logE(TAG, "listIds  ${Gson().toJson(listIds)}")
                LogUtil.logE(TAG, "printerQueueId  ${printerQueueModelGlobal?.id ?: 0}")
                ThreadPoolManager.instance.executeTask(Runnable {
                    lifecycleScope.launch {
                        printerQueueModelGlobal?.id?.let {
                            viewModelPrinter.updateStatusPrinterQueue(
                                listIds, it
                            )

                            /*delay(2000)*/
                        }
                    }
                })

            }
            try {

                if (mPrinter?.status?.connection == 1) {
                    mPrinter?.stopMonitor()
                    mPrinter?.disconnect()
                    isPrinterQueueRun = false
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }

            /*try {
                delay(5000)
                isPrinterQueueRun = false

                arrayItems.removeAt(currentIndex)
                mPrinter?.clearCommandBuffer()
                mPrinter?.disconnect()


            } catch (e: java.lang.Exception) {
                isPrinterQueueRun = false
                getPrinterQueueData()
                LogUtil.logE("statusChangePRint", "PrinterSuccessDisconnectExcep")
                e.printStackTrace()
            }*/


        }


    }

    override fun onConnection(p0: Any?, p1: Int) {
        LogUtil.logE(TAG, "onConnection: ${p0}  ${p1}")
    }

    override fun onPtrStatusChange(p0: Printer?, p1: Int) {
        LogUtil.logE(TAG, "OnStatusChanged ${p1}")
    }

    override fun updatePrinters() {

        viewModel.updatePrintersData()
    }

    override fun reloadAdapter() {

    }

    override fun onComplete() {

    }

    override fun onFailed(p0: CloudPrinterStatus?) {

    }

    fun sendBroadCast() {

        Log.e(
            TAG, "checkPrinterQueueWorker:  ${
                checkUploadWorker(
                    PRINTER_QUEUE_BACKGROUND, this@MainActivity
                )
            }"
        )

        Log.e(
            TAG, "checkQUeue: ${
                prefProvider?.getValueboolean(
                    IS_PRINTER_QUEUE_ENABLE, false
                )
            }  checkMAsterRermi: ${
                prefProvider?.getValueboolean(
                    IS_MASTER_TERMINAL, false
                )
            }  consumer: ${consumer}"
        )

        if (prefProvider?.getValueboolean(
                IS_PRINTER_QUEUE_ENABLE, false
            ) == true && prefProvider?.getValueboolean(
                IS_MASTER_TERMINAL, false
            ) == true && isLocalMasterFlag == false /*&& consumer == null && isLocalMasterFlag == false */) {
            isLocalMasterFlag = true

            Log.e(TAG, "YesIN ACtionConnect")
            connectActionCable()

        } else if ((prefProvider?.getValueboolean(
                IS_PRINTER_QUEUE_ENABLE, false
            ) == false || prefProvider?.getValueboolean(
                IS_MASTER_TERMINAL, false
            ) == false) && consumer != null
        ) {

            consumer?.disconnect()


        }



        if (prefProvider?.getValueboolean(
                IS_MASTER_TERMINAL, false
            ) == true && prefProvider?.getValueboolean(
                Constants.IS_PRINTER_QUEUE_STARTS, false
            ) == false && prefProvider?.getValueboolean(
                IS_PRINTER_QUEUE_ENABLE, false
            ) == true
        ) {
            prefProvider?.setValueboolean(Constants.IS_PRINTER_QUEUE_STARTS, true)
            prefProvider?.setValueboolean(Constants.CHECK_QUEUE_CANCEL, false)
            getKitOne()

        } else if (prefProvider?.getValueboolean(IS_MASTER_TERMINAL, false) == false) {
            WorkManager.getInstance(this@MainActivity).cancelAllWork()
            val data = Data.Builder()
                //.putString("kitchenPrinterList", Gson().toJson(kitchenPrinterList))
                // .put("kitchenSettingData", Gson().toJson(kitchenSettingModel))
                .put("location_id", prefProvider?.getValueInt(LOCATION_ID, 0))
                .put("base_url", prefProvider?.getValue(Constants.BASE_URL_NEW, "")).put(
                    IS_PRINTER_QUEUE_ENABLE,
                    prefProvider?.getValueboolean(IS_PRINTER_QUEUE_ENABLE, false)
                ).put("is_cancel_work", true).build()
            prefProvider?.setValueboolean(Constants.CHECK_QUEUE_CANCEL, true)
            val uploadWorkRequest = OneTimeWorkRequest.Builder(
                UploadWorker2::class.java
            ).addTag(Constants.PRINTER_QUEUE_BACKGROUND).setInputData(data).build()


            val workManager = WorkManager.getInstance(this@MainActivity)

            try {

                workManager.enqueueUniqueWork(
                    Constants.PRINTER_QUEUE_BACKGROUND, ExistingWorkPolicy.KEEP, uploadWorkRequest
                )

            } catch (e: java.lang.Exception) {
                LogUtil.logE(TAG, "printerQueueLog  ${e.message.toString()}")
                e.printStackTrace()
            }
        } else {
            if (prefProvider?.getValueboolean(IS_PRINTER_QUEUE_ENABLE, false) == true) {
                val data = Data.Builder()
                    //.putString("kitchenPrinterList", Gson().toJson(kitchenPrinterList))
                    // .put("kitchenSettingData", Gson().toJson(kitchenSettingModel))
                    .put("location_id", prefProvider?.getValueInt(LOCATION_ID, 0))
                    .put("base_url", prefProvider?.getValue(Constants.BASE_URL_NEW, "")).put(
                        IS_PRINTER_QUEUE_ENABLE,
                        prefProvider?.getValueboolean(IS_PRINTER_QUEUE_ENABLE, false)
                    ).put("is_cancel_work", false).build()
                prefProvider?.setValueboolean(Constants.CHECK_QUEUE_CANCEL, false)
                val uploadWorkRequest = OneTimeWorkRequest.Builder(
                    UploadWorker2::class.java
                ).addTag(Constants.PRINTER_QUEUE_BACKGROUND).setInputData(data).build()


                val workManager = WorkManager.getInstance(this@MainActivity)

                try {

                    workManager.enqueueUniqueWork(
                        Constants.PRINTER_QUEUE_BACKGROUND,
                        ExistingWorkPolicy.REPLACE,
                        uploadWorkRequest
                    )

                } catch (e: java.lang.Exception) {
                    LogUtil.logE(TAG, "printerQueueLog  ${e.message.toString()}")
                    e.printStackTrace()
                }
            } else if (prefProvider?.getValueboolean(IS_PRINTER_QUEUE_ENABLE, false) == false) {
                WorkManager.getInstance(this@MainActivity).cancelAllWork()
                val data = Data.Builder()
                    //.putString("kitchenPrinterList", Gson().toJson(kitchenPrinterList))
                    // .put("kitchenSettingData", Gson().toJson(kitchenSettingModel))
                    .put("location_id", prefProvider?.getValueInt(LOCATION_ID, 0))
                    .put("base_url", prefProvider?.getValue(Constants.BASE_URL_NEW, "")).put(
                        IS_PRINTER_QUEUE_ENABLE,
                        prefProvider?.getValueboolean(IS_PRINTER_QUEUE_ENABLE, false)
                    ).put("is_cancel_work", true).build()
                prefProvider?.setValueboolean(Constants.CHECK_QUEUE_CANCEL, true)
                val uploadWorkRequest = OneTimeWorkRequest.Builder(
                    UploadWorker2::class.java
                ).addTag(Constants.PRINTER_QUEUE_BACKGROUND).setInputData(data).build()


                val workManager = WorkManager.getInstance(this@MainActivity)

                try {

                    workManager.enqueueUniqueWork(
                        Constants.PRINTER_QUEUE_BACKGROUND,
                        ExistingWorkPolicy.KEEP,
                        uploadWorkRequest
                    )

                } catch (e: java.lang.Exception) {
                    LogUtil.logE(TAG, "printerQueueLog  ${e.message.toString()}")
                    e.printStackTrace()
                }

            }
        }


        if (prefProvider?.getValueboolean(Constants.IS_FIRST_TIME_LOGIN, false) == true) {
            val data = Data.Builder()
                //.putString("kitchenPrinterList", Gson().toJson(kitchenPrinterList))
                // .put("kitchenSettingData", Gson().toJson(kitchenSettingModel))
                .put("location_id", prefProvider?.getValueInt(LOCATION_ID, 0))
                .put("base_url", prefProvider?.getValue(Constants.BASE_URL_NEW, "")).put(
                    IS_PRINTER_QUEUE_ENABLE,
                    prefProvider?.getValueboolean(IS_PRINTER_QUEUE_ENABLE, false)
                ).put("is_cancel_work", false).build()
            prefProvider?.setValueboolean(Constants.CHECK_QUEUE_CANCEL, false)
            val uploadWorkRequest = OneTimeWorkRequest.Builder(
                UploadWorker2::class.java
            ).addTag(Constants.PRINTER_QUEUE_BACKGROUND).setInputData(data).build()


            val workManager = WorkManager.getInstance(this@MainActivity)

            try {

                workManager.enqueueUniqueWork(
                    Constants.PRINTER_QUEUE_BACKGROUND, ExistingWorkPolicy.KEEP, uploadWorkRequest
                )

            } catch (e: java.lang.Exception) {
                LogUtil.logE(TAG, "printerQueueLog  ${e.message.toString()}")
                e.printStackTrace()
            }


        }

    }

}


private fun isPrintable(status: PrinterStatusInfo?): Boolean {
    if (status == null) {
        return false
    }
    if (status.connection == Printer.FALSE) {
        return false
    } else if (status.online == Printer.FALSE) {
        return false
    } else {
        return true
    }
    return true
}


private fun makeErrorMessage(status: PrinterStatusInfo): String? {
    var msg = ""
    if (status.online == Printer.FALSE) {
        msg += "Printer is Offline."
    } else if (status.connection == Printer.FALSE) {
        msg += "Please check your Printer Connection."
    } else if (status.coverOpen == Printer.TRUE) {
        msg += "Printer Cover is Open."
    } else if (status.paper == Printer.PAPER_EMPTY) {
        msg += "Feed Paper is Empty."
    } else if (status.paperFeed == Printer.TRUE || status.panelSwitch == Printer.SWITCH_ON) {
        msg += "Please check your Printer Connection."
    }
    /*if (status.errorStatus == Printer.MECHANICAL_ERR || status.errorStatus == Printer.AUTOCUTTER_ERR) {
        msg += getString(android.R.string.handlingmsg_err_autocutter)
        msg += getString(android.R.string.handlingmsg_err_need_recover)
    }*/
    /*if (status.errorStatus == Printer.UNRECOVER_ERR) {
        msg += getString(android.R.string.handlingmsg_err_unrecover)
    }*/

    /*  if (status.errorStatus == Printer.AUTORECOVER_ERR) {
          if (status.autoRecoverError == Printer.HEAD_OVERHEAT) {
              msg += getString(android.R.string.handlingmsg_err_overheat)
              msg += getString(android.R.string.handlingmsg_err_head)
          }
          if (status.autoRecoverError == Printer.MOTOR_OVERHEAT) {
              msg += getString(android.R.string.handlingmsg_err_overheat)
              msg += getString(android.R.string.handlingmsg_err_motor)
          }
          if (status.autoRecoverError == Printer.BATTERY_OVERHEAT) {
              msg += getString(android.R.string.handlingmsg_err_overheat)
              msg += getString(android.R.string.handlingmsg_err_battery)
          }
          if (status.autoRecoverError == Printer.WRONG_PAPER) {
              msg += getString(android.R.string.handlingmsg_err_wrong_paper)
          }
      }
      if (status.batteryLevel == Printer.BATTERY_LEVEL_0) {
          msg += getString(android.R.string.handlingmsg_err_battery_real_end)
      }*/
    return msg
}

private fun isInternetAvailable(): Boolean {
    var result: Boolean
    val connectivityManager = MainApplication.getInstance()
        ?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    connectivityManager.let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            it.getNetworkCapabilities(connectivityManager.activeNetwork)?.apply {
                result = when {
                    hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                    hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                    hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                    else -> false
                }
                return result
            }
        } else {
            connectivityManager.activeNetworkInfo.also {
                return it != null && it.isConnected
            }
        }
    }
    return false
}

private fun sendNotification(messageBody: String) {
    Log.d("sendNotification", "message = $messageBody")


    val channelId =
        MainApplication.getInstance()?.getString(R.string.default_notification_channel_id)
    val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    val notificationBuilder = NotificationCompat.Builder(
        MainApplication.getInstance()?.applicationContext!!, channelId.toString()
    ).setSmallIcon(R.drawable.ic_baseline_notifications_24)
        .setContentTitle(MainApplication.getInstance()?.getString(R.string.app_name))
        .setContentText(messageBody).setAutoCancel(true).setSound(defaultSoundUri)

    val notificationManager = MainApplication.getInstance()
        ?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Since android Oreo notification channel is needed.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId, "Channel human readable title", NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
    }

    notificationManager.notify(
        System.currentTimeMillis().toInt()/* ID of notification */, notificationBuilder.build()
    )
}

private fun checkConnect(cloudPrinter: CloudPrinter): Boolean {
    if (cloudPrinter == null) {
        sendNotification("Please connect the cloud printer first!")
        return false
    }
    if (!cloudPrinter.isConnected()) {
        sendNotification("Please connect the ${cloudPrinter.cloudPrinterInfo.name} printer first!")
        return false
    }
    return true
}


