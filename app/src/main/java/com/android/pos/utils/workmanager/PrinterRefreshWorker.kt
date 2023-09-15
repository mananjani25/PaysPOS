package com.android.pos.utils.workmanager

import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.android.pos.data.remote.Constants
import com.android.pos.data.repositories.PosRepository
import com.android.pos.data.repositories.UserRepository
import com.android.pos.ui.activities.MainActivity
import com.android.pos.ui.fragments.settings.hardware.printer.Printer
import com.android.pos.utils.Event
import com.android.pos.utils.LogUtil
import com.android.pos.utils.statusUtils.Status
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.hosopy.actioncable.ActionCable
import com.hosopy.actioncable.Channel
import com.hosopy.actioncable.Consumer
import com.hosopy.actioncable.Subscription
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.NotNull
import java.net.URI
import javax.inject.Inject

class PrinterRefreshWorker(@NotNull context: Context, @NotNull params: WorkerParameters):CoroutineWorker(context,params) {

    private var subscription: Subscription? = null
    private var consumer: Consumer? = null
    private var locationId: Int = 0
    private var baseUrl = ""
    private val TAG = "PrinterRefreshWorker"

    override suspend fun doWork(): Result {


        Log.d("PrinterRefreshWorker","PrinterRefreshWorker doWork")

        withContext(Dispatchers.IO) {

            locationId = inputData.getInt("location_id", 0)
            baseUrl = inputData.getString("base_url").toString()

            LogUtil.logE(TAG,"locationId = $locationId")
            LogUtil.logE(TAG,"baseUrl = $baseUrl")

            connectActionCable()
        }

        return Result.success()
    }

    suspend fun connectActionCable() {
        // 1. Setup
        var requestURL =
            baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3

        Log.d("PrinterRefreshWorker","requestURL = $requestURL")

        val uri = URI(Constants.PRINTER_QUEUE_CONNECTION_URL_HUGEPOS)
        consumer = ActionCable.createConsumer(uri)

        Log.d("PrinterRefreshWorker","uri = $uri")

        // 2. Create subscription
        val appearanceChannel = Channel("SyncChannel")
        appearanceChannel.addParam("id", locationId)
        // appearanceChannel.addParam("id",prefProvider.getValueInt(LOCATION_ID,0))
        subscription = consumer?.subscriptions?.create(appearanceChannel)

        if (subscription != null) {
            subscription?.onConnected {
                Log.e(TAG, "onActionConnected")
                val params = JsonObject()
                params.addProperty("location_id", locationId)
              //  params.addProperty("url", requestURL)
                subscription?.perform("received", params)
            }?.onRejected {
                Log.e(TAG, "onRejected")

            }?.onReceived {
                Log.e(TAG, "onReceived  " + Gson().toJson(it))

                handleUpdatedData(it)


            }?.onDisconnected {
                Log.e(TAG, "onDisconnected")

            }?.onFailed {
                LogUtil.logE(TAG, "onFailed")
            }
        }


        // 3. Establish connection
        consumer?.connect()


    }

    private fun handleUpdatedData(it: JsonElement) {

      try {
          if (it.asJsonObject.has("setting_data")){

              val setting_data  = it.asJsonObject.get("setting_data")
              Log.e(TAG, "call setting_data API")

              if (setting_data.toString() == "true"){
                  if (Printer.updatePrinter == null){

                      MainActivity.updatePrinter?.updatePrinters()

                  }else {
                      Printer.updatePrinter?.updatePrinters()
                  }

              }
          }
      }catch (e:Exception){
          Log.e(TAG, "Exception")
      }
    }
}