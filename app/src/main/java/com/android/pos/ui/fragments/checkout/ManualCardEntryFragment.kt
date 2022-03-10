package com.android.pos.ui.fragments.checkout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import com.android.pos.data.entities.TbItem
import com.android.pos.databinding.FragmentCheckoutDetailsNewBinding
import com.android.pos.databinding.FragmentManualCardEntryBinding
import com.android.pos.ui.adapter.PaymentTypePagerAdapter
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.utils.callback.ItemListner
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ManualCardEntryFragment : Fragment(), ItemListner {
    private lateinit var binding: FragmentManualCardEntryBinding
    private val viewModel by activityViewModels<DashBoardCategoryViewModel>()
    private val TAG = "DashboardCategoryBold"
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentManualCardEntryBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onClick()
    }




    private fun onClick() {
    }

    override fun onItemSelected(item: TbItem) {
       /* Log.e(TAG, "getitem:  ${Gson().toJson(item)}")
        val fragment = AddItemFragment.newInstance(item)
        loadCategoryFragment(fragment)*/

    }

    override fun onCancelItemSelected() {

    }

}