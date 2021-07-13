package com.android.pos.ui.fragments.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.android.pos.R
import com.android.pos.data.model.responseModel.VenueDataResponse
import com.android.pos.databinding.FragmentDashboardCategoryBinding
import com.android.pos.ui.activities.MainActivity
import com.android.pos.ui.adapter.CategoryViewPagerAdapter
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.statusUtils.Status
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DashboardCategory : Fragment() {

    private lateinit var binding: FragmentDashboardCategoryBinding
    private val viewModel by viewModels<DashBoardCategoryViewModel>()
    private var categoryTabsList: ArrayList<String> = arrayListOf()
    private var itemList: ArrayList<VenueDataResponse.Data.Category.Item> = arrayListOf()

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

                        ProgressUtils.dismissProgressDialog()

                        resource.data?.let { it ->

                            val mList = it.data.categories
                            if (mList.isNotEmpty()) {
                                mList.forEach {
                                    categoryTabsList.add(it.name)
                                    binding.tabLayout.addTab(
                                        binding.tabLayout.newTab().setText(it.name)
                                    )
                                    //   it.items
                                    itemList.addAll(it.items)

                                }

                                binding.viewPagerCategory.adapter = CategoryViewPagerAdapter(
                                    requireActivity(),
                                    itemList,
                                    binding.tabLayout.tabCount
                                )
                                TabLayoutMediator(
                                    binding.tabLayout,
                                    binding.viewPagerCategory
                                ) { tab, position ->
                                    // binding.tabLayout.getTabAt(position).setText()
                                    tab.text = categoryTabsList[position]
                                }.attach()
                            }


                        }
                    }
                    Status.ERROR -> {
                        /* binding.recyclerView.visibility = View.VISIBLE
                         binding.progressBar.visibility = View.GONE*/

                        ProgressUtils.dismissProgressDialog()

                    }
                    Status.LOADING -> {
                        ProgressUtils.showProgressDialog(requireActivity())
                        /* binding.progressBar.visibility = View.VISIBLE
                         binding.recyclerView.visibility = View.GONE*/
                    }
                }
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.layoutMenu.imgDrawer.setOnClickListener {
            (requireActivity() as MainActivity).enableDrawer()
        }

    }
}