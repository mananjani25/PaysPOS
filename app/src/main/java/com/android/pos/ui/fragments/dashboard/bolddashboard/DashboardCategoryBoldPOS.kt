package com.android.pos.ui.fragments.dashboard.bolddashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import com.android.pos.databinding.FragmentDashboardCategoryBoldPosBinding
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DashboardCategoryBoldPOS : Fragment() {
    private lateinit var binding: FragmentDashboardCategoryBoldPosBinding
    private val viewModel by activityViewModels<DashBoardCategoryViewModel>()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentDashboardCategoryBoldPosBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onClick()
        loadCartFragment(CartFragment())
        loadCategoryFragment(CategoryFragment())


    }

    private fun loadCategoryFragment(fragment: Fragment) {
        val fm: FragmentManager = requireActivity().supportFragmentManager
        fm.beginTransaction().replace(binding.frameLayout.id, fragment).commit()
    }

    private fun loadCartFragment(frag: Fragment) {
        val fm: FragmentManager = requireActivity().supportFragmentManager
        fm.beginTransaction().replace(binding.frameLayoutCart.id, frag).commit()
    }

    private fun onClick() {
        binding.layoutHeader.txtDineIn.setOnClickListener {

        }
        binding.layoutHeader.txtKeypad.setOnClickListener {

        }
        binding.layoutHeader.imgDrawer.setOnClickListener {

        }

        binding.layoutHeader.txtTransaction.setOnClickListener {

        }

        binding.layoutHeader.imgSync.setOnClickListener {
            viewModel.syncInventoryModule()
        }
    }

}