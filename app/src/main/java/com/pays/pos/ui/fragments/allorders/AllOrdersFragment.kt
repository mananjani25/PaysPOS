package com.pays.pos.ui.fragments.allorders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.pays.pos.R
import com.pays.pos.databinding.FragmentAllOrdersBinding
import com.pays.pos.ui.adapter.AllOrdersTabsAdapter
import com.pays.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

@AndroidEntryPoint
class AllOrdersFragment : Fragment() {

    private lateinit var binding: FragmentAllOrdersBinding

    private val dashboardViewModel by activityViewModels<DashBoardCategoryViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAllOrdersBinding.inflate(layoutInflater)
        configureToolbar()
        setupTabs()
        val onBackPressedCallback: OnBackPressedCallback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController().navigate(R.id.action_allOrder_to_dashboarCategorynew)
                }

            }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            onBackPressedCallback
        )
        return binding.root
    }

    // this is base fragment of ALL ORDERS screen, which contains order types tab
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
                dashboardViewModel.fromAllOrderFragment = true
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
                3 ,4-> {
                    tab.text = "Online ($onlinePendingCount)"
                }
//                4 -> {
//                    tab.text = "3rd Party ($thirdPartyPendingCount)"
//                }
            }

        }.attach()
    }

}