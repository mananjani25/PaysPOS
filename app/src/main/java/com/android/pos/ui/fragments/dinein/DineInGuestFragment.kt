package com.android.pos.ui.fragments.dinein

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.model.responseModel.GetFloorPlanResponse
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.DINE_IN
import com.android.pos.data.remote.Constants.DINE_IN_TABLE_ID
import com.android.pos.data.remote.Constants.ORDER_TYPE
import com.android.pos.data.remote.Constants.ORDER_TYPE_NAME
import com.android.pos.databinding.FragmentDineInGuestBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.GuestListAdapter
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.ProgressUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DineInGuestFragment : Fragment(), GuestListAdapter.GuestListner {
    private lateinit var binding: FragmentDineInGuestBinding
    private lateinit var dineInFloorTableModel: GetFloorPlanResponse.Data.FloorPlanTable
    private lateinit var guestListAdapter: GuestListAdapter
    private val viewModel by viewModels<DineInViewModel>()
    private val TAG = "DineInGuestFragment"
    private var guestCount: Int = 0

    @Inject
    lateinit var prefProvider: PrefProvider
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDineInGuestBinding.inflate(inflater, container, false)

        dineInFloorTableModel = arguments?.getParcelable("dineInFloorTableObject")!!
        tableStatusCheck()
        tableStatusSucess()
        observeShowProgress()


        return binding.root
    }

    private fun tableStatusSucess() {
        viewModel.tableCheckSuccess.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let { status ->
                Log.e(TAG, "getstr:   $status")

                prefProvider.setValue(ORDER_TYPE, DINE_IN)
                prefProvider.setValue(ORDER_TYPE_NAME, DINE_IN)
                val bundle = bundleOf(
                    "isFromDineIn" to true,
                    "numberOfGuest" to guestCount,
                    "floorplan" to dineInFloorTableModel
                )
                prefProvider.setValueInt(DINE_IN_TABLE_ID, dineInFloorTableModel.id)
                prefProvider.setValueboolean(Constants.DINE_IN_STATUS, true)
                findNavController().navigate(
                    R.id.action_dineInGuestFragment_to_dashboardCategoryNew,
                    bundle
                )

            }
        })

    }


    private fun setUpRecyclerView() {
        guestListAdapter = GuestListAdapter()
        guestListAdapter.setListner(this)
        binding.rvNumberOfGuests.adapter = guestListAdapter

        val numberOfGuestList = ArrayList<Int>()
        for (i in 1..dineInFloorTableModel.chairCount) {
            numberOfGuestList.add(i)
        }
        guestListAdapter.addGuests(numberOfGuestList)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpRecyclerView()
        onClick()

    }

    private fun onClick() {
        binding.imgClose.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onGuestSelected(numberOfGuest: Int) {
        Log.e(TAG, "numberOfGuest:  $numberOfGuest")
        guestCount = numberOfGuest


        viewModel.getTableStatus(dineInFloorTableModel.id, "Occupied")


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
        viewModel.tableCheck.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let { status ->
                Log.e(TAG, "getstr:   $status")

                AlertUtils.showCustomAlertWithListenerWithOK(
                    requireContext(),
                    status
                ) { _, _ ->


                }

            }
        })
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

        viewModel.snackbarText.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                AlertUtils.showCustomAlert(requireContext(), it)
            }
        })


    }

}