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
import com.android.pos.data.entities.TbCategory
import com.android.pos.data.model.CategoryListItemModel
import com.android.pos.data.remote.Constants.CREATECATEGORY
import com.android.pos.data.remote.Constants.KEY
import com.android.pos.databinding.CreateCategoryActivityBinding
import com.android.pos.ui.adapter.CategoryListItemAdapter
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.liveSnackBar
import com.android.pos.utils.statusUtils.Status
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateCategory : Fragment() {
    lateinit var binding: CreateCategoryActivityBinding
    private val viewModel by viewModels<CreateCategoryViewModel>()
    private var listCategory: ArrayList<CategoryListItemModel> = arrayListOf()
    var isEdit: Boolean = false
    private lateinit var categoryData: TbCategory
    private var adapter = CategoryListItemAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.create_category_activity, container, false)
        binding.lifecycleOwner = this
        binding.createCategoryViewModel = viewModel

        isEdit = arguments?.getBoolean("isEdit")!!

        if (isEdit) {
            categoryData = arguments?.getParcelable("categoryObject")!!
            binding.txtSave.text = getString(R.string.update)
            viewModel.categoryData(categoryData)
            viewModel.isEditData(isEdit, categoryData.id)
        }

        setupSnackbar()
        observeShowProgress()
        getInventoryListObserver()
        navigationObserver()

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setAdapter()
        onClick()

    }

    private fun onClick() {

        binding.txtSave.setOnClickListener {

            viewModel.submit(adapter.getIds())

            //
        }
        binding.imgBack.setOnClickListener {
            onSubmitBack()
        }
    }

    private fun onSubmitBack() {
        val navControll = findNavController()
        navControll.previousBackStackEntry?.savedStateHandle?.set(KEY, CREATECATEGORY)
        navControll.popBackStack()
    }

    private fun navigationObserver() {

        viewModel.data.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    onSubmitBack()
                }
            }
        })
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

    private fun getInventoryListObserver() {


        viewModel.items.observe(viewLifecycleOwner, {

            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        binding.recyclerViewItemsList.visibility = View.VISIBLE
                        it.data?.let { it1 -> adapter.add(it1) }

                        if (isEdit) {
                            adapter.selectedItemFromEdit(categoryData.item_ids)
                        }
                    }
                    Status.ERROR -> {
                        binding.recyclerViewItemsList.visibility = View.GONE
                    }
                    Status.LOADING -> {
                        binding.recyclerViewItemsList.visibility = View.GONE

                    }
                }
            }


        })

    }

    private fun setAdapter() {
        binding.recyclerViewItemsList.adapter = adapter


    }
}