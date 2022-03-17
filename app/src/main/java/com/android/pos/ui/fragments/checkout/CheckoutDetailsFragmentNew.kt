package com.android.pos.ui.fragments.checkout

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.android.pos.data.entities.TbItem
import com.android.pos.databinding.FragmentCheckoutDetailsNewBinding
import com.android.pos.ui.adapter.PaymentTypePagerAdapter
import com.android.pos.utils.callback.ItemListner
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CheckoutDetailsFragmentNew : Fragment(), ItemListner {
    private lateinit var binding: FragmentCheckoutDetailsNewBinding
    private val TAG = "DashboardCategoryBold"

    private var orderId: Int? = null
    private var orderOfflineId: String = ""
    private var paymentOfflineId: String = ""
    private var paymentId: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentCheckoutDetailsNewBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        orderId = arguments?.getInt("orderId")

        Log.e("orderId :: ", orderId.toString())
        if (orderId != null) {
            paymentId = arguments?.getInt("paymentId")!!
            paymentOfflineId = arguments?.getString("paymentOfflineId").toString()
            orderOfflineId = arguments?.getString("orderOfflineId").toString()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onClick()
        setPagerAdapter()



    }

    private fun setPagerAdapter() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("PAY FULL AMOUNT"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("SPLIT CUSTOM AMOUNT"))
        binding.tabLayout.tabGravity = TabLayout.GRAVITY_FILL

        val bundle = Bundle().apply {
            putInt("frameLayoutId", binding.frameLayoutId.id)
            putInt("llRoot", binding.llRoot.id)
            orderId?.let { putInt("orderId", it) }
            if (orderId != null) {
                putInt("paymentId", paymentId)
                putString("orderOfflineId", orderOfflineId)
                putString("paymentOfflineId", paymentOfflineId)
            }
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

    override fun onCategorySelected(item: TbItem) {

    }

}