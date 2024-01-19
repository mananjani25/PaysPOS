package com.pays.pos.ui.fragments.magtekPro

import android.os.Looper
import com.magtek.mobile.android.mtusdk.*
import java.lang.Exception
import java.util.HashMap
import kotlin.experimental.and

open class FallbackManager(
    protected var mFallbackAdapter: IFallbackAdapter?,
    protected var mTransaction: ITransaction
) {
    private var mCardInserted = false
    private var mChipDetected = false
    private var mChipFailureCount = 0
    private var mPendingOnUseChipReader = false
    private var mPendingOnUseMSR = false
    private var mFallbackToMSR = false
    private fun getTLVPayload(data: ByteArray?): ByteArray? {
        var payload: ByteArray? = null
        if (data != null) {
            val dataLen = data.size
            if (dataLen > 2) {
                val tlvLen =
                    (data[0] and 0x000000FF.toByte() shl (8)) + (data[1] and 0x000000FF.toByte()).toInt()
                payload = ByteArray(tlvLen)
                System.arraycopy(data, 2, payload, 0, tlvLen)
            }
        }
        return payload
    }

    private fun sendCallbackOnUseChipReader() {
        val wsThread: Thread = object : Thread() {
            override fun run() {
                Looper.prepare()
                try {
                    sleep(1000)
                    if (mFallbackAdapter != null) {
                        mFallbackAdapter!!.OnUseChipReader()
                    }
                } catch (ex: Exception) {
                }
            }
        }
        wsThread.start()
    }

    private fun sendCallbackOnUseMSR() {
        val wsThread: Thread = object : Thread() {
            override fun run() {
                Looper.prepare()
                try {
                    sleep(1000)
                    if (mFallbackAdapter != null) {
                        mFallbackAdapter!!.OnUseMSR()
                    }
                } catch (ex: Exception) {
                }
            }
        }
        wsThread.start()
    }

    private fun sendCallbackOnSignatureCaptureRequested() {
        val wsThread: Thread = object : Thread() {
            override fun run() {
                Looper.prepare()
                try {
                    sleep(1000)
                    if (mFallbackAdapter != null) {
                        mFallbackAdapter!!.OnSignatureCaptureRequested()
                    }
                } catch (ex: Exception) {
                }
            }
        }
        wsThread.start()
    }

    fun OnEvent(eventType: EventType?, data: IData?) {
        when (eventType) {
            EventType.CardData -> {
            }
            EventType.TransactionStatus -> {
                val status = TransactionStatusBuilder.GetStatusCode(data!!.StringValue())
                if (status == TransactionStatus.CardSwiped) {
                } else if (status == TransactionStatus.CardInserted) {
                    mCardInserted = true
                } else if (status == TransactionStatus.CardRemoved) {
                    mCardInserted = false
                    if (mPendingOnUseChipReader) {
                        mPendingOnUseChipReader = false
                        sendCallbackOnUseChipReader()
                    } else if (mPendingOnUseMSR) {
                        mPendingOnUseMSR = false
                        sendCallbackOnUseMSR()
                    }
                } else if (status == TransactionStatus.CardDetected) {
                } else if (status == TransactionStatus.CardCollision) {
                } else if (status == TransactionStatus.TimedOut) {
                } else if (status == TransactionStatus.HostCancelled) {
                } else if (status == TransactionStatus.TransactionCancelled) {
                } else if (status == TransactionStatus.TransactionInProgress) {
                } else if (status == TransactionStatus.TransactionError) {
                } else if (status == TransactionStatus.TransactionCompleted) {
                    if (mChipDetected) {
                        if (mChipFailureCount < 3) {
                            if (mCardInserted) {
                                mPendingOnUseChipReader = true
                            } else {
                                sendCallbackOnUseChipReader()
                            }
                        }
                    }
                } else if (status == TransactionStatus.TransactionApproved) {
                } else if (status == TransactionStatus.TransactionDeclined) {
                } else if (status == TransactionStatus.TransactionFailed) {
                } else if (status == TransactionStatus.TransactionNotAccepted) {
                } else if (status == TransactionStatus.TechnicalFallback) {
                    mChipFailureCount++
                    if (mChipFailureCount < 3) {
                        if (mCardInserted) {
                            mPendingOnUseChipReader = true
                        } else {
                            sendCallbackOnUseChipReader()
                        }
                    } else {
                        mFallbackToMSR = true
                        if (mCardInserted) {
                            mPendingOnUseMSR = true
                        } else {
                            sendCallbackOnUseMSR()
                        }
                    }
                } else if (status == TransactionStatus.SignatureCaptureRequested) {
                    if (mChipDetected && !mFallbackToMSR) {
                        return
                    }
                    sendCallbackOnSignatureCaptureRequested()
                }
            }
            EventType.AuthorizationRequest -> if (data != null) {
                val parsedTLVList: List<HashMap<String, String>> =
                    MTParser.parseTLV(getTLVPayload(data.ByteArray()))
                val cardType: ByteArray? = MTParser.getTagByteArrayValue(parsedTLVList, "DFDF52")
                if (cardType != null && cardType.isNotEmpty()) {
                    if (cardType[0] == 0x07.toByte()) {
                        mChipDetected = true
                    }
                }
            }
            EventType.TransactionResult -> {
            }
        }
    }
}

infix fun Byte.shl(that: Int): Int = this.toInt().shl(that)
