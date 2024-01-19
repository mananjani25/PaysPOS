package com.pays.pos.ui.fragments.settings.hardware

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.pays.pos.R
import com.pays.pos.data.model.HardwareModel
import com.pays.pos.data.remote.Constants
import com.pays.pos.databinding.FragmentHardwareBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.ui.adapter.HardwareListAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class Hardware : Fragment() {
    private lateinit var binding: FragmentHardwareBinding

    @Inject
    lateinit var prefProvider: PrefProvider

    var choices = arrayOf<CharSequence>("eDynamo", "DynaFlex")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHardwareBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        setUpHeader()
        return binding.root
    }

    private fun setUpHeader() {
        binding.header.txtSave.text = getString(R.string.tv_home)
        binding.header.txtTitle.text = getString(R.string.hardware_setup)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onClick()


    }

    private fun onClick() {

        binding.header.imgBack.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.header.txtSave.setOnClickListener {
            findNavController().navigate(R.id.action_hardware_to_dashboardCategoryNew)
        }
        binding.txtPrinter.setOnClickListener {
            findNavController().navigate(R.id.action_hardware_to_printer)

        }
        binding.txtScanGun.setOnClickListener {
            findNavController().navigate(R.id.action_hardware_to_scannerListFragment)
        }
        binding.txtPrinterQueue.setOnClickListener {
            findNavController().navigate(R.id.action_hardware_to_printerQueue)
        }
        binding.txtCardMachine.setOnClickListener {
            prefProvider.setValueInt(Constants.MAGTEK_HARDWARE, 1)
            findNavController().navigate(R.id.action_hardware_to_magtekProFragment)
        }


    }

}