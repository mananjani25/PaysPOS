package com.android.pos.ui.fragments.inventory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.android.pos.R
import com.android.pos.data.model.CategoryListModel
import com.android.pos.databinding.FragmentCategoriesBinding
import com.android.pos.ui.adapter.CategoriesListAdapter
import com.android.pos.ui.adapter.CategoryItemAdapter

class Categories : Fragment() {
    private lateinit var binding: FragmentCategoriesBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_categories, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setAdapter()
    }

    private fun setAdapter() {
        var list: ArrayList<CategoryListModel> = arrayListOf()
        list.add(CategoryListModel(0, "Category One"))
        list.add(CategoryListModel(0, "Category Two"))
        binding.rvCategoriesList.adapter = CategoriesListAdapter(requireContext(), list)

    }
}