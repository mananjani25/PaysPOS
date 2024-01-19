package com.pays.pos.ui.fragments.magtekPro

import android.content.Context
import android.graphics.*
import android.util.Log
import com.pays.pos.data.remote.Constants
import com.pays.pos.di.PrefProvider
import com.pays.pos.ui.fragments.checkout.CheckoutDetailsFragmentNew
import com.pays.pos.ui.fragments.checkout.CheckoutDineInFragmentNew
import com.pays.pos.utils.LogUtil
import com.magtek.mobile.android.mtcms.MTParser
import com.magtek.mobile.android.mtusdk.*
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.experimental.and

@Singleton
open class SessionManager @Inject constructor(@ApplicationContext private val mContext: Context) :
    IEventSubscriber, IConfigurationCallback,
    IFallbackAdapter {
    private var isFragment: Boolean = false
    var device: IDevice? = null
    private var mOutputFragment: CheckoutDetailsFragmentNew? = null
    private var mDevicesFragment: MagtekProFragment? = null
    private var mDineInFragment: CheckoutDineInFragmentNew? = null
    private var mTransaction: Transaction? = null
    private var mGetSignatureFromDevice = false
    private var mFallbackManager: FallbackManager? = null

    @Inject
    lateinit var prefProvider: PrefProvider


    val transaction: ITransaction?
        get() = mTransaction
    val deviceControl: IDeviceControl
        get() = device!!.deviceControl
    private val deviceConfiguration: IDeviceConfiguration
        get() = device!!.deviceConfiguration

    fun setOutputFragment(outputFragment: CheckoutDetailsFragmentNew?) {
        mOutputFragment = outputFragment
        isFragment = false
    }

    fun setDineInFragment(outputFragment: CheckoutDineInFragmentNew?) {
        mDineInFragment = outputFragment
        isFragment = false
    }

    open fun setDevicesFragment(devicesFragment: MagtekProFragment) {
        mDevicesFragment = devicesFragment
        isFragment = true
    }

    fun sendToOutput(data: String?) {
        data?.let { LogUtil.logE("sendToOutput", it) }
    }

    private fun subscribeAll() {
        val device = device
        if (device != null) {
            device.unsubscribeAll(this)
            device.subscribeAll(this)
        }
    }

    var isConnected: Boolean = false
        get() {
            var connected = false
            val device = device
            if (device != null && device.connectionState == ConnectionState.Connected) {
                connected = true
            }
            return connected
        }
    val isDisconnected: Boolean
        get() {
            var disconnected = true
            val device = device
            if (device != null && device.connectionState != ConnectionState.Disconnected) {
                disconnected = false
            }
            return disconnected
        }

    fun connectDevice() {
        try {
            subscribeAll()
            val deviceControl: IDeviceControl? = deviceControl
            deviceControl?.open()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun disconnectDevice() {
        try {
            val deviceControl: IDeviceControl? = deviceControl
            deviceControl?.close()
        } catch (ex: Exception) {
        }
    }

    val isManualEntryTransaction: Boolean
        get() {
            var result = false
            if (mTransaction != null) {
                val paymentMethods = mTransaction!!.PaymentMethods()
                if (paymentMethods != null) {
                    if (paymentMethods.contains(PaymentMethod.ManualEntry)) {
                        result = true
                    }
                }
            }
            return result
        }

    fun startTransaction(transaction: Transaction?, getSignature: Boolean, fallback: Boolean) {
        if (transaction != null) {
            mTransaction = transaction
            mGetSignatureFromDevice = getSignature
            mFallbackManager = null
            if (fallback) {
                sendToOutput("[Fallback Enabled]")
                mFallbackManager = FallbackManager(this, mTransaction!!)
                mTransaction!!.setPreventMSRSignatureForCardWithICC(true) // Prevent signature capture on device during MSR transaction if card has ICC
            }
            val device = device
            if (device != null) {
                subscribeAll()
                if (device.startTransaction(transaction)) {
                    sendToOutput("[Transaction Started]")
                    sendToOutput("Amount=" + mTransaction!!.Amount() + ", Timeout=" + mTransaction!!.Timeout() + ", Transaction Type=" + mTransaction!!.TransactionType())
                }
            }
        }
    }

    fun cancelTransaction() {
        val device = device
        device?.cancelTransaction()
    }

    fun requestPIN(): Boolean {
        var result = false
        val device = device
        if (device != null) {
            sendToOutput("Request PIN from Device")
            result = device.requestPIN()
        }
        return result
    }

    fun requestSignature(): Boolean {
        var result = false
        val device = device
        if (device != null) {
            sendToOutput("Request Signature from Device")
            result = device.requestSignature()
        }
        return result
    }

    fun sendSelection(status: Byte, selection: Byte) {
        val device = device
        device?.sendSelection(BaseData(byteArrayOf(status, selection)))
    }

    fun sendAuthorization(data: ByteArray?) {
        val device = device
        device?.sendAuthorization(BaseData(data))
    }

    fun deviceReset() {
        try {
            val deviceControl: IDeviceControl? = deviceControl
            deviceControl?.deviceReset()
        } catch (ex: Exception) {
        }
    }


    fun displayMessage(messageID: Byte) {
        try {
            val deviceControl: IDeviceControl? = deviceControl
            if (deviceControl != null) {
                sendToOutput("Request Display Message ID: $messageID")
                deviceControl.displayMessage(messageID, 0.toByte())
            }
        } catch (ex: Exception) {
            sendToOutput("Request Display Message Failed: " + ex.message)
        }
    }


    fun getChallenge(data: String) {
        try {
            val deviceConfiguration: IDeviceConfiguration? = deviceConfiguration
            if (deviceConfiguration != null) {
                sendToOutput("Get Challenge [$data]")
                val token =
                    deviceConfiguration.getChallengeToken(MTParser.getByteArrayFromHexString(data))
                if (token != null) {
                    sendToOutput("Challenge Token=" + MTParser.getHexString(token))
                }
            }
        } catch (ex: Exception) {
            sendToOutput("Set Display Image Failed: " + ex.message)
        }
    }

    override fun OnProgress(progress: Int) {

    }


    override fun OnResult(status: StatusCode, data: ByteArray) {
    }

    override fun OnCalculateMAC(
        macType: Byte,
        data: ByteArray
    ): IResult {
        return Result(StatusCode.UNAVAILABLE)
    }


    override fun OnEvent(eventType: EventType, data: IData) {
        Log.d(TAG, "OnEvent: eventType=$eventType")
        try {
            if (isFragment)
                mDevicesFragment?.processEvent(eventType, data)
            mOutputFragment?.processEvent(eventType, data)
            mDineInFragment?.processEvent(eventType, data)


            when (eventType) {
                EventType.ConnectionState -> {
                    when (ConnectionStateBuilder.GetValue(data.StringValue())) {
                        ConnectionState.Connected -> {
                            sendToOutput("[CONNECTED]")

                            prefProvider.setValueboolean(Constants.DYNANA_FLAX, true)
                            //updateDeviceStatusUI(true);
                        }
                        ConnectionState.Disconnected -> {
                            sendToOutput("[DISCONNECTED]")

                            prefProvider.setValueboolean(Constants.DYNANA_FLAX, false)
                            //updateDeviceStatusUI(false);
                        }
                        ConnectionState.Disconnecting -> {
                            sendToOutput("[DISCONNECTING]")
                        }
                        ConnectionState.Connecting -> {
                            sendToOutput("[CONNECTING]")
                        }
                        else -> ""
                    }
                }
                EventType.DeviceResponse -> sendToOutput("""[Response]${data.StringValue()}""".trimIndent())
                EventType.DeviceNotification -> sendToOutput("""[Notification]${data.StringValue()}""".trimIndent())
                EventType.CardData -> sendToOutput("""[MSR]${data.StringValue()}""".trimIndent())
                EventType.TransactionStatus -> {
                    when (TransactionStatusBuilder.GetStatusCode(data.StringValue())) {
                        TransactionStatus.CardSwiped -> {
                            sendToOutput("[CARD SWIPED]")
                        }
                        TransactionStatus.CardInserted -> {
                            sendToOutput("[CARD INSERTED]")
                        }
                        TransactionStatus.CardRemoved -> {
                            sendToOutput("[CARD REMOVED]")
                        }
                        TransactionStatus.CardDetected -> {
                            sendToOutput("[CARD DETECTED]")
                        }
                        TransactionStatus.CardCollision -> {
                            sendToOutput("[CARD COLLISION]")
                        }
                        TransactionStatus.TimedOut -> {
                            sendToOutput("[TRANSACTION TIMED OUT]")
                            //setTransactionStatus(false);
                        }
                        TransactionStatus.HostCancelled -> {
                            sendToOutput("[HOST CANCELLED]")
                            //setTransactionStatus(false);
                        }
                        TransactionStatus.TransactionCancelled -> {
                            sendToOutput("[TRANSACTION CANCELLED]")
                            val statusDetail =
                                TransactionStatusBuilder.GetStatusDetail(data.StringValue())
                            val deviceDetail =
                                TransactionStatusBuilder.GetDeviceDetail(data.StringValue())
                            sendToOutput("(Status Detail=$statusDetail)")
                            sendToOutput("(Device Detail=$deviceDetail)")
                            //setTransactionStatus(false);
                        }
                        TransactionStatus.TransactionInProgress -> {
                            sendToOutput("[TRANSACTION IN PROGRESS]")
                        }
                        TransactionStatus.TransactionError -> {
                            sendToOutput("[TRANSACTION ERROR]")
                            val statusDetail =
                                TransactionStatusBuilder.GetStatusDetail(data.StringValue())
                            val deviceDetail =
                                TransactionStatusBuilder.GetDeviceDetail(data.StringValue())
                            sendToOutput("(Status Detail=$statusDetail)")
                            sendToOutput("(Device Detail=$deviceDetail)")
                            //setTransactionStatus(false);
                        }
                        TransactionStatus.TransactionCompleted -> {
                            sendToOutput("[TRANSACTION COMPLETED]")
                            //setTransactionStatus(false);
                        }
                        TransactionStatus.TransactionApproved -> {
                            sendToOutput("[TRANSACTION APPROVED]")
                            //setTransactionStatus(false);
                        }
                        TransactionStatus.TransactionDeclined -> {
                            sendToOutput("[TRANSACTION DECLINED]")
                            //setTransactionStatus(false);
                        }
                        TransactionStatus.TransactionFailed -> {
                            sendToOutput("[TRANSACTION FAILED]")
                            //setTransactionStatus(false);
                        }
                        TransactionStatus.TransactionNotAccepted -> {
                            sendToOutput("[TRANSACTION NOT ACCEPTED]")
                            //setTransactionStatus(false);
                        }
                        TransactionStatus.SignatureCaptureRequested -> {
                            sendToOutput("[SIGNATURE CAPTURE REQUESTED]")

                            if (mFallbackManager == null) {
                                if (mGetSignatureFromDevice) {
                                    requestSignature()
                                }
                            }
                        }
                        TransactionStatus.TechnicalFallback -> {
                            sendToOutput("[TECHNICAL FALLBACK]")
                        }
                        TransactionStatus.QuickChipDeferred -> {
                            sendToOutput("[TRANSACTION STATUS / QUICK CHIP DEFERRED]")
                        }
                        TransactionStatus.DataEntered -> {
                            sendToOutput("[DATA ENTERED]")
                        }
                        TransactionStatus.TryAnotherInterface -> {
                            sendToOutput("[TRY ANOTHER INTERFACE]")
                        }
                        else -> ""
                    }
                }
                EventType.DisplayMessage -> {
                    val displayMessage = data.StringValue()
                    if (displayMessage.length > 1) {
                        sendToOutput("[DisplayMessage] : $displayMessage")
                    }
                }
                EventType.InputRequest -> sendToOutput("[InputRequest]")
                EventType.AuthorizationRequest -> sendToOutput(
                    """[Authorization Request]${
                        MTParser.getHexString(
                            data.ByteArray()
                        )
                    }""".trimIndent()
                )
                EventType.TransactionResult -> {
                    sendToOutput(
                        """[Transaction Result]${
                            MTParser.getHexString(
                                data.ByteArray()
                            )
                        }""".trimIndent()
                    )
//                    mOutputFragment?.processEvent(false, MTParser.getHexString(data.ByteArray()))
                }
                EventType.PINBlock -> sendToOutput("""PIN Block]${data.StringValue()}""".trimIndent())
                EventType.Signature -> {
                    sendToOutput("""[Signature]${MTParser.getHexString(data.ByteArray())}""".trimIndent())
                    updateSignatureView(data.ByteArray())
                }
                EventType.OperationStatus -> {
                    val opStatus = OperationStatusBuilder.GetStatusCode(data.StringValue())
                    val opDetail = OperationStatusBuilder.GetOperationDetail(data.StringValue())
                    when (opStatus) {
                        OperationStatus.Started -> {
                            sendToOutput("[OPERATION STARTED: $opDetail]")
                        }
                        OperationStatus.Warning -> {
                            sendToOutput("[OPERATION WARNING: $opDetail]")
                            val statusDetail =
                                OperationStatusBuilder.GetStatusDetail(data.StringValue())
                            val deviceDetail =
                                OperationStatusBuilder.GetDeviceDetail(data.StringValue())
                            sendToOutput("(Status Detail=$statusDetail)")
                            sendToOutput("(Device Detail=$deviceDetail)")
                        }
                        OperationStatus.Failed -> {
                            sendToOutput("[OPERATION FAILED: $opDetail]")
                            val statusDetail =
                                OperationStatusBuilder.GetStatusDetail(data.StringValue())
                            val deviceDetail =
                                OperationStatusBuilder.GetDeviceDetail(data.StringValue())
                            sendToOutput("(Status Detail=$statusDetail)")
                            sendToOutput("(Device Detail=$deviceDetail)")
                        }
                        OperationStatus.Done -> {
                            sendToOutput("[OPERATION DONE: $opDetail]")
                        }
                        else -> ""
                    }
                }
                EventType.DeviceEvent -> {
                    val deviceEvent = DeviceEventBuilder.GetEventValue(data.StringValue())
                    val eventDetail = DeviceEventBuilder.GetDetail(data.StringValue())
                    if (deviceEvent == DeviceEvent.DeviceResetOccurred) {
                        sendToOutput("[DEVICE RESET OCCURRED: $eventDetail]")
                    } else if (deviceEvent == DeviceEvent.DeviceResetWillOccur) {
                        sendToOutput("[DEVICE RESET WILL OCCUR: $eventDetail]")
                    }
                }
                else -> ""
            }

            if (mFallbackManager != null) {
                try {
                    mFallbackManager!!.OnEvent(eventType, data)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    override fun OnUseChipReader() {
        sendToOutput("Display Message: USE CHIP READER")
        displayMessage(0x11.toByte())
        val wsThread: Thread = object : Thread() {
            override fun run() {
                try {
                    sleep(3000)
                    val contactless =
                        mTransaction!!.PaymentMethods().contains(PaymentMethod.Contactless)
                    mTransaction!!.setPaymentMethods(
                        TransactionBuilder.GetPaymentMethods(
                            false,
                            true,
                            contactless,
                            false
                        )
                    )
                    val device = device
                    if (device != null) {
                        if (device.startTransaction(mTransaction)) {
                            sendToOutput("[Chip Transaction Started]")
                            sendToOutput("Amount=" + mTransaction!!.Amount() + ", Timeout=" + mTransaction!!.Timeout() + ", Transaction Type=" + mTransaction!!.TransactionType())
                        }
                    }
                } catch (ex: Exception) {
                }
            }
        }
        wsThread.start()
    }

    override fun OnUseMSR() {
        sendToOutput("Display Message: USE MAGSTRIPE")
        displayMessage(0x12.toByte())
        val wsThread: Thread = object : Thread() {
            override fun run() {
                try {
                    sleep(3000)
                    mTransaction!!.setPaymentMethods(
                        TransactionBuilder.GetPaymentMethods(
                            true,
                            false,
                            false,
                            false
                        )
                    )
                    mTransaction!!.setPreventMSRSignatureForCardWithICC(false)
                    val device = device
                    if (device != null) {
                        if (device.startTransaction(mTransaction)) {
                            sendToOutput("[MSR Transaction Started]")
                            sendToOutput("Amount=" + mTransaction!!.Amount() + ", Timeout=" + mTransaction!!.Timeout() + ", Transaction Type=" + mTransaction!!.TransactionType())
                        }
                    }
                } catch (ex: Exception) {
                }
            }
        }
        wsThread.start()
    }

    override fun OnSignatureCaptureRequested() {
        sendToOutput("Request Signature")
        if (mGetSignatureFromDevice) {
            requestSignature()
        }
    }

    open fun updateSignatureView(data: ByteArray?) {
        val w = 255 * 3
        val h = 128 * 3
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val blackPaint = Paint()
        blackPaint.color = Color.BLACK
        blackPaint.style = Paint.Style.STROKE
        blackPaint.strokeWidth = 4.0.toFloat()
        blackPaint.isAntiAlias = true
        blackPaint.isDither = true
        blackPaint.strokeJoin = Paint.Join.ROUND
        blackPaint.strokeCap = Paint.Cap.ROUND
        val redPaint = Paint()
        redPaint.color = Color.RED
        redPaint.style = Paint.Style.FILL
        //redPaint.setStrokeWidth((float) 8.0);
        val whitePaint = Paint()
        whitePaint.color = Color.WHITE
        whitePaint.style = Paint.Style.FILL

        //Paint bitmapPaint = new Paint(Paint.DITHER_FLAG);

        //canvas.drawBitmap(bitmap, 0, 0, bitmapPaint);
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), redPaint)
        canvas.drawRect(4f, 4f, (w - 4).toFloat(), (h - 4).toFloat(), whitePaint)
        if (data != null) {
            //debugMsg("User Signature Length: " + data.length);
            //debugMsg("User Signature:\n" + TLVParser.getHexString(data));

            //Log.i(TAG, "User Signature:\n" + TLVParser.getHexString(data));
            var penDown = false
            val x1 = 0
            val y1 = 0
            var x2 = 0
            var y2 = 0
            var sx2 = 0
            var sy2 = 0
            val path = Path()
            var i = 0
            while (i < data.size) {
                x2 = (data[i] and 0x000000FF.toByte()) as Int
                y2 = data[i + 1].toInt()
                if (y2 >= 0 && y2 <= 127) {
                    sx2 = x2 * 3
                    sy2 = y2 * 3
                    if (penDown) {
                        path.lineTo(sx2.toFloat(), sy2.toFloat())
                    } else {
                        path.moveTo(sx2.toFloat(), sy2.toFloat())
                    }
                    penDown = true
                } else {
                    if (penDown == true) {
                        canvas.drawPath(path, blackPaint)
                    }
                    path.reset()
                    penDown = false
                }
                i += 2
            }
        }

    }

    companion object {
        private val TAG = SessionManager::class.java.simpleName
    }
}