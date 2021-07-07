package com.android.pos.ui.fragments.loginscreen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.android.pos.R
import com.android.pos.data.model.CategoryListItemModel
import com.android.pos.databinding.CreateCategoryActivityBinding
import com.android.pos.ui.adapter.CategoryListItemAdapter

class CreateCategory : Fragment() {
    lateinit var binding:CreateCategoryActivityBinding
    private var listCategory:ArrayList<CategoryListItemModel> = arrayListOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater,R.layout.create_category_activity,container,false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setAdapter()

    }

    private fun setAdapter() {
        listCategory.add(CategoryListItemModel(0,"Chicken","Food",""))
        listCategory.add(CategoryListItemModel(0,"Chicken Biryani","Biryani",""))

        binding.recyclerViewItemsList.adapter = CategoryListItemAdapter(requireContext(),listCategory)

    }
}