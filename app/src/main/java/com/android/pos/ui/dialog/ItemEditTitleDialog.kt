package com.android.pos.ui.dialog

import android.Manifest
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import com.android.pos.R
import com.android.pos.data.model.OptionListModel
import com.android.pos.databinding.DialogEditItemTitleBinding
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
import java.io.File
import android.view.View
import androidx.core.content.ContextCompat.getExternalCacheDirs
import androidx.navigation.fragment.findNavController
import com.android.pos.data.remote.Constants.DIALOG_IMAGE_PATH
import java.util.*
import kotlin.collections.ArrayList


@AndroidEntryPoint
class ItemEditTitleDialog : DialogFragment(), View.OnClickListener {
    private lateinit var adapter: ChooseColorsAdapter
    private lateinit var binding: DialogEditItemTitleBinding
    private var mUri: Uri? = null
    private var imagePath: String? = null
    private val OPERATION_CAPTURE_PHOTO = 1
    private val OPERATION_CHOOSE_PHOTO = 2
    private val PERMISSION = 3
    private var selectOption: String = ""

    val MULTIPART_FORM_DATA = "multipart/form-data"


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.dialog_edit_item_title, container, false)
        binding.lifecycleOwner = this

        setAdapter()

        binding.tvTakePhoto.setOnClickListener {
            selectOption = "1"
            requestPermissionDialog(selectOption)
        }

        binding.tvChoosePhoto.setOnClickListener {
            selectOption = "2"
            requestPermissionDialog(selectOption)
        }

        binding.txtSave.setOnClickListener {
            findNavController().previousBackStackEntry?.savedStateHandle?.set(DIALOG_IMAGE_PATH, imagePath)
            dismiss()
        }

        return binding.root
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
        binding.imgBack.setOnClickListener(this)
        binding.txtSave.setOnClickListener(this)


    }

    override fun onClick(v: View?) {


        when (v?.id) {
            R.id.imgBack -> {
                dismiss()
            }
            R.id.txtSave -> {

            }

        }
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
       /* val capturedImage = File(externalCacheDir, "androidPos.jpg")
          if (capturedImage.exists()) {
              capturedImage.delete()
          }
          capturedImage.createNewFile()

          mUri = if (Build.VERSION.SDK_INT >= 24) {
              FileProvider.getUriForFile(
                  requireActivity(),
                  "com.android.pos.fileprovider",
                  capturedImage
              )
          } else {
              Uri.fromFile(capturedImage)
          }

          val intent = Intent("android.media.action.IMAGE_CAPTURE")
          intent.putExtra(MediaStore.EXTRA_OUTPUT, mUri)
          startActivityForResult(intent, OPERATION_CAPTURE_PHOTO)*/
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
                if (Build.VERSION.SDK_INT >= 19) {

                    imagePath = handleImageOnKitkat(data, requireActivity())

                    if (imagePath != null) {
                        viewProfile(imagePath)
                    } else {
                        show("ImagePath is null")
                    }
                }
            }
        }
    }

    private fun show(message: String) {
        Toast.makeText(requireActivity(), message, Toast.LENGTH_SHORT).show()
    }

    private fun viewProfile(profileImage: String?) {

        Log.d("profileImage", "::" + profileImage)
        binding.includeLayout.progressBar.visibility = View.VISIBLE

        Glide.with(requireActivity()).load(profileImage)
            .apply(RequestOptions().override(100, 100))
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