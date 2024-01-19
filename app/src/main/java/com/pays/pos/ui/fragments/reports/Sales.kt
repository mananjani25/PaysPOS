package com.pays.pos.ui.fragments.reports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.pays.pos.databinding.FragmentSalesBinding

class Sales : Fragment() {
    private lateinit var binding: FragmentSalesBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSalesBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setTabs()
    }

    private fun setTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("1D"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("1W"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("1M"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("3M"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("1Y"))


    }

}