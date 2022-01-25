package com.android.pos.ui.dialog

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.android.pos.R
import com.android.pos.data.model.OptionListModel
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.DIALOG_IMAGE_PATH
import com.android.pos.databinding.DialogEditItemTitleBinding
import com.android.pos.ui.activities.MainActivity
import com.android.pos.ui.adapter.ChooseColorsAdapter
import com.android.pos.utils.FileUtils.handleImageOnKitkat
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class ItemEditTitleDialog : DialogFragment() {

    private var imgUrl: String? = ""
    private lateinit var adapter: ChooseColorsAdapter
    private lateinit var binding: DialogEditItemTitleBinding
    private var mUri: Uri? = null
    private var imagePath: String? = null
    private val OPERATION_CAPTURE_PHOTO = 1
    private val OPERATION_CHOOSE_PHOTO = 2
    private val PERMISSION = 3
    private var selectOption: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.dialog_edit_item_title, container, false)
        binding.lifecycleOwner = this

        imgUrl = arguments?.getString("imgUrl") ?: ""
        if(imgUrl?.isNotEmpty() == true){
            imagePath = imgUrl
            showImage()
        }

        setAdapter()
        initListeners()

        return binding.root
    }

    private fun initListeners() {
        binding.tvTakePhoto.setOnClickListener {
            selectOption = "1"
            requestPermissionDialog(selectOption)
        }

        binding.tvChoosePhoto.setOnClickListener {
            selectOption = "2"
            requestPermissionDialog(selectOption)
        }
        binding.includeLayout.llTapToEdit.setOnClickListener {
            selectOption = "2"
            requestPermissionDialog(selectOption)
        }

        binding.txtSave.setOnClickListener {
            findNavController().previousBackStackEntry?.savedStateHandle?.set(
                DIALOG_IMAGE_PATH,
                imagePath
            )
            dismiss()
        }

        binding.imgBack.setOnClickListener {
            dismiss()
        }

        (activity as MainActivity).activityResultCallBack = object :
            MainActivity.ActivityResultCallBack {
            override fun onReceivedCameraCapturedPath(mediaType: Int, mediaPath: String?) {
                if (mediaType == Constants.MEDIA_TYPE_IMAGE) {
                    imagePath = mediaPath
                    showImage()
                }
            }
        }
    }

    private fun setAdapter() {
        val list: ArrayList<OptionListModel> = arrayListOf()
        list.add(OptionListModel(R.color.colorF2, "Drink Size", "3 Options"))
        list.add(OptionListModel(R.color.colorFA, "Drink Size", "3 Options"))
        list.add(OptionListModel(R.color.color72, "Drink Size", "3 Options"))
        list.add(OptionListModel(R.color.colorFF, "Drink Size", "3 Options"))
        list.add(OptionListModel(R.color.color23, "Drink Size", "3 Options"))
        list.add(OptionListModel(R.color.colorAA, "Drink Size", "3 Options"))
        list.add(OptionListModel(R.color.colorF7, "Drink Size", "3 Options"))
        list.add(OptionListModel(R.color.colorFD, "Drink Size", "3 Options"))
        list.add(OptionListModel(R.color.colorD8, "Drink Size", "3 Options"))
        list.add(OptionListModel(R.color.colorF9, "Drink Size", "3 Options"))
        binding.rvColors.layoutManager = GridLayoutManager(requireContext(), 5)
        adapter = ChooseColorsAdapter(list)
        binding.rvColors.adapter = adapter
    }

    private fun requestPermissionDialog(selectOption: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(
                    requireActivity(),
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_DENIED || checkSelfPermission(
                    requireActivity(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_DENIED
            ) {
                //permission was not enabled
                val permission =
                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                //show popup to request permission
                requestPermissions(permission, PERMISSION)
            } else if (selectOption.equals("1")) {
                //permission already granted
                capturePhoto()
            } else {
                openGallery()
            }
        } else if (selectOption.equals("1")) {
            //system os is < marshmallow
            capturePhoto()
        } else {
            openGallery()
        }
    }

    private fun capturePhoto() {
        (activity as MainActivity).capturePhoto()
    }

    private fun openGallery() {
        val intent = Intent("android.intent.action.GET_CONTENT")
        intent.type = "image/*"
        startActivityForResult(intent, OPERATION_CHOOSE_PHOTO)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantedResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantedResults)
        when (requestCode) {
            PERMISSION -> if ((grantedResults.isNotEmpty() && grantedResults[0] == PackageManager.PERMISSION_GRANTED) && grantedResults[1] == PackageManager.PERMISSION_GRANTED) {
                if (selectOption == "1") {
                    capturePhoto()
                } else {
                    openGallery()
                }


            } else {
                show("Unfortunately You are Denied Permission to Perform this Operations.")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        when (requestCode) {
            OPERATION_CAPTURE_PHOTO -> if (resultCode == RESULT_OK) {

                imagePath = mUri.toString()
                if (imagePath != null) {
                    viewProfile(imagePath)
                } else {
                    show("ImagePath is null")
                }

            }
            OPERATION_CHOOSE_PHOTO -> if (resultCode == RESULT_OK) {
                imagePath = handleImageOnKitkat(data, requireActivity())
                showImage()
            }
        }
    }

    private fun showImage() {
        if (imagePath != null) {
            viewProfile(imagePath)
        } else {
            show("ImagePath is null")
        }
    }

    private fun show(message: String) {
        Toast.makeText(requireActivity(), message, Toast.LENGTH_SHORT).show()
    }

    private fun viewProfile(profileImage: String?) {

        Log.d("profileImage", "::$profileImage")
        binding.includeLayout.progressBar.visibility = View.VISIBLE

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
                    binding.includeLayout.progressBar.visibility = View.GONE
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    binding.includeLayout.progressBar.visibility = View.GONE
                    return false
                }

            }).dontTransform().dontAnimate().diskCacheStrategy(DiskCacheStrategy.ALL)
            .encodeFormat(Bitmap.CompressFormat.PNG).skipMemoryCache(true)
            .format(DecodeFormat.DEFAULT).priority(Priority.IMMEDIATE).centerCrop()
            .into(binding.includeLayout.ivImage)
    }


    override fun onResume() {
        super.onResume()

        val window: Window? = dialog!!.window
        val size = Point()
        val display: Display = window?.windowManager?.defaultDisplay!!
        display.getSize(size)
        val width: Int = size.x
        window.setLayout((width * 0.50).toInt(), WindowManager.LayoutParams.MATCH_PARENT)
        window.setGravity(Gravity.CENTER)
    }


}