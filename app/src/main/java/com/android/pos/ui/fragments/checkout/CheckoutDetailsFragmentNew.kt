package com.android.pos.ui.fragments.checkout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.android.pos.data.entities.TbItem
import com.android.pos.databinding.FragmentCheckoutDetailsNewBinding
import com.android.pos.ui.adapter.PaymentTypePagerAdapter
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.utils.callback.ItemListner
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CheckoutDetailsFragmentNew : Fragment(), ItemListner {
    private lateinit var binding: FragmentCheckoutDetailsNewBinding
    private val viewModel by activityViewModels<DashBoardCategoryViewModel>()
    private val TAG = "DashboardCategoryBold"
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentCheckoutDetailsNewBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onClick()
        setPagerAdapter()
        //loadCartFragment(CartFragment())
        //loadCategoryFragment(SplitCustomAmountFragment())


    }

    private fun setPagerAdapter() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Pay Full Amount"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Split Custom Amount"))
        binding.tabLayout.tabGravity = TabLayout.GRAVITY_FILL

        val bundle = Bundle().apply {
            putInt("frameLayoutId", binding.frameLayoutId.id)
            putInt("llRoot", binding.llRoot.id)
        }
        val adapter = PaymentTypePagerAdapter(
            requireContext(),
            childFragmentManager,
            binding.tabLayout.tabCount,
            bundle
        )
        binding.frameLayout.adapter = adapter
        binding.frameLayout.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(binding.tabLayout))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab?) {

                binding.frameLayout.currentItem = tab?.position!!

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })
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