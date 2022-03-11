package com.android.pos.ui.fragments.customer

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.entities.TbCustomer
import com.android.pos.data.remote.Constants.CUSTOMERDETAILS
import com.android.pos.data.remote.Constants.KEY
import com.android.pos.databinding.FragmentCustomerBinding
import com.android.pos.ui.activities.MainActivity
import com.android.pos.ui.adapter.CustomerListAdapter
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.SwipeHelper
import com.android.pos.utils.callback.PaginationScrollListener
import com.android.pos.utils.extensions.alert
import com.android.pos.utils.extensions.showAlert
import com.android.pos.utils.statusUtils.Status
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class Customer : Fragment() {

    private lateinit var binding: FragmentCustomerBinding
    private lateinit var customerAdapter: CustomerListAdapter
    private val viewModel by viewModels<CustomerListViewModel>()
    private val dynamicCustomerList: ArrayList<TbCustomer> =
        arrayListOf()
    private var dialog: Dialog? = null
    private val TAG = "Customer"
    private var currentpage = 1
    private val perpagedata = 50
    private var isLoading = false
    private var isLastPage = false
    private var firstDetailLoad = false
    private var deletedPos: Int? = null

    val data = LinkedHashMap<String, String>()
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
        dialog = Dialog(requireContext(), android.R.style.Theme_Light)
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)

        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        dialog?.setContentView(R.layout.dialog_customer)

        binding.lifecycleOwner = this


        // loadCustomerList()
        setUpRecyclerView()
        observeCustomerDelete()
        loadCustomerLocalList(currentpage)

        val layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.rvEmployeeList.layoutManager = layoutManager

        binding.rvEmployeeList.addOnScrollListener(object :
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
                if (dynamicCustomerList.size >= perpagedata) {
                    isLoading = true
                    currentpage += 1
                    loadCustomerLocalList(currentpage)
                }
            }

        })
        viewModel._customerNoDataFound.observe(viewLifecycleOwner) { event ->
            binding.rvEmployeeList.visibility = View.GONE
            binding.noCustomerDats.visibility = View.VISIBLE
            binding.noCustomerDats.text = event.getContentIfNotHandled()

        }
        viewModel._customerListResponse.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { customerList ->
                var data: ArrayList<TbCustomer>
                if (customerList.isNotEmpty()) {
                    binding.rvEmployeeList.visibility = View.VISIBLE
                    binding.noCustomerDats.visibility = View.GONE
                    dynamicCustomerList.clear()
                    data = customerList as ArrayList<TbCustomer>
                    dynamicCustomerList.addAll(data)
                    customerAdapter.setList(data)
                    try {
                        if (!firstDetailLoad) {
                            if (data.isNotEmpty()) {
                                loadFragment(data[0])
                            }
                        }
                    } catch (e: Exception) {

                    }
                }

            }
        }

        return binding.root
    }


    private fun loadCustomerLocalList(currentpage: Int) {
        data["page"] = currentpage.toString()
        data["per_page"] = perpagedata.toString()
        if (currentpage == 1) {
            firstDetailLoad = false
        }
        viewModel.customerList(data).observe(
            viewLifecycleOwner


        ) {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {

                        ProgressUtils.dismissProgressDialog()
                        var data: ArrayList<TbCustomer>
                        if (resource.data != null) {
                            data =
                                resource.data as ArrayList<TbCustomer>
                            binding.rvEmployeeList.visibility = View.VISIBLE
                            binding.noCustomerDats.visibility = View.GONE
                            Log.e(TAG, "getCustomerData ${Gson().toJson(data)}")
                            dynamicCustomerList.clear()
                            dynamicCustomerList.addAll(data)
                            customerAdapter.setList(data)

                            //setUpRecyclerView()

                            try {
                                if (!firstDetailLoad) {
                                    loadFragment(data[0])
                                }
                            } catch (e: Exception) {

                            }

                        }

                    }
                    Status.LOADING -> {

                        ProgressUtils.showProgressDialog(requireActivity())
                    }
                    Status.ERROR -> {
                        ProgressUtils.dismissProgressDialog()
                        binding.root.showAlert(resource.message)

                    }


                }

            }


        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configureToolbar()
        //loadFragment()
        searchQuery()

        binding.txtCrtNewCustomer.setOnClickListener {
            //dialog?.dismiss()
            binding.linearCustomerDialog.visibility = View.GONE

            val bundle: Bundle = bundleOf("isEdit" to false)
            findNavController().navigate(R.id.action_customer_to_addEditCustomer, bundle)

        }

        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<String>(KEY)
            ?.observe(viewLifecycleOwner) {
                when (it) {
                    CUSTOMERDETAILS -> {
                        Log.e(TAG, "UpdateLoadList")
                        viewModel.customerList(data)

                    }

                }

            }

    }

    private fun searchQuery() {
        binding.autoSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {

                try {
                    if (s?.trim()?.isNotEmpty() == true) {
                        searchByText(s?.trim().toString())
                    } else {
                        loadCustomerLocalList(1)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }

        })
    }

    fun searchByText(query: String) {
        viewModel.searchByTextCustomer(query)
    }

    private fun loadFragment(model: TbCustomer) {

        firstDetailLoad = true
        val frag = CustomerDetails.newInstance(model)
        val fm: FragmentManager = requireActivity().supportFragmentManager
        fm.beginTransaction().replace(binding.frameContainer.id, frag).commit()
    }


    private fun configureToolbar() {
        binding.layoutTool.txtTitle.text = "Customers"

        binding.layoutTool.imgDrawer.setOnClickListener {
            (requireActivity() as MainActivity).enableDrawer()
        }
        binding.layoutTool.txtHome.setOnClickListener {
            findNavController().navigate(R.id.action_customer_to_dashboardCategoryNew)
        }

        binding.layoutTool.imgOptionMenu.setOnClickListener {

            // showDialog()
            if (binding.linearCustomerDialog.visibility == View.VISIBLE) {
                binding.linearCustomerDialog.visibility = View.GONE
            } else {
                binding.linearCustomerDialog.visibility = View.VISIBLE
            }
            /* if (binding.linearCustomerDialog.visibility == View.VISIBLE){

             }
             else{

             }*/
        }



        binding.root.setOnClickListener {
            if (binding.linearCustomerDialog.visibility == View.VISIBLE) {
                binding.linearCustomerDialog.visibility = View.GONE
            }
        }
    }

    private fun observeCustomerDelete() {
        viewModel.data.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {

                // customerAdapter.getList()[pos]


                if (customerAdapter.getList().size - 1 != deletedPos) {
                    if (customerAdapter.getList().lastIndex == deletedPos) {
                        val model = deletedPos?.minus(1)
                            ?.let { it1 -> customerAdapter.getList().get(it1) } as TbCustomer
                        binding.layoutTool.txtSubTitle.setText(model?.first_name + " " + model?.last_name)
                        loadFragment(model)
                    } else {
                        val model = deletedPos?.plus(1)
                            ?.let { it1 -> customerAdapter.getList().get(it1) } as TbCustomer
                        binding.layoutTool.txtSubTitle.setText(model?.first_name + " " + model?.last_name)
                        loadFragment(model)
                    }

                } else {
                    val model = deletedPos?.minus(1)
                        ?.let { it1 -> customerAdapter.getList().get(it1) } as TbCustomer
                    binding.layoutTool.txtSubTitle.setText(model?.first_name + " " + model?.last_name)
                    loadFragment(model)
                }


                AlertUtils.showCustomAlertWithListenerWithOK(
                    requireContext(),
                    it.message
                ) { _, _ ->

                }


            }

        }
    }

    private fun setUpRecyclerView() {


        customerAdapter = CustomerListAdapter(object :
            CustomerListAdapter.CustomerInteface {
            override fun onCustomerSelect(
                pos: Int,
                model: TbCustomer
            ) {

                binding.layoutTool.txtSubTitle.setText(model.first_name + " " + model.last_name)
                loadFragment(model)
            }

        })

        binding.rvEmployeeList.adapter = customerAdapter


        object : SwipeHelper(activity, binding.rvEmployeeList) {
            override fun instantiateUnderlayButton(
                viewHolder: RecyclerView.ViewHolder?,
                underlayButtons: MutableList<UnderlayButton>
            ) {
                underlayButtons.add(UnderlayButton(
                    "Delete",
                    0,
                    ContextCompat.getColor(context, R.color.white_swipe)
                ) { pos ->
                    deletedPos = pos
                    alert(
                        getString(R.string.app_name),
                        getString(R.string.delete_customer_message)
                    ) {
                        positiveButton(getString(R.string.tv_delete)) {
                            customerAdapter.getList()[pos].id?.let { viewModel.delete(it) }

                        }
                        negativeButton(R.string.tv_cancel) {
                            // Do negative stuff here
                        }
                    }




                    Log.e(TAG, "posClicked  ${pos}")
                })


            }

        }
    }


}