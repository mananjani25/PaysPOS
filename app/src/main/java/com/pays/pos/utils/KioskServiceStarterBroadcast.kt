package com.pays.pos.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.pays.pos.data.remote.Constants
import com.pays.pos.di.PrefProvider
import com.pays.pos.service.KioskService

public class KioskServiceStarterBroadcast : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val i = Intent("com.pays.pos.service.KioskService")
        i.setClass(context, KioskService::class.java)

        PrefProvider(context)?.let {
            i.putExtra(Constants.LOCATION_ID,it.getValueInt(Constants.LOCATION_ID, 0))
            i.putExtra(Constants.TERMINAL_ID,it.getValueInt(Constants.TERMINAL_ID, 0))
            i.putExtra(Constants.TERMINAL_NAME,it.getValue(Constants.TERMINAL_NAME, ""))
            i.putExtra(Constants.BASE_URL_NEW,it.getBaseUrl())
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(i)
        } else {
            context.startService(i)
        }
    }
}