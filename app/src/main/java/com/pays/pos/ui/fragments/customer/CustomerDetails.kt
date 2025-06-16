package com.pays.pos.ui.fragments.customer

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.pays.pos.R
import com.pays.pos.data.entities.CartModel
import com.pays.pos.data.entities.Modifier
import com.pays.pos.data.entities.TaxData
import com.pays.pos.data.entities.TbAddress
import com.pays.pos.data.entities.TbCustomer
import com.pays.pos.data.entities.TbItem
import com.pays.pos.data.entities.TbPhones
import com.pays.pos.data.entities.TbServiceCharge
import com.pays.pos.data.entities.VariationsAttribute
import com.pays.pos.data.model.responseModel.GetOrderDetailsResponse
import com.pays.pos.data.model.responseModel.orderhistory.Orders
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.ORDER_TYPE
import com.pays.pos.data.remote.Constants.ORDER_TYPE_ID
import com.pays.pos.data.remote.Constants.ORDER_TYPE_NAME
import com.pays.pos.databinding.FragmentCustomerDetailsBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.logger.MessageEvent
import com.pays.pos.ui.adapter.GiftCardOrderHistoryAdapter
import com.pays.pos.ui.adapter.OrderHistoryAdapter
import com.pays.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.EventObserver
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.ProgressUtils
import com.pays.pos.utils.extensions.alert
import com.pays.pos.utils.extensions.gone
import com.pays.pos.utils.extensions.invisible
import com.pays.pos.utils.extensions.liveSnackBar
import com.pays.pos.utils.extensions.visible
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus
import java.text.SimpleDateFormat
import javax.inject.Inject

@AndroidEntryPoint
class CustomerDetails : Fragment(), OrderHistoryAdapter.MyOnclickedListner {

    private var isCustomerOrderHistorySelected = true
    private lateinit var binding: FragmentCustomerDetailsBinding
    lateinit var customerModel: TbCustomer
    val TAG = "CustomerDetails"

    private val viewModel by viewModels<CustomerListViewModel>()
    private val dashboardViewModel by activityViewModels<DashBoardCategoryViewModel>()
    private val editViewModel by viewModels<AddCustomerViewModel>()

    @Inject
    lateinit var prefProvider: PrefProvider
    lateinit var listOfTbItem: List<TbItem>
    lateinit var listOfItemsId: ArrayList<Int>
    lateinit var listOfServiceCharge: ArrayList<TbServiceCharge>
    var isFromSearch: Boolean = false
    var activeTaxList: List<TaxData> = arrayListOf()

    private lateinit var giftCardOrderHistoryAdapter: GiftCardOrderHistoryAdapter

    private val orderHistoryAdapter by lazy {
        OrderHistoryAdapter { view, order ->
            //order
            order.id?.let {
                viewModel.apiCallOrderDetails(orderId = order.id)
                val bundle: Bundle = bundleOf("orderId" to "${order.id}")
                //findNavController().navigate(R.id.action_customer_to_dashboardCategoryNew, bundle)
            } ?: viewModel.showError(getString(R.string.error_order_id_not_available))

        }
    }

    companion object {
        private val CUSTOMER_MODEL = "customer_model"
        fun newInstance(model: TbCustomer,  isFromSearch: Boolean): CustomerDetails {
            val args = Bundle()
            args.putParcelable(CUSTOMER_MODEL, model)
            args.putBoolean("isFromSearch", isFromSearch)
            val fragment = CustomerDetails()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCustomerDetailsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initControls()
        initObservers()
        getTaxList()   // active taxes list
        //call initial api
        viewModel.getReportSummary(
            isFromSearch
        )
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.apply {

            txtCustomerOrderHistoryTab.setOnClickListener {
                isCustomerOrderHistorySelected = true
                updateHistoryUI()
            }

            txtGiftCardOrderHistoryTab.setOnClickListener {
                isCustomerOrderHistorySelected = false
                updateHistoryUI()
            }

        }
    }

    private fun updateHistoryUI(){
        if(isCustomerOrderHistorySelected){
            binding.apply {
                binding.txtCustomerOrderHistoryTab.background = AppCompatResources.getDrawable(requireContext(), R.drawable.background_orange_with_border)
                binding.txtGiftCardOrderHistoryTab.background = AppCompatResources.getDrawable(requireContext(), R.drawable.background_gray_with_border)
                rvOrderHistory.visible()
                layoutHeader.txtReorder.visible()
                rvGiftCardOrderHistory.gone()
                layoutHeader.txtPartType.text = requireContext().getString(R.string.pay_type)
                if (customerModel.enroll_to_loyalty == true) {
                    layoutHeader.txtLoyaltyPoints.visible()
                    layoutHeader.txtUsedLoyaltyPoints.visible()
                }else{
                    layoutHeader.txtLoyaltyPoints.gone()
                    layoutHeader.txtUsedLoyaltyPoints.gone()
                }
            }
        }else{
            binding.apply {
                binding.txtCustomerOrderHistoryTab.background = AppCompatResources.getDrawable(requireContext(), R.drawable.background_gray_with_border)
                binding.txtGiftCardOrderHistoryTab.background = AppCompatResources.getDrawable(requireContext(), R.drawable.background_orange_with_border)
                layoutHeader.txtLoyaltyPoints.gone()
                layoutHeader.txtUsedLoyaltyPoints.gone()
                layoutHeader.txtReorder.gone()
                layoutHeader.txtPartType.text = requireContext().getString(R.string.amount)
                rvOrderHistory.gone()
                rvGiftCardOrderHistory.visible()
            }
        }
    }

    private fun observerServiceCharge() {
        viewModel.serviceCharges.observe(viewLifecycleOwner) {
            if (prefProvider.getValueboolean(
                    Constants.SERVICECHARGE_TAKEOUT_OPENORDER,
                    false
                )
            ) {
                Log.e(TAG, "getServiceCharge:  ${Gson().toJson(it.data)}")
                listOfServiceCharge = ArrayList()
                it.data?.forEach { service ->
                    if (service.order_type == Constants.SERVICECHARGE_TAKEOUT_OPENORDER) {
                        listOfServiceCharge.add(service)
                    }
                }
                Log.d(
                    TAG,
                    "getServiceCharges: finall " + Gson().toJson(listOfServiceCharge)
                )

            } else {
                listOfServiceCharge = ArrayList()
            }
        }

    }

    private fun initControls() {
        binding.nestedScrollView.isNestedScrollingEnabled = false

        customerModel =
            requireArguments().getParcelable<TbCustomer>(
                CUSTOMER_MODEL
            )!!
        isFromSearch = requireArguments().getBoolean("isFromSearch")

        if (customerModel.isTokenized) {
            binding.txtAddCard?.apply {
                text = "Saved Card"
                setBackgroundColor(Color.parseColor("#4CAF50"))
                isEnabled = false

            }
            binding.imgOrderMenu?.visible()
        } else {
            binding.imgOrderMenu?.invisible()
        }

        if (customerModel.birth_date?.isNotEmpty() == true) {
            try {
                val inputFormat = SimpleDateFormat("MM/dd/yyyy")
                var date = inputFormat.parse(customerModel.birth_date)
                val outputFormat = SimpleDateFormat("MM-dd-yyyy")
                val formattedDate = outputFormat.format(date)
                binding.txtBirthDate.setText(formattedDate)
            }catch (e:Exception){
                binding.txtBirthDate.text = customerModel.birth_date
            }
        }
        binding.model = customerModel
        // binding.executePendingBindings()

        viewModel.customerId = customerModel.id.toString()

        binding.txtEdit.setOnClickListener {
            Log.d(TAG, "initControls: customerdata : " + Gson().toJson(customerModel))
            val bundle: Bundle = bundleOf("isEdit" to true, "dataModel" to customerModel)
            findNavController().navigate(R.id.action_customer_to_addEditCustomer, bundle)
        }

        binding.txtAddCard?.setOnClickListener {
            context?.let { it1 -> viewModel.makeDejavooPaymentRequest(it1, customerModel) }
        }

        binding.imgOrderMenu?.setOnClickListener {
            val popupMenu = PopupMenu(requireContext(), it)
            popupMenu.menuInflater.inflate(R.menu.edit_delete__hide_menu, popupMenu.menu)
            popupMenu.menu.findItem(R.id.menu_edit).isVisible = false
            popupMenu.menu.findItem(R.id.menu_hide).isVisible = false
            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.menu_delete -> {
                        alert(
                            getString(R.string.app_name),
                            "Are you sure, you want to delete this Customer Card?"
                        ) {
                            positiveButton(getString(R.string.tv_delete)) {
                                viewModel.submit("", false, customerModel)
                            }
                            negativeButton(R.string.tv_cancel) {
                                // Do negative stuff here
                            }
                        }
                    }
                }
                true
            }
            popupMenu?.show()
        }


        if (customerModel.enroll_to_loyalty == true) {
            binding.linearRewardLayout.visibility = View.VISIBLE
            binding.layoutHeader.txtLoyaltyPoints.visibility = View.VISIBLE
            binding.layoutHeader.txtUsedLoyaltyPoints.visibility = View.VISIBLE
            binding.txtrewardpoint.text = customerModel.final_reward.toString()
            orderHistoryAdapter.finalreward = customerModel.final_reward.toString()
            orderHistoryAdapter.enrolltrueloyalty = true
        } else {
            binding.layoutHeader.txtLoyaltyPoints.visibility = View.GONE
            binding.layoutHeader.txtUsedLoyaltyPoints.visibility = View.GONE
            binding.linearRewardLayout.visibility = View.GONE
            orderHistoryAdapter.enrolltrueloyalty = false
        }


        if (customerModel.phones.isNotEmpty()) {
            binding.txtPhoneNo.text =
                "${AlertUtils.usNumberFormat(customerModel.phones[0].phone_number)}"
        }
        if (customerModel.addresses.isNotEmpty()) {
            var address: StringBuffer = StringBuffer()


            if (customerModel.addresses.size == 1) {
                if (customerModel.addresses[0].type_of_address == "Shipping") {
                    address.append("Delivery" + " : " + customerModel.addresses[0].full_address)
                }
            } else if (customerModel.addresses.size == 2) {
                if (customerModel.addresses[0].type_of_address == "Shipping") {
                    address.append("Delivery" + " : " + customerModel.addresses[0].full_address + "\n")
                    //address.append(customerModel.addresses[1].type_of_address + " : " + customerModel.addresses[1].full_address)
                } else {
                    address.append("Delivery" + " : " + customerModel.addresses[1].full_address + "\n")
                    //address.append(customerModel.addresses[0].type_of_address + " : " + customerModel.addresses[0].full_address)
                }
            }
            address.also {
                binding.txtAddress.text = it
            }
        }

        //Setup Order History List
        binding.rvOrderHistory.adapter = orderHistoryAdapter
        orderHistoryAdapter.setListner(this@CustomerDetails)
        orderHistoryAdapter.setPrefrenceData(PrefProvider(requireContext()))

        //Setup Gift Card Order History List
        giftCardOrderHistoryAdapter = GiftCardOrderHistoryAdapter()
        binding.rvGiftCardOrderHistory.adapter = giftCardOrderHistoryAdapter
        giftCardOrderHistoryAdapter.setPrefrenceData(PrefProvider(requireContext()))

    }

    private fun initObservers() {
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)
        updateHistoryUI()

        viewModel.itemlist.observe(viewLifecycleOwner) { itemlist ->
            if (itemlist.data?.isNotEmpty() == true) {
                listOfTbItem = itemlist.data as List<TbItem>
                listOfItemsId = arrayListOf()
                itemlist.data.forEach { it ->
                    listOfItemsId.add(it.itemId)
                }
                listOfItemsId.add(1)
            }
        }
        viewModel.showProgress.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        }

        viewModel.token.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    binding.txtAddCard?.apply {
                        text = context.getString(R.string.saved_card)
                        setBackgroundColor(Color.parseColor("#4CAF50"))
                        isEnabled = false
                    }
                    binding.imgOrderMenu?.visible()
                } else {
                    binding.txtAddCard?.apply {
                        text = context.getString(R.string.add_card)
                        setBackgroundColor(ContextCompat.getColor(context, R.color.txt_color_blue))
                        isEnabled = true
                    }
                    binding.imgOrderMenu?.invisible()
                }
            }
        }

        viewModel.orderHistory.observe(viewLifecycleOwner, EventObserver { data ->
            if (data?.isNotEmpty() == true) {
                observerServiceCharge()
                binding.txtCustomerOrderHistoryTab.visible()
                binding.rvOrderHistory.visible()
                binding.layoutHeader.root.visible()
                orderHistoryAdapter.add(data)
            } else {
                binding.txtCustomerOrderHistoryTab.gone()
                binding.rvOrderHistory.gone()
            }
        })
        viewModel.giftCardOrderHistory.observe(viewLifecycleOwner, EventObserver { data ->
            if (data?.isNotEmpty() == true) {
                observerServiceCharge()
                binding.txtGiftCardOrderHistoryTab.visible()
                if(!isCustomerOrderHistorySelected){//for hiding it on the first time
                    binding.rvGiftCardOrderHistory.visible()
                }
                binding.layoutHeader.root.visible()
                giftCardOrderHistoryAdapter.add(data)
            } else {
                binding.txtGiftCardOrderHistoryTab.gone()
                binding.rvGiftCardOrderHistory.visible()
            }
        })
        viewModel.orderResponse.observe(viewLifecycleOwner, EventObserver { order ->
            //reorder
            try {
                //prefProvider.setValue(Constants.ORDER_TYPE, Constants.TAKEOUT)
                LogUtil.logE("!_@_", "customer details ${order.orderType}")
                prefProvider.setValue(ORDER_TYPE, order.orderType)
                prefProvider.setValueInt(ORDER_TYPE_ID, order.orderTypeId)
                prefProvider.setValue(ORDER_TYPE_NAME, order.orderTypeName)
                if (order.orderItems.size == 1) {
                    if (listOfItemsId.contains(order.orderItems[0].itemId)) {
                        if (order.customer != null) {
                            prefProvider.setValue(
                                Constants.CUSTOMER_NAME,
                                order.customer.firstName + " " + order.customer.lastName
                            )

                            prefProvider.setValue(
                                Constants.RECEIPT_CUSTOMER_NAME,
                                order.customer.firstName + " " + order.customer.lastName
                            )
                            prefProvider.setValueInt(Constants.CUSTOMER_ID, order.customer.id)
                            prefProvider.saveCustomerData(TbCustomer.customerMapping(order.customer))

                        }

                        Log.e(TAG, "getOrderReOrder  ${Gson().toJson(order)}")
                        dashboardViewModel.addCart(
                            cartModel(order)
                        )
                        val bundle = Bundle()
                        bundle.putBoolean("update", false)
                        bundle.putBoolean("reorder", true)
                        findNavController().navigate(
                            R.id.action_customer_to_dashboardCategoryNew, bundle
                        )
                    } else {
                        AlertUtils.showCustomAlertWithListenerWithOK(
                            requireActivity(), "Not Available Item in a Restaurant."
                        ) { _, _ ->

                        }
                    }
                } else {
                    if (order.customer != null) {
                        prefProvider.setValue(
                            Constants.CUSTOMER_NAME,
                            order.customer.firstName + " " + order.customer.lastName
                        )

                        prefProvider.setValue(
                            Constants.RECEIPT_CUSTOMER_NAME,
                            order.customer.firstName + " " + order.customer.lastName
                        )
                        prefProvider.setValueInt(Constants.CUSTOMER_ID, order.customer.id)
                        prefProvider.saveCustomerData(TbCustomer.customerMapping(order.customer))
                    }

                    Log.e(TAG, "getOrderReOrder7  ${Gson().toJson(order)}")
                    dashboardViewModel.addCart(
                        cartModel(order)
                    )
                    val bundle = Bundle()
                    bundle.putBoolean("update", false)
                    bundle.putBoolean("reorder", true)
                    findNavController().navigate(
                        R.id.action_customer_to_dashboardCategoryNew, bundle
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        })
    }

    private fun cartModel(order: GetOrderDetailsResponse.Data): CartModel {
        return CartModel().apply {
            terminalId = prefProvider.getValueInt(Constants.TERMINAL_ID, 0)
            employeeID = prefProvider.employeeId()
            locationId = order.locationId
            orderTypeId = order.orderTypeId
            orderType = order.orderType
            orderTypeName = order.orderTypeName
            futureDeliveryDate = order.date.toString()
            isOpenOrder = false
            serviceCharge = listOfServiceCharge
            customer = assignCustomer(order)
            items = inventoryList(order)
            note = order.note ?: ""
            reorder = true
            var itemDiscount = 0.0
            items?.forEach {
                itemDiscount += it.discountPrice * it.itemQuantity
            }
            discountPrice += (order.totalDiscount - itemDiscount)
            taxlistDynamic = getTaxBirfucationList(inventoryList(order)!!)
        }
    }

    private fun getTaxBirfucationList(orderItems: List<TbItem>): ArrayList<TaxData> {
        var taxListDynamic: ArrayList<TaxData> = arrayListOf()
        if (orderItems.isNotEmpty()) {
            orderItems.forEach { orderItem ->
                var totalPrice =
                    (orderItem.price * orderItem.itemQuantity) - orderItem.discountPrice
                orderItem.modifiers.forEach { orderItemModifier ->
                    totalPrice += orderItemModifier.price * orderItemModifier.itemQuantity
                }
                Log.d(TAG, "navigate: itemPrice : $totalPrice")
                var totaltaxtemp = 0.0
                orderItem.taxes?.forEach { orderItemTaxe ->
                    if (taxListDynamic?.isNotEmpty() == true) {
                        var found = -1
                        taxListDynamic.forEachIndexed { index, taxData ->
                            if (taxData.id == orderItemTaxe.id) {
                                found = index
                                return@forEachIndexed
                            }
                        }
                        if (found == -1) {
                            var taxData: TaxData = TaxData(
                                orderItemTaxe.createdAt,
                                orderItemTaxe.id,
                                0,
                                orderItemTaxe.name,
                                orderItemTaxe.rate,
                                orderItemTaxe.taxType,
                                orderItemTaxe.updatedAt,
                                true,
                                orderItemTaxe.isDefault,
                                false,
                                "",
                                orderItemTaxe.itemIds,
                                orderItemTaxe.orderTaxId,
                                false,
                                getTaxFromTotalPrice(
                                    orderItemTaxe,
                                    totalPrice,
                                    orderItem
                                ),
                                totalPrice
                            )
                            taxListDynamic?.add(taxData)
                        } else {
                            taxListDynamic!![found].totalTaxTypePrice =
                                taxListDynamic!![found].totalTaxTypePrice + getTaxFromTotalPrice(
                                    orderItemTaxe,
                                    totalPrice,
                                    orderItem
                                )
                            taxListDynamic!![found].subTotalAmount =
                                taxListDynamic!![found].subTotalAmount + totalPrice
                        }
                        Log.d(TAG, "found : " + found)
                    } else {
                        var taxData: TaxData = TaxData(
                            orderItemTaxe.createdAt,
                            orderItemTaxe.id,
                            0,
                            orderItemTaxe.name,
                            orderItemTaxe.rate,
                            orderItemTaxe.taxType,
                            orderItemTaxe.updatedAt,
                            true,
                            orderItemTaxe.isDefault,
                            false,
                            "",
                            orderItemTaxe.itemIds,
                            orderItemTaxe.orderTaxId,
                            false,
                            getTaxFromTotalPrice(
                                orderItemTaxe,
                                totalPrice,
                                orderItem
                            ),
                            totalPrice
                        )
                        taxListDynamic.add(taxData)
                    }


                    Log.d(TAG, "navigate: " + totaltaxtemp)
                }

            }

            Log.d(TAG, "navigate: list " + Gson().toJson(taxListDynamic))
        }
        return taxListDynamic
    }


    fun getTaxFromTotalPrice(
        orderItemTaxe: TaxData,
        totalPrice: Double,
        item: TbItem
    ): Double {
        var totaltaxtemp = 0.0


        totaltaxtemp += if (orderItemTaxe.taxType == "Percentage") {
            if (totalPrice < 0.0) {

                String.format("%.2f", 0.00)
                    .toDouble()
            } else {
                val itemTaxPrice =
                    (orderItemTaxe.rate * totalPrice) / 100
                Log.e("itemTaxPrice", "" + itemTaxPrice)
                itemTaxPrice
            }

        } else {
            Log.d("yash", "taxCalculation: " + orderItemTaxe.taxType)
            if (totalPrice <= 0.0) {
                String.format("%.2f", 0.00)
                    .toDouble()
            } else {
                String.format("%.2f", orderItemTaxe.rate * item.itemQuantity)
                    .toDouble()
            }
        }
        return totaltaxtemp
    }

    private fun serviceChargesList(order: GetOrderDetailsResponse.Data): List<TbServiceCharge> {

        val serviceChargeList = ArrayList<TbServiceCharge>()

        order.orderServiceCharges.forEach {
            val serviceCharge = TbServiceCharge(
                it.createdAt.toString(),
                it.serviceChargeId,
                true,
                order.locationId,
                it.max_guest_count,
                it.min_guest_count,
                it.name,
                it.order_type,
                it.rate,
                it.updatedAt.toString(),
                isActive = false,
                isChecked = true,
                order_service_charge_id = it.id
            )
            serviceChargeList.add(serviceCharge)
        }

        return serviceChargeList
    }

    private fun assignCustomer(order: GetOrderDetailsResponse.Data): TbCustomer {

        val phoneList = ArrayList<TbPhones>()
        order.customer?.phones?.forEach {
            val phone = TbPhones(it.id, it.phoneNumber)
            phoneList.add(phone)
        }

        val addressList = ArrayList<TbAddress>()
        order.customer?.addresses?.forEach {
            val address = TbAddress(
                it.id,
                it.address1,
                it.address2,
                it.city,
                it.state,
                it.country ?: "",
                it.postcode ?: "",
                "",
                it.latitude ?: "",
                it.longitude ?: "",
                "",
                it.fullAddress,
                it.street
            )
            addressList.add(address)
        }

        return TbCustomer(
            order.customer?.id,
            order.customer?.firstName?.toString(),
            order.customer?.lastName.toString(),
            order.customer?.birthDate?.toString(),
            order.customer?.email?.toString(),
            false,
            false,
            0,
            order.customer?.company?.toString(),
            phoneList,
            addressList
        )
    }

    private fun inventoryList(order: GetOrderDetailsResponse.Data): List<TbItem>? {

        val inventoryModelList = ArrayList<TbItem>()

        order.orderItems.forEach {
            if (listOfItemsId.contains(it.itemId)) {
                val items = TbItem().apply {
                    orderItemId = it.id
                    id = it.custom_item_id
                    itemId = it.itemId
                    name = it.itemName
                    price = it.price
                    itemQuantity = it.quantity
                    sku = ""
                    isHide = false
                    sort = 0
                    imageUrl = ""
                    thumbImageUrl = ""
                    reorder = true
                    categoryId = it.categoryId
                    categoryName = ""
                    taxes = taxes(it.orderItemTaxes, order.locationId, it.itemId)
                    modifier_set_ids = modifiersIds(it.orderItemModifiers)
                    modifiers = modifierSets(it.orderItemModifiers)
                    discountPrice = (it.discountAmount / it.quantity)
                    discountType = it.discountType.toString()
                    if (it.discountId != null)
                        discountId = it.discountId
                    if (it.order_item_variation != null)
                        variationsAttributes = variationAtt(it.order_item_variation)
                    note = it.note
                }

                try {
                    inventoryModelList.add(items)
                } catch (e: Exception) {
                    Log.d(TAG, "inventoryList: " + e.printStackTrace())
                }
            }
        }

        return inventoryModelList
    }

    private fun variationAtt(variation: GetOrderDetailsResponse.Data.OrderItem.OrderItemVariationAttribute?): List<VariationsAttribute> {

        val variationsAttributeList = ArrayList<VariationsAttribute>()

        val variationsAttribute = VariationsAttribute()
        variationsAttribute.id = variation?.variationId
        variationsAttribute.name = variation?.name ?: ""
        variationsAttribute.price = variation?.price
        variationsAttribute.orderVariationId = variation?.id
        variationsAttributeList.add(variationsAttribute)

        return variationsAttributeList
    }

    private fun taxes(
        taxs: List<GetOrderDetailsResponse.Data.OrderItem.OrderItemTaxe>,
        locationId: Int,
        itemIdexist: Int
    ): List<TaxData>? {
        listOfTbItem.forEach { activeItems ->
            if (activeItems.itemId == itemIdexist) {
                var taxactive: ArrayList<TaxData> = arrayListOf()
                activeItems.taxes?.forEach { taxData ->
                    if (taxData.isActive) {
                        taxData.locationId = locationId
                        taxactive.add(taxData)
                    }
                }
                return taxactive.toList()
            } else if (itemIdexist == 1) {
                // if manual item > apply all active taxes
                var taxactive: ArrayList<TaxData> = arrayListOf()
                activeTaxList.forEach { taxData ->
                    taxData.locationId = locationId
                    taxactive.add(taxData)
                }
                return taxactive.toList()
            }
        }
        return emptyList()
    }

    private fun modifierSets(orderItemModifiers: List<GetOrderDetailsResponse.Data.OrderItem.OrderItemModifier>): List<Modifier> {

        val modifierList = ArrayList<Modifier>()

        orderItemModifiers.forEach {

            val modifier = Modifier().apply {
                id = it.modifierId?.toInt() ?: 0
                //modifierSetId = it.modifierSetId
                name = it.name
                price = it.price
                itemQuantity = it.quantity
                orderModifierId = it.id
                modifier_quantity = it.modifier_quantity!!

            }
            modifierList.add(modifier)
        }

        return modifierList
    }

    private fun modifiersIds(orderItemModifiers: List<GetOrderDetailsResponse.Data.OrderItem.OrderItemModifier>): List<Int> {

        val selectedIds = ArrayList<Int>()
        if (orderItemModifiers.isNotEmpty()) {
            orderItemModifiers.forEach {
                selectedIds.add(it.id)
            }
        }
        return selectedIds
    }

    override fun onclickedReorder(order: Orders) {
        try {
            order.id?.let {
                viewModel.deleteCart()
                EventBus.getDefault().post(MessageEvent("${Constants.LINE_BREAK_TAB} PosRepository.kt_CART_MODEL_CLEAR Thread.dumpStack(): it1 -> ${Gson().toJson(Thread.currentThread().stackTrace)}"))
                viewModel.apiCallOrderDetails(orderId = order.id)
            } ?: viewModel.showError(getString(R.string.error_order_id_not_available))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // fetch all active taxes list from database
    private fun getTaxList() {
        viewModel.enableTaxes.observe(viewLifecycleOwner) {
            if (it.data != null)
                activeTaxList = it.data
        }
    }
}