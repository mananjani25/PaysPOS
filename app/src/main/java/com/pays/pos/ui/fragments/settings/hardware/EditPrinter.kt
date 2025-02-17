package com.pays.pos.ui.fragments.settings.hardware

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
import com.pays.pos.R
import com.pays.pos.data.entities.TbOrderType
import com.pays.pos.data.model.PrinterListModel
import com.pays.pos.data.model.responseModel.PrinterResponse
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.CUSTOMER
import com.pays.pos.data.remote.Constants.KITCHEN
import com.pays.pos.data.remote.Constants.KITCHENANDCUSTOMER
import com.pays.pos.data.remote.Constants.LOCATION_ID
import com.pays.pos.data.remote.Constants.TERMINAL_ID
import com.pays.pos.data.remote.Constants.createRequestModelForUpdatePrinter
import com.pays.pos.data.remote.Constants.createRequestModelForUpdatePritnerType
import com.pays.pos.databinding.FragmentEditPrinterBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.ui.adapter.CategoryPrinterAdapter
import com.pays.pos.ui.adapter.EditPrinterListAdapter
import com.pays.pos.ui.fragments.settings.hardware.printer.PrinterViewModel
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.ProgressUtils
import com.pays.pos.utils.extensions.gone
import com.pays.pos.utils.extensions.visible
import com.pays.pos.utils.statusUtils.Status
import com.google.gson.Gson
import com.pays.pos.data.remote.Constants.LANDI_INNER_PRINTER
import com.pays.pos.data.remote.Constants.SUNMI_INNER_PRINTER
import com.pays.pos.data.remote.Constants.WIFI
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class EditPrinter : Fragment(), CategoryPrinterAdapter.CategoryPrinter {
    private var oldOrderTypes: List<PrinterResponse.Data.OrderTypes> = arrayListOf()
    lateinit var binding: FragmentEditPrinterBinding
    private lateinit var adapter: EditPrinterListAdapter
    private lateinit var categoryAdapter: CategoryPrinterAdapter
    private var printerModel: PrinterListModel? = null
    private val viewModel by viewModels<PrinterViewModel>()
    private lateinit var arrayAdapter: ArrayAdapter<String>
    private var list = arrayListOf<String>(CUSTOMER, KITCHEN, KITCHENANDCUSTOMER)
    private var type = ""
    var orderTypeList: ArrayList<TbOrderType> = arrayListOf()
    private var originalPrinterType: String = ""
    private var isFirstTimeAdapter: Boolean = false

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
        categoryAdapter = CategoryPrinterAdapter(arrayListOf(), this)

        //makeEditAableFalse()
        observeShowProgress()
        updateDate()
        getOrderTypes()
        setUpHeader()
        return binding.root
    }

    private fun makeEditAableFalse() {
        binding.txtPrinterName.isEnabled = false
    }

    private fun setUpHeader() {
        binding.header.txtTitle.text = getString(R.string.edit_printers)
        binding.header.txtSave.text = "Save"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        printerModel = arguments?.getParcelable("printerSetting")


        originalPrinterType = arguments?.getString("currentPrinterType").toString()
        adapter = EditPrinterListAdapter(originalPrinterType)
        if ((printerModel?.printerName?.toLowerCase()
                ?.contains("tsp") == true) || (printerModel?.printerName?.toLowerCase()
                ?.contains("sp") == true)
        ) {
            list.clear()
            list = arrayListOf<String>(KITCHEN)

        }
        arrayAdapter =
            ArrayAdapter(binding.root.context, android.R.layout.simple_spinner_item, list)
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        Log.e(TAG, "checkPrinterListModel  ${Gson().toJson(printerModel?.printerModel)}  ")
        Log.e(TAG, "currentPrinterType:  ${originalPrinterType}")


        binding.rvPrinterList.adapter = adapter
        binding.rvCategoriesList?.adapter = categoryAdapter

        setSpinnnerAdapter()

        setCategoryAdapter()
        if (printerModel != null) {
            setPrinterData()
        }
        type = printerModel?.type ?: ""

        if (printerModel?.type?.equals(CUSTOMER, false) == true) {
            binding.txtSelectCatPrint?.gone()
            binding.viewLineCat?.gone()
            binding.rvCategoriesList?.gone()
            binding.linearSelectAllCat?.gone()
        } else {
            binding.txtSelectCatPrint?.visible()
            binding.viewLineCat?.visible()
            binding.rvCategoriesList?.visible()
            binding.linearSelectAllCat?.visible()

        }


        onClick()
        onChecked()
        printerModel?.printerCategories?.forEach {

        }

    }

    private fun onChecked() {
        binding.chCategory?.setOnCheckedChangeListener { compoundButton, b ->
            if (compoundButton.isPressed) {
                if (b) {
                    categoryAdapter.selectAll(true)
                } else {

                    categoryAdapter.selectAll(false)
                }
            }
        }
    }

    private fun setCategoryAdapter() {
        if (printerModel != null) {

            var listCategories = printerModel?.printerCategories?.filter {
                it.categoryActive == true
            }

            var printerEnableData = listCategories?.filter {
                it.printerEnable == false
            }
            if (printerEnableData?.isEmpty() == true) {
                binding.chCategory?.isChecked = true
            } else {
                binding.chCategory?.isChecked = false
            }
            categoryAdapter.addList(listCategories?.toCollection(arrayListOf()) ?: arrayListOf())

        }

    }

    private fun setSpinnnerAdapter() {


        if (prefProvider.getValueboolean(Constants.IS_PRINTER_QUEUE_ENABLE, false)) {

            if (prefProvider.getValueboolean(Constants.IS_MASTER_TERMINAL, false)) {
                if (printerModel?.printerName?.startsWith("CloudPrint", true) == true) {
                    list.clear()
                    list.add(KITCHEN)
                } else {
                    list.clear()
                    list.add(CUSTOMER)
                }
            } else {
                list.clear()
                list.add(CUSTOMER)
            }

        }

        binding.spnPrinterCat.adapter = arrayAdapter
        list.forEachIndexed { index, s ->
            LogUtil.logE(TAG, "gotIndexNAme ${s.lowercase()}")
            LogUtil.logE(TAG, "gotIndexNAmeType ${printerModel?.type?.lowercase()}")
            if (s.lowercase() == printerModel?.type?.lowercase()) {
                LogUtil.logE(TAG, "gotindex:  $index")
                isFirstTimeAdapter = true
                binding.spnPrinterCat.setSelection(index)
            }
        }
        binding.spnPrinterCat.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {

                if (printerModel?.printerName?.contains("cloud",true) == true && printerModel?.connectionType == WIFI){
                    binding.spnPrinterCat.setSelection(1)
                    type = arrayAdapter.getItem(1).toString()

                }
                else {
                    val selectedValue = arrayAdapter.getItem(position)
                    var oderTypes: ArrayList<PrinterResponse.Data.OrderTypes> = arrayListOf()
                    var dataList: ArrayList<PrinterResponse.Data.PrinterSettings> =
                        arrayListOf()

                    if (isFirstTimeAdapter == false) {

                        when (selectedValue) {
                            KITCHEN -> {

                                var settingList = printerModel?.printerModel ?: arrayListOf()
                                settingList.forEach { it ->

                                    dataList = arrayListOf()


                                    if (originalPrinterType != KITCHENANDCUSTOMER) {
                                        it.printerSettings.forEach { it1 ->

                                            dataList.add(
                                                PrinterResponse.Data.PrinterSettings(
                                                    id = it1.id,
                                                    orderTypeId = it.orderTypeId,
                                                    printType = it1.printType,
                                                    manualPrinting = false,
                                                    autoPrinting = it1.autoPrinting,
                                                    printerId = 0,
                                                    createdAt = "",
                                                    updatedAt = "",
                                                    isDestroy = true
                                                )
                                            )

                                        }

                                        dataList.add(
                                            PrinterResponse.Data.PrinterSettings(
                                                orderTypeId = it.orderTypeId,
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
                                                orderTypeId = it.orderTypeId,
                                                orderTypeName = it.orderTypeName,
                                                orderType = it.orderType,
                                                dataList
                                            )
                                        )

                                    } else if (originalPrinterType == KITCHENANDCUSTOMER) {
                                        it.printerSettings.forEach { it1 ->

                                            dataList.add(
                                                PrinterResponse.Data.PrinterSettings(
                                                    id = it1.id,
                                                    orderTypeId = it.orderTypeId,
                                                    printType = it1.printType,
                                                    manualPrinting = false,
                                                    autoPrinting = it1.autoPrinting,
                                                    printerId = 0,
                                                    createdAt = "",
                                                    updatedAt = "",
                                                    isDestroy = true,

                                                    )
                                            )

                                        }

                                        dataList.add(
                                            PrinterResponse.Data.PrinterSettings(

                                                orderTypeId = it.orderTypeId,
                                                printType = KITCHEN,
                                                manualPrinting = false,
                                                autoPrinting = true,
                                                printerId = 0,
                                                createdAt = "",
                                                updatedAt = "",


                                                )
                                        )
                                        oderTypes.add(
                                            PrinterResponse.Data.OrderTypes(
                                                orderTypeId = it.orderTypeId,
                                                orderTypeName = it.orderTypeName,
                                                orderType = it.orderType,
                                                dataList
                                            )
                                        )


                                    }

                                }
                                /*orderTypeList.forEach {
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
                        }*/
                                type = selectedValue.toString()
                                if (oderTypes.isNotEmpty()) {
                                    Log.e(
                                        TAG,
                                        "oderTypesSettings 1:  ${oderTypes.get(0).printerSettings.size}"
                                    )
                                    adapter.setList(oderTypes)
                                    adapter.notifyDataSetChanged()
                                }

                            }

                            CUSTOMER -> {
                                if ((printerModel?.printerName?.toLowerCase()
                                        ?.contains("tsp") == false) || (printerModel?.printerName?.toLowerCase()
                                        ?.contains("sp") == false)
                                ) {
                                    var settingList = printerModel?.printerModel ?: arrayListOf()
                                    settingList.forEach { it ->

                                        dataList = arrayListOf()


                                        if (originalPrinterType != KITCHENANDCUSTOMER) {
                                            it.printerSettings.forEach { it1 ->

                                                dataList.add(
                                                    PrinterResponse.Data.PrinterSettings(
                                                        id = it1.id,
                                                        orderTypeId = it.orderTypeId,
                                                        printType = it1.printType,
                                                        manualPrinting = false,
                                                        autoPrinting = it1.autoPrinting,
                                                        printerId = 0,
                                                        createdAt = "",
                                                        updatedAt = "",
                                                        isDestroy = true
                                                    )
                                                )

                                            }


                                            dataList.add(
                                                PrinterResponse.Data.PrinterSettings(
                                                    orderTypeId = it.orderTypeId,
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
                                                    orderTypeId = it.orderTypeId,
                                                    orderTypeName = it.orderTypeName,
                                                    orderType = it.orderType,
                                                    dataList
                                                )
                                            )

                                        } else if (originalPrinterType == KITCHENANDCUSTOMER) {
                                            it.printerSettings.forEach { it1 ->

                                                dataList.add(
                                                    PrinterResponse.Data.PrinterSettings(
                                                        id = it1.id,
                                                        orderTypeId = it.orderTypeId,
                                                        printType = it1.printType,
                                                        manualPrinting = false,
                                                        autoPrinting = it1.autoPrinting,
                                                        printerId = 0,
                                                        createdAt = "",
                                                        updatedAt = "",
                                                        isDestroy = true,

                                                        )
                                                )


                                            }

                                            dataList.add(
                                                PrinterResponse.Data.PrinterSettings(

                                                    orderTypeId = it.orderTypeId,
                                                    printType = CUSTOMER,
                                                    manualPrinting = false,
                                                    autoPrinting = true,
                                                    printerId = 0,
                                                    createdAt = "",
                                                    updatedAt = "",


                                                    )
                                            )
                                            oderTypes.add(
                                                PrinterResponse.Data.OrderTypes(
                                                    orderTypeId = it.orderTypeId,
                                                    orderTypeName = it.orderTypeName,
                                                    orderType = it.orderType,
                                                    dataList
                                                )
                                            )

                                            if (printerModel?.printerName.equals(SUNMI_INNER_PRINTER) || printerModel?.printerName.equals(
                                                    LANDI_INNER_PRINTER)){


                                            }
                                        }

                                    }


                                    /* orderTypeList.forEach {
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

                         }*/
                                    type = selectedValue.toString()
                                    if (oderTypes.isNotEmpty()) {
                                        Log.e(
                                            TAG,
                                            "oderTypesSettings 2:  ${oderTypes.get(0).printerSettings.size}"
                                        )
                                        adapter.setList(oderTypes)
                                        adapter.notifyDataSetChanged()
                                    }
                                } else {
                                    val index = list.indexOf(KITCHEN)
                                    binding.spnPrinterCat.setSelection(index)
                                    AlertUtils.showCustomAlert(
                                        requireContext(),
                                        getString(R.string.incompatible_printer)
                                    )
                                }
                            }

                            KITCHENANDCUSTOMER -> {
                                if (((printerModel?.printerName?.toLowerCase()
                                        ?.contains("tsp") == false)) || ((printerModel?.printerName?.toLowerCase()
                                        ?.contains("sp") == false))
                                ) {
                                    Log.e(TAG, "checkHerePrintSelect 1")
                                    var settingList = printerModel?.printerModel ?: arrayListOf()

                                    if (originalPrinterType == KITCHENANDCUSTOMER) {
                                        settingList.forEach { it ->
                                            dataList = arrayListOf()
                                            it.printerSettings.forEach { it1 ->
                                                it1.isDestroy = false
                                                dataList.add(it1)


                                            }


                                            oderTypes.add(
                                                PrinterResponse.Data.OrderTypes(
                                                    it.orderTypeId,
                                                    it.orderType,
                                                    it.orderTypeName,
                                                    dataList
                                                )
                                            )


                                        }

                                    } else {

                                        settingList.forEach { it ->
                                            dataList = arrayListOf()
                                            it.printerSettings.forEach { it1 ->
                                                it1.isDestroy = true
                                                dataList.add(it1)


                                            }
                                            dataList.add(
                                                PrinterResponse.Data.PrinterSettings(
                                                    orderTypeId = it.orderTypeId,
                                                    printType = KITCHEN,
                                                    manualPrinting = false,
                                                    autoPrinting = true,
                                                    printerId = 0,
                                                    createdAt = "",
                                                    updatedAt = ""
                                                )
                                            )

                                            dataList.add(
                                                PrinterResponse.Data.PrinterSettings(
                                                    orderTypeId = it.orderTypeId,
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
                                                    orderTypeId = it.orderTypeId,
                                                    orderTypeName = it.orderTypeName,
                                                    orderType = it.orderType,
                                                    dataList
                                                )
                                            )


                                        }

                                    }


                                    /* orderTypeList.forEach {
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


                             }*/
                                    type = selectedValue.toString()
                                    if (oderTypes.isNotEmpty()) {
                                        Log.e(
                                            TAG,
                                            "oderTypesSettings 3:  ${oderTypes.get(0).printerSettings.size}"
                                        )
                                        adapter.setList(oderTypes)
                                        adapter.notifyDataSetChanged()
                                    }

                                } else {
                                    Log.e(TAG, "checkHerePrintSelect 2")
                                    val index = list.indexOf(KITCHEN)
                                    binding.spnPrinterCat.setSelection(index)
                                    AlertUtils.showCustomAlert(
                                        requireContext(),
                                        getString(R.string.incompatible_printer)
                                    )
                                }
                            }
                        }
                    }
                    isFirstTimeAdapter = false

                }

            }

            override fun onNothingSelected(p0: AdapterView<*>?) {

            }

        }
    }

    private fun setPrinterData() {
        if (printerModel?.printerName != "") {
            binding.txtPrinterName.setText(printerModel?.printerName)

        } else {
            printerModel?.printerName = "InnerPrinter"
            binding.txtPrinterName.setText(printerModel?.printerName)
        }
        binding.txtMacAddress.setText(printerModel?.deviceModel?.macAddress)
        binding.txtPrntType.setText(printerModel?.type)
        binding.txtPrntModel.setText(printerModel?.deviceModel?.deviceName)
        printerModel?.printerModel?.toCollection(ArrayList())?.let {
            LogUtil.logE(TAG, "OrderTYpeListSize  " + it.size)
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

            if (binding.txtPrinterName.text.trim().isEmpty()) {
                AlertUtils.showCustomAlertWithListenerWithOK(
                    requireContext(), "Printer Name can't be empty."
                ) { _, _ ->

                }

                return@setOnClickListener
            }
            val listCategories = categoryAdapter.getList()
            var listIds = ArrayList<Int>()
            listCategories.forEach {
                if (it.printerEnable && it.categoryActive) {

                    listIds.add(it.id)
                }
            }
            LogUtil.logE(TAG, "listIdslistIds  ${Gson().toJson(listIds)}")
            Log.e(TAG, "checkType: ${type}  printerModelType: ${printerModel?.type}")

            if (type != printerModel?.type) {
                var tempList = adapter.getList()


                LogUtil.logE(
                    TAG,
                    "getDefaultModel ${
                        Gson().toJson(
                            printerModel?.printerModel?.toCollection(arrayListOf())
                        )
                    }"
                )
                val model = createRequestModelForUpdatePritnerType(
                    printerModel?.printerModel?.toCollection(arrayListOf()),
                    tempList,
                    printerModel
                )
                model.receiptPrintType = type
                model.locationId = prefProvider.getValueInt(LOCATION_ID, 1)
                model.terminalIds = listOf(prefProvider.getValueInt(TERMINAL_ID, 0))
                model.modalName = binding.txtPrinterName.text.toString()
                model.categoryIds = listIds
                model.terminalId = prefProvider.getValueInt(TERMINAL_ID, 0)
                model.ip_address = printerModel?.deviceModel?.ipAddress

                viewModel.updatePrinter(
                    printerModel?.id!!, model, printerModel!!
                )


            } else {


                val model = createRequestModelForUpdatePrinter(
                    adapter.getList(),
                    printerModel
                )
                model.receiptPrintType = type
                model.locationId = prefProvider.getValueInt(LOCATION_ID, 1)
                model.terminalIds = listOf(prefProvider.getValueInt(TERMINAL_ID, 0))
                model.modalName = binding.txtPrinterName.text.toString()
                model.categoryIds = listIds
                model.terminalId = prefProvider.getValueInt(TERMINAL_ID, 0)
                model.ip_address = printerModel?.deviceModel?.ipAddress

                viewModel.updatePrinter(
                    printerModel?.id!!, model, printerModel!!
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
                        try {
                            if (findNavController().currentDestination?.id == R.id.editPrinter) {
                                val navController = findNavController()
                                navController.popBackStack()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

            }
        })

        viewModel.localUpdatePrinter.observe(requireActivity(), {
            it.getContentIfNotHandled()?.let { data ->

                if (data.currentPrinterType == KITCHEN) {

                } else {


                }


            }

        })
    }

    override fun categoryAllSelected(flag: Boolean) {

        binding.chCategory?.isChecked = flag


    }

}