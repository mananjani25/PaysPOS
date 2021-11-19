package com.android.pos.ui.fragments.orderassign

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.pos.R
import com.android.pos.data.entities.TbCustomer
import com.android.pos.databinding.FragmentAssignCustomerOrderBinding
import com.android.pos.ui.adapter.AssignCustomerToOrderAdapter
import com.android.pos.ui.fragments.customer.CustomerListViewModel
import com.android.pos.utils.callback.ItemCallback
import com.android.pos.utils.callback.PaginationScrollListener
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AssignCustomerOrderFragment : Fragment(), ItemCallback {
    private val TAG = "AssignCustomerOrderFr"

    companion object {
        fun newInstance() = AssignCustomerOrderFragment()
    }

    private lateinit var binding: FragmentAssignCustomerOrderBinding
    private val viewModel by viewModels<CustomerListViewModel>()
    private lateinit var adapter: AssignCustomerToOrderAdapter
    private var isFromDineIn: Boolean? = false
    private var dineInPosition: Int? = null


    private var currentpage = 1
    private val perpagedata = 50
    private var isLoading = false
    private var isLastPage = false
    private var firstDetailLoad = false
    val data = LinkedHashMap<String, String>()
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

        data["page"] = currentpage.toString()
        data["per_page"] = perpagedata.toString()


        setupUI()

        loadCustomerLocalList(currentpage)
        isFromDineIn = arguments?.getBoolean("DINE_IN", false)
        dineInPosition = arguments?.getInt("position")
        return binding.root
    }

    private fun setupUI() {

        adapter = AssignCustomerToOrderAdapter()
        adapter.setCallback(this)
        binding.rvCustomerList.adapter = adapter
        val layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.rvCustomerList.layoutManager = layoutManager

        binding.rvCustomerList.addOnScrollListener(object :
            PaginationScrollListener(layoutManager) {


            override fun isLastPage(): Boolean {
                return isLastPage
            }

            override fun isLoading(): Boolean {
                return isLoading
            }

            override fun getTotalPageCount(): Int {
                return 0
            }


            override fun loadMoreItems() {
                if (adapter.itemCount >= 50) {
                    isLoading = true
                    currentpage += 1
                    loadCustomerLocalList(currentpage)
                }

            }

        })


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


    private fun loadCustomerLocalList(currentpage: Int) {
        data["page"] = currentpage.toString()
        data["per_page"] = perpagedata.toString()
        viewModel.customerList(data).observe(viewLifecycleOwner, {
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
            putBoolean("OPEN_ORDER", false)
            isFromDineIn?.let { putBoolean("DINE_IN", it) }
            dineInPosition?.let {
                Log.e(TAG, "position:  $it")
                putInt("position", it)
            }
        }

        if (isFromDineIn == true) {
            setFragmentResult("request_key_customer_dine_in", result)
        } else {
            setFragmentResult("request_key_customer", result)
        }

        findNavController().navigateUp()
    }
}