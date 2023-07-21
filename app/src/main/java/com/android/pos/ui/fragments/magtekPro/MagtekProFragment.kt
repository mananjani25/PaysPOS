package com.android.pos.ui.fragments.magtekPro

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.pos.R
import com.android.pos.data.remote.Constants.DYNANA_FLAX
import com.android.pos.data.remote.Constants.MANUAL_SALE
import com.android.pos.data.remote.Constants.REDIRECT_FROM
import com.android.pos.databinding.FragmentTagtekBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.fragments.magtek.MagtekViewModel
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.LogUtil
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.callback.ItemCallback
import com.android.pos.utils.extensions.runOnUiThread
import com.magtek.mobile.android.mtusdk.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class MagtekProFragment : Fragment(), ItemCallback, IDeviceListCallback {

    private var device: IDevice? = null
    private lateinit var mContaxt: Context
    private var adapter: MagtakProAdapter? = null
    private lateinit var binding: FragmentTagtekBinding
    private val viewModel by viewModels<MagtekViewModel>()

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

        mSessionManager.setDevicesFragment(this)

        setup()
        setupAdapter()
        syncDevices()


        return binding.root
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
                if(prefProvider.getValue(REDIRECT_FROM, "") == MANUAL_SALE) {
                    findNavController().navigate(R.id.action_magtekProFragment_to_manualSalesNew)
                }else {
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


}