package com.android.pos.ui.fragments.createitem

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.entities.*
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.DIALOG_IMAGE_PATH
import com.android.pos.data.remote.Constants.DIALOG_KEY
import com.android.pos.data.remote.Constants.DIALOG_KEY_ADD_VARIATION_DETAILS
import com.android.pos.data.remote.Constants.DIALOG_KEY_OPTIONS
import com.android.pos.data.remote.Constants.DIALOG_KEY_VARIATION_DETAILS
import com.android.pos.data.remote.Constants.DIALOG_KEY_VARIATION_DETAILS_REMOVE
import com.android.pos.databinding.CreateItemBinding
import com.android.pos.ui.adapter.ModifierSetsListAdapter
import com.android.pos.ui.adapter.VariationListAdapter
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.AmountTextWatcher
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.callback.UpdateVariationCallback
import com.android.pos.utils.extensions.getNavigationResultLiveData
import com.android.pos.utils.extensions.liveSnackBar
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.io.ByteArrayOutputStream

@AndroidEntryPoint
class CreateItem : Fragment(), View.OnClickListener, UpdateVariationCallback {

    private var imagePath: String? = ""
    private var variationList1: ArrayList<VariationsAttribute>? = null
    private var variationListApi = ArrayList<VariationsAttribute>()
    private var selectedId: Int = -2
    private var isEdit: Boolean = false
    private lateinit var itemObject: TbItem
    private lateinit var binding: CreateItemBinding
    private val viewModel by viewModels<CreateItemViewModel>()
    private lateinit var variationListAdapter: VariationListAdapter
    private var optionSetList: ArrayList<OptionSet>? = null
    private var modifierSetIds = ArrayList<Int>()
    private var position1: Int = -1
    var customVariationList = ArrayList<VariationsAttribute>()
    private val TAG = "CreateItem"
    private var isNewVariation: Boolean = false
    private var base64: String = ""

    // private lateinit var passedVariationList: ArrayList<List<VariationsAttribute>>

    private lateinit var adapter: ModifierSetsListAdapter
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.create_item, container, false)
        binding.lifecycleOwner = this
        binding.createItemVewModel = viewModel

        isEdit = arguments?.getBoolean("isEdit")!!
        setAdapter()
        setupData()
        setupSnackbar()
        observeShowProgress()
        getModifiers()
        navigate()
        navigateToEditVariation()
        callBackFromImage()

        binding.tvAddOptions.setOnClickListener {

            val bundle = Bundle()
            if (isEdit) {
                bundle.putBoolean("isEdit", true)
                bundle.putParcelableArrayList(
                    "optionSets",
                    itemObject.optionSets?.toCollection(arrayListOf())
                )
            } else {
                bundle.putParcelableArrayList("optionSets", optionSetList)
            }
            findNavController().navigate(R.id.action_createItem_to_itemOptionsListDialog, bundle)
        }

        binding.tvAddVariation.setOnClickListener {
            findNavController().navigate(R.id.action_createItem_to_customVariationDialog)
        }


        val resultDialogOptionIds = getNavigationResultLiveData<ArrayList<OptionSet>>(DIALOG_KEY)
        resultDialogOptionIds?.observe(viewLifecycleOwner) {
            optionSetList = it
        }


        val resultDialogVariations =
            getNavigationResultLiveData<List<List<Option>>>(DIALOG_KEY_OPTIONS)
        resultDialogVariations?.observe(viewLifecycleOwner) { variationList ->

            binding.llVariationTitle.visibility = View.VISIBLE
            binding.llMainItemDetails.visibility = View.GONE

            val builder = StringBuilder()
            variationList1 = ArrayList()
            customVariationList = ArrayList()



            variationList.forEach {
                val variation = VariationsAttribute()
                val optionIds = ArrayList<Int>()
                it.forEach {
                    val optionSetIds = ArrayList<Int>()
                    builder.append(it.name.trim() + ",").toString()
                    variation.name = builder.substring(0, builder.length - 1).toString()
                    variation.priceType = "Variable"
                    variation._destroy = false

                    optionSetList?.forEach {
                        optionSetIds.add(it.id!!)
                    }
                    variation.optionSetIds = optionSetIds

                    optionIds.add(it.id!!)
                    variation.optionIds = optionIds

                }
                builder.setLength(0)
                builder.trimToSize()
                variationList1?.add(variation)

            }

            Log.d("variationList1", "::" + variationList1)

            variationListAdapter.addAllVariations(variationList1!!)
        }


        val resultVariationDetails =
            getNavigationResultLiveData<VariationsAttribute>(DIALOG_KEY_VARIATION_DETAILS)
        resultVariationDetails?.observe(viewLifecycleOwner) {
            variationListAdapter.updateVariation(position1, it)

        }


        val resultVariationDetailsRemove =
            getNavigationResultLiveData<VariationsAttribute>(DIALOG_KEY_VARIATION_DETAILS_REMOVE)
        resultVariationDetailsRemove?.observe(viewLifecycleOwner) {
            val deleteVariation = variationListAdapter.deleteVariation(position1, it)
            customVariationList.remove(it)

            if (deleteVariation == 0) {
                binding.llVariationTitle.visibility = View.GONE
                binding.llMainItemDetails.visibility = View.VISIBLE
            }

        }


        val resultVariationAddDetails =
            getNavigationResultLiveData<VariationsAttribute>(DIALOG_KEY_ADD_VARIATION_DETAILS)
        resultVariationAddDetails?.observe(viewLifecycleOwner) {
            variationList1 = ArrayList()
            customVariationList.add(it)

            binding.llVariationTitle.visibility = View.VISIBLE
            binding.llMainItemDetails.visibility = View.GONE

            val isPresent = customVariationList.any { it.name == "Regular" }
            var customVariation: VariationsAttribute? = null

            if (!isPresent) {
                if (!TextUtils.isEmpty(binding.etItemPrice.text.toString())) {

                    customVariation = VariationsAttribute().apply {
                        isActive = true
                        isCustom = true
                        price = binding.etItemPrice.text.toString().replace("$", "").toDouble()
                        name = "Regular"
                        priceType = "Fixed"
                        sku = binding.etSku.text.toString()
                        stockQty = binding.etStock.text.toString()
                    }
                } else {
                    customVariation = VariationsAttribute().apply {
                        isActive = true
                        isCustom = true
                        price = null
                        priceType = "Variable"
                        name = "Regular"
                    }
                }


            }
            if (customVariation != null) {
                customVariationList.add(0, customVariation)
            }
            variationListAdapter.addAllVariations(customVariationList)

        }



        Log.d("variationListdelete", "::" + variationList1?.size)

        binding.txtSave.setOnClickListener {
            saveItem()
        }

        return binding.root
    }

    private fun saveItem() {

        adapter.selectedItemList().forEach {
            modifierSetIds.add(it.id!!)
        }
        viewModel.selectedModifierList(modifierSetIds)

        // variationListApi = ArrayList()


        if (isEdit) {
            /*variationListApi.forEach { variationOriginal ->
                variationListAdapter.selectedVariation().forEach { variationDeleted ->
                    if (variationOriginal.name == variationDeleted.name) {
                        variationOriginal._destroy = false
                    } else {
                        variationOriginal._destroy = true
                    }
                }
            }*/
            var mList: ArrayList<VariationsAttribute> = arrayListOf()

            for (i in 0 until variationListApi.size) {
                val temp = variationListAdapter.selectedVariation().any {
                    it.name == variationListApi.get(i).name
                }

                if (temp) {
                    val obj = variationListApi.get(i)
                    obj._destroy = false
                    mList.add(obj)


                } else {
                    val obj = variationListApi.get(i)
                    obj._destroy = true
                    mList.add(obj)

                }

            }

            for (newVariation in mList) {
                if (!newVariation._destroy) {
                    isNewVariation = true
                    viewModel.variationAttribute(mList)
                    break
                }
            }


            if (!isNewVariation) {
                mList.addAll(variationListAdapter.selectedVariation())
                viewModel.variationAttribute(mList)
            }


            /*Log.e(
                TAG,
                "FinalvariationListApi:  ${Gson().toJson(mList)}"
            )*/

            // viewModel.variationAttribute(mList)
        } else {
            viewModel.variationAttribute(variationListAdapter.selectedVariation())
        }

        // viewModel.variationAttribute(variationListAdapter.selectedVariation())

        /*var body2: MultipartBody.Part? = null
        //  if (!TextUtils.isEmpty(imagePath)) {

        //   if (!(imagePath?.startsWith("https")!! || imagePath!!.startsWith("http"))) {
        val file1 = File("/storage/emulated/0/DCIM/Camera/IMG_20211006_120155.jpg")
        val requestFile1: RequestBody =
            file1.asRequestBody("multipart/form-data".toMediaTypeOrNull())
        body2 = MultipartBody.Part.createFormData("image", file1.name, requestFile1)
*/            //  }

        //  }


       /* val bitmap =
            BitmapFactory.decodeFile(imagePath)
        if (bitmap != null) {
            base64 = convertBase64(bitmap)
        }*/


        if (variationListAdapter.selectedVariation().size > 0 && variationListAdapter.selectedVariation() != null) {
            viewModel.itemDetails(
                imagePath,
                null,
                binding.etDesc.text.toString(),
                "",
                0
            )
        } else {
            val itemPrice: Double?
            val stock: Int
            if (TextUtils.isEmpty(binding.etItemPrice.text.toString())) {
                itemPrice = null
            } else {
                itemPrice = binding.etItemPrice.text.toString().replace("$", "").toDouble()
            }

            if (TextUtils.isEmpty(binding.etStock.text.toString())) {
                stock = 0
            } else {
                stock = binding.etStock.text.toString().toInt()
            }
            viewModel.itemDetails(
                imagePath,
                itemPrice,
                binding.etDesc.text.toString(),
                binding.etSku.text.toString(),
                stock
            )
        }



        viewModel.submit()
    }

    private fun convertBase64(bitmap: Bitmap): String {


        // a potentially time consuming task
        var byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 75, byteArrayOutputStream)
        var byteArray: ByteArray = byteArrayOutputStream.toByteArray()

        try {
            System.gc()
            base64 = Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
        } catch (e: OutOfMemoryError) {
            byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 50, byteArrayOutputStream)
            byteArray = byteArrayOutputStream.toByteArray()
            base64 = Base64.encodeToString(byteArray, Base64.DEFAULT)
            Log.e("Out of memory", "Out of memory error catched");


        }
        return base64

    }

    private fun getModifiers() {

        viewModel.modifierSet.observe(requireActivity(), {

            it.data?.let { it1 ->
                adapter.add(it1)
                if (isEdit) {
                    adapter.selectedItemFromEdit(itemObject.modifier_set_ids as ArrayList<Int>)
                }

            }
        })
    }

    private fun setAdapter() {
        adapter = ModifierSetsListAdapter(true)
        binding.rvModifiersList.adapter = adapter

        variationListAdapter = VariationListAdapter(viewModel)
        variationListAdapter.setCallback(this)
        binding.rvVariationList.adapter = variationListAdapter
    }

    private fun setupData() {


        if (isEdit) {

            binding.txtSave.text = getString(R.string.update)
            binding.txtTitle.text = getString(R.string.update_item)
            itemObject = arguments?.getParcelable("itemObject")!!
            viewModel.setData(itemObject)
            selectedId = itemObject.categoryId
            viewModel.setCategoryId(selectedId)

            if (itemObject.variationsAttributes != null && itemObject.variationsAttributes?.size!! > 0) {
                binding.llVariationTitle.visibility = View.VISIBLE
                binding.llMainItemDetails.visibility = View.GONE
                variationListApi = itemObject.variationsAttributes as ArrayList<VariationsAttribute>
                variationListAdapter.addAllVariations(itemObject.variationsAttributes as ArrayList<VariationsAttribute>)
            } else {
                binding.llVariationTitle.visibility = View.GONE
                binding.llMainItemDetails.visibility = View.VISIBLE
            }

            binding.txtCategoryName.text = itemObject.categoryName


            binding.etItemPrice.setText(
                activity?.getString(R.string.symbole) + " " + String.format(
                    activity?.getString(R.string.format)!!,
                    itemObject?.price
                )
            )

            binding.etDesc.setText(itemObject.shortDescription)
            binding.etSku.setText(itemObject.sku)
            binding.etStock.setText("" + itemObject.quantity)

        }

        binding.etItemPrice.addTextChangedListener(
            AmountTextWatcher(
                binding.etItemPrice,
                false
            )
        )


        binding.chooseCategory.setOnClickListener(this)
        binding.imgEdit.setOnClickListener(this)

        setFragmentResultListener("request_key") { requestKey: String, bundle: Bundle ->
            val result = bundle.getParcelable<TbCategory>("data")
            selectedId = result!!.id
            viewModel.setCategoryId(selectedId)
            if (result != null) {
                if (result.name == "None") {
                    binding.txtCategoryName.text = ""
                } else
                    binding.txtCategoryName.text = result.name
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.imgBack.setOnClickListener {
            val navControll = findNavController()
            navControll.previousBackStackEntry?.savedStateHandle?.set(
                Constants.KEY,
                Constants.CREATEITEM
            )
            navControll.popBackStack()
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

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.chooseCategory -> {
                val bundle = Bundle().apply {
                    putInt("selectedId", selectedId)
                }
                findNavController().navigate(R.id.action_createItem_to_categoriesDialog, bundle)
            }

            R.id.imgEdit -> {
                findNavController().navigate(R.id.action_createItem_to_itemEditTitleDialog)
            }
        }
    }

    private fun navigate() {

        viewModel.data.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let { createTaxResponse ->
                activity?.let {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        it, createTaxResponse.message
                    ) { _, _ ->
                        findNavController().navigateUp()
                    }
                }

            }
        })
    }

    private fun navigateToEditVariation() {

        viewModel.variationListLiveData.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {

            }
        })

    }

    private fun callBackFromImage() {
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<String>(
            DIALOG_IMAGE_PATH
        )?.observe(viewLifecycleOwner) { result ->
            // Do something with the result.
            Log.e("!_@_ image path", result)
            imagePath = result
            viewProfile(imagePath)
        }
    }

    override fun onItemClickListener(position: Int, variation: VariationsAttribute) {
        position1 = position
        val bundle = Bundle().apply {
            putParcelable("variationAttributeList", variation)
        }
        findNavController().navigate(
            R.id.action_createItem_to_editVariationDialog,
            bundle
        )
    }

    private fun viewProfile(profileImage: String?) {

        Log.d("!_@_ profileImage", "::$profileImage")

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
            .into(binding.imgItem)
    }
}