package com.pays.pos.utils.scanner.helpers

import android.content.Context
import com.zebra.scannercontrol.DCSSDKDefs.*
import com.zebra.scannercontrol.DCSScannerInfo
import com.zebra.scannercontrol.FirmwareUpdateEvent

/**
 * Interface to be implemented by classes which wish to communicate with the SDK
 */
interface ScannerAppEngine {
    /**
     * Interface to notify about the change in scanner list due to (apperance/disappearance) of scanners
     */
    interface IScannerAppEngineDevListDelegate {
        /**
         * Method to notify about the change in scanner list due to (apperance/disappearance) of scanners
         */
        fun scannersListHasBeenUpdated(): Boolean
    }

    /**
     * Interface to notify about the various states of a scanner
     */
    interface IScannerAppEngineDevConnectionsDelegate {
        /**
         * Method to notify about the appearance of a scanner
         * @param scannerID ID of the scanner which has appeared
         * @return -
         */
        fun scannerHasAppeared(scannerID: Int): Boolean

        /**
         * Method to notify about the disappearance of a scanner
         * @param scannerID ID of the scanner which has disappeared
         * @return -
         */
        fun scannerHasDisappeared(scannerID: Int): Boolean

        /**
         * Method to notify about that connection has been established with a scanner
         * @param scannerID ID of the scanner with which a connection was established
         * @return -
         */
        fun scannerHasConnected(scannerID: Int): Boolean

        /**
         * Method to notify about that connection has been terminated with a scanner
         * @param scannerID ID of the connected scanner which was disconnected
         * @return -
         */
        fun scannerHasDisconnected(scannerID: Int): Boolean
    }

    /**
     * Method to notify about the change in scanner list due to (apperance/disappearance) of scanners
     */
    fun scannersListHasBeenUpdated(): Boolean

    /**
     * Interface to notify about events like barcode received etc
     */
    interface IScannerAppEngineDevEventsDelegate {
        fun scannerBarcodeEvent(barcodeData: ByteArray?, barcodeType: Int, scannerID: Int)
        fun scannerFirmwareUpdateEvent(firmwareUpdateEvent: FirmwareUpdateEvent?)
        fun scannerImageEvent(imageData: ByteArray?)
        fun scannerVideoEvent(videoData: ByteArray?)
    }

    /**
     * Method to handle the initialization
     */
    fun initializeDcsSdkWithAppSettings()

    /**
     * Utility function to display a message box(when app is in foreground)
     * @param message - Message to be displayed
     */
    fun showMessageBox(message: String?)

    /**
     * Utility function to display a notification when the app is in background
     * @param text - Notification to be displayed
     * @return -
     */
    fun showBackgroundNotification(text: String?): Int

    /**
     * Utility function to dismiss a background notification
     * @return -
     */
    fun dismissBackgroundNotifications(): Int

    /**
     * Utility function to know if an app is in background or not
     * @return -
     */
    fun isInBackgroundMode(context: Context?): Boolean
    /* API calls for UI View Controllers */
    /**
     * Method to remove a [IScannerAppEngineDevListDelegate] from the list of delegates
     * @param delegate Delegate to be added
     */
    fun addDevListDelegate(delegate: IScannerAppEngineDevListDelegate?)

    /**
     * Method to add a [IScannerAppEngineDevListDelegate] from the list of delegates
     * @param delegate Delegate to be added
     */
    fun addDevConnectionsDelegate(delegate: IScannerAppEngineDevConnectionsDelegate?)

    /**
     * Method to add a [IScannerAppEngineDevListDelegate] from the list of delegates
     * @param delegate Delegate to be added
     */
    fun addDevEventsDelegate(delegate: IScannerAppEngineDevEventsDelegate?)

    /**
     * Method to remove a [IScannerAppEngineDevListDelegate] from the list of delegates
     * @param delegate Delegate to be removed
     */
    fun removeDevListDelegate(delegate: IScannerAppEngineDevListDelegate?)

    /**
     * Method to remove a [IScannerAppEngineDevListDelegate] from the list of delegates
     * @param delegate Delegate to be removed
     */
    fun removeDevConnectiosDelegate(delegate: IScannerAppEngineDevConnectionsDelegate?)

    /**
     * Method to remove a [IScannerAppEngineDevListDelegate] from the list of delegates
     * @param delegate Delegate to be removed
     */
    fun removeDevEventsDelegate(delegate: IScannerAppEngineDevEventsDelegate?)

    /**
     * Method to fetch the list of available scanners
     * @return List of scanners
     */
    fun getActualScannersList(): MutableList<DCSScannerInfo>?

    /**
     * Method to fetch the info about a scanner
     * @param dev_index Index of the scanner in question
     * @return Scanner Info
     */
    fun getScannerInfoByIdx(dev_index: Int): DCSScannerInfo?

    /**
     * Method to fetch the info about a scanner by it's ID
     * @param scannerId ID of the scanner of interest
     * @return Scanner Info
     */
    fun getScannerByID(scannerId: Int): DCSScannerInfo?

    /**
     * Method to raise a notification
     */
    fun raiseDeviceNotificationsIfNeeded()

    /**
     * Method to update the list of scanners from paired devices
     */
    fun updateScannersList()

    /**
     * Method to initiate a connection to a scanner
     * @param scannerId ID of the scanner to which we want to connect
     */
    fun connect(scannerId: Int): DCSSDK_RESULT?

    /**
     * Method to terminate a connection with a scanner
     * @param scannerId ID of the scanner with which the connection should be terminated
     */
    fun disconnect(scannerId: Int)

    /**
     * Method to set the reconnection option for a scanner
     * @param scannerId ID of the scanner of interest
     * @param enable Enable/Disable auto reconnection
     */
    fun setAutoReconnectOption(scannerId: Int, enable: Boolean): DCSSDK_RESULT?

    /**
     * Method to enable/disable discovery of scanners
     * @param enable enable/disable the discovery
     */
    fun enableScannersDetection(enable: Boolean)

    /**
     * Method to enable/disable discovery of Bluetooth scanners
     * @param enable enable/disable the Bluetooth scanner discovery
     */
    fun enableBluetoothScannerDiscovery(enable: Boolean)

    /**
     * Method to enable/disable 'scanner available' notifications
     * @param enable enable/disable the notifications
     */
    fun configureNotificationAvailable(enable: Boolean)

    /**
     * Method to enable/disable 'scanner active' notifications
     * @param enable enable/disable the notifications
     */
    fun configureNotificationActive(enable: Boolean)

    /**
     * Method to enable/disable 'barcode received' notifications
     * @param enable enable/disable the notifications
     */
    fun configureNotificationBarcode(enable: Boolean)

    /**
     * Method to enable/disable 'image received' notifications
     * @param enable enable/disable the notifications
     */
    fun configureNotificationImage(enable: Boolean)

    /**
     * Method to enable/disable 'video received' notifications
     * @param enable enable/disable the notifications
     */
    fun configureNotificationVideo(enable: Boolean)

    /**
     * Method to configure the BT connection mode(if multiple modes are supported)
     * @param mode Mode to be used
     */
    fun configureOperationalMode(mode: DCSSDK_MODE?)

    /**
     * Method to execute command for scanner
     * @param opCode operantional code to be used
     * @param inXML input xml to scanner
     * @param outXML output xml from scanner
     * @param scannerID id of scanner
     */
    fun executeCommand(
        opCode: DCSSDK_COMMAND_OPCODE?,
        inXML: String?,
        outXML: StringBuilder?,
        scannerID: Int
    ): Boolean

    /**
     * Method to execute command for scanner
     * @param opCode operantional code to be used
     * @param inXML input xml to scanner
     * @param outXML output xml from scanner
     * @param scannerID id of scanner
     */
    fun executeSSICommand(
        opCode: DCSSDK_COMMAND_OPCODE?,
        inXML: String?,
        outXML: StringBuilder?,
        scannerID: Int
    ): Boolean
}