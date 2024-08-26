package com.pays.pos.ui.dialog

import android.Manifest
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.DialogInterface
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
import com.pays.pos.R
import com.pays.pos.data.model.OptionListModel
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.DIALOG_IMAGE_PATH
import com.pays.pos.databinding.DialogEditItemTitleBinding
import com.pays.pos.ui.activities.MainActivity
import com.pays.pos.ui.adapter.ChooseColorsAdapter
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.FileUtils.handleImageOnKitkat
import com.pays.pos.utils.MethodUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.pays.pos.utils.extensions.setOnSingleClickListener
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import kotlin.math.log


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
    var MEGABYTE = 1024 * 1024
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.dialog_edit_item_title, container, false)
        binding.lifecycleOwner = this

        imgUrl = arguments?.getString("imgUrl") ?: ""
        if (imgUrl?.isNotEmpty() == true) {
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

        binding.tvChoosePhoto.setOnSingleClickListener {
       /*     if (MethodUtils.isDoubleClick())
                return@setOnClickListener*/
            selectOption = "2"
            requestPermissionDialog(selectOption)
        }
        binding.includeLayout.llTapToEdit.setOnClickListener {
            if (MethodUtils.isDoubleClick()) return@setOnClickListener
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

                try {
                    imagePath = mUri.toString()
                    var file: File = File(imagePath)
                    var length = file.length()
                    var mb_string = ""
                    var size_kb = length / 1024f
                    var size_mb = String.format("%.2f", size_kb / 1024f).toDouble()
                    if (size_mb > 0) {
                        mb_string = "$size_mb MB"
                    } else {
                        mb_string = "$size_kb KB"
                    }
                    Log.d("yash", "onActivityResult: $mb_string")
                    Log.d("yash", "onActivityResult: $length")
                    if (length > 5242880) {
                        AlertUtils.showCustomAlertWithListenerWithOK(
                            requireContext(),
                            "The file is $mb_string exceeding the maximum file size of 5 MB."
                        ) { _, _ ->
                            dismiss()
                        }
                    } else {
                        if (imagePath != null) {
                            viewProfile(imagePath)
                        } else {
                            show("ImagePath is null")
                        }
                    }
                } catch (e: Exception) {
                    Log.d("yash", "onActivityResult: " + e.message)
                }


            }
            OPERATION_CHOOSE_PHOTO -> if (resultCode == RESULT_OK) {
                try {
                    imagePath = handleImageOnKitkat(data, requireActivity())
                    var file: File = File(imagePath)
                    var length = file.length()
                    var mb_string = ""
                    var size_kb = length / 1024f
                    var size_mb = String.format("%.2f", size_kb / 1024f).toDouble()
                    if (size_mb > 0) {
                        mb_string = "$size_mb MB"
                    } else {
                        mb_string = "$size_kb KB"
                    }
                    Log.d("yash", "onActivityResult: $mb_string")
                    Log.d("yash", "onActivityResult: $length")
                    if (length > 5242880) {
                        AlertUtils.showCustomAlertWithListenerWithOK(
                            requireContext(),
                            "The file is $mb_string exceeding the maximum file size of 5 MB."
                        ) { _, _ ->
                            dismiss()
                        }
                    } else {
                        showImage()
                    }
                } catch (e: Exception) {
                    Log.d("yash", "onActivityResult: " + e.message)
                }
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