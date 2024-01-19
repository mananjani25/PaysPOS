package com.pays.pos.utils.scanner.helpers

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
 * Class to encapsulate Symbology Info
 */
class Symbology(var symbologyName: String?, var rmdAttributeID: Int) {
    var isEnabled = false
    var isSupported = false
    override fun equals(obj: Any?): Boolean {
        if (obj == null) {
            return false
        }
        if (javaClass != obj.javaClass) {
            return false
        }
        val other = obj as Symbology
        return !if (symbologyName == null) other.symbologyName != null else symbologyName != other.symbologyName
    }

    override fun hashCode(): Int {
        var hash = 3
        hash = 53 * hash + if (symbologyName != null) symbologyName.hashCode() else 0
        return hash
    }

    override fun toString(): String {
        return """
            $symbologyName
            $rmdAttributeID
            """.trimIndent()
    }
}