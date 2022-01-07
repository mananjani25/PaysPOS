package com.android.pos.ui.fragments.customer

import `in`.madapps.placesautocomplete.PlaceAPI
import android.R
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.DatePicker
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.data.entities.TbCustomer
import com.android.pos.data.model.requestModel.CreateCustomerRequestModel
import com.android.pos.databinding.FragmentAddEditCustomerBinding
import com.android.pos.ui.adapter.AddressListAdapter
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.liveSnackBar
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*


@AndroidEntryPoint
class AddEditCustomer : Fragment() {
    private lateinit var binding: FragmentAddEditCustomerBinding
    private var isEdit = false
    private val TAG = "AddEditCustomer"
    private val viewModel by viewModels<AddCustomerViewModel>()
    private var currentSelectedDate: Long? = null
    private lateinit var placesApi: PlaceAPI
    private var listAddress: ArrayList<CreateCustomerRequestModel.Customer.Addresses> =
        arrayListOf()

    private lateinit var modelAddress: CreateCustomerRequestModel.Customer.Addresses
    private lateinit var adapter: AddressListAdapter
    private var country = arrayOf("United States", "Canada")


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAddEditCustomerBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        setUpSnackBar()
        showObserveProgress()
        navigate()
        setPhoneCountry()

        return binding.root
    }

    private fun setPhoneCountry() {
        val adapter =
            ArrayAdapter(requireContext(), R.layout.simple_spinner_item, country)
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)

        binding.edtCountry.adapter = adapter
        binding.edtCountry.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {

                if (Build.VERSION.SDK_INT < 23) {
                    (parent?.getChildAt(0) as TextView).setTextAppearance(
                        view?.context,
                        com.android.pos.R.style.SpinnerTheme
                    )
                } else {
                    (parent?.getChildAt(0) as TextView).setTextAppearance(com.android.pos.R.style.SpinnerTheme); }


            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

        }
    }

    private fun setAddress() {

        adapter = AddressListAdapter(refreshCallBack = { adapterPos ->
            Log.e(TAG, "callback")
            if (::adapter.isInitialized) {
                Log.e(TAG, "notify list")
                activity?.runOnUiThread {
                    adapter.notifyItemChanged(adapterPos)
                }
            }
        })
        binding.rvAddresses.adapter = adapter

    }

    private fun showObserveProgress() {
        viewModel.showProgress.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }

        })
    }

    private fun setUpSnackBar() {
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        modelAddress = CreateCustomerRequestModel.Customer.Addresses(
            null,
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            0.0,
            0.0,
        )

        setAddress()
        onClick()
        isEdit = requireArguments().getBoolean("isEdit", false)
        Log.e(TAG, "isEdit  $isEdit")

        binding.chkIsLoyalty.setOnClickListener {
            viewModel.enroll_to_loyalty.value = binding.chkIsLoyalty.isChecked
        }
        if (isEdit) {
            binding.txtCustomerType.text = getString(com.android.pos.R.string.update_customer)
            binding.txtSave.text = getString(com.android.pos.R.string.update)

            val editModel: TbCustomer? =
                requireArguments().getParcelable<TbCustomer>(
                    "dataModel"
                )

            if (editModel?.id != null) {
                viewModel.isEditData(isEdit, editModel?.id!!)
            }

            viewModel.addCustomerDetails.value?.data?.first_name = editModel?.first_name.toString()
            viewModel.addCustomerDetails.value?.data?.last_name = editModel?.last_name.toString()
            viewModel.addCustomerDetails.value?.data?.enroll_to_loyalty =
                editModel?.enroll_to_loyalty


            binding.chkIsLoyalty.isChecked =
                viewModel.addCustomerDetails.value?.data?.enroll_to_loyalty == true


            /*Log.e(TAG, "Date  ${getDay(editModel?.birth_date!!)}")
            Log.e(TAG, "Month  ${getMonth(editModel?.birth_date!!)}")
            Log.e(TAG, "Year  ${getYear(editModel?.birth_date!!)}")*/
            viewModel.addCustomerDetails.value?.data?.birth_day = editModel?.birth_date?.let {
                getDay(
                    it
                )
            }
            if (editModel?.birth_date != null)
                viewModel.addCustomerDetails.value?.data?.birthday_year =
                    getYear(editModel.birth_date)

            if (editModel?.birth_date != null)
                viewModel.addCustomerDetails.value?.data?.birth_month =
                    getMonth(editModel.birth_date)



            if (editModel?.phones?.size != 0) {
                viewModel.phoneNo.value =
                    AlertUtils.usNumberFormat(editModel?.phones?.get(0)?.phone_number!!).toString()
                viewModel.phoneId = editModel.phones[0].id
            }
            viewModel.addCustomerDetails.value?.data?.email = editModel.email

            if (editModel.addresses.isNotEmpty()) {
                var list: ArrayList<CreateCustomerRequestModel.Customer.Addresses> = arrayListOf()

                for (i in 0 until editModel.addresses.size) {
                    list.add(
                        CreateCustomerRequestModel.Customer.Addresses(
                            editModel.addresses.get(i).id,
                            editModel.addresses.get(i).address1,
                            editModel.addresses.get(i).address2,
                            editModel.addresses.get(i).city,
                            editModel.addresses.get(i).state,
                            editModel.addresses.get(i).country,
                            editModel.addresses.get(i).postcode,
                            editModel.addresses.get(i).type_of_address.toString(),
                            0.0,
                            0.0,
                        )
                    )

                }


                adapter.setAddress(list)
            }


            viewModel.addCustomerDetails.value?.data?.company = editModel.company ?: ""
            //binding.edtCompany.setText(editModel.company)
            if (editModel.birth_date != null) {
                binding.edtBirthDay.setText("${editModel.birth_date}")
            }

        } else {
            binding.txtCustomerType.text = "New Customer"
            var list: ArrayList<CreateCustomerRequestModel.Customer.Addresses> = arrayListOf()
            var model = CreateCustomerRequestModel.Customer.Addresses()
            model.apply {
                latitude = 0.0
                longitude = 0.0
            }
            list.add(model)

            adapter.setAddress(list)
            viewModel.setAddressList(adapter.getList())

        }

        binding.imgBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.edtBirthDay.setOnClickListener {
            Log.e(TAG, "DatePicker  ")
            showDatePicker()

        }

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun onClick() {
        binding.imgAddressAdd.setOnClickListener {

            if (adapter.getList().isEmpty()) {
                modelAddress = CreateCustomerRequestModel.Customer.Addresses()
                modelAddress.apply {
                    latitude = 0.0
                    longitude = 0.0
                }
                adapter.addData(
                    modelAddress
                )

            } else if (adapter.getList()[adapter.getList().size - 1].address1.isNotEmpty()) {
                modelAddress = CreateCustomerRequestModel.Customer.Addresses()
                modelAddress.apply {
                    latitude = 0.0
                    longitude = 0.0
                }
                adapter.addData(
                    modelAddress
                )
            }

        }

        binding.txtSave.setOnClickListener {
            viewModel.setAddressList(adapter.getList())
            viewModel.submit()
        }
    }


    private fun showDatePicker() {
        val c = Calendar.getInstance();
        val mYear = c.get(Calendar.YEAR);
        val mMonth = c.get(Calendar.MONTH);
        val mDay = c.get(Calendar.DAY_OF_MONTH)

        val datePicker: DatePickerDialog =
            DatePickerDialog(requireContext(), object : DatePickerDialog.OnDateSetListener {
                override fun onDateSet(
                    view: DatePicker?,
                    year: Int,
                    monthOfYear: Int,
                    dayOfMonth: Int
                ) {
                    viewModel.addCustomerDetails.value?.data?.birth_day = dayOfMonth.toString()
                    viewModel.addCustomerDetails.value?.data?.birth_month =
                        (monthOfYear + 1).toString()
                    viewModel.addCustomerDetails.value?.data?.birthday_year = year.toString()


                    val mon = (monthOfYear + 1)
                    binding.edtBirthDay.text = "" + mon + "/" + dayOfMonth + "/" + year


                }

            }, mYear, mMonth, mDay)
        datePicker.datePicker.maxDate = System.currentTimeMillis()
        datePicker.show()
        Log.e(TAG, "DatePickerInside  ")
    }

    private fun navigate() {

        Log.e(TAG, "POPBACKCUSTOMER")
        viewModel._Basedata.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let { baseResponse ->
                activity?.let {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        it,
                        baseResponse.message.toString(),
                    )
                    { _, _ ->

                        val navControll = findNavController()
                        navControll.previousBackStackEntry?.savedStateHandle?.set(
                            com.android.pos.data.remote.Constants.KEY,
                            com.android.pos.data.remote.Constants.CUSTOMERDETAILS
                        )
                        navControll.popBackStack()
                    }


                }
            }
        })

    }

    fun getDay(dat: String): String {
        val format = SimpleDateFormat("dd/MM/yyyy")
        val date = format.parse(dat)
        return android.text.format.DateFormat.format("dd", date).toString()
    }

    fun getMonth(dat: String): String {
        val format = SimpleDateFormat("dd/MM/yyyy")
        val date = format.parse(dat)
        return android.text.format.DateFormat.format("MM", date).toString()
    }

    fun getYear(dat: String): String {
        val format = SimpleDateFormat("dd/MM/yyyy")
        val date = format.parse(dat)
        return android.text.format.DateFormat.format("yyyy", date).toString()
    }
}