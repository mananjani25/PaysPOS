package com.android.pos.ui.fragments.magtek

import android.content.Context
import android.os.Environment
import android.os.Message
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.R
import com.android.pos.data.entities.TbCardReader
import com.android.pos.data.model.responseModel.GetTransactionListResponse
import com.android.pos.data.model.responseModel.PosLinkResult
import com.android.pos.data.remote.Constants
import com.android.pos.data.repositories.PosRepository
import com.android.pos.di.ApiModule2
import com.android.pos.di.PrefProvider
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.Event
import com.android.pos.utils.LogUtil
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.paxUtils.AppThreadPool
import com.android.pos.utils.paxUtils.POSLinkCreatorWrapper
import com.android.pos.utils.paxUtils.SettingINI
import com.pax.poslink.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class MagtekViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val prefProvider: PrefProvider,
    private val apiModule2: ApiModule2
) : ViewModel() {

    private var posLink: PosLink = PosLink()
    fun cardReaderList() = posRepository.getCardReaderList()

    fun cardReaderById(id: String) = posRepository.getCardReaderList(id)

    private val _merchantData = MutableLiveData<Event<ManageResponse?>>()
    val merchantData: LiveData<Event<ManageResponse?>> = _merchantData

    fun addCardReader(tbCardReader: TbCardReader) {
        viewModelScope.launch {

            posRepository.addCardReader(tbCardReader)
        }

    }

    fun updateCardReader(tbCardReader: TbCardReader) {
        viewModelScope.launch {

            posRepository.updateCardReader(tbCardReader)
        }

    }

    fun deleteTable() {

        viewModelScope.launch {
            posRepository.deleteTable()
        }
    }

    fun initPOSLink(context: Context) {
        POSLinkCreatorWrapper.createSync(
            context,
            object : AppThreadPool.FinishInMainThreadCallback<PosLink?> {
                override fun onFinish(result: PosLink?) {
                    posLink = result!!
                    Log.d("initPOSLink: ", "onFinish")
                    paxNetworkCall(context)
                }
            })
    }

    private fun paxNetworkCall(context: Context) {
        ProgressUtils.showProgressDialog("Connecting to PAX", context)
        val srNo = prefProvider.getValue(
            Constants.PAX_SERIAL_NO,
            ""
        )
        val TID = prefProvider.getValue(
            Constants.PAX_TERMINAL_ID,
            ""
        )
        Log.d("Params: ", "srNo $srNo TID $TID")

        var call: Call<PosLinkResult>? =
            apiModule2.getRetrofit2().getPAXDetails("", srNo, "")
        call!!.enqueue(object : Callback<PosLinkResult> {

            override fun onResponse(
                call: Call<PosLinkResult>,
                response: Response<PosLinkResult>
            ) {
//                ProgressUtils.dismissProgressDialog()
                if (response.isSuccessful) {
                    LogUtil.logE(
                        "onResponse",
                        response.body().toString() + response.body()!!.ipAddress
                    )
                    var ipAddress = response.body()!!.ipAddress
                    var port = response.body()!!.port
                    prefProvider.setValue(
                        Constants.PAX_IP,
                        ipAddress
                    )
                    prefProvider.setValue(
                        Constants.PAX_PORT,
                        port.toString()
                    )
                    Log.d("Pax Params: ", "pax $ipAddress $port")
                    setCommSetting(context, ipAddress, port.toString())
                    getMerchantDetails(context)
//                    connectBP()
                }
            }

            override fun onFailure(
                call: Call<PosLinkResult>,
                t: Throwable
            ) {

                ProgressUtils.dismissProgressDialog()

                AlertUtils.showCustomAlert(context, t.message)
                Log.d("onFailure: ", "Message-> ${t.message}")
            }
        })
    }

    private fun setCommSetting(context: Context, edtIP: String, edtPort: String) {
        //create commsetting object

        var file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val iniFile = "/storage/emulated/0/Download/" + SettingINI.FILENAME
        /*val iniFile =
            activity!!.applicationContext.filesDir.absolutePath + "/" + SettingINI.FILENAME*/
        val commset: CommSetting = SettingINI.getCommSettingFromFile(iniFile)
        Log.d("iniFile: ", "iniFile $iniFile ${file.absolutePath}")

        //initialization value  for comsetting's attribute
        commset.type = CommSetting.TCP
        commset.timeOut = "-1"
        commset.baudRate = "9600"
//        commset.serialPort = "COM1"
        commset.isEnableProxy = false
        commset.destPort = edtPort
        commset.destIP = edtIP
        /*val selectedHost = "UNKNOWN"
        Convenience.setHost(context, commset, selectedHost)*/
        Log.i(
            "TAG", "coms.CommType = " + commset.type + "; coms.TimeOut=" + commset.timeOut
                    + "; SerialPort=" + commset.serialPort + "; coms.BaudRate=" + commset.baudRate
                    + "; coms.DestIP=" + commset.destIP + "; coms.DestPort=" + commset.destPort + "; coms.MacAddr=" + commset.macAddr + "; coms.EnableProxy=" + commset.isEnableProxy
        )
        POSLinkAndroid.initPOSListener(context, commset)
        SettingINI.saveCommSettingToFile(iniFile, commset)
        // set the folder to save the "comsetting.ini" file
        posLink.appDataFolder = file.absolutePath
        posLink.SetCommSetting(commset)
        Log.d("SetCommSetting: ", "saved successfully")
    }

    // Get merchant details from pax
    private fun getMerchantDetails(context: Context) {
        var result: ProcessTransResult? = null
        GlobalScope.launch {
            Log.d("manageRequest ", "Start")
            val manageRequest = ManageRequest()
//            manageRequest.TransType = manageRequest.ParseTransType("INIT")
            manageRequest.TransType = manageRequest.ParseTransType("GETVAR")
            manageRequest.EDCType = manageRequest.ParseEDCType("CREDIT")
            manageRequest.VarName = "MID"
            manageRequest.ContactlessEntryFlag = "1"

            posLink.ManageRequest = manageRequest


            launch {
                try {
                    result = async(IO) { posLink.ProcessTrans() }.await()
                    if (result?.Code === ProcessTransResult.ProcessTransResultCode.OK) {
                        val msg = Message()
                        msg.what = Constants.TRANSACTION_SUCCESSED
                        msg.obj = posLink.ManageResponse

                        val response = msg.obj as ManageResponse
                        _merchantData.postValue(Event(response))
                        Log.d(
                            "result: ",
                            result!!.Code.toString() + " Msg: " + result!!.Msg + response.ResultTxt
                        )
                    } else {
                        CoroutineScope(Dispatchers.Main).launch {
                            ProgressUtils.dismissProgressDialog()
                            if (result?.Msg.toString() == "CONNECT ERROR" || result?.Msg.toString() == "TIME OUT") {
                                AlertUtils.showCustomAlert(
                                    context,
                                    context.getString(R.string.pax_connect_error)
                                )
                            } else {
                                Toast.makeText(
                                    context,
                                    "getMerchantDetails Failed ${result?.Code} ${result?.Msg}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }

                } catch (e: Exception) {
                    Log.d("Exception: ", "Exception ${e.message}")
                }
            }

            /*if (result.Code === ProcessTransResult.ProcessTransResultCode.OK) {
                val msg = Message()
                msg.what = Constants.TRANSACTION_SUCCESSED
                msg.obj = posLink.ManageResponse

                val response = msg.obj as ManageResponse
                val resultCode = response.resultCode
                val status = response.resultTxt
                val mID = response.VarValue
                prefProvider.setValue(
                    Constants.MERCHANT_ID,
                    mID
                )
                CoroutineScope(Dispatchers.Main).launch {
                    ProgressUtils.dismissProgressDialog()
                    AlertUtils.showCustomAlert(context, "Merchant $mID is connected successfully")
                    *//*binding.tvDisconnectPax.visibility = View.VISIBLE
                    binding.tvPax.visibility = View.GONE*//*
                }
                prefProvider.setValueboolean(Constants.IS_PAX_CONNECTED, true)
                Log.d("Merchant Details: ", mID + " " + resultCode + "  " + status)
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    ProgressUtils.dismissProgressDialog()
                    if (result.Msg.toString() == "CONNECT ERROR" || result.Msg.toString() == "TIME OUT") {
//                        Toast.makeText(requireContext(), R.string.pax_connect_error, Toast.LENGTH_LONG).show()
                        AlertUtils.showCustomAlert(
                            context,
                            context.getString(R.string.pax_connect_error)
                        )
                    } else {
                        Toast.makeText(
                            context,
                            "getMerchantDetails Failed ${result.Code} ${result.Msg}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }*/
        }

    }

}