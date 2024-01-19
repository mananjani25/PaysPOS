package com.pays.pos.utils.paxUtils

import android.content.Context
import com.pax.poslink.CommSetting
import com.pax.poslink.POSLinkCommon
import java.lang.reflect.InvocationTargetException

object Convenience {
    private const val BUTTON_TRIGGER_CNG = 20
    const val KEY_CONVENIENCE_BUTTON_CLICK_CNT = "Convenience_button_click_cnt"
    private var buttonClickCnt = 0
    fun init(context: Context?) {
//        buttonClickCnt = SharedPreferenceHelper.getInt(KEY_CONVENIENCE_BUTTON_CLICK_CNT, 0)
    }

    fun clickBtn() {
        buttonClickCnt++
//        SharedPreferenceHelper.save(KEY_CONVENIENCE_BUTTON_CLICK_CNT, buttonClickCnt)
    }

    val isButtonClickEnough: Boolean
        get() = buttonClickCnt >= BUTTON_TRIGGER_CNG

    fun setHost(context: Context?, commSetting: CommSetting, host: String?) {
        try {
            val setHostMethod = commSetting.javaClass.getDeclaredMethod(
                "setHost",
                Context::class.java,
                String::class.java
            )
            setHostMethod.isAccessible = true
            setHostMethod.invoke(commSetting, context, host)
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        }
    }

    fun getHost(context: Context?, commSetting: CommSetting): String {
        try {
            val getHostMethod = commSetting.javaClass.getDeclaredMethod(
                "getHost",
                Context::class.java
            )
            getHostMethod.isAccessible = true
            return getHostMethod.invoke(commSetting, context) as String
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        }
        return ""
    }

    fun omahaSendData(s1c: String, s1f: String): String {
        return (POSLinkCommon.S_STX
                + "*1PPX81.022009001234566" + "#"
                + "PAXP" + s1c
                + "A01" + s1f
                + "3C20" + s1f
                + "1" + s1f
                + "4" + s1f
                + "9" + s1c
                + "4012000033330026" + "="
                + "49121015432112345601" + s1c
                + "9.00" + s1c
                + "" + s1c
                + "00030999" + s1c
                + "6" + s1c
                + "" + s1c + "" + s1c + "" + s1c + "" + s1c + "" + s1c + "" + s1c + "" + s1c + "" + s1c
                + "" + s1c + "" + s1c + "" + s1c + "" + s1c + "" + s1c + "" + s1c
                + "" + s1f + "C" + s1f + "" + s1f + "" + s1f + "" + s1f + "" + s1f + "" + s1f + "1" + s1f
                + "DPX003" + s1c
                + "" + s1c + "" + s1c + "" + s1c + "" + s1c + "" + s1c
                + "" + POSLinkCommon.S_ETX
                + "f")
    }
}