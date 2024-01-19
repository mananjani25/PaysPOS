package com.pays.pos.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import com.pays.pos.R


object Pref {
    private var sharedPreferences: SharedPreferences? = null
    private fun openPref(context: Context) {
        sharedPreferences = context.getSharedPreferences(
            context.resources.getString(R.string.app_name)+" APP",
            Context.MODE_PRIVATE
        )
    }

    fun getValue(
        context: Context, key: String?,
        defaultValue: String?
    ): String? {
        openPref(context)
        val result = sharedPreferences!!.getString(key, defaultValue)
        sharedPreferences = null
        return result
    }

    fun getValueInt(
        context: Context, key: String?,
        defaultValue: Int
    ): Int {
        openPref(context)
        val result = sharedPreferences!!.getInt(key, defaultValue)
        sharedPreferences = null
        return result
    }

    fun setValue(
        context: Context,
        key: String?,
        value: String?
    ) {
        openPref(context)
        val prefsPrivateEditor = sharedPreferences!!.edit()
        prefsPrivateEditor!!.putString(key, value)
        prefsPrivateEditor.apply()
        sharedPreferences = null
    }

    fun setValueInt(
        context: Context,
        key: String?,
        value: Int
    ) {
        openPref(context)
        val prefsPrivateEditor = sharedPreferences!!.edit()
        prefsPrivateEditor!!.putInt(key, value)
        prefsPrivateEditor.apply()
        sharedPreferences = null
    }

    fun getValueboolean(
        context: Context, key: String?,
        defaultValue: Boolean
    ): Boolean {
        openPref(context)
        val result = sharedPreferences!!.getBoolean(key, defaultValue)
        sharedPreferences = null
        return result
    }

    fun setValueboolean(
        context: Context,
        key: String?,
        value: Boolean
    ) {
        openPref(context)
        val prefsPrivateEditor = sharedPreferences!!.edit()
        prefsPrivateEditor!!.putBoolean(key, value)
        prefsPrivateEditor.apply()
        sharedPreferences = null
    }

    @SuppressLint("CommitPrefEdits")
    fun setClear(context: Context) {
        openPref(context)
        val prefsPrivateEditor = sharedPreferences!!.edit()
        prefsPrivateEditor!!.clear().apply()
        sharedPreferences = null
    }

    fun clearSingleKey(context: Context,key: String?) {
        openPref(context)
        val prefsPrivateEditor = sharedPreferences!!.edit()
        prefsPrivateEditor.remove(key).apply()

    }


}