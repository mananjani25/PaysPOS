package com.android.pos.ui.fragments.customer

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.android.pos.R
import com.android.pos.data.model.CustomerDetailModel
import com.android.pos.data.model.CustomerModel
import com.android.pos.databinding.FragmentCustomerBinding
import com.android.pos.ui.activities.MainActivity
import com.android.pos.ui.adapter.CustomerListAdapter

class Customer : Fragment() {

    private lateinit var binding: FragmentCustomerBinding
    private lateinit var customerAdapter: CustomerListAdapter
    private val TAG = "Customer"
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            LayoutInflater.from(requireContext()),
            R.layout.fragment_customer,
            container,
            false
        )
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configureToolbar()
        setAdapter()
        loadFragment()
        searchQuery()
    }

    private fun searchQuery() {
        binding.autoSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                    Log.e(TAG, "${binding.autoSearch.text}")
                    customerAdapter.filter.filter(binding.autoSearch.text.trim().toString())

            }

            override fun afterTextChanged(s: Editable?) {

            }

        })

    }

    private fun loadFragment() {
        val model = CustomerDetailModel(
            0,
            "David Miller",
            "(365) 654 9879",
            "davidmiller@gmail.com",
            "Address line one\nAddress line two",
            "",
            "David Miller LTD",
            "May 7, 1990"
        )

        val frag = CustomerDetails.newInstance(model)
        val fm: FragmentManager = requireActivity().supportFragmentManager
        fm.beginTransaction().replace(binding.frameContainer.id, frag).commit()
    }

    private fun setAdapter() {
        var listCustomer: ArrayList<CustomerModel> = arrayListOf()
        listCustomer.add(
            CustomerModel(
                0,
                "DM",
                "David Miller",
                "(365) 654 9879 | davidmiller@...",
                "(365) 654 9879 | davidmiller@...",
                true
            )
        )
        listCustomer.add(
            CustomerModel(
                0,
                "KS",
                "Kareena Smith",
                "(365) 987 5648 | kareenasmit...",
                "(365) 987 5648 | kareenasmit...",
                false
            )
        )
        listCustomer.add(
            CustomerModel(
                0,
                "KM",
                "Krisha Miller",
                "(365) 897 3214 | krishamiller@...",
                "(365) 897 3214 | krishamiller@...",
                false
            )
        )
        listCustomer.add(
            CustomerModel(
                0,
                "RD",
                "Robert Doe",
                "(365) 879 6540 | robertdoe@...",
                "(365) 879 6540 | robertdoe@...",
                false
            )
        )


        customerAdapter = CustomerListAdapter(requireContext(), listCustomer, object :
            CustomerListAdapter.CustomerInteface {
            override fun onCustomerSelect(pos: Int) {


            }

        })

        customerAdapter.setList(requireContext(), listCustomer)
        binding.rvEmployeeList.adapter = customerAdapter


    }

    private fun configureToolbar() {
        binding.layoutTool.txtTitle.setText("Customers")

        binding.layoutTool.imgDrawer.setOnClickListener {
            (requireActivity() as MainActivity).enableDrawer()
        }

    }


}