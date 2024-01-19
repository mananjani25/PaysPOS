package com.pays.pos.utils.scanner.helpers

import android.util.Log
import com.pays.pos.utils.LogUtil

/**
 * Created by mfv347 on 6/20/2014.
 *
 *
 * Helper class
 */
object Constants {
    const val PREFS_NAME = "BarcodeScannerPrefs"

    //For Debugging
    const val DEBUG = true
    const val COLOR_BG_GRAY = 0XF0F0F0

    //For shared prefs
    const val PREF_OPMODE = "MOT_SETTING_OPMODE"
    const val PREF_SCANNER_DETECTION = "MOT_SETTING_SCANNER_DETECTION"
    const val PREF_SCANNER_DISCOVERY = "MOT_SETTING_SCANNER_DISCOVERY"
    const val PREF_EVENT_ACTIVE = "MOT_SETTING_EVENT_ACTIVE"
    const val PREF_EVENT_AVAILABLE = "MOT_SETTING_EVENT_AVAILABLE"
    const val PREF_EVENT_BARCODE = "MOT_SETTING_EVENT_BARCODE"
    const val PREF_EVENT_IMAGE = "MOT_SETTING_EVENT_IMAGE"
    const val PREF_EVENT_VIDEO = "MOT_SETTING_EVENT_VIDEO"
    const val PREF_EVENT_BINARY_DATA = "MOT_SETTING_EVENT_BINARY_DATA"
    const val PREF_DONT_SHOW_INSTRUCTIONS = "MOT_SETTING_DONT_SHOW_MSG"
    const val PREF_BT_ADDRESS = "MOT_SETTING_BT_ADDRESS"
    const val PREF_NOTIFY_ACTIVE = "MOT_SETTING_NOTIFICATION_ACTIVE"
    const val PREF_NOTIFY_AVAILABLE = "MOT_SETTING_NOTIFICATION_AVAILABLE"
    const val PREF_NOTIFY_BARCODE = "MOT_SETTING_NOTIFICATION_BARCODE"
    const val PREF_NOTIFY_IMAGE = "MOT_SETTING_NOTIFICATION_IMAGE"
    const val PREF_NOTIFY_VIDEO = "MOT_SETTING_NOTIFICATION_VIDEO"
    const val PREF_NOTIFY_BINARY_DATA = "MOT_SETTING_NOTIFICATION_BINARY_DATA"
    const val PREF_PAIRING_BARCODE_TYPE = "MOT_SETTING_PAIRING_BARCODE_TYPE"
    const val PREF_PAIRING_BARCODE_CONFIG = "MOT_SETTING_PAIRING_BARCODE_CONFIG"
    const val PREF_COMMUNICATION_PROTOCOL_TYPE = "MOT_SETTING_COMMUNICATION_PROTOCOL_TYPE"

    //Data related to notifications
    const val NOTIFICATIONS_TYPE = "notifications_type"
    const val NOTIFICATIONS_TEXT = "notifications_text"
    const val NOTIFICATIONS_ID = "notifications_id"

    //Action strings for various RFID Events
    const val ACTION_SCANNER_CONNECTED = "com.pays.pos.connected"
    const val ACTION_SCANNER_DISCONNECTED = "com.pays.pos.disconnected"
    const val ACTION_SCANNER_AVAILABLE = "com.pays.pos.available"
    const val ACTION_SCANNER_CONN_FAILED = "com.pays.pos.conn.failed"
    const val ACTION_SCANNER_BARCODE_RECEIVED = "com.pays.pos.barcode.received"
    const val ACTION_SCANNER_IMAGE_RECEIVED = "com.pays.pos.image.received"
    const val ACTION_SCANNER_VIDEO_RECEIVED = "com.pays.pos.video.received"

    //Data regarding bluetooth
    const val DATA_BLUETOOTH_DEVICE = "com.zebra.scannercontrol.data.bluetooth.device"

    //Virtual tether
    const val PREF_VIRTUAL_TETHER_SCANNER_SETTINGS = "MOT_VIRTUAL_TETHER_SCANNER_SETTINGS"
    const val PREF_VIRTUAL_TETHER_HOST_FEEDBACK = "MOT_VIRTUAL_TETHER_HOST_FEEDBACK"
    const val PREF_VIRTUAL_TETHER_HOST_VIBRATION_ALARM = "MOT_VIRTUAL_TETHER_HOST_VIBRATION_ALARM"
    const val PREF_VIRTUAL_TETHER_HOST_AUDIO_ALARM = "MOT_VIRTUAL_TETHER_HOST_AUDIO_ALARM"
    const val PREF_VIRTUAL_TETHER_HOST_POPUP_MESSAGE = "MOT_VIRTUAL_TETHER_HOST_POPUP_MESSAGE"
    const val VIRTUAL_TETHER_HOST_BACKGROUND_MODE_NOTIFICATION =
        "Zebra Virtual Tether alarm activated"
    const val PREF_VIRTUAL_TETHER_HOST_SCREEN_FLASH = "MOT_VIRTUAL_TETHER_HOST_SCREEN_FLASH"
    const val VIRTUAL_TETHER_HOST_NOTIFICATION_CHANNEL_ID = 1111
    const val VIRTUAL_TETHER_EVENT_NOTIFY = "intent_virtual_tether_event_notify"
    const val PREF_VIRTUAL_TETHER_HOST_BACKGROUND_COLOR = "backgroundColor"
    const val VIRTUAL_TETHER_HOST_ANIMATION_DURATION = 1000

    //Intent Data
    const val INTENT_ACTION = "intent_action"
    const val INTENT_DATA = "intent_data"

    //Config Details Intent Data
    const val CONFIG_NAME = "intent_config_name"
    const val CONFIG_DESC = "intent_config_desc"
    const val CONFIG_VALUE = "intent_config_value"
    const val CONFIG_TITLE = "intent_config_title"
    const val CONFIG_MSG = "intent_config_message"
    const val LAUNCH_FROM_FCS = "launch_from_fcs"

    //Available Scanners Data
    const val SCANNER_NAME = "avail_scanner_name"
    const val SCANNER_TYPE = "avail_scanner_type"
    const val SCANNER_ADDRESS = "avail_scanner_address"
    const val SCANNER_ID = "active_scanner_id"
    const val SELECTED_BARCODE_SSA = "selected_barcode_ssa"
    const val AUTO_RECONNECTION = "auto_reconnection"
    const val PICKLIST_MODE = "picklist+mode"
    const val PAGER_MOTOR_STATUS = "pager_motor_status"
    const val CONNECTED = "connected"
    const val SHOW_BARCODE_VIEW = "barcode_view"
    const val FW_REBOOT = "fw_reboot"
    const val BATTERY_STATUS = "battery_status"
    const val SYMBOLOGY_SSA = "symbology_ssa"
    const val SYMBOLOGY_SSA_ENABLED = "symbology_ssa_enabled"
    const val SYMBOLOGY_SSA_UNSUPPORTED = "symbology_ssa_unsupported"
    const val SYMBOLOGY_SSA_STR = "symbology_ssa_str"
    const val BEEPER_VOLUME = "beeper_volume"
    const val SSA_STATUS = "ssa_status"
    const val SCALE_STATUS = "scale_status"
    const val UNPAIR_AND_REBOOT_STATUS = "unpair_and_reboot_status"
    const val CONNECTION_HELP_TYPE = "connection_help"
    const val CONNECTION_HELP_TYPE_CS4070 = 0
    const val CONNECTION_HELP_TYPE_LI4278 = 1
    const val CONNECTION_HELP_TYPE_RFD8500 = 2
    const val CONNECTION_HELP_CS4070_RESET_DEFAULTS = "Reset Factory Defaults"
    const val CONNECTION_HELP_CS4070_SSI_PROFILE = "Bluetooth SSI Profile"
    const val CONNECTION_HELP_LI4278_SET_DEFAULTS = "Set Factory Defaults"
    const val CONNECTION_HELP_LI4278_SSI_HOST_SERVER = "SSI Host Server"

    //Error Messages
    const val INVALID_SCANNER_ID_MSG = "Invalid Scanner ID"

    //Type of data recieved
    const val BARCODE_RECEIVED = 30
    const val SESSION_ESTABLISHED = 31
    const val SESSION_TERMINATED = 32
    const val SCANNER_APPEARED = 33
    const val SCANNER_DISAPPEARED = 34
    const val FW_UPDATE_EVENT = 35
    const val AUX_SCANNER_CONNECTED = 36
    const val IMAGE_RECEIVED = 37
    const val VIDEO_RECEIVED = 38

    ///---
    const val BTH_SCAN_TO_CONNECT = "[BTH_CONNECT]"

    //Xml tags
    const val XMLTAG_SCANNER_ID = "<scannerID>"
    const val XMLTAG_ARGXML = "<inArgs>"

    //Weight status
    const val WEIGHT_XML_ELEMENT = "weight"
    const val WEIGHT_MODE_XML_ELEMENT = "weight_mode"
    const val WEIGHT_STATUS_XML_ELEMENT = "status"
    const val SCALE_STATUS_SCALE_NOT_ENABLED = "Scale Not Enabled"
    const val SCALE_STATUS_SCALE_NOT_READY = "Scale Not Ready"
    const val SCALE_STATUS_STABLE_WEIGHT_OVER_LIMIT = "Stable Weight OverLimit"
    const val SCALE_STATUS_STABLE_WEIGHT_UNDER_ZERO = "Stable Weight Under Zero"
    const val SCALE_STATUS_NON_STABLE_WEIGHT = "Non Stable Weight"
    const val SCALE_STATUS_STABLE_ZERO_WEIGHT = "Stable Zero Weight"
    const val SCALE_STATUS_STABLE_NON_ZERO_WEIGHT = "Stable NonZero Weight"

    //Scanner models
    const val SCANNER_MODEL_CS4070 = "CS4070"

    /**
     * Method to be used throughout the app for logging debug messages
     *
     * @param type    - One of TYPE_ERROR or TYPE_DEBUG
     * @param TAG     - Simple String indicating the origin of the message
     * @param message - Message to be logged
     */
    fun logAsMessage(type: DEBUG_TYPE, TAG: String?, message: String?) {
        if (DEBUG) {
            if (type == DEBUG_TYPE.TYPE_DEBUG) Log.d(
                TAG,
                message!!
            ) else if (type == DEBUG_TYPE.TYPE_ERROR) TAG?.let { LogUtil.logE(it, message!!) }
        }
    }

    enum class DEBUG_TYPE {
        TYPE_DEBUG, TYPE_ERROR
    }
}