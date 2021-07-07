package com.android.pos.ui.fragments.createmodifier

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.databinding.CreateItemBinding
import com.android.pos.databinding.CreateModifierSetBinding

class CreateModifierSet : Fragment() {

    private lateinit var binding: CreateModifierSetBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.create_modifier_set, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //  onClick()
    }

    private fun onClick() {
//        binding.txtSave.setOnClickListener {
//            findNavController().navigate(R.id.action_createItem_to_createCategory)
//        }
    }
}