package com.pays.pos.ui.fragments.orderassign

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.pays.pos.R
import com.pays.pos.data.entities.CartModel
import com.pays.pos.data.entities.TbCustomer
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.DELIVERY
import com.pays.pos.data.remote.Constants.DELIVERY_TYPE
import com.pays.pos.data.remote.Constants.DINE_IN
import com.pays.pos.data.remote.Constants.ORDER_TYPE
import com.pays.pos.data.remote.Constants.PHONE_ORDER
import com.pays.pos.data.remote.Constants.PICK_UP
import com.pays.pos.data.remote.Constants.TAKEOUT
import com.pays.pos.databinding.FragmentAssignCustomerOrderBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.logger.MessageEvent
import com.pays.pos.ui.adapter.AssignCustomerToOrderAdapter
import com.pays.pos.ui.fragments.customer.AddCustomerViewModel
import com.pays.pos.ui.fragments.customer.CustomerListViewModel
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.ProgressUtils
import com.pays.pos.utils.callback.ItemCallback
import com.pays.pos.utils.callback.PaginationScrollListener
import com.pays.pos.utils.extensions.liveSnackBar
import com.pays.pos.utils.extensions.setOnSingleClickListener
import com.pays.pos.utils.statusUtils.Status
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

@AndroidEntryPoint
class AssignCustomerOrderFragment : Fragment(), ItemCallback {
    private var isPhoneOrder: Boolean? = false
    private var orderType: String? = ""
    private val TAG = "AssignCustomerOrderFr"

    companion object {
        fun newInstance() = AssignCustomerOrderFragment()
    }

    private lateinit var binding: FragmentAssignCustomerOrderBinding
    private val viewModel by viewModels<CustomerListViewModel>()
    private val addCustomerViewModel by viewModels<AddCustomerViewModel>()
    private lateinit var adapter: AssignCustomerToOrderAdapter
    private var isFromDineIn: Boolean? = false
    private var dineInPosition: Int? = null
    private var isFromCompletePayment: Boolean = false
    private var customerListIDs: ArrayList<Int> = arrayListOf()


    @Inject
    lateinit var prefProvider: PrefProvider

    private var currentpage = 1
    private val perpagedata = 50
    private var isLoading = false
    private var isLastPage = false
    private var firstDetailLoad = false
    private var selectedDate: String? = null
    private var searchedCustomer: String = ""
    private var cartList: ArrayList<CartModel> = arrayListOf()
    private val dynamicCustomerList: java.util.ArrayList<TbCustomer> =
        arrayListOf()
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

        setUpSnackBar()
        setupUI()
        addObserver()

        loadCustomerLocalList(currentpage)
        customerListSizeFromPagination(currentpage)
        if (arguments != null) {
            isFromDineIn = arguments?.getBoolean("DINE_IN", false)
            isPhoneOrder = arguments?.getBoolean("PhoneOrder", false)
            orderType = arguments?.getString(ORDER_TYPE, "")
            isFromCompletePayment = arguments?.getBoolean("fromPayment") ?: false
            dineInPosition = arguments?.getInt("position")
            selectedDate = arguments?.getString("SELECTED_DATE")
            if (arguments?.getParcelableArrayList<CartModel>("cartList") != null) {
                cartList = (arguments?.getParcelableArrayList<CartModel>("cartList")
                    ?: emptyList<CartModel>()) as ArrayList<CartModel>
            }

            if (prefProvider.getValue(ORDER_TYPE, "") == DINE_IN) {
                customerListIDs =
                    arguments?.getIntegerArrayList("listOfCustomersID") ?: arrayListOf()
                Log.e("CheckSelectedID", "customerListIDs  ${Gson().toJson(customerListIDs)}")

            }
        }

        EventBus.getDefault()
            .post(MessageEvent("${Constants.LINE_BREAK_TAB} AssignCustomerOrderFragment.kt onCreateView"))

        return binding.root
    }

    private fun addObserver() {
        addCustomerViewModel.customerListResponse.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { customerList ->
                val data: java.util.ArrayList<TbCustomer>
                if (customerList.isNotEmpty()) {
                    dynamicCustomerList.clear()
                    data = customerList as java.util.ArrayList<TbCustomer>
                    dynamicCustomerList.addAll(data)
                    adapter.add(data)

                    Handler().postDelayed({
                        val s = binding.etSearch.text.toString()
                        adapter.filter.filter(s.toString().lowercase().trim())
                    },500)
                }
            }
        }

        addCustomerViewModel.customerNoDataFound.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { customerListResponse ->
                if (customerListResponse.isNotEmpty()) {
                    AlertUtils.showCustomAlert(requireContext(), customerListResponse)
                }
            }
        }

        addCustomerViewModel.showProgress.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }

        }
    }

    private fun setUpSnackBar() {
        binding.root.liveSnackBar(this, addCustomerViewModel.snackbarText, Snackbar.LENGTH_SHORT)
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
                    customerListSizeFromPagination(currentpage)
                }

            }

        })


        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun afterTextChanged(s: Editable) {

                adapter.filter.filter(s.toString().lowercase().trim())
                searchedCustomer = s.toString().lowercase().trim()

            }
        })

        binding.txtSearchCustomer.setOnSingleClickListener {
            it.isEnabled = false
            hideKeyboard()
            addCustomerViewModel.searchCustomerForAssignCustomerOrderFragment(searchedCustomer)
            Handler().postDelayed({
                it.isEnabled = true
            }, 2000)
        }

        binding.imgBack.setOnClickListener {
            hideKeyboard()
            val navController = findNavController()
            var bundle = Bundle()
            bundle.putString("SELECTED_DATE", selectedDate)
            bundle.putBundle("updateBundle", arguments)
            bundle.putString(Constants.KEY, "FROM_CUSTOMER")
            navController.previousBackStackEntry?.savedStateHandle?.set(
                "data", bundle
            )
            navController.popBackStack()

        }

        binding.txtCreateCustomer.setOnClickListener {
            var bundle: Bundle = Bundle().apply {
                putString(ORDER_TYPE, orderType)
            }

            findNavController().navigate(
                R.id.action_assignCustomerOrderFragment_to_addEditCustomer,
                bundle
            )
        }
        binding.txtHome.setOnClickListener {
            hideKeyboard()
            if (arguments != null) {
                var bundle: Bundle = Bundle()
                bundle.putBoolean("update", arguments?.getBoolean("update") ?: false)
                bundle.putInt("orderId", arguments?.getInt("orderId")!!)
                bundle.putInt("paymentId", arguments?.getInt("paymentId")!!)
                bundle.putString("paymentOfflineId", arguments?.getString("paymentOfflineId"))
                bundle.putString("orderOfflineId", arguments?.getString("orderOfflineId"))

                EventBus.getDefault().post(
                    MessageEvent(
                        "${Constants.LINE_BREAK_TAB} AssignCustomerOrderFragment.kt  binding.txtHome bundle -> ${
                            Gson().toJson(bundle)
                        }"
                    )
                )

                findNavController().navigate(
                    R.id.action_assignCustomerOrderFragment_to_dashboard_category_new,
                    bundle
                )
            } else {
                findNavController().navigate(R.id.action_assignCustomerOrderFragment_to_dashboard_category_new)
            }

        }

    }

    private fun customerListSizeFromPagination(currentPage: Int) {
        data["page"] = currentPage.toString()
        data["per_page"] = perpagedata.toString()

        CoroutineScope(Dispatchers.IO).launch {
            val result = viewModel.fetchCustomersList(data)

            when (result.status) {
                Status.SUCCESS -> {
                    result.data?.let { customerList ->
                        if (customerList.data.size < perpagedata) {
                            isLastPage = true
                        }
                    }
                }

                Status.ERROR -> {

                }

                Status.LOADING -> {

                }
            }
        }
    }

    private fun loadCustomerLocalList(currentpage: Int) {
        data["page"] = currentpage.toString()
        data["per_page"] = perpagedata.toString()
        viewModel.customerList(data).observe(viewLifecycleOwner) {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {

                        if (resource.data != null) {

                            val data =
                                resource.data as ArrayList<TbCustomer>

                            if (prefProvider.getValue(ORDER_TYPE, "") == DINE_IN) {
                                data.removeAll { customerListIDs.contains(it.id) }
                            }
                            adapter.add(data)
                        }
                        binding.rvCustomerList.visibility = View.VISIBLE
                        binding.progressCircular.visibility = View.GONE
                        isLoading = false
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

    }

    private fun hideKeyboard() {
        val view = activity!!.currentFocus
        if (view != null) {
            val imm: InputMethodManager =
                activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    override fun onItemClickListener(view: View?, pos: Int) {
        hideKeyboard()
        // if adding customer to phone order, verify if customer has necessary details
        if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == PHONE_ORDER) {
            val customer = adapter.getItem(pos)
            if ((prefProvider.getValue(DELIVERY_TYPE, PICK_UP) == PICK_UP
                        || prefProvider.getValue(DELIVERY_TYPE, PICK_UP) == DELIVERY)
                && customer.phones.isEmpty()
            ) {
                navigateToEditCustomer(
                    getString(R.string.add_phone_in_profile),
                    customer = customer
                )
            } else if (prefProvider.getValue(DELIVERY_TYPE, PICK_UP) == DELIVERY
                && customer.addresses.isEmpty()
            ) {
                navigateToEditCustomer(
                    getString(R.string.add_address_in_profile),
                    customer = customer
                )
            } else {
                onSelectingCustomer(pos)
            }
        } else {
            onSelectingCustomer(pos)
        }
    }

    private fun onSelectingCustomer(pos: Int) {
        try {
            val customer = adapter.getItem(pos)
            prefProvider.setValue(
                Constants.CUSTOMER_NAME,
                customer.first_name + " " + customer.last_name
            )

            prefProvider.setValue(
                Constants.RECEIPT_CUSTOMER_NAME,
                customer.first_name + " " + customer.last_name
            )
            prefProvider.setValueboolean(Constants.LOYALTY_ADDED, false)
            prefProvider.setValueboolean(Constants.IS_UPDATE_ORDER_LOYALTY_APPLIED, false)
            customer.id?.let { prefProvider.setValueInt(Constants.CUSTOMER_ID, it) }
            prefProvider.saveCustomerData(customer)
            val result = Bundle().apply {
                putParcelable("data", customer)
                putBoolean("OPEN_ORDER", false)
                putString("SELECTED_DATE", selectedDate)
                putBoolean("isEdit", true)
                putBundle("updateBundle", arguments)
                putParcelableArrayList("cartList", cartList)
                putString(Constants.KEY, "FROM_CUSTOMER")

                isFromDineIn?.let { putBoolean("DINE_IN", it) }
                dineInPosition?.let {
                    LogUtil.logE(TAG, "position:  $it")
                    putInt("position", it)
                }
            }

            if (isFromDineIn == true) {
                LogUtil.logE(TAG, "isFromDineIn:  ${isFromDineIn}")
                setFragmentResult("request_key_customer_dine_in", result)
            } else {
                if (isPhoneOrder == true) {
                    setFragmentResult("request_key_customer_phone_order", result)
                } else
                    setFragmentResult("request_key_customer", result)
            }

//        val navController =
//        navController.previousBackStackEntry?.savedStateHandle?.set("data",result)
        }catch (e:Exception) {
            Log.e("PAYS ERROR",e.message.toString())
        }
        findNavController().popBackStack()

    }

    private fun navigateToEditCustomer(message: String, customer: TbCustomer) {
        AlertUtils.showCustomAlertWithListenerWithOKCancel(
            requireContext(),
            message, getString(R.string.edit),
        )
        { _, _ ->
            val bundle: Bundle = bundleOf(
                "isEdit" to true,
                "dataModel" to customer,
                "isFromPhoneOrderEdit" to true,
                ORDER_TYPE to orderType
            )
            findNavController().navigate(
                R.id.action_assignCustomerOrderFragment_to_addEditCustomer_,
                bundle
            )
        }
    }
}