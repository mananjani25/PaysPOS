package com.android.pos.ui.fragments.magtekPro

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.pos.R
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.DYNANA_FLAX
import com.android.pos.databinding.FragmentTagtekBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.fragments.magtek.MagtekViewModel
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.LogUtil
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.callback.ItemCallback
import com.android.pos.utils.extensions.runOnUiThread
import com.magtek.mobile.android.mtusdk.*
import com.pax.poslink.*
import com.android.pos.utils.paxUtils.AppThreadPool
import com.android.pos.utils.paxUtils.Convenience
import com.android.pos.utils.paxUtils.POSLinkCreatorWrapper
import com.android.pos.utils.paxUtils.SettingINI
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class MagtekProFragment : Fragment(), ItemCallback, IDeviceListCallback {

    private var device: IDevice? = null
    private lateinit var mContaxt: Context
    private var adapter: MagtakProAdapter? = null
    private lateinit var binding: FragmentTagtekBinding
    private val viewModel by viewModels<MagtekViewModel>()

    lateinit var commSetting: CommSetting
    private lateinit var mPaymentRequest: PaymentRequest
    private var posLink: PosLink = PosLink()

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
            LayoutInflater.from(requireContext()),
            R.layout.fragment_tagtek,
            container,
            false
        )
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        binding.tvPax.setOnClickListener {
            initPOSLink()
            setCommSetting()
            getMerchantDetails()
        }

        mSessionManager.setDevicesFragment(this)

        setup()
        setupAdapter()
        syncDevices()

        return binding.root
    }

    private fun initPOSLink() {
        POSLinkCreatorWrapper.createSync(
            context!!,
            object : AppThreadPool.FinishInMainThreadCallback<PosLink?> {
                override fun onFinish(result: PosLink?) {
                    posLink = result!!
                }
            })
    }

    private fun setCommSetting() {
        //create commsetting object
        val iniFile =
            activity!!.applicationContext.filesDir.absolutePath + "/" + SettingINI.FILENAME
        val commset: CommSetting = SettingINI.getCommSettingFromFile(iniFile)

        //initialization value  for comsetting's attribute
        commset.type = CommSetting.TCP
        commset.timeOut = "3000"
        commset.baudRate = "9600"
        commset.isEnableProxy = false
        commset.destPort = "10009"
        commset.destIP = "192.168.7.160"
        val selectedHost = "UNKNOWN"
        Convenience.setHost(context, commset, selectedHost)
        Log.i(
            "TAG", "coms.CommType = " + commset.type + "; coms.TimeOut=" + commset.timeOut
                    + "; SerialPort=" + commset.serialPort + "; coms.BaudRate=" + commset.baudRate
                    + "; coms.DestIP=" + commset.destIP + "; coms.DestPort=" + commset.destPort
                    + "; coms.MacAddr=" + commset.macAddr + "; coms.EnableProxy=" + commset.isEnableProxy
        )
        POSLinkAndroid.initPOSListener(context, commset)
        SettingINI.saveCommSettingToFile(iniFile, commset)
        // set the folder to save the "comsetting.ini" file
        posLink.appDataFolder = context!!.filesDir.absolutePath
        posLink.SetCommSetting(commset)
    }

    // Get merchant details from pax
    private fun getMerchantDetails() {
        GlobalScope.launch {
            val manageRequest = ManageRequest()
            manageRequest.TransType = manageRequest.ParseTransType("GETVAR")
            manageRequest.EDCType = manageRequest.ParseEDCType("CREDIT")
            manageRequest.VarName = "MID"
            manageRequest.ContactlessEntryFlag = "1"

            posLink.ManageRequest = manageRequest
            val result = posLink.ProcessTrans()
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(requireContext(), "result: ${result.Code} ${result.Msg}", Toast.LENGTH_SHORT).show()
            }
            Log.d("result: ", result.Code.toString() + " " + result.Msg)
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
                    Toast.makeText(requireContext(), "Merchant ID: $mID", Toast.LENGTH_SHORT).show()
                }
                Log.d("Merchant Details: ", mID + " " + resultCode + "  " + status)
            }else{
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(requireContext(), "getMerchantDetails Failed ${result.Code} ${result.Msg}", Toast.LENGTH_SHORT).show()
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
                    Manifest.permission.ACCESS_FINE_LOCATION
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
                findNavController().navigate(R.id.action_magtekProFragment_to_dashboardCategoryBoldPOS)
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


}