package com.android.pos.ui.fragments.inventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.android.pos.R
import com.android.pos.data.model.InventoryItemModel
import com.android.pos.databinding.FragmentInventoryBinding
import com.android.pos.ui.adapter.InventoryAdapter

class Inventory : Fragment() {
    private lateinit var binding: FragmentInventoryBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_inventory, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setAdapter()
    }

    private fun setAdapter() {
        val list:ArrayList<InventoryItemModel> = arrayListOf()
        list.add(InventoryItemModel(0,"All Items"))
        list.add(InventoryItemModel(0,"Categories"))
        list.add(InventoryItemModel(0,"Modifiers"))
        list.add(InventoryItemModel(0,"Discounts"))
        list.add(InventoryItemModel(0,"Options"))
        binding.recyclerViewItemsList.adapter = InventoryAdapter(requireContext(),list)


    }
}