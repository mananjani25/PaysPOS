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
import com.android.pos.R
import com.android.pos.data.model.CustomerDetailModel
import com.android.pos.data.model.CustomerModel
import com.android.pos.data.model.responseModel.EmployeeListResponse
import com.android.pos.databinding.FragmentCustomerBinding
import com.android.pos.ui.activities.MainActivity
import com.android.pos.ui.adapter.CustomerListAdapter
import com.android.pos.ui.fragments.team.TeamListViewModel
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.showAlert
import com.android.pos.utils.statusUtils.Status
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class Customer : Fragment() {

    private lateinit var binding: FragmentCustomerBinding
    private lateinit var customerAdapter: CustomerListAdapter
    private val viewModel by viewModels<CustomerListViewModel>()
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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configureToolbar()
        setAdapter()
        //loadFragment()
        searchQuery()
        loadCustomerList()
        binding.txtCrtNewCustomer.setOnClickListener {
            //dialog?.dismiss()
            binding.linearCustomerDialog.visibility = View.GONE

            val bundle: Bundle = bundleOf("isEdit" to false)
            findNavController().navigate(R.id.action_customer_to_addEditCustomer, bundle)

        }

    }

    private fun loadCustomerList() {
        viewModel.customerList.observe(viewLifecycleOwner, {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        if (resource.data?.data != null) {
                            val list: ArrayList<CustomerModel> = arrayListOf()

                            val data =
                                resource.data.data as ArrayList<com.android.pos.data.model.CustomerListResponse.Data>

                            Log.e(TAG, "getCustomerData ${Gson().toJson(data)}")

                            val adapter = CustomerListAdapter(requireContext(), data, object :
                                CustomerListAdapter.CustomerInteface {
                                override fun onCustomerSelect(
                                    pos: Int,
                                    model: com.android.pos.data.model.CustomerListResponse.Data
                                ) {
                                    binding.layoutTool.txtSubTitle.setText(model.first_name + " " + model.last_name)
                                    loadFragment(model)
                                }

                            })

                            binding.rvEmployeeList.adapter = adapter

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

    private fun searchQuery() {
        binding.autoSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                Log.e(TAG, "${binding.autoSearch.text}")
                //  customerAdapter.filter.filter(binding.autoSearch.text.trim().toString())

            }

            override fun afterTextChanged(s: Editable?) {

            }

        })

    }

    private fun loadFragment(model: com.android.pos.data.model.CustomerListResponse.Data) {


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


        /* binding.layoutTool.txtSubTitle.setText("${listCustomer.get(0).name}")
         customerAdapter = CustomerListAdapter(requireContext(), listCustomer, object :
             CustomerListAdapter.CustomerInteface {
             override fun onCustomerSelect(pos: Int, model: CustomerModel) {
                 binding.layoutTool.txtSubTitle.setText("${model.name}")

             }

         })

         customerAdapter.setList(requireContext(), listCustomer)
         binding.rvEmployeeList.adapter = customerAdapter
 */

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

    private fun showDialog() {


        dialog?.setCanceledOnTouchOutside(true)
        dialog?.setCancelable(true)
        dialog?.setOnDismissListener {
            binding.linearCustomerDialog.visibility = View.GONE
        }
        dialog?.show()
    }


}