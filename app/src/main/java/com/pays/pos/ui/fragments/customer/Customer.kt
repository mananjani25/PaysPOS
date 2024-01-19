package com.pays.pos.ui.fragments.customer

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.pays.pos.R
import com.pays.pos.data.entities.TbCustomer
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.CUSTOMERDETAILS
import com.pays.pos.data.remote.Constants.KEY
import com.pays.pos.databinding.FragmentCustomerBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.ui.adapter.CustomerListAdapter
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.ProgressUtils
import com.pays.pos.utils.callback.ItemCallback
import com.pays.pos.utils.callback.PaginationScrollListener
import com.pays.pos.utils.extensions.alert
import com.pays.pos.utils.extensions.gone
import com.pays.pos.utils.extensions.showAlert
import com.pays.pos.utils.extensions.visible
import com.pays.pos.utils.statusUtils.Status
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class Customer : Fragment(), ItemCallback {

    private lateinit var binding: FragmentCustomerBinding
    private lateinit var customerAdapter: CustomerListAdapter
    private val viewModel by viewModels<CustomerListViewModel>()
    private val dynamicCustomerList: ArrayList<TbCustomer> =
        arrayListOf()
    private var dialog: Dialog? = null
    private val TAG = "Customer"
    private var currentpage = 1
    var isFromSearch: Boolean = false

    @Inject
    lateinit var prefProvider: PrefProvider
    private val perpagedata = 50
    private var isLoading = false
    private var isLastPage = false
    private var firstDetailLoad = false
    private var deletedPos: Int? = null
    var isIn = false
    val data = LinkedHashMap<String, String>()
    var isEmptyString = true
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
        prefProvider = PrefProvider(requireContext())
        setUpRecyclerView()
        observeCustomerDelete()
        isFromSearch = false
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
                                setSubTitleFirstLastName(data[0])
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

                        if (!isFromSearch) {
                            ProgressUtils.dismissProgressDialog()
                        }
                        var data: ArrayList<TbCustomer>
                        if (resource.data != null) {
                            data =
                                resource.data as ArrayList<TbCustomer>
                            if (data.isNotEmpty()) {
                                binding.frameContainer.visible()
                                binding.layout.gone()
                                binding.rvEmployeeList.visibility = View.VISIBLE
                                binding.noCustomerDats.visibility = View.GONE
                                LogUtil.logE(TAG, "getCustomerData ${Gson().toJson(data)}")
                                dynamicCustomerList.clear()
                                dynamicCustomerList.addAll(data)
                                customerAdapter.setList(data)
                            } else {
                                binding.frameContainer.gone()
                                binding.layout.visible()
                                binding.txtNodatavallidation?.text =
                                    "${data.size} customers that you manage at " + prefProvider.getValue(
                                        Constants.BUSINESS_NAME,
                                        ""
                                    )

                                binding.rvEmployeeList.visibility = View.GONE
                                binding.noCustomerDats.visibility = View.VISIBLE
                            }


                            //setUpRecyclerView()

                            try {

                                if (!firstDetailLoad) {
                                    //loadFragment(data[0])
                                    for (i in data.indices) {
                                        if (data.get(i).isSelcted) {
                                            customerAdapter.isSelectedPos = i
                                            setSubTitleFirstLastName(data[i])
                                            loadFragment(data[i])
                                            isIn = true
                                            break
                                        }
                                    }
                                }
                                if (!isIn) {
                                    setSubTitleFirstLastName(data[0])
                                    loadFragment(data[0])
                                }

                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                        }

                    }
                    Status.LOADING -> {
                        if (!isFromSearch) {
                            ProgressUtils.showProgressDialog(requireActivity())
                        }
                    }
                    Status.ERROR -> {
                        if (!isFromSearch) {
                            ProgressUtils.dismissProgressDialog()
                        }
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
                        LogUtil.logE(TAG, "UpdateLoadList")
                        viewModel.customerList(data)

                    }

                }

            }

    }

    public fun setSubTitleFirstLastName(model: TbCustomer) {
        if (model.last_name != null && model.last_name!!.isNotEmpty() && !model.last_name.equals(
                "null",
                ignoreCase = true
            )
        ) {
            var final_string =
                model.first_name.toString().substring(0, 1)
                    .uppercase(Locale.getDefault()) + model.first_name.toString()
                    .substring(1, model.first_name.toString().length) + " " +
                        model.last_name.toString().substring(0, 1)
                            .uppercase(Locale.getDefault()) + model.last_name.toString()
                    .substring(1, model.last_name.toString().length)
            binding.layoutTool.txtSubTitle.setText(final_string)
        } else {
            var final_string =
                model.first_name.toString().substring(0, 1)
                    .uppercase(Locale.getDefault()) + model.first_name.toString()
                    .substring(1, model.first_name.toString().length)
            binding.layoutTool.txtSubTitle.setText(final_string)
        }
    }

    private fun searchQuery() {
        binding.autoSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.toString() == " ") {
                    binding.autoSearch.setText("")
                    isEmptyString = true
                }
            }

            override fun afterTextChanged(s: Editable?) {
                try {
                    if (s?.trim()?.isNotEmpty() == true) {
                        isEmptyString = false
                        searchByText(s?.trim().toString())
                    } else {
                        if (!isEmptyString) {
                            isEmptyString = true
                            isFromSearch = true
                            loadCustomerLocalList(1)
                        }
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

    private fun removeFragment() {
        val fragment: Fragment? =
            requireActivity().supportFragmentManager.findFragmentByTag("Customer")
        if (fragment != null) requireActivity().supportFragmentManager.beginTransaction()
            .remove(fragment)
            .commit()
    }

    private fun loadFragment(model: TbCustomer) {
        firstDetailLoad = true
        val frag = CustomerDetails.newInstance(model, isFromSearch)
        val fm: FragmentManager = requireActivity().supportFragmentManager
        fm.beginTransaction().replace(binding.frameContainer.id, frag, "Customer").commit()
    }


    private fun configureToolbar() {
        binding.layoutTool.txtTitle.text = "Customers"

        binding.layoutTool.imgDrawer.setOnClickListener {
            findNavController().navigate(R.id.action_customer_to_menuFragment2)
        }
        binding.layoutTool.txtHome.setOnClickListener {
            findNavController().navigate(R.id.action_customer_to_dashboardCategoryNew)
        }
        binding.layoutTool.imgOptionMenu.setImageResource(R.drawable.ic_add)

        binding.layoutTool.imgOptionMenu.setOnClickListener {

            val bundle: Bundle = bundleOf("isEdit" to false)
            findNavController().navigate(R.id.action_customer_to_addEditCustomer, bundle)

            // showDialog()
//            if (binding.linearCustomerDialog.visibility == View.VISIBLE) {
//                binding.linearCustomerDialog.visibility = View.GONE
//            } else {
//                binding.linearCustomerDialog.visibility = View.VISIBLE
//            }
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

                AlertUtils.showCustomAlertWithListenerWithOK(
                    requireContext(),
                    it.message
                ) { _, _ ->
                    if (customerAdapter.getList().size > 0) {
                        if (customerAdapter.getList().size - 1 == deletedPos) {
                            if (deletedPos == 0 && customerAdapter.getList().size > 0) {
                                customerAdapter.isSelectedPos = 0
                                val model = customerAdapter.getList()[0]
                                setSubTitleFirstLastName(model)
                                loadFragment(model)
                            } else {
                                binding.layoutTool.txtSubTitle.text = ""
                                removeFragment()
                            }
                        } else {
                            customerAdapter.isSelectedPos = 0
                            val model = customerAdapter.getList()[0]
                            setSubTitleFirstLastName(model)
                            loadFragment(model)
                        }
                    } else {
                        binding.layoutTool.txtSubTitle.text = ""
                        removeFragment()
                    }
                }


//
//                if (customerAdapter.getList().size - 1 != deletedPos) {
//                    if (customerAdapter.getList().lastIndex == deletedPos) {
//                        val model = deletedPos?.minus(1)
//                            ?.let { it1 -> customerAdapter.getList().get(it1) } as TbCustomer
//
//                        if (model.last_name != null && model.last_name.isNotEmpty() && !model.last_name.equals(
//                                "null",
//                                ignoreCase = true
//                            )
//                        ) {
//                            var final_string =
//                                model.first_name.toString().substring(0, 1)
//                                    .toUpperCase() + model.first_name.toString()
//                                    .substring(1, model.first_name.toString().length) + " " +
//                                        model.last_name.toString().substring(0, 1)
//                                            .toUpperCase() + model.last_name.toString()
//                                    .substring(1, model.last_name.toString().length)
//                            binding.layoutTool.txtSubTitle.setText(final_string)
//                        } else {
//                            var final_string =
//                                model.first_name.toString().substring(0, 1)
//                                    .toUpperCase() + model.first_name.toString()
//                                    .substring(1, model.first_name.toString().length)
//                            binding.layoutTool.txtSubTitle.setText(final_string)
//                        }
//
//                        loadFragment(model)
//                    } else {
//                        deletedPos = 0
//                        customerAdapter.isSelectedPos = 0
//                        val model = deletedPos?.let { it1 ->
//                            customerAdapter.getList().get(it1)
//                        } as TbCustomer
//                        if (model.last_name != null && model.last_name.isNotEmpty() && !model.last_name.equals(
//                                "null",
//                                ignoreCase = true
//                            )
//                        ) {
//                            var final_string =
//                                model.first_name.toString().substring(0, 1)
//                                    .toUpperCase() + model.first_name.toString()
//                                    .substring(1, model.first_name.toString().length) + " " +
//                                        model.last_name.toString().substring(0, 1)
//                                            .toUpperCase() + model.last_name.toString()
//                                    .substring(1, model.last_name.toString().length)
//                            binding.layoutTool.txtSubTitle.setText(final_string)
//                        } else {
//                            var final_string =
//                                model.first_name.toString().substring(0, 1)
//                                    .toUpperCase() + model.first_name.toString()
//                                    .substring(1, model.first_name.toString().length)
//                            binding.layoutTool.txtSubTitle.setText(final_string)
//                        }
//
//                        loadFragment(model)
//                    }
//
//                } else {
//                    val model = deletedPos?.minus(1)
//                        ?.let { it1 -> customerAdapter.getList().get(it1) } as TbCustomer
//                    if (model.last_name != null && model.last_name.isNotEmpty() && !model.last_name.equals(
//                            "null",
//                            ignoreCase = true
//                        )
//                    ) {
//                        var final_string =
//                            model.first_name.toString().substring(0, 1)
//                                .toUpperCase() + model.first_name.toString()
//                                .substring(1, model.first_name.toString().length) + " " +
//                                    model.last_name.toString().substring(0, 1)
//                                        .toUpperCase() + model.last_name.toString()
//                                .substring(1, model.last_name.toString().length)
//                        binding.layoutTool.txtSubTitle.setText(final_string)
//                    } else {
//                        var final_string =
//                            model.first_name.toString().substring(0, 1)
//                                .toUpperCase() + model.first_name.toString()
//                                .substring(1, model.first_name.toString().length)
//                        binding.layoutTool.txtSubTitle.setText(final_string)
//                    }
//
//                    loadFragment(model)
//                }


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
                setSubTitleFirstLastName(model)
                loadFragment(model)
            }

        })

        binding.rvEmployeeList.adapter = customerAdapter

        customerAdapter.setCallback(this)

    }

    override fun onItemClickListener(view: View?, pos: Int) {
        val popupMenu = view?.let { PopupMenu(requireContext(), it) }
        popupMenu?.menuInflater?.inflate(R.menu.edit_delete__hide_menu, popupMenu.menu)
        popupMenu?.menu?.findItem(R.id.menu_edit)?.isVisible = false
        popupMenu?.menu?.findItem(R.id.menu_hide)?.isVisible = false
        popupMenu?.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_delete -> {
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




                    LogUtil.logE(TAG, "posClicked  ${pos}")
                }
            }
            true
        }
        popupMenu?.show()
    }


}