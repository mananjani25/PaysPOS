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


    //Api End Points
    const val USERS_LOG_IN = "users/log_in"
    const val EMPLOYEE_CLOCK_IN = "employee_activities/clock_in"
    const val LOGIN_TERMINAL = "users/login_terminal"
    const val EMPLOYEE_LOG_IN = "employee_activities/log_in"
    const val CLOCK_OUT = "employee_activities/clock_out"
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
    const val LOGOUT = "users/log_out"

    const val SERVICE_CHARGE = "service_charges"
    const val SERVICE_CHARGE_UPDATE_DELETE = "service_charges/{id}"


    const val CREATECATEGORY = "create_category"
    const val CREATEMODIFIER = "create_modifier"
    const val CREATEDISCOUNT = "create_discount"
    const val CREATEOPTION = "create_option"
    const val CREATEITEM = "create_item"

    const val KEY = "key"

    const val VERTICAL = "vertical"
    const val HORIZONTAL = "horizontal"

}