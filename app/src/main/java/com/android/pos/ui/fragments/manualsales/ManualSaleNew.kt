package com.android.pos.ui.fragments.manualsales

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.entities.CartModel
import com.android.pos.data.entities.TbItem
import com.android.pos.data.model.CustomerListResponse
import com.android.pos.data.model.responseModel.GetServiceChargeResponse
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.ADD
import com.android.pos.data.remote.Constants.SALE_CUSTOMER_NAME
import com.android.pos.databinding.FragmentManualSaleNewBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.ManualSaleCartAdapter
import com.android.pos.utils.AmountTextWatcher
import com.android.pos.utils.SwipeHelper
import com.android.pos.utils.extensions.alert
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ManualSaleNew : Fragment() {
    private lateinit var binding: FragmentManualSaleNewBinding
    private val TAG = "ManualSaleNew"
    private var cartList: List<CartModel>? = null
    private lateinit var cartAdapter: ManualSaleCartAdapter
    private var cartItemModel = TbItem()
    private val viewModel by viewModels<ManualSaleViewModel>()
    private var serviceChargesList: List<GetServiceChargeResponse.Data>? = null

    @Inject
    lateinit var prefProvider: PrefProvider


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentManualSaleNewBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.layoutMenu.imgSearch.visibility = View.GONE
        binding.layoutMenu.autoSearch.visibility = View.GONE
        onConfig()
        onClickKeypad()
        getServiceCharge()
        getCartList()
        onClick()
        listner()
    }

    private fun addEmptyItem() {
        cartItemModel = TbItem()
        cartItemModel.apply {
            isTax = false
            name = "Custom Item"
            isManualSales = true
            itemQuantity = 1
            price = 0.00

        }
        cartAdapter.addItem(cartItemModel)

    }

    private fun getCartList() {

        viewModel.cartList.observe(requireActivity(), {
            cartList = it
            Log.e(TAG, "manualCartList  ${Gson().toJson(it)}")
            if (cartList?.isNotEmpty()!!) {
                cartAdapter.setList(cartList?.get(0)?.items)

                viewModel.itemCalculation(
                    cartList?.get(0)?.items,
                    binding.txtNoSale,
                    serviceChargesList
                )
            } else {

                cartAdapter.clearList()

            }

            if (cartAdapter.getList().isEmpty()) {
                addEmptyItem()
                Log.e(TAG, "CartListReload")
            } else if (cartAdapter.getList()
                    .get(cartAdapter.getList().size - 1).price.toString() != "0.0".toString()
            ) {
                addEmptyItem()
                Log.e(TAG, "CartListReload")
            }


        })
    }

    private fun getServiceCharge() {
        viewModel.serviceCharge.observe(requireActivity(), {
            serviceChargesList = it.data
        })
    }

    private fun listner() {
        setFragmentResultListener("request_key_customer") { requestKey: String, bundle: Bundle ->
            val result = bundle.getParcelable<CustomerListResponse.Data>("data")
            if (result != null) {
                prefProvider.setValue(
                    SALE_CUSTOMER_NAME,
                    result.first_name + " " + result.last_name
                )
                binding.txtCustomerName.text = result.first_name + " " + result.last_name
                binding.txtCrtNewCustomer.text = "Remove Customer"
            }
        }
    }

    private fun onClick() {
        binding.relAddCustomer.setOnClickListener {
            dialogMenu()
        }

        binding.txtCrtNewCustomer.setOnClickListener {
            if (prefProvider.getValue(SALE_CUSTOMER_NAME, "").toString().isNotEmpty()) {
                binding.txtCrtNewCustomer.text = "Add Customer"
                binding.txtCustomerName.text = "Add Customer"
                prefProvider.setValue(SALE_CUSTOMER_NAME, "")
            } else {
                findNavController().navigate(R.id.action_manualSaleNew_to_assignCustomerOrderFragment)


            }

        }

        binding.txtClearItems.setOnClickListener {
            alert(
                getString(R.string.app_name),
                getString(R.string.delete_items_message)
            ) {
                positiveButton(getString(R.string.tv_delete)) {
                    // Do positive stuff here
                    viewModel.deleteCart()
                    //resetCart()
                    dialogMenu()

                }
                negativeButton(R.string.tv_cancel) {
                    // Do negative stuff here
                }
            }

        }

        binding.root.setOnClickListener {
            if (binding.llCustomerDialog.visibility == View.VISIBLE) {
                binding.llCustomerDialog.visibility = View.GONE
            }

        }
    }

    private fun onClickKeypad() {
        binding.llKeypad.tvOne.setOnClickListener {
            calculateValue("1", false)

        }

        binding.llKeypad.tvTwo.setOnClickListener {

            calculateValue("2", false)
        }
        binding.llKeypad.tvThree.setOnClickListener {
            calculateValue("3", false)
        }
        binding.llKeypad.tvFour.setOnClickListener {

            calculateValue("4", false)
        }
        binding.llKeypad.tvFive.setOnClickListener {
            calculateValue("5", false)
        }
        binding.llKeypad.tvSix.setOnClickListener {

            calculateValue("6", false)
        }
        binding.llKeypad.tvSeven.setOnClickListener {

            calculateValue("7", false)
        }
        binding.llKeypad.tvEight.setOnClickListener {

            calculateValue("8", false)
        }
        binding.llKeypad.tvNine.setOnClickListener {

            calculateValue("9", false)
        }
        binding.llKeypad.tvZero.setOnClickListener {
            calculateValue("0", false)

        }
        binding.llKeypad.imgAdd.setOnClickListener {
            // binding.txtAmount.setText( "0.00")

            if (!(binding.txtAmount.text!!.trim().toString()
                    .equals("0.00")) && (!(binding.txtAmount.text!!.trim().toString()
                    .equals("$0.00"))) && (!binding.txtAmount.text!!.trim().toString().equals("0"))
            ) {
                addItemToCart(binding.txtAmount.text.toString(), true)
                binding.txtAmount.setText("0.00")
            }


        }
        binding.llKeypad.tvBack.setOnClickListener {
            if (binding.txtAmount.text.toString().isNotEmpty()) {
                calculateValue("", true)
            }

        }
    }

    private fun addItemToCart(price: String, isAdd: Boolean) {
        if (isAdd) {
            cartAdapter.getItem(cartAdapter.getList().size - 1)

            Log.e(
                TAG,
                "getAddItem:  ${Gson().toJson(cartAdapter.getItem(cartAdapter.getList().size - 1))}"
            )
            viewModel.cartLogic(cartList, cartAdapter.getItem(cartAdapter.getList().size - 1), ADD)

        } else {

            cartItemModel = TbItem()
            cartItemModel.apply {
                this.itemQuantity = 1
                this.name = "Custom Item"
                this.price = price.toDouble()
                this.isManualSales = true


            }
            cartAdapter.updateItem(cartItemModel, cartAdapter.getList().size - 1)

            /*  cartItemModel.apply {
                  isManualSales = true
                  isTax = true
                  this.name = "Tax"
                  this.price = 22.5.toDouble()
              }
              cartAdapter.updateItem(cartItemModel, cartAdapter.getList().size - 1)
  */
            // this.customerName = prefProvider.getValue(SALE_CUSTOMER_NAME, "")
            // this.itemPrice = price


        }

    }

    private fun onConfig() {
        binding.txtAmount.addTextChangedListener(AmountTextWatcher(binding.txtAmount, true))
        if (prefProvider.getValue(SALE_CUSTOMER_NAME, "").toString().isNotEmpty()) {
            binding.txtCustomerName.text = prefProvider.getValue(SALE_CUSTOMER_NAME, "")
            binding.txtCrtNewCustomer.text = "Remove Customer"
        }
        cartAdapter = ManualSaleCartAdapter()
        binding.rvSaleCart.adapter = cartAdapter

        object : SwipeHelper(activity, binding.rvSaleCart) {
            override fun instantiateUnderlayButton(
                viewHolder: RecyclerView.ViewHolder?,
                underlayButtons: MutableList<UnderlayButton?>
            ) {
                underlayButtons.add(
                    UnderlayButton(
                        "Delete",
                        0,
                        Color.parseColor("#FF3C30")
                    ) { pos ->
                        alert(
                            getString(R.string.app_name),
                            getString(R.string.delete_item_message)
                        ) {
                            positiveButton(getString(R.string.tv_delete)) {
                                // Do positive stuff here
                                val item = cartAdapter.getItem(pos)

                                Log.e(TAG, "item ${Gson().toJson(item)}")
                                viewModel.cartLogic(cartList, item, Constants.DELETE)
                            }
                            negativeButton(R.string.tv_cancel) {
                                // Do negative stuff here
                            }
                        }
                    })

            }

        }

        binding.layoutMenu.txtProducts.setTextColor(resources.getColor(R.color.txtColor))
        binding.layoutMenu.txtKeypad.setTextColor(resources.getColor(R.color.txt_color_blue))

    }

    private fun calculateValue(number: String, delete: Boolean) {

        if (delete && binding.txtAmount.text?.length!! > 1) {

            binding.txtAmount.setText(removeLastCharacter(binding.txtAmount.text.toString()))
        } else if (binding.txtAmount.text?.trim()!!.equals("0.00")) {
            binding.txtAmount.setText("")
            binding.txtAmount.append(number)
        } else {
            binding.txtAmount.append(number)

        }
        addItemToCart(binding.txtAmount.text.toString(), false)
    }

    private fun removeLastCharacter(str: String): String {
        return str.substring(0, str.length - 1)
    }

    private fun getFirstValue(str: String): String {
        return str.toString().substring(0, str.indexOf('.'))


    }

    private fun dialogMenu() {
        if (binding.llCustomerDialog.visibility == View.VISIBLE) {
            binding.llCustomerDialog.visibility = View.GONE
        } else {
            binding.llCustomerDialog.visibility = View.VISIBLE

        }

    }

    private fun resetCart() {
        cartItemModel = TbItem()
        cartItemModel.apply {
            price = 0.00
            isManualSales = true
            name = "Custom Item"
            itemQuantity = 1

        }
        cartAdapter.addItem(cartItemModel)
    }

}