package com.android.pos.ui.fragments.eGiftCard

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.pos.R
import com.android.pos.data.entities.CartModel
import com.android.pos.data.entities.TbCustomer
import com.android.pos.data.remote.Constants
import com.android.pos.databinding.FragmentAddCustomerToGiftCardBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.AssignCustomerToOrderAdapter
import com.android.pos.ui.fragments.customer.CustomerListViewModel
import com.android.pos.utils.LogUtil
import com.android.pos.utils.callback.ItemCallback
import com.android.pos.utils.callback.PaginationScrollListener
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AddCustomerToGiftCardFragment : Fragment(), ItemCallback {

    private val TAG = "AddCustomerToGiftCardFr"

    private lateinit var binding: FragmentAddCustomerToGiftCardBinding
    private val viewModel by viewModels<CustomerListViewModel>()
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
    private var cartList: ArrayList<CartModel> = arrayListOf()
    val data = LinkedHashMap<String, String>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddCustomerToGiftCardBinding.inflate(layoutInflater)

        data["page"] = currentpage.toString()
        data["per_page"] = perpagedata.toString()

        setupUI()

        loadCustomerLocalList(currentpage)

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

                adapter.filter.filter(s.toString().lowercase().trim())

            }
        })

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
            findNavController().navigate(R.id.action_addCustomerToGiftCard_to_dashboard_category_new)
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

                            if (prefProvider.getValue(Constants.ORDER_TYPE, "") == Constants.DINE_IN) {
                                data.removeAll { customerListIDs.contains(it.id) }
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

        val customer = adapter.getItem(pos)

        prefProvider.setValue(
            Constants.CUSTOMER_NAME,
            customer.first_name + " " + customer.last_name
        )

        customer.id?.let { prefProvider.setValueInt(Constants.CUSTOMER_ID, it) }

        prefProvider.saveCustomerData(customer)

        findNavController().navigate(R.id.action_addCustomerToGiftCard_to_dashboard_category_new)

    }

}