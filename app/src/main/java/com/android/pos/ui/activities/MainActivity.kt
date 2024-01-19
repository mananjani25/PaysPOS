package com.android.pos.ui.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
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
import com.android.pos.R
import com.android.pos.data.model.PrinterQueueModel
import com.android.pos.data.model.TmpPrinterModel
import com.android.pos.data.model.responseModel.GetKitchenReceiptSettingsResponse
import com.android.pos.data.model.responseModel.PrinterResponse
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.IS_MASTER_TERMINAL
import com.android.pos.data.remote.Constants.IS_PRINTER_QUEUE_ENABLE
import com.android.pos.data.remote.Constants.LOCATION_ID
import com.android.pos.data.remote.Constants.UNIQUE_ID
import com.android.pos.data.repositories.UserRepository
import com.android.pos.databinding.ParentActivityBinding
import com.android.pos.di.ApiModule.BASE_URL
import com.android.pos.di.HostSelectionInterceptor
import com.android.pos.di.PrefProvider
import com.android.pos.di.RolePermission
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.ui.fragments.dashboard.bolddashboard.CustomDisplay
import com.android.pos.ui.fragments.dashboard.bolddashboard.DashboardCategoryBoldPOS
import com.android.pos.ui.fragments.dinein.DineInOrderTableViewModel
import com.android.pos.ui.fragments.loginscreen.PasscodeViewModel
import com.android.pos.ui.fragments.payment.OrderCompleteViewModel
import com.android.pos.ui.fragments.settings.hardware.Hardware
import com.android.pos.ui.fragments.settings.hardware.printer.UpdatePrinters
import com.android.pos.utils.*
import com.android.pos.utils.extensions.alert
import com.android.pos.utils.statusUtils.Status
import com.android.pos.utils.workmanager.ThreadPoolManager
import com.android.pos.utils.workmanager.UploadWorker2
import com.epson.epos2.ConnectionListener
import com.epson.epos2.printer.Printer
import com.epson.epos2.printer.PrinterStatusInfo
import com.epson.epos2.printer.ReceiveListener
import com.epson.epos2.printer.StatusChangeListener
import com.epson.eposprint.Builder
import com.felhr.usbserial.BuildConfig.APPLICATION_ID
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.hosopy.actioncable.ActionCable
import com.hosopy.actioncable.Channel
import com.hosopy.actioncable.Consumer
import com.hosopy.actioncable.Subscription
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.net.URI
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : BaseScannerActivity(), ReceiveListener, ConnectionListener,
    StatusChangeListener, UpdatePrinters {
    private var printerList: List<PrinterResponse.Data.KitchenReceiptPrinters> = arrayListOf()
    private val dashboardViewModel: DashBoardCategoryViewModel by viewModels()
    private val passcodeViewModel: PasscodeViewModel by viewModels()
    private var globalListPrinters: ArrayList<TmpPrinterModel> = arrayListOf()
    private var printerQueueModelGlobal: PrinterQueueModel? = null
    private var isPrinterQueueRun: Boolean = false
    private var kitchenPrinterList: List<PrinterResponse.Data.KitchenReceiptPrinters> =
        emptyList()
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
    var mPrinter: Printer? = null
    var arrayItems: ArrayList<PrinterQueueModel> = arrayListOf()
    var isKitchenFlag: Boolean = false
    var isPrinterOnline = false

    private val dashBoardCategoryViewModel by viewModels<DashBoardCategoryViewModel>()

    @set:Inject
    internal var prefProvider: PrefProvider? = null

    @set:Inject
    var hostSelectionInterceptor: HostSelectionInterceptor? = null

    @Inject
    lateinit var rolePermission: RolePermission
    var currentIndex: Int = 0


    companion object{
        var updatePrinter: UpdatePrinters? = null
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
                    context,
                    message
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

    private var syncReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {

            try {
                if (prefProvider?.getValue(Constants.AUTH_TOKEN,"")?.isNotEmpty() == true) {
                    dashBoardCategoryViewModel.syncInventoryModule(true)
                }
            }catch (e:Exception){
                Log.d("syncReceiver","dashBoardCategoryViewModel create exception")

            }
        }

    }

    private var masterTerminal = object : BroadcastReceiver() {
        @SuppressLint("RestrictedApi")
        override fun onReceive(p0: Context?, p1: Intent?) {

            Log.e(
                "checkMAsterTeminal",
                "check  ${prefProvider?.getValueboolean(IS_MASTER_TERMINAL, false)}"
            )
            if (prefProvider?.getValueboolean(IS_MASTER_TERMINAL, false) == true && prefProvider?.getValueboolean(Constants.IS_PRINTER_QUEUE_STARTS,false) == false && prefProvider?.getValueboolean(
                    IS_PRINTER_QUEUE_ENABLE,false) == true) {
                prefProvider?.setValueboolean(Constants.IS_PRINTER_QUEUE_STARTS,true)
                prefProvider?.setValueboolean(Constants.CHECK_QUEUE_CANCEL,false)
                getKitOne()

            } else if (prefProvider?.getValueboolean(IS_MASTER_TERMINAL, false) == false){
                WorkManager.getInstance(this@MainActivity).cancelAllWork()
                val data = Data.Builder()
                    //.putString("kitchenPrinterList", Gson().toJson(kitchenPrinterList))
                    // .put("kitchenSettingData", Gson().toJson(kitchenSettingModel))
                    .put("location_id", prefProvider?.getValueInt(LOCATION_ID, 0))
                    .put("base_url", prefProvider?.getValue(Constants.BASE_URL_NEW, ""))
                    .put(IS_PRINTER_QUEUE_ENABLE,prefProvider?.getValueboolean(IS_PRINTER_QUEUE_ENABLE,false))
                    .put("is_cancel_work",true)
                    .build()
                prefProvider?.setValueboolean(Constants.CHECK_QUEUE_CANCEL,true)
                val uploadWorkRequest =
                    OneTimeWorkRequest.Builder(
                        UploadWorker2::class.java
                    ).addTag(Constants.PRINTER_QUEUE_BACKGROUND)
                        .setInputData(data)
                        .build()


                val workManager = WorkManager.getInstance(this@MainActivity)

                try {

                    workManager.enqueueUniqueWork(
                        Constants.PRINTER_QUEUE_BACKGROUND, ExistingWorkPolicy.KEEP,
                        uploadWorkRequest
                    )

                } catch (e: java.lang.Exception) {
                    LogUtil.logE(TAG, "printerQueueLog  ${e.message.toString()}")
                    e.printStackTrace()
                }
            }
        }

    }

    private var syncFloorPlan = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            Log.e("SyncFloorPlan", "onReceiveSync")
            if (findNavController(R.id.navHostFrag).currentDestination?.id == R.id.dineInFragment){

                navController?.popBackStack(R.id.dineInFragment,true)
                navController?.navigate(R.id.dineInFragment)

            }
        }

    }

    private var syncSettingReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {

            LogUtil.logEN("onReceive", "" + p1?.action)
            if (prefProvider?.getValue(Constants.AUTH_TOKEN,"")?.isNotEmpty() == true) {
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
                val type = object :
                    TypeToken<List<PrinterQueueModel?>?>() {}.type
                arrayItems =
                    gson.fromJson<Any>(
                        serializedObject,
                        type
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
                        it[it.size - 1],
                        it.size - 1,
                        it.toCollection(arrayListOf())
                    )

                }
                /* prefProvider?.setValue(Constants.PRINTER_QUEUE_DATA, "")
                 prefProvider?.setValue(Constants.PRINTER_QUEUE_DATA, Gson().toJson(it))*/


            } else {
                LogUtil.logE(TAG, "getPrinterNull")
            }
        }
    }


    private fun newKitchenPrinterInit(
        printerQueueModel: PrinterQueueModel,
        index: Int,
        arrayItems: ArrayList<PrinterQueueModel>
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
                    TAG,
                    "printerSuccessData  ${Gson().toJson(printerQueueModel.printSuccessData)}"
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
                        lifecycleScope.executeAsyncTask(
                            onPostExecute = {
                                if (mPrinter != null) {
                                    LogUtil.logE(
                                        TAG,
                                        "statusInfo  ${Gson().toJson(mPrinter?.status)}"
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
                                        Builder.FALSE,
                                        Builder.FALSE,
                                        Builder.FALSE,
                                        Builder.COLOR_1
                                    )

                                    mPrinter?.addText(
                                        padLine(
                                            "OrderID:" + printerQueueModel.orderID,
                                            "",
                                            48
                                        )
                                    )

                                    mPrinter?.addFeedUnit(30)
                                    mPrinter?.addTextFont(Builder.FONT_E)
                                    //  builder.addTextAlign(Builder.ALIGN_LEFT)
                                    mPrinter?.addTextLang(Builder.LANG_EN)
                                    mPrinter?.addTextSize(1, 1)
                                    mPrinter?.addTextStyle(
                                        Builder.FALSE,
                                        Builder.FALSE,
                                        Builder.FALSE,
                                        Builder.COLOR_1
                                    )


                                    mPrinter?.addText(
                                        padLine(
                                            "ReceiptID:" + printerQueueModel.offlineId,
                                            "",
                                            48
                                        )
                                    )

                                    mPrinter?.addFeedUnit(30)
                                    mPrinter?.addTextFont(Builder.FONT_E)
                                    mPrinter?.addTextAlign(Builder.ALIGN_LEFT)
                                    mPrinter?.addTextLang(Builder.LANG_EN)
                                    mPrinter?.addTextSize(fontSizeH, fontSizeW)
                                    mPrinter?.addTextStyle(
                                        Builder.FALSE,
                                        Builder.FALSE,
                                        Builder.FALSE,
                                        Builder.COLOR_1
                                    )

                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        val current = LocalDateTime.now()
                                        val formatter =
                                            DateTimeFormatter.ofPattern("MMM-dd-yyyy hh:mm:a")
                                        val formatted = current.format(formatter)
                                        mPrinter?.addText(
                                            "Print Time:" + Constants.getCurrentTimeFromTimeZone(
                                                this,
                                                formatted
                                            )
                                        )
                                    }
                                    mPrinter?.addFeedLine(1)
                                    mPrinter?.let {
                                        addHorizontalLineNew(it)
                                    }

                                    printerQueueModel.orderItems.let {
                                        addOrdersForKitchenCustomerNewPrinter(
                                            mPrinter!!,
                                            it,
                                            fontSizeH,
                                            fontSizeW
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

                            },
                            doInBackground = {


                                LogUtil.logE(
                                    TAG,
                                    "getIpAddress  ${kitchenPrinterList[i].ipAddress}"
                                )
                                LogUtil.logE(
                                    TAG,
                                    "connectionPrinter   ${mPrinter?.status?.connection}"
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

                            },
                            onPreExecute = {
                                isPrinterQueueRun = true
                            }
                        )


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
            Log.e(TAG,"CheckHere DAta:")
            val data = Data.Builder()
                //.putString("kitchenPrinterList", Gson().toJson(kitchenPrinterList))
                // .put("kitchenSettingData", Gson().toJson(kitchenSettingModel))
                .put("location_id", prefProvider?.getValueInt(LOCATION_ID, 0))
                .put("base_url", prefProvider?.getValue(Constants.BASE_URL_NEW, ""))
                .put(IS_PRINTER_QUEUE_ENABLE,prefProvider?.getValueboolean(IS_PRINTER_QUEUE_ENABLE,false))
                .put("is_cancel_work",false)
                .build()

            prefProvider?.setValueboolean(Constants.CHECK_QUEUE_CANCEL,false)

            val uploadWorkRequest =
                OneTimeWorkRequest.Builder(
                    UploadWorker2::class.java
                ).addTag(Constants.PRINTER_QUEUE_BACKGROUND)
                    .setInputData(data)
                    .build()


            val workManager = WorkManager.getInstance(this)

            try {

                workManager.enqueueUniqueWork(
                    Constants.PRINTER_QUEUE_BACKGROUND, ExistingWorkPolicy.REPLACE,
                    uploadWorkRequest
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
                                Constants.IS_MASTER_TERMINAL,
                                false
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
            Constants.EMPLOYEE_ROLE,
            ""
        )
        prefProvider?.setValueInt(
            Constants.EMPLOYEE_ROLE_ID,
            0
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
        if (prefProvider?.getValueboolean(IS_MASTER_TERMINAL, false) == true && prefProvider?.getValueboolean(
                Constants.IS_PRINTER_QUEUE_ENABLE,false) == true
        ){

            disconnectSocket()

        }
        updatePrinter = null
        unregisterReceiver(broadcastReceiver)
        unregisterReceiver(broadcastReceiveronlineOrder)
    }

    private lateinit var presentation: CustomDisplay

    private fun initCustomerDisplay() {
        getCustomerDisplay(this)?.let { display ->
            presentation = CustomDisplay(
                display,
                this,
                this,
                dashboardViewModel,
                passcodeViewModel,
                dineInViewModel
            )
        }
    }

    override fun onStop() {
        super.onStop()
        Log.e(TAG,"checkActivityStop:")




    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updatePrinter = this
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
            broadCastReceiverPrinterQueueSuccess,
            IntentFilter(Constants.PRITNER_QUEUE_DATA_DELETE)
        )
        registerReceiver(broadcastReceiver, IntentFilter(Constants.SEND_CLOCKOUT_NOTIFICATION))
        registerReceiver(
            broadcastReceiveronlineOrder,
            IntentFilter(Constants.ONLINE_ORDER_GET_NOTIFICATION)
        )
        registerReceiver(
            broadCastReceiverPrinterQueueDataGet,
            IntentFilter(Constants.PRINTER_QUEUE_DATA_RECEIVED)
        )

        registerReceiver(
            syncReceiver,
            IntentFilter(Constants.SYNC_NOTIFICATION)
        )
        registerReceiver(
            syncFloorPlan,
            IntentFilter(Constants.SYNC_FLOORPLAN)
        )

        registerReceiver(
            masterTerminal,
            IntentFilter(Constants.MASTER_TEMINAL_CHANGED)
        )

        registerReceiver(
            syncSettingReceiver,
            IntentFilter(Constants.SYNC_SETTING_NOTIFICATION)
        )

        registerReceiver(
            syncMarkupReceiver,
            IntentFilter(Constants.SYNC_MARKUP)
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

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFrag) as NavHostFragment
        navController = navHostFragment.navController

//        navController = findNavController(R.id.navHostFrag) as NavHostFragment

        listner = NavController.OnDestinationChangedListener { controller, destination, arguments ->

            if (destination.id == R.id.dashboard || destination.id == R.id.dashboardCategory || destination.id == R.id.teamList ||
                destination.id == R.id.settings || destination.id == R.id.inventory || destination.id == R.id.reports || destination.id == R.id.customer
            ) {
                //drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            } else {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

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
                    printerAdd,
                    Printer.PARAM_DEFAULT
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
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
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
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
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
            contentResolver,
            Settings.Secure.ANDROID_ID
        )
    }

    fun alertLogout() {
        alert("", "Are you sure you want to Logout?") {
            this.positiveButton("Logout") {
                viewModel.logoutAPI()
            }
            this.negativeButton("Cancel") {
            }

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
        updatePrinter = this
        if (this::presentation.isInitialized) {
            presentation.show()
            presentation.onDisplayChanged()
        }
        prefProvider?.setValue(UNIQUE_ID, getDeviceId())

        navController?.addOnDestinationChangedListener(listner)
    }

    override fun onPause() {
        super.onPause()

       // disconnectSocket()


        if (this::presentation.isInitialized) {
            presentation.hide()
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

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
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
                this,
                "$APPLICATION_ID.provider",
                photo
            )
            val pictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            pictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
                pictureIntent.clipData = ClipData.newRawUri("", cameraUri)
                pictureIntent.addFlags(
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            if (pictureIntent.resolveActivity(this.packageManager) != null) {
                this.startActivityForResult(pictureIntent, Constants.REQUEST_GET_IMAGE_CAMERA)
            } else {
                Toast.makeText(
                    this,
                    R.string.error_camera_app_not_found,
                    Toast.LENGTH_SHORT
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
                    mediaType = Constants.MEDIA_TYPE_IMAGE,
                    mediaPath = selectedFilePath
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
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED)
        ) {
            true
        } else {
            ActivityCompat.requestPermissions(
                this,
                permissionsLocation,
                Constants.REQUEST_LOCATION_PERMISSION
            )
            false
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        LogUtil.logE("!_@_", "$requestCode")
        when (requestCode) {
            Constants.REQUEST_LOCATION_PERMISSION ->
                if (permissions.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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
                var listIds: ArrayList<Int> =
                    arrayListOf()
                listIds.add(kitchenPrinterList[currentIndex].id)

                LogUtil.logE(TAG, "listIds  ${Gson().toJson(listIds)}")
                LogUtil.logE(TAG, "printerQueueId  ${printerQueueModelGlobal?.id ?: 0}")
                ThreadPoolManager.instance.executeTask(Runnable {
                    lifecycleScope.launch {
                        printerQueueModelGlobal?.id?.let {
                            viewModelPrinter.updateStatusPrinterQueue(
                                listIds,
                                it
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



