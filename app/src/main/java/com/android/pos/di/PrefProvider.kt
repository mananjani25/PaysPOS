package com.android.pos.di

import android.content.Context
import android.content.SharedPreferences
import com.android.pos.R
import com.android.pos.data.entities.TbCustomer
import com.android.pos.data.remote.Constants
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrefProvider @Inject constructor(@ApplicationContext context: Context) {

    private var sharedPreferences: SharedPreferences? = null
    private var mContext = context

    private fun openPref() {
        sharedPreferences = mContext.getSharedPreferences(
            mContext.resources.getString(R.string.app_name),
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
        return result?:""
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

    fun getValueboolean(
        key: String?,
        defaultValue: Boolean
    ): Boolean {
        openPref()
        val result = sharedPreferences!!.getBoolean(key, defaultValue)
        sharedPreferences = null
        return result
    }

    fun setValueboolean(

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

    fun saveCustomerData(customer: TbCustomer?) {
        setValue(Constants.PREF_CUSTOMER, Gson().toJson(customer))
    }

    fun getCustomerData(): TbCustomer? {
        return Gson().fromJson(
            getValue(Constants.PREF_CUSTOMER,""),
            TbCustomer::class.java
        )
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