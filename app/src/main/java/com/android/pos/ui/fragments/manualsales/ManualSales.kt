package com.android.pos.ui.fragments.manualsales

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.pos.databinding.FragmentManualSalesBinding

class ManualSales : Fragment() {
    private lateinit var binding: FragmentManualSalesBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentManualSalesBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onClick()
    }

    private fun onClick() {
        binding.imgCacncel.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.txtHome.setOnClickListener {
            findNavController().popBackStack()
        }
    }
}