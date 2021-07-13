package com.android.pos.ui.fragments.createcategory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.model.CategoryListItemModel
import com.android.pos.databinding.CreateCategoryActivityBinding
import com.android.pos.ui.adapter.CategoryListItemAdapter
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.liveSnackBar
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateCategory : Fragment() {
    lateinit var binding: CreateCategoryActivityBinding
    private val viewModel by viewModels<CreateCategoryViewModel>()
    private var listCategory: ArrayList<CategoryListItemModel> = arrayListOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.create_category_activity, container, false)
        binding.lifecycleOwner = this
        binding.createCategoryViewModel = viewModel

        setupSnackbar()
        observeShowProgress()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setAdapter()
        onClick()


    }

    private fun onClick() {

        binding.txtSave.setOnClickListener {
            findNavController().navigate(R.id.action_createCategory_to_createIModifierSet)
        }
        binding.imgBack.setOnClickListener {
            findNavController().navigateUp()
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

    private fun setupSnackbar() {


        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)


    }

    private fun setAdapter() {
        listCategory.add(CategoryListItemModel(0, "Chicken", "Food", ""))
        listCategory.add(CategoryListItemModel(0, "Chicken Biryani", "Biryani", ""))

        binding.recyclerViewItemsList.adapter =
            CategoryListItemAdapter(requireContext(), listCategory)


    }
}