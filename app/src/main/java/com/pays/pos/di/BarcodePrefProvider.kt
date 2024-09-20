package com.pays.pos.di

import android.content.Context
import android.content.SharedPreferences
import com.pays.pos.MainApplication
import com.pays.pos.R
import com.pays.pos.data.remote.Constants
import com.pays.pos.utils.scanner.helpers.AvailableScanner
import com.google.gson.Gson
import com.pays.pos.ui.activities.MainActivity
import com.zebra.scannercontrol.DCSScannerInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
/*ALL THE SCAN GUN VARIABLES ARE COMMENTED AND MOVED TO MAINACTIVITY(for solving permission issue), PLEASE UNCOMMENT IT AND REMOVE THE VARIABLES FROM MAINACTIVITY*/

@Singleton
class BarcodePrefProvider @Inject constructor(@ApplicationContext context: Context) {

    private var sharedPreferences: SharedPreferences? = null
    private var mContext = context

    private fun openPref() {
        sharedPreferences = mContext.getSharedPreferences(
            mContext.resources.getString(R.string.PREF_BARCODE_SCANNER),
            Context.MODE_PRIVATE
        )
    }

    fun getValue(
        key: String,
        defaultValue: String
    ): String {
        openPref()
        val result = sharedPreferences?.getString(key, defaultValue)
        sharedPreferences = null
        return result ?: ""
    }

    fun getValueInt(
        key: String?,
        defaultValue: Int
    ): Int {
        openPref()
        val result = sharedPreferences!!.getInt(key, defaultValue)
        sharedPreferences = null
        return result
    }

    fun setValue(
        key: String,
        value: String
    ) {
        openPref()
        val prefsPrivateEditor = sharedPreferences!!.edit()
        prefsPrivateEditor!!.putString(key, value)
        prefsPrivateEditor.apply()
        sharedPreferences = null
    }

    fun setValueInt(
        key: String?,
        value: Int
    ) {
        openPref()
        val prefsPrivateEditor = sharedPreferences!!.edit()
        prefsPrivateEditor!!.putInt(key, value)
        prefsPrivateEditor.apply()
        sharedPreferences = null
    }


    fun setValueDouble(
        key: String?,
        value: Double
    ) {
        openPref()
        val prefsPrivateEditor = sharedPreferences!!.edit()
        prefsPrivateEditor!!.putLong(key, value.toLong())
        prefsPrivateEditor.apply()
        sharedPreferences = null
    }

    fun getValueDouble(
        key: String,
        defaultValue: Double
    ): Double {
        openPref()
        val result = sharedPreferences!!.getFloat(key, defaultValue.toFloat())
        sharedPreferences = null
        return result.toDouble()
    }

    fun getValueBoolean(
        key: String?,
        defaultValue: Boolean
    ): Boolean {
        openPref()
        val result = sharedPreferences!!.getBoolean(key, defaultValue)
        sharedPreferences = null
        return result
    }

    fun setValueBoolean(

        key: String?,
        value: Boolean
    ) {
        openPref()
        val prefsPrivateEditor = sharedPreferences!!.edit()
        prefsPrivateEditor!!.putBoolean(key, value)
        prefsPrivateEditor.apply()
        sharedPreferences = null
    }

    fun setClear() {
        openPref()
        val prefsPrivateEditor = sharedPreferences!!.edit()
        prefsPrivateEditor!!.clear().apply()
        sharedPreferences = null
    }

    fun saveScannerId(scannerId: Int?) {
        setValueInt(Constants.PREF_CURRENT_SCANNER_ID, scannerId?:MainActivity.SCANNER_ID_NONE)
    }

    fun getScannerId(): Int {
        return getValueInt(Constants.PREF_CURRENT_SCANNER_ID, MainActivity.SCANNER_ID_NONE)
    }

    fun saveScannerData(availableScanner: AvailableScanner?) {
        setValue(Constants.PREF_CURRENT_SCANNER, Gson().toJson(availableScanner))
    }

    fun getScannerData(): AvailableScanner? {
        return Gson().fromJson(
            getValue(Constants.PREF_CURRENT_SCANNER, ""),
            AvailableScanner::class.java
        )
    }

}