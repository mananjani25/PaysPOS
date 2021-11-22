package com.android.pos.data.remote

import android.util.Log
import com.android.pos.data.model.PrinterListModel
import com.android.pos.data.model.requestModel.CreatePrinterRequestModel
import com.android.pos.data.model.responseModel.PrinterResponse
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*

object Constants {
    const val ARG_PARAM1 = "param1"
    const val ARG_PARAM2 = "param2"

    //Database Name
    const val DATABASE_NAME = "androidPos-db"

    const val QRCODE_STATIC_URL = "https://tgb.possoft.io/receipt/TGTT5M019451"

    //SharedPref Keys
    const val AUTH_TOKEN = "authToken"
    const val TERMINAL_ID = "terminalId"
    const val BASE_URL_NEW = "baseUrlNew"
    const val PASSCODE = "passcode"
    const val LOCATION_ID = "locationId"
    const val EMAIL = "email"
    const val USERNAME = "userName"
    const val PREF_CUSTOMER = "pref_customer"
    const val CUSTOMER_NAME = "customer_name"
    const val SALE_CUSTOMER_NAME = "sale_customer_name"
    const val IS_CLOCKOUT = "isClockout"
    const val IS_REFUND = "isRefund"
    const val EMPLOYEE_ID = "employee_id"
    const val EMPLOYEE_NAME = "employee_name"
    const val EMPLOYEE_ROLE = "employee_role"
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
    const val AMOUNT = "Amount"
    const val IS_ORDER_UPDATE = "isOrderUpdate"
    const val BUNDLE_ORDER_ID = "BUNDLE_ORDER_ID"
    const val BUNDLE_PAYMENT_ID = "BUNDLE_PAYMENT_ID"
    const val BUNDLE_PAYMENT_OFFLINE_ID = "BUNDLE_PAYMENT_OFFLINE_ID"
    const val BUNDLE_ORDER_OFFLINE_ID = "BUNDLE_ORDER_OFFLINE_ID"

    const val TAKEOUT = "TakeOut"
    const val DINE_IN = "DineIn"
    const val DINE_IN_UPDATE = "DineInUpdate"
    const val DINE_IN_ITEM = "DineInItem"
    const val OPEN_ORDER = "OpenOrder"
    const val OPEN_ORDER_ = "Open Order"
    const val DINE_IN_STATUS = "DineInStatus"
    const val DINE_IN_TABLE_ID = "DineInTableId"

    //Api End Points
    const val USERS_LOG_IN = "users/log_in"
    const val EMPLOYEE_CLOCK_IN = "employee_activities/clock_in"
    const val LOGIN_TERMINAL = "users/login_terminal"
    const val EMPLOYEE_LOG_IN = "employee_activities/log_in"
    const val CLOCK_OUT = "employee_activities/clock_out"
    const val LOGOUT = "users/log_out"
    const val FORGOT_PASSWORD = "users/forgot_password"

    const val EMPLOYEES = "employees"
    const val CUSTOMERS = "customers"
    const val EMPLOYEES_UPDATE_DELETE = "employees/{id}"
    const val DISCOVERY_INTERVAL = 500
    const val EMPLOYEES_TIMESHEET = "employees/timesheet"
    const val EMPLOYEES_TIMESHEET_DETAILS = "employees/timesheet_details"

    const val SYNC_VENUE_DATA = "locations/sync_data"
    const val SYNC_VENUE_DETAILS = "locations/setting_data"
    const val ORDER_TYPES = "order_types"
    const val GET_PRINTERS = "printers"
    const val DELETE_UPDATE_PRINTER = "printers/{id}"
    const val UPDATE_PRINTER_STATUS = "printers/{id}/update_printer_status"


    const val TAXES = "taxes"
    const val TAX_UPDATE_DELETE = "taxes/{id}"
    const val TAX_ACTIVE = "taxes/{id}/active"

    const val TIPS = "tip_settings"
    const val TIPS_UPDATE_DELETE = "tip_settings/{id}"
    const val TIPS_ACTIVE = "tip_settings/{id}/active"

    const val DISCOUNTS = "discounts"
    const val DISCOUNTS_UPDATE_DELETE = "discounts/{id}"

    const val DISCOUNTS_ACTIVE = "discounts/{id}/active"

    const val NOTES = "dynamic_notes"
    const val NOTE_UPDATE_DELETE = "dynamic_notes/{id}"
    const val CUSTOMER_UPDATE = "customers/{id}"
    const val NOTES_ACTIVE = "dynamic_notes/{id}/active"

    const val SERVICE_CHARGE = "service_charges"
    const val SERVICE_CHARGE_UPDATE_DELETE = "service_charges/{id}"
    const val SERVICE_CHARGE_ACTIVE = "service_charges/{id}/active"

    const val LOYALTY_POINT = "loyalty_programs"
    const val LOYALTY_POINT_UPDATE_DELETE = "loyalty_programs/{id}"
    const val LOYALTY_POINT_ACTIVE = "loyalty_programs/{id}/active"

    const val ITEM_UPDATE_DELETE = "items/{id}"
    const val HIDE_ITEM = "items/{id}/active"
    const val ITEMS = "items"
    const val REORDER_ITEM = "items/{id}/reorder"

    const val TEAM_ROLES = "team_roles"
    const val TEAM_ROLES_UPDATE_DELETE = "team_roles/{id}"
    const val GET_TEAM_MODULE = "team_roles/modules"


    const val KITCHEN_RECEIPT_SETTINGS = "kitchen_receipts"
    const val KITCHEN_RECEIPT_UPDATE_SEETINGS = "kitchen_receipts/{id}"

    const val CUSTOMER_RECEIPT_SETTINGS = "customer_receipts"
    const val CUSTOMER_RECEIPTS_UPDATE_SETTINGS = "customer_receipts/{id}"

    const val TRANSACTION_LIST = "payments"
    const val REFUND_PAYMENT = "payments/refund"


    const val VERTICAL = "vertical"
    const val HORIZONTAL = "horizontal"

    const val KEY = "key"
    const val MANUALSALE = "ManualSale"
    const val CUSTOMERDETAILS = "customer_details"
    const val CREATECATEGORY = "create_category"
    const val CREATEMODIFIER = "create_modifier"
    const val CREATEDISCOUNT = "create_discount"
    const val CREATELOYALTY = "create_loyalty"
    const val CREATEOPTION = "create_option"
    const val CREATEITEM = "create_item"
    const val PRINTER = "printer"
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

    const val WIFI = "Wifi"
    const val BLUETOOTH = "Bluetooth"
    const val KITCHEN = "Kitchen"
    const val CUSTOMER = "Customer"
    const val AVAILABLE = "Available"


    const val MODIFIER_UPDATE_DELETE = "modifier_sets/{id}"
    const val MODIFIER = "modifier_sets"
    const val REORDER_MODIFIER = "modifier_sets/{id}/reorder"

    const val OPTION_SETS = "option_sets"
    const val OPTION_UPDATE_DELETE = "option_sets/{id}"
    const val REORDER_OPTION_SET = "option_sets/{id}/reorder_option_sets"


    const val ORDERS = "orders"
    const val ORDER_DETAILS = "orders/{id}"
    const val PAYMENT_DETAILS = "payments/{id}"
    const val ORDER_EMAIL_RECEIPT = "orders/send_order_email_receipt"
    const val ORDER_PHONE_RECEIPT = "orders/sms_order_detail"
    const val ORDER_ASSIGN_CUSTOMER = "orders/{id}/assign_customer_into_order"
    const val ORDER_PAY_AMOUNT_WISE = "payments/pay_amount_wise"

    const val ACTIVE_ORDER = "active_order"
    const val UPCOMING_ORDER = "upcoming_order"
    const val COMPLETED_ORDER = "completed_order"
    const val CANCELED_ORDER = "canceled_order"


    const val UPDATE_TIP = "orders/{id}/update_tip"
    const val FIRE_ITEM_TO_KITCHEN = "orders/{id}/update_fire_status_of_items"
    const val PAY_BY_GUEST = "payments/pay_by_guest"

    const val GET_FLOOR_PLAN = "floor_plans"
    const val FLOOR_PLAN_STATUS = "floor_plan_tables/{id}/check_employee_table_lock"

    const val REPORT_SUMMARY = "reports/report_summary"
    const val ORDER_HISTORY = "customers/{id}/customer_order_history"

    const val BUSINESS_NAME = "business_name"
    const val BUSINESS_ADDRESS = "business_address"
    const val BUSINESS_PHONE_NO = "business_phone_no"
    const val BUSINESS_WEBSITE = "business_website"

    const val CUSTOMER_ID = "customer_id"

    const val OCCUPIED = "Occupied"

    fun createRequestModelForUpdatePrinter(
        model: ArrayList<PrinterResponse.Data.OrderTypes>,
        printerModel: PrinterListModel?
    ): CreatePrinterRequestModel {
        var list: ArrayList<CreatePrinterRequestModel.PrinterSettingsAttributes> = arrayListOf()

        model.forEach { it ->
            for (i in 0 until it.printerSettings.size) {
                list.add(
                    CreatePrinterRequestModel.PrinterSettingsAttributes(
                        it.printerSettings.get(i).id,
                        it.printerSettings.get(i).orderTypeId,
                        it.printerSettings.get(i).printType,
                        it.printerSettings.get(i).manualPrinting,
                        it.printerSettings.get(i).autoPrinting
                    )
                )

            }
        }

        Log.e("ListConvert", "listlist:  ${Gson().toJson(list)}")

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
    const val CASH_EVENTS = "cash_events"

    const val UTC_SERVER_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"


    fun getReceiptFormatDateFromUTCServer(mdate: String): String {

        val df = SimpleDateFormat(UTC_SERVER_FORMAT, Locale.ENGLISH)
        df.setTimeZone(TimeZone.getTimeZone("UTC"))
        val date: Date = df.parse(mdate)
        df.setTimeZone(TimeZone.getDefault())
        val dateFormatter = SimpleDateFormat("MMM-dd-yyyy HH:mm:aa")
        return dateFormatter.format(date)

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


    var SPLIT_PAY_TYPE = "split_pay_type"
    var SPLIT_PAY_AMOUNT = "split_pay_amount"
    var SPLIT_NO = "split_no"


    var SPLIT_PAY_TYPE_DINE_IN = "split_pay_type_dine_in"
    var SPLIT_PAY_AMOUNT_DINE_IN = "split_pay_amount_dine_in"
    var SPLIT_NO_DINE_IN = "split_no_dine_in"

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
}