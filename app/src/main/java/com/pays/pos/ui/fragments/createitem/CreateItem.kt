package com.pays.pos.ui.fragments.createitem

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.widget.PopupMenu
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.R
import com.pays.pos.data.entities.*
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.DIALOG_IMAGE_PATH
import com.pays.pos.data.remote.Constants.DIALOG_KEY
import com.pays.pos.data.remote.Constants.DIALOG_KEY_ADD_VARIATION_DETAILS
import com.pays.pos.data.remote.Constants.DIALOG_KEY_OPTIONS
import com.pays.pos.data.remote.Constants.DIALOG_KEY_VARIATION_DETAILS
import com.pays.pos.data.remote.Constants.DIALOG_KEY_VARIATION_DETAILS_REMOVE
import com.pays.pos.databinding.CreateItemBinding
import com.pays.pos.ui.adapter.ModifierSetsListAdapter
import com.pays.pos.ui.adapter.VariationListAdapter
import com.pays.pos.ui.fragments.inventory.CategoriesViewModel
import com.pays.pos.utils.*
import com.pays.pos.utils.MethodUtils.Companion.isDoubleClick
import com.pays.pos.utils.callback.ItemCallback
import com.pays.pos.utils.callback.UpdateVariationCallback
import com.pays.pos.utils.extensions.getNavigationResultLiveData
import com.pays.pos.utils.extensions.liveSnackBar
import com.pays.pos.utils.extensions.runOnUiThread
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
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateItem : Fragment(), View.OnClickListener, UpdateVariationCallback, ItemCallback,
    ModifierSetsListAdapter.ModifierCallback, ModifierSetsListAdapter.ModifierDeleteCallback {

    private var spinnerAdapter: ArrayAdapter<ModifierSet>? = null
    private var productCode: String? = ""
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
    var spinnerList: ArrayList<ModifierSet> = arrayListOf()
    var dragFrom = -1
    var dragTo = -1
    private val categoriesViewModel by viewModels<CategoriesViewModel>()

    var allTaxes: List<TaxData>? = null

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
        productCode = arguments?.getString("productCode")
        setAdapter()
        setupData()
        setupSnackbar()
        getModifiers()
        navigate()
        navigateToEditVariation()
        callBackFromImage()
        initObservers()
        onTextChanged()

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
            if (findNavController().currentDestination?.id == R.id.createItem) {
                findNavController().navigate(R.id.action_createItem_to_customVariationDialog)
            }
        }

        binding.header.txtSave.setOnClickListener {
            MethodUtils.hideKeyboard(requireActivity())
            saveItem()
        }

        if (productCode != null && productCode!!.isNotEmpty()) {

            binding.etSku.setText(productCode)
        }

        return binding.root
    }

    private fun onTextChanged() {
        binding.etStock.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(
                s: CharSequence, start: Int, before: Int,
                count: Int
            ) {
                val enteredString = s.toString()
                if (enteredString.startsWith("0")) {

                    if (enteredString.isNotEmpty()) {
                        binding.etStock.setText(enteredString.substring(1))
                    } else {
                        binding.etStock.setText("")
                    }
                } else if (s.toString().trim().isNotEmpty() && s.toString().toInt() > 10000) {
                    binding.etStock.setText("10000")
                }

            }

            override fun beforeTextChanged(
                s: CharSequence?, start: Int, count: Int,
                after: Int
            ) {
            }

            override fun afterTextChanged(s: Editable?) {}
        })

    }

    private fun initObservers() {

        viewModel.showProgress.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        }

        getNavigationResultLiveData<ArrayList<OptionSet>>(DIALOG_KEY)?.observe(viewLifecycleOwner) {
            optionSetList = it
        }

        getNavigationResultLiveData<List<List<Option>>>(DIALOG_KEY_OPTIONS)?.observe(
            viewLifecycleOwner
        ) { variationList ->

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

            Log.d("variationList1", "::$variationList1")

            variationListAdapter.addAllVariations(variationList1!!)
        }

        getNavigationResultLiveData<VariationsAttribute>(DIALOG_KEY_VARIATION_DETAILS)?.observe(
            viewLifecycleOwner
        ) {
            variationListAdapter.updateVariation(position1, it)
        }

        getNavigationResultLiveData<VariationsAttribute>(DIALOG_KEY_VARIATION_DETAILS_REMOVE)?.observe(
            viewLifecycleOwner
        ) {
            val deleteVariation = variationListAdapter.deleteVariation(position1, it)
            customVariationList.remove(it)

            if (deleteVariation == 0) {
                binding.llVariationTitle.visibility = View.GONE
                binding.llMainItemDetails.visibility = View.VISIBLE
            }
        }

        Log.d("variationListdelete", "::" + variationList1?.size)

        getNavigationResultLiveData<VariationsAttribute>(DIALOG_KEY_ADD_VARIATION_DETAILS)?.observe(
            viewLifecycleOwner
        ) {
            Log.e(TAG, "variationList1  ${Gson().toJson(variationListAdapter.variationList)}")
            variationList1 = ArrayList()

            if (customVariationList.isEmpty()) {
                customVariationList.addAll(variationListAdapter.variationList)
            }
            customVariationList.add(it)

            binding.llVariationTitle.visibility = View.VISIBLE
            binding.llMainItemDetails.visibility = View.GONE

            val isPresent = customVariationList.any { it.name == "Regular" }
            var customVariation: VariationsAttribute? = null

            if (!isPresent) {
                val itemPriceString = binding.etItemPrice.text.toString()
                if (!TextUtils.isEmpty(itemPriceString)) {

                    customVariation = VariationsAttribute().apply {
                        isActive = true
                        isCustom = true
                        price =
                            itemPriceString.replace("$", "").replace("\\s".toRegex(), "").toDouble()
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
    }

    private fun saveItem() {
        var listSortMod: ArrayList<Int> = arrayListOf()

        adapter.filterList.forEach {
            it.id?.let { it1 -> listSortMod.add(it1) }
            modifierSetIds.add(it.id!!)
        }
        viewModel.selectedModifierList(modifierSetIds)
        viewModel.selectedModifierSortList(listSortMod)


        if (isEdit) {

            val mList: ArrayList<VariationsAttribute> = arrayListOf()

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

            var varApiEmp = false
            if (variationListApi.isEmpty()) {
                varApiEmp = true
                mList.addAll(variationListAdapter.variationList)
            }


            Log.e("VariationList", "varSize ${Gson().toJson(variationListAdapter.variationList)}")


            Log.e("VarEd", "varApiEmp  ${varApiEmp}")
            if (varApiEmp) {
                Log.e(TAG, "insideEmpty")
                viewModel.variationAttribute(variationListAdapter.variationList)
            } else {
                viewModel.variationAttribute(mList)
                /*   for (newVariation in mList) {
                       if (!newVariation._destroy) {
                           isNewVariation = true
                           Log.e(TAG, "getVariationL  ${mList.size}")
                           viewModel.variationAttribute(mList)
                           break
                       }
                   }*/



                Log.e(TAG, "isNewVariation  ${isNewVariation}")
                if (!isNewVariation) {
                    mList.addAll(variationListAdapter.variationList)
                    viewModel.variationAttribute(mList)
                }
            }

        } else {
            viewModel.variationAttribute(variationListAdapter.selectedVariation())
            var taxNames = viewModel.taxNameToDisplay.split(",")
            var selectedTaxIds=ArrayList<String>()
            allTaxes?.let {
                it.forEach {
                    if (it.isActive && taxNames.contains(it.name)){
                        selectedTaxIds.add(it.id.toString())
                    }
                }
            }

            viewModel.selectedTaxList(selectedTaxIds)
        }

        var newImagePathToUpload = imagePath
        if (isEdit && imagePath.equals(itemObject.imageUrl, true)) {
            //send image if its altered.
            newImagePathToUpload = ""
        }

        if (variationListAdapter.selectedVariation().size > 0 && variationListAdapter.selectedVariation() != null) {
            viewModel.itemDetails(
                newImagePathToUpload,
                null,
                binding.etDesc.text.toString(),
                binding.etSku.text.toString(),
                0,
                productCode ?: ""
            )
        } else {
            val itemPrice: Double?
            val itemPriceString = binding.etItemPrice.text.toString()
            itemPrice = if (TextUtils.isEmpty(itemPriceString)) {
                null
            } else {
                itemPriceString.replace("$", "").replace("\\s".toRegex(), "").toDouble()
            }

            val stock = if (TextUtils.isEmpty(binding.etStock.text.toString())) {
                0
            } else {
                binding.etStock.text.toString().toInt()
            }
            viewModel.itemDetails(
                newImagePathToUpload,
                itemPrice,
                binding.etDesc.text.toString(),
                binding.etSku.text.toString(),
                stock,
                productCode ?: ""
            )
        }

        viewModel.submit()
    }

    private fun getModifiers() {

        viewModel.modifierSet.observe(requireActivity()) {
            if (it.data?.isNotEmpty() == true) {
                var checkedList: ArrayList<ModifierSet> = arrayListOf()
                var model = ModifierSet()
                model.name = "Select Modifier Set"
                spinnerList.add(model)
                it.data.forEach {
                    if (isEdit && itemObject.modifier_set_ids.contains(it.id) && it.itemIds.contains(
                            itemObject.itemId
                        )
                    ) {
                        it.isChecked = true

                        checkedList.add(it)
                    } else if (it.isChecked) {
                        checkedList.add(it)
                    } else {
                        spinnerList.add(it)
                    }
                }

                if (isEdit) {
                    adapter.add(
                        MethodUtils.convertSortListForModifierSet(
                            itemObject.modifier_set_ids,
                            checkedList
                        )
                    )
                }




                spinnerAdapter =
                    ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        spinnerList
                    )
                spinnerAdapter?.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

                binding.edtModifiersList?.adapter = spinnerAdapter

                binding.edtModifiersList?.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            p0: AdapterView<*>?,
                            p1: View?,
                            position: Int,
                            p3: Long
                        ) {
                            Log.e("OnSpinItemSelected", "OnItemSelected")
                            if (position != 0) {
                                var mod = spinnerList.get(position)

                                mod.id?.let { it1 ->
                                    if (isEdit) {
                                        val updatedModifiersList: ArrayList<Int> = arrayListOf()
                                        updatedModifiersList.addAll(itemObject.modifier_set_ids)
                                        updatedModifiersList.add(it1)
                                        itemObject.modifier_set_ids = updatedModifiersList
//                                        itemObject.modifier_set_ids.toCollection(arrayListOf())
//                                            .add(
//                                                it1
//                                            )
                                    }

                                    runOnUiThread(Runnable {
                                        spinnerAdapter?.remove(spinnerList.get(position))
                                        spinnerAdapter?.notifyDataSetChanged()
                                        binding.edtModifiersList?.setSelection(0)
                                        adapter.addItem(mod)
                                    })
                                }

                                //  viewModel.updateMod(mod)


                            }

                        }

                        override fun onNothingSelected(p0: AdapterView<*>?) {

                        }

                    }


            }
/*
            it.data?.let { it1 ->
                adapter.add(it1)
                if (isEdit) {
                    adapter.selectedItemFromEdit(itemObject.modifier_set_ids as ArrayList<Int>)
                }

            }*/
        }
    }

    private fun setAdapter() {

        binding.rvModifiersList.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayoutManager.VERTICAL
            )
        )

        adapter = ModifierSetsListAdapter(true)
        binding.rvModifiersList.adapter = adapter
        adapter.setCallback(this)
        adapter.onDelteCallbackMod(this)

        val touchHelper =
            ItemTouchHelper(object :
                ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP + ItemTouchHelper.DOWN, 0) {


                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    val oldPos = viewHolder.bindingAdapterPosition
                    val newPos = target.bindingAdapterPosition
                    LogUtil.logE(TAG, "posGOTPoldPos ${oldPos}")
                    LogUtil.logE(TAG, "posGOTPnewPos ${newPos}")

                    if (dragFrom == -1) {
                        dragFrom = oldPos
                    }
                    dragTo = target.layoutPosition

                    adapter.onItemMove(
                        viewHolder.bindingAdapterPosition,
                        target.bindingAdapterPosition
                    )

                    return true
                }

                override fun isLongPressDragEnabled(): Boolean {
                    return true
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

                }

                override fun clearView(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder
                ) {

                    if (dragFrom != -1 && dragTo != -1 && dragFrom != dragTo) {

                        /*reallyMoved(
                            adapter.getItem(dragFrom).sort,
                            adapter.getItem(dragTo).sort,
                            adapter.getItem(viewHolder.layoutPosition).id
                        )*/
                        /* reallyMoved(
                             dragFrom,
                             dragTo,
                             adapter.getItem(dragTo).id
                         )*/

                    }

                    dragFrom = -1
                    dragTo = -1
                }

            })

        touchHelper.attachToRecyclerView(binding.rvModifiersList)


        variationListAdapter = VariationListAdapter(viewModel)
        variationListAdapter.setCallback(this)
        binding.rvVariationList.adapter = variationListAdapter
    }

    private fun setupData() {


        binding.header.txtSave.text = getString(R.string.save)
        binding.header.txtTitle.text = getString(R.string.add_new_itemswithoutplus)

        if (isEdit) {

            binding.header.txtSave.text = getString(R.string.update)
            binding.header.txtTitle.text = getString(R.string.update_item)
            itemObject = arguments?.getParcelable("itemObject")!!
            viewModel.setData(itemObject)
            selectedId = itemObject.categoryId
            viewModel.setCategoryId(selectedId)

            //taxes
            val taxIds = ArrayList<String>()
            var taxNameToDisplay = ""
            itemObject.taxes?.forEach {
                taxIds.add("${it.id}")
                taxNameToDisplay += "${it.name}, "
            }
            if (taxNameToDisplay.isNotEmpty()) {
                taxNameToDisplay = taxNameToDisplay.dropLast(2)
                viewModel.taxNameToDisplay = taxNameToDisplay
            }
            viewModel.selectedTaxList(taxIds)
            binding.txtTaxName.text = viewModel.taxNameToDisplay

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

            //profile image
            viewProfile(itemObject.imageUrl)

        } else {
            viewModel.itemDetails.value?.productCode = productCode ?: ""
            getAllTaxesList()
        }

        binding.etItemPrice.addTextChangedListener(
            ItemPriceTextWatcher(
                binding.etItemPrice,
                false
            )
        )


        binding.chooseCategory.setOnClickListener(this)
        binding.ivCategory.setOnClickListener(this)
        binding.chooseTax.setOnClickListener(this)
        binding.ivTax.setOnClickListener(this)
        binding.imgEdit.setOnClickListener(this)
        binding.llTapToEdit.setOnClickListener(this)

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
        setFragmentResultListener("tax_request_key") { requestKey: String, bundle: Bundle ->
            val selectedIds = bundle.getStringArrayList("selectedId")
            val nameToDisplay = bundle.getString("nameToDisplay", "")
            viewModel.taxNameToDisplay = nameToDisplay
            binding.txtTaxName.text = viewModel.taxNameToDisplay
            selectedIds?.let { viewModel.selectedTaxList(it) }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.header.imgBack.setOnClickListener {
            MethodUtils.hideKeyboard(requireActivity())
            val navControll = findNavController()
            navControll.previousBackStackEntry?.savedStateHandle?.set(
                Constants.KEY,
                Constants.CREATEITEM
            )
            navControll.popBackStack()
        }

        // To update modifiers list if items set is updated for any modifier
        setFragmentResultListener("request_key_modifier_set_updated") { requestKey: String, bundle: Bundle ->
            val isDataUpdated = bundle.getBoolean("isUpdated")
            if (isDataUpdated) {
                getModifiers()
            }
        }

    }

    private fun observeShowProgress() {


    }

    private fun setupSnackbar() {


        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)


    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.chooseCategory -> {
                openCategoryDialog()

            }
            R.id.ivCategory -> {
                openCategoryDialog()

            }
            R.id.chooseTax -> {
                openTaxDialog()
            }
            R.id.ivTax -> {
                openTaxDialog()
            }
            R.id.imgEdit -> {
                var profileImg = ""
                if (::itemObject.isInitialized && !itemObject.imageUrl.isNullOrEmpty()) {
                    profileImg = itemObject.imageUrl ?: ""

                }
                val bundle = Bundle()
                bundle.putString("imgUrl", profileImg)
                findNavController().navigate(R.id.action_createItem_to_itemEditTitleDialog, bundle)
            }
            R.id.llTapToEdit -> {
                var profileImg = ""
                if (::itemObject.isInitialized && !itemObject.imageUrl.isNullOrEmpty()) {
                    profileImg = itemObject.imageUrl ?: ""

                }
                val bundle = Bundle()
                bundle.putString("imgUrl", profileImg)
                findNavController().navigate(R.id.action_createItem_to_itemEditTitleDialog, bundle)
            }
        }
    }

    private fun openTaxDialog() {
        val bundle = Bundle().apply {
            putStringArrayList("selectedId", viewModel.getSelectedTaxList())
            putBoolean("isEdit", isEdit)
        }
        if (findNavController().currentDestination?.id == R.id.createItem) {
            findNavController().navigate(R.id.action_createItem_to_taxesDialog, bundle)
        }
    }

    private fun openCategoryDialog() {
        val bundle = Bundle().apply {
            putInt("selectedId", selectedId)
        }
        if (findNavController().currentDestination?.id == R.id.createItem) {
            findNavController().navigate(R.id.action_createItem_to_categoriesDialog, bundle)
        }
    }

    private fun navigate() {

        viewModel.data.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let { createTaxResponse ->
                activity?.let {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        it, createTaxResponse.message
                    ) { _, _ ->

                        val navControll = findNavController()
                        navControll.previousBackStackEntry?.savedStateHandle?.set(
                            Constants.KEY,
                            Constants.CREATEITEM
                        )
                        findNavController().popBackStack()
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
            //  LogUtil.logE("!_@_ image path", result)
            viewProfile(result)
        }
    }

    override fun onItemClickListener(position: Int, variation: VariationsAttribute) {
        if (isDoubleClick()) return
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
            .into(binding.imgItem)
    }

    override fun onItemClickListener(view: View?, pos: Int) {
        val popupMenu = view?.let { PopupMenu(requireContext(), it) }
        popupMenu?.menuInflater?.inflate(R.menu.edit_delete_menu, popupMenu.menu)
        popupMenu?.menu?.findItem(R.id.menu_delete)?.isVisible = false

        popupMenu?.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_edit -> {
                    position1 = pos
                    val bundle = Bundle().apply {
                        putBoolean("isEdit", true)
                        putParcelable("modifierObject", adapter.getItem(pos))
                    }
                    findNavController().navigate(
                        R.id.action_createItem_to_editModifiersDialog,
                        bundle
                    )
                }
            }
            true
        }
        popupMenu?.show()
    }

    override fun onDeleteCallback(modifierSet: ModifierSet) {


        if (isEdit) {
            modifierSet.id?.let {
                itemObject.modifier_set_ids.toCollection(arrayListOf()).remove(it)
            }
        }

        runOnUiThread(Runnable {
            spinnerList.add(modifierSet)

            binding.edtModifiersList?.adapter = null
            Log.e(TAG, "spinnerListSize:  ${spinnerList.size}")
            spinnerAdapter =
                ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    spinnerList
                )
            spinnerAdapter?.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            binding.edtModifiersList?.adapter = spinnerAdapter

            binding.edtModifiersList?.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        p0: AdapterView<*>?,
                        p1: View?,
                        position: Int,
                        p3: Long
                    ) {
                        Log.e("OnSpinItemSelected", "OnItemSelected")
                        if (position != 0) {
                            var mod = spinnerList.get(position)

                            mod.id?.let { it1 ->
                                if (isEdit) {
                                    itemObject.modifier_set_ids.toCollection(arrayListOf())
                                        .add(
                                            it1
                                        )
                                }

                                runOnUiThread(Runnable {
                                    spinnerAdapter?.remove(spinnerList.get(position))
                                    spinnerAdapter?.notifyDataSetChanged()
                                    binding.edtModifiersList?.setSelection(0)
                                    adapter.addItem(mod)
                                })
                            }

                            //  viewModel.updateMod(mod)


                        }

                    }

                    override fun onNothingSelected(p0: AdapterView<*>?) {

                    }

                }


        })


    }

    override fun onDelete(pos: Int) {

        var modifierSet = adapter.getAll().get(pos)

        if (isEdit) {
            modifierSet.id?.let {
                val updatedModifiersList: ArrayList<Int> = arrayListOf()
                updatedModifiersList.addAll(itemObject.modifier_set_ids)
                updatedModifiersList.remove(it)
                itemObject.modifier_set_ids = updatedModifiersList
//                itemObject.modifier_set_ids.toCollection(arrayListOf()).remove(it)
            }
        }

        adapter.removeItem(pos)

        runOnUiThread(Runnable {
            spinnerList.add(modifierSet)

            binding.edtModifiersList?.adapter = null
            Log.e(TAG, "spinnerListSize:  ${spinnerList.size}")
            spinnerAdapter =
                ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    spinnerList
                )
            spinnerAdapter?.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            binding.edtModifiersList?.adapter = spinnerAdapter

            binding.edtModifiersList?.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        p0: AdapterView<*>?,
                        p1: View?,
                        position: Int,
                        p3: Long
                    ) {
                        Log.e("OnSpinItemSelected", "OnItemSelected")
                        if (position != 0) {
                            var mod = spinnerList.get(position)

                            mod.id?.let { it1 ->
                                if (isEdit) {
                                    val updatedModifierList: ArrayList<Int> = arrayListOf()
                                    updatedModifierList.addAll(itemObject.modifier_set_ids)
                                    updatedModifierList.add(it1)
                                    itemObject.modifier_set_ids = updatedModifierList
//                                    itemObject.modifier_set_ids.toCollection(arrayListOf())
//                                        .add(
//                                            it1
//                                        )
                                }

                                runOnUiThread(Runnable {
                                    spinnerAdapter?.remove(spinnerList.get(position))
                                    spinnerAdapter?.notifyDataSetChanged()
                                    binding.edtModifiersList?.setSelection(0)
                                    adapter.addItem(mod)
                                })
                            }

                            //  viewModel.updateMod(mod)


                        }

                    }

                    override fun onNothingSelected(p0: AdapterView<*>?) {

                    }

                }


        })

    }

    /*if (isDoubleClick()) return
    position1 = pos
    val bundle = Bundle().apply {
        putBoolean("isEdit", true)
        putParcelable("modifierObject", adapter.getItem(pos))
    }
    findNavController().navigate(
    R.id.action_createItem_to_editModifiersDialog,
    bundle
    )*/

    private fun getAllTaxesList() {
        var nameToDisplay = ""
        categoriesViewModel.enableTaxes.observe(viewLifecycleOwner) {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        if (it.data?.isNotEmpty() == true) {
                            allTaxes = it.data
                            for (i in 0 until it.data.size) {
                                nameToDisplay += "${it.data[i].name}, "
                                if (i > 4) break
                            }
                            if (nameToDisplay.isNotEmpty()) {
                                viewModel.taxNameToDisplay = nameToDisplay.dropLast(2)
                                binding.txtTaxName.text = viewModel.taxNameToDisplay
                            }
                        }
                    }
                }
            }
        }
    }
}