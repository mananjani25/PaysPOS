package com.android.pos.ui.fragments.dashboard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.android.pos.R
import com.android.pos.data.model.CategoryListItemModel
import com.android.pos.data.model.DashboardItemModel
import com.android.pos.databinding.FragmentCategoryItemListBinding
import com.android.pos.ui.adapter.CategoryItemAdapter
import com.android.pos.ui.adapter.CategoryListItemAdapter

class CategoryList : Fragment() {

    private lateinit var binding: FragmentCategoryItemListBinding


    companion object {
        private val ITEM_LIST = "item_list"
        fun newInstance(list: ArrayList<DashboardItemModel>): CategoryList {
            val args: Bundle = Bundle()
            args.putSerializable(ITEM_LIST, list)
            val fragment = CategoryList()
            fragment.arguments = args
            return fragment
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_category_item_list,
            container,
            false
        )
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val list: ArrayList<DashboardItemModel> =
            requireArguments().get(ITEM_LIST) as ArrayList<DashboardItemModel>
        Log.e("CategoryList", "${list.size}")

        binding.recyclerViewItemsList.adapter = CategoryItemAdapter(requireContext(),list)
    }
}