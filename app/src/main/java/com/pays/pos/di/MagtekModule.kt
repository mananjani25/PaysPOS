package com.pays.pos.di

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.pays.pos.R
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.TLVParser
import com.pays.pos.utils.callback.magtekCallback
import com.magtek.mobile.android.mtlib.*
import com.magtek.mobile.android.mtlib.MTEMVEvent.*
import com.magtek.mobile.android.mtlib.MTSCRAEvent.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.experimental.and
import kotlin.experimental.or

@SuppressLint("MissingPermission")
@Singleton
open class MagtekModule @Inject constructor(
    @ApplicationContext private val mContext: Context,
    val prefProvider: PrefProvider
) : AppCompatActivity() {

    private var handlerStop: Boolean = false
    private var dataRecv: Boolean = false
    private var m_startTransactionActionPending = false
    var m_scra: MTSCRA? = null
    private var mScanCallback: CustomScanCallback? = null
    private var mHandler: Handler? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private val SCAN_PERIOD: Long = 5000
    private var mScanning = false
    private var listner: magtekCallback? = null

    private val mTypes = arrayOf("Swipe", "Chip", "Contactless")
    private val mTypeChecked = booleanArrayOf(false, true, false)
    private var mTransactionDialog: AlertDialog? = null
    private var m_emvMessageFormatRequestPending = false
    private var m_emvMessageFormat = 0

    fun setCallback(callback: magtekCallback) {
        listner = callback
    }


    private var mScraHandler = Handler(Looper.getMainLooper()) { msg ->

        LogUtil.logE("handlerStop", handlerStop.toString())

        if (handlerStop)
           return@Handler true

        when (msg.what) {
            OnDeviceConnectionStateChanged -> {

                OnDeviceConnectionStateChanged(msg.obj as MTConnectionState)
            }

            OnCardDataStateChanged -> {


                OnCardDataStateChanged(msg.obj as MTCardDataState)
            }

            OnDeviceResponse -> {

                onDeviceResponse(msg.obj as String)
            }
            OnDataReceived -> {

                OnCardDataReceived(msg.obj as IMTCardData)
            }

            OnTransactionStatus -> {

                OnTransactionStatus(msg.obj as ByteArray)
            }

            OnDisplayMessageRequest -> {

                OnDisplayMessageRequest(msg.obj as ByteArray)
            }

            OnARQCReceived -> {

                OnARQCReceived(msg.obj as ByteArray)
            }

            OnTransactionResult -> {
                OnTransactionResult(msg.obj as ByteArray)

            }

        }
        true
    }

    protected open fun OnCardDataStateChanged(cardDataState: MTCardDataState?) {
        when (cardDataState) {
            MTCardDataState.DataNotReady -> LogUtil.logE("[Card Data Not Ready]", "")
            MTCardDataState.DataReady -> LogUtil.logE("[Card Data Ready]", "")
            MTCardDataState.DataError -> LogUtil.logE("[Card Data Error]", "")
        }
    }

    private fun OnTransactionResult(data: ByteArray?) {

        LogUtil.logE("[Transaction Result]", _root_ide_package_.com.pays.pos.utils.TLVParser.getHexString(data))


        if (data != null) {
            if (data.isNotEmpty()) {
                val signatureRequired = data[0] != 0.toByte()
                val lenBatchData: Int = data.size - 3
                if (lenBatchData > 0) {
                    val batchData = ByteArray(lenBatchData)
                    System.arraycopy(data, 3, batchData, 0, lenBatchData)
                    LogUtil.logE("[Parsed Batch Data]", "")
                    val parsedTLVList = _root_ide_package_.com.pays.pos.utils.TLVParser.parseEMVData(batchData, false, "")
                    val cidString = _root_ide_package_.com.pays.pos.utils.TLVParser.getTagValue(parsedTLVList, "9F27")
                    val cidValue = _root_ide_package_.com.pays.pos.utils.TLVParser.getByteArrayFromHexString(cidString)
                    var approved = false
                    if (cidValue != null) {
                        if (cidValue.isNotEmpty()) {
                            if (cidValue[0] and 0x40.toByte() != 0.toByte()) {
                                approved = true
                            }
                        }
                    }
                    if (approved) {
                        if (signatureRequired) {
                            //  displayMessage2("( Signature Required )")
                        } else {
                            // displayMessage2("( No Signature Required )")
                        }
                    }
                }
            }
        }
    }

    private fun OnDisplayMessageRequest(bytes: ByteArray) {

        val message = _root_ide_package_.com.pays.pos.utils.TLVParser.getTextString(bytes, 0)
        LogUtil.logE("[Display Mes Request]", message)

//        if (message == "DECLINED"){
//            listner?.processStart("DECLINED", true)
//        }
    }

    private fun OnTransactionStatus(bytes: ByteArray) {

        LogUtil.logE("[Transaction Status]", _root_ide_package_.com.pays.pos.utils.TLVParser.getHexString(bytes))

        when {
            "0500010000" == _root_ide_package_.com.pays.pos.utils.TLVParser.getHexString(bytes) -> {
                listner?.processStart("Request Cancelled", true)
            }
            "0600910000" == _root_ide_package_.com.pays.pos.utils.TLVParser.getHexString(bytes) -> {
                listner?.processStart("Request Cancelled", true)
            }
            "0200120000" == _root_ide_package_.com.pays.pos.utils.TLVParser.getHexString(bytes) -> {
                if (!dataRecv) {
                    listner?.processStart("Card Error", true)
                } else dataRecv = false
            }
        }


    }

    private fun OnARQCReceived(data: ByteArray) {

        dataRecv = true
        listner?.OnARQCReceived(data)
    }


    private fun OnCardDataReceived(imtCardData: IMTCardData) {

        listner?.OnCardDataReceived(imtCardData)
    }


    fun setupInit() {

        mHandler = Handler(Looper.getMainLooper())
        m_scra = MTSCRA(mContext, mScraHandler)

        val bluetoothManager =
            mContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

        mBluetoothAdapter = bluetoothManager.adapter

    }

    private fun OnDeviceConnectionStateChanged(mtConnectionState: MTConnectionState) {

        listner?.onConnect(mtConnectionState)
    }

    private fun onDeviceResponse(data: String) {

        if (m_emvMessageFormatRequestPending) {
            m_emvMessageFormatRequestPending = false
            val emvMessageFormatResponseByteArray = _root_ide_package_.com.pays.pos.utils.TLVParser.getByteArrayFromHexString(data)
            if (emvMessageFormatResponseByteArray.size == 3) {

                if (emvMessageFormatResponseByteArray[0].toInt() == 0 && emvMessageFormatResponseByteArray[1].toInt() == 1) {
                    m_emvMessageFormat = emvMessageFormatResponseByteArray[2].toInt()
                }
            }
        } else if (m_startTransactionActionPending) {
            m_startTransactionActionPending = false

            listner?.processStart("Please insert or swipe card", false)
            startTransaction()
        }

    }


    fun startTransaction() {
        LogUtil.logE("[Start Transaction 2]", "Result=$")
        if (m_scra != null) {
            val timeLimit: Byte = 0x3C

            //byte cardType = 0x02;  // Chip Only
            val cardType = 0x03.toByte()
            //byte cardType = 0x02;  // Chip Only
            //byte cardType = 0x03;  // MSR + Chip
            val option = if (isQuickChipEnabled()) 0x80.toByte() else 0
            val amount = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x15, 0x00)
            val transactionType: Byte = 0x00 // Purchase
            val cashBack = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
            val currencyCode = byteArrayOf(0x08, 0x40)
            val reportingOption: Byte = 0x02 // All Status Changes

            val result = m_scra!!.startTransaction(
                timeLimit,
                cardType,
                option,
                amount,
                transactionType,
                cashBack,
                currencyCode,
                reportingOption
            )
            LogUtil.logE("[Start Transaction 3]", "Result=$result")

        }

    }

    private fun isQuickChipEnabled(): Boolean {
        return false
    }

    open class CustomScanCallback(private val listner: magtekCallback) :
        ScanCallback() {


        override fun onBatchScanResults(results: List<ScanResult>) {
            val resultsIt = results.listIterator()
            while (resultsIt.hasNext()) {
                val result = resultsIt.next()
                processScanResult(result)
            }
        }

        override fun onScanFailed(errorCode: Int) {}
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)


            processScanResult(result)
        }

        private fun processScanResult(result: ScanResult?) {

            var found = false
            if (result != null) {
                val scanRecord = result.scanRecord
                val device = result.device
                if (scanRecord != null) {
                    val uuidList: List<UUID> = parseUUIDs(scanRecord.bytes)
                    val uuidListIt = uuidList.listIterator()
                    while (uuidListIt.hasNext()) {
                        val scanUuid = uuidListIt.next()
                        LogUtil.logE("scanUuid", scanUuid.toString())
                        LogUtil.logE(
                            "scanUuid1",
                            MTDeviceConstants.UUID_SCRA_BLE_EMV_DEVICE_READER_SERVICE.toString()
                        )
                        if (scanUuid.compareTo(MTDeviceConstants.UUID_SCRA_BLE_EMV_DEVICE_READER_SERVICE) == 0) {
                            found = true
                        }
                    }
                }
                if (found && device != null) {
                    listner.onDeviceList(device)
                }
            }
        }

        private fun parseUUIDs(advertisedData: ByteArray): List<UUID> {
            val uuids: MutableList<UUID> = ArrayList()
            var offset = 0
            while (offset < advertisedData.size - 2) {
                var len = advertisedData[offset++].toInt()
                if (len == 0) break
                val type = advertisedData[offset++].toInt()
                when (type) {
                    0x06, 0x07 ->                     // Loop through the advertised 128-bit UUID's.
                        while (len >= 16) {
                            try {
                                // Wrap the advertised bits and order them.
                                val buffer = ByteBuffer.wrap(
                                    advertisedData,
                                    offset++, 16
                                ).order(ByteOrder.LITTLE_ENDIAN)
                                val mostSignificantBit = buffer.long
                                val leastSignificantBit = buffer.long
                                uuids.add(
                                    UUID(
                                        leastSignificantBit,
                                        mostSignificantBit
                                    )
                                )
                            } catch (e: IndexOutOfBoundsException) {
                                continue
                            } finally {
                                offset += 15
                                len -= 16
                            }
                        }
                    else -> offset += len - 1
                }
            }
            return uuids
        }
    }

    @SuppressLint("MissingPermission")
    fun scanBluetoothDevice(enable: Boolean) {
        if (!mBluetoothAdapter!!.isEnabled) {
            return
        }
        if (enable) {
            // Get a set of currently paired devices
            val pairedDevices = mBluetoothAdapter!!.bondedDevices
            LogUtil.logE("pairedDevices", pairedDevices.size.toString())
            if (pairedDevices.size > 0) {
                for (device in pairedDevices) {
                    if (device.type == BluetoothDevice.DEVICE_TYPE_LE) {
                        listner?.onDeviceList(device)
                    }
                }
            }

        }

    }

    @SuppressLint("MissingPermission")
    fun scanLeDevice(enable: Boolean) {

        if (!mBluetoothAdapter!!.isEnabled) {
            return
        }
        if (enable) {

            listner?.startScanning()

            stopScanning()

            // Stops scanning after a pre-defined scan period.
            mHandler!!.postDelayed({
                stopScanning()
                listner?.stopScanning()
            }, SCAN_PERIOD)
            mScanning = true
            val leScanner = mBluetoothAdapter!!.bluetoothLeScanner
            if (leScanner != null) {
                if (mScanCallback == null) {
                    mScanCallback = listner?.let { CustomScanCallback(it) }
                }

                leScanner.startScan(mScanCallback)
            }


        } else {
            stopScanning()
        }
    }

    private fun stopScanning() {
        if (mScanning) {
            if (mBluetoothAdapter != null) {
                val leScanner = mBluetoothAdapter!!.bluetoothLeScanner
                if (leScanner != null && mScanCallback != null) {
                    try {
                        if (ActivityCompat.checkSelfPermission(
                                mContext,
                                Manifest.permission.BLUETOOTH_SCAN
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            return
                        }
                        leScanner.stopScan(mScanCallback)
                        LogUtil.logE("stopScanning", "Called")
                    } catch (ex: Exception) {
                        LogUtil.logE("stopScanning error", "Called")
                        ex.printStackTrace()
                    }
                    mScanCallback = null
                }
            }
            mScanning = false
        }
    }

    fun openDevice(mcAddress: String): Long {
        Log.i("TAG", "SCRADevice openDevice")
        var result: Long = -1

        if (m_scra != null) {

            val divisionChar = ':' //change to '-' if you want your mac to be like 00-15-5D-03-8D-01


            val formattedMAC =
                mcAddress.replace("(.{2})".toRegex(), "$1$divisionChar").substring(0, 17)

            LogUtil.logE("mcAddress1 :: ", formattedMAC)

            m_scra!!.setConnectionType(MTConnectionType.BLEEMV)
            m_scra!!.setAddress(formattedMAC)
            m_scra!!.setConnectionRetry(true)
            m_scra!!.openDevice()
            result = 0
        }
        LogUtil.logE("openDevice ", result.toString())
        return result
    }

    fun openDeviceTest(string: String): Long {
        LogUtil.logE("TAG", "SCRADevice openDevice")
        var result: Long = -1

        if (m_scra != null) {
            Log.i("TAG", "SCRADevice $m_scra")
            m_scra!!.setConnectionType(MTConnectionType.BLEEMV)
            m_scra!!.setAddress(string)
            m_scra!!.setConnectionRetry(true)
            m_scra!!.openDevice()
            result = 0
        }
        LogUtil.logE("openDeviceTest ", result.toString())
        return result
    }

    fun closeDevice(): Long {
        Log.i("TAG", "SCRADevice closeDevice")
        var result: Long = -1
        if (m_scra != null) {
            m_scra!!.closeDevice()
            result = 0
        }
        return result
    }

    fun startTransactionWithLED() {
        m_startTransactionActionPending = true
        setLED(true)
    }


    fun setLED(on: Boolean) {
        if (m_scra != null) {
            if (on) {
                m_scra!!.sendCommandToDevice(MTDeviceConstants.SCRA_DEVICE_COMMAND_STRING_SET_LED_ON)
            } else {
                m_scra!!.sendCommandToDevice(MTDeviceConstants.SCRA_DEVICE_COMMAND_STRING_SET_LED_OFF)
            }
        }
    }

    open fun cancelTransaction() {
        if (m_scra != null) {
            val result = m_scra!!.cancelTransaction()
            listner?.processStart("Cancel Transaction", true)
            LogUtil.logE("[Cancel Transaction]", "(Result=$result)")
        }
    }


    fun stopListner(boolean: Boolean) {

        handlerStop = boolean

    }


}