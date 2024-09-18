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
import com.pax.poslink.log.LogFilter.Const
import com.pays.pos.data.remote.Constants.UPDATE
import com.pays.pos.logger.MessageEvent
import com.pays.pos.ui.fragments.payment.PaymentViewModel
import com.pays.pos.ui.fragments.settings.notes.NoteListViewModel
import com.pays.pos.utils.Event
import com.pays.pos.utils.extensions.runOnUiThread
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import kotlinx.coroutines.*
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
    private val noteListViewModel by activityViewModels<NoteListViewModel>()
    private val TAG = "AddItemFragment"
    private lateinit var variationAdapter: VariationListAdapter
    private lateinit var modifiersAdapter: ModifiersAdapter
    private var isUpdateItem: Boolean = false

    private var itemModifiersAdapter: ItemModifierSetAdapter? = null
    private var intArray: IntArray? = null
    private var mainModifiersId: ArrayList<Int> = arrayListOf()
    private var mainVariationId: ArrayList<Int> = arrayListOf()
    private var originalModifiersList: List<Modifier> = arrayListOf()

    private val paymentViewModel by activityViewModels<PaymentViewModel>()


    @Inject
    lateinit var prefProvider: PrefProvider
    private var qty = 1
    private var itemPosition = -1

    var dineInItemQunatity = 0
    var modifiers:List<Modifier> = arrayListOf()

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
                Log.d("AddItemFragment.kt", "afterTextChanged")
                if (s.toString().isNotEmpty()) {
                    Log.d("AddItemFragment.kt", "afterTextChanged_2: ${s.toString()}")
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
                    Log.d("AddItemFragment.kt", "afterTextChanged_else")
                    try {
                        qty = binding.edttxtQuantity.text!!.toString().toInt()
                        Log.d(
                            "AddItemFragment.kt",
                            "afterTextChanged_else_qty: ${Gson().toJson(qty)}"
                        )
                    } catch (e: Exception) {
                        Log.d(
                            "AddItemFragment.kt",
                            "afterTextChanged_else_catch: ${Gson().toJson(e.printStackTrace())}"
                        )
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

    private fun imgMinusClick() {
        binding.imgMinus.setOnClickListener {
            MethodUtils.hideSoftKeyboard(requireActivity())
            if (qty == 1) {
                qty = 1
            } else {
                qty -= 1
            }
            binding.edttxtQuantity.setText("" + qty)
        }
    }

    private fun onClick() {

        if(prefProvider.getValueboolean(DINE_IN_UPDATE,false)) {
            if(item.isFired) {
                binding.imgMinus.setOnClickListener {
                    AlertUtils.showCustomAlert(requireContext(),
                        resources.getString(R.string.cant_decrease_item_quantity_dinein))
                }
            } else
                imgMinusClick()
        }else {
           imgMinusClick()
        }

        binding.imgPlus.setOnClickListener {
            MethodUtils.hideSoftKeyboard(requireActivity())
            qty += 1
            binding.edttxtQuantity.setText("" + qty)
        }

        binding.txtCancel.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {
                try {
                    Log.d("AddItemFragment.kt", "txtCancel: setOnClickListener")

                    viewModel.duplicateCurrentCartItem.forEach { duplicateCartItem ->
                        viewModel.currentCartItems.forEach { currentCartItem ->
                            if (duplicateCartItem.cartItemId == currentCartItem.cartItemId) {
                                if (duplicateCartItem.note.isNullOrEmpty()) {
                                    currentCartItem.note = ""
                                }
                            }
                            if (currentCartItem.cartItemId == item.cartItemId) {
                                item.note = ""
                            }
                        }
                    }


                } catch (e: Exception) {
                    Log.d("AddItemFragment.kt", "txtCancel: catch")
                }
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
        })

        /* binding.txtCancel.setOnClickListener {
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
         }*/

        /* OLD IMPLEMENTATION, BEFORE UPDATE SCENARIOS
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
                        *//*cartList[0].dineInList?.forEach { dineInModel ->
                            dineInModel.items.forEach { items ->
                                items.taxes?.forEach { taxData ->
                                    taxData.subTotalAmount = 0.0
                                    taxData.totalTaxTypePrice = 0.0
                                }
                            }
                        }*//*
                        viewModel.currentCartItems.forEach {
                            it.taxes?.forEach { taxData ->
                                taxData.subTotalAmount = 0.0
                                taxData.totalTaxTypePrice = 0.0
                            }
                        }

                        Log.e(TAG, "dineInListWhenUpdate:  ${Gson().toJson(cartModelsList)}")
                        *//*viewModel.newCartLogicModifier(
                            cartList,
                            item,
                            Constants.UPDATE,
                            false,
                            dineInList ?: arrayListOf()
                        )*//*
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
                            prefProvider.setValue(Constants.OLD_ITEM,Gson().toJson(viewModel.currentCartItems))

                            for(it in viewModel.currentCartItems){

                                if(it.cartItemId == item.cartItemId){
                                    Log.e("Current Cart Item","${it.cartItemId} AND ${item.cartItemId}")
                                    viewModel.currentCartItems.remove(it)
                                    break
                                }

                            }

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
//                                position = itemPosition1
//                            )
                            viewModel.doesItemContainsModifiers.value = true

                        } else {
                            LogUtil.logE("NewItem", "ItemSameNot")

                      //   if(!isUpdateItem)
                    //    viewModel.currentCartItems.remove(item)

                            for(it in viewModel.currentCartItems){

                                if(it.cartItemId == item.cartItemId){
                                    Log.e("Current Cart Item","${it.cartItemId} AND ${item.cartItemId}")
                                    viewModel.currentCartItems.remove(it)
                                    break
                                }

                            }

                            prefProvider.setValue(Constants.OLD_ITEM,Gson().toJson(viewModel.currentCartItems))


                            runBlocking {
                                viewModel.deleteCartItems()

                                viewModel.currentCartItems.forEach {
                                    viewModel.addItemToCartItems(it)
                                }
                            }




                            viewModel.doesItemContainsModifiers.value = true

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
                    }
                }

                requireActivity().supportFragmentManager.popBackStackImmediate(
                    AddItemFragment.javaClass.getName(),
                    FragmentManager.POP_BACK_STACK_INCLUSIVE
                )
                //listner.onCancelItemSelected()


            //   viewModel.fragmentNeedToBeUpdated.value = true

            }
        })
*/



      /* THIS UPDATE SCENARIO IS AFFECTED, THE ITEMS ARE NOT MERGING
       binding.txtDone.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {

                if (binding.edttxtQuantity.text.isNullOrEmpty()) {
                    binding.edttxtQuantity.setText("1")
                }
                MethodUtils.hideSoftKeyboard(requireActivity())
                *//*compare this qty from base item in preference*//*
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

                        val modifiers =
                            itemModifiersAdapter?.getSelectedModifiers() ?: arrayListOf()
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
                                            item.isItemEdited=true
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
                                            if (it.modifier_quantity!=it1.modifier_quantity){
                                                item.isItemEdited=true
                                            }
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


                for (it in viewModel.currentCartItems) {
                    if (it.cartItemId != item.cartItemId) {
                        if (it.name == item.name && it.cartItemId==item.cartItemId) {
                            if (viewModel.checkModifierNew(it, item)) {
                                Log.e(
                                    "Tracking Cart",
                                    "SAME ITEM  ${it.cartItemId} && ${item.cartItemId}"
                                )
                                found = true
                                it.itemQuantity += item.itemQuantity
                                item.isItemEdited=true
                                break
                            }
                        }
                    } else {
                        Log.e(
                            "Tracking Cart",
                            "FOUND SAME ITEM ${it.cartItemId} && ${item.cartItemId}"
                        )
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
                        *//*cartList[0].dineInList?.forEach { dineInModel ->
                            dineInModel.items.forEach { items ->
                                items.taxes?.forEach { taxData ->
                                    taxData.subTotalAmount = 0.0
                                    taxData.totalTaxTypePrice = 0.0
                                }
                            }
                        }*//*
                        viewModel.currentCartItems.forEach {
                            it.taxes?.forEach { taxData ->
                                taxData.subTotalAmount = 0.0
                                taxData.totalTaxTypePrice = 0.0
                            }
                        }

                        Log.e(TAG, "dineInListWhenUpdate:  ${Gson().toJson(cartModelsList)}")
                        *//*viewModel.newCartLogicModifier(
                            cartList,
                            item,
                            Constants.UPDATE,
                            false,
                            dineInList ?: arrayListOf()
                        )*//*
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

                            for (it in viewModel.currentCartItems) {

                                if (it.cartItemId == item.cartItemId) {
                                    Log.e(
                                        "Current Cart Item",
                                        "${it.cartItemId} AND ${item.cartItemId}"
                                    )
                                    viewModel.currentCartItems.remove(it)
                                    break
                                }

                            }

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
//                                position = itemPosition1
//                            )
                            viewModel.doesItemContainsModifiers.value = true

                        } else {
                            LogUtil.logE("NewItem", "ItemSameNot")

                            //   if(!isUpdateItem)
                            //    viewModel.currentCartItems.remove(item)

                            for (it in viewModel.currentCartItems) {

                                if (it.cartItemId == item.cartItemId) {
                                    Log.e(
                                        "Current Cart Item",
                                        "${it.cartItemId} AND ${item.cartItemId}"
                                    )
                                    viewModel.currentCartItems.remove(it)
                                    break
                                }

                            }

                            runBlocking {
                                viewModel.deleteCartItems()

                                viewModel.currentCartItems.forEach {
                                    viewModel.addItemToCartItems(it)
                                }
                            }




                            viewModel.doesItemContainsModifiers.value = true

                        }


                    }


                } else {
                    if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == Constants.DINE_IN) {
                        item.guestIndexForDineIn = viewModel.dineInHeaderPosition
                        if (prefProvider.getValueboolean(DINE_IN_UPDATE, false)) {
                            item.isItemEdited = true
                        }
                        val dineInList = cartModelsList[0].dineInList
                        Log.e(TAG, "checkCartIsEmpty  ${cartModelsList.size}")
                        LogUtil.logE(TAG, "dineInList:  ${Gson().toJson(dineInList)}")
                        if (dineInList?.isNotEmpty() == true && dineInList != null) {
                            dineInList[0].selectedPosition = viewModel.dineInHeaderPosition
                            Log.d(
                                TAG,
                                "448 dineintest currentCartItems: " + viewModel.currentCartItems
                            )
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
                    } else {
                        item.guestIndexForDineIn = null

                        Log.e("cshffasf", "checkElsee")
                        //val tbItem = TbCartItem().convertToCartItem(item, item)

                        var newFound = false

                        viewModel.currentCartItems.forEach {

                            Log.e("Tracking Cart", "Each Item ${it.name}")

                            if (it.name == item.name) {
                                Log.e("Tracking Cart", "SAME ITEM")
                                if (viewModel.checkModifierNew(it, item)) {
                                    it.itemQuantity = item.itemQuantity
                                    newFound = true
                                    item.isItemEdited=true
                                    it.isItemEdited=true
                                    return@forEach
                                }
                            }
                        }

                        *//*Added by Rahul - Update, Checking if updated or not*//*
                        if (isUpdateItem) {

                            var oldItem = Gson().fromJson<TbCartItem>(
                                prefProvider.getValue(
                                    Constants.OPEN_ORDER_ITEMS_BASE,
                                    ""
                                ), TbCartItem::class.java
                            )
                            var newItem = item

                            *//* Same item, now proceed further *//*

                            if (newItem.cartItemId == oldItem.cartItemId) {
                                if (newItem.quantity != oldItem.quantity) {
                                    newItem.isItemEdited = true
                                } else if (newItem.itemQuantity != oldItem.itemQuantity) {
                                    newItem.isItemEdited = true
                                } else if (!newItem.discountType.equals(oldItem.discountType)) {
                                    newItem.isItemEdited = true
                                } else if (newItem.discountPrice != oldItem.discountPrice) {
                                    newItem.isItemEdited = true
                                } else if (newItem.price != oldItem.price) {
                                    newItem.isItemEdited
                                } else if (newItem.itemOriginalModifiersList?.size != oldItem.itemOriginalModifiersList?.size) {
                                    newItem.isItemEdited = true
                                } else if (newItem.modifiers.size != oldItem.modifiers.size) {
                                    newItem.isItemEdited = true
                                }

                                try {
                                    *//*if (newItem.modifiers.size == oldItem.modifiers.size) {
                                        var isFound = false
                                        for (newIndex in 0 until newItem.modifiers.size) {
                                            for (oldIndex in 0 until oldItem.modifiers.size) {
//                                            if (newItem.modifiers.get(newIndex). == oldItem.modifiers.get(oldIndex))
                                            }
                                        }
                                    }*//*
                                } catch (e: Exception) {

                                }

                            }
                            item = newItem

                        }


                        Log.d("Updated_Value_NEW:", Gson().toJson(item))
                        Log.d(
                            "Updated_Value_OLD:",
                            prefProvider.getValue(Constants.OPEN_ORDER_ITEMS_BASE, "")
                        )


                        if (!newFound)
                            viewModel.updateCart(
                                viewModel.currentCartItems,
                                item,
                                Constants.ADD,
                                false
                            )
                        else {

                            //viewModel.currentCartItems.remove(item)

                            runBlocking {
                                viewModel.deleteCartItems()

                                viewModel.currentCartItems.forEach {

                                    viewModel.addItemToCartItems(it)
                                }
                            }


                        }
                        viewModel.doesItemContainsModifiers.value = true
                    }
                }

                requireActivity().supportFragmentManager.popBackStackImmediate(
                    AddItemFragment.javaClass.getName(),
                    FragmentManager.POP_BACK_STACK_INCLUSIVE
                )
                //listner.onCancelItemSelected()


                //   viewModel.fragmentNeedToBeUpdated.value = true

            }
        })
*/

        binding.txtDone.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {

                Log.d("AddItemFragment.kt", "txtDone: setOnClickListener")

                /* if (item!=null){

                     noteListViewModel.unSavedNote.value?.let {
                         if (it.isNotEmpty()) {
                             item.note = it

                             noteListViewModel.unSavedNote.postValue("")
                         }
                     }

                     try {
                         if (viewModel.noteTbCartItem.value != null) {
                             var singleItem=viewModel.noteTbCartItem.value
                             viewModel.updateCart(
                                 viewModel.currentCartItems,
                                 singleItem,
                                 UPDATE,
                                 false,
                                 isFromDetail = true
                             )

                             viewModel.noteTbCartItem.value=null
                         }
                     } catch (e: Exception) {

                     }
                 }
 */

                if (binding.edttxtQuantity.text.isNullOrEmpty()) {
                    binding.edttxtQuantity.setText("1")
                }

                Log.d("AddItemFragment.kt", "txtDone_qty: ${Gson().toJson(qty)}")

                MethodUtils.hideSoftKeyboard(requireActivity())
                val oldItemQuantity = item.itemQuantity
                item.itemQuantity = qty
                prefProvider.setValueInt(Constants.CAT_ID_SELECTED, item.categoryId)
                var isPriceNull = true
                Log.d("AddItemFragment.kt", "txtDone_item_qty: ${Gson().toJson(item.itemQuantity)}")
                if (prefProvider.getValue(ORDER_TYPE, "") == Constants.OPEN_ORDER) {
                    Log.d("AddItemFragment.kt", "txtDone_openOrder")
                    if (cartModelsList.isEmpty()) {
                        Log.d(
                            "AddItemFragment.kt",
                            "txtDone_cartModelList: ${Gson().toJson(cartModelsList)}"
                        )
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

                        Log.d("AddItemFragment.kt", "txtDone_model: ${Gson().toJson(model)}")

                    } else {
                        Log.d("AddItemFragment.kt", "txtDone_cartModelList_empty")
                    }

                } else {
                    Log.d("AddItemFragment.kt","txtDone_item_cartModelList: ${Gson().toJson(cartModelsList)}")
                   var cartModels = viewModel.createCart(cartModelsList)

                    runBlocking {
                        cartModels.forEach {
                            CoroutineScope(Dispatchers.IO).async{
                                var cart=null
                                runBlocking {
                                    viewModel.getCartModelFromID(it.cartId)
                                }
                                runBlocking {
                                    if (cart==null){
                                        viewModel.createEmptyCart(it)
                                    }
                                }
                            }.await()
                        }
                    }
                }



                if (item.modifier_set_ids.isNotEmpty() && itemModifiersAdapter != null) {
                    Log.d(
                        "AddItemFragment.kt",
                        "txtDone_if (item.modifier_set_ids.isNotEmpty() && itemModifiersAdapter != null)"
                    )
                    if (minMaxValidationCheck(itemModifiersAdapter)) {

                        val modifiers =
                            itemModifiersAdapter?.getSelectedModifiers() ?: arrayListOf()
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
                                            item.isItemEdited = true
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
                                            if (it.modifier_quantity != it1.modifier_quantity) {
                                                if (prefProvider.getValue(
                                                        Constants.OPEN_ORDER_ITEMS,
                                                        ""
                                                    ).isNotEmpty()
                                                ) {
                                                    item.isItemEdited = true
                                                }
                                            }

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

                } else {
                    Log.d("AddItemFragment.kt", "txtDone_else_2")
                }

                val variationList = ArrayList<VariationsAttribute>()
                if (item.variationsAttributes.isNotEmpty()) {
                    if (variationAdapter.variationList.isNotEmpty()) {
                        val variation = variationAdapter.getItem()
                        variationList.add(variation)
                        item.name = item.name.substringBefore(" (") + " (" + variation.name + ")"
                        item.price = variation.price ?: 0.0

                        /* with(item.variationsAttributes.get(0)){
                             if (!name.equals(variation.name) || (id!=variation.id) || (price!=variation.price)){
                                 item.isItemEdited=true
                             }
                         }
 */
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
                lateinit var foundItem:TbCartItem

                Log.d("AddItemFragment.kt", "txtDone_before_for (it in viewModel.currentCartItems)")

                //Check if item already present in Dine In
                if(prefProvider.getValue(ORDER_TYPE, TAKEOUT) == Constants.DINE_IN) {


                    for(it in viewModel.currentCartItems) {
                        if (it.guestIndexForDineIn == viewModel.dineInHeaderPosition && it.itemId == item.itemId) {
                            if (viewModel.checkModifierNew(it, item)) {
                                Log.e(
                                    "Tracking Cart",
                                    "SAME ITEM  ${it.cartItemId} && ${item.cartItemId}"
                                )
                                found = true
                                //it.itemQuantity = item.itemQuantity

                                //item.itemQuantity += qty

                                foundItem = it

                                 dineInItemQunatity =
                                     if(isUpdateItem) {

                                         Log.e("ITEM QUANTITY, ----","ITEM UPDATE -> YES QTY= $qty")

                                         qty
                                     }
                                     else{
                                         Log.e("ITEM QUANTITY, ----","ITEM UPDATE -> NO QTY= $qty -- ITEM QUNANTITY = ${it.itemQuantity} ")
                                        it.itemQuantity + qty
                                     }

                                val itemOldModifiers = viewModel.cartItemModifiersBeforeUpdate

                                if(itemOldModifiers!=null && itemOldModifiers.isNotEmpty()) {
                                    if (!it.isFired) {
                                        modifiers = item.modifiers
                                    } else {
                                        modifiers = itemOldModifiers


//                                            runOnUiThread {
//                                                AlertUtils.showCustomAlert(
//                                                    requireContext(),
//                                                    "Can't update item modifiers! Item is already fired to the kitchen !"
//                                                )
//                                            }
                                    }
                                    viewModel.cartItemModifiersBeforeUpdate = null
                                }

                                if(prefProvider.getValueboolean(DINE_IN_UPDATE, false) && it.isFired) {

                                    if(dineInItemQunatity < oldItemQuantity) {
                                        dineInItemQunatity = oldItemQuantity

                                        AlertUtils.showCustomAlert(
                                            requireContext(),
                                            resources.getString(R.string.cant_decrease_item_quantity_dinein)
                                        )
                                    }
                                }

                                break
//                                it.isItemEdited=true
                            }

                        } else {
                            Log.e(
                                "Tracking Cart",
                                "FOUND SAME ITEM ${it.cartItemId} && ${item.cartItemId}"
                            )
                        }
                    }
                } else
                {

                    //Order type other than Dine in
                    for (it in viewModel.currentCartItems) {
                        if (it.cartItemId != item.cartItemId && it.itemId == item.itemId) {
                            if (it.name == item.name && !it.isManualSaleItem) {
                                if (viewModel.checkModifierNew(it, item)) {
                                    Log.e(
                                        "Tracking Cart",
                                        "SAME ITEM  ${it.cartItemId} && ${item.cartItemId}"
                                    )
                                    found = true
                                    it.itemQuantity += item.itemQuantity
//                                it.isItemEdited=true
                                    break
                                }
                            }
                        } else {
                            Log.e(
                                "Tracking Cart",
                                "FOUND SAME ITEM ${it.cartItemId} && ${item.cartItemId}"
                            )
                        }
                    }
                }

                if (found) {
                    Log.d("AddItemFragment.kt", "txtDone_inside_found")

                    // Added to resolve Add Discount issue BIS-3547
                    viewModel.cartFooterNeedToBeUpdated = true

                    if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == Constants.DINE_IN) {

                        item.guestIndexForDineIn = viewModel.dineInHeaderPosition
                        val dineInList = cartModelsList[0].dineInList
                        dineInList?.get(0)?.headerPosition = viewModel.dineInSelectedItemHeaderPos
                        dineInList?.get(0)?.selectedPosition = viewModel.dineInSelectedItemHeaderPos
                        LogUtil.logE(TAG, "getItem  ${Gson().toJson(item)}")
                        cartModelsList[0].taxlistDynamic = arrayListOf()

                        viewModel.currentCartItems.forEach {
                            it.taxes?.forEach { taxData ->
                                taxData.subTotalAmount = 0.0
                                taxData.totalTaxTypePrice = 0.0
                            }
                        }

                        Log.e(TAG, "dineInListWhenUpdate:  ${Gson().toJson(cartModelsList)}")
                        Log.d(TAG, "398 dineintest currentCartItems: " + viewModel.currentCartItems)
                        Log.d(TAG, "dineintest item: " + item)
                        Log.d(TAG, "dineintest dineInList: " + dineInList)

                        CoroutineScope(Dispatchers.IO).launch {
                            runBlocking {
                                item.itemQuantity = dineInItemQunatity


                                viewModel.updateDineInCartItemsByIdGuestIndex(
                                    dineInItemQunatity,
                                    foundItem.cartItemId,
                                    Gson().toJson(modifiers),
                                    item.guestIndexForDineIn ?: -1
                                )
                                dineInItemQunatity = 0


                                viewModel.cartModel.let {
                                    if (it != null) {
                                        viewModel.taxBifurcationCalculationNew(
                                            item,
                                            it, "UPDATE", false
                                        )
                                    }
                                }
                                viewModel.updateCartModel(viewModel.cartModel!!)

                            }
                        }
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
                            prefProvider.setValue(
                                Constants.OLD_ITEM,
                                Gson().toJson(viewModel.currentCartItems)
                            )

                            for (it in viewModel.currentCartItems) {

                                if (it.cartItemId == item.cartItemId) {
                                    Log.e(
                                        "AddItemFragment.kt",
                                        "${it.cartItemId} AND ${item.cartItemId}"
                                    )
                                    viewModel.currentCartItems.remove(it)
                                    break
                                }

                            }
                            Log.d("AddItemFragment.kt", "txtDone_found_1")

                            runBlocking {

                                viewModel.deleteCartItems()
                                Log.d("AddItemFragment.kt", "txtDone_found_2")
                                Log.d(
                                    "AddItemFragment.kt",
                                    "txtDone_found: ${Gson().toJson(viewModel.currentCartItems)}"
                                )

                                viewModel.currentCartItems.forEach {
                                    viewModel.addItemToCartItems(it)
                                }
                            }

                            viewModel.cartModel.let {
                                if (it != null) {
                                    viewModel.taxBifurcationCalculationNew(
                                        item,
                                        it, "UPDATE", false
                                    )
                                }
                            }

                            viewModel.updateCartModel(viewModel.cartModel!!)

//                            viewModel.updateCart(
//                                viewModel.currentCartItems,
//                                item,
//                                Constants.UPDATE,
//                                false,
//                                position = itemPosition1
//                            )
                            viewModel.doesItemContainsModifiers.value = true

                        } else {
                            LogUtil.logE("NewItem", "ItemSameNot")

                            //   if(!isUpdateItem)
                            //    viewModel.currentCartItems.remove(item)

                            for (it in viewModel.currentCartItems) {

                                if (it.cartItemId == item.cartItemId) {
                                    Log.e(
                                        "Current Cart Item",
                                        "${it.cartItemId} AND ${item.cartItemId}"
                                    )
                                    viewModel.currentCartItems.remove(it)
                                    break
                                }

                            }

                            prefProvider.setValue(
                                Constants.OLD_ITEM,
                                Gson().toJson(viewModel.currentCartItems)
                            )


                            runBlocking {
                                viewModel.deleteCartItems()

                                viewModel.currentCartItems.forEach {
                                    viewModel.addItemToCartItems(it)
                                }
                            }




                            viewModel.doesItemContainsModifiers.value = true

                        }


                    }


                } else
                {
                    Log.d("AddItemFragment.kt", "txtDone_else_found")
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
                            Log.d(
                                TAG,
                                "448 dineintest currentCartItems: " + viewModel.currentCartItems
                            )
                            Log.d(TAG, "dineintest item: " + item)
                            Log.d(TAG, "dineintest dineInList: " + dineInList)
                            item.itemQuantity = qty
//                            viewModel.updateDineInCart(
//                                viewModel.currentCartItems,
//                                item,
//                                Constants.ADD,
//                                false,
//                                dineInList
//                            )

                            viewModel.addItemToCartItems(item)
                            viewModel.cartModel.let {
                                if (it != null) {
                                    viewModel.taxBifurcationCalculationNew(
                                        item,
                                        it, "ADD", false
                                    )
                                }
                            }
                            viewModel.updateCartModel(viewModel.cartModel!!)
                        }
                    } else {
                        item.guestIndexForDineIn = null

                        Log.e("cshffasf", "checkElsee")
                        //val tbItem = TbCartItem().convertToCartItem(item, item)

                        var newFound = false

                        Log.d(
                            "AddItemFragment.kt",
                            "txtDone_before_currentCartItems: ${Gson().toJson(viewModel.currentCartItems)}"
                        )

                        viewModel.currentCartItems.forEach {

                            Log.e("Tracking Cart", "Each Item ${it.name}")

                            if (it.name == item.name && it.cartItemId == item.cartItemId) {
                                Log.e("Tracking Cart", "SAME ITEM")
                                if (viewModel.checkModifierNew(it, item)) {
                                    // it.itemQuantity=item.itemQuantity
                                    newFound = true
//                                    it.isItemEdited=true
                                    return@forEach
                                }
                            }
                        }

                        Log.d(
                            "AddItemFragment.kt",
                            "txtDone_before_isUpdateItem: ${Gson().toJson(isUpdateItem)}"
                        )

                        if (isUpdateItem) {

                            var oldItem = Gson().fromJson<TbCartItem>(
                                prefProvider.getValue(
                                    Constants.OPEN_ORDER_ITEMS_BASE,
                                    ""
                                ), TbCartItem::class.java
                            )
                            var newItem = item

                            Log.d(
                                "AddItemFragment.kt",
                                "txtDone_if (prefProvider.getValue(Constants.OPEN_ORDER_ITEMS)"
                            )

                            if (prefProvider.getValue(Constants.OPEN_ORDER_ITEMS, "")
                                    .isNotEmpty()
                            ) {
                                if (newItem.cartItemId == oldItem.cartItemId) {
                                    if (newItem.quantity != oldItem.quantity) {
                                        newItem.isItemEdited = true
                                    } else if (newItem.itemQuantity != oldItem.itemQuantity) {
                                        newItem.isItemEdited = true
                                    } else if (!newItem.discountType.equals(oldItem.discountType)) {
                                        newItem.isItemEdited = true
                                    } else if (newItem.discountPrice != oldItem.discountPrice) {
                                        newItem.isItemEdited = true
                                    } else if (newItem.price != oldItem.price) {
                                        newItem.isItemEdited = true
                                    } else if (newItem.itemOriginalModifiersList?.size != oldItem.itemOriginalModifiersList?.size) {
                                        newItem.isItemEdited = true
                                    } else if (newItem.modifiers.size != oldItem.modifiers.size) {
                                        newItem.isItemEdited = true
                                    }
/*
                                try {

                                    var isFound = false
                                    for (newIndex in 0 until newItem.modifiers.size) {
                                        for (oldIndex in 0 until oldItem.modifiers.size) {
//                                            if (newItem.modifiers.get(newIndex). == oldItem.modifiers.get(oldIndex))
                                        }
                                    }
                            } catch (e: Exception) {

                            }*/

                                }
                            }

                            Log.d(
                                "AddItemFragment.kt",
                                "txtDone_before_item = newItem: ${Gson().toJson(newItem)}"
                            )

                            item = newItem

                            /*Added by Rahul and Aman to solve the quantity increment decrement issue - BIS-3874: START*/
                            var updatedIndex = -1
                            viewModel.currentCartItems.forEachIndexed { index, it ->
                                if (it.itemId == newItem.itemId && it.cartItemId == newItem.cartItemId) {
                                    updatedIndex = index
                                    return@forEachIndexed
                                }
                            }
                            if (updatedIndex != -1) {
                                viewModel.currentCartItems.set(updatedIndex, newItem)
                            }

                            /*Added by Rahul and Aman to solve the quantity increment decrement issue - BIS-3874: END*/

                        } else {
                            Log.d("AddItemFragment.kt", "txtDone_else_of_isUpdateItem")
                        }

                        Log.d(
                            "AddItemFragment.kt",
                            "txtDone_before_if (!newFound) ->: ${Gson().toJson(!newFound)}"
                        )

                        if (!newFound) {
                            Log.d(
                                "AddItemFragment.kt",
                                "txtDone_before_updateCart: ${Gson().toJson(item)}"
                            )

                            viewModel.updateCart(
                                viewModel.currentCartItems,
                                item,
                                Constants.ADD,
                                false
                            )
                            Log.d("AddItemFragment.kt", "txtDone_before_updateCart: Sent")

                        } else {

                            //viewModel.currentCartItems.remove(item)

                            runBlocking {
                                viewModel.deleteCartItems()
                                Log.d("AddItemFragment.kt", "txtDone_runBlocking_1")
                                viewModel.currentCartItems.forEach {
                                    Log.d(
                                        "AddItemFragment.kt",
                                        "txtDone_runBlocking_item: ${Gson().toJson(it)}"
                                    )
                                    viewModel.addItemToCartItems(it)
                                }
                            }


                        }
                        viewModel.doesItemContainsModifiers.value = true
                    }
                }

                requireActivity().supportFragmentManager.popBackStackImmediate(
                    AddItemFragment.javaClass.getName(),
                    FragmentManager.POP_BACK_STACK_INCLUSIVE
                )
                //listner.onCancelItemSelected()


                //   viewModel.fragmentNeedToBeUpdated.value = true

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

                try {
                    perItemDiscount =
                        MethodUtils.roundOffAmountDouble(
                            viewModel.cartModel?.discountPrice?.div(
                                totalItemswithQuantity
                            )
                        )
                } catch (_: Exception) {

                    Log.e("Tracking Discount", "Exception FOUND DISCOUNT")

                    perItemDiscount = 0.0
                }
            }

            LogUtil.logE(TAG, "totalItemswithQuantity  ${totalItemswithQuantity}")
            LogUtil.logE(TAG, "perItemDiscount  ${perItemDiscount}")
            val bundle = Bundle().apply {
                viewModel.cartModel?.discountPrice?.let { it1 -> putDouble("orderDiscount", it1) }
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
                putString("from", AddItemFragment.javaClass.name)
                putString("oldNote",item.note)
            }

            if (findNavController().currentDestination?.id == R.id.dashboardCategoryBoldPOS) {
                findNavController().navigate(
                    R.id.action_dashboardCategoryBoldPOS_to_addNoteDialog,
                    bundle

                )
            }
        }

        binding.txtRemoveItem.setOnClickListener {
            viewModel.cartFooterNeedToBeUpdated = true
            Log.e(TAG, "getDeleteItem  ${Gson().toJson(item)}")
            makeItemEditedNew(item)
            if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == DINE_IN && item.orderType == "DineIn") {
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


                    CoroutineScope(Dispatchers.IO).launch {
                        if(viewModel.currentCartItems.size == 1) {
                            viewModel.duplicateCurrentCartItem.clear()
                            viewModel.currentCartItems.clear()

                            //viewModel.lastItemRemoveFromCart.postValue(Pair(true,item.cartItemId))
                        }

                        viewModel.deleteCartItem(item.cartItemId)
                    }

                   // viewModel.updateDineInCart(viewModel.currentCartItems, item, DELETE, false, it1)
                }
            } else {
                item.guestIndexForDineIn = null

                if(viewModel.currentCartItems.size == 1) {
                    viewModel._removeLastItem.value= Event(true)

                    /*Storing the data into backup variables, these data will be used to solve BIS-3973*/
//                    ---------------------------------------
                    viewModel.backupOrderId = paymentViewModel.orderId
                    viewModel.backupPaymentId = paymentViewModel.paymentId
                    viewModel.backupOrderOfflineId = paymentViewModel.orderOfflineId
                    viewModel.backupPaymentOfflineId = paymentViewModel.paymentOfflineId
//                    ----------------------------------------

                    viewModel.currentCartItems.clear()
                    viewModel.duplicateCurrentCartItem.clear()
                    //   viewModel.deleteCartItems()
                    viewModel.deleteCart()
                    EventBus.getDefault().post(
                        MessageEvent(
                            "${Constants.LINE_BREAK_TAB} PosRepository.kt_CART_MODEL_CLEAR Thread.dumpStack(): it1 -> ${
                                Gson().toJson(Thread.currentThread().stackTrace)
                            }"
                        )
                    )
                    viewModel.fragmentNeedToBeUpdated.value = true
                } else {
                    if (true) {
                        CoroutineScope(Dispatchers.IO).launch {
                            viewModel.deleteCartItem(item.cartItemId)
                        }
                    } else
                        viewModel.updateCart(
                            viewModel.currentCartItems,
                            item,
                            DELETE,
                            item.isManualSales

                        )
                }
                //viewModel.newCartLogicModifier(cartModelsList, item, DELETE, item.isManualSales)

            }

            if (viewModel.currentCartItems.size == 1) {

                if(prefProvider.getValue(ORDER_TYPE, TAKEOUT) != DINE_IN)
                    createCart()

                //  viewModel.fragmentNeedToBeUpdated.value = true

                Log.e("Tracking Cart", "IN" + viewModel.currentCartItems.size.toString())
            } else {
                Log.e("Tracking Cart", viewModel.currentCartItems.size.toString())
            }


            requireActivity().supportFragmentManager.popBackStackImmediate(
                AddItemFragment.javaClass.getName(),
                FragmentManager.POP_BACK_STACK_INCLUSIVE
            )
            //  listner.onCancelItemSelected()


//            viewModel.fragmentNeedToBeUpdated.value = true
            viewModel.updateCartFooter.value = Event(true)

        }

        requireActivity().supportFragmentManager.setFragmentResultListener(
            "request_key_note",
            viewLifecycleOwner
        ) { requestKey: String, bundle: Bundle ->
            val note = bundle.getString("note")
            noteListViewModel.unSavedNote.postValue(note.toString())

//            item.note = note.toString()
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
                    if (!minLogic(it.min_required, it.modifiers))
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