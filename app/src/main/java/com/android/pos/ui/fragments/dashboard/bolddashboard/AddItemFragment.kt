package com.android.pos.ui.fragments.dashboard.bolddashboard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.entities.*
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.DELETE
import com.android.pos.data.remote.Constants.DINE_IN
import com.android.pos.data.remote.Constants.ORDER_TYPE
import com.android.pos.data.remote.Constants.TAKEOUT
import com.android.pos.data.remote.Constants.TERMINAL_ID
import com.android.pos.databinding.FragmentAddItemBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.ItemModifierSetAdapter
import com.android.pos.ui.adapter.VariationDashboardListAdapter
import com.android.pos.ui.adapter.boldpos.ModifiersAdapter
import com.android.pos.ui.adapter.boldpos.VariationListAdapter
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.callback.ItemCallback
import com.android.pos.utils.callback.ItemListner
import com.android.pos.utils.statusUtils.Resource
import com.android.pos.utils.statusUtils.Status
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AddItemFragment(val listner: ItemListner) : Fragment(), ItemCallback {
    private lateinit var item: TbItem
    private var cartList: ArrayList<CartModel> = arrayListOf()
    private lateinit var binding: FragmentAddItemBinding
    private var serviceChargesList: List<TbServiceCharge>? = null
    private var serviceChargesObserve: Observer<Resource<List<TbServiceCharge>>>? = null
    private val viewModel by activityViewModels<DashBoardCategoryViewModel>()
    private val TAG = "AddItemFragment"
    private lateinit var variationAdapter: VariationListAdapter
    private lateinit var modifiersAdapter: ModifiersAdapter
    private var isUpdateItem: Boolean = false

    private lateinit var adapter: ItemModifierSetAdapter
    private var intArray: IntArray? = null

    @Inject
    lateinit var prefProvider: PrefProvider
    private var qty = 1

    companion object {
        fun newInstance(
            item: TbItem,
            callback: ItemListner,
            cartListModel: ArrayList<CartModel>,
            isItemUpdate: Boolean
        ): AddItemFragment {
            val bundle: Bundle = Bundle()
            bundle.putParcelable("item", item)
            bundle.putBoolean(Constants.IS_UPDATE_ITEM, isItemUpdate)
            bundle.putSerializable("cartList", cartListModel)
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

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getData()
        onClick()
        getCartList()
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<VariationsAttribute>(
            Constants.DIALOG_KEY_VARIATION_DETAILS
        )
            ?.observe(viewLifecycleOwner) { it ->
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

    }

    private fun getCartList() {
        viewModel.mAllWords(
            prefProvider.getValue(ORDER_TYPE, TAKEOUT),
            prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
        ).observe(requireActivity()) {
            Log.e(TAG, "MAllWords:::  ${Gson().toJson(it)}")
            if (it.isEmpty()) {
                cartList.clear()
                cartList = arrayListOf()

            } else {
                cartList.clear()
                cartList = arrayListOf()
                cartList.addAll(it.toCollection(arrayListOf()))
            }


        }
    }


    private fun onClick() {
        binding.imgMinus.setOnClickListener {
            if (qty == 1) {
                qty = 1
            } else {
                qty -= 1
            }

            binding.txtQuantity.setText("" + qty)

        }
        binding.imgPlus.setOnClickListener {
            qty += 1
            binding.txtQuantity.setText("" + qty)
        }

        binding.txtCancel.setOnClickListener {
            if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == DINE_IN) {
                listner.onCancelItemSelected(true)
            } else {
                listner.onCancelItemSelected(false)

            }
        }

        binding.txtDone.setOnClickListener {


            item.itemQuantity = qty


            var isPriceNull = true

            /*  item.variationsAttributes.forEach {
                  if (it.price!=0.00){
                      adapter.getSelectedModifiers().forEach {
                          if (it.price!=0.00)
                          {
                              // item.price=it.price
                              isPriceNull=false
                          }
                      }
                      return@forEach
                  }
              }*/
            Log.e(
                "TAG",
                "price - ${item.price.toString() + " variatiions :  " + item.variationsAttributes + " modifiers : "}"
            )
            /*  if (item.price == 0.0 && item.variationsAttributes.isNotEmpty()*//*&&isPriceNull*//*) {
                AlertUtils.showCustomAlert(
                    requireActivity(),
                    "Please enter atleast one price of item"
                )
                return@setOnClickListener
            }*/



            if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == Constants.OPEN_ORDER) {

                if (cartList.isEmpty()) {
                    val model = CartModel()
                    model.employeeID =
                        prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
                    model.terminalId = prefProvider.getValueInt(TERMINAL_ID, 0)
                    model.orderType = prefProvider.getValue(ORDER_TYPE, TAKEOUT)
                    model.locationId = prefProvider.getValueInt(Constants.LOCATION_ID, 1)
                    model.serviceCharge = serviceChargesList
                    viewModel.ordertypelist.forEach {
                        if (it.orderType == Constants.OPEN_ORDER) {
                            model.orderTypeId = it.id
                        }
                    }
                    cartList.add(model)
                }

            } else {
                viewModel.createCart(cartList)
            }




            if (item.modifier_set_ids.isNotEmpty()) {
                if (minMaxValidationCheck(adapter)) {

                    val modifiers = adapter.getSelectedModifiers()

                    Log.e(TAG, "selectedmodifiers  ${Gson().toJson(modifiers)}")
                    Log.e(TAG, "getQuantity  ${qty}")
                    if (modifiers != null) {
                        modifiers.forEach {
                            it.itemQuantity = qty
                        }
                        item.modifiers = modifiers


                    }
                } else {
                    AlertUtils.showCustomAlert(
                        binding.root.context,
                        binding.root.context.getString(R.string.you_can_add)
                    )

                    return@setOnClickListener
                }

            }

            val variationList = ArrayList<VariationsAttribute>()
            if (item.variationsAttributes.isNotEmpty()) {
                val variation = variationAdapter.getItem()
                variationList.add(variation)
                item.name = item.name.substringBefore(" (") + " (" + variation.name + ")"
                item.variationsAttributes = variationList

            }

            if (isUpdateItem) {
                if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == Constants.DINE_IN) {
                    val dineInList = cartList[0].dineInList
                    dineInList?.get(0)?.headerPosition = viewModel.dineInSelectedItemHeaderPos
                    dineInList?.get(0)?.selectedPosition = viewModel.dineInSelectedItemHeaderPos
                    viewModel.cartLogic(
                        cartList,
                        item,
                        Constants.UPDATE,
                        false,
                        dineInList ?: arrayListOf()
                    )
                } else {

                    viewModel.cartLogic(cartList, item, Constants.UPDATE, false)
                }
            } else {

                if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == Constants.DINE_IN) {
                    val dineInList = cartList[0].dineInList
                    Log.e(TAG, "dineInList:  ${Gson().toJson(dineInList)}")
                    if (dineInList?.isNotEmpty() == true && dineInList != null) {
                        dineInList[0].selectedPosition = viewModel.dineInHeaderPosition
                        viewModel.cartLogic(cartList, item, Constants.ADD, false, dineInList)
                    }
                } else {

                    viewModel.cartLogic(cartList, item, Constants.ADD, false)
                }
            }

            listner.onCancelItemSelected()

        }

        binding.txtAddDiscount.setOnClickListener {
            var totalItemswithQuantity = 0

            if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == DINE_IN) {
                cartList.get(0).dineInList?.forEach {
                    it.items.forEach { it1 ->
                        totalItemswithQuantity += it1.itemQuantity
                    }
                }
            } else {
                cartList.get(0).items?.forEach {
                    totalItemswithQuantity += it.itemQuantity

                }
            }

            var perItemDiscount = 0.0
            if (cartList[0].discountPrice != 0.0) {
                if (totalItemswithQuantity == 0) {
                    totalItemswithQuantity = 1
                }
                perItemDiscount =
                    MethodUtils.roundOffAmountDouble(cartList[0].discountPrice / totalItemswithQuantity)
            }

            Log.e(TAG, "totalItemswithQuantity  ${totalItemswithQuantity}")
            Log.e(TAG, "perItemDiscount  ${perItemDiscount}")
            val bundle = Bundle().apply {
                putDouble("orderDiscount", cartList[0].discountPrice)
                putBoolean("isFromDetails", true)
                putParcelable("model", item)
                putDouble("itemOrderDiscount", perItemDiscount)
            }

            findNavController().navigate(
                R.id.action_dashboardCategoryBoldPOS_to_addDiscountDialog,
                bundle
            )
        }
        binding.txtAddNote.setOnClickListener {

            Log.e("HeaderPos", "${viewModel.dineInSelectedItemHeaderPos}")
            Log.e("HeaderPosdineInHea", "${viewModel.dineInHeaderPosition}")
            val bundle = Bundle().apply {
                putParcelable("item", item)
                putParcelableArrayList("cartList", cartList)
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

            makeItemEdited(item)

            if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == DINE_IN) {

                cartList[0].dineInList?.let { it1 ->
                    viewModel.cartLogic(
                        cartList, item, DELETE, false,
                        it1
                    )
                }
            } else {
                viewModel.cartLogic(cartList, item, DELETE, item.isManualSales)
            }
            listner.onCancelItemSelected()

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
        if (cartList.isEmpty()) {
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
            cartList.add(0, model)
            // viewModel.createEmptyCart(model)
            return cartList
        }

        return cartList
    }

    private fun getData() {
        item = requireArguments().getParcelable<TbItem>("item") ?: TbItem()
        cartList = requireArguments().getSerializable("cartList") as ArrayList<CartModel>
        setData()
    }

    private fun setData() {
        binding.txtItem.text = "" + item?.name
        binding.txtPrice.text = MethodUtils.roundOffAmount(item.price)
        if (view != null) {
            viewModel.getItemsbyId(item.itemId).observe(viewLifecycleOwner) {

                it?.let { resource ->
                    when (resource.status) {
                        Status.SUCCESS -> {
                            it.data?.let {
                                binding.rvVariationList.visibility = View.VISIBLE

                                intArray = IntArray(it.modifier_set_ids.size) { i ->
                                    it.modifier_set_ids[i]
                                }

                                variationAdapter.addVariations(it.variationsAttributes)
                                val variationList = ArrayList<VariationsAttribute>()
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

                                    }
                                }
                                if (intArray!!.isNotEmpty()) {
                                } else binding.dividerLine.root.visibility = View.GONE

                                if (intArray!!.isNotEmpty()) {
                                    binding.dividerLine2.root.visibility = View.VISIBLE

                                    viewModel.modifierSet(intArray!!).observe(requireActivity()) {
                                        if (it.data != null && it.data.isNotEmpty() && view != null) {
                                            adapter = ItemModifierSetAdapter(
                                                viewModel,
                                                item.itemId,
                                                viewLifecycleOwner
                                            )
                                            binding.rvModifiersList.adapter = adapter
                                            binding.rvModifiersList.visibility = View.VISIBLE
                                            it.data.forEach { modifierSet ->
                                                modifierSet.modifiers.forEach { modifier ->
                                                    item.modifiers.forEach { oldmodifier ->
                                                        if (oldmodifier.id == modifier.id) {
                                                            modifier.isChecked = true
                                                        }
                                                    }
                                                }

                                            }
                                            adapter.add(it.data)
                                            adapter.setData(item.modifiers)
                                        } else binding.rvModifiersList.visibility = View.GONE

                                    }


                                } else {
                                    binding.rvModifiersList.visibility = View.GONE
                                    binding.dividerLine2.root.visibility = View.GONE
                                }

                                variationAdapter?.showVariationPriceClick =
                                    { it: VariationsAttribute ->
                                        if (!MethodUtils.isDoubleClick()) {
                                            Log.e(TAG, "getpriceType:  ${it.priceType}")
                                            if (it.priceType == "Variable") {
                                                val bundle = Bundle().apply {
                                                    putParcelable("variationAttribute", it)
                                                }
                                                findNavController().navigate(
                                                    R.id.action_dashboardCategoryBoldPOS_to_addVariablePriceDialog,
                                                    bundle
                                                )


                                            } else if (it.priceType == "Fixed") {

                                                var variation: VariationsAttribute? = null
                                                if (variationAdapter != null) {
                                                    variation = variationAdapter.getItem()
                                                } else if (variation != null) {
                                                    variation = it
                                                }



                                                if (variation != null) {
                                                    variation.price?.let {
                                                        item.price = it
                                                    }

                                                } else {
                                                    item.price = item.price
                                                }
                                            }

//                                    if (item.variationsAttributes.isNotEmpty()) {
//                                        val resultVariationDetails =
//                                            getNavigationResultLiveData<VariationsAttribute>(
//                                                Constants.DIALOG_KEY_VARIATION_DETAILS
//                                            )
//                                        resultVariationDetails?.observe(viewLifecycleOwner) {
//                                            variationAdapter?.updateVariation(it)
//                                            var variation: VariationsAttribute? = null
//                                            if (variationAdapter != null) {
//                                                variation = variationAdapter.getItem()
//                                            } else if (it != null) {
//                                                variation = it
//                                            }
//
//
//
//
//                                            if (variation != null) {
//                                                variation?.price?.let {
//                                                    item.price = it
//                                                }
//
//                                            } else {
//                                                item.price = item.price
//                                            }
//                                        }
//                                    }
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
            binding.txtQuantity.text = "" + qty
            binding.txtRemoveItem.visibility = View.VISIBLE
            binding.txtDone.text = "Update"


        } else {
            binding.txtRemoveItem.visibility = View.GONE
            binding.txtAddNote.visibility = View.GONE
            binding.txtAddDiscount.visibility = View.GONE
            binding.dividerUpdate.root.visibility = View.GONE
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

        serviceChargesObserve = Observer {

            if (it.status == Status.SUCCESS) {
                Log.e(TAG, "getServiceCharge:  ${Gson().toJson(it.data)}")
                serviceChargesList = it.data
                viewModel.serviceChargesList = it.data ?: arrayListOf()

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

                var min_required = 0

                val mlist = ArrayList<Modifier>()

                list.forEach {
                    min_required += it.min_required
                    mlist.addAll(it.modifiers)
                }

                return (min_required == 0) || minLogic(
                    min_required,
                    mlist
                )

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
            Log.e(TAG, "priceType:  ${variationAdapter.variationList[pos].priceType}")
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
        Log.e(TAG, "isOrderUpdateOpen:  ${Gson().toJson(viewModel.openOrderUpdate)}")
        if (viewModel.openOrderUpdate == true) {
            //for open order and edit cart
            item.isEdited = true
        }
    }
}