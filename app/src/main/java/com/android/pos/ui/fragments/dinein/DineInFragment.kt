package com.android.pos.ui.fragments.dinein

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.widget.TextViewCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.pos.R
import com.android.pos.data.model.responseModel.GetFloorPlanResponse
import com.android.pos.data.remote.Constants.AVAILABLE
import com.android.pos.data.remote.Constants.DINE_IN_STATUS
import com.android.pos.data.remote.Constants.EMPLOYEE_ID
import com.android.pos.data.remote.Constants.MERGED
import com.android.pos.data.remote.Constants.MERGEDANDOCCUPIED
import com.android.pos.data.remote.Constants.OCCUPIED
import com.android.pos.databinding.FragmentDineInBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.DineInFloorNameListAdapter
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.showAlert
import com.android.pos.utils.extensions.toDp
import com.android.pos.utils.statusUtils.Status
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DineInFragment : Fragment() {

    private lateinit var binding: FragmentDineInBinding
    private lateinit var dineInFloorNameListAdapter: DineInFloorNameListAdapter
    private val viewModel by viewModels<DineInViewModel>()
    private var dineInFloorNameList = ArrayList<GetFloorPlanResponse.Data>()
    private var dineInFloorTablesList = ArrayList<GetFloorPlanResponse.Data.FloorPlanTable>()
    private val TAG = this.javaClass.name.toString()
    private var floorPlanSelectedPos = 0


    @Inject
    lateinit var prefProvider: PrefProvider
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_dine_in,
            container,
            false
        )

        binding.lifecycleOwner = this
        // binding.viewModel = viewModel

        loadFloorPlan()
        setUpRecyclerView()
        prefProvider.setValueboolean(DINE_IN_STATUS, false)

        TextViewCompat.setTextAppearance(binding.layoutHeader.txtMerge,R.style.CustomFontRegularStyle)
        TextViewCompat.setTextAppearance(binding.layoutHeader.txtDineinordere,R.style.CustomFontBold)
        binding.layoutHeader.txtMerge.setTextColor(resources.getColor(R.color.txtColor))
        binding.layoutHeader.txtDineinordere.setTextColor(resources.getColor(R.color.btnColor))

        binding.layoutHeader.txtTransaction.setOnClickListener {
            findNavController().navigate(R.id.action_dineInFragment_to_transactionFragment)
        }


        binding.layoutHeader.txtMerge.setOnClickListener {
            loadFloorPlanDetails()
        }



        dineInFloorNameListAdapter.showFloorPlan = {
            dineInFloorTablesList =
                it.floorPlanTables as ArrayList<GetFloorPlanResponse.Data.FloorPlanTable>
            setFloorPlan(dineInFloorTablesList)
        }

        binding.layoutHeader.txthome.setOnClickListener {
            findNavController().popBackStack(R.id.dashboardCategoryNew, false)
        }
        binding.layoutHeader.imgDrawer.setOnClickListener {
            findNavController().popBackStack(
                R.id.menuPOS,
                false
            )

        }
        return binding.root
    }


    private fun setUpRecyclerView() {
        dineInFloorNameListAdapter = DineInFloorNameListAdapter(viewModel)
        binding.rvFloorName.layoutManager =
            LinearLayoutManager(requireActivity(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvFloorName.adapter = dineInFloorNameListAdapter
        binding.previousImg.setOnClickListener {
            var firstvisiiblleItem =
                (binding.rvFloorName.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
            if (firstvisiiblleItem > 0) {
                binding.rvFloorName.smoothScrollToPosition(firstvisiiblleItem - 1)
            }
        }
        binding.nextImg.setOnClickListener {
            var firstvisiiblleItem =
                (binding.rvFloorName.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
            if (firstvisiiblleItem < dineInFloorNameListAdapter.itemCount - 1) {
                binding.rvFloorName.smoothScrollToPosition(firstvisiiblleItem + 1)
            }
        }

    }

    private fun loadFloorPlan() {
        viewModel.getFloorPlan().observe(viewLifecycleOwner) {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()

                        if (resource.data != null && resource.data.data.isNotEmpty()) {
                            dineInFloorNameList =
                                it.data!!.data as ArrayList<GetFloorPlanResponse.Data>
                            dineInFloorNameListAdapter.addFloorName(dineInFloorNameList)


                            setFloorPlan(dineInFloorNameList[floorPlanSelectedPos].floorPlanTables)

                            floorPlanSelectedPos = 0
                        }
                    }
                    Status.ERROR -> {
                        ProgressUtils.dismissProgressDialog()
                        binding.root.showAlert(resource.message)

                    }
                    Status.LOADING -> {
                        ProgressUtils.showProgressDialog(requireActivity())
                    }
                }
            }
        }
    }

    private fun loadFloorPlanDetails() {
        viewModel.getFloorPlanDetails.observe(viewLifecycleOwner) {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        val bundle = Bundle()
                        if (resource.data?.status == 200) {
                            setFragmentResultListener("request_key_table_selection") { requestKey: String, bundle: Bundle ->
                                var mergeStatus = bundle.getBoolean("merge_done")
                                if (mergeStatus) {
                                    floorPlanSelectedPos =
                                        dineInFloorNameListAdapter.getSelectedPos()
                                    loadFloorPlan()


                                }

                            }
                            //bundle.putParcelable("floorList", resource.data.data)

                            bundle.putParcelableArrayList(
                                "floorList", it.data?.data?.toCollection(
                                    arrayListOf()
                                )
                            )
                           /* findNavController().navigate(
                                R.id.action_dineInFragment_to_mergetablefragment,
                                bundle
                            )*/

                        } else {
                            AlertUtils.showCustomAlertWithListenerWithOK(
                                requireContext(), it.message.toString()
                            ) { _, _ ->
                                val navController = findNavController()
                                navController.popBackStack()
                            }

                        }

                    }
                    Status.ERROR -> {
                        ProgressUtils.dismissProgressDialog()
                        binding.root.showAlert(resource.message)

                    }
                    Status.LOADING -> {
                        ProgressUtils.showProgressDialog(requireActivity())
                    }
                }
            }
        }

    }

    private fun setFloorPlan(dineInFloorTablesList: List<GetFloorPlanResponse.Data.FloorPlanTable>) {

        binding.flFloorPlan.removeAllViews()
        if (dineInFloorTablesList.isNotEmpty()) {
            for (i in dineInFloorTablesList.indices) {
                if (dineInFloorTablesList[i].tableType == "square" && (!dineInFloorTablesList[i].childTable)) {
                    val inflatedViewSquare = layoutInflater.inflate(
                        R.layout.view_floor_square,
                        binding.flFloorPlan,
                        false
                    )

                    if (inflatedViewSquare != null) {
                        val llMainParentSquare: LinearLayout =
                            inflatedViewSquare.findViewById(R.id.llMainParentSquare)

                        val tvNoOFChairs: AppCompatTextView =
                            inflatedViewSquare.findViewById(R.id.tvNoOFChairs)


                        val tvTableName: AppCompatTextView =
                            inflatedViewSquare.findViewById(R.id.tvTableName)

                        tvTableName.text = "" + dineInFloorTablesList[i].tableName

                        val tvTableNumber: AppCompatTextView =
                            inflatedViewSquare.findViewById(R.id.tvTableNumber)


                        tvNoOFChairs.setTextColor(Color.WHITE)
                        tvTableName.setTextColor(Color.WHITE)
                        tvTableNumber.setTextColor(Color.WHITE)
                        if (dineInFloorTablesList[i].parentTable) {
                            var tableNo: String =
                                dineInFloorTablesList[i].tableNumber.toString()
                            var chairCount = dineInFloorTablesList[i].chairCount
                            dineInFloorTablesList[i].merged_child_table_details.forEach {
                                tableNo = tableNo + "," + it.table_number
                                chairCount += it.chair_count
                            }
                            dineInFloorTablesList[i].chairCount = chairCount
                            tvTableNumber.text = tableNo
                            tvNoOFChairs.text = "" + dineInFloorTablesList[i].chairCount

                        } else {
                            tvTableNumber.text = "" + dineInFloorTablesList[i].tableNumber
                            tvNoOFChairs.text = "" + dineInFloorTablesList[i].chairCount

                        }
                        if (llMainParentSquare.parent != null) {
                            (llMainParentSquare.parent as ViewGroup).removeView(llMainParentSquare)
                        }

                        /*pass object in settag*/
                        inflatedViewSquare.tag = dineInFloorTablesList[i]

                        val paramsSquare =
                            if (dineInFloorTablesList[i].height.toInt() <= 100) {
                                FrameLayout.LayoutParams(
                                    100,
                                    100
                                )
                            } else {
                                FrameLayout.LayoutParams(
                                    (dineInFloorTablesList[i].width.toInt().toDp()).toInt(),
                                    (dineInFloorTablesList[i].height.toInt().toDp()).toInt()
                                )
                            }

                        paramsSquare.leftMargin =
                            ((dineInFloorTablesList[i].xLeft.toInt() * 1.04).toInt())


                        paramsSquare.topMargin = if (dineInFloorTablesList[i].yTop > 735) {
                            735
                        } else {
                            ((dineInFloorTablesList[i].yTop.toInt() * 1.04).toInt())
                        }

                        Log.e(TAG, "YTOPVALUE:  ${dineInFloorTablesList[i].yTop}")

                        if (dineInFloorTablesList[i].status == OCCUPIED || dineInFloorTablesList[i].status == MERGEDANDOCCUPIED) {
                            llMainParentSquare.background =
                                resources.getDrawable(R.drawable.background_occupied_table)
                        } else {
                            llMainParentSquare.background =
                                resources.getDrawable(R.drawable.background_free_table)
                        }


                        binding.flFloorPlan.addView(llMainParentSquare, paramsSquare)

                        inflatedViewSquare.setOnClickListener(clickInInflatedLayout()) //setting click to each item_content
                    }
                } else if (dineInFloorTablesList[i].tableType == "round" && (!dineInFloorTablesList[i].childTable)) {
                    val inflatedViewRound = layoutInflater.inflate(
                        R.layout.view_floor_round,
                        binding.flFloorPlan,
                        false
                    )

                    if (inflatedViewRound != null) {
                        val llMainParentRound: LinearLayout =
                            inflatedViewRound.findViewById(R.id.llMainParentRound)

                        val tvNoOFChairs: AppCompatTextView =
                            inflatedViewRound.findViewById(R.id.tvNoOFChairs)

                        //tvNoOFChairs.text = "" + dineInFloorTablesList[i].chairCount

                        val tvTableName: AppCompatTextView =
                            inflatedViewRound.findViewById(R.id.tvTableName)

                        tvTableName.text = "" + dineInFloorTablesList[i].tableName

                        val tvTableNumber: AppCompatTextView =
                            inflatedViewRound.findViewById(R.id.tvTableNumber)
                        tvNoOFChairs.setTextColor(Color.WHITE)
                        tvTableName.setTextColor(Color.WHITE)
                        tvTableNumber.setTextColor(Color.WHITE)
                        if (dineInFloorTablesList[i].parentTable) {
                            var tableNo: String =
                                dineInFloorTablesList[i].tableNumber.toString()
                            var chairCount = dineInFloorTablesList[i].chairCount
                            dineInFloorTablesList[i].merged_child_table_details.forEach {
                                tableNo = tableNo + "," + it.table_number
                                chairCount += it.chair_count
                            }
                            dineInFloorTablesList[i].chairCount = chairCount
                            tvTableNumber.text = tableNo
                            tvNoOFChairs.text = "" + dineInFloorTablesList[i].chairCount

                        } else {
                            tvTableNumber.text = "" + dineInFloorTablesList[i].tableNumber
                            tvNoOFChairs.text = "" + dineInFloorTablesList[i].chairCount

                        }

                        if (llMainParentRound.parent != null) {
                            (llMainParentRound.parent as ViewGroup).removeView(llMainParentRound)
                        }

                        /*pass object in settag*/
                        inflatedViewRound.tag = dineInFloorTablesList[i]
                        val paramsRound =
                            if (dineInFloorTablesList[i].height.toInt().toDp() <= 100) {
                                FrameLayout.LayoutParams(
                                    100,
                                    100
                                )
                            } else {
                                FrameLayout.LayoutParams(
                                    (dineInFloorTablesList[i].width.toInt().toDp()).toInt(),
                                    (dineInFloorTablesList[i].height.toInt().toDp()).toInt()
                                )

                            }

                        paramsRound.leftMargin =
                            ((dineInFloorTablesList[i].xLeft.toInt() * 1.04).toInt())



                        paramsRound.topMargin =
                            if (dineInFloorTablesList[i].yTop > 735) {
                                735
                            } else {
                                ((dineInFloorTablesList[i].yTop.toInt() * 1.04).toInt())
                            }
                        // binding.flFloorPlan.removeAllViews()
                        if (dineInFloorTablesList[i].status == OCCUPIED || dineInFloorTablesList[i].status == MERGEDANDOCCUPIED) {
                            llMainParentRound.background =
                                resources.getDrawable(R.drawable.bg_circle_name_green)

                        } else {
                            llMainParentRound.background =
                                resources.getDrawable(R.drawable.background_round_free_table)
                        }
                        binding.flFloorPlan.addView(llMainParentRound, paramsRound)

                        inflatedViewRound.setOnClickListener(clickInInflatedLayout()) //setting click to each item_content


                    }

                }
            }
        } else {
            binding.flFloorPlan.removeAllViews()
        }
    }

    private fun clickInInflatedLayout(): View.OnClickListener {
        return View.OnClickListener { v ->
            val dineInFloorTableModel = v.tag as GetFloorPlanResponse.Data.FloorPlanTable
            if (dineInFloorTableModel.status == OCCUPIED && dineInFloorTableModel.currentOrderDetails != null
            ) {
                if (dineInFloorTableModel.currentOrderDetails.employeeId == prefProvider.getValueInt(
                        EMPLOYEE_ID, 0
                    )
                ) {
                    val bundle = Bundle()
                    bundle.putBoolean("isFromFloor", true)
                    bundle.putBoolean("isMerged", false)
                    /*prefProvider.setValueInt(
                        "ORDER_ID",
                        dineInFloorTableModel.currentOrderDetails.orderId
                    )*/
                    bundle.putParcelable("floorPlan", dineInFloorTableModel)


                    findNavController().navigate(
                        R.id.action_dineInFragment_to_dineInOrderTable,
                        bundle
                    )
                } else {
                    val status =
                        "This table is locked by " + dineInFloorTableModel.currentOrderDetails.employeeName + "."

                    AlertUtils.showCustomAlertWithListenerWithOK(
                        requireContext(),
                        status
                    ) { _, _ ->


                    }
                }

            } else if (dineInFloorTableModel.status == AVAILABLE) {

                val bundle = Bundle()
                bundle.putBoolean("isMerged", false)
                bundle.putParcelable("dineInFloorTableObject", dineInFloorTableModel)
                findNavController().navigate(
                    R.id.action_dineInFragment_to_dineInGuestFragment,
                    bundle
                )

            } else if (dineInFloorTableModel.status == MERGED) {
                Log.e(TAG, "dineInFloorTableModel:  ${Gson().toJson(dineInFloorTableModel)}")

                val bundle = Bundle()
                bundle.putBoolean("isMerged", true)
                bundle.putParcelable("dineInFloorTableObject", dineInFloorTableModel)
                findNavController().navigate(
                    R.id.action_dineInFragment_to_dineInGuestFragment,
                    bundle
                )


            } else if (dineInFloorTableModel.status == MERGEDANDOCCUPIED) {

                if (dineInFloorTableModel.currentOrderDetails.employeeId == prefProvider.getValueInt(
                        EMPLOYEE_ID, 0
                    )
                ) {
                    Log.e(
                        TAG,
                        "dineInFloorTableModelMErged:  ${Gson().toJson(dineInFloorTableModel)}"
                    )
                    val bundle = Bundle()
                    bundle.putBoolean("isFromFloor", true)
                    bundle.putBoolean("isMerged", false)
                    bundle.putParcelable("floorPlan", dineInFloorTableModel)


                    findNavController().navigate(
                        R.id.action_dineInFragment_to_dineInOrderTable,
                        bundle
                    )
                }

            }
        }
    }

}