package com.android.pos.ui.fragments.orderassign

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.android.pos.R
import com.android.pos.databinding.FragmentAssignCustomerOrderBinding
import com.android.pos.ui.adapter.AssignCustomerToOrderAdapter
import com.android.pos.ui.fragments.customer.CustomerListViewModel
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AssignCustomerOrderFragment : Fragment() {

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
        binding.rvCustomerList.adapter = adapter

    }


    private fun loadCustomerLocalList() {

        viewModel.customerList().observe(viewLifecycleOwner, {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {

                        if (resource.data != null) {

                            val data =
                                resource.data as ArrayList<com.android.pos.data.model.CustomerListResponse.Data>
                            adapter.add(data)
                        }
                        binding.rvCustomerList.visibility = View.VISIBLE
                        binding.progressCircular.visibility = View.GONE

                    }
                    Status.LOADING -> {

                        binding.rvCustomerList.visibility = View.GONE
                        binding.progressCircular.visibility = View.GONE
                    }
                    Status.ERROR -> {
                        binding.rvCustomerList.visibility = View.GONE
                        binding.progressCircular.visibility = View.VISIBLE

                    }


                }

            }


        }


        )

    }
}