package com.android.pos.ui.fragments.dashboard.bolddashboard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.entities.CategoryWithInventory
import com.android.pos.data.model.CategoryParentModel
import com.android.pos.data.model.CategoryTabModel
import com.android.pos.databinding.FragmentCategoryBinding
import com.android.pos.ui.adapter.CategoryTabAdapter1
import com.android.pos.ui.adapter.boldpos.CategoryParentAdapter
import com.android.pos.ui.adapter.boldpos.CategoryTabAdapter
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.statusUtils.Status
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CategoryFragment : Fragment(), CategoryTabAdapter1.TabListner {
    private var categoryList1: ArrayList<CategoryWithInventory> = arrayListOf()
    private lateinit var binding: FragmentCategoryBinding
    private lateinit var categoryParentAdapter: CategoryParentAdapter
    private val viewModel by activityViewModels<DashBoardCategoryViewModel>()
    private var tabList: ArrayList<CategoryTabModel> = arrayListOf()
    private val TAG = "CategoryFragment"
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        observeShowProgress()
        binding = FragmentCategoryBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setAdapter()
        setVenueData()

    }

    private fun setVenueData() {
        viewModel.venueDataLocal().observe(
            viewLifecycleOwner
        ) {
            when (it.status) {
                Status.SUCCESS -> {

                    val tbCategory = it.data
                    if (tbCategory != null) {
                        Log.e(TAG, "tbCategory  ${Gson().toJson(tbCategory)}")
                        tabList.clear()
                        tabList = arrayListOf()



                        categoryList1 = tbCategory as ArrayList<CategoryWithInventory>
                        var list: ArrayList<CategoryParentModel> = arrayListOf()
                        var tmpTabList: ArrayList<CategoryTabModel> = arrayListOf()
                        Log.e(TAG, "categorySize: ${categoryList1.size}")
                        for (i in 0 until categoryList1.size) {


                            tabList.add(
                                CategoryTabModel(
                                    categoryList1[i].category.id,
                                    categoryList1[i].category.name ?: "",
                                    if (i == 0) true else false,
                                    0
                                )
                            )

                            tmpTabList.add(
                                CategoryTabModel(
                                    categoryList1[i].category.id,
                                    categoryList1[i].category.name ?: "",
                                    if (i == 0) true else false,
                                    0
                                )

                            )



                            Log.e(TAG, "tabListSize  ${tmpTabList.size}")
                            if (tmpTabList.size == 8) {
                                Log.e(TAG, "AddedFirst")
                                list.add(CategoryParentModel(tmpTabList))

                                tmpTabList = arrayListOf()

                            } else if (tmpTabList.size < 8 && tmpTabList.size == categoryList1.size) {
                                Log.e(TAG, "AddedFirstSecond")
                                list.add(CategoryParentModel(tmpTabList))
                            }


                        }
                        Log.e(TAG, "listlist:  ${Gson().toJson(list)}")
                        categoryParentAdapter.addList(list)

                    }
                    ProgressUtils.dismissProgressDialog()


                }
                Status.ERROR ->
                    ProgressUtils.dismissProgressDialog()

                Status.LOADING -> ProgressUtils.showProgressDialog(requireActivity())

            }
        }

    }

    private fun observeShowProgress() {
        viewModel.showProgress.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        })
    }


    private fun setAdapter() {

        var list: ArrayList<CategoryParentModel> = arrayListOf()
        var listCategories: ArrayList<CategoryTabModel> = arrayListOf()
        for (i in 0 until 8) {
            listCategories.add(CategoryTabModel(0, "Drinks", if (i == 0) true else false, 0))
        }
        list.add(CategoryParentModel(listCategories))
        list.add(CategoryParentModel(listCategories))
        categoryParentAdapter = CategoryParentAdapter(requireContext(), arrayListOf(), this)
        binding.rvCategoryParent.adapter = categoryParentAdapter
        binding.rvCategoryParent.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        var snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(binding.rvCategoryParent)

        binding.rvCategoryParent.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                Log.e(TAG, "dx  ${dx}")
                Log.e(TAG, "dy  ${dy}")


            }


        })
        var tabList: ArrayList<CategoryTabModel> = arrayListOf()
        for (i in 0 until list.size) {
            tabList.add(CategoryTabModel(0, "", if (i == 0) true else false, 0))
        }

        binding.rvTabLayout.adapter = CategoryTabAdapter(tabList)

    }

    override fun onTabSelected(pos: Int) {

    }
}