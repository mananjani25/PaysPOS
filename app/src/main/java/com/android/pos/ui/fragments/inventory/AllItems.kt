package com.android.pos.ui.fragments.inventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.android.pos.data.model.AllItemModel
import com.android.pos.databinding.FragmentItemsBinding
import com.android.pos.ui.adapter.ItemListAdapter

class AllItems : Fragment() {
    private lateinit var binding: FragmentItemsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentItemsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setAdapter()
    }

    private fun setAdapter() {
        var list: ArrayList<AllItemModel> = arrayListOf()
        list.add(AllItemModel(0, "Chicken", "Variable", ""))
        list.add(AllItemModel(0, "Chicken Biriyani", "Variable", ""))
        list.add(AllItemModel(0, "Chicken Handi", "$10.12", ""))
        list.add(AllItemModel(0, "Butter Chicken", "$15.10", ""))
        binding.rvAllItemList.adapter = ItemListAdapter(requireContext(), list)
    }
}