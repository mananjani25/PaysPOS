package com.android.pos.ui.fragments.dashboard

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.android.pos.R
import com.android.pos.data.model.responseModel.VenueDataResponse
import com.android.pos.databinding.FragmentDashboardCategoryBinding
import com.android.pos.ui.activities.MainActivity
import com.android.pos.ui.adapter.CategoryViewPagerAdapter
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.statusUtils.Status
import com.google.android.material.internal.ViewUtils.dpToPx
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DashboardCategory : Fragment() {

    private lateinit var binding: FragmentDashboardCategoryBinding
    private val viewModel by viewModels<DashBoardCategoryViewModel>()
    private var categoryList: List<VenueDataResponse.Data.Category> = arrayListOf()
    private  var categoryTabsList: ArrayList<String> = arrayListOf()
    private var isFlag = false

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

        /* binding.layoutMenu.imgOptionMenu.setOnClickListener {

             if (isFlag) {
                 isFlag = false
                 binding.tabLayout.rotation = 0F
             } else {
                 isFlag = true
                 binding.tabLayout.rotation = 270F

             }
         }*/

        binding.lifecycleOwner = this

        setVenueData()

        binding.viewPagerCategory.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.viewPagerCategory.adapter = CategoryViewPagerAdapter(
                    requireActivity(),
                    categoryList[position].items,
                    binding.tabLayout.tabCount
                )
            }
        })
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.layoutMenu.imgDrawer.setOnClickListener {
            (requireActivity() as MainActivity).enableDrawer()
        }


    }

    private fun setVenueData() {
        viewModel.venueData.observe(viewLifecycleOwner, {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {

                        ProgressUtils.dismissProgressDialog()

                        resource.data?.let { it ->

                            categoryList = it.data.categories
                            if (categoryList.isNotEmpty()) {
                                categoryList.forEach {
                                    categoryTabsList.add(it.name)
                                    binding.tabLayout.addTab(
                                        binding.tabLayout.newTab().setText(it.name)
                                    )
                                    //   itemList.addAll(it.items)


                                }

                                TabLayoutMediator(
                                    binding.tabLayout,
                                    binding.viewPagerCategory
                                ) { tab, position ->
                                    tab.text = categoryList[position].name
                                }.attach()
                            }


                        }
                    }
                    Status.ERROR -> {
                        ProgressUtils.dismissProgressDialog()

                    }
                    Status.LOADING -> {
                        ProgressUtils.showProgressDialog(requireActivity())
                    }
                }
            }
        })
    }

}