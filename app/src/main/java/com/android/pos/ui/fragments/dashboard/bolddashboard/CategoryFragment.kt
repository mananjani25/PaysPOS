package com.android.pos.ui.fragments.dashboard.bolddashboard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.CategoryParentModel
import com.android.pos.data.model.CategoryTabModel
import com.android.pos.databinding.FragmentCategoryBinding
import com.android.pos.ui.adapter.CategoryTabAdapter1
import com.android.pos.ui.adapter.boldpos.CategoryParentAdapter
import com.android.pos.ui.adapter.boldpos.CategoryTabAdapter

class CategoryFragment : Fragment(), CategoryTabAdapter1.TabListner {
    private lateinit var binding: FragmentCategoryBinding
    private lateinit var categoryParentAdapter: CategoryParentAdapter
    private val TAG = "CategoryFragment"
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCategoryBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setAdapter()


    }

    private fun setAdapter() {

        var list: ArrayList<CategoryParentModel> = arrayListOf()
        var listCategories: ArrayList<CategoryTabModel> = arrayListOf()
        for (i in 0 until 8) {
            listCategories.add(CategoryTabModel(0, "Drinks", if (i == 0) true else false, 0))
        }
        list.add(CategoryParentModel(listCategories))
        list.add(CategoryParentModel(listCategories))
        categoryParentAdapter = CategoryParentAdapter(requireContext(), list, this)
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