package com.pays.pos.ui.fragments.dinein

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.pays.pos.R
import com.pays.pos.data.model.MergeFloorModel
import com.pays.pos.data.model.MergeTableListModel
import com.pays.pos.data.model.MergeTableModel
import com.pays.pos.data.model.responseModel.GetFloorPlanDetailResponse
import com.pays.pos.data.model.responseModel.GetOrderDetailsResponse
import com.pays.pos.data.remote.Constants.OCCUPIED
import com.pays.pos.databinding.DialogMergeTableSelectionNewBinding
import com.pays.pos.ui.adapter.MergeTableFloorSelectAdapter
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.ProgressUtils
import dagger.hilt.android.AndroidEntryPoint
import java.util.stream.Collectors

@AndroidEntryPoint
class MergeTableFragment : Fragment() {
    private lateinit var binding: DialogMergeTableSelectionNewBinding
    private var list: ArrayList<MergeTableListModel> = arrayListOf()
    private lateinit var adapter: MergeTableFloorSelectAdapter
    private var listFloorPlan: ArrayList<GetFloorPlanDetailResponse.Data>? = null
    private lateinit var tableAdapter: ArrayAdapter<MergeTableModel>
    private lateinit var floorAdapter: ArrayAdapter<MergeFloorModel>
    var count = 0
    private var floorPlanID: ArrayList<Int> = arrayListOf()
    private var floorplanHashMap: HashMap<MergeFloorModel, ArrayList<MergeTableModel>> = hashMapOf()


    private var primaryFloorplanId = 0
    private var primarytableId = 0
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
            R.layout.dialog_merge_table_selection_new,
            container,
            false
        )
        observeMergeTable()
        observeShowProgress()


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
        floorPlanID.add(count)
        setData()
        onClick()

        TextViewCompat.setTextAppearance(binding.layoutHeader.txtMerge, R.style.CustomFontBold)
        TextViewCompat.setTextAppearance(
            binding.layoutHeader.txtDineinordere,
            R.style.CustomFontRegularStyle
        )
        binding.layoutHeader.txtMerge.setTextColor(resources.getColor(R.color.btnColor))
        binding.layoutHeader.txtDineinordere.setTextColor(resources.getColor(R.color.txtColor))
        binding.layoutHeader.txtDineinordere.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.layoutHeader.txtTransaction.setOnClickListener {
            // findNavController().navigate(R.id.action_mergetablefragment_to_transactionlistfragment)
        }
        binding.layoutHeader.txthome.setOnClickListener {
            findNavController().popBackStack(
                R.id.dashboardCategoryNew,
                false
            )
        }
        binding.layoutHeader.imgDrawer.setOnClickListener {
            findNavController().popBackStack(
                R.id.menuPOS,
                false
            )

        }
    }

    private fun setData() {

        listFloorPlan = requireArguments().getParcelableArrayList("floorList")
        listFloor = arrayListOf()
        listTable = arrayListOf()


        listFloorPlan?.forEach {
            var list_of_table: ArrayList<MergeTableModel> = arrayListOf()

            it.floor_plan_tables.forEach { it1 ->
                list_of_table.add(
                    MergeTableModel(
                        it1.id, it1.table_name, it1.floor_plan_id, it.name,
                        it1.status == OCCUPIED, orderId = if (it1.order_details != null) {
                            it1.order_details.id
                        } else {
                            null
                        },
                        orderDetails = it1.order_details,
                        chairCount = it1.chair_count
                    )
                )
            }

            floorplanHashMap[MergeFloorModel(it.id, it.name)] = list_of_table
        }

        var floorplandefault: ArrayList<String> = arrayListOf()
        var tabledefault: ArrayList<String> = arrayListOf()

        floorplanHashMap.forEach {
            floorplandefault.add(it.key.name)
        }

        binding.txtFloorPlanLabel.setOnClickListener {
            binding.txtselectprimarytable.text = "Select Primary Table"
            val textView = TextView(context)
            textView.text = "Select Floor Plan"
            textView.setPadding(20, 20, 20, 20)
            textView.setTypeface(Typeface.DEFAULT_BOLD);
            textView.textSize = 20f
            textView.setBackgroundColor(resources.getColor(R.color.btnColor))
            textView.setTextColor(Color.WHITE)
            val builder = AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
            builder.setCustomTitle(textView)
            builder.setItems(
                floorplandefault.toArray(arrayOfNulls<String>(floorplandefault.size))
            ) { dialog, which ->

                binding.txtFloorPlanLabel.text = floorplandefault[which]
                tabledefault = arrayListOf()
                floorplanHashMap.forEach {

                    if (it.key.name == floorplandefault[which]) {
                        primaryFloorplanId = it.key.id
                        floorplanHashMap[it.key]?.forEach {
                            tabledefault.add(it.name)
                        }
                    }
                }

                dialog.dismiss()

            }
            val alertDialog: AlertDialog = builder.create()
            alertDialog.show()

        }
        binding.txtselectprimarytable.setOnClickListener {
            if (primaryFloorplanId != 0) {
                val textView = TextView(context)
                textView.text = "Select Primary Table"
                textView.setPadding(20, 20, 20, 20)
                textView.setTypeface(Typeface.DEFAULT_BOLD);
                textView.textSize = 20f
                textView.setBackgroundColor(resources.getColor(R.color.btnColor))
                textView.setTextColor(Color.WHITE)
                val builder = AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
                builder.setCustomTitle(textView)
                builder.setItems(
                    tabledefault.toArray(arrayOfNulls<String>(tabledefault.size))
                ) { dialog, which ->
                    dialog.dismiss()
                    floorplanHashMap.forEach {
                        if (it.key.id == primaryFloorplanId) {
                            primarytableId = floorplanHashMap[it.key]?.get(which)?.id ?: 0
                        }
                    }
                    binding.txtselectprimarytable.text = tabledefault[which]

                }
                val alertDialog: AlertDialog = builder.create()
                alertDialog.show()
            } else {
                AlertUtils.showCustomAlertWithListenerWithOK(
                    requireContext(),
                    "Please Select Floor."
                ) { _, _ ->

                }
            }

        }


        adapter = MergeTableFloorSelectAdapter()
        binding.rvTableList.adapter = adapter
        adapter.setList(floorPlanID, floorplanHashMap)
    }

    private fun onClick() {

        binding.txtaddmore.setOnClickListener {
            floorPlanID.add(count++)
            adapter.setList(floorPlanID, floorplanHashMap)
//            adapter.addItem(MergeTableListModel(listTable, listFloor))
        }


        binding.save.setOnClickListener {
            var listtemp: ArrayList<MergeTableListModel> = arrayListOf()
            list = ArrayList()
            list.add(
                MergeTableListModel(
                    arrayListOf(),
                    arrayListOf(),
                    primarytableId,
                    null,
                    primaryFloorplanId,
                    null,
                    null,
                    null
                )
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                list.addAll(
                    adapter.getFloorPlanTableIDs().stream().distinct().collect(Collectors.toList())
                )
                listtemp.addAll(
                    adapter.getFloorPlanTableIDs().stream().distinct().collect(Collectors.toList())
                )
            }

            var childTableList: ArrayList<String> = arrayListOf()
            listtemp.forEach {
                childTableList.add(it.selectedTableId.toString())
            }
            Log.d("yash", "primaryTable : " + primarytableId)
            Log.d("yash", "secondaryTableList : " + childTableList)

            if (childTableList.contains(primarytableId.toString())) {
                AlertUtils.showCustomAlertWithListenerWithOK(
                    requireContext(),
                    "Same table can't be merged."
                ) { _, _ ->

                }
            } else {
//            viewModel.mergeTable(primarytableId)
            }
        }
//        binding.save.setOnClickListener {
//            var listSecondary = adapter.getList()
//            var allIds: ArrayList<Int?> = arrayListOf()
//            var isDuplicateIdTrue = false
//
//            listSecondary.forEach {
//                allIds.add(it.selectedTableId)
//            }
//            allIds.add(tableAdapter.getItem(tableSelectedPos)?.id)
//
//            var setIds: Set<Int> = HashSet<Int>(allIds)
//            if (setIds.size < allIds.size) {
//                isDuplicateIdTrue = true
//            } else {
//                isDuplicateIdTrue = false
//            }
//
//
//            if (!isDuplicateIdTrue) {
//                var primaryTable = tableAdapter.getItem(tableSelectedPos)
//                var totalChairCount = 0
//
//
//                var listSecondaryOrderDetails: ArrayList<GetFloorPlanDetailResponse.OrderDetails> =
//                    arrayListOf()
//
//                val parentTableId = tableAdapter.getItem(tableSelectedPos)?.id
//                var childIds: String = ""
//                var arrayChildIds: ArrayList<String> = arrayListOf()
//
//                for (i in 0 until listSecondary.size) {
//                    arrayChildIds.add(listSecondary.get(i).selectedTableId.toString())
//
//                    if (listSecondary[i].orderDetails == null) {
//                        totalChairCount += listSecondary.get(i).listTable.get(
//                            listSecondary.get(i).tableSelectedPosition ?: 0
//                        ).chairCount
//                            ?: 0
//                    }
//                }
//                childIds = android.text.TextUtils.join(",", arrayChildIds)
//
//
//                var tableMergeList: ArrayList<MergeTableModel> = arrayListOf()
//                for (i in 0 until totalChairCount) {
//
//                    tableMergeList.add(
//                        MergeTableModel(
//                            id = adapter.getList().get(0).tableSelectedPosition?.let { it1 ->
//                                adapter.getList().get(0).listTable.get(
//                                    it1
//                                ).id
//                            }!!, name = "", floorId = 0, floorName = ""
//                        )
//                    )
//                }
//
//
//
//
//                listSecondary.add(
//                    MergeTableListModel(
//                        listTable = listTable,
//                        listFloor,
//                        tableSelectedPosition = tableSelectedPos,
//                        orderDetails = primaryTable?.orderDetails
//                    )
//                )
//
//                for (i in 0 until listSecondary.size) {
//                    if (listSecondary[i].orderDetails != null) {
//                        listSecondary[i].orderDetails?.let { it1 ->
//                            listSecondaryOrderDetails.add(
//                                it1
//                            )
//                        }
//                    }
//
//                }
//                LogUtil.logE(TAG, "listSecondaryOrderDetailsSize:  ${listSecondaryOrderDetails.size}")
//
//                if (listSecondaryOrderDetails.size == 0) {
//                    //This is for Every Empty Table for both Primary and Secondary
//
//                    parentTableId?.let { it1 ->
//                        viewModel.mergeTable(
//                            it1,
//                            childIds,
//                            null,
//                            null
//                        )
//                    }
//
//
//                } else if (listSecondaryOrderDetails.size == 1) {
//                    //One Occupied and Other's Available
//
//                    var orderModel: OrderAttributeRequestModel = OrderAttributeRequestModel()
//                    orderModel = viewModel.createMergeOrderRequest(
//                        listSecondaryOrderDetails.get(0),
//                        tableMergeList
//                    )
//
//                    viewModel.mergeTable(
//                        parentTableId ?: 0,
//                        childIds,
//                        orderModel = orderModel,
//                        orderId = orderModel.id
//
//                    )
//
//                } else {
//                    //Multiple Occupied and Other's Available
//                    var orderModel =
//                        viewModel.createMultipleMergeOrder(
//                            listSecondaryOrderDetails,
//                            tableMergeList
//                        )
//                    var mergedChildsOrderIds = ""
//                    for (i in 0 until listSecondaryOrderDetails.size) {
//                        if (listSecondaryOrderDetails.get(i).id != primaryTable?.orderId) {
//
//                            mergedChildsOrderIds += listSecondaryOrderDetails.get(i).id
//                            if (i != listSecondaryOrderDetails.size - 1) {
//                                mergedChildsOrderIds += ","
//
//                            }
//
//                        }
//                    }
//                    var listOrderIds: ArrayList<String> = arrayListOf()
//                    listSecondaryOrderDetails.forEach {
//                        listOrderIds.add(it.id.toString())
//                    }
//                    var mergedOrderIds = android.text.TextUtils.join(",", listOrderIds)
//
//                    viewModel.mergeTable(parentTableId ?: 0, childIds, mergedOrderIds, orderModel)
//
//
//                }
//
//
//            } else if (isDuplicateIdTrue) {
//
//                AlertUtils.showCustomAlertWithListenerWithOK(
//                    requireContext(), "Same Table Can't be Merged."
//                ) { _, _ ->
//
//                }
//
//
//            }
//        }
    }


    private fun observeShowProgress() {

        viewModel.showProgress.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        }


    }
}