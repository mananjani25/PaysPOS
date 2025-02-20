package com.pays.pos.ui.fragments.eGiftCard

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
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.pays.pos.R
import com.pays.pos.data.entities.CartModel
import com.pays.pos.data.entities.TbCartItem
import com.pays.pos.data.entities.TbCustomer
import com.pays.pos.data.remote.Constants
import com.pays.pos.databinding.FragmentAddCustomerToGiftCardBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.ui.adapter.AssignCustomerToOrderAdapter
import com.pays.pos.ui.fragments.customer.AddCustomerViewModel
import com.pays.pos.ui.fragments.customer.CustomerListViewModel
import com.pays.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.MethodUtils
import com.pays.pos.utils.ProgressUtils
import com.pays.pos.utils.callback.ItemCallback
import com.pays.pos.utils.callback.PaginationScrollListener
import com.pays.pos.utils.extensions.liveSnackBar
import com.pays.pos.utils.extensions.runOnUiThread
import com.pays.pos.utils.extensions.setOnSingleClickListener
import com.pays.pos.utils.statusUtils.Status
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AddCustomerToGiftCardFragment : Fragment(), ItemCallback {

    private var selectedPosition: Int = -1

    private lateinit var binding: FragmentAddCustomerToGiftCardBinding
    private val viewModel by viewModels<CustomerListViewModel>()
    private val addCustomerViewModel by viewModels<AddCustomerViewModel>()
    private lateinit var adapter: AssignCustomerToOrderAdapter
    private var customerListIDs: ArrayList<Int> = arrayListOf()
    private val TAG = "AddCustomerToGiftCardFragment"

    @Inject
    lateinit var prefProvider: PrefProvider
    private val dashboardViewModel by activityViewModels<DashBoardCategoryViewModel>()

    private var currentPage = 1
    private val perPageData = 50
    private var isLoading = false
    private var isLastPage = false
    private val dynamicCustomerList: java.util.ArrayList<TbCustomer> =
        arrayListOf()
    val data = LinkedHashMap<String, String>()
    private var searchedCustomer: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddCustomerToGiftCardBinding.inflate(layoutInflater)

        data["page"] = currentPage.toString()
        data["per_page"] = perPageData.toString()

        setUpSnackBar()
        setupUI()
        addObserver()

        loadCustomerLocalList(currentPage)

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
                    currentPage += 1
                    loadCustomerLocalList(currentPage)
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

            /*val navController = findNavController()
            var bundle = Bundle()
            bundle.putString("SELECTED_DATE", selectedDate)
            bundle.putBundle("updateBundle", arguments)
            bundle.putString(Constants.KEY, "FROM_CUSTOMER")
            navController.previousBackStackEntry?.savedStateHandle?.set(
                "data", bundle
            )*/
            findNavController().popBackStack()

        }

        binding.txtCreateCustomer.setOnClickListener {
            findNavController().navigate(R.id.action_addCustomerToGiftCard_to_addNewCustomerForGiftCard)
        }
        binding.txtNext.setOnClickListener {
            if (selectedPosition != -1) {
                moveToCheckout()
            } else {
                AlertUtils.showCustomAlert(requireContext(), "Please choose customer")
                return@setOnClickListener
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

    private fun moveToCheckout() {
        val customer = adapter.getItem(selectedPosition)
        if (prefProvider.getValue(Constants.GIFT_CARD_TYPE,"").equals("Physical",true)){

            val dataBundle = Bundle()
            dataBundle.putParcelable("customer",customer)

            findNavController().navigate(R.id.action_addCustomerToGiftCard_to_plasticCardNumber,dataBundle)
        }
        else {


            prefProvider.setValue(
                Constants.CUSTOMER_NAME,
                customer.first_name + " " + customer.last_name
            )

            prefProvider.setValue(
                Constants.RECEIPT_CUSTOMER_NAME,
                customer.first_name + " " + customer.last_name
            )
            var orderTypeIdFromDb: Int = 0
            synchronized(this) {
                CoroutineScope(Dispatchers.IO).launch {
                    orderTypeIdFromDb = dashboardViewModel.orderTypeByName(Constants.TAKEOUT)
                }
            }
            customer.id?.let { prefProvider.setValueInt(Constants.CUSTOMER_ID, it) }

            prefProvider.saveCustomerData(customer)

            prefProvider.setValue("PaidAmount", "")
            prefProvider.setValue(Constants.WHOLE_AMOUNT, "")
            prefProvider.setValueInt("cardCount", 0)
            prefProvider.setValue(Constants.SUB_TOTAL, "")
            prefProvider.setValue(Constants.CASH_DISCOUNT_SURCHARGE, "")
            prefProvider.setValue(Constants.TOTAL_DISCOUNT, "")
            prefProvider.setValue(Constants.TIP, "")
            prefProvider.setValue(Constants.TAX_CHARGE, "")
            prefProvider.setValue(Constants.SERVICE_CHARGE, "")

            dashboardViewModel.deleteCart()

            prefProvider.setValue(Constants.ORDER_TYPE, Constants.GIFT_CARD)
            prefProvider.setValue(Constants.ORDER_TYPE_NAME, Constants.GIFT_CARD_NAME)
            prefProvider.setValueboolean(Constants.IS_ADD_VALUE_IN_GIFT_CARD, false)

            val cm = CartModel()
            val tbItem = TbCartItem()
            tbItem.name = "Digital Gift Card"
            tbItem.quantity = 1
            tbItem.itemQuantity = 1
            val totalPrice =
                prefProvider.getValue(Constants.GIFT_CARD_PURCHASE_AMOUNT, "0.0").toDouble()
            tbItem.price = totalPrice
            tbItem.employeeID = prefProvider.getValueInt(Constants.EMPLOYEE_ID, -1)
            tbItem.orderTypeId = orderTypeIdFromDb
            tbItem.orderType = Constants.GIFT_CARD
            tbItem.orderTypeName = Constants.GIFT_CARD

            cm.apply {
                employeeID = prefProvider.getValueInt(Constants.EMPLOYEE_ID, -1)
                terminalId = prefProvider.getValueInt(Constants.TERMINAL_ID, -1)
                isOpenOrder = false
                orderTypeId = orderTypeIdFromDb
                orderType = Constants.GIFT_CARD
                orderTypeName = Constants.GIFT_CARD
                locationId = prefProvider.getLocationId()
            }
            Log.e(TAG, "checkItems: ${Gson().toJson(tbItem)}")

            dashboardViewModel.addCart(cm)
            dashboardViewModel.addItemToCartItems(tbItem)


            CoroutineScope(Dispatchers.IO).launch {
                delay(100)

                runOnUiThread(kotlinx.coroutines.Runnable {
                    val bundle = Bundle()
                    bundle.putBoolean("update", true)
                    bundle.putDouble("totalPrice", totalPrice)
                    bundle.putDouble("finalprice", totalPrice)
                    bundle.putDouble("cashDiscountSurcharge", 0.0)
                    bundle.putDouble("subTotalPrice", totalPrice)
                    bundle.putDouble("totalTax", 0.0)
                    bundle.putDouble("totalDiscount", 0.0)
                    bundle.putDouble("totalServiceCharge", 0.0)
                    bundle.putParcelable("cartList", cm)

                    findNavController().navigate(
                        R.id.action_addCustomerToGiftCard_to_paymentBoldPosFragment,
                        bundle
                    )
                })
            }
        }
    }

    private fun loadCustomerLocalList(currentPage: Int) {
        data["page"] = currentPage.toString()
        data["per_page"] = perPageData.toString()
        viewModel.customerList(data).observe(viewLifecycleOwner) {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {

                        if (resource.data != null) {

                            val data =
                                resource.data as ArrayList<TbCustomer>

                            if (prefProvider.getValue(
                                    Constants.ORDER_TYPE,
                                    ""
                                ) == Constants.DINE_IN
                            ) {
                                data.removeAll { tbCustomer -> customerListIDs.contains(tbCustomer.id) }
                            }
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

    }

    override fun onItemClickListener(view: View?, pos: Int) {
        MethodUtils.hideSoftKeyboard(requireActivity())
        if (pos > 0 || pos <= adapter.itemCount) {
            selectedPosition = pos

            val customer = adapter.getItem(selectedPosition)

            if (customer.email.isNullOrEmpty() && customer.phones.isEmpty()) {
                AlertUtils.showCustomAlertWithListenerWithOKCancelUpdated(
                    requireContext(),
                    getString(R.string.lbl_please_add_phone_or_email), "Edit",
                )
                { _, _ ->
                    prefProvider.setValue(Constants.ORDER_TYPE, Constants.GIFT_CARD)
                    prefProvider.setValue(Constants.ORDER_TYPE_NAME, Constants.GIFT_CARD_NAME)
                    prefProvider.setValueboolean(Constants.IS_ADD_VALUE_IN_GIFT_CARD, false)
                    val bundle: Bundle = bundleOf("isEdit" to true, "dataModel" to customer, "isEmailAndPhoneEmpty" to true)
                    findNavController().navigate(
                        R.id.action_addCustomerToGiftCard_to_addEditCustomer,
                        bundle
                    )
                }
            } else {
                moveToCheckout()
            }
        }


    }

}