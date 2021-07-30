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
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
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
    private val dynamicCustomerList: ArrayList<com.android.pos.data.model.CustomerListResponse.Data> =
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

        // binding.lifecycleOwner = this
        loadCustomerList()
        return binding.root
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
                        //   loadCustomerList()

                    }

                }

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
                            dynamicCustomerList.clear()
                            dynamicCustomerList.addAll(data)

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
                            object : SwipeHelper(activity, binding.rvEmployeeList) {
                                override fun instantiateUnderlayButton(
                                    viewHolder: RecyclerView.ViewHolder?,
                                    underlayButtons: MutableList<UnderlayButton>
                                ) {
                                    underlayButtons.add(
                                        UnderlayButton(
                                            "Delete",
                                            0,
                                            Color.parseColor("#FF3C30")
                                        ) { pos ->

                                            Log.e(TAG, "UnderLAyButton")
                                            alert(
                                                getString(R.string.app_name),
                                                getString(R.string.delete_customer_message)
                                            ) {
                                                positiveButton(getString(R.string.tv_delete)) {
                                                    Log.e(
                                                        "Delete",
                                                        "getDeleteItem  ${adapter.getItem(pos)}"
                                                    )

                                                    viewModel.delete(adapter.getItem(pos).id)
                                                    removeItem(pos)

                                                    // discountObject = discountListadapter.getItem(pos)
                                                    // viewModel.delete(discountListadapter.getItem(pos).id)
                                                }
                                                negativeButton(R.string.tv_cancel) {
                                                    // Do negative stuff here
                                                }
                                            }

                                        })

                                }

                            }


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


                if (binding.autoSearch.text.trim().isNotEmpty()) {
                    (binding.rvEmployeeList.adapter as CustomerListAdapter).filter.filter(
                        binding.autoSearch.text.trim().toString()
                    )
                } else {
                    (binding.rvEmployeeList.adapter as CustomerListAdapter?)?.setList(
                        requireContext(),
                        dynamicCustomerList
                    )
                    (binding.rvEmployeeList.adapter as CustomerListAdapter?)?.notifyDataSetChanged()


                }
                binding.rvEmployeeList.adapter?.notifyDataSetChanged()


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

    private fun swipeToDelete() {

    }

    fun removeItem(pos:Int){
        var customerList = (binding.rvEmployeeList.adapter as CustomerListAdapter).list
        customerList.removeAt(pos)
        binding.rvEmployeeList.removeViewAt(pos)
        (binding.rvEmployeeList.adapter as CustomerListAdapter).notifyItemRemoved(pos)
        (binding.rvEmployeeList.adapter as CustomerListAdapter).notifyItemRangeChanged(pos,customerList.size)
        (binding.rvEmployeeList.adapter as CustomerListAdapter).notifyDataSetChanged()

    }
}