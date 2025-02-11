package com.pays.pos.ui.dialog

import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.pays.pos.R
import com.pays.pos.data.model.MergeFloorModel
import com.pays.pos.data.model.MergeTableListModel
import com.pays.pos.data.model.MergeTableModel
import com.pays.pos.data.model.requestModel.OrderAttributeRequestModel
import com.pays.pos.data.model.responseModel.GetFloorPlanDetailResponse
import com.pays.pos.data.model.responseModel.GetOrderDetailsResponse
import com.pays.pos.data.remote.Constants.AVAILABLE
import com.pays.pos.data.remote.Constants.EMPLOYEE_ID
import com.pays.pos.data.remote.Constants.OCCUPIED
import com.pays.pos.databinding.DialogMergeTableSelectionBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.ui.adapter.MergeTableSelectionAdapter
import com.pays.pos.ui.fragments.dinein.DineInViewModel
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.ProgressUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MergeTableDialog : DialogFragment() {
    private lateinit var binding: DialogMergeTableSelectionBinding
    private val list: ArrayList<MergeTableListModel> = arrayListOf()
    private lateinit var adapter: MergeTableSelectionAdapter
    private var listFloorPlan: ArrayList<GetFloorPlanDetailResponse.Data>? = null
    private lateinit var tableAdapter: ArrayAdapter<MergeTableModel>
    private lateinit var floorAdapter: ArrayAdapter<MergeFloorModel>
    private val viewModel by viewModels<DineInViewModel>()
    private val TAG = "MergeTableDialog"
    private var tableSelectedPos: Int = 0
    private var listOrdersMerged: ArrayList<GetOrderDetailsResponse.Data> = arrayListOf()
    private var orderList: ArrayList<GetFloorPlanDetailResponse.OrderDetails> = arrayListOf()

    var listTable: ArrayList<MergeTableModel> = arrayListOf()
    var listFloor: ArrayList<MergeFloorModel> = arrayListOf()

    @Inject
    lateinit var prefProvider: PrefProvider

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.dialog_merge_table_selection,
            container,
            false
        )
        observeMergeTable()
        observeShowProgress()

        dialog?.setCanceledOnTouchOutside(false)
        return binding.root
    }

    private fun observeMergeTable() {
        viewModel.mergeStatusChange.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { status ->
                AlertUtils.showCustomAlertWithListenerWithOK(
                    requireContext(), status.toString()
                ) { _, _ ->
                    val bundle = Bundle()
                    bundle.putBoolean("merge_done", true)
                    setFragmentResult("request_key_table_selection", bundle)
                    val navController = findNavController()
                    navController.popBackStack()
                }


            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = MergeTableSelectionAdapter()

        setData()
        onClick()


    }

    private fun setData() {
        listFloorPlan = requireArguments().getParcelableArrayList("floorList")
        listFloor = arrayListOf()
        listTable = arrayListOf()


        listFloor.add(MergeFloorModel(0, "Select Floor"))
        listTable.add(MergeTableModel(0, "Select Table", 0, "Select Floor", false, 0, null, 0))
        listFloorPlan?.forEach {
            listFloor.add(MergeFloorModel(it.id, it.name))

            it.floor_plan_tables.forEach { table ->
                if (table.status == AVAILABLE) {
                    listTable.add(
                        MergeTableModel(
                            table.id,
                            table.table_name,
                            it.id,
                            it.name,
                            if (table.status == OCCUPIED) {
                                true
                            } else {
                                false
                            },
                            orderId = if (table.order_details != null) {
                                table.order_details.id
                            } else {
                                null
                            },
                            orderDetails = table.order_details,
                            chairCount = table.chair_count
                        )
                    )

                } else if (table.lock_by_id != null && table.lock_by_id == prefProvider.getValueInt(
                        EMPLOYEE_ID, 0
                    )
                ) {

                    if (table.status == OCCUPIED) {
                        listTable.add(
                            MergeTableModel(
                                table.id,
                                table.table_name,
                                it.id,
                                it.name,
                                if (table.status == OCCUPIED) {
                                    true
                                } else {
                                    false
                                },
                                orderId = if (table.order_details != null) {
                                    table.order_details.id
                                } else {
                                    null
                                },
                                orderDetails = table.order_details,
                                chairCount = table.chair_count
                            )
                        )
                    }
                }
            }
        }

        list.add(MergeTableListModel(listTable, listFloor))

        binding.rvTableList.adapter = adapter
        adapter.setList(list)

        tableAdapter = ArrayAdapter(
            binding.root.context,
            R.layout.spinner_text_selected,
            arrayListOf()
        )
        floorAdapter = ArrayAdapter(
            binding.root.context,
            R.layout.spinner_text_selected,
            list.get(0).listFloorPlan
        )
        tableAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        floorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//        binding.spnFloorName.adapter = floorAdapter
        binding.dropdownSpinnerFloor?.adapter = floorAdapter
//        binding.spnTableName.adapter = tableAdapter
        binding.dropdownSpinnerTable?.adapter = tableAdapter

        binding.dropdownSpinnerTable?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                tableSelectedPos = position
                Log.e(TAG, "tableSelectedPos:  ${tableSelectedPos}")
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

        }

        binding.dropdownSpinnerFloor?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                var tempTableList: ArrayList<MergeTableModel> = arrayListOf()
                list.get(0).listFloorPlan.get(position).id
                tempTableList = list.get(0).listTable.filter { it ->
                    it.floorId == list.get(0).listFloorPlan.get(position).id
                }.toCollection(arrayListOf())
                var sortedlist = tempTableList.toList().sortedBy { it.id }
                tempTableList = ArrayList(sortedlist)
                tableAdapter.clear()
                tableAdapter.addAll(tempTableList)
                tableAdapter.notifyDataSetChanged()

            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

        }

    }

    private fun onClick() {
        binding.imgBack.setOnClickListener {
            dismiss()
        }
        binding.txtAddMore.setOnClickListener {
            adapter.addItem(MergeTableListModel(listTable, listFloor))

        }
        binding.txtSave.setOnClickListener {
            var listSecondary = adapter.getList()
            Log.e(TAG, "listofSecondary  ${Gson().toJson(listSecondary.get(0))}")
            var allIds: MutableList<Int?> = mutableListOf()
            var isDuplicateIdTrue = false

            listSecondary.forEach {
                allIds.add(it.selectedTableId)
            }

            allIds.add(tableAdapter.getItem(tableSelectedPos)?.id)

            var set: MutableList<Int> = mutableListOf()
            allIds.forEach {
                if (set.contains(it)) {
                    isDuplicateIdTrue = true
                    return@forEach
                } else {
                    set.add(it ?: 0)

                }

            }
            LogUtil.logE(TAG, "getSetData  ${Gson().toJson(set)}")


            if (!isDuplicateIdTrue) {
                var primaryTable = tableAdapter.getItem(tableSelectedPos)
                var totalChairCount = 0


                var listSecondaryOrderDetails: ArrayList<GetFloorPlanDetailResponse.OrderDetails> =
                    arrayListOf()

                val parentTableId = tableAdapter.getItem(tableSelectedPos)?.id
                var childIds: String = ""
                var arrayChildIds: ArrayList<String> = arrayListOf()

                Log.e(TAG, "listSecondaryData  ${listSecondary.size}")
                for (i in 0 until listSecondary.size) {
                    arrayChildIds.add(listSecondary.get(i).selectedTableId.toString())
                    Log.e(
                        TAG, "listSecondarychairCount:  ${
                            listSecondary.get(i).listTable.get(
                                listSecondary.get(i).tableSelectedPosition ?: 0
                            ).name
                        }"
                    )



                    if (listSecondary[i].orderDetails == null) {
                        totalChairCount += listSecondary.get(i).secondaryChairCount ?: 0
                    }
                }
                childIds = android.text.TextUtils.join(",", arrayChildIds)


                var tableMergeList: ArrayList<MergeTableModel> = arrayListOf()
                Log.e(TAG, "totalChairCount:   ${totalChairCount}")
                for (i in 0 until totalChairCount) {

                    tableMergeList.add(
                        MergeTableModel(
                            id = adapter.getList().get(0).tableSelectedPosition?.let { it1 ->
                                adapter.getList().get(0).listTable.get(
                                    it1
                                ).id
                            }!!, name = "", floorId = 0, floorName = ""
                        )
                    )
                }




                listSecondary.add(
                    MergeTableListModel(
                        listTable = listTable,
                        listFloor,
                        tableSelectedPosition = tableSelectedPos,
                        orderDetails = primaryTable?.orderDetails
                    )
                )

                for (i in 0 until listSecondary.size) {
                    if (listSecondary[i].orderDetails != null) {
                        listSecondary[i].orderDetails?.let { it1 ->
                            listSecondaryOrderDetails.add(
                                it1
                            )
                        }
                    }

                }
                LogUtil.logE(
                    TAG,
                    "listSecondaryOrderDetailsSize:  ${listSecondaryOrderDetails.size}"
                )

                if (listSecondaryOrderDetails.size == 0) {
                    //This is for Every Empty Table for both Primary and Secondary
                    var childzero = false
                    childIds.forEach {
                        if (it.toString() != "0") {
                            childzero = true
                            return@forEach
                        }
                    }
                    if (parentTableId != 0 && childzero) {
                        parentTableId?.let { it1 ->
                            viewModel.mergeTable(
                                it1,
                                childIds,
                                null,
                                null
                            )
                        }
                    } else {
                        AlertUtils.showCustomAlertWithListenerWithOK(
                            requireContext(), "Please select Floor and Table"
                        ) { _, _ ->

                        }

                    }


                } else if (listSecondaryOrderDetails.size == 1) {
                    //One Occupied and Other's Available

                    var orderModel: OrderAttributeRequestModel = OrderAttributeRequestModel()
                    orderModel = viewModel.createMergeOrderRequest(
                        listSecondaryOrderDetails.get(0),
                        tableMergeList
                    )
                    var childzero = false
                    childIds.forEach {
                        if (it.toString() != "0") {
                            childzero = true
                            return@forEach
                        }
                    }

                    if (parentTableId != 0 && childzero) {
                        viewModel.mergeTable(
                            parentTableId ?: 0,
                            childIds,
                            orderModel = orderModel,
                            orderId = orderModel.id
                        )
                    } else {
                        AlertUtils.showCustomAlertWithListenerWithOK(
                            requireContext(), "Please select Floor and Table"
                        ) { _, _ ->

                        }
                    }
                } else {
                    //Multiple Occupied and Other's Available
                    var orderModel =
                        viewModel.createMultipleMergeOrder(
                            listSecondaryOrderDetails,
                            tableMergeList
                        )
                    var mergedChildsOrderIds = ""
                    for (i in 0 until listSecondaryOrderDetails.size) {
                        if (listSecondaryOrderDetails.get(i).id != primaryTable?.orderId) {

                            mergedChildsOrderIds += listSecondaryOrderDetails.get(i).id
                            if (i != listSecondaryOrderDetails.size - 1) {
                                mergedChildsOrderIds += ","

                            }

                        }
                    }
                    var listOrderIds: ArrayList<String> = arrayListOf()
                    listSecondaryOrderDetails.forEach {
                        listOrderIds.add(it.id.toString())
                    }
                    var mergedOrderIds = android.text.TextUtils.join(",", listOrderIds)

                    var childzero = false
                    childIds.forEach {
                        if (it.toString() != "0") {
                            childzero = true
                            return@forEach
                        }
                    }
                    if (parentTableId != 0 && childzero) {
                        viewModel.mergeTable(
                            parentTableId ?: 0,
                            childIds,
                            mergedOrderIds,
                            orderModel
                        )
                    } else {
                        AlertUtils.showCustomAlertWithListenerWithOK(
                            requireContext(), "Please select Floor and Table"
                        ) { _, _ ->

                        }

                    }


                }


            } else if (isDuplicateIdTrue) {
                var valdate = false
                for (i in allIds.indices) {
                    if (allIds[i] == 0) {
                        valdate = true
                    }
                }
                if (valdate) {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        requireContext(), "Please select Floor and Table"
                    ) { _, _ ->

                    }
                } else {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        requireContext(), "Same table can't be merged."
                    ) { _, _ ->

                    }
                }


            }
        }

    }


    override fun onResume() {
        super.onResume()

        val window: Window? = dialog!!.window
        val size = Point()
        val display: Display = window?.windowManager?.defaultDisplay!!
        display.getSize(size)
        val width: Int = size.x
        window.setLayout((width * 0.60).toInt(), WindowManager.LayoutParams.MATCH_PARENT)
        window.setGravity(Gravity.CENTER)
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
}