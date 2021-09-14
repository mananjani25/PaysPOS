package com.android.pos.ui.fragments.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.pos.R
import com.android.pos.data.entities.CartModel
import com.android.pos.data.entities.TbCustomer
import com.android.pos.data.entities.TbItem
import com.android.pos.data.entities.TbServiceCharge
import com.android.pos.data.model.responseModel.OpenOrderResponse
import com.android.pos.databinding.FragmentActiveOrdersBinding
import com.android.pos.ui.adapter.OpenOrderAdapter
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.callback.OrderCallBack
import com.android.pos.utils.extensions.alert
import com.android.pos.utils.extensions.showAlert
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ActiveOrderFragment : Fragment(), OrderCallBack {

    private var itemPos: Int = 0
    private lateinit var binding: FragmentActiveOrdersBinding
    private val viewModel by viewModels<ActiveOrderViewModel>()
    private lateinit var adapter: OpenOrderAdapter
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

        viewModel.openOrders.observe(viewLifecycleOwner, { it ->

            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        resource.data?.let {

                            adapter.add(it.data.orders)
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
        if (status == "UPDATE") {

            val inventoryModelList = ArrayList<TbItem>()
            CartModel().apply {
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
            }


        } else {

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

    private fun inventoryList(order: OpenOrderResponse.Data.Order): List<TbItem>? {

        val inventoryModelList = ArrayList<TbItem>()

        order.orderItems.forEach {

            val items = TbItem().apply {
                itemId = it.id
                name = it.itemName
                cost = it.price
                price = it.price
                priceType = ""
                quantity = it.quantity
                kitchenName = ""
                productCode = ""
                sku = ""
                isHide = false
                sort = 0
                imageUrl = ""
                thumbImageUrl = ""
                categoryId = it.categoryId
                categoryName = ""
//                taxes = it.taxes
//                modifier_set_ids = it.modifierIds
//                variationsAttributes = it.variations

            }

            inventoryModelList.add(items)

        }

        return inventoryModelList
    }

    private fun assignCustomer(order: OpenOrderResponse.Data.Order): TbCustomer {

        return TbCustomer(
            order.customer?.id,
            order.customer?.firstName.toString(),
            order.customer?.lastName.toString(),
            order.customer?.birthDate.toString(),
            order.customer?.email.toString(),
            order.customer?.company.toString(),
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
                isActive = false, isChecked = true,
                order_service_charge_id = it.id
            )
            serviceChargeList.add(serviceCharge)
        }

        return serviceChargeList
    }
}