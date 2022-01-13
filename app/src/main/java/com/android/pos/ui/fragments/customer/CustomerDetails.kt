package com.android.pos.ui.fragments.customer

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.entities.*
import com.android.pos.data.model.responseModel.GetOrderDetailsResponse
import com.android.pos.data.remote.Constants
import com.android.pos.databinding.FragmentCustomerDetailsBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.OrderHistoryAdapter
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.EventObserver
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.gone
import com.android.pos.utils.extensions.liveSnackBar
import com.android.pos.utils.extensions.visible
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CustomerDetails : Fragment() {

    private lateinit var binding: FragmentCustomerDetailsBinding
    lateinit var customerModel: TbCustomer
    val TAG = "CustomerDetails"

    private val viewModel by viewModels<CustomerListViewModel>()
    private val dashboardViewModel by activityViewModels<DashBoardCategoryViewModel>()

    @Inject
    lateinit var prefProvider: PrefProvider

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
        fun newInstance(model: TbCustomer): CustomerDetails {
            val args = Bundle()
            args.putParcelable(CUSTOMER_MODEL, model)
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

        //call initial api
        viewModel.getReportSummary()
    }

    private fun initControls() {
        binding.nestedScrollView.isNestedScrollingEnabled = false

        customerModel =
            requireArguments().getParcelable<TbCustomer>(
                CUSTOMER_MODEL
            )!!

        binding.model = customerModel
        binding.executePendingBindings()

        viewModel.customerId = customerModel.id.toString()

        Log.e(TAG, "CustomerDetails:  ${Gson().toJson(customerModel)}")
        binding.txtEdit.setOnClickListener {
            val bundle: Bundle = bundleOf("isEdit" to true, "dataModel" to customerModel)
            findNavController().navigate(R.id.action_customer_to_addEditCustomer, bundle)
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
            var address = ""
            var pos=0
            for (i in customerModel.addresses.indices) {
                if (customerModel.addresses[i].full_address.isNotEmpty()){
                    pos=i+1
                    address = address + "Address" + pos.toString() + " : " + customerModel.addresses[i].full_address + "\n\n"
                }

            }
            address.also {
                binding.txtAddress.text = it
            }
        }

        binding.rvOrderHistory.adapter = orderHistoryAdapter
    }

    private fun initObservers() {
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)
        viewModel.showProgress.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        })
        viewModel.orderHistory.observe(viewLifecycleOwner, EventObserver { data ->
            if (data?.isNotEmpty() == true) {
                binding.llOrderHistory.visible()
                orderHistoryAdapter.add(data)
                /*var point = 0.0

                data.forEach {

                    if (it.order_loyalty_points != null)
                        point += it.order_loyalty_points
                }

                binding.txtrewardpoint.text = "" + point*/

            } else {
                binding.llOrderHistory.gone()
            }
        })
        viewModel.orderResponse.observe(viewLifecycleOwner, EventObserver { order ->
            //reorder
            prefProvider.setValue(Constants.ORDER_TYPE, order.orderType)
            Log.e("!_@_", "customer details ${order.orderType}")
            if (order.customer != null) {
                prefProvider.setValue(
                    Constants.CUSTOMER_NAME,
                    order.customer.firstName + " " + order.customer.lastName
                )
                prefProvider.setValueInt(Constants.CUSTOMER_ID, order.customer.id)
                prefProvider.saveCustomerData(TbCustomer.customerMapping(order.customer))
            }

            dashboardViewModel.addCart(
                cartModel(order)
            )
            val bundle = Bundle()
            bundle.putBoolean("update", false)
            bundle.putBoolean("reorder", true)
            findNavController().navigate(
                R.id.action_customer_to_dashboardCategoryNew, bundle
            )
        })
    }

    private fun cartModel(order: GetOrderDetailsResponse.Data): CartModel {
        return CartModel().apply {
            terminalId = order.terminalId
            employeeID = order.employeeId
            locationId = order.locationId
            orderTypeId = order.orderTypeId
            orderType = order.orderType
            orderTypeName = order.orderType
            futureDeliveryDate = order.date
            isOpenOrder = false
            serviceCharge = serviceChargesList(order)
            customer = assignCustomer(order)
            items = inventoryList(order)
            note = order.note
            var itemDiscount = 0.0
            items?.forEach {
                itemDiscount += it.discountPrice
            }
            discountPrice = (order.totalDiscount - itemDiscount)
        }
    }

    private fun serviceChargesList(order: GetOrderDetailsResponse.Data): List<TbServiceCharge> {

        val serviceChargeList = ArrayList<TbServiceCharge>()

        order.orderServiceCharges.forEach {
            val serviceCharge = TbServiceCharge(
                it.createdAt,
                it.serviceChargeId,
                true,
                order.locationId,
                it.name,
                it.rate,
                it.updatedAt,
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
            order.customer?.lastName?.toString(),
            order.customer?.birthDate?.toString(),
            order.customer?.email?.toString(),
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

            val items = TbItem().apply {
                orderItemId = it.id
                itemId = it.itemId
                name = it.itemName
                cost = it.price
                price = it.price
                priceType = ""
                itemQuantity = it.quantity
                kitchenName = ""
                productCode = ""
                sku = ""
                isHide = false
                sort = 0
                imageUrl = ""
                thumbImageUrl = ""
                categoryId = it.categoryId
                categoryName = ""
                taxes = taxes(it.orderItemTaxes, order.locationId)
                modifier_set_ids = modifiersIds(it.orderItemModifiers)
                modifiers = modifierSets(it.orderItemModifiers)
                discountPrice = it.discountAmount
                discountType = it.discountType
                if (it.discountId != null)
                    discountId = it.discountId
                if (it.order_item_variation != null)
                    variationsAttributes = variationAtt(it.order_item_variation)
                note = it.note
            }

            inventoryModelList.add(items)

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
        locationId: Int
    ): List<TaxData>? {
        val taxList = ArrayList<TaxData>()

        taxs.forEach {
            val tax = TaxData(
                it.createdAt,
                it.taxId,
                locationId,
                it.name,
                it.rate,
                "Percentage",
                it.updatedAt,
                true,
                it.isDefault,
                false,
                "",
                listOf(),
                it.id
            )
            taxList.add(tax)
        }

        return taxList
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
}