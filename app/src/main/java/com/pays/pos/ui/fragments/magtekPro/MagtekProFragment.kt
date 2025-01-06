package com.pays.pos.ui.fragments.magtekPro

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.pays.pos.R
import com.pays.pos.data.model.responseModel.PosLinkResult
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.DYNANA_FLAX
import com.pays.pos.data.remote.Constants.MANUAL_SALE
import com.pays.pos.data.remote.Constants.REDIRECT_FROM
import com.pays.pos.databinding.FragmentTagtekBinding
import com.pays.pos.di.ApiModule2
import com.pays.pos.di.PrefProvider
import com.pays.pos.ui.fragments.magtek.MagtekViewModel
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.ProgressUtils
import com.pays.pos.utils.callback.ItemCallback
import com.pays.pos.utils.extensions.runOnUiThread
import com.pays.pos.utils.paxUtils.AppThreadPool
import com.pays.pos.utils.paxUtils.POSLinkCreatorWrapper
import com.pays.pos.utils.paxUtils.SettingINI
import com.magtek.mobile.android.mtusdk.*
import com.pax.poslink.*
import com.pax.poslink.broadpos.BroadPOSCommunicator
import com.pax.poslink.log.LogFilter.Const
import com.pays.pos.MainApplication
import com.pays.pos.data.entities.ActivePaymentGateway
import com.pays.pos.utils.Event
import com.pays.pos.utils.extensions.gone
import com.pays.pos.utils.extensions.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject


@AndroidEntryPoint
class MagtekProFragment : Fragment(), ItemCallback, IDeviceListCallback {

    private var device: IDevice? = null
    private lateinit var mContaxt: Context
    private var adapter: MagtakProAdapter? = null
    private lateinit var binding: FragmentTagtekBinding
    private val viewModel by viewModels<MagtekViewModel>()
    private var builder: Dialog? = null
    private val paymentGatewayTypeViewModel by activityViewModels<PaymentGatewayTypeViewModel>()

    lateinit var commSetting: CommSetting
    private lateinit var mPaymentRequest: PaymentRequest
    private var posLink: PosLink = PosLink()

    @Inject
    lateinit var apiModule2: ApiModule2

    @Inject
    lateinit var prefProvider: PrefProvider

    @Inject
    lateinit var mSessionManager: SessionManager

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                refreshList()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            LayoutInflater.from(context),
            R.layout.fragment_tagtek,
            container,
            false
        )
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        setProgressObservers()
        initPaymentGateway()
        binding.tvPax.setOnClickListener {
//            initPOSLink()
//            connectBP()
//            paxNetworkCall()
            context?.let { it1 -> viewModel.initPOSLink(it1) }

        }

        binding.tvDisconnectPax.setOnClickListener {
//            BroadPOSCommunicator.getInstance(activity).stopListeningService()
            prefProvider.setValueboolean(Constants.IS_PAX_CONNECTED, false)
            binding.tvDisconnectPax.visibility = View.GONE
            binding.tvPax.visibility = View.VISIBLE
        }

        /*if (prefProvider.getValueboolean(Constants.IS_PAX_CONNECTED, false)) {
            binding.tvDisconnectPax.visibility = View.VISIBLE
            binding.tvPax.visibility = View.GONE
        } else {
            binding.tvDisconnectPax.visibility = View.GONE
            binding.tvPax.visibility = View.VISIBLE
        }*/
        initPaxButtonsVisibility()

        mSessionManager.setDevicesFragment(this)

        setup()
        setupAdapter()
        syncDevices()
        getMerchantDataObserver()

        return binding.root
    }

    var coroutineExceptionHandler= CoroutineExceptionHandler{_,exception->
        with(binding){
            runOnUiThread(Runnable {
                tvPax.visible()
                tvDisconnectPax.gone()
            })
        }
    }
    var paymentTypeCoroutine= CoroutineScope(Dispatchers.IO + coroutineExceptionHandler)

    private fun initPaymentGateway() {
        val activePaymentTypeObserver=object:Observer<List<ActivePaymentGateway>>{
            override fun onChanged(t: List<ActivePaymentGateway>?) {
                t?.let {
                    if (it.isNotEmpty()){
                        prefProvider.setValue(Constants.PAYMENT_GATEWAY_TYPE,it.get(0).type.toString())
                        setPaxButtonVisibility(it.get(0).type)
                    }
                }
            }
        }
        if (!prefProvider.getValue(Constants.PAYMENT_GATEWAY_TYPE,"").isEmpty()){
            setPaxButtonVisibility(prefProvider.getValue(Constants.PAYMENT_GATEWAY_TYPE,""))
        }else{
            paymentGatewayTypeViewModel.activePaymentGatewayObservable.removeObserver(activePaymentTypeObserver)

            paymentTypeCoroutine.launch {
                paymentGatewayTypeViewModel.activePaymentGatewayObservable.observe(viewLifecycleOwner, activePaymentTypeObserver)

                paymentGatewayTypeViewModel.getActivePaymentGatewayType()
            }
        }


    }

    private fun setPaxButtonVisibility(type: String?) {
        if (type.equals(Constants.PAX)) {
            initPaxButtonsVisibility()
        } else {
//            runOnUiThread(Runnable {
            Handler(Looper.getMainLooper()).postDelayed(Runnable {
                with(binding) {
                 tvPax.visibility = View.GONE
                 tvDisconnectPax.visibility = View.GONE
                }
            },500)

//            })
        }
    }
    private fun initPaxButtonsVisibility() {
        if (prefProvider.getValueboolean(Constants.IS_PAX_CONNECTED, false)) {
            binding.tvDisconnectPax.visibility = View.VISIBLE
            binding.tvPax.visibility = View.GONE
        } else {
            binding.tvDisconnectPax.visibility = View.GONE
            binding.tvPax.visibility = View.VISIBLE
        }
    }

    private fun setProgressObservers() {
        viewModel.progressDialog.observe(viewLifecycleOwner,object:Observer<Event<Boolean>?>{
            override fun onChanged(t: Event<Boolean>?) {
                t?.let {
                    it.getContentIfNotHandled()?.let {
                        if (it){
                            showProgressDialog("Connecting to PAX", requireContext(), View.GONE)
                        }else{
                            dismissProgressDialog()
                        }
                    }
                }
            }

        })
    }

    private fun getMerchantDataObserver() {
        viewModel.merchantData.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { response ->
                Log.d("merchantData: ","merchantData observe")
                val resultCode = response.resultCode
                val status = response.resultTxt
                val mID = response.VarValue
                prefProvider.setValue(
                    Constants.MERCHANT_ID,
                    mID
                )
                CoroutineScope(Dispatchers.Main).launch {
                    ProgressUtils.dismissProgressDialog()
                    try {
                        AlertUtils.showCustomAlert(requireContext(), "Merchant $mID is connected successfully")
                    } catch (e: Exception) {
                        delay(500)
                        AlertUtils.showCustomAlert(requireActivity(), "Merchant $mID is connected successfully")
                    }
                    binding.tvDisconnectPax.visibility = View.VISIBLE
                    binding.tvPax.visibility = View.GONE
                }
                prefProvider.setValueboolean(Constants.IS_PAX_CONNECTED, true)
                Log.d("Merchant Details: ", mID + " " + resultCode + "  " + status)
            }
        }
    }

    /*Added by Rahul to solve the crash issue, when PAX is connected - START*/
    override fun onPause() {
        super.onPause()
        try{
            ProgressUtils.dismissProgressDialog()
        }catch (e:Exception){
            if (activity!=null) {
                ProgressUtils.dismissProgressDialog()
            }
        }
    }
    /*Added by Rahul to solve the crash issue, when PAX is connected - END*/


    private fun connectBP() {
        BroadPOSCommunicator.getInstance(activity)
            .startListeningService(object : BroadPOSCommunicator.StartListenerCallBack {
                override fun onSuccess() {
                    Toast.makeText(context, "Successful StartListenerCallBack", Toast.LENGTH_SHORT)
                        .show()
                }

                override fun onFail(msg: String) {
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun paxNetworkCall() {
        ProgressUtils.showProgressDialog("Connecting to PAX", requireActivity())
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
                    setCommSetting(ipAddress, port.toString())
                    getMerchantDetails()
//                    connectBP()
                }
            }

            override fun onFailure(
                call: Call<PosLinkResult>,
                t: Throwable
            ) {

                ProgressUtils.dismissProgressDialog()

                AlertUtils.showCustomAlert(requireContext(), t.message)
                Log.d("onFailure: ", "Message-> ${t.message}")
            }
        })
    }


    private fun initPOSLink() {
        POSLinkCreatorWrapper.createSync(
            context!!,
            object : AppThreadPool.FinishInMainThreadCallback<PosLink?> {
                override fun onFinish(result: PosLink?) {
                    posLink = result!!
                    Log.d("initPOSLink: ", "onFinish")
                }
            })
    }

    private fun setCommSetting(edtIP: String, edtPort: String) {
        //create commsetting object

        var file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val iniFile = "/storage/emulated/0/Download/" + SettingINI.FILENAME
        /*val iniFile =
            activity!!.applicationContext.filesDir.absolutePath + "/" + SettingINI.FILENAME*/
        val commset: CommSetting = SettingINI.getCommSettingFromFile(context!!,iniFile)
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
        SettingINI.saveCommSettingToFile(context!!, iniFile, commset)
        // set the folder to save the "comsetting.ini" file
        posLink.appDataFolder = file.absolutePath
        posLink.SetCommSetting(commset)
        Log.d("SetCommSetting: ", "saved successfully")
    }

    // Get merchant details from pax
    private fun getMerchantDetails() {
        GlobalScope.launch {
            Log.d("manageRequest ", "Start")
            val manageRequest = ManageRequest()
//            manageRequest.TransType = manageRequest.ParseTransType("INIT")
            manageRequest.TransType = manageRequest.ParseTransType("GETVAR")
            manageRequest.EDCType = manageRequest.ParseEDCType("CREDIT")
            manageRequest.VarName = "MID"
            manageRequest.ContactlessEntryFlag = "1"

            posLink.ManageRequest = manageRequest
            val result = posLink.ProcessTrans()
            Log.d("result: ", result.Code.toString() + " Msg: " + result.Msg)
            if (result.Code === ProcessTransResult.ProcessTransResultCode.OK) {
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
                    AlertUtils.showCustomAlert(
                        requireContext(),
                        "Merchant $mID is connected successfully"
                    )
                    binding.tvDisconnectPax.visibility = View.VISIBLE
                    binding.tvPax.visibility = View.GONE
                }
//                getPaymentResponse()
                prefProvider.setValueboolean(Constants.IS_PAX_CONNECTED, true)
                Log.d("Merchant Details: ", mID + " " + resultCode + "  " + status)
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    ProgressUtils.dismissProgressDialog()
                    if (result.Msg.toString() == "CONNECT ERROR" || result.Msg.toString() == "TIME OUT") {
//                        Toast.makeText(requireContext(), R.string.pax_connect_error, Toast.LENGTH_LONG).show()
                        AlertUtils.showCustomAlert(
                            requireContext(),
                            getString(R.string.pax_connect_error)
                        )
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "getMerchantDetails Failed ${result.Code} ${result.Msg}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun getPaymentResponse() {
        GlobalScope.launch {
            mPaymentRequest = PaymentRequest()
            mPaymentRequest.TransType = mPaymentRequest.ParseTransType("SALE")
            mPaymentRequest.TenderType = mPaymentRequest.ParseTenderType("CREDIT")

            mPaymentRequest.Amount = "001"
            mPaymentRequest.TipAmt = ""
            mPaymentRequest.ECRRefNum = "123441"

            posLink.PaymentRequest = mPaymentRequest
            val result = posLink.ProcessTrans()
            Log.d("result: ", result.Code.toString() + " " + result.Msg)
            if (result.Code === ProcessTransResult.ProcessTransResultCode.OK) {
                val msg = Message()
                msg.what = Constants.TRANSACTION_SUCCESSED
                msg.obj = posLink.PaymentResponse

                val response = msg.obj as PaymentResponse
                val resultCode = response.ResultCode
                val resultTxt = response.ResultTxt
                val approvedAmount = response.ApprovedAmount
                val ExtData = response.ExtData

                Log.d(
                    "Payment Details: ",
                    "$ExtData $resultCode $resultTxt"
                )
                Log.d("Payment Details: ", " Amt $approvedAmount ")
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    ProgressUtils.dismissProgressDialog()
                    Toast.makeText(
                        requireContext(),
                        "Payment Failed ${result.Code} ${result.Msg}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun syncDevices() {

        if (Build.VERSION.SDK_INT >= 23) {
            requestMultiplePermissions.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        } else {

            refreshList()
        }
    }

    private fun refreshList() {

        val deviceList: List<IDevice> = CoreAPI.getDeviceList(context, DeviceType.MMS, this)

        updateList(deviceList)
    }

    private fun updateList(deviceList: List<IDevice>) {
        adapter?.add(deviceList)

        if (prefProvider.getValueboolean(DYNANA_FLAX, false)) {
            adapter?.update(true)
        } else {
            adapter?.update(false)
        }
    }

    override fun OnDeviceList(deviceList: MutableList<IDevice>?) {

        deviceList?.let { updateList(it) }
    }

    private fun setup() {


        binding.txtRefresh.setOnClickListener {
            adapter?.clear()
            refreshList()
        }

        binding.imgBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.txtHome.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.magtekProFragment) {
                if (prefProvider.getValue(REDIRECT_FROM, "") == MANUAL_SALE) {
                    findNavController().navigate(R.id.action_magtekProFragment_to_manualSalesNew)
                } else {
                    findNavController().navigate(R.id.action_magtekProFragment_to_dashboardCategoryBoldPOS)
                }
            }
        }

        mContaxt = requireContext()


    }

    private fun setupAdapter() {

        binding.rvDeviceList.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayoutManager.VERTICAL
            )
        )

        adapter = MagtakProAdapter()
        adapter?.setCallback(this)
        binding.rvDeviceList.adapter = adapter

        if (adapter?.itemCount!! > 0) {
            val device = adapter?.getItem(0)
            if (device != null) {
                mSessionManager.device = device
            }
        }

    }


    override fun onItemClickListener(view: View?, pos: Int) {

        ProgressUtils.showProgressDialog(requireActivity())

        if (view == null) {
            mSessionManager.disconnectDevice()
        } else {
            device = adapter?.getItem(pos)
            if (device != null) {
                mSessionManager.device = device
                mSessionManager.connectDevice()
            }
        }

    }


    override fun onResume() {
        super.onResume()

        if (device != null) {
            mSessionManager.device = device
            mSessionManager.connectDevice()
        }


    }


    fun processEvent(eventType: EventType, data: IData) {
        Log.d(
            "processEvent", ": eventType=$eventType"
        )
        when (eventType) {
            EventType.ConnectionState -> {
                when (ConnectionStateBuilder.GetValue(data.StringValue())) {
                    ConnectionState.Connected -> {
                        LogUtil.logE("", "[CONNECTED]")
                        ProgressUtils.dismissProgressDialog()
                        updateUIControls(true)
                        prefProvider.setValueboolean(DYNANA_FLAX, true)

                    }
                    ConnectionState.Disconnected -> {
                        LogUtil.logE("", "[DISCONNECTED]")
                        updateUIControls(false)
                        AlertUtils.showCustomAlert(requireContext(), "DISCONNECTED")

                        ProgressUtils.dismissProgressDialog()
                        prefProvider.setValueboolean(DYNANA_FLAX, false)

                    }
                    ConnectionState.Disconnecting -> {
                        LogUtil.logE("", "[DISCONNECTING]")

                    }
                    ConnectionState.Connecting -> {
                        LogUtil.logE("", "[CONNECTING]")


                    }
                    else -> ""
                }
            }
        }
    }

    private fun updateUIControls(b: Boolean) {

        runOnUiThread {
            adapter?.update(b)
        }


    }

    fun showProgressDialog(message: String?, context: Context, showClose: Int = 0) {
        if (builder == null)
            builder = Dialog(context)

        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.view_loading, null)
        builder?.setContentView(dialogView)

        dialogView.findViewById<AppCompatTextView>(R.id.txtMessage).text = message
        val imgClose = dialogView.findViewById<AppCompatImageView>(R.id.imgClose)
        builder?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
//        builder?.window?.setBackgroundDrawable(
//            ColorDrawable(Color.WHITE)
//        )
        builder?.setCanceledOnTouchOutside(false)
        builder?.window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        imgClose.visible()
        imgClose.visibility = showClose


        try {
            if (builder != null) {
                if (!builder!!.isShowing) {
                    builder!!.show()
                }
            }
        } catch (e: java.lang.Exception) {
            Toast.makeText(context, "Loading...", Toast.LENGTH_SHORT).show()
        }


    }


    private fun dismissProgressDialog()
    {
        try {
            if (builder != null && builder?.isShowing == true) {
                builder?.dismiss()
                builder = null
            }
        } catch (e: java.lang.Exception) {
            Log.d("pos", "dismissProgressDialog: " + e.message)
        }

    }

    override fun onStop() {
        paymentTypeCoroutine.cancel()
        super.onStop()
    }

}