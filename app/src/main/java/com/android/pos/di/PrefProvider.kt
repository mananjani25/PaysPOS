package com.android.pos.di

import android.content.Context
import android.content.SharedPreferences
import com.android.pos.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrefProvider @Inject constructor(@ApplicationContext context: Context) {

    private var sharedPreferences: SharedPreferences? = null

    private fun openPref(context: Context) {
        sharedPreferences = context.getSharedPreferences(
            context.resources.getString(R.string.app_name),
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


    fun setClear(context: Context) {
        openPref(context)
        val prefsPrivateEditor = sharedPreferences!!.edit()
        prefsPrivateEditor!!.clear().apply()
        sharedPreferences = null
    }

   /* fun setCustomObject(
        context: Context,
        key: String?,
        `object`: Any?
    ) {
        val gson = Gson()
        val json = gson.toJson(`object`)

        openPref(context)
        val prefsPrivateEditor = sharedPreferences!!.edit()
        prefsPrivateEditor!!.putString(key, json)
        prefsPrivateEditor.apply()
        sharedPreferences = null
    }

    fun getCustomObject(
        context: Context,
        key: String?,
        dataBeanClass: Class<NearByLocation?>?
    ): NearByLocation? {
        val gson = Gson()
        openPref(context)
        val json = sharedPreferences!!.getString(key, "")

        return gson.fromJson(json, dataBeanClass)
    }

    fun saveUser(user: UserModel?) {
        val userString = Gson().toJson(user)
        preference.edit().putString(
            KEY_USER, userString
        ).apply()
    }

    fun getUser(): UserModel {
        return Gson().fromJson(
            preference.getString(KEY_USER, null),
            UserModel::class.java
        )
    }*/

}