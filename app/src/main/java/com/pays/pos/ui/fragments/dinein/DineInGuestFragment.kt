package com.pays.pos.ui.fragments.dinein

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.pays.pos.R
import com.pays.pos.data.entities.TbServiceCharge
import com.pays.pos.data.model.responseModel.GetFloorPlanResponse
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.AVAILABLE
import com.pays.pos.data.remote.Constants.DINE_IN
import com.pays.pos.data.remote.Constants.DINE_IN_TABLE_ID
import com.pays.pos.data.remote.Constants.MERGED
import com.pays.pos.data.remote.Constants.OCCUPIED
import com.pays.pos.data.remote.Constants.ORDER_TYPE
import com.pays.pos.data.remote.Constants.ORDER_TYPE_NAME
import com.pays.pos.databinding.FragmentDineInGuestBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.ui.adapter.GuestListAdapter
import com.pays.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.ProgressUtils
import com.pays.pos.utils.statusUtils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DineInGuestFragment : Fragment(), GuestListAdapter.GuestListner {
    private lateinit var binding: FragmentDineInGuestBinding
    private var dineInFloorTableModel: GetFloorPlanResponse.Data.FloorPlanTable?=null
    private lateinit var guestListAdapter: GuestListAdapter
    private val viewModel by viewModels<DineInViewModel>()
    private val TAG = "DineInGuestFragment"
    private val viewModelDash by activityViewModels<DashBoardCategoryViewModel>()
    private var guestCount: Int = 0
    private var serviceChargesObserve: androidx.lifecycle.Observer<Resource<List<TbServiceCharge>>>? = null

    @Inject
    lateinit var prefProvider: PrefProvider
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDineInGuestBinding.inflate(inflater, container, false)

        dineInFloorTableModel = arguments?.getParcelable("dineInFloorTableObject")
        tableStatusCheck()
        tableStatusSucess()
        observeShowProgress()
        observeUnMergeTable()



        return binding.root
    }

    private fun tableStatusSucess() {
        viewModel.tableCheckSuccess.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { status ->

                Log.e("tableCheckSuccess",""+status)

                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.increaseOnGoingOrderCounter()
                }

//                gotoDashboard()



            }

        }
    }

    private fun gotoDashboard() {
        prefProvider.setValue(ORDER_TYPE, DINE_IN)
        val bundle = bundleOf(
            "isFromDineIn" to true,
            "numberOfGuest" to guestCount,
            "floorplan" to dineInFloorTableModel
        )
        prefProvider.setValueInt(DINE_IN_TABLE_ID, dineInFloorTableModel?.id ?:0)
        prefProvider.setValueboolean(Constants.DINE_IN_STATUS, true)



        if (findNavController().currentDestination?.id == R.id.dineInGuestFragment) {


            findNavController().navigate(
                R.id.action_dineInGuestFragment_to_dashboardCategoryNew,
                bundle
            )
        }
    }


    private fun setUpRecyclerView() {
        guestListAdapter = GuestListAdapter()
        guestListAdapter.setListner(this)
        binding.rvNumberOfGuests.adapter = guestListAdapter

        val numberOfGuestList = ArrayList<Int>()
        var count = dineInFloorTableModel?.chairCount ?: 1
        for (i in 1..count) {
            numberOfGuestList.add(i)
        }
        guestListAdapter.addGuests(numberOfGuestList)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpRecyclerView()
        onClick()
        if (requireArguments()?.getBoolean("isMerged")) {
            binding.imgUnMergeTable.visibility = View.VISIBLE
        } else {
            binding.imgUnMergeTable.visibility = View.GONE
        }

    }

    private fun onClick() {
        binding.imgClose.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.imgUnMergeTable.setOnClickListener {
            viewModel.unMergeTable(dineInFloorTableModel?.id ?:0)
        }
    }

    override fun onGuestSelected(numberOfGuest: Int) {
        guestCount = numberOfGuest

        Log.e(TAG, "DineMergeStatus  ${dineInFloorTableModel?.status}")

        if (dineInFloorTableModel?.status == AVAILABLE) {
            viewModelDash.deleteCart()
            prefProvider.setValue(Constants.CUSTOMER_NAME, "")
            prefProvider.setValue(Constants.PREF_CUSTOMER, "")
            prefProvider.setValueInt(Constants.CUSTOMER_ID, -1)
        }
        if (dineInFloorTableModel?.status == MERGED) {
            viewModel.getTableStatus(dineInFloorTableModel?.id ?:0, MERGED)
        } else {
            viewModel.getTableStatus(dineInFloorTableModel?.id ?:0, OCCUPIED)
        }

        /*
        prefProvider.setValue(ORDER_TYPE, DINE_IN)
         prefProvider.setValue(ORDER_TYPE_NAME, DINE_IN)
         val bundle = bundleOf(
             "isFromDineIn" to true,
             "numberOfGuest" to numberOfGuest,
             "floorplan" to dineInFloorTableModel
         )
         prefProvider.setValueboolean(Constants.DINE_IN_STATUS, true)
         findNavController().navigate(
             R.id.action_dineInGuestFragment_to_dashboardCategoryNew,
             bundle
         )
         */

    }

    private fun tableStatusCheck() {
        viewModel.tableCheck.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { status ->
                LogUtil.logE(TAG, "getstr:   $status")

                AlertUtils.showCustomAlertWithListenerWithOK(
                    requireContext(),
                    status
                ) { _, _ ->


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


        viewModelDash.showProgress.observe(viewLifecycleOwner) { event ->
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
               gotoDashboard()
            }
        }


    }

    private fun observeUnMergeTable() {
        viewModel.unMergeStatusUpdate.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { status ->
                LogUtil.logE(TAG, "AnyStatus:  ${status}")
                AlertUtils.showCustomAlertWithListenerWithOK(
                    requireContext(), status.toString()
                ) { _, _ ->
                    findNavController().popBackStack()
                }


            }
        }
    }


}