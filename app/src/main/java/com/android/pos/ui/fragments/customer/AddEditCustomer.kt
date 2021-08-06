package com.android.pos.ui.fragments.customer

import `in`.madapps.placesautocomplete.PlaceAPI
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.data.model.requestModel.CreateCustomerRequestModel
import com.android.pos.databinding.FragmentAddEditCustomerBinding
import com.android.pos.ui.adapter.AddressListAdapter
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.liveSnackBar
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


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

        return binding.root
    }

    private fun setAddress() {

        adapter = AddressListAdapter()
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

        if (isEdit) {
            binding.txtCustomerType.setText("Edit Customer")

            val editModel: com.android.pos.data.model.CustomerListResponse.Data? =
                requireArguments().getParcelable<com.android.pos.data.model.CustomerListResponse.Data>(
                    "dataModel"
                )

            if (editModel?.id != null) {
                viewModel.isEditData(isEdit, editModel?.id!!)
            }

            Log.e(TAG, "editModel  ${Gson().toJson(editModel)}")
            viewModel.addCustomerDetails.value?.data?.first_name = editModel?.first_name.toString()
            viewModel.addCustomerDetails.value?.data?.last_name = editModel?.last_name.toString()

            Log.e(TAG, "Date  ${getDay(editModel?.birth_date!!)}")
            Log.e(TAG, "Month  ${getMonth(editModel?.birth_date!!)}")
            Log.e(TAG, "Year  ${getYear(editModel?.birth_date!!)}")
            viewModel.addCustomerDetails.value?.data?.birth_day = getDay(editModel?.birth_date!!)
            viewModel.addCustomerDetails.value?.data?.birthday_year =
                getYear(editModel?.birth_date!!)
            viewModel.addCustomerDetails.value?.data?.birth_month =
                getMonth(editModel?.birth_date!!)



            if (editModel?.phones?.size != 0) {
                viewModel.phoneNo.value =
                    AlertUtils.usNumberFormat(editModel?.phones?.get(0)?.phone_number!!).toString()
            }
            if (editModel.email != null) {
                viewModel.addCustomerDetails.value?.data?.email = editModel?.email
            }

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


            binding.edtCompany.setText("company")
            if (editModel.birth_date != null) {
                binding.edtBirthDay.setText("${editModel.birth_date}")
            }

        } else {
            binding.txtCustomerType.setText("New Customer")
            adapter.addData(modelAddress)
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
            adapter.addData(
                modelAddress
            )

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