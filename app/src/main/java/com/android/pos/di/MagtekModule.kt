package com.android.pos.di

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
import com.android.pos.R
import com.android.pos.utils.TLVParser
import com.android.pos.utils.callback.magtekCallback
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
            MTCardDataState.DataNotReady -> Log.e("[Card Data Not Ready]", "")
            MTCardDataState.DataReady -> Log.e("[Card Data Ready]", "")
            MTCardDataState.DataError -> Log.e("[Card Data Error]", "")
        }
    }

    private fun OnTransactionResult(data: ByteArray?) {

        Log.e("[Transaction Result]", TLVParser.getHexString(data))


        if (data != null) {
            if (data.isNotEmpty()) {
                val signatureRequired = data[0] != 0.toByte()
                val lenBatchData: Int = data.size - 3
                if (lenBatchData > 0) {
                    val batchData = ByteArray(lenBatchData)
                    System.arraycopy(data, 3, batchData, 0, lenBatchData)
                    Log.e("[Parsed Batch Data]", "")
                    val parsedTLVList = TLVParser.parseEMVData(batchData, false, "")
                    val cidString = TLVParser.getTagValue(parsedTLVList, "9F27")
                    val cidValue = TLVParser.getByteArrayFromHexString(cidString)
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

        val message = TLVParser.getTextString(bytes, 0)
        Log.e("[Display Mes Request]", message)
    }

    private fun OnTransactionStatus(bytes: ByteArray) {

        Log.e("[Transaction Status]", TLVParser.getHexString(bytes))

        when {
            "0500010000" == TLVParser.getHexString(bytes) -> {
                listner?.processStart("Request Canceled", true)
            }
            "0600910000" == TLVParser.getHexString(bytes) -> {
                listner?.processStart("Request Canceled", true)
            }
            "0200120000" == TLVParser.getHexString(bytes) -> {
                if (!dataRecv) {
                    listner?.processStart("Card Error", true)
                } else dataRecv = false
            }
        }


    }

    private fun OnARQCReceived(data: ByteArray) {

        dataRecv = true

        Log.e("ARQC Received", TLVParser.getHexString(data))


//        val parsedTLVList = TLVParser.parseEMVData(data, true, "")
//
//        if (parsedTLVList != null) {
//            val macKSNString = TLVParser.getTagValue(parsedTLVList, "DFDF54")
//            val macKSN = TLVParser.getByteArrayFromHexString(macKSNString)
//            val macEncryptionTypeString = TLVParser.getTagValue(parsedTLVList, "DFDF55")
//            val macEncryptionType = TLVParser.getByteArrayFromHexString(macEncryptionTypeString)
//            val deviceSNString = TLVParser.getTagValue(parsedTLVList, "DFDF25")
//            val deviceSN = TLVParser.getByteArrayFromHexString(deviceSNString)
//
//            val approved: Boolean = true
//            var response: ByteArray? = null
//            Log.e("m_emvMessageFormat", m_emvMessageFormat.toString())
//            if (m_emvMessageFormat == 0) {
//                response = buildAcquirerResponseFormat0(deviceSN, approved)
//            } else if (m_emvMessageFormat == 1) {
//                response =
//                    buildAcquirerResponseFormat1(macKSN, macEncryptionType, deviceSN, approved)
//            }
//            setAcquirerResponse(response)
//        }

        listner?.OnARQCReceived(data)
    }

    private fun setAcquirerResponse(response: ByteArray?) {
        if (m_scra != null && response != null) {
            Log.e("Sending Acquirer Res", TLVParser.getHexString(response))
            m_scra!!.setAcquirerResponse(response)
        }
    }

    private fun buildAcquirerResponseFormat0(
        deviceSN: ByteArray?,
        approved: Boolean
    ): ByteArray? {
        var response: ByteArray? = null
        var lenSN = 0
        if (deviceSN != null) lenSN = deviceSN.size
        val snTag = byteArrayOf(0xDF.toByte(), 0xDF.toByte(), 0x25, lenSN.toByte())
        val container = byteArrayOf(0xFA.toByte(), 0x06, 0x70, 0x04)
        val approvedARC = byteArrayOf(0x8A.toByte(), 0x02, 0x30, 0x30)
        val declinedARC = byteArrayOf(0x8A.toByte(), 0x02, 0x30, 0x35)
        var len = 4 + snTag.size + lenSN + container.size + approvedARC.size
        response = ByteArray(len)
        var i = 0
        len -= 2
        response[i++] = (len shr 8 and 0xFF).toByte()
        response[i++] = (len and 0xFF).toByte()
        len -= 2
        response[i++] = 0xF9.toByte()
        response[i++] = len.toByte()
        System.arraycopy(snTag, 0, response, i, snTag.size)
        i += snTag.size
        System.arraycopy(deviceSN, 0, response, i, deviceSN!!.size)
        i += deviceSN.size
        System.arraycopy(container, 0, response, i, container.size)
        i += container.size
        if (approved) {
            System.arraycopy(approvedARC, 0, response, i, approvedARC.size)
        } else {
            System.arraycopy(declinedARC, 0, response, i, declinedARC.size)
        }
        return response
    }

    private fun buildAcquirerResponseFormat1(
        macKSN: ByteArray?,
        macEncryptionType: ByteArray?,
        deviceSN: ByteArray?,
        approved: Boolean
    ): ByteArray? {
        var response: ByteArray? = null
        var lenMACKSN = 0
        var lenMACEncryptionType = 0
        var lenSN = 0
        if (macKSN != null) {
            lenMACKSN = macKSN.size
        }
        if (macEncryptionType != null) {
            lenMACEncryptionType = macEncryptionType.size
        }
        if (deviceSN != null) {
            lenSN = deviceSN.size
        }
        val macKSNTag = byteArrayOf(
            0xDF.toByte(), 0xDF.toByte(), 0x54,
            lenMACKSN.toByte()
        )
        val macEncryptionTypeTag = byteArrayOf(
            0xDF.toByte(),
            0xDF.toByte(), 0x55, lenMACEncryptionType.toByte()
        )
        val snTag = byteArrayOf(0xDF.toByte(), 0xDF.toByte(), 0x25, lenSN.toByte())
        val container = byteArrayOf(0xFA.toByte(), 0x06, 0x70, 0x04)
        val approvedARC = byteArrayOf(0x8A.toByte(), 0x02, 0x30, 0x30)
        val declinedARC = byteArrayOf(0x8A.toByte(), 0x02, 0x30, 0x35)
        val lenTLV =
            4 + macKSNTag.size + lenMACKSN + macEncryptionTypeTag.size + lenMACEncryptionType + snTag.size + lenSN + container.size + approvedARC.size
        var lenPadding = 0
        if (lenTLV % 8 > 0) {
            lenPadding = 8 - lenTLV % 8
        }
        val lenData = lenTLV + lenPadding + 4
        response = ByteArray(lenData)
        var i = 0
        response[i++] = (lenData - 2 shr 8 and 0xFF).toByte()
        response[i++] = (lenData - 2 and 0xFF).toByte()
        response[i++] = 0xF9.toByte()
        response[i++] = (lenTLV - 4).toByte()
        System.arraycopy(macKSNTag, 0, response, i, macKSNTag.size)
        i += macKSNTag.size
        System.arraycopy(macKSN, 0, response, i, macKSN!!.size)
        i += macKSN.size
        System.arraycopy(macEncryptionTypeTag, 0, response, i, macEncryptionTypeTag.size)
        i += macEncryptionTypeTag.size
        System.arraycopy(macEncryptionType, 0, response, i, macEncryptionType!!.size)
        i += macEncryptionType.size
        System.arraycopy(snTag, 0, response, i, snTag.size)
        i += snTag.size
        System.arraycopy(deviceSN, 0, response, i, deviceSN!!.size)
        i += deviceSN.size
        System.arraycopy(container, 0, response, i, container.size)
        i += container.size
        if (approved) {
            System.arraycopy(approvedARC, 0, response, i, approvedARC.size)
        } else {
            System.arraycopy(declinedARC, 0, response, i, declinedARC.size)
        }
        return response
    }


    private fun OnCardDataReceived(imtCardData: IMTCardData) {

        listner?.OnCardDataReceived(imtCardData)
    }


    init {

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
            val emvMessageFormatResponseByteArray = TLVParser.getByteArrayFromHexString(data)
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

    private fun startTransaction() {
        var type: Byte = 0
        if (mTypeChecked[0]) {
            type = type or 0x01.toByte()
        }
        if (mTypeChecked[1]) {
            type = type or 0x02.toByte()
        }
        if (mTypeChecked[2]) {
            type = type or 0x04.toByte()
        }
        startTransactionWithOptions(type)
    }

    private fun startTransactionWithOptions(cardType1: Byte) {
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
            Log.e("[Start Transaction]", "Result=$result")

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
                        Log.e("scanUuid", scanUuid.toString())
                        Log.e(
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
            Log.e("pairedDevices",pairedDevices.size.toString())
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
                        Log.e("stopScanning", "Called")
                    } catch (ex: Exception) {
                        Log.e("stopScanning error", "Called")
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

            Log.e("mcAddress1 :: ", formattedMAC)

            m_scra!!.setConnectionType(MTConnectionType.BLEEMV)
            m_scra!!.setAddress(formattedMAC)
            m_scra!!.setConnectionRetry(true)
            m_scra!!.openDevice()
            result = 0
        }
        Log.e("openDevice ", result.toString())
        return result
    }

    fun openDeviceTest(string: String): Long {
        Log.e("TAG", "SCRADevice openDevice")
        var result: Long = -1

        if (m_scra != null) {
            Log.i("TAG", "SCRADevice $m_scra")
            m_scra!!.setConnectionType(MTConnectionType.BLEEMV)
            m_scra!!.setAddress(string)
            m_scra!!.setConnectionRetry(true)
            m_scra!!.openDevice()
            result = 0
        }
        Log.e("openDeviceTest ", result.toString())
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
            Log.e("[Cancel Transaction]", "(Result=$result)")
        }
    }


    fun stopListner() {

        mScraHandler.removeCallbacksAndMessages(null)
    }


}