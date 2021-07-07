package com.android.pos.ui.fragments.loginscreen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.model.DashboardItemModel
import com.android.pos.databinding.FragmentDashboardBinding
import com.android.pos.ui.adapter.DashboardItemAdapter

class Dashboard : Fragment() {
    private lateinit var binding: FragmentDashboardBinding
    private var listItem: ArrayList<DashboardItemModel> = arrayListOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_dashboard, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setAdapter()
    }

    private fun setAdapter() {

        listItem.add(DashboardItemModel(0, "TP", "Three Piece...", "3.50", false))
        listItem.add(DashboardItemModel(0, "TP", "Three Piece...", "3.50", false))
        listItem.add(DashboardItemModel(0, "TP", "Three Piece...", "3.50", true))
        listItem.add(DashboardItemModel(0, "TP", "Three Piece...", "3.50", true))
        listItem.add(DashboardItemModel(0, "TP", "Three Piece...", "3.50", true))
        listItem.add(DashboardItemModel(0, "TP", "Three Piece...", "3.50", true))
        listItem.add(DashboardItemModel(0, "TP", "Three Piece...", "3.50", true))
        listItem.add(DashboardItemModel(0, "TP", "Three Piece...", "3.50", true))
        listItem.add(DashboardItemModel(0, "TP", "Three Piece...", "3.50", true))
        listItem.add(DashboardItemModel(0, "TP", "Three Piece...", "3.50", true))
        listItem.add(DashboardItemModel(0, "TP", "Three Piece...", "3.50", true))
        listItem.add(DashboardItemModel(0, "TP", "Three Piece...", "3.50", true))
        listItem.add(DashboardItemModel(0, "TP", "Three Piece...", "3.50", true))
        listItem.add(DashboardItemModel(0, "TP", "Three Piece...", "3.50", true))
        listItem.add(DashboardItemModel(0, "TP", "Three Piece...", "3.50", true))
        listItem.add(DashboardItemModel(0, "TP", "Three Piece...", "3.50", true))
        listItem.add(DashboardItemModel(0, "TP", "Three Piece...", "3.50", true))
        listItem.add(DashboardItemModel(0, "TP", "Three Piece...", "3.50", true))
        listItem.add(DashboardItemModel(0, "TP", "Three Piece...", "3.50", true))
        listItem.add(DashboardItemModel(0, "TP", "Three Piece...", "3.50", true))
        listItem.add(DashboardItemModel(0, "TP", "Three Piece...", "3.50", true))
        listItem.add(DashboardItemModel(0, "TP", "Three Piece...", "3.50", true))


        binding.recyclerViewItemsList.adapter = DashboardItemAdapter(
            requireContext(),
            listItem,
            object : DashboardItemAdapter.DashboardListner {
                override fun onItemClick() {
                    findNavController().navigate(R.id.action_dashboard_to_createItem)
                }

            })

    }
}