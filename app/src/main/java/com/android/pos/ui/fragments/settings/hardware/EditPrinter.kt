package com.android.pos.ui.fragments.settings.hardware

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.entities.TbOrderType
import com.android.pos.data.model.PrinterListModel
import com.android.pos.data.model.responseModel.PrinterResponse
import com.android.pos.data.remote.Constants.CUSTOMER
import com.android.pos.data.remote.Constants.KITCHEN
import com.android.pos.data.remote.Constants.KITCHENANDCUSTOMER
import com.android.pos.data.remote.Constants.LOCATION_ID
import com.android.pos.data.remote.Constants.TERMINAL_ID
import com.android.pos.data.remote.Constants.createRequestModelForUpdatePrinter
import com.android.pos.data.remote.Constants.createRequestModelForUpdatePritnerType
import com.android.pos.databinding.FragmentEditPrinterBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.EditPrinterListAdapter
import com.android.pos.ui.fragments.settings.hardware.printer.PrinterViewModel
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.statusUtils.Status
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class EditPrinter : Fragment() {
    private var oldOrderTypes: List<PrinterResponse.Data.OrderTypes> = arrayListOf()
    lateinit var binding: FragmentEditPrinterBinding
    private lateinit var adapter: EditPrinterListAdapter
    private var printerModel: PrinterListModel? = null
    private val viewModel by viewModels<PrinterViewModel>()
    private lateinit var arrayAdapter: ArrayAdapter<String>
    private var list = arrayListOf<String>(CUSTOMER, KITCHEN, KITCHENANDCUSTOMER)
    private var type = ""
    var orderTypeList: ArrayList<TbOrderType> = arrayListOf()

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
        arrayAdapter =
            ArrayAdapter(binding.root.context, android.R.layout.simple_spinner_item, list)
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        observeShowProgress()
        updateDate()
        getOrderTypes()
        setUpHeader()
        return binding.root
    }

    private fun setUpHeader() {
        binding.header.txtTitle.text=getString(R.string.edit_printers)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = EditPrinterListAdapter()


        binding.rvPrinterList.adapter = adapter
        printerModel = arguments?.getParcelable("printerSetting")
        setSpinnnerAdapter()
        Log.e(TAG, "SettingprinterModel:  ${Gson().toJson(printerModel)}")
        if (printerModel != null) {
            setPrinterData()
        }
        type = printerModel?.type ?: ""


        onClick()

    }

    private fun setSpinnnerAdapter() {
        binding.spnPrinterCat.adapter = arrayAdapter
        list.forEachIndexed { index, s ->
            Log.e(TAG,"gotIndexNAme ${s.lowercase()}")
            Log.e(TAG,"gotIndexNAmeType ${printerModel?.type?.lowercase()}")
            if (s.lowercase() == printerModel?.type?.lowercase()) {
                Log.e(TAG,"gotindex:  $index")
                binding.spnPrinterCat.setSelection(index)
            }
        }
        binding.spnPrinterCat.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {

                val selectedValue = arrayAdapter.getItem(position)
                var oderTypes: ArrayList<PrinterResponse.Data.OrderTypes> = arrayListOf()
                var dataList: ArrayList<PrinterResponse.Data.PrinterSettings> =
                    arrayListOf()
                type = selectedValue.toString()

                when (selectedValue) {
                    KITCHEN -> {
                        orderTypeList.forEach {
                            dataList = arrayListOf()
                            dataList.add(
                                PrinterResponse.Data.PrinterSettings(
                                    orderTypeId = it.id,
                                    printType = KITCHEN,
                                    manualPrinting = false,
                                    autoPrinting = true,
                                    printerId = 0,
                                    createdAt = "",
                                    updatedAt = ""
                                )
                            )

                            oderTypes.add(
                                PrinterResponse.Data.OrderTypes(
                                    it.id,
                                    it.orderType,
                                    it.name,
                                    dataList
                                )
                            )
                        }

                    }
                    CUSTOMER -> {
                        orderTypeList.forEach {
                            dataList = arrayListOf()
                            dataList.add(
                                PrinterResponse.Data.PrinterSettings(
                                    orderTypeId = it.id,
                                    printType = CUSTOMER,
                                    manualPrinting = false,
                                    autoPrinting = true,
                                    printerId = 0,
                                    createdAt = "",
                                    updatedAt = ""
                                )
                            )

                            oderTypes.add(
                                PrinterResponse.Data.OrderTypes(
                                    it.id,
                                    it.orderType,
                                    it.name,
                                    dataList
                                )
                            )

                        }

                    }
                    KITCHENANDCUSTOMER -> {
                        orderTypeList.forEach {
                            dataList = arrayListOf()
                            dataList.add(
                                PrinterResponse.Data.PrinterSettings(
                                    orderTypeId = it.id,
                                    printType = CUSTOMER,
                                    manualPrinting = false,
                                    autoPrinting = true,
                                    printerId = 0,
                                    createdAt = "",
                                    updatedAt = ""
                                )
                            )
                            dataList.add(
                                PrinterResponse.Data.PrinterSettings(
                                    orderTypeId = it.id,
                                    printType = KITCHEN,
                                    manualPrinting = false,
                                    autoPrinting = true,
                                    printerId = 0,
                                    createdAt = "",
                                    updatedAt = ""
                                )
                            )

                            oderTypes.add(
                                PrinterResponse.Data.OrderTypes(
                                    it.id,
                                    it.orderType,
                                    it.name,
                                    dataList
                                )
                            )
                        }

                    }
                }
                Log.e(TAG, "oderTypes:  ${Gson().toJson(oderTypes)}")
                if (oderTypes.isNotEmpty()) {
                    adapter.setList(oderTypes)
                    adapter.notifyDataSetChanged()
                }


            }

            override fun onNothingSelected(p0: AdapterView<*>?) {

            }

        }
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
        oldOrderTypes = printerModel?.printerModel ?: arrayListOf()


    }

    private fun getOrderTypes() {
        viewModel.orderTypes.observe(viewLifecycleOwner, {
            when (it.status) {
                Status.LOADING -> {
                    ProgressUtils.showProgressDialog(requireActivity())
                }
                Status.ERROR -> {
                    ProgressUtils.dismissProgressDialog()
                }
                Status.SUCCESS -> {
                    ProgressUtils.dismissProgressDialog()
                    if (it.data != null) {
                        orderTypeList.clear()
                        orderTypeList = arrayListOf()
                        orderTypeList.addAll(it.data.toCollection(ArrayList()))

                    }


                }
            }
        })
    }


    private fun onClick() {
        binding.header.imgBack.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.header.txtSave.setOnClickListener {
            Log.e(TAG,"gettype:  ${type}")
            Log.e(TAG,"getPrinertype:  ${printerModel?.type}")

            if (type != printerModel?.type) {
                var tempList = adapter.getList()


                Log.e(TAG,"getDefaultModel ${Gson().toJson(printerModel?.printerModel?.toCollection(arrayListOf()))}")
                val model = createRequestModelForUpdatePritnerType(
                    printerModel?.printerModel?.toCollection(arrayListOf()),
                    tempList,
                    printerModel
                )
                model.receiptPrintType = type
                model.locationId = prefProvider.getValueInt(LOCATION_ID, 1)
                model.terminalIds = listOf(prefProvider.getValueInt(TERMINAL_ID, 0))
                model.name = binding.txtPrinterName.text.toString()

                viewModel.updatePrinter(
                    printerModel?.id!!, model
                )



            } else {

                val model = createRequestModelForUpdatePrinter(
                    adapter.getList(),
                    printerModel
                )
                model.receiptPrintType = type
                model.locationId = prefProvider.getValueInt(LOCATION_ID, 1)
                model.terminalIds = listOf(prefProvider.getValueInt(TERMINAL_ID, 0))
                model.name = binding.txtPrinterName.text.toString()

                viewModel.updatePrinter(
                    printerModel?.id!!, model
                )
            }


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