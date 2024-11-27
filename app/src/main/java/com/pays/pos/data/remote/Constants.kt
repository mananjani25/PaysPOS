package com.pays.pos.data.remote

import android.content.Context
import android.util.Log
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.gson.Gson
import com.pays.pos.data.model.PrinterListModel
import com.pays.pos.data.model.requestModel.CreatePrinterRequestModel
import com.pays.pos.data.model.responseModel.PrinterResponse
import com.pays.pos.di.PrefProvider
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.TimeFormatUtils
import com.sunmi.externalprinterlibrary2.printer.CloudPrinter
import com.sunmi.externalprinterlibrary2.printer.CloudPrinterBuilder
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutionException

object Constants {

    const val isPaxInDebugMode: Boolean = false
    const val paxLive="https://secure.epx.com/"
    const val paxDebug="https://secure.epxuap.com/"

    const val DO_PRINT: String = "DO_PRINT"
    const val DO_PRINT_CUSTOM: String = "DO_PRINT_CUSTOM"
    const val OLD_ITEM: String = "OLD_ITEM"
    const val OLD_ITEM_BASE: String = "OLD_ITEM_BASE"

    /**
     * This will hold old Item list when "UPDATE" from all orders is clicked
     * **/
    const val OLD_ITEM_BASE_CUSTOM_ITEM: String = "OLD_ITEM_BASE_ITEM"
    const val START_DATE = "start_date"
    const val END_DATE = "end_date"


    val IS_ORDER_LAST_PAYMENT = "is_order_last_payment"
    const val ARG_PARAM1 = "param1"
    const val ARG_PARAM2 = "param2"
    const val ARG_PARAM3 = "param2"
    const val PRINTER_QUEUE_BACKGROUND = "printer_queue_background"

    const val CURRENT_VERSION = 1

    const val KITCHEN_PRINTER_LIST_PREF = "kitchen_printer_list_pref"

    const val IS_MASTER_TERMINAL = "is_master_terminal"
    const val IS_PRINTER_QUEUE_STARTS = "is_printer_queue_starts"

    const val MAX_ITEM_QUANTITY = 1000
    const val MAX_ITEM_QUANTITY_FOR_MANUAL_SALES = 15

    const val SUNMIBRAND = "Sunmi"
    const val STAR = "Star"
    const val EPSONBRAND = "TM"

    const val FILE_PATH = "/storage/emulated/0/Download/"

    // payment magtek

    const val TSYS_PRODUCTION = "TSYS - Production"
    const val TSYS_PILOT = "TSYS - Pilot"

    const val RAPID_PRODUCTION = "Rapid Connect v3 - Production"
    const val RAPID_PILOT = "Rapid Connect v3 - Pilot"

    //Database Name
    const val DATABASE_NAME = "androidPos-db"

    const val QRCODE_STATIC_URL = "https://tgb.possoft.io/receipt/TGTT5M019451"

    const val IS_UPDATE_ITEM = "isUpdateItem"

    const val SERVICECHARGE_TAKEOUT_OPENORDER = "TakeOutAndParkOrder"
    const val SERVICECHARGE_DINEIN_ORDER = "DineIn"
    const val LOCK_SCREEN_TRANSACTION = "lock_screen_after_each_transaction"
    const val DINEIN_FLOORPLAN_SHOW_TABLENAME = "dine_floorplan_show_tablename"
    const val ONLY_SHOW_PRICE_GREATER_THAN_ZERO = "only_show_price_greater_than_zero"
    const val ORDER_NUMBER_STARTING_FROM_ONE = "order_number_starting_from_one"

    const val PRINTER_QUEUE_DATA = "Printer_Queue_Data"
    const val PRINTER_QUEUE_DATA_RECEIVED = "Printer_Queue_Data_Received"
    const val DATA = "Data"
    const val PRITNER_QUEUE_DATA_DELETE = "PRITNER_QUEUE_DATA_DELETE"


    //SharedPref Keys
    const val AUTH_TOKEN = "authToken"
    const val CHECK_QUEUE_CANCEL = "check_queue_cancel"
    const val WORKER_QUEUE_IN_PROGRESS = "worker_queue_in_progress"
    const val TERMINAL_ID = "terminalId"
    const val TERMINAL_NAME = "terminalName"
    const val SYNC_TIME_STAMP = "SyncTimeStamp"
    const val SYNC_SETTING_TIME_STAMP = "SyncSettingTimeStamp"
    const val ONLINE_ORDER_ENABLE = "ONLINE_ORDER_ENABLE"
    const val BASE_URL_NEW = "baseUrlNew"
    const val PASSCODE = "passcode"
    const val CAT_ID_SELECTED = "cate_id_selected"
    const val LOCATION_ID = "locationId"
    const val LOCATION_NAME = "location_name"
    const val EMAIL = "email"
    const val LOGIN_EMAIL = "login_email"
    const val LOGIN_REMEMBER = "login_remember"
    const val LOGIN_PASSWORD = "login_password"
    const val USERNAME = "userName"
    const val PREF_CUSTOMER = "pref_customer"
    const val PREF_ACTIVE_LOYALTY_PROGRAM = "pref_active_loyalty_program"
    const val PREF_CURRENT_SCANNER = "pref_current_scanner"
    const val PREF_CURRENT_SCANNER_ID = "pref_current_scanner_id"
    const val CUSTOMER_NAME = "customer_name"
    const val RECEIPT_CUSTOMER_NAME = "receipt_customer_name"
    const val SALE_CUSTOMER_NAME = "sale_customer_name"
    const val IS_CLOCKOUT = "isClockout"
    const val IS_REFUND = "isRefund"
    const val EMPLOYEE_ID = "employee_id"
    const val EMPLOYEE_NAME = "employee_name"
    const val EMPLOYEE_ROLE = "employee_role"
    const val EMPLOYEE_ROLE_ID = "employee_role_id"
    const val CURRENT_EMPLOYEE_ROLE = "current_employee_role"
    const val QUEUE_SYNC_TIME_STAMP = "queue_sync_time_stamp"
    const val UPDATE = "UPDATE"
    const val DELETE = "DELETE"
    const val PRINT_PAID = "PrintPaid"
    const val PRINT_UNPAID = "PrintUnpaid"
    const val DINE_IN_LIST_EDIT = "DineInListEdit"
    const val ADD = "ADD"
    const val ORDER_TYPE_ID = "order_type_id"
    const val ORDER_TYPE_NAME = "order_type_name"
    const val ORDER_TYPE = "order_type"
    const val MEDIUM = "medium"
    const val SMALL = "small"
    const val LARGE = "large"
    const val PERCENTAGE = "Percentage"
    const val DINE_INGUEST_SELECTED = "dinein_guestselected"
    const val AMOUNT = "Amount"
    const val IS_ORDER_UPDATE = "isOrderUpdate"
    const val OPEN_ORDER_UPDATE_FOR_PRINT = "open_order_update_for_print"
    const val NO_NEED_TO_PRINT = "cart_updated"
    const val BACK_FROM_PAYMENT = "back_from_payment"
    const val BUNDLE_ORDER_ID = "BUNDLE_ORDER_ID"
    const val PAYMENT_ID = "payment_id"
    const val BUNDLE_PAYMENT_ID = "BUNDLE_PAYMENT_ID"
    const val BUNDLE_PAYMENT_OFFLINE_ID = "BUNDLE_PAYMENT_OFFLINE_ID"
    const val BUNDLE_ORDER_OFFLINE_ID = "BUNDLE_ORDER_OFFLINE_ID"
    const val BUNDLE_ISLOYALTYAPPLIED = "BUNDLE_ISLOYALTYAPPLIED"

    const val TAKEOUT = "TakeOut"
    const val DINE_IN = "DineIn"
    const val DINE_IN_SPACE = "Dine In"
    const val PHONE_ORDER = "PhoneOrder"
    const val PHONE_ORDER_ = "Phone Order"
    const val DINE_IN_UPDATE = "DineInUpdate"
    const val DINE_IN_ITEM = "DineInItem"
    const val OPEN_ORDER = "OpenOrder"
    const val OPEN_ORDER_ = "Open Order"
    const val GIFT_CARD = "GiftCard"
    const val GIFT_CARD_AMOUNT_TAB = "GiftCardAmountTab"
    const val INVOICE = "Invoice"
    const val GIFT_CARD_NAME = "Gift Card"
    const val DINE_IN_STATUS = "DineInStatus"
    const val DINE_IN_TABLE_ID = "DineInTableId"
    const val EXTERNAL_PAYMENT = "External"
    const val GIFT_CARD_AT_FIRST = "gidt_card_at_first"
    const val GIFT_CARD_SORT = "gidt_card_sort"
    const val DEFAULT_CATEGORY_SORT = "default_category_sort"

    const val AMOUNT_TYPE = "amountType"
    const val RATE_OR_AMOUNT = "rateAmount"
    const val OPTION_TYPE = "optionType"
    const val CASHDIS_SURCHARGEENABLE = "cashdisurchargeenable"
    const val CASH_DIS_STORED = "cashDisStore"

    const val CASHBOX = "CASHBOX"

    const val PICK_UP = "Pickup"
    const val DELIVERY = "Delivery"
    const val IS_PAX_CONNECTED = "isPAXConnected"
    const val PAX_SERIAL_NO = "SerialNo"
    const val PAX_TERMINAL_ID = "TerminalID"
    const val PAX_IP = "IPAddress"
    const val PAX_PORT = "Port"
    const val BROADPOS_VERSION = "broadPOS_version"

    const val DELIVERY_TYPE = "Delivery_Type"
    const val OPEN_ORDER_ITEMS = "Open_Order_Items"

    /*Added By Rahul */
    const val OPEN_ORDER_ITEMS_OLD = "Open_Order_Items_Old"
    const val OPEN_ORDER_ITEMS_BASE = "OPEN_ORDER_ITEMS_BASE"

    //Api End Points
    const val USERS_LOG_IN = "users/log_in"
    const val EMPLOYEE_CLOCK_IN = "employee_activities/clock_in"
    const val LOGIN_TERMINAL = "users/login_terminal"
    const val EMPLOYEE_LOG_IN = "employee_activities/log_in"
    const val CHECK_PERMISSION_MANAGER = "employees/check_employee_role"
    const val CLOCK_OUT = "employee_activities/clock_out"
    const val LOGOUT = "users/log_out"
    const val FORGOT_PASSWORD = "users/forgot_password"
    const val PAX_DETAILS = "GetDeviceLocalIP"

    const val EMPLOYEES = "employees"
    const val CUSTOMERS = "customers"
    const val CUSTOMERS_SEARCH = "customers/search"
    const val CUSTOMER_SEARCH_BY_ID = "customers/search_by_id"
    const val EMPLOYEES_UPDATE_DELETE = "employees/{id}"
    const val DISCOVERY_INTERVAL = 500
    const val EMPLOYEES_TIMESHEET = "employees/timesheet"
    const val EMPLOYEES_TIMESHEET_DETAILS = "employees/timesheet_details"
    const val ORDER_ID = "orderID"
    const val PRINT_DATA_DINE_IN = "print_data_dine_in"
    const val IS_FIRST_TIME_LOGIN = "is_first_time_login"
    const val DINE_IN_SUBTOTAL = "dine_in_subtotal"
    const val DINE_IN_TAX = "dine_in_tax"
    const val DINE_IN_DISCOUNT = "dine_in_discount"
    const val DINE_IN_SERVICECHARGE = "dine_in_servicecharge"
    const val IS_GUEST_PAYMNET = "is_guest_payment"
    const val GUEST_POSITION = "guest_position"
    const val GUEST_COUNT = "guest_count"
    const val DINE_IN_ADAPTER_LIST = "dine_in_adapter_list"
    const val DINE_IN_GUEST_PAYMENT_DATA = "dine_in_guest_payment_data"
    const val SYNC_VENUE_DATA = "locations/sync_data"
    const val SYNC_VENUE_DETAILS = "locations/setting_data"
    const val ORDER_TYPES = "order_types"
    const val GET_PRINTERS = "printers"
    const val DESTROY_QUEUE = "printer_queues/clear_printer_queue"
    const val DELETE_UPDATE_PRINTER = "printers/{id}"
    const val DELETE_QUEUE_PRINTER = "printer_queues/{id}"
    const val DELETE_ALL_QUEUE_PRINTER = "printer_queues/delete_all"

    const val UPDATE_PRINTER_STATUS = "printers/{id}/update_printer_status"
    const val UPDATE_SERVICECHARGE = "locations/{id}/enable_service_charge"
    const val CREATE_QUEUE_PRINTER = "printer_queues"
    const val CREATE_QUEUE_PRINTER_PHASE3 = "printer_queues/index_v2"
    const val DELETE_QUEUE_ORDER_PHASE3 = "printer_queues/destroy_v2?order_id="
    const val UPDATE_PRITNER_QUEUE_TRACK = "update_printer_queue_track"

    // const val PRINTER_QUEUE_CONNECTION_URL_SNACKPOS = "wss://snackhq.com/cable"
//    const val PRINTER_QUEUE_CONNECTION_URL_SNACKPOS = "wss://hugepos.com/cable"
    const val PRINTER_QUEUE_CONNECTION_URL_SNACKPOS = "wss://pays.app/cable"

    const val INCREASE_ONGOING_ORDER_COUNTER = "locations/increase_ongoing_order_counter"
    const val DECREASE_ONGOING_ORDER_COUNTER = "locations/decrease_ongoing_order_counter"


    const val TAXES = "taxes"
    const val TAX_UPDATE_DELETE = "taxes/{id}"
    const val TAX_ACTIVE = "taxes/{id}/active"

    const val TIPS = "tip_settings"
    const val TIPS_UPDATE_DELETE = "tip_settings/{id}"
    const val TIPS_ACTIVE = "tip_settings/{id}/active"

    const val DISCOUNTS = "discounts"
    const val DISCOUNTS_UPDATE_DELETE = "discounts/{id}"

    const val TEXT_TO_PAY_SPIT = "sms_order_payment_link/{id}"

    const val DISCOUNTS_ACTIVE = "discounts/{id}/active"

    const val DINE_IN_UPDATE_LIST = "dine_in_update_list"

    const val NOTES = "dynamic_notes"
    const val NOTE_UPDATE_DELETE = "dynamic_notes/{id}"
    const val CUSTOMER_UPDATE = "customers/{id}"
    const val NOTES_ACTIVE = "dynamic_notes/{id}/active"

    const val SERVICE_CHARGE = "service_charges"
    const val SERVICE_CHARGE_WHOLE = "service_charges/show_all_service_charges"
    const val SERVICE_CHARGE_UPDATE_DELETE = "service_charges/{id}"
    const val SERVICE_CHARGE_ACTIVE = "service_charges/{id}/active"

    const val LOYALTY_POINT = "loyalty_programs"
    const val LOYALTY_POINT_UPDATE_DELETE = "loyalty_programs/{id}"
    const val LOYALTY_POINT_ACTIVE = "loyalty_programs/{id}/active"

    const val ITEM_UPDATE_DELETE = "items/{id}"
    const val HIDE_ITEM = "items/{id}/active"
    const val ITEMS = "items"
    const val REORDER_ITEM = "items/{id}/reorder"

    const val REORDER_NOTE = "dynamic_notes/{id}/reorder"
    const val REORDER_TIP = "tip_settings/{id}/reorder"

    const val TEAM_ROLES = "team_roles"
    const val TIME_TRACKER_ENABLED = "time_Trackerenable"
    const val TEAM_ROLES_UPDATE_DELETE = "team_roles/{id}"
    const val GET_TEAM_MODULE = "team_roles/modules"


    const val KITCHEN_RECEIPT_SETTINGS = "kitchen_receipts"
    const val KITCHEN_RECEIPT_UPDATE_SEETINGS = "kitchen_receipts/{id}"

    const val CUSTOMER_RECEIPT_SETTINGS = "customer_receipts"
    const val CUSTOMER_RECEIPTS_UPDATE_SETTINGS = "customer_receipts/{id}"

    const val TRANSACTION_LIST = "payments"
    const val TRANSACTION_DETAIL = "TRANSACTION_DETAIL"
    const val REFUND_PAYMENT = "payments/refund"


    const val VERTICAL = "vertical"
    const val HORIZONTAL = "horizontal"

    const val KEY = "key"
    const val ORDER_COMPLETED = "order_completed"
    const val MANUALSALE = "ManualSale"
    const val CUSTOMERDETAILS = "customer_details"
    const val CREATECATEGORY = "create_category"
    const val CREATEMODIFIER = "create_modifier"
    const val CREATEDISCOUNT = "create_discount"
    const val CREATELOYALTY = "create_loyalty"
    const val SETUP_BUSINESS_DETAILS = "setup_business_details"
    const val CREATEOPTION = "create_option"
    const val CREATEITEM = "create_item"
    const val PRINTER = "printer"
    const val SCAN_GUN = "scan_gun"
    const val GUESTPAID = "guest_paid"
    const val MANUAL_SALE_CATEGORY_ID = "manual_sale_category_id"
    const val MANUAL_SALE_ITEM_ID = "manual_sale_item_id"

    const val SETTING_KEY = "setting_key"
    const val CREATE_TAX = "create_tax"
    const val CREATE_TIP = "create_tip"
    const val ORDER_RECEIPTS = "order_receipts"
    const val CREATE_NOTES = "create_notes"
    const val ADD_SERVICE_CHARGE = "add_service_charge"

    const val TEAM_MEMBER = "team_member"
    const val LOYALTY_ADDED = "loyaltyAdded"

    const val DIALOG_KEY = "dialog_key"
    const val DIALOG_KEY_OPTIONS = "dialog_key_options"
    const val DIALOG_KEY_VARIATION_DETAILS = "dialog_key_variation_details"
    const val DIALOG_KEY_ADD_VARIATION_DETAILS = "dialog_key_add_variation_details"
    const val DIALOG_KEY_VARIATION_DETAILS_REMOVE = "dialog_key_variation_details_remove"
    const val DIALOG_KEY_VARIATION_DETAILS_POSITION = "dialog_key_variation_details_position"
    const val DIALOG_KEY_TAX = "dialog_key_tax"
    const val ADD_TAX = "Add Tax To Item Price"
    const val INCLUDE_TAX = "Include Tax in Item Price"
    const val DIALOG_IMAGE_PATH = "dialog_image_path"

    const val CATEGORY_UPDATE_DELETE = "categories/{id}"
    const val HIDE_CATEGORY = "categories/{id}/active"
    const val REORDER_CATEGORY = "categories/{id}/reorder"
    const val CATEGORY = "categories"

    const val IS_PAYMENT_SCREEN = "is_payment_screen"
    const val CHECK_PHYSICAL_CARD_EXIST_OR_NOT = "check_gift_card"


    const val WIFI = "Wifi"
    const val BLUETOOTH = "Bluetooth"
    const val KITCHEN = "Kitchen"
    const val CUSTOMER = "Customer"
    const val KITCHENANDCUSTOMER = "KitchenAndCustomer"
    const val AVAILABLE = "Available"
    const val MERGED = "Merged"


    const val MODIFIER_UPDATE_DELETE = "modifier_sets/{id}"
    const val MODIFIER = "modifier_sets"
    const val REORDER_MODIFIER = "modifier_sets/{id}/reorder"

    const val OPTION_SETS = "option_sets"
    const val OPTION_UPDATE_DELETE = "option_sets/{id}"
    const val REORDER_OPTION_SET = "option_sets/{id}/reorder_option_sets"


    const val ORDERS = "orders"
    const val GIFT_CARDS = "gift_cards"
    const val GIFT_CARD_CHECK_BALANCE = "gift_card_check_balance"
    const val GIFT_CARD_ADD_BALANCE = "gift_card_add_balance"
    const val ORDER_DETAILS = "orders/{id}"
    const val PAYMENT_DETAILS = "payments/{id}"
    const val ORDER_EMAIL_RECEIPT = "orders/send_order_email_receipt"
    const val ORDER_PHONE_RECEIPT = "orders/sms_order_detail"
    const val GIFT_CARD_EMAIL_RECEIPT = "gift_cards/send_gift_card_email_receipt"
    const val GIFT_CARD_PHONE_RECEIPT = "gift_cards/sms_gift_card_detail"
    const val ORDER_ASSIGN_CUSTOMER = "orders/{id}/assign_customer_into_order"
    const val ORDER_PAY_AMOUNT_WISE = "payments/pay_amount_wise"
    const val ORDER_COUNTS = "orders/open_orders_show_count"
    const val PHONE_ORDER_COUNTS = "orders/phone_orders_show_count"
    const val ONLINE_ORDER_COUNTS = "online_ordering_orders/web_orders_count"
    const val ALL_ORDER_COUNTS = "orders/all_orders_count"
    const val ONLINE_ORDER_NOTIFICATION_COUNT = "locations/web_ordering_count"
    const val UPDATE_LOCK_SCREEN_PERMISSION = "locations/update_lock_screen_permission"


    const val ACTIVE_ORDER = "active_order"
    const val UPCOMING_ORDER = "upcoming_order"
    const val COMPLETED_ORDER = "completed_order"
    const val CANCELED_ORDER = "canceled_order"


    const val BUSINESS_UPDATE = "locations/{id}/update_business_detail"


    const val UPDATE_TIP = "orders/{id}/update_tip"
    const val UPDATE_TIP_WITH_SIGNATURE = "payments/update_tip_and_signature"
    const val FIRE_ITEM_TO_KITCHEN = "orders/{id}/update_fire_status_of_items"
    const val PAY_BY_GUEST = "payments/pay_by_guest"

    const val GET_FLOOR_PLAN = "floor_plans"
    const val FLOOR_PLAN_TABLE_DETAILS = "floor_plans/floor_plan_table_details"
    const val AVAILABLE_TRANSFER_TABLE_LIST = "floor_plan_tables/get_available_table_list"
    const val FLOOR_PLAN_STATUS = "floor_plan_tables/{id}/check_employee_table_lock"
    const val MERGE_FLOOR_TABLE = "floor_plan_tables/{id}/merge_floor_plan_tables"
    const val UNMERGE_TABLE = "floor_plan_tables/{id}/unmerge_floor_plan_tables"
    const val TRASNFER_TABLE = "floor_plans/transfer_table"

    const val REPORT_SUMMARY = "reports/report_summary"
    const val REPORT_EOD_SUMMARY = "reports/employee_eod_report"
    const val REPORT_EMPLOYEE_TIP_SUMMARY = "reports/employee_tip_summary"
    const val EMAIL_REPORT_SUMMARY = "reports/email_timesheet"
    const val ORDER_HISTORY = "customers/{id}/customer_order_history"

    const val BUSINESS_NAME = "business_name"
    const val SYSTEM_TIMEZONE = "time_zone"
    const val BUSINESS_ADDRESS = "business_address"
    const val BUSINESS_PHONE_NO = "business_phone_no"
    const val BUSINESS_WEBSITE = "business_website"
    const val IS_PRINTER_QUEUE_ENABLE = "is_printer_queue_enable"
    const val VENUE_LOGO = "venue_logo"
    const val REPORT_START_TIME = "report_start_time"
    const val REPORT_END_TIME = "report_end_time"
    const val VENUE_LOGO_URL = "venue_logo_url"

    const val CUSTOMER_ID = "customer_id"

    const val OCCUPIED = "Occupied"

    const val MERGEDANDOCCUPIED = "MergedAndOccupied"

    const val PENDING = "PENDING"
    const val IN_PROCESS = "IN PROCESS"
    const val COMPLETED = "COMPLETED"

    const val INVENTORY_COUNTS = "items/inventories_count"


    const val TIME_DETAILS = "time_details"

    const val CUSTOMER_SIGN_REQUIRED_ON_CD = "customer_sign_required_on_cd"
    const val SHOW_CASH_CREDIT_PRICE_ON_CUSTOMER_DISPLAY =
        "show_cash_credit_price_on_customer_display"

    const val ALL_ORDER_TAB_POS = 0
    const val OPEN_ORDER_TAB_POS = 1
    const val PHONE_ORDER_TAB_POS = 2
    const val ONLINE_ORDER_TAB_POS = 3
    const val THIRD_PARTY_ORDER_TAB_POS = 4

    const val ALL_ORDER_TAB = "ALL"
    const val OPEN_ORDER_TAB = "OpenOrder"
    const val KIOSK_OPEN_ORDER = "KioskOpenorder"
    const val PHONE_ORDER_TAB = "PhoneOrder"
    const val ONLINE_ORDER_TAB = "OnlineWebOrder"
    const val THIRD_PARTY_ORDER_TAB = "OnlineOrder"
    const val taxListDynamic = "taxListDynamic"
    const val discountType = "discountType"
    const val discountSelectedValue = "discountSelectedValue"
    const val orderNote = "orderNote"
    const val discountPrice = "discountPrice"
    const val isManual = "isManual"
    const val orderNoteOld = "orderNoteOld"
    const val orderNoteNew = "orderNoteNew"
    const val CREATE_CUSTOMER = "CREATE_CUSTOMER"

    const val SUNMI_FRAMEWORK_VERSION = "SUNMI_FRAMEWORK_VERSION"

    const val LINE_BREAK_TAB = "\n \t"

    //PAX
    const val TRANSACTION_SUCCESSED = 100 //transaction success
    const val TRANSACTION_FAILURE = 101 //transaction failure
    const val TRANSACTION_TIMEOOUT = 102 //transaction timeout
    const val TRANSACTION_STATUS = 103 //transaction timeout
    const val MERCHANT_ID = "merchant_id"

    fun createRequestModelForUpdatePritnerType(
        oldList: ArrayList<PrinterResponse.Data.OrderTypes>?,
        adapterList: ArrayList<PrinterResponse.Data.OrderTypes>,
        printerModel: PrinterListModel?
    ): CreatePrinterRequestModel {

        var list: ArrayList<CreatePrinterRequestModel.PrinterSettingsAttributes> = arrayListOf()


        oldList?.forEach {
            it.printerSettings.forEach {

                it.isDestroy = true
                list.add(
                    CreatePrinterRequestModel.PrinterSettingsAttributes(
                        it.id,
                        it.orderTypeId,
                        it.printType,
                        it.manualPrinting,
                        it.autoPrinting,
                        true
                    )
                )

            }
        }



        for (i in 0 until adapterList.size) {
            adapterList.get(i).printerSettings.forEach {
                list.add(
                    CreatePrinterRequestModel.PrinterSettingsAttributes(
                        it.id,
                        it.orderTypeId,
                        it.printType,
                        it.manualPrinting,
                        it.autoPrinting,
                        it.isDestroy
                    )
                )

            }

        }


        val model = CreatePrinterRequestModel(
            id = printerModel?.id,
            name = printerModel?.printerName,
            macAddress = printerModel?.deviceModel?.macAddress,
            modalName = printerModel?.deviceModel?.printerName,
            status = printerModel!!.isActive,
            ip_address = printerModel.deviceModel?.ipAddress,
            receiptPrintType = printerModel.type,
            printer_type = printerModel.connectionType,
            printerSettingsAttributes = list
        )

        return model
    }


    fun createRequestModelForUpdatePrinter(
        model: ArrayList<PrinterResponse.Data.OrderTypes>,
        printerModel: PrinterListModel?
    ): CreatePrinterRequestModel {
        var list: ArrayList<CreatePrinterRequestModel.PrinterSettingsAttributes> = arrayListOf()

        model.forEach { it ->
            for (i in 0 until it.printerSettings.size) {
                Log.e("checkPrinterSettings","printerSettingsID:  ${it.printerSettings.get(i).id}")
                list.add(
                    CreatePrinterRequestModel.PrinterSettingsAttributes(
                        it.printerSettings.get(i).id,
                        it.printerSettings.get(i).orderTypeId,
                        it.printerSettings.get(i).printType,
                        it.printerSettings.get(i).manualPrinting,
                        it.printerSettings.get(i).autoPrinting,
                        it.printerSettings.get(i).isDestroy
                    )
                )

            }
        }

        LogUtil.logE("ListConvert", "listlist:  ${Gson().toJson(list)}")

        val model = CreatePrinterRequestModel(
            id = printerModel?.id,
            name = printerModel?.printerName,
            macAddress = printerModel?.deviceModel?.macAddress,
            modalName = printerModel?.deviceModel?.printerName,
            status = printerModel!!.isActive,
            ip_address = printerModel.deviceModel?.ipAddress,
            receiptPrintType = printerModel.type,
            printer_type = printerModel.connectionType,
            printerSettingsAttributes = list
        )

        return model

    }


    const val OPEN_ORDERS = "orders/open_orders"
    const val PHONE_ORDERS = "orders/phone_orders"
    const val ONLINE_ORDERING = "online_ordering_orders/web_orders"
    const val ACCEPTED_DECLINE_ONLINEORDER = "online_ordering_orders/{id}/accept_order"
    const val UPDATE_ONLINE_ORDER = "online_ordering_orders/{id}"
    const val CASH_EVENTS = "cash_events"
    const val WASTAGE_ITEM = "orders/wastage_item"

    const val UTC_SERVER_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

    const val ALL_ORDERS = "orders/all_orders"

    fun getCurrentTimeFromTimeZone(context: Context, mdate: String): String {
        try {
            val inputFormat = SimpleDateFormat("MMM-dd-yyyy hh:mm:a")

            val outputFormat = SimpleDateFormat("MMM-dd-yyyy hh:mm:a")
            TimeFormatUtils.prefProvider = PrefProvider(context = context)
            outputFormat.timeZone =
                TimeZone.getTimeZone(TimeFormatUtils.prefProvider.getValue(SYSTEM_TIMEZONE, ""))
            val date = inputFormat.parse(mdate)
            val formattedDate = outputFormat.format(date)
            //  val formattedDateFinalDate = outputFormat.parse(formattedDate)
            return formattedDate
        } catch (e: Exception) {
            e.printStackTrace()
//Thu Jul 16 05:23:26 EDT 2020
            val inputFormat = SimpleDateFormat("MMM-dd-yyyy hh:mm:a", Locale.US)
            val outputFormat = SimpleDateFormat("MMM-dd-yyyy")
            val date = inputFormat.parse(mdate)
            val formattedDate = outputFormat.format(date)
            //  val forreceiptModel?.order?.createdAt.toString()mattedDateFinalDate = outputFormat.parse(formattedDate)
            return formattedDate

        }
    }


    fun getReceiptFormatDateFromUTCServer(context: Context, mdate: String): String {

        /* val df = SimpleDateFormat(UTC_SERVER_FORMAT, Locale.ENGLISH)
         df.setTimeZone(TimeZone.getTimeZone("UTC"))
         val date: Date = df.parse(mdate)
         df.setTimeZone(TimeZone.getDefault())
         val dateFormatter = SimpleDateFormat("MMM-dd-yyyy hh:mm:aa")
         return dateFormatter.format(date)*/


        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val outputFormat = SimpleDateFormat("MMM-dd-yyyy hh:mm:a")
            TimeFormatUtils.prefProvider = PrefProvider(context = context)
            outputFormat.timeZone =
                TimeZone.getTimeZone(TimeFormatUtils.prefProvider.getValue(SYSTEM_TIMEZONE, ""))
            val date = inputFormat.parse(mdate)
            val formattedDate = outputFormat.format(date)
            //  val formattedDateFinalDate = outputFormat.parse(formattedDate)
            return formattedDate
        } catch (e: Exception) {
//Thu Jul 16 05:23:26 EDT 2020
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            val outputFormat = SimpleDateFormat("MMM-dd-yyyy")
            val date = inputFormat.parse(mdate)
            val formattedDate = outputFormat.format(date)
            //  val formattedDateFinalDate = outputFormat.parse(formattedDate)
            return formattedDate

        }


    }

    /*file related constants*/
    const val FILE_NAME_VIDEO = "VID_CAPTURE_"
    const val FILE_NAME_IMG = "IMG_CAPTURE_"
    const val FILE_CAPTURE = "CAPTURE_"
    const val FOLDER_CAPTURE = "Android POS/CapturedFiles"
    const val MEDIA_TYPE_IMAGE = 111
    const val MEDIA_TYPE_VIDEO = 222
    const val EXTENSION_CAMERA_IMAGE_TEMP_IMG = "jpg"
    const val EXTENSION_CAMERA_VIDEO_TEMP_IMG = "mp4"

    //request codes
    var REQUEST_GET_IMAGE_GALLERY = 1
    var REQUEST_GET_IMAGE_CAMERA = 2
    val REQUEST_LOCATION_PERMISSION = 3


    var SPLIT_PAY_TYPE = "split_pay_type"
    var SPLIT_PAY_AMOUNT = "split_pay_amount"
    var SPLIT_NO = "split_no"


    var SPLIT_PAY_TYPE_DINE_IN = "split_pay_type_dine_in"
    var SPLIT_PAY_AMOUNT_DINE_IN = "split_pay_amount_dine_in"
    var SPLIT_NO_DINE_IN = "split_no_dine_in"


    var SPLIT_ENABLE = "split_enable"

    //api constants
    const val EMP_NAME = "Employee Name"
    const val AMT_BY_CASH = "Amount by Cash"
    const val SERVICE_CHARGE_BY_CASH = "Service Charge by Cash"
    const val TIP_BY_CASH = "Tip by Cash"
    const val REFUND = "Refund"
    const val TIP = "Tips"
    const val DISCOUNT = "Discounts"
    const val AMT_COLLECTED = "Amount Collected"
    const val SYNC_DATA = "SyncData"

    //date format
    const val DateFormat_yyyy_MM_dd_T_HH_mm_ss_SSSZ = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    const val DateFormat_hh_mm_a_MM_dd_yyyy = "hh:mm a | MM/dd/yyyy"
    const val DateFormat_MMM_dd_yyyy = "MMM-dd-yyyy"
    const val DateFormat_hh_mm_a = "hh:mm a"


    const val DINE_IN_SPLIT_FLAG = "dine_in_split_flag"

    const val UNIQUE_ID = "unique_id"

    //user role
    const val ROLE_MANAGER = "Manager"
    const val ROLE_OWNER = "Owner"
    const val ROLE_EMPLOYEE = "Employee"
    const val ROLE_ADMIN = "Admin"

    //hardware
    const val HARDWARE_PRINTER = "Printer"
    const val HARDWARE_CREDIT_CARD_MACHINE = "Credit Card Machine"
    const val HARDWARE_SCAN_GUN = "Scan Gun"
    const val HARDWARE_TERMINAL = "Terminal"
    const val HARDWARE_KITCHEN_DISPLAY = "Kitchen Display"
    const val HARDWARE_PRINTER_QUEUE = "Printer Queue"

    //fragment keys
    const val FRAGMENT_HARDWARE = 1
    const val FRAGMENT_SCANNER_LIST = 2
    const val FRAGMENT_RESET_SCANNER = 3

    //takeout_openorder
    const val CASH_DISCOUNT_SURCHARGE = "cashDiscountSurcharge"
    const val SUB_TOTAL = "subtotal"
    const val TAX_CHARGE = "taxcharge"
    const val TIPS_AMOUNT = "tips"
    const val TOTAL_DISCOUNT = "totalDiscount"
    const val IS_NEXT_AMOUNT = "is_next_amount"
    const val SAVE_SPLIT_BUNDLE = "save_split_bundle"
    const val WHOLE_AMOUNT = "whole_amount"


    const val CASH_DISCOUNT_SURCHARGE_ACTUAL = "cashDiscountSurchargeactual"
    const val SUB_TOTAL_ACTUAL = "subtotalactual"
    const val TAX_CHARGE_ACTUAL = "taxchargeactual"
    const val TIPS_AMOUNT_ACTUAL = "tipsactual"
    const val TOTAL_DISCOUNT_ACTUAL = "totalDiscountactual"
    const val TOTAL_PRICE_ACTUAL = "totalPriceActual"
    const val TOTAL_SERVICE_CHARGE_ACTUAL = "totalServiceChargeactual"

    // broadcast
    const val SEND_CLOCKOUT_NOTIFICATION = "send_clockout_notification"
    const val ONLINE_ORDER_GET_NOTIFICATION = "online_order_get_notification"
    const val ONLINE_ORDER_REFRESH = "online_order_refresh"
    const val SYNC_NOTIFICATION = "sync_notification"
    const val SYNC_FLOORPLAN = "sync_floorplan"
    const val SYNC_SETTING_NOTIFICATION = "sync_setting_notification"
    const val SYNC_MARKUP = "MarkupSync"
    const val IS_SYNC_MARKUP = "is_markup_sync"
    const val MASTER_TEMINAL_CHANGED = "master_teminal_changed"


    // dinein
    const val CASH_DISCOUNT_SURCHARGE_DINEIN = "cashDiscountSurcharge_dinein"
    const val SUB_TOTAL_DINEIN = "subtotal_dinein"
    const val TAX_CHARGE_DINEIN = "taxcharge_dinein"
    const val TIPS_AMOUNT_DINEIN = "tips_dinein"
    const val TOTAL_DISCOUNT_DINEIN = "totalDiscount_dinein"
    const val SERVICE_CHARGE_DINEIN = "servicecharge_dinein"
    const val TOTAL_PRICE_DINEIN = "totalprice_dinein"
    const val LAYOUT_ORIENTATION = "layout_orientation"


    const val MAGENSA_SETTINGS = "magensaSettings"
    const val MAGENSA_SETTINGS1 = "magensaSettings1"

    const val SHIFT_REPORT_SETTINGS = "shift_report_setting"

    const val ELAVON_GATEWAY = "Elavon"
    const val FIRST_DATA_GATEWAY = "Rapid Connect" //(First Data Nashville/Omaha/North)
    const val CHASE_GATEWAY = "Chase" // (Orbital)
    const val EPX_GATEWAY = "EPX"
    const val HEARTLAND_GATEWAY = "Heartland"
    const val TSYS_GATEWAY = "TSYS" //(MultiPass)
    const val VANIT_EXORESS_GATEWAY = "Vantiv Express" //(WorldPay)

    const val SALE = 1
    const val AUTHORIZE = 2
    const val CAPTURE = 3
    const val VOID = 4
    const val REFUND1 = 5
    const val FORCE = 6
    const val REJECT = 7

    const val MAGTEK_HARDWARE = "MegtekHardware"

    const val REDIRECT_FROM = "redirect_from"
    const val MANUAL_SALE = "manual_sale"

    const val DYNANA_FLAX = "DYNAMO_FLAX"

    const val CASH_DISCOUNT_SURCHARGE_AMOUNT_TYPE = "cashDiscountSurchargeAmountType"
    const val CASH_DISCOUNT_SURCHARGE_RATE = "cashDiscountSurchargeRate"

    const val SERVER_ORDER_ID = "server_order_id"

    const val IS_UPDATE_ORDER = "is_update_order"
    const val IS_UPDATE_ORDER_ID = "is_update_order_id"
    const val PAYMENT_ID_FOR_CUSTOMER_DISPLAY = "payment_id_for_customer_display"
    const val IS_UPDATE_ORDER_PAYMENT_ID = "is_update_order_payment_id"
    const val IS_UPDATE_ORDER_PAY_OFFLINE_ID = "is_update_order_pay_offline_id"
    const val IS_UPDATE_ORDER_OFFLINE_ID = "is_update_order_offline_id"
    const val IS_UPDATE_ORDER_FROM_ACTIVE_ORDER = "is_update_order_from_active_order"
    const val IS_UPDATE_ORDER_LOYALTY_APPLIED = "is_update_order_loyalty_applied"

    const val SPLIT_DINEIN_MODEL = "split_dinein_model"
    const val SPLIT_IS_GUESTPAY = "split_is_guestpay"
    const val SPLIT_DINEIN_CHECKOUT = "split_dinein_checkout"


    const val SUNMI_INNER_PRINTER = "InnerPrinter"
    const val SUNMI_PRINTER = "CloudPrint"

    const val LANDI_INNER_PRINTER = "Inner Printer"

    const val SHIPPING_ADDRESS = "Shipping"
    const val BILLING_ADDRESS = "Billing"


    const val TIP_ADDED = "TipAdded"
    const val TIP_ADDED_AMOUNT = "TipAddedAmount"
    const val TIP_ADDED_ID = "TipAddedId"

    const val OPEN_ORDER_DIRECT_PAY = "open_order_direct_pay"
    const val DEFAULT_ORDER = "Take Out"

    const val IS_FROM_ALL_ORDER = "IS_FROM_ALL_ORDER"

    const val GIFT_CARD_TYPE = "gift_card_type"
    const val GIFT_CARD_PURCHASE_AMOUNT = "gift_card_purchase_amount"
    const val IS_GIFT_CARD_REDEEM = "is_gift_card_redeem"
    const val IS_ORDER_REDEEMABLE_WITH_GIFT_CARD = "is_order_redeemable_with_gift_card"
    const val GIFT_CARD_NUMBER = "gift_card_number"
    const val PHYSICAL_GIFT_CARD_NUMBER = "physical_gift_card_number"
    const val GIFT_CARD_PIN = "gift_card_pin"
    const val IS_ADD_VALUE_IN_GIFT_CARD = "is_add_value_in_gift_card"

    const val SELL_CARD = "Sell Card"
    const val ADD_VALUE = "Add Value"
    const val BALANCE_INQUIRY = "Balance Inquiry"
    const val GIFT_CARD_CATEGORY = "GIFT CARD"
    const val DEFAULT_CATEGORY = "Default Category"
    const val IS_LAST_ITEM_DELETE = "IS_LAST_ITEM_DELETE"

    const val SURCHARGE_TEXT = "Surcharge"
    const val IS_PAX_PAYMENT_FAILED = "isPaxPaymentFailed"
    const val INVENTORY_SYNC = "INVENTORY_SYNC"
    const val UPDATED_CARTMODEL_ID = "updated_cartmodel_id"


    const val PERMISSION_BLUETOOTH = 1
    const val PERMISSION_BLUETOOTH_ADMIN = 2
    const val PERMISSION_BLUETOOTH_CONNECT = 3
    const val PERMISSION_BLUETOOTH_SCAN = 4

    const val SOAP_ACTION = "https://www.sc-solutions.com/SmartTrackSE/RequestGateway/AuthenticateAndAuthorizeTransaction"
    const val NAMESPACE = "https://www.sc-solutions.com/SmartTrackSE/RequestGateway"
    const val ENDPOINT_URL = "https://demo.ecardsystems.net/requestgatewaydemo/requestgatewaywebservice.asmx"

    fun createCloudPrinter(ipAddress: String, portNo: Int): CloudPrinter {
        return CloudPrinterBuilder.buildPrinter(ipAddress, portNo)
    }

    fun createCloudPrinterWithName(name: String, ipAddress: String, portNo: Int): CloudPrinter {
        return CloudPrinterBuilder.buildPrinter(name, ipAddress, portNo)
    }

    fun checkUploadWorker(str: String, context: Context): Boolean {
        var instance = WorkManager.getInstance(context)

        val statuses = instance.getWorkInfosByTag(str)
        return try {
            var running = false
            val workInfoList = statuses.get()
            for (workInfo in workInfoList) {
                val state = workInfo.state

                Log.e("ConstantsExt", "checkState:  ${state}")
                running = (state == WorkInfo.State.RUNNING) or (state == WorkInfo.State.ENQUEUED)
            }
            running
        } catch (e: ExecutionException) {
            e.printStackTrace()
            false
        } catch (e: InterruptedException) {
            e.printStackTrace()
            false
        }
    }

}