package com.pays.pos.ui.fragments.createcategory

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
import com.pays.pos.R
import com.pays.pos.data.entities.TbCategory
import com.pays.pos.data.model.CategoryListItemModel
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.CREATECATEGORY
import com.pays.pos.data.remote.Constants.KEY
import com.pays.pos.databinding.CreateCategoryActivityBinding
import com.pays.pos.ui.adapter.CategoryListItemAdapter
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.MethodUtils
import com.pays.pos.utils.ProgressUtils
import com.pays.pos.utils.extensions.liveSnackBar
import com.pays.pos.utils.statusUtils.Status
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.pays.pos.data.remote.Constants.IMAGE_DIALOG_TITTLE
import com.pays.pos.logger.MessageEvent
import com.pays.pos.utils.extensions.setOnSingleClickListener
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus

@AndroidEntryPoint
class CreateCategory : Fragment() {


    private var imagePath: String? = ""
    lateinit var binding: CreateCategoryActivityBinding
    private val viewModel by viewModels<CreateCategoryViewModel>()
    private var listCategory: ArrayList<CategoryListItemModel> = arrayListOf()
    var isEdit: Boolean = false
    private var categoryData: TbCategory? = null
    private var defaultCategoryData: TbCategory? = null
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
            defaultCategoryData = arguments?.getParcelable("DefaultCategoryObject")
            binding.header.txtSave.text = getString(R.string.update)
            binding.header.txtTitle.text = getString(R.string.update_category)

            viewModel.categoryData(categoryData!!)
            viewModel.isEditData(isEdit, categoryData!!.id)

            // Disabled Default Category name edit
            binding.etCategoryName.isEnabled = categoryData!!.name != "Default Category"
            //load image from edit
            viewProfile(categoryData!!.thumbImgUrl)
        } else {
            binding.header.txtTitle.text = getString(R.string.create_category)
            binding.header.txtSave.text = getString(R.string.save)
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

        binding.header.txtSave.setOnSingleClickListener{
            var newImagePathToUpload = imagePath
            if (isEdit && imagePath.equals(categoryData?.thumbImgUrl, true)) {
                //send image if its altered.
                newImagePathToUpload = ""

                EventBus.getDefault().post(MessageEvent("${Constants.LINE_BREAK_TAB} CreateCategory_onClick_binding.header.txtSave, isEdit=${Gson().toJson(isEdit)}, imagePath.equals(categoryData?.thumbImgUrl, true)= ${imagePath.equals(categoryData?.thumbImgUrl, true)}"))
                viewModel.submit(
                    adapter.getIds(), newImagePathToUpload,
                    adapter.getTbItemsList(),
                    defaultCategoryData
                )
            } else {
                EventBus.getDefault().post(MessageEvent("${Constants.LINE_BREAK_TAB} CreateCategory_onClick_binding.header.txtSave, else"))
                viewModel.submit(adapter.getIds(), newImagePathToUpload,  adapter.getTbItemsList(),
                defaultCategoryData)
            }
        }
        binding.header.imgBack.setOnClickListener {
            onSubmitBack()
        }
        binding.ilImage.relImage.setOnClickListener {
            if (MethodUtils.isDoubleClick()) return@setOnClickListener
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
        bundle.putString(IMAGE_DIALOG_TITTLE, getString(R.string.update_category_image))
        findNavController().navigate(R.id.action_createCategory_to_itemEditTitleDialog, bundle)
    }

    private fun callBackFromImage() {
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<String>(
            Constants.DIALOG_IMAGE_PATH
        )?.observe(viewLifecycleOwner) { result ->
            // Do something with the result.
            LogUtil.logE("!_@_ image path", result)
            viewProfile(result)
        }
    }

    private fun onSubmitBack() {
        val navControll = findNavController()
        navControll.previousBackStackEntry?.savedStateHandle?.set(KEY, CREATECATEGORY)
        navControll.popBackStack()
    }

    private fun navigationObserver() {

        viewModel.data.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { createOptionResponse ->

                activity?.let {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        it, createOptionResponse
                    ) { _, _ ->
                        if (isAdded && view != null) {
                            val navControll = findNavController()
                            if (navControll.currentDestination?.id == R.id.createCategory) {
                                navControll.previousBackStackEntry?.savedStateHandle?.set(
                                    KEY,
                                    CREATECATEGORY
                                )
                                navControll.popBackStack()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun observeShowProgress() {

        viewModel.showProgress.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        }

    }

    private fun setupSnackbar() {


        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)


    }

    private fun getInventoryListObserver() {

        viewModel.items.observe(viewLifecycleOwner) {

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


        }

    }

    private fun setAdapter() {
        binding.recyclerViewItemsList.adapter = adapter
        if(categoryData != null){
            adapter.categoryName(categoryData?.name!!)
        }
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