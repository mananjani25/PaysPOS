package com.android.pos.ui.fragments.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.pos.databinding.FragmentActiveOrdersBinding
import com.android.pos.ui.adapter.OpenOrderAdapter
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.showAlert
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ActiveOrderFragment : Fragment() {

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
    }

    private fun setupAdapter() {

        binding.rvOpenOrder.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayoutManager.VERTICAL
            )
        )

        adapter = OpenOrderAdapter()
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


}