package com.android.pos.ui.fragments.dashboard

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.*
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
    private var categoryList: MutableList<VenueDataResponse.Data.Category> = arrayListOf()
    private var itemList: ArrayList<VenueDataResponse.Data.Category.Item> = arrayListOf()
    private var categoryTabsList: ArrayList<String> = arrayListOf()
    private var isFlag = false
    override fun onPause() {
        super.onPause()

    }

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


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setVenueData()

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

                        resource.data?.let { category ->

                            categoryList = category.data.categories.toMutableList()
                            if (categoryList.isNotEmpty()) {
                                categoryList.forEach {
                                    categoryTabsList.add(it.name)
                                    binding.tabLayout.addTab(
                                        binding.tabLayout.newTab().setText(it.name)
                                    )
                                    itemList.addAll(it.items)

                                }
                                binding.viewPagerCategory.adapter = CategoryViewPagerAdapter(
                                    requireActivity(),
                                    categoryList,
                                    binding.tabLayout.tabCount
                                )


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
                        try {
                            val activity: Activity = requireActivity() as Activity
                            if (!activity.isFinishing) {
                                ProgressUtils.showProgressDialog(requireActivity())
                            }
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                    }
                }
            }
        })
    }

}