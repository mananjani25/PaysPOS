package com.pays.pos.utils.paxUtils

import android.content.Context
import android.util.Log
import com.pax.poslink.CommSetting
import com.pax.poslink.PosLink
import com.pax.poslink.poslink.POSLinkCreator
import com.pax.poslink.usb.UsbUtil
import com.pays.pos.utils.paxUtils.SettingINI.getCommSettingFromFile


object POSLinkCreatorWrapper {
    private fun create(context: Context): PosLink {
        val iniFile: String = context.getFilesDir().getAbsolutePath() + "/" + SettingINI.FILENAME
        val commset = getCommSettingFromFile(context!!,iniFile)
        if (commset.type == CommSetting.USB && !UsbUtil.hasPermission(context)) {
            val usbDevice = UsbUtil.getDevice(context)
            if (usbDevice == null) {
                Log.i("usbDevice: ", "Please plug in the POS machine with USB.")
            }
        }
        return POSLinkCreator.createPoslink(context)
    }

    fun createSync(context: Context, callback: AppThreadPool.FinishInMainThreadCallback<PosLink?>) {
        Log.i("DEBUG", "Start Create POSLink")
        try {
            callback.onFinish(create(context))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        Log.i("DEBUG", "Finish Create POSLink")
    }
}