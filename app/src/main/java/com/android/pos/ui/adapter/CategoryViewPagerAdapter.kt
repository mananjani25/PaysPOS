package com.android.pos.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.android.pos.data.model.DashboardItemModel
import com.android.pos.ui.fragments.dashboard.CategoryList

class CategoryViewPagerAdapter(
    fa: FragmentActivity,
    val list: ArrayList<DashboardItemModel>,
    val tabCount: Int
) : FragmentStateAdapter(fa) {
    override fun getItemCount(): Int = tabCount

    override fun createFragment(position: Int): Fragment =
        CategoryList.newInstance(list)

}