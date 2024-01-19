package com.pays.pos.ui.fragments.settings.hardware.scangun

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.pays.pos.databinding.FragmentScannerResetBinding
import com.pays.pos.ui.activities.MainActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ScannerResetFragment : Fragment() {

    private lateinit var binding: FragmentScannerResetBinding
    private val TAG = "ScannerResetFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentScannerResetBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initControls()
    }

    private fun initControls() {
        binding.nsvScanner.isNestedScrollingEnabled = false

        binding.imgBack.setOnClickListener {
            findNavController().navigateUp()
        }

        //get all connected bluetooth devices
        //updateScannerListView(CREATE)

        if ((activity as MainActivity).requestLocationPermissions()) {
            initScanner()
        }
        (activity as MainActivity).getRequestCallBack {
            initScanner()
        }
    }

    private fun initScanner() {
        //show barcode view
        (activity as MainActivity).generatePairingBarcode(binding.flBarCode)
        (activity as MainActivity).getSnapiBarcode(binding.snapiBarcode)

        //generate reset connection barcode
        (activity as MainActivity).resetConnectionThroughBarcode(binding.flBarCodeReset)
    }


}