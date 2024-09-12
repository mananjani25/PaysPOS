package com.pays.pos.ui.fragments.settings.hardware.scangun

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.pays.pos.MainApplication
import com.pays.pos.R
import com.pays.pos.data.remote.Constants
import com.pays.pos.databinding.FragmentScannerListBinding
import com.pays.pos.ui.activities.MainActivity
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.extensions.gone
import com.pays.pos.utils.extensions.visible
import dagger.hilt.android.AndroidEntryPoint

/*ALL THE SCAN GUN VARIABLES ARE COMMENTED AND MOVED TO MAINACTIVITY(for solving permission issue), PLEASE UNCOMMENT IT AND REMOVE THE VARIABLES FROM MAINACTIVITY*/

@AndroidEntryPoint
class ScannerListFragment : Fragment() {

    private lateinit var binding: FragmentScannerListBinding
    private val TAG = "ScannerListFragment"

    private val lastConnectedScannerListAdapter by lazy {
        ScannerListAdapter(isConnectedList = true) { view, availableScanner ->

            //Disconnect scanner
            (activity as MainActivity).disconnect(availableScanner.scannerId)
        }
    }
    private val availableScannerListAdapter by lazy {
        ScannerListAdapter(isConnectedList = false) { view, availableScanner ->

            // Cancel discovery because it's costly and we're about to connect
            // Get the device MAC address, which is the last 17 chars in the View
            (activity as MainActivity).connectToScanner(availableScanner)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentScannerListBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        initControls()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    private fun initControls() {
        binding.nsvScanner.isNestedScrollingEnabled = false

        setupAdapter()

        binding.txtResetDevice.setOnClickListener {
            //(activity as MainActivity).loadFragmentInSettings(fragment = ScannerResetFragment())
            findNavController().navigate(R.id.action_scannerListFragment_to_scannerResetFragment)
        }
        binding.imgBack.setOnClickListener {
            backPressManage()
        }

//        MainActivity.sdkHandler?.dcssdkSetDelegate((activity as MainActivity))
        //get all connected bluetooth devices
        (activity as MainActivity).updateScannerListView()

        if ((activity as MainActivity).requestLocationPermissions()) {
            initScanner()
        }
        (activity as MainActivity).getRequestCallBack {
            initScanner()
        }

        //back press manage
        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true /* enabled by default */) {
                override fun handleOnBackPressed() {
                    backPressManage()
                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    private fun backPressManage() {
        val navController = findNavController()
        navController.previousBackStackEntry?.savedStateHandle?.set(
            Constants.KEY,
            Constants.SCAN_GUN
        )
        navController.popBackStack()
    }

    private fun initScanner() {
        broadcastSCAisListening()
    }

    private fun setupAdapter() {
        binding.rvConnectedScanner.adapter = lastConnectedScannerListAdapter
        lastConnectedScannerListAdapter.arrayList =
            (activity as MainActivity).lastConnectedScannerList

        binding.rvOtherScanners.adapter = availableScannerListAdapter
        availableScannerListAdapter.arrayList = (activity as MainActivity).scannersList

        //get list callback
        (activity as MainActivity).notifyAdaptersCallBack =
            { connectedScanner -> notifyAdapters(connectedScanner) }
    }

    private fun broadcastSCAisListening() {
        val intent = Intent()
        intent.action = "com.pays.pos.LISTENING_STARTED"
        activity?.sendBroadcast(intent)
    }

    private fun notifyAdapters(connectedScanner: Boolean) {
        //Notify Adapters method
        binding.rvConnectedScanner.isEnabled = connectedScanner

        lastConnectedScannerListAdapter.notifyDataSetChanged()
        availableScannerListAdapter.notifyDataSetChanged()

        LogUtil.logE(TAG, "connected device list : ${lastConnectedScannerListAdapter.arrayList.size}")
        LogUtil.logE(TAG, "other device list : ${availableScannerListAdapter.arrayList.size}")

        if (lastConnectedScannerListAdapter.arrayList.isNotEmpty()) {
            //connected device list > Not Empty
            binding.rvConnectedScanner.visible()
            binding.txtConnectedScanner.visible()
            binding.rvOtherScanners.visible()
            binding.txtOtherScanner.visible()
            binding.txtNoScannerMessage.gone()
        } else if (availableScannerListAdapter.arrayList.isEmpty()) {
            //connected device list > Empty
            // other device list > Empty
            binding.rvConnectedScanner.gone()
            binding.txtConnectedScanner.gone()
            binding.rvOtherScanners.gone()
            binding.txtOtherScanner.gone()
            binding.txtNoScannerMessage.visible()
        } else {
            //connected device list > Empty
            // other device list > Not Empty
            binding.rvConnectedScanner.gone()
            binding.txtConnectedScanner.gone()
            binding.rvOtherScanners.visible()
            binding.txtOtherScanner.visible()
            binding.txtNoScannerMessage.gone()
        }
    }
}