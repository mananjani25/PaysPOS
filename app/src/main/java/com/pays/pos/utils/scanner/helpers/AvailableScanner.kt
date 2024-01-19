package com.pays.pos.utils.scanner.helpers

import com.zebra.scannercontrol.DCSSDKDefs.DCSSDK_CONN_TYPES
import com.pays.pos.utils.scanner.helpers.AvailableScanner
import com.zebra.scannercontrol.DCSScannerInfo

/**
 * Class to encapsulate scanner data and connected info
 */
class AvailableScanner(
    var scannerId: Int,
    var scannerName: String?,
    var scannerAddress: String,
    var isConnected: Boolean,
    var isAutoReconnection: Boolean,
    var connectionType: DCSSDK_CONN_TYPES
) : Comparable<AvailableScanner> {
    var isConnectable = false

    constructor(activeScanner: DCSScannerInfo) : this(
        activeScanner.scannerID,
        activeScanner.scannerName,
        activeScanner.scannerHWSerialNumber,
        activeScanner.isActive,
        activeScanner.isAutoCommunicationSessionReestablishment,
        activeScanner.connectionType
    ) {
    }

    override fun equals(obj: Any?): Boolean {
        if (obj == null) {
            return false
        }
        if (javaClass != obj.javaClass) {
            return false
        }
        val other = obj as AvailableScanner
        return !if (scannerName == null) other.scannerName != null else scannerName != other.scannerName
    }

    override fun hashCode(): Int {
        var hash = 3
        hash =
            53 * hash + (if (scannerName != null) scannerName.hashCode() else 0) + if (scannerName != null) scannerName.hashCode() else 0
        return hash
    }

    override fun compareTo(availableScanner: AvailableScanner): Int {
        return this.toString().compareTo(availableScanner.toString())
    }

    override fun toString(): String {
        return """
            $scannerName
            $scannerAddress
            """.trimIndent()
    }
}