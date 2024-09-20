package com.pays.pos.ui.activities

import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Point
import android.os.*
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pays.pos.MainApplication
import com.pays.pos.R
import com.pays.pos.di.BarcodePrefProvider
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.executeAsyncTask
import com.pays.pos.utils.scanner.barcode.GenerateBarcode128B
import com.pays.pos.utils.scanner.helpers.*
import com.google.gson.Gson
import com.zebra.scannercontrol.*
import com.zebra.scannercontrol.DCSSDKDefs.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/*ALL THE SCAN GUN VARIABLES ARE COMMENTED from MAINAPPLICATION AND MOVED TO MAINACTIVITY(for solving permission issue), PLEASE UNCOMMENT IT AND REMOVE THE VARIABLES FROM MAINACTIVITY*/

@AndroidEntryPoint
open class BaseScannerActivity : AppCompatActivity(), ScannerAppEngine, IDcsSdkApiDelegate,
    ScannerAppEngine.IScannerAppEngineDevConnectionsDelegate {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (checkBluetoothAvailable()) {
            initializeScanner()
        } else {

            Toast.makeText(this, "Bluetooth is not supported !!", Toast.LENGTH_SHORT).show()
        }
    }

    /*Scanner Implementation*/

    private var selectedProtocol: DCSSDK_BT_PROTOCOL? = null
    private var selectedConfig: DCSSDK_BT_SCANNER_CONFIG? = null
    private var mDevConnDelegates: ArrayList<ScannerAppEngine.IScannerAppEngineDevConnectionsDelegate>? =
        ArrayList<ScannerAppEngine.IScannerAppEngineDevConnectionsDelegate>()
    private var mDevEventsDelegates: ArrayList<ScannerAppEngine.IScannerAppEngineDevEventsDelegate>? =
        ArrayList<ScannerAppEngine.IScannerAppEngineDevEventsDelegate>()

    protected var mScannerInfoList: ArrayList<DCSScannerInfo> = arrayListOf()
    protected var mOfflineScannerInfoList: ArrayList<DCSScannerInfo> = arrayListOf()

    private var SCANNER_ID_NONE = -1
    private var curAvailableScanner: AvailableScanner? = null

    private var vibrator: ManagedVibrator? = null

    val scannersList: ArrayList<AvailableScanner> = arrayListOf()
    val lastConnectedScannerList: ArrayList<AvailableScanner> = arrayListOf()

    var notifyAdaptersCallBack: ((Boolean) -> Unit)? = null
    var barcodeEventCallBack: ((String) -> Unit)? = null

    private val EVENT = 2
    private val TAG = "BaseScannerActivity"

    private val mSNAPIList = java.util.ArrayList<DCSScannerInfo>()


    @Inject
    lateinit var barcodePrefProvider: BarcodePrefProvider

    fun initializeScanner() {
//        mScannerInfoList = MainActivity.mScannerInfoList
        mOfflineScannerInfoList = ArrayList<DCSScannerInfo>()

//        if (MainActivity.sdkHandler == null) {
//            MainActivity.sdkHandler = SDKHandler(this, true)
//        }

        //set mac address and protocols
        selectedProtocol = DCSSDK_BT_PROTOCOL.CRD_BT_LE
        selectedConfig = DCSSDK_BT_SCANNER_CONFIG.SET_FACTORY_DEFAULTS
//        MainActivity.sdkHandler?.dcssdkSetBTAddress(getMacAddress())

        //set the delegates method
//        MainActivity.sdkHandler?.dcssdkSetDelegate(this)

        initializeDcsSdk()

        // enable scanner detection
//        MainActivity.sdkHandler?.dcssdkEnableAvailableScannersDetection(true)

        //Synchronous Scanner Retrieval
//        MainActivity.sdkHandler?.dcssdkGetAvailableScannersList(mScannerInfoList)
//        MainActivity.sdkHandler?.dcssdkGetActiveScannersList(mScannerInfoList)

        // ...
        initializeDcsSdkWithAppSettings()


    }


    fun resetConnectionThroughBarcode(flBarcode: FrameLayout?) {
        if (!checkBluetoothAvailable()) {
            return
        }
        val layoutParams = LinearLayout.LayoutParams(-1, -1)
        val data2encode = (3.toChar()) + "92"
        val barcode =
            GenerateBarcode128B(data2encode)
        val barCodeView =
            com.pays.pos.utils.scanner.barcode.BarCodeView(this, barcode)
        barCodeView.setBackgroundColor(resources.getColor(R.color.txtColor))
        val display: Display? = windowManager?.defaultDisplay
        val size = Point()
        display?.getSize(size)
        val width = size.x
        val height = size.y
        val x = width * 3 / 10
        val y = x / 4
        barCodeView.setSize(x, y)
        flBarcode?.addView(barCodeView, layoutParams)
    }

    fun generatePairingBarcode(flBarcode: FrameLayout?) {
        if (!checkBluetoothAvailable()) {
            return
        }
        val layoutParams = LinearLayout.LayoutParams(-1, -1)
        // SDK was not able to determine Bluetooth MAC. So call the dcssdkGetPairingBarcode with BT Address.
//        MainActivity.sdkHandler?.dcssdkSetSTCEnabledState(true)
        MainActivity.btAddress = getMacAddress()
        if (MainActivity.btAddress?.isNotEmpty() == true) {
//            MainActivity.sdkHandler?.dcssdkSetBTAddress(MainActivity.btAddress)
//            val barCodeView = MainActivity.sdkHandler?.dcssdkGetPairingBarcode(
//                selectedProtocol,
//                selectedConfig,
//                MainActivity.btAddress
//            )
//            if (barCodeView != null) {
//                updateBarcodeView(flBarcode, layoutParams, barCodeView)
//            }
        } else {
            flBarcode?.removeAllViews()
        }
    }

    open fun getSnapiBarcode(llBarcode: FrameLayout?) {
        val layoutParams = LinearLayout.LayoutParams(-1, -1)
//        val barCodeView: BarCodeView? =
//            MainActivity.sdkHandler?.dcssdkGetUSBSNAPIWithImagingBarcode()
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        val width = size.x
        val height = size.y
        val orientation = this.resources.configuration.orientation
        var x = width * 9 / 10
        var y = x / 3
        if (getDeviceScreenSize() > 6) { // TODO: Check 6 is ok or not
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                x = width / 2
                y = x / 3
            } else {
                x = width * 2 / 3
                y = x / 3
            }
        }
//        barCodeView?.setSize(x, y)
//        llBarcode?.addView(barCodeView, layoutParams)
    }

    fun checkBluetoothAvailable(): Boolean {
        return packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
    }

    override fun onResume() {
        super.onResume()
//        MainActivity.sdkHandler?.dcssdkSetDelegate(this)
        //Register a dynamic receiver to handle the various RFID Reader Events when the app is in foreground
        //Actions to be handled should be registered here
        val filter: IntentFilter = IntentFilter(Constants.ACTION_SCANNER_CONNECTED)
        filter.addAction(Constants.ACTION_SCANNER_DISCONNECTED)
        filter.addAction(Constants.ACTION_SCANNER_AVAILABLE)
        filter.addAction(Constants.ACTION_SCANNER_CONN_FAILED)

        //Use a positive priority
        filter.priority = 2
        registerReceiver(onNotification, filter)

        mSNAPIList.clear()
        updateScannersList()
        for (device in getActualScannersList()!!) {
            if (device.connectionType == DCSSDK_CONN_TYPES.DCSSDK_CONNTYPE_USB_SNAPI) {
                mSNAPIList.add(device)
            }
        }
        //reconnect the scanner
//        connectToScanner(getScannerPref().getScannerData())
        LogUtil.logE("mSNAPIList", mSNAPIList.size.toString())
        if (mSNAPIList.isNotEmpty() && !mSNAPIList[0].isActive) {
            Handler(Looper.getMainLooper()).postDelayed({
                connectScannerCable(mSNAPIList[0].scannerID)
            }, 2000)

        }
    }

    override fun onPause() {
        super.onPause()
        removeDevConnectiosDelegate(this)
        unregisterReceiver(onNotification)
    }

    protected fun addToAvailableScannerList(availableScanner: AvailableScanner) {
        if (!scannersList.contains(availableScanner)) {
            scannersList.add(availableScanner)
        }
    }

    protected fun addToLastConnectedScannerList(availableScanner: AvailableScanner) {
        lastConnectedScannerList.clear()
        lastConnectedScannerList.add(availableScanner)
    }

    override fun initializeDcsSdkWithAppSettings() {
        // Restore preferences
        val settings: BarcodePrefProvider = getScannerPref()
        vibrator = ManagedVibrator(this)
        MainActivity.MOT_SETTING_OPMODE = settings.getValueInt(
            Constants.PREF_OPMODE,
            DCSSDK_CONN_TYPES.DCSSDK_CONNTYPE_BT_NORMAL.value
        )

        MainActivity.MOT_SETTING_SCANNER_DETECTION =
            settings.getValueBoolean(Constants.PREF_SCANNER_DETECTION, true)
        MainActivity.MOT_SETTING_EVENT_IMAGE =
            settings.getValueBoolean(Constants.PREF_EVENT_IMAGE, true)
        MainActivity.MOT_SETTING_EVENT_VIDEO =
            settings.getValueBoolean(Constants.PREF_EVENT_VIDEO, true)
        MainActivity.MOT_SETTING_EVENT_BINARY_DATA =
            settings.getValueBoolean(Constants.PREF_EVENT_BINARY_DATA, true)

        MainActivity.MOT_SETTING_EVENT_ACTIVE =
            settings.getValueBoolean(Constants.PREF_EVENT_ACTIVE, true)
        MainActivity.MOT_SETTING_EVENT_AVAILABLE =
            settings.getValueBoolean(Constants.PREF_EVENT_AVAILABLE, true)
        MainActivity.MOT_SETTING_EVENT_BARCODE =
            settings.getValueBoolean(Constants.PREF_EVENT_BARCODE, true)

        /*MainActivity.MOT_SETTING_NOTIFICATION_AVAILABLE = true
            //settings.getValueBoolean(Constants.PREF_NOTIFY_AVAILABLE, false)
        MainActivity.MOT_SETTING_NOTIFICATION_ACTIVE = true
           // settings.getValueBoolean(Constants.PREF_NOTIFY_ACTIVE, false)
        MainActivity.MOT_SETTING_NOTIFICATION_BARCODE = true
            //settings.getValueBoolean(Constants.PREF_NOTIFY_BARCODE, false)*/

        MainActivity.MOT_SETTING_NOTIFICATION_IMAGE =
            settings.getValueBoolean(Constants.PREF_NOTIFY_IMAGE, false)
        MainActivity.MOT_SETTING_NOTIFICATION_VIDEO =
            settings.getValueBoolean(Constants.PREF_NOTIFY_VIDEO, false)
        MainActivity.MOT_SETTING_NOTIFICATION_BINARY_DATA =
            settings.getValueBoolean(Constants.PREF_NOTIFY_BINARY_DATA, false)


        var notifications_mask = 0
        if (MainActivity.MOT_SETTING_EVENT_AVAILABLE) {
            notifications_mask =
                notifications_mask or (DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_APPEARANCE.value or DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_DISAPPEARANCE.value)
        }
        if (MainActivity.MOT_SETTING_EVENT_ACTIVE) {
            notifications_mask =
                notifications_mask or (DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SESSION_ESTABLISHMENT.value or DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_SESSION_TERMINATION.value)
        }
        if (MainActivity.MOT_SETTING_EVENT_BARCODE) {
            notifications_mask =
                notifications_mask or DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_BARCODE.value
        }
        if (MainActivity.MOT_SETTING_EVENT_IMAGE) {
            notifications_mask =
                notifications_mask or DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_IMAGE.value
        }
        if (MainActivity.MOT_SETTING_EVENT_VIDEO) {
            notifications_mask =
                notifications_mask or DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_VIDEO.value
        }
        if (MainActivity.MOT_SETTING_EVENT_BINARY_DATA) {
            notifications_mask =
                notifications_mask or DCSSDKDefs.DCSSDK_EVENT.DCSSDK_EVENT_BINARY_DATA.value
        }
//        MainActivity.sdkHandler?.dcssdkSubsribeForEvents(notifications_mask)
    }

    override fun showMessageBox(message: String?) {
    }

    override fun showBackgroundNotification(text: String?): Int {
        return 0
    }

    override fun dismissBackgroundNotifications(): Int {
        return 0
    }

    override fun isInBackgroundMode(context: Context?): Boolean {
        return Foreground.get()?.isBackground == true
    }

    override fun addDevListDelegate(delegate: ScannerAppEngine.IScannerAppEngineDevListDelegate?) {
        if (MainActivity.mDevListDelegates == null) {
            MainActivity.mDevListDelegates =
                ArrayList<ScannerAppEngine.IScannerAppEngineDevListDelegate>()
        }
        delegate?.let {
            MainActivity.mDevListDelegates?.add(it)
        }
    }

    override fun addDevConnectionsDelegate(delegate: ScannerAppEngine.IScannerAppEngineDevConnectionsDelegate?) {
        if (mDevConnDelegates == null) {
            mDevConnDelegates =
                ArrayList<ScannerAppEngine.IScannerAppEngineDevConnectionsDelegate>()
        }
        delegate?.let {
            mDevConnDelegates?.add(it)
        }
    }

    override fun addDevEventsDelegate(delegate: ScannerAppEngine.IScannerAppEngineDevEventsDelegate?) {
        if (mDevEventsDelegates == null) {
            mDevEventsDelegates = ArrayList<ScannerAppEngine.IScannerAppEngineDevEventsDelegate>()
        }
        delegate?.let {
            mDevEventsDelegates?.add(it)
        }
        LogUtil.logE(TAG, "mDevEventsDelegates.size ADD : ${mDevEventsDelegates?.size ?: 0}")
    }

    override fun removeDevListDelegate(delegate: ScannerAppEngine.IScannerAppEngineDevListDelegate?) {
        if (MainActivity.mDevListDelegates != null) {
            MainActivity.mDevListDelegates?.remove(delegate)
        }
    }

    override fun removeDevConnectiosDelegate(delegate: ScannerAppEngine.IScannerAppEngineDevConnectionsDelegate?) {
        if (mDevConnDelegates != null) {
            mDevConnDelegates?.remove(delegate)
        }
        LogUtil.logE(TAG, "mDevEventsDelegates.size REMOVE : ${mDevEventsDelegates?.size ?: 0}")
    }

    override fun removeDevEventsDelegate(delegate: ScannerAppEngine.IScannerAppEngineDevEventsDelegate?) {
        if (mDevEventsDelegates != null) {
            mDevEventsDelegates?.remove(delegate)
        }
    }

    override fun getActualScannersList(): MutableList<DCSScannerInfo>? {
        return mScannerInfoList
    }

    override fun getScannerInfoByIdx(dev_index: Int): DCSScannerInfo? {
        return mScannerInfoList[dev_index]
    }

    override fun getScannerByID(scannerId: Int): DCSScannerInfo? {
        for (scannerInfo in mScannerInfoList) {
            if (scannerInfo.scannerID == scannerId) {
                return scannerInfo
            }
        }
        return null
    }

    override fun raiseDeviceNotificationsIfNeeded() {
    }

    override fun updateScannersList() {
       /* if (MainActivity.sdkHandler != null) {
            mScannerInfoList.clear()
            val scannerTreeList = ArrayList<DCSScannerInfo>()
            MainActivity.sdkHandler?.dcssdkGetAvailableScannersList(scannerTreeList)
            MainActivity.sdkHandler?.dcssdkGetActiveScannersList(scannerTreeList)
            createFlatScannerList(scannerTreeList)
        }*/
    }

    private fun createFlatScannerList(scannerTreeList: ArrayList<DCSScannerInfo>) {
        for (s in scannerTreeList) {
            addToScannerList(s)
        }
    }

    private fun addToScannerList(s: DCSScannerInfo) {
        mScannerInfoList.add(s)
        if (s.auxiliaryScanners != null) {
            for (aux in s.auxiliaryScanners.values) {
                addToScannerList(aux)
            }
        }
    }

    override fun connect(scannerId: Int): DCSSDK_RESULT {
        /*return if (MainActivity.sdkHandler != null) {
            if (curAvailableScanner != null) {
                resetVirtualTetherHostConfigurations()
                curAvailableScanner?.let {
                    MainActivity.sdkHandler?.dcssdkTerminateCommunicationSession(it.scannerId)
                }
            }
            MainActivity.sdkHandler?.dcssdkEstablishCommunicationSession(scannerId)
                ?: DCSSDK_RESULT.DCSSDK_RESULT_FAILURE
        } else {
            DCSSDK_RESULT.DCSSDK_RESULT_FAILURE
        }*/
        return DCSSDK_RESULT.DCSSDK_RESULT_FAILURE
    }

    override fun disconnect(scannerId: Int) {
        /*if (MainActivity.sdkHandler != null) {
            val ret: DCSSDK_RESULT? =
                MainActivity.sdkHandler?.dcssdkTerminateCommunicationSession(scannerId)
            curAvailableScanner = null
            MainActivity.intentionallyDisconnected = true
            updateScannersList()
        }*/
    }

    override fun setAutoReconnectOption(scannerId: Int, enable: Boolean): DCSSDK_RESULT {
        val ret: DCSSDK_RESULT
        /*MainActivity.sdkHandler?.let {
            ret = it.dcssdkEnableAutomaticSessionReestablishment(
                enable,
                scannerId
            )
            return ret
        }*/
        return DCSSDK_RESULT.DCSSDK_RESULT_FAILURE
    }

    override fun enableScannersDetection(enable: Boolean) {
//        MainActivity.sdkHandler?.dcssdkEnableAvailableScannersDetection(enable)
    }

    override fun enableBluetoothScannerDiscovery(enable: Boolean) {
//        MainActivity.sdkHandler?.dcssdkEnableBluetoothScannersDiscovery(enable)
    }

    override fun configureNotificationAvailable(enable: Boolean) {
//        if (enable) {
//            MainActivity.sdkHandler?.dcssdkSubsribeForEvents(
//                DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_APPEARANCE.value or
//                        DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_DISAPPEARANCE.value
//            )
//        } else {
//            MainActivity.sdkHandler?.dcssdkUnsubsribeForEvents(
//                DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_APPEARANCE.value or
//                        DCSSDK_EVENT.DCSSDK_EVENT_SCANNER_DISAPPEARANCE.value
//            )
//        }
    }

    override fun configureNotificationActive(enable: Boolean) {
        /*if (enable) {
            MainActivity.sdkHandler?.dcssdkSubsribeForEvents(
                DCSSDK_EVENT.DCSSDK_EVENT_SESSION_ESTABLISHMENT.value or
                        DCSSDK_EVENT.DCSSDK_EVENT_SESSION_TERMINATION.value
            )
        } else {
            MainActivity.sdkHandler?.dcssdkUnsubsribeForEvents(
                DCSSDK_EVENT.DCSSDK_EVENT_SESSION_ESTABLISHMENT.value or
                        DCSSDK_EVENT.DCSSDK_EVENT_SESSION_TERMINATION.value
            )
        }*/
    }

    override fun configureNotificationBarcode(enable: Boolean) {
       /* if (enable) {
            MainActivity.sdkHandler?.dcssdkSubsribeForEvents(DCSSDK_EVENT.DCSSDK_EVENT_BARCODE.value)
        } else {
            MainActivity.sdkHandler?.dcssdkUnsubsribeForEvents(DCSSDK_EVENT.DCSSDK_EVENT_BARCODE.value)
        }*/
    }

    override fun configureNotificationImage(enable: Boolean) {
        /*if (enable) {
            MainActivity.sdkHandler?.dcssdkSubsribeForEvents(DCSSDK_EVENT.DCSSDK_EVENT_IMAGE.value)
        } else {
            MainActivity.sdkHandler?.dcssdkUnsubsribeForEvents(DCSSDK_EVENT.DCSSDK_EVENT_IMAGE.value)
        }*/
    }

    override fun configureNotificationVideo(enable: Boolean) {
       /* if (enable) {
            MainActivity.sdkHandler?.dcssdkSubsribeForEvents(DCSSDK_EVENT.DCSSDK_EVENT_VIDEO.value)
        } else {
            MainActivity.sdkHandler?.dcssdkUnsubsribeForEvents(DCSSDK_EVENT.DCSSDK_EVENT_VIDEO.value)
        }*/
    }

    override fun configureOperationalMode(mode: DCSSDKDefs.DCSSDK_MODE?) {
        LogUtil.logE(TAG, "")
        initializeDcsSdk()
        //MainActivity.sdkHandler?.dcssdkSetOperationalMode(DCSSDK_MODE.DCSSDK_OPMODE_BT_LE)
    }

    override fun executeCommand(
        opCode: DCSSDKDefs.DCSSDK_COMMAND_OPCODE?,
        inXML: String?,
        outXML: StringBuilder?,
        scannerID: Int
    ): Boolean {
        var outXmlNew = outXML
        /*MainActivity.sdkHandler?.let {
            if (outXML == null) {
                outXmlNew = java.lang.StringBuilder()
            }
            val result: DCSSDK_RESULT? =
                MainActivity.sdkHandler?.dcssdkExecuteCommandOpCodeInXMLForScanner(
                    opCode,
                    inXML,
                    outXmlNew,
                    scannerID
                )
            if (result == DCSSDK_RESULT.DCSSDK_RESULT_SUCCESS) return true else if (result == DCSSDK_RESULT.DCSSDK_RESULT_FAILURE) return false
        }*/
        return false
    }

    override fun executeSSICommand(
        opCode: DCSSDKDefs.DCSSDK_COMMAND_OPCODE?,
        inXML: String?,
        outXML: StringBuilder?,
        scannerID: Int
    ): Boolean {
        var outXmlNew = outXML
        /*if (MainActivity.sdkHandler != null) {
            if (outXML == null) {
                outXmlNew = java.lang.StringBuilder()
            }
            val result: DCSSDK_RESULT? =
                MainActivity.sdkHandler?.dcssdkExecuteSSICommandOpCodeInXMLForScanner(
                    opCode,
                    inXML,
                    outXmlNew,
                    scannerID
                )
            if (result == DCSSDK_RESULT.DCSSDK_RESULT_SUCCESS) return true else if (result == DCSSDK_RESULT.DCSSDK_RESULT_FAILURE) return false
        }*/
        return false
    }

    override fun scannersListHasBeenUpdated(): Boolean {
        updateScannerListView()
        return true
    }

    private fun connectScanner(scanner: AvailableScanner) {
        var progressDialog: CustomProgressDialog? = null
        lifecycleScope.executeAsyncTask(onPreExecute = {
            progressDialog =
                CustomProgressDialog(this, "Connecting To scanner. Please Wait...")
            progressDialog?.setCancelable(false)
            progressDialog?.show()
            progressDialog?.setOnCancelListener(DialogInterface.OnCancelListener {
                //scanner dialog cancel
            })
        }, doInBackground = {
            scanner.scannerId.let {
                var result: DCSSDK_RESULT? = null
                runOnUiThread(Runnable {
                    result = connect(it)
                })

                if (result == DCSSDK_RESULT.DCSSDK_RESULT_SUCCESS) {
                    curAvailableScanner = scanner
                    curAvailableScanner?.isConnected = true
                    return@executeAsyncTask true
                }
            }
            curAvailableScanner = null
            return@executeAsyncTask false

        }, onPostExecute = {
            progressDialog?.let { progressDialog ->
                if (progressDialog.isShowing) {
                    progressDialog.dismiss()
                }
            }
            if (!it) {
                Toast.makeText(
                    MainActivity.getInstance(),
                    "Unable to communicate with scanner",
                    Toast.LENGTH_SHORT
                ).show()
                scannersListHasBeenUpdated()
            }
        })
    }


    private fun connectScannerCable(scanner: Int) {
        var progressDialog: CustomProgressDialog? = null
        lifecycleScope.executeAsyncTask(onPreExecute = {
            progressDialog =
                CustomProgressDialog(this, "Connecting To scanner. Please Wait...")
            progressDialog?.setCancelable(false)
            progressDialog?.show()
            progressDialog?.setOnCancelListener(DialogInterface.OnCancelListener {
                //scanner dialog cancel
            })
        }, doInBackground = {
            scanner.let {
                var result: DCSSDK_RESULT? = null
                runOnUiThread(Runnable {
                    result = connect(it)
                })

                if (result == DCSSDK_RESULT.DCSSDK_RESULT_SUCCESS) {
//                    curAvailableScanner = scanner
                    curAvailableScanner?.isConnected = true
                    return@executeAsyncTask true
                }
            }
            curAvailableScanner = null
            return@executeAsyncTask false

        }, onPostExecute = {
            progressDialog?.let { progressDialog ->
                if (progressDialog.isShowing) {
                    progressDialog.dismiss()
                }
            }
            if (!it) {
                Toast.makeText(
                    MainActivity.getInstance(),
                    "Unable to communicate with scanner",
                    Toast.LENGTH_SHORT
                ).show()
                scannersListHasBeenUpdated()
            }
        })
    }

    private val updateScannersHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            updateScannersList()
            scannersList.clear()
            lastConnectedScannerList.clear()
            var enableLastScannerConnection = false
            /*MainActivity.lastConnectedScanner?.let {
                if (it.isActive) {
                    val device: DCSScannerInfo = it
                    addToLastConnectedScannerList(
                        AvailableScanner(
                            device.scannerID,
                            device.scannerName,
                            device.scannerHWSerialNumber,
                            false,
                            device.isAutoCommunicationSessionReestablishment,
                            device.connectionType
                        )
                    )
                }
            }*/
            getActualScannersList()?.let {
                for (device in it) {
                    if (device.isActive) {
                        LogUtil.logE(TAG, " Last connected scanner : ACTIVE: ${device.scannerName}")
                        val availableScanner = AvailableScanner(
                            device.scannerID,
                            device.scannerName,
                            device.scannerHWSerialNumber,
                            true,
                            device.isAutoCommunicationSessionReestablishment,
                            device.connectionType
                        )

                        //save connected scanner data
                        saveScanner(availableScanner, true)

                        availableScanner.isConnectable = true
                        addToLastConnectedScannerList(availableScanner)
                        enableLastScannerConnection = true
                    } else {
                        LogUtil.logE(TAG, " Available scanner : NOT ACTIVE: ${device.scannerName}")
                        val availableScanner = AvailableScanner(
                            device.scannerID,
                            device.scannerName,
                            device.scannerHWSerialNumber,
                            false,
                            device.isAutoCommunicationSessionReestablishment,
                            device.connectionType
                        )
                        availableScanner.isConnectable = true
                        addToAvailableScannerList(availableScanner)
                    }
                }
            }

            //-- CDC readers --
            scannersList.sort()

            LogUtil.logE(TAG, "lastConnectedScannerList size : ${lastConnectedScannerList.size}")
            LogUtil.logE(TAG, "scannersList size : ${scannersList.size}")

            notifyAdapters(enableLastScannerConnection)

            // val ll = findViewById(R.id.noScannersMessage) as View
            /*val tblROwLastScannerConnected =
                findViewById(R.id.tbl_row_last_connected_scanner) as TableRow*/


            /*if (lastConnectedScannerListAdapter.getCount() === 0) {
                tblROwLastScannerConnected.visibility = View.GONE
            } else {
                tblROwLastScannerConnected.visibility = View.VISIBLE
            }
            val lastConnectedScannerTxt = findViewById(R.id.txt_last_connected_scanner) as TextView?
            if (lastConnectedScannerTxt != null) {
                lastConnectedScannerTxt.text = "Last Connected Scanner"
            }*/
            if (lastConnectedScannerList.size > 0) {
                if (lastConnectedScannerList[0].isConnected) {
                    /*if (msg.what == EVENT) {
                        if (asyncTaskRunning == null || AsyncTask.Status.RUNNING != com.zebra.scannercontrol.app.activities.ScannersActivity.cmdExecTask.getStatus()) {
                            currentScannerName = lastConnectedScannerList[0].scannerName
                            currentScannerAddress = lastConnectedScannerList[0].scannerAddress
                            currentScannerId = lastConnectedScannerList[0].scannerId
                            currentAutoReconnectionState = lastConnectedScannerList[0].isAutoReconnection
                            val intent: Intent = Intent(
                                activity,
                                ActiveScannerActivity::class.java
                            )
                            intent.putExtra(
                                Constants.SCANNER_NAME,
                                MainActivity.currentScannerName
                            )
                            intent.putExtra(
                                Constants.SCANNER_ADDRESS,
                                MainActivity.currentScannerAddress
                            )
                            intent.putExtra(Constants.SCANNER_ID, MainActivity.currentScannerId)
                            intent.putExtra(
                                Constants.AUTO_RECONNECTION,
                                MainActivity.currentAutoReconnectionState
                            )
                            intent.putExtra(Constants.CONNECTED, true)
                            intent.putExtra(Constants.SHOW_BARCODE_VIEW, false)
                            startActivity(intent)
                        }
                    }*/
                    /*if (lastConnectedScannerTxt != null) {
                        lastConnectedScannerTxt.text = "Currently Connected Scanner"
                    }*/
                }
            }
        }
    }

    fun updateScannerListView() {
        val msg = Message()
        msg.what = EVENT
        updateScannersHandler.sendMessage(msg)
    }

    //TODO scanner remove

    //Handler to show the data on UI
    protected var dataHandler: Handler = object : Handler(Looper.getMainLooper()) {
        var notificaton_processed = false
        var result = false
        var found = false
        override fun handleMessage(msg: Message) {

            var log = ""
            try {
                log = " ${Gson().toJson(msg)}"
            } catch (e: java.lang.Exception) {

            }
            LogUtil.logE(TAG, "dataHandler : $log")
            when (msg.what) {
                Constants.IMAGE_RECEIVED -> {
                    LogUtil.logE(TAG, "Image Received")
                    val imageData = msg.obj as ByteArray
                    //Barcode barcode=(Barcode)msg.obj;
                    //Application.barcodeData.add(barcode);

                    mDevEventsDelegates?.let {
                        for (delegate in it) {
                            if (delegate != null) {
                                LogUtil.logE(TAG, "Show Image Received")
                                delegate.scannerImageEvent(imageData)
                            }
                        }
                    }
                }
                Constants.VIDEO_RECEIVED -> {
                    LogUtil.logE(TAG, "Video Received")
                    val videoEvent = msg.obj as ByteArray
                    mDevEventsDelegates?.let {
                        for (delegate in it) {
                            if (delegate != null) {
                                LogUtil.logE(TAG, "Show Video Received")
                                delegate.scannerVideoEvent(videoEvent)
                            }
                        }
                    }
                }
                Constants.FW_UPDATE_EVENT -> {
                    LogUtil.logE(TAG, "FW_UPDATE_EVENT")
                    LogUtil.logE(
                        TAG,
                        "FW_UPDATE_EVENT Received. Client count = " + mDevEventsDelegates?.size
                    )
                    val firmwareUpdateEvent = msg.obj as FirmwareUpdateEvent
                    mDevEventsDelegates?.let {
                        for (delegate in it) {
                            if (delegate != null) {
                                LogUtil.logE(TAG, "Show FW_UPDATE_EVENT Received")
                                delegate.scannerFirmwareUpdateEvent(firmwareUpdateEvent)
                            }
                        }
                    }
                }
                Constants.BARCODE_RECEIVED -> {
                    LogUtil.logE(TAG, "Barcode Received")
                    val barcode = msg.obj as Barcode
                    MainActivity.barcodeData.add(barcode)
                    mDevEventsDelegates?.let {
                        for (delegate in it) {
                            if (delegate != null) {
                                LogUtil.logE(TAG, "Show Barcode Received")
                                delegate.scannerBarcodeEvent(
                                    barcode.barcodeData,
                                    barcode.barcodeType,
                                    barcode.fromScannerID
                                )
                            }
                        }
                    }
                    if (MainActivity.MOT_SETTING_NOTIFICATION_BARCODE && !notificaton_processed) {
                        var scannerName = ""
                        if (mScannerInfoList != null) {
                            for (ex_info in mScannerInfoList) {
                                if (ex_info.scannerID == barcode.fromScannerID) {
                                    scannerName = ex_info.scannerName
                                    break
                                }
                            }
                        }
                        if (isInBackgroundMode(MainActivity.getInstance())) {
                            val intent = Intent()
                            intent.action = Constants.ACTION_SCANNER_BARCODE_RECEIVED
                            intent.putExtra(
                                Constants.NOTIFICATIONS_TEXT,
                                "Barcode received from $scannerName"
                            )
                            intent.putExtra(
                                Constants.NOTIFICATIONS_TYPE,
                                Constants.BARCODE_RECEIVED
                            )
                            sendOrderedBroadcast(intent, null)
                        } else {
                            /*Toast.makeText(
                                MainActivity.getInstance(),
                                "Barcode received from $scannerName", Toast.LENGTH_SHORT
                            ).show()*/
                        }
                    }
                }
                Constants.SESSION_ESTABLISHED -> {
                    LogUtil.logE(TAG, "SESSION_ESTABLISHED")
                    val activeScanner = msg.obj as DCSScannerInfo
                    notificaton_processed = false
                    result = false
                    curAvailableScanner = AvailableScanner(activeScanner)
                    curAvailableScanner?.isConnected = true
                    setAutoReconnectOption(activeScanner.scannerID, true)
                    /* notify connections delegates */
                    mDevConnDelegates?.let {
                        for (delegate in it) {
                            if (delegate != null) {
                                result = delegate.scannerHasConnected(activeScanner.scannerID)
                                if (result) {
                                    /*
                                 DevConnections delegates should NOT display any UI alerts,
                                 so from UI notification side the event is not processed
                                 */
                                    notificaton_processed = false
                                }
                            }
                        }
                    }

                    /* update dev list */
                    found = false
                    for (dcsScannerInfo in mScannerInfoList) {
                        if (dcsScannerInfo.scannerID == activeScanner.scannerID) {
                            mScannerInfoList.remove(dcsScannerInfo)
                            MainActivity.barcodeData.clear()
                            found = true
                            break
                        }
                    }
                    for (offInfo in mOfflineScannerInfoList) {
                        if (offInfo.scannerID == activeScanner.scannerID) {
                            mOfflineScannerInfoList.remove(offInfo)
                            break
                        }
                    }
                    //add active scanner in the list
                    mScannerInfoList.add(activeScanner)

                    /* notify dev list delegates */
                    MainActivity.mDevListDelegates?.let {
                        for (delegate in it) {
                            if (delegate != null) {
                                result = delegate.scannersListHasBeenUpdated()
                                if (result) {
                                    /*
                                     DeList delegates should NOT display any UI alerts,
                                     so from UI notification side the event is not processed
                                     */
                                    notificaton_processed = false
                                    resetVirtualTetherHostConfigurations()
                                }
                            }
                        }
                    }

                    // Showing notifications in foreground and background mode
                    if (MainActivity.MOT_SETTING_NOTIFICATION_ACTIVE && !notificaton_processed) {
                        val notificationMsg = java.lang.StringBuilder()
                        if (!found) {
                            notificationMsg.append(activeScanner.scannerName)
                                .append(" has appeared and connected")
                        } else {
                            notificationMsg.append(activeScanner.scannerName)
                                .append(" has connected")
                        }
                        if (isInBackgroundMode(MainActivity.getInstance()?.applicationContext)) {
                            val intent = Intent()
                            intent.action = Constants.ACTION_SCANNER_CONNECTED
                            intent.putExtra(
                                Constants.NOTIFICATIONS_TEXT,
                                notificationMsg.toString()
                            )
                            intent.putExtra(
                                Constants.NOTIFICATIONS_TYPE,
                                Constants.SESSION_ESTABLISHED
                            )
                            sendOrderedBroadcast(intent, null)
                        } else {
                            Toast.makeText(
                                MainActivity.getInstance(),
                                notificationMsg.toString(),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                Constants.SESSION_TERMINATED -> {
                    LogUtil.logE(TAG, "SESSION_TERMINATED")
                    val scannerID = msg.obj as Int
                    var scannerName = ""
                    notificaton_processed = false
                    result = false

                    /* notify connections delegates */
                    mDevConnDelegates?.let {
                        for (delegate in it) {
                            if (delegate != null) {
                                result = delegate.scannerHasDisconnected(scannerID)
                                if (result) {
                                    /*
                                 DevConnections delegates should NOT display any UI alerts,
                                 so from UI notification side the event is not processed
                                 */
                                    notificaton_processed = false
                                }
                            }
                        }
                    }
                    val scannerInfo = getScannerByID(scannerID)
                    scannerInfo?.let {
                        mOfflineScannerInfoList.add(it)
                        scannerName = it.scannerName
                        curAvailableScanner = null
                    }

                    updateScannersList()

                    /* notify dev list delegates */
                    MainActivity.mDevListDelegates?.let {
                        for (delegate in it) {
                            if (delegate != null) {
                                result = delegate.scannersListHasBeenUpdated()
                                if (result) {
                                    /*
                                     DeList delegates should NOT display any UI alerts,
                                     so from UI notification side the event is not processed
                                     */
                                    notificaton_processed = false
                                }
                            }
                        }
                    }

                    val virtualTetherSharedPreferences = getScannerPref()
                    val virtualTetherEnabled = virtualTetherSharedPreferences.getValueBoolean(
                        Constants.PREF_VIRTUAL_TETHER_SCANNER_SETTINGS,
                        false
                    )
                    if (MainActivity.MOT_SETTING_NOTIFICATION_ACTIVE && !notificaton_processed && !virtualTetherEnabled) {
                        if (isInBackgroundMode(MainActivity.getInstance())) {
                            val intent = Intent()
                            intent.action = Constants.ACTION_SCANNER_DISCONNECTED
                            intent.putExtra(
                                Constants.NOTIFICATIONS_TEXT,
                                "$scannerName has disconnected"
                            )
                            intent.putExtra(
                                Constants.NOTIFICATIONS_TYPE,
                                Constants.SESSION_TERMINATED
                            )
                            sendOrderedBroadcast(intent, null)
                        } else {
                            Toast.makeText(
                                MainActivity.getInstance(),
                                "$scannerName has disconnected", Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                Constants.SCANNER_APPEARED, Constants.AUX_SCANNER_CONNECTED -> {
                    LogUtil.logE(TAG, "SCANNER_APPEARED AUX_SCANNER_CONNECTED ")
                    notificaton_processed = false
                    result = false
                    val availableScanner = msg.obj as DCSScannerInfo

                    /* notify connections delegates */
                    mDevConnDelegates?.let {
                        for (delegate in it) {
                            if (delegate != null) {
                                result = delegate.scannerHasAppeared(availableScanner.scannerID)
                                if (result) {
                                    /*
                                 DevConnections delegates should NOT display any UI alerts,
                                 so from UI notification side the event is not processed
                                 */
                                    notificaton_processed = false
                                }
                            }
                        }
                    }

                    /* update dev list */
                    for (ex_info in mScannerInfoList) {
                        if (ex_info.scannerID == availableScanner.scannerID) {
                            mScannerInfoList.remove(ex_info)
                            break
                        }
                    }
                    mScannerInfoList.add(availableScanner)

                    /* notify dev list delegates */
                    MainActivity.mDevListDelegates?.let {
                        for (delegate in it) {
                            if (delegate != null) {
                                result = delegate.scannersListHasBeenUpdated()
                                if (result) {
                                    /*
                                 DeList delegates should NOT display any UI alerts,
                                 so from UI notification side the event is not processed
                                 */
                                    notificaton_processed = false
                                }
                            }
                        }
                    }

                    //Showing notifications in foreground and background mode
                    if (MainActivity.MOT_SETTING_NOTIFICATION_AVAILABLE && !notificaton_processed && !found) {
                        if (isInBackgroundMode(MainActivity.getInstance())) {
                            val intent = Intent()
                            intent.action = Constants.ACTION_SCANNER_CONNECTED
                            intent.putExtra(
                                Constants.NOTIFICATIONS_TEXT,
                                availableScanner.scannerName + " has appeared"
                            )
                            intent.putExtra(
                                Constants.NOTIFICATIONS_TYPE,
                                Constants.SCANNER_APPEARED
                            )
                            sendOrderedBroadcast(intent, null)
                        } else {
                            Toast.makeText(
                                MainActivity.getInstance(),
                                availableScanner.scannerName + " has appeared",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                Constants.SCANNER_DISAPPEARED -> {
                    LogUtil.logE(TAG, "SCANNER_DISAPPEARED")
                    notificaton_processed = false
                    result = false
                    val scannerID = msg.obj as Int
                    var scannerName = ""
                    /* notify connections delegates */
                    mDevConnDelegates?.let {
                        for (delegate in it) {
                            if (delegate != null) {
                                result = delegate.scannerHasDisappeared(scannerID)
                                if (result) {
                                    /*
                                 DevConnections delegates should NOT display any UI alerts,
                                 so from UI notification side the event is not processed
                                 */
                                    notificaton_processed = false
                                }
                            }
                        }
                    }

                    /* update dev list */
                    found = false
                    for (ex_info in mScannerInfoList) {
                        if (ex_info.scannerID == scannerID) {
                            /* find scanner with ID in dev list */
                            mScannerInfoList.remove(ex_info)
                            scannerName = ex_info.scannerName
                            found = true
                            break
                        }
                    }
                    if (!found) {
                        for (off_info in mOfflineScannerInfoList) {
                            if (off_info.scannerID == scannerID) {
                                scannerName = off_info.scannerName
                                break
                            }
                        }
                        LogUtil.logE(
                            TAG,
                            "ScannerAppEngine:dcssdkEventScannerDisappeared: scanner is not in list"
                        )
                    }

                    /* notify dev list delegates */
                    MainActivity.mDevListDelegates?.let {
                        for (delegate in it) {
                            if (delegate != null) {
                                result = delegate.scannersListHasBeenUpdated()
                                if (result) {
                                    /*
                                 DeList delegates should NOT display any UI alerts,
                                 so from UI notification side the event is not processed
                                 */
                                    notificaton_processed = false
                                }
                            }
                        }
                    }

                    val virtualTetherSettings: BarcodePrefProvider = getScannerPref()
                    val virtualTetherEnable = virtualTetherSettings.getValueBoolean(
                        Constants.PREF_VIRTUAL_TETHER_SCANNER_SETTINGS,
                        false
                    )
                    //Showing notifications in foreground and background mode
                    if (MainActivity.MOT_SETTING_NOTIFICATION_AVAILABLE
                        && !notificaton_processed && !found && !virtualTetherEnable
                    ) {

                        val notification_Msg = java.lang.StringBuilder()
                        notification_Msg.append(scannerName).append(" has disappeared")
                        if (isInBackgroundMode(MainActivity.getInstance())) {
                            val intent = Intent()
                            intent.action = Constants.ACTION_SCANNER_CONNECTED
                            intent.putExtra(
                                Constants.NOTIFICATIONS_TEXT,
                                notification_Msg.toString()
                            )
                            intent.putExtra(
                                Constants.NOTIFICATIONS_TYPE,
                                Constants.SCANNER_DISAPPEARED
                            )
                            sendOrderedBroadcast(intent, null)
                        } else {
                            Toast.makeText(
                                MainActivity.getInstance(),
                                notification_Msg.toString(),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    if (curAvailableScanner != null && scannerID == curAvailableScanner?.scannerId) {
                        curAvailableScanner = null
                    }
                }
            }
        }
    }

    /**
     * This method is responsible for resetting the virtual tether host configurations for the connected scanner
     */
    protected fun resetVirtualTetherHostConfigurations() {
        val settingsEditor = getScannerPref()
        settingsEditor.setValueBoolean(Constants.PREF_VIRTUAL_TETHER_HOST_FEEDBACK, false)
        settingsEditor.setValueBoolean(Constants.PREF_VIRTUAL_TETHER_HOST_VIBRATION_ALARM, false)
        settingsEditor.setValueBoolean(Constants.PREF_VIRTUAL_TETHER_HOST_AUDIO_ALARM, false)
        settingsEditor.setValueBoolean(Constants.PREF_VIRTUAL_TETHER_HOST_SCREEN_FLASH, false)
        settingsEditor.setValueBoolean(Constants.PREF_VIRTUAL_TETHER_HOST_POPUP_MESSAGE, false)
        settingsEditor.setValueBoolean(Constants.PREF_VIRTUAL_TETHER_SCANNER_SETTINGS, false)
    }

    private fun initializeDcsSdk() {
        /*MainActivity.sdkHandler?.dcssdkEnableAvailableScannersDetection(true)
        MainActivity.sdkHandler?.dcssdkSetOperationalMode(DCSSDK_MODE.DCSSDK_OPMODE_BT_NORMAL)
        MainActivity.sdkHandler?.dcssdkSetOperationalMode(DCSSDK_MODE.DCSSDK_OPMODE_BT_LE)
        MainActivity.sdkHandler?.dcssdkSetOperationalMode(DCSSDK_MODE.DCSSDK_OPMODE_USB_CDC)
        MainActivity.sdkHandler?.dcssdkSetOperationalMode(DCSSDK_MODE.DCSSDK_OPMODE_SNAPI)*/
    }

    fun connectToScanner(availableScanner: AvailableScanner?) {
        getActualScannersList()?.let { actualScannerList ->
            for (device in actualScannerList) {
                if (device.scannerID == availableScanner?.scannerId) {
                    availableScanner.isAutoReconnection =
                        device.isAutoCommunicationSessionReestablishment
                    availableScanner.connectionType = DCSSDK_CONN_TYPES.DCSSDK_CONNTYPE_BT_NORMAL
                }
            }
        }
        if (availableScanner != null) {
            if (!availableScanner.isConnected) {
                //scanner is not connected
                if (curAvailableScanner != null && availableScanner.scannerAddress != curAvailableScanner?.scannerAddress
                ) {
                    //disconnect curAvailableScanner
                    if (curAvailableScanner?.isConnected == true) {
                        curAvailableScanner?.scannerId?.let { disconnect(it) }
                    }
                }
                //connect to scanner
                connectScanner(availableScanner)
            } else {
                //scanner is already connected.
                curAvailableScanner = availableScanner
                if (curAvailableScanner?.isConnected == true) {
                    MainActivity.currentScannerName = availableScanner.scannerName.toString()
                    MainActivity.currentScannerAddress =
                        availableScanner.scannerAddress.toString()
                    MainActivity.currentAutoReconnectionState =
                        availableScanner.isAutoReconnection
                    MainActivity.currentScannerId = availableScanner.scannerId

                    Toast.makeText(this, "Scanner is connected..", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateBarcodeView(
        llBarcode: FrameLayout?,
        layoutParams: LinearLayout.LayoutParams,
        barCodeView: BarCodeView
    ) {
        val display: Display? = windowManager?.defaultDisplay
        val size = Point()
        display?.getSize(size)
        val width = size.x
        val height = size.y
        val orientation = this.resources.configuration.orientation
        var x = width * 9 / 10
        var y = x / 3
        if (getDeviceScreenSize() > 6) { // Check 6 is ok or not
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                x = width / 3
                y = x / 4
            } else {
                x = width * 2 / 3
                y = x / 3
            }
        }
        barCodeView.setSize(x, y)
        llBarcode?.addView(barCodeView, layoutParams)
    }

    private fun getDeviceScreenSize(): Double {
        var screenInches = 0.0
        val windowManager: WindowManager? = windowManager
        val display = windowManager?.defaultDisplay
        val mWidthPixels: Int
        val mHeightPixels: Int
        try {
            val realSize = Point()
            Display::class.java.getMethod("getRealSize", Point::class.java)
                .invoke(display, realSize)
            mWidthPixels = realSize.x
            mHeightPixels = realSize.y
            val dm = DisplayMetrics()
            windowManager?.defaultDisplay?.getMetrics(dm)
            val x = Math.pow((mWidthPixels / dm.xdpi).toDouble(), 2.0)
            val y = Math.pow((mHeightPixels / dm.ydpi).toDouble(), 2.0)
            screenInches = Math.sqrt(x + y)
        } catch (ignored: Exception) {
        }
        return screenInches
    }

    /*TODO permission*/
    @SuppressLint("MissingPermission")
    private fun getMacAddress(): String? {
        /*val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val address = bluetoothAdapter.address
        LogUtil.logE("!_@_ MAC :", address.toString())*/
        // return "E0:D0:83:0B:B9:7A"
        // return "0C:25:76:B4:0B:93"
        return "0c:25:76:b4:0b:95" // Sunmi Bluetooth MAC Address
        //0c:25:76:b4:0b:95
        //  return "0c:25:76:b4:0b:95"
    }


    /**
     * Receiver to handle the events about RFID Reader
     */
    private val onNotification: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctxt: Context, i: Intent) {

            //Since the application is in foreground, show a dialog.
            Toast.makeText(ctxt, i.getStringExtra(Constants.NOTIFICATIONS_TEXT), Toast.LENGTH_SHORT)
                .show()

            //Abort the broadcast since it has been handled.
            abortBroadcast()
        }
    }

    override fun scannerHasAppeared(scannerID: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun scannerHasDisappeared(scannerID: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun scannerHasConnected(scannerID: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun scannerHasDisconnected(scannerID: Int): Boolean {
        //pairNewScannerMenu.setTitle(R.string.menu_item_device_pair)
        saveScanner(null, false)
        return false
    }

    override fun dcssdkEventScannerAppeared(availableScanner: DCSScannerInfo?) {
        LogUtil.logE(TAG, "Event: dcssdkEventScannerAppeared")
        dataHandler.obtainMessage(
            com.pays.pos.utils.scanner.helpers.Constants.SCANNER_APPEARED,
            availableScanner
        ).sendToTarget()
    }

    override fun dcssdkEventScannerDisappeared(scannerID: Int) {
        LogUtil.logE(TAG, "Event: dcssdkEventScannerDisappeared")
        dataHandler.obtainMessage(
            com.pays.pos.utils.scanner.helpers.Constants.SCANNER_DISAPPEARED,
            scannerID
        ).sendToTarget()
    }

    override fun dcssdkEventCommunicationSessionEstablished(activeScanner: DCSScannerInfo?) {
        LogUtil.logE(TAG, "Event: dcssdkEventCommunicationSessionEstablished")
        dataHandler.obtainMessage(
            com.pays.pos.utils.scanner.helpers.Constants.SESSION_ESTABLISHED,
            activeScanner
        ).sendToTarget()
        resetVirtualTetherHostConfigurations()
        scannersListHasBeenUpdated()
    }

    override fun dcssdkEventCommunicationSessionTerminated(scannerID: Int) {
        LogUtil.logE(TAG, "Event: dcssdkEventCommunicationSessionTerminated")
        dataHandler.obtainMessage(
            com.pays.pos.utils.scanner.helpers.Constants.SESSION_TERMINATED,
            scannerID
        ).sendToTarget()
        scannersListHasBeenUpdated()
    }

    override fun dcssdkEventBarcode(barcodeData: ByteArray?, barcodeType: Int, fromScannerID: Int) {
        LogUtil.logE(TAG, "Event: dcssdkEventBarcode")
        val barcode = barcodeData?.let { Barcode(it, barcodeType, fromScannerID) }
        dataHandler.obtainMessage(
            com.pays.pos.utils.scanner.helpers.Constants.BARCODE_RECEIVED,
            barcode
        ).sendToTarget()
    }

    override fun dcssdkEventImage(imageData: ByteArray?, fromScannerID: Int) {
        LogUtil.logE(TAG, "Event: dcssdkEventImage")
        dataHandler.obtainMessage(
            com.pays.pos.utils.scanner.helpers.Constants.IMAGE_RECEIVED,
            imageData
        ).sendToTarget()
    }

    override fun dcssdkEventVideo(videoFrame: ByteArray?, fromScannerID: Int) {
        LogUtil.logE(TAG, "Event: dcssdkEventVideo")
        dataHandler.obtainMessage(
            com.pays.pos.utils.scanner.helpers.Constants.VIDEO_RECEIVED,
            videoFrame
        ).sendToTarget()
    }

    override fun dcssdkEventBinaryData(binaryData: ByteArray?, fromScannerID: Int) {
        LogUtil.logE(
            TAG,
            "BinaryData Event received no.of bytes : " + binaryData?.size + " for Scanner ID : " + fromScannerID
        )
    }

    override fun dcssdkEventFirmwareUpdate(firmwareUpdateEvent: FirmwareUpdateEvent?) {
        LogUtil.logE(TAG, "Event: dcssdkEventFirmwareUpdate")
        dataHandler.obtainMessage(
            com.pays.pos.utils.scanner.helpers.Constants.FW_UPDATE_EVENT,
            firmwareUpdateEvent
        ).sendToTarget()
    }

    override fun dcssdkEventAuxScannerAppeared(
        newTopology: DCSScannerInfo?,
        auxScanner: DCSScannerInfo?
    ) {
        LogUtil.logE(TAG, "Event: dcssdkEventAuxScannerAppeared")
        dataHandler.obtainMessage(
            com.pays.pos.utils.scanner.helpers.Constants.AUX_SCANNER_CONNECTED,
            auxScanner
        ).sendToTarget()
    }

    //save scanner data
    fun saveScanner(availableScanner: AvailableScanner?, connect: Boolean) {
        if (connect) {
            MainActivity.isAnyScannerConnected = true
            MainActivity.currentConnectedScanner = availableScanner
            getScannerPref().saveScannerData(availableScanner)
            MainActivity.lastConnectedScanner = availableScanner
            MainActivity.currentConnectedScannerID =
                availableScanner?.scannerId ?: MainActivity.SCANNER_ID_NONE
        } else {
            MainActivity.lastConnectedScanner = MainActivity.currentConnectedScanner
            MainActivity.currentConnectedScanner = null
            MainActivity.currentConnectedScannerID = MainActivity.SCANNER_ID_NONE
            MainActivity.isAnyScannerConnected = false
        }
    }

    open fun notifyAdapters(connectedScanner: Boolean) {
        notifyAdaptersCallBack?.invoke(connectedScanner)
    }

    open fun getScannerPref(): BarcodePrefProvider {
        return barcodePrefProvider
    }
}