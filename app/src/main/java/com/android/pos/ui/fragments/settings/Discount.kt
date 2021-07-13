package com.android.pos.ui.fragments.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.model.DiscountListModel
import com.android.pos.databinding.DiscountFragmentBinding
import com.android.pos.databinding.FragmentDiscountBinding
import com.android.pos.ui.adapter.DiscountListAdapter

class Discount : Fragment() {
    private lateinit var binding: DiscountFragmentBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DiscountFragmentBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setAdapter()
        onClick()

    }

    private fun onClick() {
        binding.txtCreateDiscount.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_createDiscount)
        }
    }

    private fun setAdapter() {
        val list: ArrayList<DiscountListModel> = arrayListOf()
        list.add(DiscountListModel(0, "Staff Meal", "20%", false))
        list.add(DiscountListModel(0, "Military", "15%", false))
        list.add(DiscountListModel(0, "Senior Citizen", "25%", false))
        binding.rvDiscountList.adapter = DiscountListAdapter(requireContext(), list)

    }

}