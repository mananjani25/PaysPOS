package com.pays.pos.utils.scanner.helpers

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.scanner.helpers.CustomProgressDialog
import com.pays.pos.utils.scanner.helpers.Foreground
import com.pays.pos.utils.scanner.helpers.ManagedVibrator
import com.zebra.scannercontrol.FirmwareUpdateEvent
import com.pays.pos.utils.scanner.helpers.ScannerAppEngine.IScannerAppEngineDevListDelegate
import com.pays.pos.utils.scanner.helpers.ScannerAppEngine.IScannerAppEngineDevConnectionsDelegate
import com.pays.pos.utils.scanner.helpers.ScannerAppEngine.IScannerAppEngineDevEventsDelegate
import com.zebra.scannercontrol.DCSScannerInfo
import com.zebra.scannercontrol.DCSSDKDefs.DCSSDK_RESULT
import com.zebra.scannercontrol.DCSSDKDefs.DCSSDK_MODE
import com.zebra.scannercontrol.DCSSDKDefs.DCSSDK_COMMAND_OPCODE
import com.pays.pos.utils.scanner.helpers.SSASymbologyType
import com.zebra.scannercontrol.RMDAttributes
import com.pays.pos.utils.scanner.helpers.Symbology
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Wrapper class to handle the vibrator when virtual tether event occurs
 */
class ManagedVibrator(private val mContext: Context) {
    private val mVibrator: Vibrator
    var isVibrating = false
        private set
    private val mExecutor: ScheduledThreadPoolExecutor
    private val mVibrationEndRunnable: Runnable = object : Runnable {
        override fun run() {
            isVibrating = false
        }
    }

    fun hasVibrator(): Boolean {
        return mVibrator.hasVibrator()
    }

    fun vibrate(milliseconds: Long) {
        isVibrating = true
        mVibrator.vibrate(milliseconds)
        notifyOnVibrationEnd(milliseconds)
    }

    fun vibrate(pattern: LongArray?, repeat: Int) {
        isVibrating = true
        mVibrator.vibrate(pattern, repeat)
    }

    // Requires API v21
    fun vibrate(pattern: LongArray?, repeat: Int, attributes: AudioAttributes?) {
        isVibrating = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mVibrator.vibrate(
                VibrationEffect.createWaveform(pattern, repeat),
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
            )
        } else {
            mVibrator.vibrate(pattern, 0)
        }
    }

    fun cancel() {
        mVibrator.cancel()
        isVibrating = false
    }

    private fun notifyOnVibrationEnd(milliseconds: Long) {
        try {
            mExecutor.schedule(mVibrationEndRunnable, milliseconds, TimeUnit.MILLISECONDS)
        } catch (e: RejectedExecutionException) {
            LogUtil.logE(TAG, e.message!!)
        }
    }

    companion object {
        val TAG = ManagedVibrator::class.java.simpleName
    }

    init {
        mVibrator = mContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        mExecutor = ScheduledThreadPoolExecutor(1)
    }
}