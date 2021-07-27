package com.android.pos.data.remote

object Constants {

    //Database Name
    const val DATABASE_NAME = "androidPos-db"

    //SharedPref Keys
    const val AUTH_TOKEN = "authToken"
    const val TERMINAL_ID = "terminalId"
    const val BASE_URL_NEW = "baseUrlNew"
    const val PASSCODE = "passcode"
    const val LOCATION_ID = "locationId"
    const val EMAIL = "email"
    const val USERNAME = "userName"
    const val IS_CLOCKOUT = "isClockout"


    //Api End Points
    const val USERS_LOG_IN = "users/log_in"
    const val EMPLOYEE_CLOCK_IN = "employee_activities/clock_in"
    const val LOGIN_TERMINAL = "users/login_terminal"
    const val EMPLOYEE_LOG_IN = "employee_activities/log_in"
    const val CLOCK_OUT = "employee_activities/clock_out"
    const val LOGOUT = "users/log_out"
    const val FORGOT_PASSWORD = "users/forgot_password"

    const val EMPLOYEES = "employees"
    const val EMPLOYEES_UPDATE_DELETE = "employees/{id}"

    const val SYNC_VENUE_DATA = "locations/sync_data"

    const val TAXES = "taxes"
    const val TAX_UPDATE_DELETE = "taxes/{id}"

    const val TIPS = "tip_settings"
    const val TIPS_UPDATE_DELETE = "tip_settings/{id}"

    const val DISCOUNTS = "discounts"
    const val DISCOUNTS_UPDATE_DELETE = "discounts/{id}"

    const val NOTES = "dynamic_notes"
    const val NOTE_UPDATE_DELETE = "dynamic_notes/{id}"

    const val SERVICE_CHARGE = "service_charges"
    const val SERVICE_CHARGE_UPDATE_DELETE = "service_charges/{id}"

    const val ITEM_UPDATE_DELETE = "item/{id}"


    const val VERTICAL = "vertical"
    const val HORIZONTAL = "horizontal"

    const val KEY = "key"
    const val CREATECATEGORY = "create_category"
    const val CREATEMODIFIER = "create_modifier"
    const val CREATEDISCOUNT = "create_discount"
    const val CREATEOPTION = "create_option"
    const val CREATEITEM = "create_item"

    const val SETTING_KEY = "setting_key"
    const val CREATE_TAX = "create_tax"
    const val CREATE_TIP = "create_tip"
    const val ORDER_RECEIPTS = "order_receipts"
    const val CREATE_NOTES = "create_notes"
    const val ADD_SERVICE_CHARGE = "add_service_charge"

    const val DIALOG_KEY = "dialog_key"
    const val DIALOG_KEY_TAX = "dialog_key_tax"

    const val CATEGORY_UPDATE_DELETE = "categories/{id}"
    const val HIDE_CATEGORY = "categories/{id}/active"
    const val REORDER_CATEGORY = "categories/{id}/reorder"
    const val CATEGORY = "categories"
}