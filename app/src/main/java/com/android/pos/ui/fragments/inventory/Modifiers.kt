package com.android.pos.ui.fragments.inventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.android.pos.data.model.ModifiersListModel
import com.android.pos.databinding.FragmentModifiersBinding
import com.android.pos.ui.adapter.ModifiersListAdapter

class Modifiers : Fragment() {
    private lateinit var binding: FragmentModifiersBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentModifiersBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setAdapter()

    }

    private fun setAdapter() {
        var list: ArrayList<ModifiersListModel> = arrayListOf()
        list.add(ModifiersListModel(0, "Toppings", "3 Options"))
        binding.rvModifiersList.adapter = ModifiersListAdapter(requireContext(), list)
    }
}