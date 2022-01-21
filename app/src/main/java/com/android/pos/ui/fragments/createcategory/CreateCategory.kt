package com.android.pos.ui.fragments.createcategory

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
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
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.CREATECATEGORY
import com.android.pos.data.remote.Constants.KEY
import com.android.pos.databinding.CreateCategoryActivityBinding
import com.android.pos.ui.adapter.CategoryListItemAdapter
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.liveSnackBar
import com.android.pos.utils.statusUtils.Status
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateCategory : Fragment() {


    private var imagePath: String? = ""
    lateinit var binding: CreateCategoryActivityBinding
    private val viewModel by viewModels<CreateCategoryViewModel>()
    private var listCategory: ArrayList<CategoryListItemModel> = arrayListOf()
    var isEdit: Boolean = false
    private var categoryData: TbCategory? = null
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
            binding.txtTitle.text = getString(R.string.update_category)
            viewModel.categoryData(categoryData!!)
            viewModel.isEditData(isEdit, categoryData!!.id)

            //load image from edit
            viewProfile(categoryData!!.thumbImgUrl)
        }

        setupSnackbar()
        observeShowProgress()
        getInventoryListObserver()
        navigationObserver()
        callBackFromImage()

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setAdapter()
        onClick()

    }

    private fun onClick() {

        binding.txtSave.setOnClickListener {
            var newImagePathToUpload = imagePath
            if (isEdit && imagePath.equals(categoryData?.thumbImgUrl, true)) {
                //send image if its altered.
                newImagePathToUpload = ""
            }
            if (categoryData != null)
                categoryData?.name?.let { it1 ->
                    viewModel.submit(adapter.getIds(), newImagePathToUpload,
                        it1
                    )
                }
            else
                viewModel.submit(adapter.getIds(), newImagePathToUpload, "")

        }
        binding.imgBack.setOnClickListener {
            onSubmitBack()
        }
        binding.ilImage.relImage.setOnClickListener {
            openDialog()
        }
    }

    private fun openDialog() {
        var profileImg = ""
        if (categoryData != null && !categoryData!!.thumbImgUrl.isNullOrEmpty()) {
            profileImg = categoryData!!.thumbImgUrl ?: ""

        }
        val bundle = Bundle()
        bundle.putString("imgUrl", profileImg)
        findNavController().navigate(R.id.action_createCategory_to_itemEditTitleDialog, bundle)
    }

    private fun callBackFromImage() {
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<String>(
            Constants.DIALOG_IMAGE_PATH
        )?.observe(viewLifecycleOwner) { result ->
            // Do something with the result.
            Log.e("!_@_ image path", result)
            viewProfile(result)
        }
    }

    private fun onSubmitBack() {
        val navControll = findNavController()
        navControll.previousBackStackEntry?.savedStateHandle?.set(KEY, CREATECATEGORY)
        navControll.popBackStack()
    }

    private fun navigationObserver() {

        viewModel.data.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let { createOptionResponse ->

                activity?.let {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        it, createOptionResponse
                    ) { _, _ ->
                        val navControll = findNavController()
                        navControll.previousBackStackEntry?.savedStateHandle?.set(
                            KEY,
                            CREATECATEGORY
                        )
                        navControll.popBackStack()
                    }
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
                            categoryData?.item_ids?.let { it1 -> adapter.selectedItemFromEdit(it1) }
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

    private fun viewProfile(profileImage: String?) {

        Log.d("!_@_ profileImage", "::$profileImage")
        imagePath = profileImage

        Glide.with(requireActivity()).load(profileImage)
            //.apply(RequestOptions().override(100, 100))
            .placeholder(R.drawable.ic_item_placeholder)

            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

            }).dontTransform().dontAnimate().diskCacheStrategy(DiskCacheStrategy.ALL)
            .encodeFormat(Bitmap.CompressFormat.PNG).skipMemoryCache(true)
            .format(DecodeFormat.DEFAULT).priority(Priority.IMMEDIATE).centerCrop()
            .into(binding.ilImage.ivImage)
    }
}