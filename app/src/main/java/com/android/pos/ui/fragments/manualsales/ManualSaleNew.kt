package com.android.pos.ui.fragments.manualsales

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.entities.TbItem
import com.android.pos.data.model.CustomerListResponse
import com.android.pos.data.model.ManualSaleCartModel
import com.android.pos.data.remote.Constants.SALE_CUSTOMER_NAME
import com.android.pos.databinding.FragmentManualSaleNewBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.ManualSaleCartAdapter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ManualSaleNew : Fragment() {
    private lateinit var binding: FragmentManualSaleNewBinding
    private val TAG = "ManualSaleNew"
    private lateinit var cartAdapter: ManualSaleCartAdapter
    private var cartModel = ManualSaleCartModel()

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
        onClick()
        listner()
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
            binding.txtAmount.text = "0.00"
            addItemToCart(binding.txtAmount.text.toString(), true)


        }
        binding.llKeypad.tvBack.setOnClickListener {
            if (binding.txtAmount.text.toString().isNotEmpty()) {
                calculateValue("", true)
            }

        }
    }

    private fun addItemToCart(price: String, isAdd: Boolean) {
        if (isAdd) {


        } else {
            cartModel.apply {
                this.customerName = prefProvider.getValue(SALE_CUSTOMER_NAME, "")
                this.itemPrice = price
            }


            cartAdapter.updateItem(cartModel, cartAdapter.getList().size - 1)


        }

    }

    private fun onConfig() {
        if (prefProvider.getValue(SALE_CUSTOMER_NAME, "").toString().isNotEmpty()) {
            binding.txtCustomerName.text = prefProvider.getValue(SALE_CUSTOMER_NAME, "")
            binding.txtCrtNewCustomer.text = "Remove Customer"
        }
        cartAdapter = ManualSaleCartAdapter()
        binding.rvSaleCart.adapter = cartAdapter
        cartAdapter.addItem(cartModel)
        binding.layoutMenu.txtProducts.setTextColor(resources.getColor(R.color.txtColor))
        binding.layoutMenu.txtKeypad.setTextColor(resources.getColor(R.color.txt_color_blue))

    }

    private fun calculateValue(number: String, delete: Boolean) {

        if (delete) {
            binding.txtAmount.text = removeLastCharacter(binding.txtAmount.text.toString())
        } else if (binding.txtAmount.text.trim().equals("0.00")) {
            binding.txtAmount.text = ""
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

}