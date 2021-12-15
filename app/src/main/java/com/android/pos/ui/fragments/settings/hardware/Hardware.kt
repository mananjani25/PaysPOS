package com.android.pos.ui.fragments.settings.hardware

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.model.HardwareModel
import com.android.pos.data.remote.Constants
import com.android.pos.databinding.FragmentHardwareBinding
import com.android.pos.ui.adapter.HardwareListAdapter

class Hardware : Fragment(), HardwareListAdapter.HardwareListner {
    private lateinit var binding: FragmentHardwareBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHardwareBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setAdapter()
    }

    private fun setAdapter() {
        var list: ArrayList<HardwareModel> = arrayListOf()
        list.add(HardwareModel(0, Constants.HARDWARE_PRINTER))
        list.add(HardwareModel(0, Constants.HARDWARE_CREDIT_CARD_MACHINE))
        list.add(HardwareModel(0, Constants.HARDWARE_SCAN_GUN))
        list.add(HardwareModel(0, Constants.HARDWARE_TERMINAL))
        list.add(HardwareModel(0, Constants.HARDWARE_KITCHEN_DISPLAY))
        val adapter = HardwareListAdapter(requireContext(), list)
        adapter.setListner(this)
        binding.rvHardwareList.adapter = adapter

    }

    override fun onITemClicked(itemName: String) {

        when (itemName) {
            Constants.HARDWARE_PRINTER -> {
                findNavController().navigate(R.id.action_settings_to_printer)
            }
            Constants.HARDWARE_SCAN_GUN -> {
                findNavController().navigate(R.id.action_settings_to_scannerListFragment)
            }
        }
    }
}