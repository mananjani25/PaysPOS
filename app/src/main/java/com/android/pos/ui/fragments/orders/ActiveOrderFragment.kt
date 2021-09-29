package com.android.pos.ui.fragments.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.pos.R
import com.android.pos.data.entities.*
import com.android.pos.data.model.requestModel.OrderItemVariationAttribute
import com.android.pos.data.model.responseModel.OpenOrderResponse
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.ARG_PARAM1
import com.android.pos.databinding.FragmentActiveOrdersBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.OpenOrderAdapter
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.callback.OrderCallBack
import com.android.pos.utils.extensions.alert
import com.android.pos.utils.extensions.showAlert
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ActiveOrderFragment : Fragment(), OrderCallBack {
    private var param1: String = "Unpaid"

    private var itemPos: Int = 0
    private lateinit var binding: FragmentActiveOrdersBinding
    private val viewModel by viewModels<ActiveOrderViewModel>()
    private val dashboardViewModel by activityViewModels<DashBoardCategoryViewModel>()
    private lateinit var adapter: OpenOrderAdapter

    @Inject
    lateinit var prefProvider: PrefProvider

    companion object {
        @JvmStatic
        fun newInstance(param1: String) =
            ActiveOrderFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1).toString()
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentActiveOrdersBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapter()
        getOpenOrders()
        observeShowProgress()
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

        viewModel.data.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let { baseResponse ->


                adapter.update(itemPos)

                activity?.let {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        it, baseResponse.message
                    ) { _, _ ->
                    }
                }
            }
        })
    }

    private fun setupAdapter() {

        binding.rvOpenOrder.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayoutManager.VERTICAL
            )
        )

        adapter = OpenOrderAdapter()
        adapter.setCallback(this)
        binding.rvOpenOrder.adapter = adapter
    }

    private fun getOpenOrders() {


        viewModel.openOrders(param1).observe(viewLifecycleOwner, { it ->

            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        resource.data?.let {

                            val data = it.data.orders
                            adapter.add(data)
                        }
                    }
                    Status.ERROR -> {
                        ProgressUtils.dismissProgressDialog()
                        binding.root.showAlert(resource.message)

                    }
                    Status.LOADING -> {
                        ProgressUtils.showProgressDialog(requireActivity())
                    }
                }
            }
        })

    }

    override fun onItemClickListener(view: View?, pos: Int, status: String) {
        val order = adapter.getItem(pos)
        when (status) {
            "UPDATE" -> {
                prefProvider.setValue(Constants.ORDER_TYPE, order.orderType)


                if (order.customer != null) {
                    prefProvider.setValue(
                        Constants.CUSTOMER_NAME,
                        order.customer.firstName + " " + order.customer.lastName
                    )
                }


                dashboardViewModel.addCart(
                    cartModel(order)
                )
                val bundle = Bundle()
                bundle.putBoolean("update", true)
                bundle.putInt("orderId", order.id)
                bundle.putInt("paymentId", order.payments[0].id)
                bundle.putString("paymentOfflineId", order.payments[0].offlineId)
                bundle.putString("orderOfflineId", order.offlineId)
                findNavController().navigate(
                    R.id.action_orders_to_dashboardCategoryNew, bundle
                )

            }
            "PAY" -> {

                val cartModel = cartModel(order)

                val bundle = Bundle()
                bundle.putDouble("totalPrice", order.payments[0].amount)
                bundle.putDouble("subTotalPrice", order.payments[0].subTotal)
                bundle.putDouble("totalTax", order.payments[0].taxAmount)
                bundle.putDouble("totalDiscount", order.payments[0].totalDiscount)
                bundle.putDouble("totalServiceCharge", order.payments[0].serviceChargeAmount)
                bundle.putString("future_delivery_date", order.futureDeliveryDate)
                bundle.putString("future_delivery_time", order.futureDeliveryTime)
                bundle.putParcelable("cartList", cartModel)


                bundle.putBoolean("update", true)
                bundle.putInt("orderId", order.id)
                bundle.putInt("paymentId", order.payments[0].id)
                bundle.putString("paymentOfflineId", order.payments[0].offlineId)
                bundle.putString("orderOfflineId", order.offlineId)

                findNavController().navigate(
                    R.id.action_orders_to_paymentFragment,
                    bundle
                )

            }
            else -> {

                alert(
                    getString(R.string.app_name),
                    getString(R.string.cancel_order_message)
                ) {
                    positiveButton(getString(R.string.yes)) {

                        itemPos = pos
                        viewModel.cancelOrder(order.id)
                    }
                    negativeButton(R.string.no) {
                        // Do negative stuff here
                    }
                }
            }
        }

    }

    private fun cartModel(order: OpenOrderResponse.Data.Order): CartModel {
        return CartModel().apply {
            terminalId = order.terminalId
            employeeID = order.employeeId
            locationId = order.locationId
            orderTypeId = order.orderTypeId
            orderType = order.orderType
            orderTypeName = order.orderType
            futureDeliveryDate = order.date
            isOpenOrder = true
            serviceCharge = serviceChargesList(order)
            customer = assignCustomer(order)
            items = inventoryList(order)
            note = order.note
        }
    }

    private fun inventoryList(order: OpenOrderResponse.Data.Order): List<TbItem>? {

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
                taxes = taxes(it.orderItemTax, order.locationId)
                modifier_set_ids = modifiersIds(it.orderItemModifiers)
                modifiers = modifierSets(it.orderItemModifiers)
                discountPrice = it.discountAmount
                discountType = it.discountType
                if (it.discountId != null)
                    discountId = it.discountId
                if (it.order_item_variation != null)
                    variationsAttributes = variationAtt(it.order_item_variation)

            }

            inventoryModelList.add(items)

        }

        return inventoryModelList
    }

    private fun variationAtt(variation: OrderItemVariationAttribute): List<VariationsAttribute> {

        val variationsAttributeList = ArrayList<VariationsAttribute>()

        val variationsAttribute = VariationsAttribute()
        variationsAttribute.id = variation.variationId
        variationsAttribute.name = variation.name
        variationsAttribute.price = variation.price
        variationsAttribute.orderVariationId = variation.id
        variationsAttributeList.add(variationsAttribute)

        return variationsAttributeList
    }

    private fun modifierSets(orderItemModifiers: List<OpenOrderResponse.Data.Order.OrderItem.OrderItemModifier>): List<Modifier> {

        val modifierList = ArrayList<Modifier>()

        orderItemModifiers.forEach {

            val modifier = Modifier().apply {
                id = it.modifierId
                modifierSetId = it.modifierSetId
                name = it.name
                price = it.price
                itemQuantity = it.quantity
                orderModifierId = it.id

            }
            modifierList.add(modifier)
        }

        return modifierList
    }

    private fun modifiersIds(orderItemModifiers: List<OpenOrderResponse.Data.Order.OrderItem.OrderItemModifier>): List<Int> {

        val selectedIds = ArrayList<Int>()
        if (orderItemModifiers.isNotEmpty()) {
            orderItemModifiers.forEach {
                selectedIds.add(it.id)
            }
        }
        return selectedIds
    }

    private fun taxes(
        taxs: List<OpenOrderResponse.Data.Order.OrderItem.OrderItemTax>,
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

    private fun assignCustomer(order: OpenOrderResponse.Data.Order): TbCustomer {

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
                it.country,
                it.postcode,
                "",
                it.latitude,
                it.longitude,
                "",
                it.fullAddress,
                it.street
            )
            addressList.add(address)
        }

        return TbCustomer(
            order.customer?.id,
            order.customer?.firstName.toString(),
            order.customer?.lastName.toString(),
            order.customer?.birthDate.toString(),
            order.customer?.email.toString(),
            order.customer?.company.toString(),
            phoneList,
            addressList
        )
    }


    private fun serviceChargesList(order: OpenOrderResponse.Data.Order): List<TbServiceCharge> {

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
}