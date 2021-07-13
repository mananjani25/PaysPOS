package com.android.pos.ui.fragments.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.android.pos.data.model.BusinessSettingModel
import com.android.pos.databinding.FragmentNotesBinding
import com.android.pos.ui.adapter.NotesListAdapter

class Notes : Fragment() {
    private lateinit var binding: FragmentNotesBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentNotesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setAdapter()
    }

    private fun setAdapter() {
        var list: ArrayList<BusinessSettingModel> = arrayListOf()
        list.add(BusinessSettingModel(0, "Make Fast ", false))
        list.add(BusinessSettingModel(0, "Sauce on Side ", false))
        list.add(BusinessSettingModel(0, "To Go ", false))
        list.add(BusinessSettingModel(0, "With butter ", false))
        list.add(BusinessSettingModel(0, "Appetizer", false))
        list.add(BusinessSettingModel(0, "Vegan", false))
        binding.rvNoteLise.adapter = NotesListAdapter(requireContext(), list)

    }
}