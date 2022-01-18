package com.android.pos.ui.fragments.settings.hardware

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.data.model.PrinterListModel
import com.android.pos.data.remote.Constants.LOCATION_ID
import com.android.pos.data.remote.Constants.TERMINAL_ID
import com.android.pos.data.remote.Constants.createRequestModelForUpdatePrinter
import com.android.pos.databinding.FragmentEditPrinterBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.EditPrinterListAdapter
import com.android.pos.ui.fragments.settings.hardware.printer.PrinterViewModel
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.ProgressUtils
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class EditPrinter : Fragment() {
    lateinit var binding: FragmentEditPrinterBinding
    private lateinit var adapter: EditPrinterListAdapter
    private var printerModel: PrinterListModel? = null
    private val viewModel by viewModels<PrinterViewModel>()

    @Inject
    lateinit var prefProvider: PrefProvider
    private val TAG = "EditPrinter"
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentEditPrinterBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        observeShowProgress()
        updateDate()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = EditPrinterListAdapter()
        binding.rvPrinterList.adapter = adapter
        printerModel = arguments?.getParcelable("printerSetting")
        Log.e(TAG, "SettingprinterModel:  ${Gson().toJson(printerModel)}")
        if (printerModel != null) {
            setPrinterData()
        }

        onClick()

    }

    private fun setPrinterData() {
        binding.txtPrinterName.setText(printerModel?.printerName)
        binding.txtMacAddress.setText(printerModel?.deviceModel?.macAddress)
        binding.txtPrntType.setText(printerModel?.type)
        binding.txtPrntModel.setText(printerModel?.deviceModel?.deviceName)
        printerModel?.printerModel?.toCollection(ArrayList())?.let {
            Log.e(TAG, "OrderTYpeListSize  " + it.size)
            adapter.setList(it)
        }


    }


    private fun onClick() {
        binding.imgClose.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.txtSave.setOnClickListener {


            val model = createRequestModelForUpdatePrinter(
                adapter.getList(),
                printerModel
            )
            model.locationId = prefProvider.getValueInt(LOCATION_ID, 1)
            model.terminalIds = listOf(prefProvider.getValueInt(TERMINAL_ID, 0))
            model.name = binding.txtPrinterName.text.toString()

            viewModel.updatePrinter(
                printerModel?.id!!, model
            )


        }
    }

    private fun observeShowProgress() {

        viewModel.showProgress.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        })
    }

    private fun updateDate() {
        viewModel.updatePrinter.observe(requireActivity(), {
            it.getContentIfNotHandled()?.let { data ->
                activity?.let {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        it, data.toString()
                    ) { _, _ ->
                        val navController = findNavController()
                        navController.popBackStack()
                    }
                }

            }
        })
    }
}