package com.android.pos.ui.fragments.customer

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.entities.TbCustomer
import com.android.pos.data.model.CustomerDetailModel
import com.android.pos.data.model.CustomerModel
import com.android.pos.data.model.responseModel.EmployeeListResponse
import com.android.pos.data.remote.Constants.CUSTOMERDETAILS
import com.android.pos.data.remote.Constants.KEY
import com.android.pos.databinding.FragmentCustomerBinding
import com.android.pos.ui.activities.MainActivity
import com.android.pos.ui.adapter.CustomerListAdapter
import com.android.pos.ui.fragments.team.TeamListViewModel
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.SwipeHelper
import com.android.pos.utils.SwipeHelperNew
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
        loadCustomerLocalList()
        return binding.root
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
                    Color.parseColor("#FF3C30")
                ) { pos ->
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

    private fun loadCustomerLocalList() {


        //viewModel.clearDataBase()

        viewModel.customerList().observe(viewLifecycleOwner, {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {

                        ProgressUtils.dismissProgressDialog()
                        if (resource.data != null) {
                            val data =
                                resource.data as ArrayList<TbCustomer>

                            Log.e(TAG, "getCustomerData ${Gson().toJson(data)}")
                            dynamicCustomerList.clear()
                            dynamicCustomerList.addAll(data)
                            customerAdapter.setList(data)
                            //setUpRecyclerView()


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


        )

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
                        viewModel.customerList()

                    }

                }

            }

    }

    private fun searchQuery() {
        binding.autoSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {


                try {
                    customerAdapter.filter.filter(
                        binding.autoSearch.text.trim().toString()
                    )

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun afterTextChanged(s: Editable?) {

            }

        })
    }

    private fun loadFragment(model: TbCustomer) {


        val frag = CustomerDetails.newInstance(model)
        val fm: FragmentManager = requireActivity().supportFragmentManager
        fm.beginTransaction().replace(binding.frameContainer.id, frag).commit()
    }


    private fun configureToolbar() {
        binding.layoutTool.txtTitle.setText("Customers")

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


}