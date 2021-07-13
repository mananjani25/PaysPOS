package com.android.pos.ui.fragments.inventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.android.pos.data.model.OptionListModel
import com.android.pos.databinding.FragmentOptionsBinding
import com.android.pos.ui.adapter.OptionListAdapter

class Options : Fragment() {
    private lateinit var binding: FragmentOptionsBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOptionsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setAdapter()
    }

    private fun setAdapter() {
        val list: ArrayList<OptionListModel> = arrayListOf()
        list.add(OptionListModel(0, "Drink Size", "3 Options"))
        list.add(OptionListModel(0, "Small", "5 Options"))
        binding.rvOptonList.adapter = OptionListAdapter(requireContext(), list)
    }
}