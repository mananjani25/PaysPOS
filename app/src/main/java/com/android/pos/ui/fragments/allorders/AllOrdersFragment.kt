package com.android.pos.ui.fragments.allorders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.databinding.FragmentAllOrdersBinding
import com.android.pos.ui.adapter.AllOrdersTabsAdapter
import com.android.pos.utils.LogUtil
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

@AndroidEntryPoint
class AllOrdersFragment : Fragment() {

    private lateinit var binding: FragmentAllOrdersBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAllOrdersBinding.inflate(layoutInflater)
        configureToolbar()
        setupTabs()
        return binding.root
    }

    private fun setupTabs() {
        val adapter = AllOrdersTabsAdapter(requireActivity().supportFragmentManager, lifecycle)

        binding.viewPager.adapter = adapter
        binding.commonToolbar.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.position?.let { binding.viewPager.setCurrentItem(it, false) }
            }

        })
        updateTabTitle(0,0,0,0,0)
    }

    private fun configureToolbar() {
        binding.commonToolbar.imgDrawer.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_allOrder_to_menuposbold)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        binding.commonToolbar.txtHome.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_allOrder_to_dashboarCategorynew)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        org.greenrobot.eventbus.EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        org.greenrobot.eventbus.EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(pendingCounts: PendingCounts) {
        updateTabTitle(pendingCounts.allPendingCount,pendingCounts.openPendingCount,pendingCounts.phonePendingCount,pendingCounts.onlinePendingCount,pendingCounts.thirdPartyPendingCount)
    }

    private fun updateTabTitle(
        allPendingCount: Int,
        openPendingCount: Int,
        phonePendingCount: Int,
        onlinePendingCount: Int,
        thirdPartyPendingCount: Int
    ) {
        TabLayoutMediator(binding.commonToolbar.tabLayout, binding.viewPager) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = "All Orders ($allPendingCount)"
                }
                1 -> {
                    tab.text = "Open ($openPendingCount)"
                }
                2 -> {
                    tab.text = "Phone ($phonePendingCount)"
                }
                3 -> {
                    tab.text = "Online ($onlinePendingCount)"
                }
                4 -> {
                    tab.text = "3rd Party ($thirdPartyPendingCount)"
                }
            }

        }.attach()
    }

}