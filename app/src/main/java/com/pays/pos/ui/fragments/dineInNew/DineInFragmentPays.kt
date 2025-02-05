package com.pays.pos.ui.fragments.dineInNew

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.pays.pos.R
import com.pays.pos.data.model.responseModel.GetFloorPlanResponse
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.AVAILABLE
import com.pays.pos.data.remote.Constants.DINEIN_FLOORPLAN_SHOW_TABLENAME
import com.pays.pos.data.remote.Constants.DINE_IN_STATUS
import com.pays.pos.data.remote.Constants.EMPLOYEE_ID
import com.pays.pos.data.remote.Constants.MERGED
import com.pays.pos.data.remote.Constants.MERGEDANDOCCUPIED
import com.pays.pos.data.remote.Constants.OCCUPIED
import com.pays.pos.databinding.FragmentDineInBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.di.RolePermission
import com.pays.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.MethodUtils
import com.pays.pos.utils.ProgressUtils
import com.pays.pos.utils.extensions.gone
import com.pays.pos.utils.extensions.showAlert
import com.pays.pos.utils.extensions.toDp
import com.pays.pos.utils.extensions.visible
import com.pays.pos.utils.statusUtils.Status
import com.google.gson.Gson
import com.pays.pos.data.remote.Constants.DINE_IN_UPDATE
import com.pays.pos.ui.fragments.dineInNew.adapter.DineInFloorNameListAdapterPays
import com.pays.pos.ui.fragments.dineInNew.model.SyncDineInEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import javax.inject.Inject

@AndroidEntryPoint
class DineInFragmentPays : Fragment() {

    private lateinit var binding: FragmentDineInBinding
    private lateinit var dineInFloorNameListAdapter: DineInFloorNameListAdapterPays
    private val viewModel by viewModels<DineInViewModelPays>()
    private var dineInFloorNameList = ArrayList<GetFloorPlanResponse.Data>()
    private var dineInFloorTablesList = ArrayList<GetFloorPlanResponse.Data.FloorPlanTable>()
    private val TAG = this.javaClass.name.toString()
    private var floorPlanSelectedPos = 0
    private var dineInFloorTable : GetFloorPlanResponse.Data.FloorPlanTable? = null
    private val dashBoardCategoryViewModel by activityViewModels<DashBoardCategoryViewModel>()

    @Inject
    lateinit var rolePermission: RolePermission

    @Inject
    lateinit var prefProvider: PrefProvider
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dashBoardCategoryViewModel.deleteCart()

        //set dine in update false
        prefProvider.setValueboolean(DINE_IN_UPDATE,false)

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

        /*TextViewCompat.setTextAppearance(
            binding.layoutHeader.txtMerge,
            R.style.CustomFontRegularStyle
        )
        TextViewCompat.setTextAppearance(
            binding.layoutHeader.txtDineinordere,
            R.style.CustomFontBold
        )
        binding.layoutHeader.txtMerge.setTextColor(resources.getColor(R.color.txtColor))
        binding.layoutHeader.txtDineinordere.setTextColor(resources.getColor(R.color.btnColor))*/


        val onBackPressedCallback: OnBackPressedCallback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {

                    prefProvider.setValue(Constants.ORDER_TYPE, "")
                    prefProvider.setValue(Constants.ORDER_TYPE_NAME, "")
                    findNavController().popBackStack()
                }

            }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            onBackPressedCallback
        )


        dineInFloorNameListAdapter.showFloorPlan = {
            dineInFloorTablesList =
                it.floorPlanTables as ArrayList<GetFloorPlanResponse.Data.FloorPlanTable>
            setFloorPlan(dineInFloorTablesList)
        }


        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true /* enabled by default */) {
                override fun handleOnBackPressed() {
                    findNavController().navigate(R.id.action_dineInFragmentPays_to_dashboardCategoryBoldPOS)
                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onClick()
        tableStatusCheck()
        tableStatusSucess()
        observeShowProgress()

        //   binding.layoutHeader.imgTransferTable?.gone()
        //   binding.layoutHeader.txtMerge.gone()
        binding.layoutHeader.imgRefreshTables?.visible()
        binding.layoutHeader.txtUserName.text = prefProvider.getValue(Constants.EMPLOYEE_NAME, "")

    }

    override fun onStart() {
        super.onStart()

        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()

        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSyncDineInEvent(event: SyncDineInEvent) {
        if (event.doSync) {
            floorPlanSelectedPos =
                dineInFloorNameListAdapter.getSelectedPos()
            loadFloorPlan()
        }
    }


    private fun onClick() {
        binding.layoutHeader.imgTransferTable?.setOnClickListener {
            loadTransferTableDetails()


        }

        binding.layoutHeader.txtTransaction.setOnClickListener {
            if (rolePermission.hasTransactionPermission(binding.root)) {
                findNavController().navigate(R.id.action_dineInFragmentPays_to_transactionFragment)
            }
        }

        binding.layoutHeader.linearSwitchUser.setOnClickListener {
            try {
                var bundle = Bundle()
                bundle.putBoolean("isSwap", true)
                bundle.putBoolean("isDashboard", false)
                findNavController().navigate(
                    R.id.action_dineInFragmentPays_to_passcode,
                    bundle
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
       /* binding.layoutHeader.ivLock.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_dineInFragment_to_reportEODFragmeent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }*/
        binding.layoutHeader.txtMerge.setOnClickListener {
            if (MethodUtils.isDoubleClick()) return@setOnClickListener
            loadFloorPlanDetails()
        }
        binding.layoutHeader.txthome.setOnClickListener {

//            dashBoardCategoryViewModel.cartModel = null
//            prefProvider.setValue(Constants.ORDER_TYPE, "")
//            prefProvider.setValue(Constants.ORDER_TYPE_NAME, "")
            dashBoardCategoryViewModel.deleteCart()
            dashBoardCategoryViewModel.currentCartItems.clear()
            dashBoardCategoryViewModel.duplicateCurrentCartItem.clear()

            try {
                findNavController().popBackStack()
            }catch (e:Exception){}
        }
        binding.layoutHeader.imgDrawer.setOnClickListener {
            if (findNavController()?.currentDestination?.id == R.id.dineInFragmentPays) {
                findNavController().navigate(
                    R.id.action_dineInFragmentPays_to_menuFragment

                )
            }

        }

        // Refresh floor plan
        binding.layoutHeader.imgRefreshTables.setOnClickListener {
            floorPlanSelectedPos =
                dineInFloorNameListAdapter.getSelectedPos()
            loadFloorPlan()
        }

        setFragmentResultListener("request_key_table_selection") { requestKey: String, bundle: Bundle ->
            var mergeStatus = bundle.getBoolean("merge_done")
            if (mergeStatus) {
                floorPlanSelectedPos =
                    dineInFloorNameListAdapter.getSelectedPos()
                loadFloorPlan()
            }

        }

    }


    private fun setUpRecyclerView() {
        dineInFloorNameListAdapter = DineInFloorNameListAdapterPays(viewModel)
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

                            dineInFloorNameList[floorPlanSelectedPos].floorPlanTables?.let { it1 ->
                                setFloorPlan(
                                    it1
                                )
                            }

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

    private fun loadTransferTableDetails() {
        viewModel.getAvailableTransferTableList().observe(viewLifecycleOwner) {
            it?.let {
                when (it.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        val bundle = Bundle()
                        if (it.data?.status == 200) {
                            bundle.putParcelable("floorList", it.data)
                            if (findNavController().currentDestination?.id == R.id.dineInFragmentPays) {
                                findNavController().navigate(
                                    R.id.action_dineInFragmentPays_to_transferTableDialog,
                                    bundle
                                )
                            }

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
                        binding.root.showAlert(it.message)

                    }

                    Status.LOADING -> {
                        ProgressUtils.showProgressDialog(requireActivity())
                    }
                }
            }
        }

    }

    private fun loadFloorPlanDetails() {
        viewModel.getFloorPlanDetails().observe(viewLifecycleOwner) {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        val bundle = Bundle()
                        if (resource.data?.status == 200) {
                            bundle.putParcelableArrayList(
                                "floorList", it.data?.data?.toCollection(
                                    arrayListOf()
                                )
                            )
                            if (findNavController().currentDestination?.id == R.id.dineInFragmentPays) {
                                findNavController().navigate(
                                    R.id.action_dineInFragmentPays_to_mergeTableDialog,
                                    bundle
                                )
                            }

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

                        val tvCustomerName: AppCompatTextView =
                            inflatedViewSquare.findViewById(R.id.tv_customername)

                        val img_chair: ImageView =
                            inflatedViewSquare.findViewById(R.id.img_chair)


                        val img_table: ImageView =
                            inflatedViewSquare.findViewById(R.id.img_table)

                        tvTableName.text = "" + dineInFloorTablesList[i].tableName
                        val tvTableNumber: AppCompatTextView =
                            inflatedViewSquare.findViewById(R.id.tvTableNumber)
                        tvNoOFChairs.setTextColor(Color.WHITE)
                        tvTableName.setTextColor(Color.WHITE)
                        tvTableNumber.setTextColor(Color.WHITE)
                        tvCustomerName.setTextColor(Color.WHITE)
                        if (dineInFloorTablesList[i].parentTable) {
                            var tableNo: String =
                                dineInFloorTablesList[i].tableNumber.toString()
                            var chairCount = dineInFloorTablesList[i].chairCount
                            dineInFloorTablesList[i].merged_child_table_details?.forEach {
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
                        tvCustomerName.text =
                            dineInFloorTablesList[i].lock_by_name.toString().substringBefore(" ")
                        if (dineInFloorTablesList[i].status == MERGED || dineInFloorTablesList[i].status == MERGEDANDOCCUPIED) {
                            img_chair.gone()
                            img_table.gone()
                            tvNoOFChairs.gone()
                            tvTableName.gone()
                            tvCustomerName.gone()
                            tvTableNumber.visible()
                        } else if (dineInFloorTablesList[i].status == OCCUPIED) {
                            tvTableNumber.visible()
                            tvCustomerName.visible()
                            img_chair.gone()
                            tvTableName.gone()
                            tvNoOFChairs.gone()
                            img_table.gone()
                        } else if (dineInFloorTablesList[i].status == AVAILABLE) {
                            if (prefProvider.getValueboolean(
                                    DINEIN_FLOORPLAN_SHOW_TABLENAME,
                                    false
                                )
                            ) {
                                tvTableName.visible()
                                tvTableNumber.gone()
                            } else {
                                tvTableName.gone()
                                tvTableNumber.visible()
                            }
                            img_chair.visible()
                            tvNoOFChairs.visible()
                            img_table.gone()
                            tvCustomerName.gone()
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


                        if (dineInFloorTablesList[i].yTop > 0) {
                            paramsSquare.topMargin =
                                (dineInFloorTablesList[i].yTop.toInt() * 1.04).toInt()
                        }
//                        paramsSquare.topMargin = if (dineInFloorTablesList[i].yTop > 735) {
//                            735
//                        } else {
//                            ((dineInFloorTablesList[i].yTop.toInt() * 1.04).toInt())
//                        }

                        Log.d(
                            TAG,
                            "setFloorPlan: xPosition : " + dineInFloorTablesList[i].xPosition
                        )
                        Log.d(TAG, "setFloorPlan: yTop      : " + dineInFloorTablesList[i].yTop)
                        LogUtil.logE(TAG, "YTOPVALUE:  ${dineInFloorTablesList[i].xPosition}")

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


                        val img_chair: ImageView =
                            inflatedViewRound.findViewById(R.id.img_round_chair)
                        val tvCustomerName: AppCompatTextView =
                            inflatedViewRound.findViewById(R.id.tv_customername)

                        val img_table: ImageView =
                            inflatedViewRound.findViewById(R.id.img_round_table)
                        val tvTableName: AppCompatTextView =
                            inflatedViewRound.findViewById(R.id.tvTableName)

                        tvTableName.text = "" + dineInFloorTablesList[i].tableName

                        val tvTableNumber: AppCompatTextView =
                            inflatedViewRound.findViewById(R.id.tvTableNumber)
                        tvNoOFChairs.setTextColor(Color.WHITE)
                        tvTableName.setTextColor(Color.WHITE)
                        tvTableNumber.setTextColor(Color.WHITE)
                        tvCustomerName.setTextColor(Color.WHITE)
                        if (dineInFloorTablesList[i].parentTable) {
                            var tableNo: String =
                                dineInFloorTablesList[i].tableNumber.toString()
                            var chairCount = dineInFloorTablesList[i].chairCount
                            dineInFloorTablesList[i].merged_child_table_details?.forEach {
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
                        tvCustomerName.text =
                            dineInFloorTablesList[i].lock_by_name.toString().substringBefore(" ")
                        if (llMainParentRound.parent != null) {
                            (llMainParentRound.parent as ViewGroup).removeView(llMainParentRound)
                        }
                        if (dineInFloorTablesList[i].status == MERGED || dineInFloorTablesList[i].status == MERGEDANDOCCUPIED) {
                            img_chair.gone()
                            img_table.gone()
                            tvNoOFChairs.gone()
                            tvTableName.gone()
                            tvCustomerName.gone()
                            tvTableNumber.visible()
                        } else if (dineInFloorTablesList[i].status == OCCUPIED) {
                            tvTableNumber.visible()
                            tvCustomerName.visible()
                            img_chair.gone()
                            tvTableName.gone()
                            tvNoOFChairs.gone()
                            img_table.gone()
                        } else if (dineInFloorTablesList[i].status == AVAILABLE) {
                            if (prefProvider.getValueboolean(
                                    DINEIN_FLOORPLAN_SHOW_TABLENAME,
                                    false
                                )
                            ) {
                                tvTableName.visible()
                                tvTableNumber.gone()
                            } else {
                                tvTableName.gone()
                                tvTableNumber.visible()
                            }
                            img_chair.visible()
                            tvNoOFChairs.visible()
                            img_table.gone()
                            tvCustomerName.gone()
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

                        if (dineInFloorTablesList[i].yTop > 0) {
                            paramsRound.topMargin =
                                (dineInFloorTablesList[i].yTop.toInt() * 1.04).toInt()
                        }
//
//                        paramsRound.topMargin =
//                            if (dineInFloorTablesList[i].yTop > 735) {
//                                735
//                            } else {
//                                ((dineInFloorTablesList[i].yTop.toInt() * 1.04).toInt())
//                            }
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
            LogUtil.logE(TAG, "dineInFloorTableModel:  ${Gson().toJson(dineInFloorTableModel)}")
            LogUtil.logE(
                TAG, "dineInFloorTableModelEmployeeId:  ${
                    prefProvider.getValueInt(
                        EMPLOYEE_ID, 0
                    )
                }"
            )
            if (dineInFloorTableModel.status == OCCUPIED) {
                if (dineInFloorTableModel.lock_by_id == prefProvider.getValueInt(
                        EMPLOYEE_ID,
                        0
                    ) || prefProvider.isAdmin()
                ) {
                    Log.d(
                        TAG,
                        "clickInInflatedLayout: current " + prefProvider.getValueInt(EMPLOYEE_ID, 0)
                    )
                    Log.d(TAG, "clickInInflatedLayout: dynamic " + dineInFloorTableModel.lock_by_id)
                    Log.d(TAG, "clickInInflatedLayout: isadmin " + prefProvider.isAdmin())
                    if (dineInFloorTableModel.currentOrderDetails != null) {
                        try {
                            val bundle = Bundle()
                            bundle.putBoolean("isFromFloor", true)
                            bundle.putBoolean("isMerged", false)
                            bundle.putParcelable("floorPlan", dineInFloorTableModel)


                            findNavController().navigate(
                                R.id.action_dineInFragmentPays_to_dineInOrderTable,
                                bundle
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else if (dineInFloorTableModel.lock_by_id == prefProvider.getValueInt(
                            EMPLOYEE_ID, 0
                        )
                    ) {
                        prefProvider.setValue(Constants.ORDER_TYPE, Constants.DINE_IN)
                        prefProvider.setValue(Constants.ORDER_TYPE_NAME, Constants.DINE_IN)
                        try {
                            val bundle = bundleOf(
                                "isFromDineIn" to true,
                                "numberOfGuest" to dineInFloorTableModel.chairCount,
                                "floorplan" to dineInFloorTableModel
                            )
                            prefProvider.setValueInt(
                                Constants.DINE_IN_TABLE_ID,
                                dineInFloorTableModel.id
                            )
                            prefProvider.setValueboolean(Constants.DINE_IN_STATUS, true)
                            findNavController().navigate(
                                R.id.action_dineInFragmentPays_to_dashboardCategoryBoldPOS,
                                bundle
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        var status = ""
                        if (dineInFloorTableModel.lock_by_name != null) {
                            status =
                                "This table is locked by " + dineInFloorTableModel.lock_by_name + "."
                        } else {
                            if (dineInFloorTableModel.currentOrderDetails != null) {
                                status =
                                    "This table is locked by " + dineInFloorTableModel.currentOrderDetails?.employeeName + "."
                            } else {
                                status =
                                    "This table is locked by " + dineInFloorTableModel.lock_by_name + "."
                            }

                        }



                        AlertUtils.showCustomAlertWithListenerWithOK(
                            requireContext(),
                            status
                        ) { _, _ ->


                        }


                        /*   val bundle = Bundle()
                           bundle.putBoolean("isMerged", false)
                           bundle.putParcelable("dineInFloorTableObject", dineInFloorTableModel)
                           findNavController().navigate(
                               R.id.action_dineInFragment_to_dineInGuestFragment,
                               bundle
                           )*/
                    }

                } else {
                    var status = ""
                    if (dineInFloorTableModel.lock_by_name != null) {
                        status =
                            "This table is locked by " + dineInFloorTableModel.lock_by_name + "."
                    } else {
                        if (dineInFloorTableModel.currentOrderDetails != null) {
                            status =
                                "This table is locked by " + dineInFloorTableModel.currentOrderDetails?.employeeName + "."
                        } else {
                            status =
                                "This table is locked by " + dineInFloorTableModel.lock_by_name + "."
                        }

                    }


                    AlertUtils.showCustomAlertWithListenerWithOK(
                        requireContext(),
                        status
                    ) { _, _ ->


                    }
                }

            } else if (dineInFloorTableModel.status == AVAILABLE) {
                try {
                    val bundle = Bundle()
                    bundle.putBoolean("isMerged", false)
                    bundle.putParcelable("dineInFloorTableObject", dineInFloorTableModel)
                    dineInFloorTable = dineInFloorTableModel
                    onTableSelected(dineInFloorTableModel)
//                findNavController().navigate(
                    //                    R.id.action_dineInFragment_to_dineInGuestFragment,
                    //                    bundle
                    //                )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else if (dineInFloorTableModel.status == MERGED) {
                LogUtil.logE(TAG, "dineInFloorTableModel:  ${Gson().toJson(dineInFloorTableModel)}")
                try {
                    val bundle = Bundle()
                    bundle.putBoolean("isMerged", true)
                    bundle.putParcelable("dineInFloorTableObject", dineInFloorTableModel)
                    dineInFloorTable = dineInFloorTableModel
                    onTableSelected(dineInFloorTableModel)
                    //findNavController().navigate(
                    //                    R.id.action_dineInFragment_to_dineInGuestFragment,
                    //                    bundle
                    //                )
                } catch (e: Exception) {
                    e.printStackTrace()
                }


            } else if (dineInFloorTableModel.status == MERGEDANDOCCUPIED) {

                if (dineInFloorTableModel.lock_by_id == prefProvider.getValueInt(
                        EMPLOYEE_ID,
                        0
                    ) || prefProvider.isAdmin()
                ) {
                    LogUtil.logE(
                        TAG,
                        "dineInFloorTableModelMErged:  ${Gson().toJson(dineInFloorTableModel)}"
                    )
                    try {
                        val bundle = Bundle()
                        bundle.putBoolean("isFromFloor", true)
                        bundle.putBoolean("isMerged", false)
                        bundle.putParcelable("floorPlan", dineInFloorTableModel)


                        findNavController().navigate(
                            R.id.action_dineInFragmentPays_to_dineInOrderTable,
                            bundle
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    var status = ""
                    if (dineInFloorTableModel.lock_by_name != null) {
                        status =
                            "This table is locked by " + dineInFloorTableModel.lock_by_name + "."
                    } else {
                        status =
                            "This table is locked by " + dineInFloorTableModel.currentOrderDetails?.employeeName + "."
                    }


                    AlertUtils.showCustomAlertWithListenerWithOK(
                        requireContext(),
                        status
                    ) { _, _ ->


                    }
                }

            }
        }
    }

    private fun onTableSelected(dineInFloorTableModel: GetFloorPlanResponse.Data.FloorPlanTable) {
        if (dineInFloorTableModel.status == AVAILABLE) {
            dashBoardCategoryViewModel.deleteCart()
            prefProvider.setValue(Constants.CUSTOMER_NAME, "")
            prefProvider.setValue(Constants.RECEIPT_CUSTOMER_NAME, "")
            prefProvider.setValue(Constants.PREF_CUSTOMER, "")
            prefProvider.setValueInt(Constants.CUSTOMER_ID, -1)
            prefProvider.setValueboolean(DINE_IN_UPDATE,false)
        }
        if (dineInFloorTableModel.status == MERGED) {
            viewModel.getTableStatus(dineInFloorTableModel.id ?:0, MERGED)
        } else {
            viewModel.getTableStatus(dineInFloorTableModel.id ?:0, OCCUPIED)
        }
    }

    private fun tableStatusCheck() {
        viewModel.tableCheck.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { status ->
                AlertUtils.showCustomAlertWithListenerWithOK(
                    requireContext(),
                    status
                ) { _, _ ->
                }
            }
        }
    }

    private fun tableStatusSucess() {
        viewModel.tableCheckSuccess.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { status ->
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.increaseOnGoingOrderCounter()
                }
            }
        }
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

        viewModel.snackbarText.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                AlertUtils.showCustomAlert(requireContext(), it)
            }
        }


        dashBoardCategoryViewModel.showProgress.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        }

        viewModel.increaseCounter.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                gotoDashboard(dineInFloorTable!!)
            }
        }
    }

    private fun gotoDashboard(dineInFloorTableModel: GetFloorPlanResponse.Data.FloorPlanTable) {
        prefProvider.setValue(Constants.ORDER_TYPE, Constants.DINE_IN)
        val bundle = bundleOf(
            "isFromDineIn" to true,
            "numberOfGuest" to dineInFloorTableModel.chairCount,
            "floorplan" to dineInFloorTableModel
        )
        prefProvider.setValueInt(Constants.DINE_IN_TABLE_ID, dineInFloorTableModel.id ?:0)
        prefProvider.setValueboolean(Constants.DINE_IN_STATUS, true)
        if (findNavController().currentDestination?.id == R.id.dineInFragmentPays) {
            findNavController().navigate(
                R.id.action_dineInFragmentPays_to_dashboardCategoryBoldPOS,
                bundle
            )
        }
    }
}