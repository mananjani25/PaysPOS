package com.android.pos.ui.dialog

import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.model.MergeFloorModel
import com.android.pos.data.model.MergeTableListModel
import com.android.pos.data.model.MergeTableModel
import com.android.pos.data.model.requestModel.OrderAttributeRequestModel
import com.android.pos.data.model.responseModel.GetFloorPlanDetailResponse
import com.android.pos.data.model.responseModel.GetOrderDetailsResponse
import com.android.pos.data.remote.Constants.OCCUPIED
import com.android.pos.databinding.DialogMergeTableSelectionBinding
import com.android.pos.ui.adapter.MergeTableSelectionAdapter
import com.android.pos.ui.fragments.dinein.DineInViewModel
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.ProgressUtils
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint

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
        viewModel.mergeStatusChange.observe(viewLifecycleOwner, { event ->
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
        })
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


        listFloorPlan?.forEach {
            listFloor.add(MergeFloorModel(it.id, it.name))

            it.floor_plan_tables.forEach { table ->
                listTable.add(
                    MergeTableModel(
                        table.id, table.table_name, it.id, it.name, if (table.status == OCCUPIED) {
                            true
                        } else {
                            false
                        }, orderId = if (table.order_details != null) {
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
        binding.spnFloorName.adapter = floorAdapter
        binding.spnTableName.adapter = tableAdapter

        binding.spnTableName.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                tableSelectedPos = position
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

        }

        binding.spnFloorName.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
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
            var primaryTable = tableAdapter.getItem(tableSelectedPos)
            var totalChairCount = 0

            var listSecondary = adapter.getList()
            var listSecondaryOrderDetails: ArrayList<GetFloorPlanDetailResponse.OrderDetails> =
                arrayListOf()

            val parentTableId = tableAdapter.getItem(tableSelectedPos)?.id
            var childIds: String = ""
            for (i in 0 until listSecondary.size) {
                childIds +=
                    listSecondary.get(i).listTable.get(listSecondary.get(i).tableSelectedPosition!!).id.toString()
                if (i != listSecondary.size - 1) {
                    childIds += ","
                }

                totalChairCount += listSecondary.get(i).listTable.get(
                    listSecondary.get(i).tableSelectedPosition ?: 0
                ).chairCount
                    ?: 0
            }


            var tableMergeList: ArrayList<MergeTableModel> = arrayListOf()
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

            Log.e(TAG, "childIds:  ${childIds}")
            Log.e(TAG, "totalChairCount:  ${totalChairCount}")



            listSecondary.add(
                MergeTableListModel(
                    listTable = listTable,
                    listFloor,
                    tableSelectedPosition = tableSelectedPos
                )
            )

            for (i in 0 until listSecondary.size) {
                if (listSecondary.get(i).orderId != null) {

                    listSecondary[i]?.listTable[listSecondary[i]?.tableSelectedPosition!!]?.orderDetails?.let { it1 ->
                        listSecondaryOrderDetails.add(
                            it1
                        )
                    }
                }

            }

            if (listSecondaryOrderDetails.size == 0) {
                //This is for Every Empty Table for both Primary and Secondary
                parentTableId?.let { it1 ->
                    viewModel.mergeTable(
                        it1,
                        childIds,
                        null,
                        null
                    )
                }


            } else if (listSecondaryOrderDetails.size == 1) {
                //One Occupied and Other's Available

                var orderModel: OrderAttributeRequestModel = OrderAttributeRequestModel()
                orderModel = viewModel.createMergeOrderRequest(
                    primaryTable?.orderDetails!!,
                    tableMergeList
                )

                viewModel.mergeTable(
                    parentTableId ?: 0,
                    childIds,
                    orderModel = orderModel,
                    orderId = orderModel.id

                )

            } else {
                //Multiple Occupied and Other's Available
                var orderModel =
                    viewModel.createMultipleMergeOrder(listSecondaryOrderDetails, tableMergeList)
                /* var Mergedids =listSecondaryOrderDetails.filter {
                        it.id
                }
*/
                viewModel.mergeTable(parentTableId ?: 0, childIds)


            }


            //OLD Code
            if (listSecondary.size > 1) {
                for (i in 0 until listSecondary.size) {
                    if (listSecondary.get(i).orderId != null) {

                        listSecondary[i]?.listTable[listSecondary[i]?.tableSelectedPosition!!]?.orderDetails?.let { it1 ->
                            listSecondaryOrderDetails.add(
                                it1
                            )
                        }
                    }

                }

                if (listSecondaryOrderDetails.size == 0) {


                    //This is for Every Empty Table for both Primary and Secondary


                    parentTableId?.let { it1 ->
                        viewModel.mergeTable(
                            it1,
                            childIds,
                            null,
                            null
                        )
                    }


                } else if (listSecondaryOrderDetails.size == 1) {
                    var orderModel: OrderAttributeRequestModel = OrderAttributeRequestModel()
                    orderModel = viewModel.createMergeOrderRequest(
                        primaryTable?.orderDetails!!,
                        arrayListOf()
                    )

                }


            } else {


                var secondaryTable =
                    adapter.getList().get(0).tableSelectedPosition?.let { it1 ->
                        adapter.getList().get(0).listTable.get(
                            it1
                        )
                    }
                var listofOrderIds: ArrayList<Int> = arrayListOf()

                if (primaryTable?.orderId != null) {
                    listofOrderIds.add(primaryTable.orderId!!)

                }
                var totalChairCount = 0

                totalChairCount = adapter.getList().get(0).tableChairCount
                    ?: 0

                var tableMergeList: ArrayList<MergeTableModel> = arrayListOf()
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

                var orderModel: OrderAttributeRequestModel = OrderAttributeRequestModel()
                if (primaryTable?.orderDetails != null) {

                    if (secondaryTable?.orderDetails != null) {
                        orderModel = viewModel.mergeTwoOrders(
                            primaryOrder = primaryTable?.orderDetails!!,
                            secondaryOrder = secondaryTable.orderDetails!!
                        )


                    } else {

                        orderModel = viewModel.createMergeOrderRequest(
                            primaryTable?.orderDetails!!,
                            tableMergeList
                        )
                    }
                }


                if (primaryTable?.orderDetails != null && secondaryTable?.orderDetails != null) {
                    val mergedChildOrderIds: String =
                        primaryTable.orderDetails?.id.toString() + "," + secondaryTable.orderDetails?.id.toString()
                    Log.e(TAG, "primaryTableID  ${primaryTable.orderDetails?.id}")
                    Log.e(TAG, "secondaryTableID  ${secondaryTable.orderDetails?.id}")


                    viewModel.mergeTable(parentTableId!!, childIds, mergedChildOrderIds, orderModel)
                } else if (primaryTable?.orderDetails != null) {
                    parentTableId?.let { it1 ->
                        orderModel.id?.let { it2 ->
                            viewModel.mergeTable(
                                it1, childIds, null, orderModel,
                                it2
                            )
                        }
                    }

                } else {


                    parentTableId?.let { it1 ->
                        viewModel.mergeTable(
                            it1,
                            childIds,
                            null,
                            null
                        )
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