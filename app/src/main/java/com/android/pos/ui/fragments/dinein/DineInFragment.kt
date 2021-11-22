package com.android.pos.ui.fragments.dinein

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.android.pos.R
import com.android.pos.databinding.FragmentDineInBinding
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.viewModels
import com.android.pos.ui.adapter.DineInFloorNameListAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import androidx.navigation.fragment.findNavController
import com.android.pos.data.model.responseModel.GetFloorPlanResponse
import com.android.pos.data.remote.Constants.AVAILABLE
import com.android.pos.data.remote.Constants.DINE_IN_STATUS
import com.android.pos.data.remote.Constants.EMPLOYEE_ID
import com.android.pos.data.remote.Constants.OCCUPIED
import com.android.pos.di.PrefProvider
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.showAlert
import com.android.pos.utils.extensions.toDp
import com.android.pos.utils.statusUtils.Status
import com.google.gson.Gson
import javax.inject.Inject

@AndroidEntryPoint
class DineInFragment : Fragment() {

    private lateinit var binding: FragmentDineInBinding
    private lateinit var dineInFloorNameListAdapter: DineInFloorNameListAdapter
    private val viewModel by viewModels<DineInViewModel>()
    private var dineInFloorNameList = ArrayList<GetFloorPlanResponse.Data>()
    private var dineInFloorTablesList = ArrayList<GetFloorPlanResponse.Data.FloorPlanTable>()
    private val TAG = this.javaClass.name.toString()

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
        //   binding.viewModel = viewModel

        loadFloorPlan()
        setUpRecyclerView()
        prefProvider.setValueboolean(DINE_IN_STATUS, false)


        binding.tvTransaction.setOnClickListener {
            findNavController().navigate(R.id.action_dineInFragment_to_transactionFragment)
        }

        binding.tvOrders.setOnClickListener {
            findNavController().navigate(R.id.action_dineInFragment_to_orders)
        }

        binding.imgMergeTable.setOnClickListener {
            loadFloorPlanDetails()
           // findNavController().navigate(R.id.action_dineInFragment_to_mergeTableDialog)

        }



        dineInFloorNameListAdapter.showFloorPlan = {
            dineInFloorTablesList =
                it.floorPlanTables as ArrayList<GetFloorPlanResponse.Data.FloorPlanTable>
            setFloorPlan(dineInFloorTablesList)
        }

        binding.txtHome.setOnClickListener {
            findNavController().popBackStack(R.id.dashboardCategoryNew, false)
        }

        return binding.root
    }


    private fun setUpRecyclerView() {
        dineInFloorNameListAdapter = DineInFloorNameListAdapter(viewModel)
        binding.rvFloorName.layoutManager =
            LinearLayoutManager(requireActivity(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvFloorName.adapter = dineInFloorNameListAdapter

    }

    private fun loadFloorPlan() {
        viewModel.getFloorPlan.observe(viewLifecycleOwner, {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()

                        if (resource.data != null && resource.data.data.isNotEmpty()) {
                            dineInFloorNameList =
                                it.data!!.data as ArrayList<GetFloorPlanResponse.Data>
                            dineInFloorNameListAdapter.addFloorName(dineInFloorNameList)

                            setFloorPlan(dineInFloorNameList[0].floorPlanTables)
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
        })
    }

    private fun loadFloorPlanDetails(){
        viewModel.getFloorPlanDetails.observe(viewLifecycleOwner, {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()

                        Log.e(TAG,"getFloorPlanDetails:  ${Gson().toJson(resource.data)}")
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
        })

    }
    private fun setFloorPlan(dineInFloorTablesList: List<GetFloorPlanResponse.Data.FloorPlanTable>) {

        binding.flFloorPlan.removeAllViews()
        if (dineInFloorTablesList.isNotEmpty()) {
            for (i in dineInFloorTablesList.indices) {
                if (dineInFloorTablesList[i].tableType == "square") {
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

                        tvNoOFChairs.text = "" + dineInFloorTablesList[i].chairCount

                        val tvTableName: AppCompatTextView =
                            inflatedViewSquare.findViewById(R.id.tvTableName)

                        tvTableName.text = "" + dineInFloorTablesList[i].tableName

                        val tvTableNumber: AppCompatTextView =
                            inflatedViewSquare.findViewById(R.id.tvTableNumber)

                        tvTableNumber.text = "" + dineInFloorTablesList[i].tableNumber

                        if (llMainParentSquare.parent != null) {
                            (llMainParentSquare.parent as ViewGroup).removeView(llMainParentSquare)
                        }

                        /*pass object in settag*/
                        inflatedViewSquare.tag = dineInFloorTablesList[i]

                        val paramsSquare = FrameLayout.LayoutParams(
                            dineInFloorTablesList[i].width.toInt().toDp(),
                            dineInFloorTablesList[i].height.toInt().toDp()
                        )

                        paramsSquare.leftMargin = dineInFloorTablesList[i].xPosition.toInt().toDp()
                        paramsSquare.topMargin = dineInFloorTablesList[i].yPosition.toInt().toDp()
                        if (dineInFloorTablesList[i].status == OCCUPIED) {
                            llMainParentSquare.background =
                                resources.getDrawable(R.drawable.background_drawer_button_green)
                        } else {
                            llMainParentSquare.background =
                                resources.getDrawable(R.drawable.background_drawer_button)
                        }


                        binding.flFloorPlan.addView(llMainParentSquare, paramsSquare)

                        inflatedViewSquare.setOnClickListener(clickInInflatedLayout()) //setting click to each item_content
                    }
                } else if (dineInFloorTablesList[i].tableType == "round") {
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

                        tvNoOFChairs.text = "" + dineInFloorTablesList[i].chairCount

                        val tvTableName: AppCompatTextView =
                            inflatedViewRound.findViewById(R.id.tvTableName)

                        tvTableName.text = "" + dineInFloorTablesList[i].tableName

                        val tvTableNumber: AppCompatTextView =
                            inflatedViewRound.findViewById(R.id.tvTableNumber)

                        tvTableNumber.text = "" + dineInFloorTablesList[i].tableNumber

                        if (llMainParentRound.parent != null) {
                            (llMainParentRound.parent as ViewGroup).removeView(llMainParentRound)
                        }

                        /*pass object in settag*/
                        inflatedViewRound.tag = dineInFloorTablesList[i]

                        val paramsRound = FrameLayout.LayoutParams(
                            dineInFloorTablesList[i].width.toInt().toDp(),
                            dineInFloorTablesList[i].height.toInt().toDp()
                        )
                        paramsRound.leftMargin = dineInFloorTablesList[i].xPosition.toInt().toDp()
                        paramsRound.topMargin = dineInFloorTablesList[i].yPosition.toInt().toDp()
                        // binding.flFloorPlan.removeAllViews()
                        if (dineInFloorTablesList[i].status == OCCUPIED) {
                            llMainParentRound.background =
                                resources.getDrawable(R.drawable.bg_circle_name_green)

                        } else {
                            llMainParentRound.background =
                                resources.getDrawable(R.drawable.bg_circle_name)
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
                    bundle.putParcelable("floorPlan", dineInFloorTableModel)


                    findNavController().navigate(
                        R.id.action_dineInFragment_to_dineInOrderTable,
                        bundle
                    )
                }

            } else if (dineInFloorTableModel.status == AVAILABLE) {

                val bundle = Bundle()
                bundle.putParcelable("dineInFloorTableObject", dineInFloorTableModel)
                findNavController().navigate(
                    R.id.action_dineInFragment_to_dineInGuestFragment,
                    bundle
                )
                Log.d("Clickeditematposition", "::$dineInFloorTableModel")
            }
        }
    }

}