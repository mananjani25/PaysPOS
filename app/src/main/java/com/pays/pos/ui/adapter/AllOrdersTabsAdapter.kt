package com.pays.pos.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.pays.pos.ui.fragments.allorders.AllOrdersCountsFragment
import com.pays.pos.ui.fragments.onlineorder.OnlineOrderFragment

class AllOrdersTabsAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
    FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int {
        return 4
    }

    override fun createFragment(position: Int): Fragment {
        return AllOrdersCountsFragment.newInstance(tabPos = position)
    }
}