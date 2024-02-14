package com.pays.pos.di

import android.content.Context
import android.content.SharedPreferences
import com.pays.pos.R
import com.pays.pos.data.entities.LoyaltyProgramsModel
import com.pays.pos.data.entities.TbCustomer
import com.pays.pos.data.entities.TeamRole
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.BASE_URL_NEW
import com.pays.pos.data.remote.Constants.LOCATION_ID
import com.pays.pos.data.remote.Constants.ROLE_ADMIN
import com.pays.pos.data.remote.Constants.ROLE_EMPLOYEE
import com.pays.pos.data.remote.Constants.ROLE_MANAGER
import com.pays.pos.data.remote.Constants.ROLE_OWNER
import com.pays.pos.data.remote.Constants.UNIQUE_ID
import com.pays.pos.di.ApiModule.BASE_URL
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrefProvider @Inject constructor(@ApplicationContext context: Context) {

    internal var sharedPreferences: SharedPreferences? = null
    internal var loginRememberPreferences: SharedPreferences? = null
    internal var mContext = context

    fun openPref() {
        sharedPreferences = mContext.getSharedPreferences(
            mContext.resources.getString(R.string.app_name),
            Context.MODE_PRIVATE
        )
    }

    fun openPrefForLogin() {
        loginRememberPreferences = mContext.getSharedPreferences(
            "LoginRememberPref",
            Context.MODE_PRIVATE
        )
    }
    fun getValueForLogin(
        key: String,
        defaultValue: String
    ): String {
        openPrefForLogin()
        val result = loginRememberPreferences?.getString(key, defaultValue)
        loginRememberPreferences = null
        return result ?: ""
    }

    fun setValueForLogin(
        key: String,
        value: String
    ) {
        try{
        openPrefForLogin()
        val prefsPrivateEditor = loginRememberPreferences!!.edit()
        prefsPrivateEditor!!.putString(key, value)
        prefsPrivateEditor.apply()
        loginRememberPreferences = null
        }catch (_:Exception){}
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
        try{
            val result = sharedPreferences!!.getInt(key, defaultValue)
            sharedPreferences = null
            return result
        }catch (e:Exception){
            return 0
        }
    }

    fun setValue(
        key: String,
        value: String
    ) {
        try{
        openPref()
        val prefsPrivateEditor = sharedPreferences!!.edit()
        prefsPrivateEditor!!.putString(key, value)
        prefsPrivateEditor.commit()
        sharedPreferences = null
    }catch (_:Exception){}
    }

    fun setValueInt(
        key: String?,
        value: Int
    ) {
        try{
        openPref()
        val prefsPrivateEditor = sharedPreferences!!.edit()
        prefsPrivateEditor!!.putInt(key, value)
        prefsPrivateEditor.commit()
        sharedPreferences = null
        }catch (_:Exception){}
    }


    fun setValueDouble(
        key: String?,
        value: Double
    ) {
        try{
        openPref()
        val prefsPrivateEditor = sharedPreferences!!.edit()
        prefsPrivateEditor!!.putLong(key, value.toLong())
        prefsPrivateEditor.commit()
        sharedPreferences = null
        }catch (_:Exception){}
    }

    fun getValueDouble(
        key: String,
        defaultValue: Double
    ): Double {

        openPref()
        val result = sharedPreferences?.getFloat(key, defaultValue.toFloat())
        sharedPreferences = null
        return result?.toDouble()?:defaultValue
    }

    fun getValueboolean(
        key: String?,
        defaultValue: Boolean
    ): Boolean {
        openPref()
        val result = sharedPreferences?.getBoolean(key, defaultValue)
        sharedPreferences = null
        return result?:defaultValue
    }

    fun setValueboolean(

        key: String?,
        value: Boolean
    ) {
        try{
        openPref()
        val prefsPrivateEditor = sharedPreferences!!.edit()
        prefsPrivateEditor!!.putBoolean(key, value)
        prefsPrivateEditor.commit()
        sharedPreferences = null
        }catch (_:Exception){}
    }


    fun setClear() {
        try {
        openPref()
        val prefsPrivateEditor = sharedPreferences!!.edit().clear().commit()
//        prefsPrivateEditor!!.clear().apply()
        sharedPreferences = null
        }catch (_:Exception){}
    }

    fun saveCustomerData(customer: TbCustomer?) {
        setValue(Constants.PREF_CUSTOMER, Gson().toJson(customer))
    }

    fun getCustomerData(): TbCustomer? {
        return Gson().fromJson(
            getValue(Constants.PREF_CUSTOMER, ""),
            TbCustomer::class.java
        )
    }

    fun saveActiveLoyaltyData(loyaltyProgramsModel: LoyaltyProgramsModel?) {
        setValue(Constants.PREF_ACTIVE_LOYALTY_PROGRAM, Gson().toJson(loyaltyProgramsModel))
    }

    fun getActiveLoyaltyData(): LoyaltyProgramsModel? {
        return Gson().fromJson(
            getValue(Constants.PREF_ACTIVE_LOYALTY_PROGRAM, ""),
            LoyaltyProgramsModel::class.java
        )
    }

    fun saveCurrentRoleDetails(teamRole: TeamRole) {
        setValue(Constants.CURRENT_EMPLOYEE_ROLE, Gson().toJson(teamRole))
    }

    fun getCurrentEmployeeRole(): TeamRole? {
        return Gson().fromJson(
            getValue(Constants.CURRENT_EMPLOYEE_ROLE, ""),
            TeamRole::class.java
        )
    }

    fun getEmployeeRoleId(): Int {
        return getValueInt(Constants.EMPLOYEE_ROLE_ID, 0)
    }

    fun getTerminalId(): Int {
        return getValueInt(Constants.TERMINAL_ID, 0)
    }

    fun getEmployeeRole(): String {
        return getValue(Constants.EMPLOYEE_ROLE, "")

    }

    fun employeeId(): Int {
      return   getValueInt(Constants.EMPLOYEE_ID, 0)
    }
    fun employeeName(): String {
      return   getValue(Constants.EMPLOYEE_NAME, "0")
    }

    fun setUniqueId(deviceId: String) {
        setValue(UNIQUE_ID, deviceId)
    }

    fun getUniqueId(): String {
        return getValue(UNIQUE_ID, "")
    }

    fun getLocationId(): Int {
        return getValueInt(LOCATION_ID, -1)
    }

    fun getBaseUrl(): String {
        return getValue(BASE_URL_NEW, BASE_URL)
    }

    fun setBaseUrl(string: String): String {
        return getValue(BASE_URL_NEW, string)
    }

    fun isManager(): Boolean {
        return getValue(Constants.EMPLOYEE_ROLE, "").equals(ROLE_MANAGER, true)
    }

    fun isOwner(): Boolean {
        return getValue(Constants.EMPLOYEE_ROLE, "").equals(ROLE_OWNER, true)
    }

    fun isEmployee(): Boolean {
        return getValue(Constants.EMPLOYEE_ROLE, "").equals(ROLE_EMPLOYEE, true)
    }

    fun isAdmin(): Boolean {
        return getValue(Constants.EMPLOYEE_ROLE, "").equals(ROLE_ADMIN, true)
    }

    fun getOrderTypeName(
        key: String,
        defaultValue: String
    ): String {
        openPref()
        val result = sharedPreferences?.getString(key, defaultValue)
        return if (result != "") {
            result.toString()
        } else {
            defaultValue
        }
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
         prefsPrivateEditor.commit()
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