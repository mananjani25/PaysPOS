package com.android.pos.ui.fragments.orderassign

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.entities.TbCustomer
import com.android.pos.databinding.FragmentAssignCustomerOrderBinding
import com.android.pos.ui.adapter.AssignCustomerToOrderAdapter
import com.android.pos.ui.fragments.customer.CustomerListViewModel
import com.android.pos.utils.callback.ItemCallback
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AssignCustomerOrderFragment : Fragment(), ItemCallback {

    companion object {
        fun newInstance() = AssignCustomerOrderFragment()
    }

    private lateinit var binding: FragmentAssignCustomerOrderBinding
    private val viewModel by viewModels<CustomerListViewModel>()
    private lateinit var adapter: AssignCustomerToOrderAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            LayoutInflater.from(requireContext()),
            R.layout.fragment_assign_customer_order,
            container,
            false
        )

        setupUI()
        loadCustomerLocalList()
        return binding.root
    }

    private fun setupUI() {

        adapter = AssignCustomerToOrderAdapter()
        adapter.setCallback(this)
        binding.rvCustomerList.adapter = adapter


        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun afterTextChanged(s: Editable) {

                adapter.filter.filter(s.toString().trim())

            }
        })

        binding.imgBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.txtCreateCustomer.setOnClickListener {
            findNavController().navigate(R.id.action_assignCustomerOrderFragment_to_addEditCustomer)
        }

    }


    private fun loadCustomerLocalList() {

        viewModel.customerList().observe(viewLifecycleOwner, {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {

                        if (resource.data != null) {

                            val data =
                                resource.data as ArrayList<TbCustomer>
                            adapter.add(data)
                        }
                        binding.rvCustomerList.visibility = View.VISIBLE
                        binding.progressCircular.visibility = View.GONE

                    }
                    Status.LOADING -> {

                        binding.rvCustomerList.visibility = View.GONE
                        binding.progressCircular.visibility = View.VISIBLE
                    }
                    Status.ERROR -> {
                        binding.rvCustomerList.visibility = View.GONE
                        binding.progressCircular.visibility = View.GONE

                    }


                }

            }


        }


        )

    }

    override fun onItemClickListener(view: View?, pos: Int) {

        val customer = adapter.getItem(pos)

        val result = Bundle().apply {
            putParcelable("data", customer)
        }
        setFragmentResult("request_key_customer", result)

        findNavController().navigateUp()
    }
}