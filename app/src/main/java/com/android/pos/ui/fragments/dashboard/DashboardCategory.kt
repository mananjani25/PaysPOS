package com.android.pos.ui.fragments.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.android.pos.R
import com.android.pos.data.model.DashboardItemModel
import com.android.pos.databinding.FragmentDashboardCategoryBinding
import com.android.pos.ui.activities.MainActivity
import com.android.pos.ui.adapter.CategoryViewPagerAdapter
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DashboardCategory : Fragment() {

    private lateinit var binding: FragmentDashboardCategoryBinding
    private val listTabs: ArrayList<String> = arrayListOf(
        "Category One",
        "Category Two",
        "Category Three",
        "Category Four",
        "Category Five",
        "Category Six"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_dashboard_category,
            container,
            false
        )
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setTabs()
        setViewPager()
        // binding.tabLayout.setupWithViewPager(binding.viewPagerCategory)
        TabLayoutMediator(binding.tabLayout, binding.viewPagerCategory) { tab, position ->
            // binding.tabLayout.getTabAt(position).setText()
            tab.text = listTabs[position]
        }.attach()

        binding.layoutMenu.imgDrawer.setOnClickListener {
            (requireActivity() as MainActivity).enableDrawer()
        }


    }

    private fun setViewPager() {
        var list: ArrayList<DashboardItemModel> = arrayListOf()
        list.add(DashboardItemModel(0, "TP", "Three Piece...", "15.33"))
        list.add(DashboardItemModel(0, "TP", "Three Piece...", "15.33"))
        list.add(DashboardItemModel(0, "TP", "Three Piece...", "15.33"))
        list.add(DashboardItemModel(0, "TP", "Three Piece...", "15.33"))
        list.add(DashboardItemModel(0, "TP", "Three Piece...", "15.33"))
        list.add(DashboardItemModel(0, "TP", "Three Piece...", "15.33"))
        list.add(DashboardItemModel(0, "TP", "Three Piece...", "15.33"))
        list.add(DashboardItemModel(0, "TP", "Three Piece...", "15.33"))
        list.add(DashboardItemModel(0, "TP", "Three Piece...", "15.33"))
        list.add(DashboardItemModel(0, "TP", "Three Piece...", "15.33"))
        list.add(DashboardItemModel(0, "TP", "Three Piece...", "15.33"))
        list.add(DashboardItemModel(0, "TP", "Three Piece...", "15.33"))
        list.add(DashboardItemModel(0, "TP", "Three Piece...", "15.33"))
        list.add(DashboardItemModel(0, "TP", "Three Piece...", "15.33"))
        list.add(DashboardItemModel(0, "TP", "Three Piece...", "15.33"))
        list.add(DashboardItemModel(0, "TP", "Three Piece...", "15.33"))
        list.add(DashboardItemModel(0, "TP", "Three Piece...", "15.33"))
        list.add(DashboardItemModel(0, "TP", "Three Piece...", "15.33"))
        list.add(DashboardItemModel(0, "TP", "Three Piece...", "15.33"))

        binding.viewPagerCategory.adapter = CategoryViewPagerAdapter(requireActivity(),list, binding.tabLayout.tabCount)

    }

    private fun setTabs() {
        for (i in 0 until listTabs.size) {
            binding.tabLayout.addTab(binding.tabLayout.newTab().setText(listTabs[i]))
        }


    }

    /*inner class ViewPagerAdapter(var list: ArrayList<DashboardItemModel>, var tabCount: Int) :
        FragmentStateAdapter(requireActivity()) {
        override fun getItemCount(): Int {
            return tabCount

        }

        override fun createFragment(position: Int): Fragment {
            return CategoryList.newInstance(list)
        }

    }*/
}