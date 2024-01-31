package com.pays.pos.ui.fragments.dashboard.bolddashboard

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import com.pays.pos.R
import com.pays.pos.data.entities.CartModel
import com.pays.pos.data.entities.Modifier
import com.pays.pos.data.entities.TbCartItem
import com.pays.pos.data.entities.TbItem
import com.pays.pos.data.entities.TbServiceCharge
import com.pays.pos.data.entities.VariationsAttribute
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.DELETE
import com.pays.pos.data.remote.Constants.DINE_IN
import com.pays.pos.data.remote.Constants.DINE_IN_UPDATE
import com.pays.pos.data.remote.Constants.MAX_ITEM_QUANTITY
import com.pays.pos.data.remote.Constants.ORDER_TYPE
import com.pays.pos.data.remote.Constants.SERVICECHARGE_TAKEOUT_OPENORDER
import com.pays.pos.data.remote.Constants.TAKEOUT
import com.pays.pos.data.remote.Constants.TERMINAL_ID
import com.pays.pos.databinding.FragmentAddItemBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.ui.adapter.ItemModifierSetAdapter
import com.pays.pos.ui.adapter.VariationDashboardListAdapter
import com.pays.pos.ui.adapter.boldpos.ModifiersAdapter
import com.pays.pos.ui.adapter.boldpos.VariationListAdapter
import com.pays.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.MethodUtils
import com.pays.pos.utils.callback.ItemCallback
import com.pays.pos.utils.callback.ItemListner
import com.pays.pos.utils.callback.ModifierLongClickCallback
import com.pays.pos.utils.extensions.gone
import com.pays.pos.utils.extensions.visible
import com.pays.pos.utils.statusUtils.Resource
import com.pays.pos.utils.statusUtils.Status
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class AddItemFragment(val listner: ItemListner) : Fragment(), ItemCallback,
    ModifierLongClickCallback {
    private var mainItem: TbCartItem? = null
    private lateinit var item: TbCartItem
    private var cartModelsList: ArrayList<CartModel> = arrayListOf()
    private lateinit var binding: FragmentAddItemBinding
    private var serviceChargesList: ArrayList<TbServiceCharge>? = null
    private var serviceChargesObserve: Observer<Resource<List<TbServiceCharge>>>? = null
    private val viewModel by activityViewModels<DashBoardCategoryViewModel>()
    private val TAG = "AddItemFragment"
    private lateinit var variationAdapter: VariationListAdapter
    private lateinit var modifiersAdapter: ModifiersAdapter
    private var isUpdateItem: Boolean = false

    private var itemModifiersAdapter: ItemModifierSetAdapter? = null
    private var intArray: IntArray? = null
    private var mainModifiersId: ArrayList<Int> = arrayListOf()
    private var mainVariationId: ArrayList<Int> = arrayListOf()
    private var originalModifiersList: List<Modifier> = arrayListOf()

    @Inject
    lateinit var prefProvider: PrefProvider
    private var qty = 1
    private var itemPosition = -1

    companion object {
        fun newInstance(
            item: TbCartItem,
            callback: ItemListner,
            cartListModel: ArrayList<CartModel>,
            isItemUpdate: Boolean,
            itemPosition: Int = -1
        ): AddItemFragment {
            val bundle: Bundle = Bundle()
            bundle.putParcelable("item", item)
            bundle.putBoolean(Constants.IS_UPDATE_ITEM, isItemUpdate)
            bundle.putSerializable("cartList", cartListModel)
            bundle.putInt("itemPosition", itemPosition)
            val frag = AddItemFragment(callback)
            frag.arguments = bundle
            return frag

        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAddItemBinding.inflate(inflater, container, false)
        getServiceCharges()
        binding.lifecycleOwner = this
        variationAdapter = VariationListAdapter()
        binding.rvVariationList.adapter = variationAdapter
        variationAdapter.setCallback(this)
        isUpdateItem = requireArguments().getBoolean(Constants.IS_UPDATE_ITEM)
        Log.e("GetDataAdd", "isUpdateItem:    ${isUpdateItem}")

        binding.txtDone.isEnabled = isUpdateItem

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getData()
        onClick()
        getCartList()
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<VariationsAttribute>(
            Constants.DIALOG_KEY_VARIATION_DETAILS
        )?.observe(viewLifecycleOwner) { it ->
            if (variationAdapter.variationList.size > 0) {
                variationAdapter.updateVariation(it)
                var variation: VariationsAttribute? = null
                if (variationAdapter != null) {

                    variation = variationAdapter.getItem()
                } else if (it != null) {
                    variation = it
                }




                if (variation != null) {
                    variation?.price?.let {
                        item.price = it
                    }

                } else {
                    item.price = item.price
                }
            }

        }
        binding.edttxtQuantity.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                binding.edttxtQuantity?.isCursorVisible = true
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                if (s.toString().isNotEmpty()) {
                    qty = s.toString().toInt()
                    if (/*!item.isManualSales &&  */qty > MAX_ITEM_QUANTITY) {
                        qty = MAX_ITEM_QUANTITY
                        binding.edttxtQuantity.setText(MAX_ITEM_QUANTITY.toString())
                    }/* else if(item.isManualSales &&  qty > MAX_ITEM_QUANTITY_FOR_MANUAL_SALES){
                        qty = MAX_ITEM_QUANTITY_FOR_MANUAL_SALES
                        binding.edttxtQuantity.setText(MAX_ITEM_QUANTITY_FOR_MANUAL_SALES.toString())
                    }*/ else if (qty == 0) {
                        binding.edttxtQuantity.setText("1")
                    }
                    binding.edttxtQuantity.setSelection(binding.edttxtQuantity.text!!.length)
                } else {
                    try {
                        qty = binding.edttxtQuantity.text!!.toString().toInt()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    binding.edttxtQuantity.setSelection(binding.edttxtQuantity.text!!.length)
                }

            }

        })
    }

    private fun getCartList() {
        viewModel.observeLatestCartModel().observe(requireActivity()) {
            if (it.isEmpty()) {
                cartModelsList.clear()
                cartModelsList = arrayListOf()

            } else {
                cartModelsList.clear()
                cartModelsList = arrayListOf()
                cartModelsList.addAll(it.toCollection(arrayListOf()))
            }


        }
    }


    private fun onClick() {
        binding.imgMinus.setOnClickListener {
            MethodUtils.hideSoftKeyboard(requireActivity())
            /* if (prefProvider.getValueboolean(DINE_IN_UPDATE, false) == true && item.isFired) {
                 if (qty > item.itemQuantity) {
                     qty -= 1
                 } else {
                     qty = qty
                 }
             } */

            if (qty == 1) {
                qty = 1
            } else {
                qty -= 1
            }


            binding.edttxtQuantity.setText("" + qty)

        }
        binding.imgPlus.setOnClickListener {
            MethodUtils.hideSoftKeyboard(requireActivity())
            qty += 1
            binding.edttxtQuantity.setText("" + qty)
        }

        binding.txtCancel.setOnClickListener {
            MethodUtils.hideSoftKeyboard(requireActivity())
            if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == DINE_IN) {
                listner.onCancelItemSelected(true)
            } else {
                requireActivity().supportFragmentManager.popBackStackImmediate(
                    AddItemFragment.javaClass.getName(),
                    FragmentManager.POP_BACK_STACK_INCLUSIVE
                )
                //  listner.onCancelItemSelected(false)

            }
        }

        binding.txtDone.setOnClickListener(object:View.OnClickListener{
            override fun onClick(p0: View?) {

                if (binding.edttxtQuantity.text.isNullOrEmpty()) {
                    binding.edttxtQuantity.setText("1")
                }
                MethodUtils.hideSoftKeyboard(requireActivity())
                item.itemQuantity = qty
                prefProvider.setValueInt(Constants.CAT_ID_SELECTED, item.categoryId)
                var isPriceNull = true

                if (prefProvider.getValue(ORDER_TYPE, "") == Constants.OPEN_ORDER) {

                    if (cartModelsList.isEmpty()) {
                        val model = CartModel()
                        model.employeeID =
                            prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
                        model.terminalId = prefProvider.getValueInt(TERMINAL_ID, 0)
                        model.orderType = prefProvider.getValue(ORDER_TYPE, "")
                        model.locationId = prefProvider.getValueInt(Constants.LOCATION_ID, 1)
                        model.serviceCharge = serviceChargesList
                        viewModel.ordertypelist.forEach {
                            if (it.orderType == Constants.OPEN_ORDER) {
                                model.orderTypeId = it.id
                            }
                        }
                        cartModelsList.add(model)
                    }

                } else {
                    viewModel.createCart(cartModelsList)
                }



                if (item.modifier_set_ids.isNotEmpty() && itemModifiersAdapter != null) {
                    if (minMaxValidationCheck(itemModifiersAdapter)) {

                        val modifiers = itemModifiersAdapter?.getSelectedModifiers() ?: arrayListOf()
                        if (isUpdateItem && originalModifiersList.isNotEmpty()) {
                            var updatedModifiersList: ArrayList<Modifier> = arrayListOf()
                            updatedModifiersList.addAll(modifiers)
                            try {
                                // get removed modifiers from list
                                for (originalMod in originalModifiersList) {
                                    var removedModifier: Modifier?
                                    removedModifier =
                                        modifiers.find { updatedMod -> originalMod.id == updatedMod.id }
                                    if (removedModifier == null) {
                                        originalMod.apply {
                                            _destroy = true
                                            isChecked = false
                                        }
                                        if (!updatedModifiersList.contains(originalMod)) {
                                            updatedModifiersList.add(originalMod)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            modifiers.clear()
                            modifiers.addAll(updatedModifiersList)
                        }

                        if (modifiers.isNotEmpty()) {
                            modifiers.forEach {
                                if (item.modifiers.isNotEmpty()) {
                                    item.modifiers.forEach { it1 ->
                                        if (it.id == it1.id) {
                                            // it1.itemQuantity = it.itemQuantity
                                            it.orderModifierId = it1.orderModifierId

                                        }
                                        it.itemQuantity = qty
                                    }
                                } else {
                                    it.itemQuantity = qty
                                }
                            }
                            item.modifiers = modifiers


                        } else {
                            item.modifiers = arrayListOf()
                        }
                    } else {
                        AlertUtils.showCustomAlert(
                            binding.root.context,
                            binding.root.context.getString(R.string.you_can_add)
                        )
                        viewModel.doesItemContainsModifiers.value = false
                        return
                    }

                }

                val variationList = ArrayList<VariationsAttribute>()
                if (item.variationsAttributes.isNotEmpty()) {
                    if (variationAdapter.variationList.isNotEmpty()) {
                        val variation = variationAdapter.getItem()
                        variationList.add(variation)
                        item.name = item.name.substringBefore(" (") + " (" + variation.name + ")"
                        item.price = variation.price ?: 0.0
                        if (item.price == 0.0 && variation.priceType == "Variable") {
                            AlertUtils.showCustomAlertWithListenerWithOK(
                                requireContext(), "Please enter amount"
                            ) { _, _ ->

                            }
                            viewModel.doesItemContainsModifiers.value = false
                            return
                        }
                        item.variationsAttributes = variationList
                    } else {
                        viewModel.doesItemContainsModifiers.value = true
                        return
                    }

                }


                var found = false


                for(it in viewModel.currentCartItems){
                    if(it.cartItemId!=item.cartItemId)
                    {
                        if (it.name == item.name) {
                            if (viewModel.checkModifierNew(it, item)) {
                                Log.e(
                                    "Tracking Cart",
                                    "SAME ITEM  ${it.cartItemId} && ${item.cartItemId}"
                                )
                                found = true
                                it.itemQuantity += item.itemQuantity
                                break
                            }
                        }
                    }else {
                        Log.e("Tracking Cart","FOUND SAME ITEM ${it.cartItemId } && ${item.cartItemId}")
                    }
                }


                if (found) {
                    // Added to resolve Add Discount issue BIS-3547
                    viewModel.cartFooterNeedToBeUpdated = true

                    if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == Constants.DINE_IN) {
                        item.guestIndexForDineIn = viewModel.dineInHeaderPosition
                        val dineInList = cartModelsList[0].dineInList
                        dineInList?.get(0)?.headerPosition = viewModel.dineInSelectedItemHeaderPos
                        dineInList?.get(0)?.selectedPosition = viewModel.dineInSelectedItemHeaderPos
                        LogUtil.logE(TAG, "getItem  ${Gson().toJson(item)}")
                        cartModelsList[0].taxlistDynamic = arrayListOf()
                        /*cartList[0].dineInList?.forEach { dineInModel ->
                            dineInModel.items.forEach { items ->
                                items.taxes?.forEach { taxData ->
                                    taxData.subTotalAmount = 0.0
                                    taxData.totalTaxTypePrice = 0.0
                                }
                            }
                        }*/
                        viewModel.currentCartItems.forEach {
                            it.taxes?.forEach { taxData ->
                                taxData.subTotalAmount = 0.0
                                taxData.totalTaxTypePrice = 0.0
                            }
                        }

                        Log.e(TAG, "dineInListWhenUpdate:  ${Gson().toJson(cartModelsList)}")
                        /*viewModel.newCartLogicModifier(
                            cartList,
                            item,
                            Constants.UPDATE,
                            false,
                            dineInList ?: arrayListOf()
                        )*/
                        Log.d(TAG, "398 dineintest currentCartItems: " + viewModel.currentCartItems)
                        Log.d(TAG, "dineintest item: " + item)
                        Log.d(TAG, "dineintest dineInList: " + dineInList)
                        viewModel.updateDineInCart(
                            viewModel.currentCartItems,
                            item,
                            Constants.UPDATE,
                            false,
                            dineInList ?: arrayListOf()
                        )
                    } else {
                        item.guestIndexForDineIn = null
                        cartModelsList[0].taxlistDynamic = arrayListOf()
                        viewModel.currentCartItems.forEach { items ->
                            items.taxes?.forEach { taxData ->
                                taxData.subTotalAmount = 0.0
                                taxData.totalTaxTypePrice = 0.0
                            }
                        }


                        if (checkVar()) {

                            LogUtil.logE("NewItem", "ItemSame ${Gson().toJson(item)}")

                           // if(!isUpdateItem)
                            viewModel.currentCartItems.remove(item)

                            runBlocking {

                                viewModel.deleteCartItems()

                                    viewModel.currentCartItems.forEach {
                                        viewModel.addItemToCartItems(it)
                                    }
                                }

//                            viewModel.updateCart(
//                                viewModel.currentCartItems,
//                                item,
//                                Constants.UPDATE,
//                                false,
//                                position = itemPosition
//                            )
                            viewModel.doesItemContainsModifiers.value = true

                        } else {
                            LogUtil.logE("NewItem", "ItemSameNot")

                      //   if(!isUpdateItem)
                        viewModel.currentCartItems.remove(item)

                            runBlocking {
                                viewModel.deleteCartItems()

                                viewModel.currentCartItems.forEach {
                                    viewModel.addItemToCartItems(it)
                                }
                            }



//                            item.orderItemId = null
//                            viewModel.updateCart(
//                                viewModel.currentCartItems,
//                                item,
//                                Constants.UPDATE,
//                                false,
//                                position = itemPosition
//                            )
                            viewModel.doesItemContainsModifiers.value = true


//                            var found = false
//
//                            viewModel.currentCartItems.forEach {
//
//                                Log.e("Tracking Cart","Each Item ${it.name}")
//
//                                if(it.name == item.name)
//                                {
//                                    Log.e("Tracking Cart","SAME ITEM")
//                                    if(viewModel.checkModifierNew(it,item)){
//                                        found = true
//                                        it.itemQuantity += 1
//                                        return@forEach
//                                    }
//                                }
//                            }
//
//                            if(!found)
//                                viewModel.updateCart(viewModel.currentCartItems, item, Constants.UPDATE, false)
//                            else {
//
//                                runBlocking {
//                                    viewModel.deleteCartItems()
//
//                                    viewModel.currentCartItems.forEach {
//
//                                        viewModel.addItemToCartItems(it)
//                                    }
//                                }
//                            }
//
//                            LogUtil.logE("NewItem", "ItemSameNot")
//                            item.orderItemId = null
////                            viewModel.updateCart(
////                                viewModel.currentCartItems,
////                                item,
////                                Constants.UPDATE,
////                                false,
////                                position = itemPosition
////                            )
//                            viewModel.doesItemContainsModifiers.value = true
                        }


                    }


                } else {
                    if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == Constants.DINE_IN) {
                        item.guestIndexForDineIn = viewModel.dineInHeaderPosition
                        if (prefProvider.getValueboolean(DINE_IN_UPDATE, false)) {
                            item.isEdited = true
                        }
                        val dineInList = cartModelsList[0].dineInList
                        Log.e(TAG, "checkCartIsEmpty  ${cartModelsList.size}")
                        LogUtil.logE(TAG, "dineInList:  ${Gson().toJson(dineInList)}")
                        if (dineInList?.isNotEmpty() == true && dineInList != null) {
                            dineInList[0].selectedPosition = viewModel.dineInHeaderPosition
                            /*viewModel.newCartLogicModifier(
                                cartModelsList,
                                item,
                                Constants.ADD,
                                false,
                                dineInList
                            )*/

                            Log.d(TAG, "448 dineintest currentCartItems: " + viewModel.currentCartItems)
                            Log.d(TAG, "dineintest item: " + item)
                            Log.d(TAG, "dineintest dineInList: " + dineInList)
                            viewModel.updateDineInCart(
                                viewModel.currentCartItems,
                                item,
                                Constants.ADD,
                                false,
                                dineInList
                            )
                        }
                    } else
                    {
                        item.guestIndexForDineIn = null

                        Log.e("cshffasf", "checkElsee")
                        //val tbItem = TbCartItem().convertToCartItem(item, item)

                        var newFound = false

                        viewModel.currentCartItems.forEach {

                            Log.e("Tracking Cart","Each Item ${it.name}")

                            if(it.name == item.name)
                            {
                                Log.e("Tracking Cart","SAME ITEM")
                                if(viewModel.checkModifierNew(it,item)){
                                    it.itemQuantity=item.itemQuantity
                                    newFound = true
                                    return@forEach
                                }
                            }
                        }


                        if(!newFound)
                            viewModel.updateCart(viewModel.currentCartItems, item, Constants.ADD, false)
                        else{

                            //viewModel.currentCartItems.remove(item)

                            runBlocking {
                                viewModel.deleteCartItems()

                                viewModel.currentCartItems.forEach {

                                    viewModel.addItemToCartItems(it)
                                }
                            }


                        }
                        viewModel.doesItemContainsModifiers.value = true


//                        item.guestIndexForDineIn = null
//
//                        Log.e("cshffasf", "checkElsee")
//                        //val tbItem = TbCartItem().convertToCartItem(item, item)
//                        viewModel.updateCart(viewModel.currentCartItems, item, Constants.ADD, false)
//                        viewModel.doesItemContainsModifiers.value = true

                    }
                }

                requireActivity().supportFragmentManager.popBackStackImmediate(
                    AddItemFragment.javaClass.getName(),
                    FragmentManager.POP_BACK_STACK_INCLUSIVE
                )
                //listner.onCancelItemSelected()


            }
        })


        binding.txtAddDiscount.setOnClickListener {
            var totalItemswithQuantity = 0

            // Added to resolve Add Discount issue BIS-3547
            viewModel.discountNeedToUpdate = false

            if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == DINE_IN) {
                viewModel.cartModel?.dineInList?.forEach {
                    it.items.forEach { it1 ->
                        totalItemswithQuantity += it1.itemQuantity
                    }
                }
            } else {
                viewModel.currentCartItems.forEach {
                    totalItemswithQuantity += it.itemQuantity

                }
            }

            var perItemDiscount = 0.0
            if (viewModel.cartModel?.discountPrice != 0.0) {
                if (totalItemswithQuantity == 0) {
                    totalItemswithQuantity = 1
                }
                perItemDiscount =
                    MethodUtils.roundOffAmountDouble(viewModel.cartModel?.discountPrice!! / totalItemswithQuantity)
            }

            LogUtil.logE(TAG, "totalItemswithQuantity  ${totalItemswithQuantity}")
            LogUtil.logE(TAG, "perItemDiscount  ${perItemDiscount}")
            val bundle = Bundle().apply {
                putDouble("orderDiscount", viewModel.cartModel?.discountPrice!!)
                putBoolean("isFromDetails", true)
                putParcelable("model", item)
                putDouble("itemOrderDiscount", perItemDiscount)
                putInt("totalquantity", totalItemswithQuantity)
            }
            bundle.putString("isFrom", "itemDiscount")
            if (prefProvider.isAdmin() || prefProvider.isManager()) {
                findNavController().navigate(
                    R.id.action_dashboardCategoryBoldPOS_to_addDiscountDialog,
                    bundle
                )
            } else {
                findNavController().navigate(
                    R.id.actionboldpos_to_pascodeManagerDailog, bundle
                )
            }

        }
        binding.txtAddNote.setOnClickListener {

            LogUtil.logE("HeaderPos", "${viewModel.dineInSelectedItemHeaderPos}")
            LogUtil.logE("HeaderPosdineInHea", "${viewModel.dineInHeaderPosition}")
            val bundle = Bundle().apply {
                putParcelable("item", item)
                putParcelableArrayList("cartList", cartModelsList)
                putInt("headerPos", viewModel.dineInHeaderPosition)
            }

            if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
                findNavController().navigate(
                    R.id.action_dashboardCategoryBoldPOS_to_addNoteDialog,
                    bundle

                )
            }
        }

        binding.txtRemoveItem.setOnClickListener {
            Log.e(TAG, "getDeleteItem  ${Gson().toJson(item)}")
            makeItemEditedNew(item)
            if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == DINE_IN) {
                item.guestIndexForDineIn = viewModel.dineInHeaderPosition
                LogUtil.logE(TAG, "isEditedisEdited  ${item.isEdited}")
                cartModelsList[0].dineInList?.let { it1 ->
                    /*viewModel.newCartLogicModifier(
                        cartModelsList, item, DELETE, false,
                        it1
                    )*/

                    Log.d(TAG, "548 dineintest currentCartItems: " + viewModel.currentCartItems)
                    Log.d(TAG, "dineintest item: " + Gson().toJson(item))
                    Log.d(TAG, "dineintest dineInList: " + it1)
                    viewModel.updateDineInCart(viewModel.currentCartItems, item, DELETE, false, it1)
                }
            } else {
                item.guestIndexForDineIn = null
                viewModel.updateCart(viewModel.currentCartItems, item, DELETE, item.isManualSales)
                //viewModel.newCartLogicModifier(cartModelsList, item, DELETE, item.isManualSales)

            }


            if(viewModel.currentCartItems.size==1) {

                createCart()

                viewModel.fragmentNeedToBeUpdated.value = true

                Log.e("Tracking Cart","IN"+viewModel.currentCartItems.size.toString())
            }else
            {
                Log.e("Tracking Cart",viewModel.currentCartItems.size.toString())
            }


            requireActivity().supportFragmentManager.popBackStackImmediate(
                AddItemFragment.javaClass.getName(),
                FragmentManager.POP_BACK_STACK_INCLUSIVE
            )
            //  listner.onCancelItemSelected()


        }

        requireActivity().supportFragmentManager.setFragmentResultListener(
            "request_key_note",
            viewLifecycleOwner
        ) { requestKey: String, bundle: Bundle ->
            val note = bundle.getString("note")
            item.note = note.toString()
        }
    }



    fun createCart(): ArrayList<CartModel>? {
        if (cartModelsList.isEmpty()) {
            val model = CartModel()
            model.employeeID =
                prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
            model.terminalId = prefProvider.getValueInt(TERMINAL_ID, 0)
            model.orderType = TAKEOUT
            model.locationId = prefProvider.getValueInt(Constants.LOCATION_ID, 1)
            model.serviceCharge = serviceChargesList
            // model.orderTypeId = 1
            viewModel.ordertypelist.forEach {
                if (it.orderType.lowercase() == TAKEOUT.lowercase()) {
                    model.orderTypeId = it.id
                }
            }
            cartModelsList.add(0, model)
            // viewModel.createEmptyCart(model)
            return cartModelsList
        }

        return cartModelsList
    }

    private fun getData() {
        originalModifiersList = arrayListOf()
        item = requireArguments().getParcelable<TbCartItem>("item") ?: TbCartItem()
        Log.e(TAG, "getMainItemAdd  ${Gson().toJson(item)}")
        originalModifiersList = item.itemOriginalModifiersList ?: arrayListOf()
        if (item.modifiers.isNotEmpty()) {
            item.modifiers.forEach {
                mainModifiersId.add(it.id ?: 0)
            }
        }
        if (item.variationsAttributes.isNotEmpty()) {
            item.variationsAttributes.forEach {
                mainVariationId.add(it.id ?: 0)
            }
        }
        mainItem = requireArguments().getParcelable<TbCartItem>("item") ?: TbCartItem()
        if (requireArguments().containsKey("itemPosition")) {
            itemPosition = requireArguments().getInt("itemPosition") ?: -1
        }
        LogUtil.logE(TAG, "getIrem  ${Gson().toJson(item)}")
        cartModelsList = requireArguments().getSerializable("cartList") as ArrayList<CartModel>
        setData()
    }

    private fun setData() {
        binding.txtItem.text = "" + item?.name
        binding.txtPrice.text = MethodUtils.roundOffAmount(item.price)
        if (view != null) {
            Log.e(TAG, "checkItemID:  ${item.itemId}")
            viewModel.getItemsbyId(item.itemId).observe(viewLifecycleOwner) {

                it?.let { resource ->
                    when (resource.status) {
                        Status.SUCCESS -> {
                            it.data?.let { it1 ->
                                Log.e(TAG, "getItemForMos  ${Gson().toJson(it)}")


                                binding.rvVariationList.visibility = View.VISIBLE
                                intArray = IntArray(it1.modifier_set_ids.size) { i ->
                                    it1.modifier_set_ids[i]
                                }

                                if (isUpdateItem) {
                                    item.modifier_set_ids = it1.modifier_set_ids
                                }
                                variationAdapter.addVariations(it1.variationsAttributes.filter { !it.isDeleted })
                                val variationList = ArrayList<VariationsAttribute>()
                                Log.e(
                                    TAG,
                                    "variationsAttributesInData   ${Gson().toJson(item.variationsAttributes)}"
                                )
                                if (item.variationsAttributes.isNotEmpty()) {
                                    binding.dividerLine.root.visibility = View.VISIBLE
                                    val variation = item.variationsAttributes[0]
                                    variationList.add(variation)
                                    item.name =
                                        item.name.substringBefore(" (") + " (" + variation.name + ")"
                                    item.variationsAttributes = variationList


                                    if (isUpdateItem) {
                                        variation.id?.let { it1 -> variationAdapter.selectItem(it1) }
                                        variationAdapter.updateVariation(variation)

                                    } else {
                                        variationList.get(0).id?.let { it1 ->
                                            variationAdapter.selectItem(
                                                it1
                                            )
                                        }
                                        variationAdapter.updateVariation(variation)

                                    }

                                    variationAdapter.showVariationPriceClick =
                                        { it1: VariationsAttribute ->
                                            Log.e(TAG, "VariationClicked")
                                            if (!MethodUtils.isDoubleClick()) {
                                                LogUtil.logE(TAG, "getpriceType:  ${it1.priceType}")
                                                if (it1.priceType == "Variable") {
                                                    val bundle = Bundle().apply {
                                                        putParcelable("variationAttribute", it1)
                                                    }
                                                    findNavController().navigate(
                                                        R.id.action_dashboardCategoryBoldPOS_to_addVariablePriceDialog,
                                                        bundle
                                                    )


                                                } else if (it1.priceType == "Fixed") {


                                                    var variation: VariationsAttribute? = null
                                                    if (variationAdapter != null) {
                                                        variation = variationAdapter.getItem()
                                                    } else if (variation != null) {
                                                        variation = it1
                                                    }

                                                    binding.txtPrice.text =
                                                        it1.price?.let { it2 ->
                                                            MethodUtils.roundOffAmount(
                                                                it2
                                                            )
                                                        }



                                                    if (variation != null) {
                                                        variation.price?.let {
                                                            item.price = it
                                                        }

                                                    } else {
                                                        item.price = item.price
                                                    }
                                                }

                                            }
                                        }
                                    binding.txtDone.isEnabled = true
                                }


                                if (intArray!!.isNotEmpty()) {
                                } else binding.dividerLine.root.visibility = View.GONE

                                if (intArray!!.isNotEmpty()) {
                                    if (itemModifiersAdapter == null) {
                                        binding.dividerLine2.root.visibility = View.GONE
                                    } else {
                                        binding.dividerLine2.root.visibility = View.VISIBLE
                                    }

                                    Log.e(TAG, "intArrayList   ${Gson().toJson(intArray)}")
                                    viewModel.modifierSet(intArray!!).observe(requireActivity()) {
                                        if (it.data != null && it.data.isNotEmpty() && view != null) {
                                            itemModifiersAdapter = ItemModifierSetAdapter(
                                                viewModel,
                                                item.itemId,
                                                viewLifecycleOwner
                                            )
                                            itemModifiersAdapter?.setLongCallback(this)
                                            binding.rvModifiersList.adapter = itemModifiersAdapter
                                            binding.rvModifiersList.visibility = View.VISIBLE

                                            if (isUpdateItem) {
                                                it.data.forEach { modifierSet ->
                                                    modifierSet.modifiers.forEach { modifier ->
                                                        item.modifiers.forEach { oldmodifier ->
                                                            if (oldmodifier.id == modifier.id && !oldmodifier._destroy) {
                                                                modifier.isChecked = true
                                                                modifier.itemQuantity =
                                                                    oldmodifier.itemQuantity
                                                                modifier.modifier_quantity =
                                                                    oldmodifier.modifier_quantity
                                                            }
                                                        }
                                                    }

                                                }

                                                if (prefProvider.getValue(
                                                        ORDER_TYPE,
                                                        ""
                                                    ) == DINE_IN
                                                ) {
                                                    it.data.forEach { modifierSet ->
                                                        modifierSet.modifiers.forEach { modifier ->
                                                            item.modifiers.forEach { oldmodifier ->

                                                                if (oldmodifier.name.lowercase() == modifier.name.lowercase()) {
                                                                    modifier.isChecked = true
                                                                    modifier.itemQuantity =
                                                                        oldmodifier.itemQuantity
                                                                    modifier.modifier_quantity =
                                                                        oldmodifier.modifier_quantity
                                                                }
                                                                /* if (oldmodifier.id == modifier.id) {

                                                                 }*/
                                                            }
                                                        }

                                                    }


                                                }
                                            }
                                            var dataMod =
                                                MethodUtils.convertSortListForModifierSet(
                                                    it1.modifier_set_ids, it.data.toCollection(
                                                        arrayListOf()
                                                    )
                                                )

                                            Log.e(
                                                TAG,
                                                "getdataModSets:  ${Gson().toJson(dataMod)}"
                                            )

                                            itemModifiersAdapter?.add(dataMod)
                                            binding.txtDone.isEnabled = true

                                        } else {
                                            binding.rvModifiersList.visibility = View.GONE
                                            binding.dividerLine2.root.visibility = View.GONE
                                            binding.txtDone.isEnabled = true
                                        }
                                    }


                                }

                            }
                        }

                        Status.ERROR -> {
                            binding.rvVariationList.visibility = View.GONE
                        }

                        Status.LOADING -> {
                            binding.rvVariationList.visibility = View.GONE
                        }
                    }


                }
            }
        }





        if (isUpdateItem) {
            qty = item.itemQuantity
            binding.edttxtQuantity.setText("" + qty)
            if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == DINE_IN && item.isFired) {
                binding.txtRemoveItem.visibility = View.GONE
            } else {
                binding.txtRemoveItem.visibility = View.VISIBLE
            }
            binding.txtDone.text = "Update"


        } else {
            binding.txtRemoveItem.visibility = View.GONE
            binding.txtAddNote.visibility = View.GONE
            binding.txtAddDiscount.visibility = View.GONE
            binding.dividerUpdate.root.visibility = View.GONE
            if (item.variationsAttributes.isNotEmpty()) {
                binding.txtPrice.text =
                    item?.variationsAttributes.get(0)?.price?.let { MethodUtils.roundOffAmount(it) }
            }
        }

    }

    private fun showPriceTitle(
        variationsAttribute: VariationsAttribute?,
        variationAdapter: VariationDashboardListAdapter?,
        data: TbItem,
        /*txtTitle: AppCompatTextView,*/
        isItemClick: Boolean
    ) {
        var variation: VariationsAttribute? = null
        if (variationAdapter != null) {
            variation = variationAdapter.getItem()
        } else if (variationsAttribute != null) {
            variation = variationsAttribute
        }



        if (variation != null) {
            variation.price?.let {
                data.price = it
            }

        } else {
            data.price = data.price
        }
        if (data.discountPrice != 0.0) {
            /*  txtTitle.text = data.name.substringBefore(" (") + "  $" + String.format(
                  "%.2f", (totalPrice(data) - data.discountPrice)
              )*/
        } else {
            /* if (isItemClick) {
                 txtTitle.text = data.name.substringBefore(" (") + "  $" + String.format(
                     "%.2f",
                     data.price
                 )
             } else
                 txtTitle.text = data.name.substringBefore(" (") + "  $" + String.format(
                     "%.2f",
                     totalPrice(data)
                 )*/
        }
    }

    private fun totalPrice(model: TbItem): Double {

        return if (model.modifiers.isNotEmpty()) {

            var totalPrice = 0.0

            val mList = model.modifiers
            mList.forEach { items ->
                totalPrice += items.price * items.itemQuantity
            }

            (model.price * model.itemQuantity) + totalPrice
        } else {

            model.price * model.itemQuantity

        }
    }

    private fun checkItemQty(
        data: TbItem,
        variationAdapter: VariationListAdapter?
    ): Boolean {

        if (data.variationsAttributes.isNotEmpty()) {

            val stockQty = variationAdapter?.getItem()?.stockQty

            return if (stockQty?.isNotEmpty() == true) {

                stockQty.toInt() >= 1

            } else {
                false
            }

        } else {

            return if (data.isManualSales) {
                true
            } else {
                data.quantity >= 1
            }
        }

        return false
    }


    private fun getServiceCharges() {

        serviceChargesObserve = Observer { it ->

            if (it.status == Status.SUCCESS) {
                if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == DINE_IN) {
                    LogUtil.logE(TAG, "getServiceCharge:  ${Gson().toJson(it.data)}")
                    serviceChargesList = ArrayList()
                    viewModel.serviceChargesList.clear()
                    it.data?.forEach { service ->
                        if (service.order_type == Constants.SERVICECHARGE_DINEIN_ORDER) {
                            serviceChargesList?.add(service)
                            viewModel.serviceChargesList.add(service)
                        }
                    }
                } else {
                    if (prefProvider.getValueboolean(SERVICECHARGE_TAKEOUT_OPENORDER, false)) {
                        LogUtil.logE(TAG, "getServiceCharge:  ${Gson().toJson(it.data)}")
                        serviceChargesList = ArrayList()
                        viewModel.serviceChargesList.clear()
                        it.data?.forEach { service ->
                            if (service.order_type == SERVICECHARGE_TAKEOUT_OPENORDER) {
                                serviceChargesList?.add(service)
                                viewModel.serviceChargesList.add(service)
                            }
                        }

                    }
                }
            }
        }
        viewModel.serviceCharges.observe(requireActivity(), serviceChargesObserve!!)
    }

    private fun calculateDiscountPercentage(originalPrice: Double, percentage: Double): Double {
        return MethodUtils.roundOffAmountDouble((originalPrice * percentage) / 100)
    }


    private fun minMaxValidationCheck(adapter: ItemModifierSetAdapter?): Boolean {

        if (adapter != null) {
            val list = adapter.getAll()
            if (list.size == 1) {
                list.forEach {
                    return (it.min_required == 0) || minLogic(
                        it.min_required,
                        it.modifiers
                    )
                }
            } else {
                list.forEach {
                    if(!minLogic(it.min_required,it.modifiers))
                        return false
                }
            }
        }
        return true
    }

    private fun minLogic(
        maxCount: Int,
        modifiers: List<Modifier>
    ): Boolean {

        if (maxCount == 0) {
            return true
        }
        var totalMinMax = 0

        modifiers.forEach {
            if (it.isChecked) {
                totalMinMax += 1
            }
        }

        return maxCount <= totalMinMax
    }

    override fun onItemClickListener(view: View?, pos: Int) {
        /*if (!MethodUtils.isDoubleClick()) {
            LogUtil.logE(TAG, "priceType:  ${variationAdapter.variationList[pos].priceType}")
            if (variationAdapter.variationList[pos].priceType == "Variable") {
                val bundle = Bundle().apply {
                    putParcelable("variationAttribute", variationAdapter.variationList[pos])
                }

                if (item.price == 0.0 && variationAdapter.variationList.isNotEmpty()) {
                    *//*   AlertUtils.showCustomAlert(
                           requireActivity(),
                           "Please enter atleast one price of item"
                       )

*//*
                } else if (!checkItemQty(item, variationAdapter)) {
                    *//* AlertUtils.showCustomAlert(
                         requireActivity(),
                         getString(R.string.qty_validation)
                     )*//*

                }


                *//*  findNavController().navigate(
                      R.id.action_dashboardCategoryNew_to_addVariablePriceDialog, bundle
                  )*//*
            } else if (variationAdapter.variationList[pos].priceType == "Fixed") {
                showPriceTitle(variationAdapter.variationList[pos], variationAdapter = null, item, true)

            }

            val variationList = ArrayList<VariationsAttribute>()
            if (variationAdapter.variationList.isNotEmpty()) {
                val variation = variationAdapter.variationList[pos]
                variationList.add(variation)
                item.name =
                    item.name.substringBefore(" (") + " (" + variation.name + ")"
                item.variationsAttributes = variationList
            }
        }
    }*/

    }

    private fun makeItemEdited(item: TbItem) {
        LogUtil.logE(TAG, "isOrderUpdateOpen:  ${Gson().toJson(viewModel.openOrderUpdate)}")
        if (viewModel.openOrderUpdate == true) {
            //for open order and edit cart
            item.isEdited = true
        } else if (isUpdateItem && prefProvider.getValueboolean(
                DINE_IN_UPDATE,
                false
            ) && item.orderItemId != null && item.orderItemId != 0
        ) {
            item.isEdited = true
        }
    }

    private fun makeItemEditedNew(item: TbCartItem) {
        LogUtil.logE(TAG, "isOrderUpdateOpen:  ${Gson().toJson(viewModel.openOrderUpdate)}")
        if (viewModel.openOrderUpdate == true) {
            //for open order and edit cart
            item.isEdited = true
        } else if (isUpdateItem && prefProvider.getValueboolean(
                DINE_IN_UPDATE,
                false
            ) && item.orderItemId != null && item.orderItemId != 0
        ) {
            item.isEdited = true
        }
    }

    private fun checkVariation(mainItem: TbItem, item: TbItem): Boolean {

        var isSame = false
        LogUtil.logE(TAG, "mainItem:  ${Gson().toJson(mainItem)}")
        LogUtil.logE(TAG, "mainItemitem:  ${Gson().toJson(item)}")

        if (mainItem.variationsAttributes.size == item.variationsAttributes.size && item.variationsAttributes.containsAll(
                mainItem.variationsAttributes
            )
        ) {
            isSame = true
        } else {
            isSame = false
        }


        return isSame
    }

    private fun checkMod(): Boolean {
        var isSame = true

        var listIds = ArrayList<Int>()
        item.modifiers.forEach {
            listIds.add(it.id ?: 0)
        }

        if (listIds.isNotEmpty() && mainModifiersId.isNotEmpty()) {
            if (mainModifiersId.containsAll(listIds) && listIds.size == mainModifiersId.size) {
                isSame = true
            } else {
                isSame = false
            }
        }

        return isSame

    }

    private fun checkVar(): Boolean {
        var isSame = true

        var listIds = ArrayList<Int>()
        item.variationsAttributes.forEach {
            listIds.add(it.id ?: 0)
        }

        if (listIds.isNotEmpty() && mainVariationId.isNotEmpty()) {
            if (mainVariationId.containsAll(listIds) && listIds.size == mainVariationId.size) {
                isSame = true
            } else {
                isSame = false
            }
        }

        return isSame

    }

    private fun checkModifier(mainItem: TbItem, item: TbItem): Boolean {


        var isSame = true

        if (mainItem.modifiers.size == item.modifiers.size && item.modifiers.containsAll(
                mainItem.modifiers
            )
        ) {
            isSame = true
        } else {
            isSame = false
        }



        return isSame


    }


    override fun onLongClickListener(modifier_id: Int?, pos: Int, itemQuantity: Int?) {
        var counter = itemQuantity!!
        binding.relativeAddItem.gone()
        binding.relativeModifierqntUpdatte.visible()
        binding.edtQntModifir.setText(itemQuantity.toString())
        binding.linearMinusQty.setOnClickListener {
            if (counter > 1) {
                counter -= 1
                binding.edtQntModifir.setText(counter.toString())
            }
        }
        binding.linearPlusQty.setOnClickListener {
            if (counter < 15) {
                counter += 1
                binding.edtQntModifir.setText(counter.toString())
            }
        }
        binding.edtQntModifir.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                binding.edtQntModifir?.isCursorVisible = true
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                if (s.toString().isNotEmpty()) {
                    counter = s.toString().toInt()
                    if (counter > 15) {
                        counter = 15
                        binding.edtQntModifir.setText("15")
                    } else if (counter == 0) {
                        binding.edtQntModifir.setText("1")
                    }
                    binding.edtQntModifir.setSelection(binding.edtQntModifir.text!!.length)
                } else {
                    counter = 1
                    binding.edtQntModifir.setSelection(binding.edtQntModifir.text!!.length)
                }

            }

        })
        binding.linearDonemodifier.setOnClickListener {
            MethodUtils.hideKeyboard(requireActivity())
            if (binding.edtQntModifir.text.toString().trim().isEmpty()) {
                counter = 1
            } else {
                counter = binding.edtQntModifir.text.toString().toInt()
            }
            if (itemModifiersAdapter?.filterList?.isNotEmpty() == true) {
                itemModifiersAdapter?.filterList?.forEachIndexed { indexset, modifierset ->
                    modifierset.modifiers.forEachIndexed { index, modifier ->
                        if (modifier.id == modifier_id) {
                            itemModifiersAdapter?.filterList!![indexset].modifiers[index].isChecked =
                                true
                            itemModifiersAdapter?.filterList!![indexset].modifiers[index].modifier_quantity =
                                counter
                            itemModifiersAdapter?.filterList!![indexset].modifiers[index].itemQuantity =
                                counter
                            itemModifiersAdapter?.notifyDataSetChanged()
                        }
                    }

                }
            }
            Log.d(TAG, "onLongClickListener: " + counter)
            binding.relativeModifierqntUpdatte.gone()
            binding.relativeAddItem.visible()

        }
        binding.linearCancelmodifier.setOnClickListener {
            MethodUtils.hideKeyboard(requireActivity())
            binding.relativeModifierqntUpdatte.gone()
            binding.relativeAddItem.visible()
        }

    }
}