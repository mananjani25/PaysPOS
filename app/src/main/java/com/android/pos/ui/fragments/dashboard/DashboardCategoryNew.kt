package com.android.pos.ui.fragments.dashboard

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.model.CategoryTabModel
import com.android.pos.data.model.responseModel.VenueDataResponse
import com.android.pos.databinding.FragmentDashboardCategoryNewBinding
import com.android.pos.ui.activities.MainActivity
import com.android.pos.ui.adapter.CategoryItemAdapter
import com.android.pos.ui.adapter.CategoryTabAdapter
import com.android.pos.ui.adapter.CategoryViewPagerAdapter
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.statusUtils.Status
import com.google.android.material.tabs.TabLayoutMediator
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.internal.notify

@AndroidEntryPoint
class DashboardCategoryNew : Fragment() {
    private lateinit var binding: FragmentDashboardCategoryNewBinding
    private val TAG = "DashboardCategoryNew"

    private val viewModel by viewModels<DashBoardCategoryViewModel>()
    private var categoryList: MutableList<VenueDataResponse.Data.Category> = arrayListOf()
    private var itemList: ArrayList<VenueDataResponse.Data.Category.Item> = arrayListOf()
    private var categoryTabsList: ArrayList<String> = arrayListOf()
    private var tabList: ArrayList<CategoryTabModel> = arrayListOf()
    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDashboardCategoryNewBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setVenueData()
        configureDrawer()
    }

    private fun configureDrawer() {
        binding.layoutMenu.txtKeypad.setOnClickListener {
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

                            for (i in 0 until categoryList.size) {

                                if (i == 0) {
                                    tabList.add(
                                        CategoryTabModel(
                                            0,
                                            categoryList.get(i).name,
                                            true,
                                            0
                                        )
                                    )
                                } else {
                                    tabList.add(
                                        CategoryTabModel(
                                            0,
                                            categoryList.get(i).name,
                                            false,
                                            0
                                        )
                                    )
                                }
                            }
                            if (categoryList.isNotEmpty()) {
                                categoryList.forEach {
                                    categoryTabsList.add(it.name)

                                    binding.rvTabLayout.adapter = CategoryTabAdapter(
                                        requireContext(),
                                        tabList,
                                        object : CategoryTabAdapter.TabListner {
                                            override fun onTabSelected(pos: Int) {
                                                Log.e("CatTab", "CatTab $pos")
                                                var listCategories =
                                                    (binding.rvPagerCategory?.adapter as CategoryItemAdapter).list
                                                listCategories.clear()
                                                Log.e(
                                                    TAG,
                                                    "categoryListData  ${categoryList.get(pos).items}"
                                                )
                                                listCategories.addAll(categoryList.get(pos).items)
                                                (binding.rvPagerCategory?.adapter as CategoryItemAdapter).list =
                                                    listCategories
                                                if (listCategories.isNotEmpty()) {
                                                    binding.rvPagerCategory?.adapter?.notifyDataSetChanged()
                                                } else {
                                                    (binding.rvPagerCategory?.adapter as CategoryItemAdapter).list?.clear()
                                                    binding.rvPagerCategory?.adapter?.notifyDataSetChanged()
                                                }

                                            }
                                        })
                                    itemList.clear()
                                    Log.e("CatList", "${Gson().toJson(categoryList.get(0).items)}")
                                    itemList.addAll(categoryList.get(0).items)

                                    binding.rvPagerCategory.adapter =
                                        CategoryItemAdapter(requireContext(), itemList, object :
                                            CategoryItemAdapter.CategoryItemList {
                                            override fun onClick() {
                                                findNavController().navigate(R.id.action_dashboardCategoryNew_to_createItem)
                                            }

                                        })
                                }
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