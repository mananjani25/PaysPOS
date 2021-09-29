package com.android.pos.ui.fragments.dinein

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.model.responseModel.GetFloorPlanResponse
import com.android.pos.data.remote.Constants.DINE_IN
import com.android.pos.data.remote.Constants.ORDER_TYPE
import com.android.pos.data.remote.Constants.ORDER_TYPE_NAME
import com.android.pos.databinding.FragmentDineInGuestBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.GuestListAdapter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DineInGuestFragment : Fragment(), GuestListAdapter.GuestListner {
    private lateinit var binding: FragmentDineInGuestBinding
    private lateinit var dineInFloorTableModel: GetFloorPlanResponse.Data.FloorPlanTable
    private lateinit var guestListAdapter: GuestListAdapter
    private val TAG = "DineInGuestFragment"

    @Inject
    lateinit var prefProvider: PrefProvider
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDineInGuestBinding.inflate(inflater, container, false)

        dineInFloorTableModel = arguments?.getParcelable("dineInFloorTableObject")!!
        return binding.root
    }


    private fun setUpRecyclerView() {
        guestListAdapter = GuestListAdapter()
        guestListAdapter.setListner(this)
        binding.rvNumberOfGuests.adapter = guestListAdapter

        val numberOfGuestList = ArrayList<Int>()
        for (i in 1..20) {
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
        prefProvider.setValue(ORDER_TYPE, DINE_IN)
        prefProvider.setValue(ORDER_TYPE_NAME, DINE_IN)
        val bundle = bundleOf("isFromDineIn" to true, "numberOfGuest" to numberOfGuest)
        findNavController().navigate(
            R.id.action_dineInGuestFragment_to_dashboardCategoryNew,
            bundle
        )
    }

}