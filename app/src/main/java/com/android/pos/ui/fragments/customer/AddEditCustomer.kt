package com.android.pos.ui.fragments.customer

import `in`.madapps.placesautocomplete.PlaceAPI
import `in`.madapps.placesautocomplete.adapter.PlacesAutoCompleteAdapter
import `in`.madapps.placesautocomplete.listener.OnPlacesDetailsListener
import `in`.madapps.placesautocomplete.model.Place
import `in`.madapps.placesautocomplete.model.PlaceDetails
import android.R
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.location.Geocoder
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


        //edit.setFilters(new InputFilter[] { filter })


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
        }, requireContext())
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
//        modelAddress = CreateCustomerRequestModel.Customer.Addresses(
//            null,
//            "",
//            "",
//            "",
//            "",
//            "",
//            "",
//            "",
//            0.0,
//            0.0,
//        )

//        setAddress()
        onClick()
        setPlaceApi()
        isEdit = requireArguments().getBoolean("isEdit", false)
        Log.e(TAG, "isEdit  $isEdit")

        binding.chkIsLoyalty.setOnClickListener {
            viewModel.enroll_to_loyalty.value = binding.chkIsLoyalty.isChecked
        }
        binding.header.txtTitle.text = getString(com.android.pos.R.string.add_new_customer)
        binding.header.txtSave.text = getString(com.android.pos.R.string.save)
        setCountryAddress()
        if (isEdit) {
            listAddress = arrayListOf()
            binding.header.txtTitle.text = getString(com.android.pos.R.string.update_customer)
            binding.header.txtSave.text = getString(com.android.pos.R.string.update)

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



            viewModel.addCustomerDetails.value?.data?.enroll_to_loyalty?.let {
                binding.chkIsLoyalty.isChecked = it
            }


            /*Log.e(TAG, "Date  ${getDay(editModel?.birth_date!!)}")
            Log.e(TAG, "Month  ${getMonth(editModel?.birth_date!!)}")
            Log.e(TAG, "Year  ${getYear(editModel?.birth_date!!)}")*/
            viewModel.addCustomerDetails.value?.data?.birth_day =
                if (editModel?.birth_date?.isNotEmpty() == true) {
                    editModel?.birth_date?.let {
                        getDay(
                            it
                        )
                    }
                } else {
                    ""

                }
            if (editModel?.birth_date != null && editModel?.birth_date?.isNotEmpty())
                viewModel.addCustomerDetails.value?.data?.birthday_year =
                    getYear(editModel.birth_date)

            if (editModel?.birth_date != null && editModel?.birth_date?.isNotEmpty())
                viewModel.addCustomerDetails.value?.data?.birth_month =
                    getMonth(editModel.birth_date)



            if (editModel?.phones?.size != 0) {
                viewModel.phoneNo.value =
                    AlertUtils.usNumberFormat(editModel?.phones?.get(0)?.phone_number!!).toString()
                viewModel.phoneId = editModel.phones[0].id
            }
            viewModel.addCustomerDetails.value?.data?.email = editModel.email




            if (editModel.addresses.isNotEmpty()) {
                editModel.addresses.forEach {
                    listAddress.add(
                        CreateCustomerRequestModel.Customer.Addresses(
                            it.id,
                            it.address1,
                            it.address2,
                            it.city,
                            it.state,
                            it.country,
                            it.postcode,
                            it.type_of_address,
                            0.0,
                            0.0,
                            "false"
                        )
                    )
                }
                viewModel.setAddressList(listAddress)
                binding.edtStreet?.setText(editModel.addresses[0].address1)
                binding.edtSuite?.setText(editModel.addresses[0].address2)
                binding.edtCity?.setText(editModel.addresses[0].city)
                binding.edtState?.setText(editModel.addresses[0].state)
                binding.edtZip?.setText(editModel.addresses[0].postcode)
                if (editModel.addresses[0].country == "United States") {
                    binding.edtAddress?.setSelection(0)
                } else {
                    binding.edtAddress?.setSelection(1)
                }
                if (editModel.addresses[1] != null) {
                    binding.edtStreetDel?.setText(editModel.addresses[1].address1)
                    binding.edtSuiteDel?.setText(editModel.addresses[1].address2)
                    binding.edtCityDel?.setText(editModel.addresses[1].city)
                    binding.edtStateDel?.setText(editModel.addresses[1].state)
                    binding.edtZipDel?.setText(editModel.addresses[1].postcode)
                    if (editModel.addresses[1].country == "United States") {
                        binding.edtAddressDel?.setSelection(0)
                    } else {
                        binding.edtAddressDel?.setSelection(1)
                    }
                }

            }


            viewModel.addCustomerDetails.value?.data?.company = editModel.company ?: ""
            //binding.edtCompany.setText(editModel.company)
            if (editModel.birth_date != null && editModel.birth_date?.isNotEmpty()) {
                val inputFormat = SimpleDateFormat("MM/dd/yyyy")
                var date = inputFormat.parse(editModel.birth_date)
                val outputFormat = SimpleDateFormat("MMM-dd-yyyy")
                val formattedDate = outputFormat.format(date)
                binding.edtBirthDay.text = formattedDate
            }

        }

        binding.header.imgBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.edtBirthDay.setOnClickListener {
            Log.e(TAG, "DatePicker  ")
            showDatePicker()

        }

    }

    private fun decodeLocation(lat: Double, lng: Double, place: String) {

    }

    private fun setPlaceApi() {
        placesApi =
            PlaceAPI.Builder()
                .apiKey(binding.root.context.getString(com.android.pos.R.string.api_key))
                .build(binding.root.context)

        binding.edtStreet?.setAdapter(PlacesAutoCompleteAdapter(binding.root.context, placesApi))
        binding.edtStreet?.setOnItemClickListener { parent, view, position, id ->
            val place = parent.getItemAtPosition(position) as Place

            //binding.edtStreet.setText("${place.description}")
            placesApi.fetchPlaceDetails(place.id, object : OnPlacesDetailsListener {
                override fun onError(errorMessage: String) {
                }

                override fun onPlaceDetailsFetched(placeDetails: PlaceDetails) {

                    decodeLocation(placeDetails.lat, placeDetails.lng, placeDetails.name)

                    val gcd = Geocoder(requireContext(), Locale.getDefault())
                    /* val address: List<Address> =
                         gcd.getFromLocation(placeDetails.lat, placeDetails.lng, 1)*/

                    var street = ""
                    var suite = ""
                    var city = ""
                    var state = ""
                    var zip = ""
                    placeDetails.address.forEach {
                        it.type.forEach { type ->
                            if (type.trim().lowercase() == "street_number".trim().lowercase()) {
                                street += it.longName
                            } else if (type.trim().lowercase() == "route".trim().lowercase()) {
                                street += it.longName
                            } else if (type.trim().lowercase() == "neighborhood".trim()
                                    .lowercase()
                            ) {
                                suite = it.longName
                            } else if (type.trim().lowercase() == "locality".trim()
                                    .lowercase()
                            ) {
                                city = it.longName
                            } else if (type.trim()
                                    .lowercase() == "administrative_area_level_1".trim()
                                    .lowercase()
                            ) {
                                state = it.longName
                            } else if (type.trim().lowercase() == "postal_code".trim()
                                    .lowercase()
                            ) {
                                zip = it.longName
                            }

                        }

                    }


                    if (placeDetails.address.isNotEmpty()) {
                        try {
                            binding.edtStreet?.setText(street)
                            binding.edtSuite?.setText(suite)
                            binding.edtCity?.setText(city)
                            binding.edtState?.setText(state)
                            binding.edtZip?.setText(zip)
                            binding.edtStreet?.dismissDropDown()
                        } catch (e: Exception) {
                            Log.e(TAG, "exception in pplaces api")
                        } finally {
                            binding.edtStreet?.dismissDropDown()
                            Log.e(TAG, "notify callback")
                        }
                    }

                    Log.e(TAG, "placeDetails:  ${Gson().toJson(placeDetails.name)}")

                }

            })
        }

        binding.edtStreetDel?.setAdapter(PlacesAutoCompleteAdapter(binding.root.context, placesApi))
        binding.edtStreetDel?.setOnItemClickListener { parent, view, position, id ->
            val place = parent.getItemAtPosition(position) as Place

            //binding.edtStreet.setText("${place.description}")
            placesApi.fetchPlaceDetails(place.id, object : OnPlacesDetailsListener {
                override fun onError(errorMessage: String) {
                }

                override fun onPlaceDetailsFetched(placeDetails: PlaceDetails) {

                    decodeLocation(placeDetails.lat, placeDetails.lng, placeDetails.name)

                    val gcd = Geocoder(requireContext(), Locale.getDefault())
                    /* val address: List<Address> =
                         gcd.getFromLocation(placeDetails.lat, placeDetails.lng, 1)*/

                    var street = ""
                    var suite = ""
                    var city = ""
                    var state = ""
                    var zip = ""
                    placeDetails.address.forEach {
                        it.type.forEach { type ->
                            if (type.trim().lowercase() == "street_number".trim().lowercase()) {
                                street += it.longName
                            } else if (type.trim().lowercase() == "route".trim().lowercase()) {
                                street += it.longName
                            } else if (type.trim().lowercase() == "neighborhood".trim()
                                    .lowercase()
                            ) {
                                suite = it.longName
                            } else if (type.trim().lowercase() == "locality".trim()
                                    .lowercase()
                            ) {
                                city = it.longName
                            } else if (type.trim()
                                    .lowercase() == "administrative_area_level_1".trim()
                                    .lowercase()
                            ) {
                                state = it.longName
                            } else if (type.trim().lowercase() == "postal_code".trim()
                                    .lowercase()
                            ) {
                                zip = it.longName
                            }

                        }

                    }


                    if (placeDetails.address.isNotEmpty()) {
                        try {
                            binding.edtStreetDel?.setText(street)
                            binding.edtSuiteDel?.setText(suite)
                            binding.edtCityDel?.setText(city)
                            binding.edtStateDel?.setText(state)
                            binding.edtZipDel?.setText(zip)
                            binding.edtStreetDel?.dismissDropDown()
                        } catch (e: Exception) {
                            Log.e(TAG, "exception in pplaces api")
                        } finally {
                            binding.edtStreetDel?.dismissDropDown()
                            Log.e(TAG, "notify callback")
                        }
                    }

                    Log.e(TAG, "placeDetails:  ${Gson().toJson(placeDetails.name)}")

                }

            })

        }

    }

    private fun setCountryAddress() {
        val adapter =
            ArrayAdapter(requireContext(), R.layout.simple_spinner_item, country)
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)

        binding.edtAddress?.adapter = adapter
        binding.edtAddress?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
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
        val adapter1 =
            ArrayAdapter(requireContext(), R.layout.simple_spinner_item, country)
        adapter1.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)

        binding.edtAddressDel?.adapter = adapter
        binding.edtAddressDel?.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
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

    @SuppressLint("NotifyDataSetChanged")
    private fun onClick() {
//        binding.imgAddressAdd.setOnClickListener {
//
//            Log.e(TAG, "adapterGetAddress  ${Gson().toJson(adapter.getList())}")
//            if (adapter.getList().isEmpty()) {
//                modelAddress = CreateCustomerRequestModel.Customer.Addresses()
//                modelAddress.apply {
//                    latitude = 0.0
//                    longitude = 0.0
//                }
//                adapter.addData(
//                    modelAddress
//                )
//
//            } else if (adapter.getList()[adapter.getList().size - 1].address1.isNotEmpty() || adapter.getList()[adapter.getList().size - 1].city.isNotEmpty() || adapter.getList()
//                    .get(adapter.getList().size - 1)._destroy == "true"
//            ) {
//                Log.d("yash", "onClick: " + adapter.getList()[adapter.getList().size - 1].address1)
//                modelAddress = CreateCustomerRequestModel.Customer.Addresses()
//                modelAddress.apply {
//                    latitude = 0.0
//                    longitude = 0.0
//                }
//                adapter.addData(
//                    modelAddress
//                )
//            }
//
//        }

        binding.header.txtSave.setOnClickListener {
            if (isEdit) {
                var id1: Int? = null
                var id2: Int? = null

                if (viewModel.listAddress.size > 0) {
                    id1 = viewModel.listAddress[0].id!!
                    id2 = viewModel.listAddress[1].id!!
                }
                listAddress = arrayListOf()


                listAddress.add(
                    CreateCustomerRequestModel.Customer.Addresses(
                        id1,
                        binding.edtStreet?.text.toString(),
                        binding.edtStreet?.text.toString(),
                        binding.edtCity?.text.toString(),
                        binding.edtState?.text.toString(),
                        binding.edtAddress?.selectedItem.toString(),
                        binding.edtZip?.text.toString(),
                        "Billing",
                        0.0,
                        0.0,
                        "false"
                    )
                )
                listAddress.add(
                    CreateCustomerRequestModel.Customer.Addresses(
                        id2,
                        binding.edtStreetDel?.text.toString(),
                        binding.edtStreetDel?.text.toString(),
                        binding.edtCityDel?.text.toString(),
                        binding.edtStateDel?.text.toString(),
                        binding.edtAddressDel?.selectedItem.toString(),
                        binding.edtZipDel?.text.toString(),
                        "Shipping",
                        0.0,
                        0.0,
                        "false"
                    )
                )
            } else {
                listAddress = arrayListOf()
                if (binding.edtStreet?.text.toString().isNotEmpty())
                    listAddress.add(
                        CreateCustomerRequestModel.Customer.Addresses(
                            null,
                            binding.edtStreet?.text.toString(),
                            binding.edtStreet?.text.toString(),
                            binding.edtCity?.text.toString(),
                            binding.edtState?.text.toString(),
                            binding.edtAddress?.selectedItem.toString(),
                            binding.edtZip?.text.toString(),
                            "Billing",
                            0.0,
                            0.0,
                            "false"
                        )
                    )
                if (binding.edtStreetDel?.text.toString().isNotEmpty())
                    listAddress.add(
                        CreateCustomerRequestModel.Customer.Addresses(
                            null,
                            binding.edtStreetDel?.text.toString(),
                            binding.edtStreetDel?.text.toString(),
                            binding.edtCityDel?.text.toString(),
                            binding.edtStateDel?.text.toString(),
                            binding.edtAddressDel?.selectedItem.toString(),
                            binding.edtZipDel?.text.toString(),
                            "Shipping",
                            0.0,
                            0.0,
                            "false"
                        )
                    )
            }

            viewModel.submit(listAddress)
        }
    }


    private fun showDatePicker() {
        val c = Calendar.getInstance();
        val mYear = c.get(Calendar.YEAR);
        val mMonth = c.get(Calendar.MONTH);
        val mDay = c.get(Calendar.DAY_OF_MONTH)

        val datePicker: DatePickerDialog =
            DatePickerDialog(
                requireContext(),
                android.R.style.Theme_Material_Light_Dialog,
                object : DatePickerDialog.OnDateSetListener {
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


                        val calendar = Calendar.getInstance()
                        calendar.set(year, monthOfYear, dayOfMonth)
                        val outputFormat = SimpleDateFormat("MMM-dd-yyyy")
                        var datestring = outputFormat.format(calendar.time)

                        binding.edtBirthDay.text = datestring

                    }

                },
                mYear,
                mMonth,
                mDay
            )
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
                        baseResponse.message,
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