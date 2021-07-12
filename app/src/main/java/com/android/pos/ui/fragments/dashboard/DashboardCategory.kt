package com.android.pos.ui.fragments.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.android.pos.R
import com.android.pos.data.model.DashboardItemModel
import com.android.pos.databinding.FragmentDashboardCategoryBinding
import com.android.pos.ui.activities.MainActivity
import com.android.pos.ui.adapter.CategoryViewPagerAdapter
import com.android.pos.utils.statusUtils.Status
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DashboardCategory : Fragment() {

    private lateinit var binding: FragmentDashboardCategoryBinding
    private val viewModel by viewModels<DashBoardCategoryViewModel>()
    private lateinit var categoryTabsList: ArrayList<String>

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

        setVenueData()

        return binding.root
    }

    private fun setVenueData() {
        viewModel.venueData.observe(viewLifecycleOwner, {


            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        resource.data?.let {

                            val mList = it.data.categories
                            if (mList.isNotEmpty()) {
                                mList.forEach {
                                    categoryTabsList.add(it.name)
                                    binding.tabLayout.addTab(binding.tabLayout.newTab().setText(it.name))
                                 //   it.items

                                }
                              //  binding.viewPagerCategory.adapter = CategoryViewPagerAdapter(requireActivity(),it.items, binding.tabLayout.tabCount)
                            }

                        }
                    }
                    Status.ERROR -> {
                       /* binding.recyclerView.visibility = View.VISIBLE
                        binding.progressBar.visibility = View.GONE*/

                    }
                    Status.LOADING -> {
                       /* binding.progressBar.visibility = View.VISIBLE
                        binding.recyclerView.visibility = View.GONE*/
                    }
                }
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

       // setTabs()
        setViewPager()
        // binding.tabLayout.setupWithViewPager(binding.viewPagerCategory)
        TabLayoutMediator(binding.tabLayout, binding.viewPagerCategory) { tab, position ->
            // binding.tabLayout.getTabAt(position).setText()
            tab.text = categoryTabsList[position]
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

       /* binding.viewPagerCategory.adapter =
            CategoryViewPagerAdapter(requireActivity(), list, binding.tabLayout.tabCount)*/

    }

    /*private fun setTabs() {
        for (i in 0 until listTabs.size) {
            binding.tabLayout.addTab(binding.tabLayout.newTab().setText(listTabs[i]))
        }


    }*/

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