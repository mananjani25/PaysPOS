package com.android.pos.ui.fragments.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.model.DiscountListModel
import com.android.pos.databinding.FragmentTipsBinding
import com.android.pos.ui.adapter.DiscountListAdapter

class Tips : Fragment() {
    private lateinit var binding: FragmentTipsBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentTipsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setAdapter()
        binding.txtAddNewTip.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_addTip)
        }
    }

    private fun setAdapter() {
        var list: ArrayList<DiscountListModel> = arrayListOf()
        list.add(DiscountListModel(0, "Entertainment", "3.45%", false))
        list.add(DiscountListModel(0, "Tip One", "3.45%", false))
        list.add(DiscountListModel(0, "Entertainment Tip Two", "3.45%", false))
        list.add(DiscountListModel(0, "Tip Three", "3.45%", false))
        binding.rvTipList.adapter = DiscountListAdapter(requireContext(), list)
    }
}