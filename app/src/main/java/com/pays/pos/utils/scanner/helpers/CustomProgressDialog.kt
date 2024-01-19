package com.pays.pos.utils.scanner.helpers

import android.app.ProgressDialog
import android.content.Context
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

/**
 * Custom Dialog to be shown when sending commands to the RFID Reader
 */
class CustomProgressDialog(context: Context?, message: String?) :
    ProgressDialog(context, STYLE_SPINNER) {
    companion object {
        private const val MESSAGE = "Saving Settings...."
    }

    /**
     * Constructor to handle the initialization
     * @param context - Context to be used
     */
    init {
        setTitle(null)
        if (message != null) setMessage(message) else setMessage(MESSAGE)
        setCancelable(true)
    }
}