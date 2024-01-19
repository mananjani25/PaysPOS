package com.pays.pos.ui.fragments.inventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.pays.pos.R
import com.pays.pos.databinding.FragmentDiscountBinding

class LoyaltyFragment : Fragment() {
    private lateinit var binding: FragmentDiscountBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDiscountBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

      //  setAdapter()
        onClick()
    }

  /*  private fun setAdapter() {
        val list: ArrayList<DiscountListModel> = arrayListOf()
        list.add(DiscountListModel(0, "Discount 1", "20%", false))
        list.add(DiscountListModel(0, "Discount 2", "15%", false))
        list.add(DiscountListModel(0, "Discount 3", "25%", false))
        binding.rvDiscountList.adapter = DiscountListAdapter(requireContext(), list)

    }*/

    private fun onClick() {
        binding.txtCreateDiscount.setOnClickListener {
            findNavController().navigate(R.id.action_inventory_to_createDiscount)
        }


    }


}