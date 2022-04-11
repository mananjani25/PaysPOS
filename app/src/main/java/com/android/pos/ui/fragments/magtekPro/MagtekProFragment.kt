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
import com.android.pos.databinding.FragmentTagtekBinding
import com.android.pos.ui.fragments.magtek.MagtekViewModel
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.callback.ItemCallback
import com.android.pos.utils.extensions.runOnUiThread
import com.magtek.mobile.android.mtusdk.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class MagtekProFragment : Fragment(), ItemCallback, IDeviceListCallback {

    private lateinit var mContaxt: Context
    private var adapter: MagtakProAdapter? = null
    private lateinit var binding: FragmentTagtekBinding
    private val viewModel by viewModels<MagtekViewModel>()

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
                findNavController().navigate(R.id.action_magtekFragment_to_dashboardCategoryBoldPOS)
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

        val device = adapter?.getItem(pos)
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
                        Log.e("", "[CONNECTED]")
                        updateUIControls(true)
                    }
                    ConnectionState.Disconnected -> {
                        Log.e("", "[DISCONNECTED]")
                        updateUIControls(false)
                        AlertUtils.showCustomAlert(requireContext(), "DISCONNECTED")
                    }
                    ConnectionState.Disconnecting -> {
                        Log.e("", "[DISCONNECTING]")
                    }
                    ConnectionState.Connecting -> {
                        Log.e("", "[CONNECTING]")

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