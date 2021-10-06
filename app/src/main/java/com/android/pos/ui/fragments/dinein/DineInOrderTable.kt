package com.android.pos.ui.fragments.dinein

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.entities.CartModel
import com.android.pos.databinding.FragmentDineInOrderTableBinding
import com.android.pos.ui.adapter.DineInTableAdapter
import com.google.gson.Gson
import java.util.zip.DeflaterOutputStream

class DineInOrderTable : Fragment() {
    private lateinit var binding: FragmentDineInOrderTableBinding
    private var cartList: CartModel? = null
    private val TAG = "DineInOrderTable"
    private lateinit var dineInTableAdapter: DineInTableAdapter
    private var totalPrice: Double = 0.0
    private var totalDiscount: Double = 0.0
    private var subTotalPrice: Double = 0.0
    private var totalTax: Double = 0.0
    private var totalServiceCharge: Double = 0.0
    private var paymentAmount: Double = 0.0
    private var future_delivery_date: String = ""
    private var future_delivery_time: String = ""
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_dine_in_order_table,
            container,
            false
        )
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getData()
        dineInTableAdapter = DineInTableAdapter()
        binding.rvItemList.adapter = dineInTableAdapter


        cartList = requireArguments().getParcelable("cartList")
        totalPrice = requireArguments().getDouble("totalPrice")

        Log.e(TAG, "getDineIncartList:   ${Gson().toJson(cartList)}")

        if (cartList?.dineInList != null) {
            dineInTableAdapter.setList(cartList?.dineInList!!.toCollection(arrayListOf()))
            binding.txtTotalAmount.setText("$$totalPrice")

        }

        onClick()


    }

    private fun getData() {
        totalPrice = requireArguments().getDouble("totalPrice")
        subTotalPrice = requireArguments().getDouble("subTotalPrice")
        totalTax = requireArguments().getDouble("totalTax")
        totalServiceCharge = requireArguments().getDouble("totalServiceCharge")
        totalDiscount = requireArguments().getDouble("totalDiscount")
        future_delivery_time = requireArguments().getString("future_delivery_time").toString()
        future_delivery_date = requireArguments().getString("future_delivery_date").toString()

    }

    private fun onClick() {
        binding.btnPay.setOnClickListener {
            val bundle = Bundle()
            bundle.putDouble("totalPrice", totalPrice)
            bundle.putDouble("subTotalPrice", subTotalPrice)
            bundle.putDouble("totalTax", totalTax)
            bundle.putDouble("totalDiscount", totalDiscount)
            bundle.putDouble("totalServiceCharge", totalServiceCharge)
            bundle.putString("future_delivery_date", future_delivery_date)
            bundle.putString("future_delivery_time", future_delivery_time)
            bundle.putBoolean("update", false)

            bundle.putParcelable("cartList", cartList)

            findNavController().navigate(R.id.action_dineInOrderTable_to_paymentFragment, bundle)

        }

        binding.imgClose.setOnClickListener {
            findNavController().popBackStack()
        }
    }


}