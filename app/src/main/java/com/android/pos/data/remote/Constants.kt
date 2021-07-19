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


    //Api End Points
    const val USERS_LOG_IN = "users/log_in"
    const val EMPLOYEE_CLOCK_IN = "employee_activities/clock_in"
    const val LOGIN_TERMINAL = "users/login_terminal"
    const val EMPLOYEE_LOG_IN = "employee_activities/log_in"
    const val CLOCK_OUT = "employee_activities/clock_out"
    const val EMPLOYEES = "employees"

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

}