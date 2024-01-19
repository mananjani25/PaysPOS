package com.pays.pos.ui.fragments.customer

import android.R
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.DatePicker
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.pays.pos.data.entities.CartModel
import com.pays.pos.data.entities.TbCartItem
import com.pays.pos.data.entities.TbCustomer
import com.pays.pos.data.entities.TbItem
import com.pays.pos.data.model.requestModel.CreateCustomerRequestModel
import com.pays.pos.data.remote.Constants
import com.pays.pos.databinding.FragmentAddEditCustomerBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.ui.adapter.AddressListAdapter
import com.pays.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.pays.pos.ui.fragments.settings.business.AutoCompleteAdapter
import com.pays.pos.utils.*
import com.pays.pos.utils.callback.AddressTextChangeListner
import com.pays.pos.utils.extensions.gone
import com.pays.pos.utils.extensions.liveSnackBar
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject


@AndroidEntryPoint
class AddEditCustomer : Fragment(), AddressTextChangeListner {

    @Inject
    lateinit var prefProvider: PrefProvider
    private val dashboardViewModel by activityViewModels<DashBoardCategoryViewModel>()

    private lateinit var binding: FragmentAddEditCustomerBinding
    private var isEdit = false
    private var isFromPhoneOrderEdit = false
    private val TAG = "AddEditCustomer"
    private val viewModel by viewModels<AddCustomerViewModel>()
    private var currentSelectedDate: Long? = null

    //    private lateinit var placesApi: PlaceAPI
    private var listAddress: ArrayList<CreateCustomerRequestModel.Customer.Addresses> =
        arrayListOf()
    private lateinit var modelAddress: CreateCustomerRequestModel.Customer.Addresses
    private lateinit var adapter: AddressListAdapter
    private var country = arrayOf("United States", "Canada")

    var placesClient: PlacesClient? = null
    var adapter1: _root_ide_package_.com.pays.pos.ui.fragments.settings.business.AutoCompleteAdapter? = null
    var adapter2: _root_ide_package_.com.pays.pos.ui.fragments.settings.business.AutoCompleteAdapter? = null
    var changeField: Boolean = false

    @SuppressLint("ClickableViewAccessibility")
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




        binding.edtStreet?.setOnTouchListener { view, event ->
            binding.nestedScrollView?.smoothScrollTo(500, 500)
            false
        }

        binding.edtStreetDel?.setOnTouchListener { view, event ->
            binding.nestedScrollView?.smoothScrollTo(500, 500)
            false
        }


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
                        com.pays.pos.R.style.SpinnerTheme1
                    )
                } else {
                    try {
                        (parent?.getChildAt(0) as TextView).setTextAppearance(com.pays.pos.R.style.SpinnerTheme1);
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

        }
    }

    private fun setAddress() {

        adapter = AddressListAdapter(refreshCallBack = { adapterPos ->
            LogUtil.logE(TAG, "callback")
            if (::adapter.isInitialized) {
                LogUtil.logE(TAG, "notify list")
                activity?.runOnUiThread {
                    adapter.notifyItemChanged(adapterPos)
                }
            }
        }, requireContext())
        binding.rvAddresses.adapter = adapter

    }

    private fun showObserveProgress() {
        viewModel.showProgress.observe(viewLifecycleOwner) { event ->
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
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onClick()
        setPlaceApi()
        isEdit = requireArguments().getBoolean("isEdit", false)
        isFromPhoneOrderEdit = requireArguments().getBoolean("isFromPhoneOrderEdit", false)
        LogUtil.logE(TAG, "isEdit  $isEdit")

        binding.chkIsLoyalty.setOnClickListener {
            viewModel.enroll_to_loyalty.value = binding.chkIsLoyalty.isChecked
        }
        binding.header.txtTitle.text = getString(com.pays.pos.R.string.add_new_customer)
        binding.header.txtSave.text = getString(com.pays.pos.R.string.save)
        setCountryAddress()
        if (isEdit) {
            listAddress = arrayListOf()
            binding.header.txtTitle.text = getString(com.pays.pos.R.string.update_customer)
            binding.header.txtSave.text = getString(com.pays.pos.R.string.update)

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

            if (editModel?.addresses?.isNotEmpty() == true) {
                if (editModel.addresses.size == 2) {
                    if (editModel.addresses[0].full_address.contentEquals(editModel.addresses[1].full_address)) {
                        viewModel.addCustomerDetails.value?.data?.same_as_billing_address =
                            editModel?.same_as_billing_address

                        viewModel.addCustomerDetails.value?.data?.same_as_billing_address?.let {
                            binding.chksameasbilling.isChecked = it
                        }
                    } else {
                        viewModel.addCustomerDetails.value?.data?.same_as_billing_address =
                            false

                        viewModel.addCustomerDetails.value?.data?.same_as_billing_address?.let {
                            binding.chksameasbilling.isChecked = it
                        }
                    }
                }
            }


            /*LogUtil.logE(TAG, "Date  ${getDay(editModel?.birth_date!!)}")
            LogUtil.logE(TAG, "Month  ${getMonth(editModel?.birth_date!!)}")
            LogUtil.logE(TAG, "Year  ${getYear(editModel?.birth_date!!)}")*/
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
                if (editModel.addresses.size == 1) {
                    listAddress.add(
                        CreateCustomerRequestModel.Customer.Addresses(
                            editModel.addresses[0].id,
                            editModel.addresses[0].address1,
                            editModel.addresses[0].address2,
                            editModel.addresses[0].city,
                            editModel.addresses[0].state,
                            editModel.addresses[0].country,
                            editModel.addresses[0].postcode,
                            editModel.addresses[0].type_of_address,
                            0.0,
                            0.0,
                            "false"
                        )
                    )
                } else if (editModel.addresses.size == 2) {
                    if (editModel.addresses[0].type_of_address == "Shipping") {
                        listAddress.add(
                            CreateCustomerRequestModel.Customer.Addresses(
                                editModel.addresses[0].id,
                                editModel.addresses[0].address1,
                                editModel.addresses[0].address2,
                                editModel.addresses[0].city,
                                editModel.addresses[0].state,
                                editModel.addresses[0].country,
                                editModel.addresses[0].postcode,
                                editModel.addresses[0].type_of_address,
                                0.0,
                                0.0,
                                "false"
                            )
                        )
                        listAddress.add(
                            CreateCustomerRequestModel.Customer.Addresses(
                                editModel.addresses[1].id,
                                editModel.addresses[1].address1,
                                editModel.addresses[1].address2,
                                editModel.addresses[1].city,
                                editModel.addresses[1].state,
                                editModel.addresses[1].country,
                                editModel.addresses[1].postcode,
                                editModel.addresses[1].type_of_address,
                                0.0,
                                0.0,
                                "false"
                            )
                        )
                    } else {
                        listAddress.add(
                            CreateCustomerRequestModel.Customer.Addresses(
                                editModel.addresses[1].id,
                                editModel.addresses[1].address1,
                                editModel.addresses[1].address2,
                                editModel.addresses[1].city,
                                editModel.addresses[1].state,
                                editModel.addresses[1].country,
                                editModel.addresses[1].postcode,
                                editModel.addresses[1].type_of_address,
                                0.0,
                                0.0,
                                "false"
                            )
                        )
                        listAddress.add(
                            CreateCustomerRequestModel.Customer.Addresses(
                                editModel.addresses[0].id,
                                editModel.addresses[0].address1,
                                editModel.addresses[0].address2,
                                editModel.addresses[0].city,
                                editModel.addresses[0].state,
                                editModel.addresses[0].country,
                                editModel.addresses[0].postcode,
                                editModel.addresses[0].type_of_address,
                                0.0,
                                0.0,
                                "false"
                            )
                        )
                    }
                }
                viewModel.setAddressList(listAddress)
                if (editModel.addresses.isNotEmpty()) {
                    if (editModel.addresses.size == 1) {
                        if (editModel.addresses[0].type_of_address == "Shipping") {
                            binding.edtStreet.setText(editModel.addresses[0].address1)
                            binding.edtSuite.setText(editModel.addresses[0].address2)
                            binding.edtCity.setText(editModel.addresses[0].city)
                            binding.edtState.setText(editModel.addresses[0].state)
                            binding.edtZip.setText(editModel.addresses[0].postcode)
                            if (editModel.addresses[0].country == "United States") {
                                binding.edtAddress.setSelection(0)
                            } else {
                                binding.edtAddress.setSelection(1)
                            }
                        }
                    } else if (editModel.addresses.size == 2) {
                        if (editModel.addresses[0].type_of_address == "Shipping") {
                            binding.edtStreet.setText(editModel.addresses[0].address1)
                            binding.edtSuite.setText(editModel.addresses[0].address2)
                            binding.edtCity.setText(editModel.addresses[0].city)
                            binding.edtState.setText(editModel.addresses[0].state)
                            binding.edtZip.setText(editModel.addresses[0].postcode)
                            if (editModel.addresses[0].country == "United States") {
                                binding.edtAddress.setSelection(0)
                            } else {
                                binding.edtAddress.setSelection(1)
                            }

                            binding.edtStreetDel.setText(editModel.addresses[1].address1)
                            binding.edtSuiteDel.setText(editModel.addresses[1].address2)
                            binding.edtCityDel.setText(editModel.addresses[1].city)
                            binding.edtStateDel.setText(editModel.addresses[1].state)
                            binding.edtZipDel.setText(editModel.addresses[1].postcode)
                            if (editModel.addresses[1].country == "United States") {
                                binding.edtAddressDel.setSelection(0)
                            } else {
                                binding.edtAddressDel.setSelection(1)
                            }
                        } else {
                            binding.edtStreet.setText(editModel.addresses[1].address1)
                            binding.edtSuite.setText(editModel.addresses[1].address2)
                            binding.edtCity.setText(editModel.addresses[1].city)
                            binding.edtState.setText(editModel.addresses[1].state)
                            binding.edtZip.setText(editModel.addresses[1].postcode)
                            if (editModel.addresses[1].country == "United States") {
                                binding.edtAddress.setSelection(0)
                            } else {
                                binding.edtAddress.setSelection(1)
                            }

                            binding.edtStreetDel.setText(editModel.addresses[0].address1)
                            binding.edtSuiteDel.setText(editModel.addresses[0].address2)
                            binding.edtCityDel.setText(editModel.addresses[0].city)
                            binding.edtStateDel.setText(editModel.addresses[0].state)
                            binding.edtZipDel.setText(editModel.addresses[0].postcode)
                            if (editModel.addresses[0].country == "United States") {
                                binding.edtAddressDel.setSelection(0)
                            } else {
                                binding.edtAddressDel.setSelection(1)
                            }
                        }
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

        } else {
            viewModel.enroll_to_loyalty.value = true
            viewModel.same_as_billing_address.value = false
        }

        binding.header.imgBack.setOnClickListener {
            val navControll = findNavController()
            navControll.previousBackStackEntry?.savedStateHandle?.set(
                Constants.KEY,
                Constants.CUSTOMERDETAILS
            )
            navControll.popBackStack()
        }

        binding.edtBirthDay.setOnClickListener {
            LogUtil.logE(TAG, "DatePicker  ")
            showDatePicker()

        }
        binding.relativeDeliveryaddress.visibility=View.GONE

        setTextWatcherForAddressField(false)

    }

    private fun setTextWatcherForAddressField(onTextChanges: Boolean) {
        binding.edtZip.addTextChangedListener(
            CustomerAddressTextWatcher(
                binding.edtZip,
                changeField,
                this,
                onTextChanges
            )
        )
        binding.edtZipDel.addTextChangedListener(
            CustomerAddressTextWatcher(
                binding.edtZipDel,
                changeField,
                this,
                onTextChanges
            )
        )
        binding.edtSuiteDel.addTextChangedListener(
            CustomerAddressTextWatcher(
                binding.edtSuiteDel,
                changeField, this,
                onTextChanges
            )
        )
        binding.edtCityDel.addTextChangedListener(
            CustomerAddressTextWatcher(
                binding.edtCityDel,
                changeField,
                this,
                onTextChanges
            )
        )
        binding.edtStateDel.addTextChangedListener(
            CustomerAddressTextWatcher(
                binding.edtStateDel,
                changeField,
                this,
                onTextChanges
            )
        )
        binding.edtSuite.addTextChangedListener(
            CustomerAddressTextWatcher(
                binding.edtSuite,
                changeField,
                this,
                onTextChanges
            )
        )
        binding.edtCity.addTextChangedListener(
            CustomerAddressTextWatcher(
                binding.edtCity,
                changeField, this,
                onTextChanges
            )
        )
        binding.edtState.addTextChangedListener(
            CustomerAddressTextWatcher(
                binding.edtState,
                changeField, this,
                onTextChanges
            )
        )
    }

    private fun decodeLocation(lat: Double, lng: Double, place: String) {
    }

    private fun setPlaceApi() {

//        placesApi =
//            PlaceAPI.Builder()
//                .apiKey(binding.root.context.getString(com.pays.pos.R.string.api_key))
//                .build(binding.root.context)

        if (!Places.isInitialized()) {
            Places.initialize(
                requireContext(),
                binding.root.context.getString(com.pays.pos.R.string.api_key)
            )
        }

        placesClient = Places.createClient(requireContext())


        binding.edtStreet.threshold = 1
        binding.edtStreet.onItemClickListener = autocompleteClickListener
        adapter1 = placesClient?.let {
            _root_ide_package_.com.pays.pos.ui.fragments.settings.business.AutoCompleteAdapter(
                requireContext(),
                it
            )
        }
        adapter1?.setCountry("US")
        binding.edtStreet.setAdapter(adapter1)


        binding.edtStreetDel.threshold = 1
        binding.edtStreetDel.onItemClickListener = autocompleteClickListener1
        adapter2 = placesClient?.let {
            _root_ide_package_.com.pays.pos.ui.fragments.settings.business.AutoCompleteAdapter(
                requireContext(),
                it
            )
        }
        adapter2?.setCountry("US")
        binding.edtStreetDel.setAdapter(adapter2)


//        binding.edtStreet?.setAdapter(PlacesAutoCompleteAdapter(binding.root.context, placesApi))
//        binding.edtStreet?.setOnItemClickListener { parent, view, position, id ->
//            val place = parent.getItemAtPosition(position) as Place
//
//            //binding.edtStreet.setText("${place.description}")
//            placesApi.fetchPlaceDetails(place.id, object : OnPlacesDetailsListener {
//                override fun onError(errorMessage: String) {
//                }
//
//                override fun onPlaceDetailsFetched(placeDetails: PlaceDetails) {
//
//                    decodeLocation(placeDetails.lat, placeDetails.lng, placeDetails.name)
//
//                    val gcd = Geocoder(requireContext(), Locale.getDefault())
//                    /* val address: List<Address> =
//                         gcd.getFromLocation(placeDetails.lat, placeDetails.lng, 1)*/
//
//                    var street = ""
//                    var suite = ""
//                    var city = ""
//                    var state = ""
//                    var zip = ""
//                    placeDetails.address.forEach {
//                        it.type.forEach { type ->
//                            if (type.trim().lowercase() == "street_number".trim().lowercase()) {
//                                street += it.longName
//                            } else if (type.trim().lowercase() == "route".trim().lowercase()) {
//                                street += it.longName
//                            } else if (type.trim().lowercase() == "neighborhood".trim()
//                                    .lowercase()
//                            ) {
//                                suite = it.longName
//                            } else if (type.trim().lowercase() == "locality".trim()
//                                    .lowercase()
//                            ) {
//                                city = it.longName
//                            } else if (type.trim()
//                                    .lowercase() == "administrative_area_level_1".trim()
//                                    .lowercase()
//                            ) {
//                                state = it.longName
//                            } else if (type.trim().lowercase() == "postal_code".trim()
//                                    .lowercase()
//                            ) {
//                                zip = it.longName
//                            }
//
//                        }
//
//                    }
//
//
//                    if (placeDetails.address.isNotEmpty()) {
//                        try {
//                            runOnUiThread {
//                                binding.edtStreet?.setText(street)
//                                binding.edtSuite?.setText(suite)
//                                binding.edtCity?.setText(city)
//                                binding.edtState?.setText(state)
//                                binding.edtZip?.setText(zip)
//                                binding.edtStreet?.dismissDropDown()
//                            }
//                        } catch (e: Exception) {
//                            LogUtil.logE(TAG, "exception in pplaces api")
//                        } finally {
//                            binding.edtStreet?.dismissDropDown()
//                            LogUtil.logE(TAG, "notify callback")
//                        }
//                    }
//
//                    LogUtil.logE(TAG, "placeDetails:  ${Gson().toJson(placeDetails.name)}")
//
//                }
//
//            })
//        }
//
//        binding.edtStreetDel?.setAdapter(PlacesAutoCompleteAdapter(binding.root.context, placesApi))
//        binding.edtStreetDel.setOnItemClickListener { parent, view, position, id ->
//            val place = parent.getItemAtPosition(position) as Place
//
//            //binding.edtStreet.setText("${place.description}")
//            placesApi.fetchPlaceDetails(place.id, object : OnPlacesDetailsListener {
//                override fun onError(errorMessage: String) {
//                }
//
//                override fun onPlaceDetailsFetched(placeDetails: PlaceDetails) {
//
//                    decodeLocation(placeDetails.lat, placeDetails.lng, placeDetails.name)
//
//                    val gcd = Geocoder(requireContext(), Locale.getDefault())
//                    /* val address: List<Address> =
//                             gcd.getFromLocation(placeDetails.lat, placeDetails.lng, 1)*/
//
//                    var street = ""
//                    var suite = ""
//                    var city = ""
//                    var state = ""
//                    var zip = ""
//                    placeDetails.address.forEach {
//                        it.type.forEach { type ->
//                            if (type.trim().lowercase() == "street_number".trim().lowercase()) {
//                                street += it.longName
//                            } else if (type.trim().lowercase() == "route".trim().lowercase()) {
//                                street += it.longName
//                            } else if (type.trim().lowercase() == "neighborhood".trim()
//                                    .lowercase()
//                            ) {
//                                suite = it.longName
//                            } else if (type.trim().lowercase() == "locality".trim()
//                                    .lowercase()
//                            ) {
//                                city = it.longName
//                            } else if (type.trim()
//                                    .lowercase() == "administrative_area_level_1".trim()
//                                    .lowercase()
//                            ) {
//                                state = it.longName
//                            } else if (type.trim().lowercase() == "postal_code".trim()
//                                    .lowercase()
//                            ) {
//                                zip = it.longName
//                            }
//
//                        }
//
//                    }
//
//
//                    if (placeDetails.address.isNotEmpty()) {
//                        try {
//                            runOnUiThread {
//                                binding.edtStreetDel?.setText(street)
//                                binding.edtSuiteDel?.setText(suite)
//                                binding.edtCityDel?.setText(city)
//                                binding.edtStateDel?.setText(state)
//                                binding.edtZipDel?.setText(zip)
//                                binding.edtStreetDel?.dismissDropDown()
//                            }
//                        } catch (e: Exception) {
//                            LogUtil.logE(TAG, "exception in pplaces api")
//                        } finally {
//                            binding.edtStreetDel?.dismissDropDown()
//                            LogUtil.logE(TAG, "notify callback")
//                        }
//                    }
//
//                    LogUtil.logE(TAG, "placeDetails:  ${Gson().toJson(placeDetails.name)}")
//
//                }
//
//            })
//
//        }


        binding.edtStreet.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                if (binding.edtStreet.text.isNullOrEmpty()) {
                    binding.chksameasbilling.isChecked = false
                }
            }

        })
        binding.edtStreet2.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                if (binding.edtStreet2.text.isNullOrEmpty()) {
                    binding.chksameasbilling.isChecked = false
                }
            }

        })

    }

    private val autocompleteClickListener =
        AdapterView.OnItemClickListener { _, _, i, _ ->
            try {
                val item = adapter1!!.getItem(i)
                var placeID: String? = null
                if (item != null) {
                    placeID = item.placeId
                }
                val placeFields = listOf(
                    com.google.android.libraries.places.api.model.Place.Field.ID,
                    com.google.android.libraries.places.api.model.Place.Field.NAME,
                    com.google.android.libraries.places.api.model.Place.Field.ADDRESS,
                    com.google.android.libraries.places.api.model.Place.Field.ADDRESS_COMPONENTS,
                    com.google.android.libraries.places.api.model.Place.Field.LAT_LNG
                )
                var request: FetchPlaceRequest? = null
                if (placeID != null) {
                    request = FetchPlaceRequest.builder(placeID, placeFields)
                        .build()
                }
                if (request != null) {
                    placesClient!!.fetchPlace(request).addOnSuccessListener { task ->

                        MethodUtils.hideKeyboard(requireActivity())
                        binding.chksameasbilling.isChecked = false
                        binding.edtStreet.clearFocus()
                        binding.edtStreet.isFocusableInTouchMode = false;
                        binding.edtStreet.isFocusable = false;
                        binding.edtStreet.isFocusableInTouchMode = true;
                        binding.edtStreet.isFocusable = true;

                        var street = ""
                        var suite = ""
                        var city = ""
                        var state = ""
                        var zip = ""

                        task.place.name?.let { LogUtil.logE("Task", it) }
                        task.place.address?.let { LogUtil.logE("Task", it) }
                        task.place.addressComponents?.asList()
                            ?.forEachIndexed { index, addressComponent ->

                                addressComponent.types.forEach { type ->

                                    LogUtil.logE(
                                        "addressComponent",
                                        type + " ===  " + addressComponent.name
                                    )

                                    when {
                                        type.trim().lowercase() == "street_number".trim()
                                            .lowercase() -> {
                                            street += addressComponent.name
                                        }
                                        type.trim().lowercase() == "route".trim().lowercase() -> {
                                            street += addressComponent.name
                                        }
                                        type.trim().lowercase() == "neighborhood".trim()
                                            .lowercase() -> {
                                            suite = addressComponent.name
                                        }
                                        type.trim().lowercase() == "locality".trim()
                                            .lowercase() -> {
                                            city = addressComponent.name
                                        }
                                        type.trim()
                                            .lowercase() == "administrative_area_level_1".trim()
                                            .lowercase() -> {
                                            state = addressComponent.name
                                        }
                                        type.trim().lowercase() == "postal_code".trim()
                                            .lowercase() -> {
                                            zip = addressComponent.name
                                        }
                                    }
                                }

                                LogUtil.logE("index$index", addressComponent.name)
                            }


                        binding.edtStreet.setText(task.place.name)
                        binding.edtSuite.setText(suite)
                        binding.edtCity.setText(city)
                        binding.edtState.setText(state)
                        binding.edtZip.setText(zip)


                    }.addOnFailureListener { e ->
                        e.printStackTrace()

                    }
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }

    private val autocompleteClickListener1 =
        AdapterView.OnItemClickListener { _, _, i, _ ->
            try {
                val item = adapter2!!.getItem(i)
                var placeID: String? = null
                if (item != null) {
                    placeID = item.placeId
                }
                val placeFields = listOf(
                    com.google.android.libraries.places.api.model.Place.Field.ID,
                    com.google.android.libraries.places.api.model.Place.Field.NAME,
                    com.google.android.libraries.places.api.model.Place.Field.ADDRESS,
                    com.google.android.libraries.places.api.model.Place.Field.ADDRESS_COMPONENTS,
                    com.google.android.libraries.places.api.model.Place.Field.LAT_LNG
                )
                var request: FetchPlaceRequest? = null
                if (placeID != null) {
                    request = FetchPlaceRequest.builder(placeID, placeFields)
                        .build()
                }
                if (request != null) {
                    placesClient!!.fetchPlace(request).addOnSuccessListener { task ->

                        MethodUtils.hideKeyboard(requireActivity())
                        binding.chksameasbilling.isChecked = false
                        binding.edtStreetDel.clearFocus()
                        binding.edtStreetDel.isFocusableInTouchMode = false;
                        binding.edtStreetDel.isFocusable = false;
                        binding.edtStreetDel.isFocusableInTouchMode = true;
                        binding.edtStreetDel.isFocusable = true;

                        var street = ""
                        var suite = ""
                        var city = ""
                        var state = ""
                        var zip = ""

                        task.place.name?.let { LogUtil.logE("Task", it) }
                        task.place.address?.let { LogUtil.logE("Task", it) }
                        task.place.addressComponents?.asList()
                            ?.forEachIndexed { index, addressComponent ->

                                addressComponent.types.forEach { type ->

                                    LogUtil.logE(
                                        "addressComponent",
                                        type + " ===  " + addressComponent.name
                                    )

                                    when {
                                        type.trim().lowercase() == "street_number".trim()
                                            .lowercase() -> {
                                            street += addressComponent.name
                                        }
                                        type.trim().lowercase() == "route".trim().lowercase() -> {
                                            street += addressComponent.name
                                        }
                                        type.trim().lowercase() == "neighborhood".trim()
                                            .lowercase() -> {
                                            suite = addressComponent.name
                                        }
                                        type.trim().lowercase() == "locality".trim()
                                            .lowercase() -> {
                                            city = addressComponent.name
                                        }
                                        type.trim()
                                            .lowercase() == "administrative_area_level_1".trim()
                                            .lowercase() -> {
                                            state = addressComponent.name
                                        }
                                        type.trim().lowercase() == "postal_code".trim()
                                            .lowercase() -> {
                                            zip = addressComponent.name
                                        }
                                    }
                                }

                                LogUtil.logE("index$index", addressComponent.name)
                            }


                        binding.edtStreetDel.setText(task.place.name)
                        binding.edtSuiteDel.setText(suite)
                        binding.edtCityDel.setText(city)
                        binding.edtStateDel.setText(state)
                        binding.edtZipDel.setText(zip)


                    }.addOnFailureListener { e ->
                        e.printStackTrace()

                    }
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
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

                if (parent?.selectedItem.toString() == "United States") {
                    adapter1?.setCountry("US")
                } else if (parent?.selectedItem.toString() == "Canada") {
                    adapter1?.setCountry("CA")
                }

                if (Build.VERSION.SDK_INT < 23) {
                    (parent?.getChildAt(0) as TextView).setTextAppearance(
                        view?.context,
                        com.pays.pos.R.style.SpinnerTheme1
                    )
                } else {
                    try {
                        (parent?.getChildAt(0) as TextView).setTextAppearance(com.pays.pos.R.style.SpinnerTheme1);
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }


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

                    if (parent?.selectedItem.toString() == "United States") {
                        adapter2?.setCountry("US")
                    } else if (parent?.selectedItem.toString() == "Canada") {
                        adapter2?.setCountry("CA")
                    }

                    if (Build.VERSION.SDK_INT < 23) {
                        (parent?.getChildAt(0) as TextView).setTextAppearance(
                            view?.context,
                            com.pays.pos.R.style.SpinnerTheme1
                        )
                    } else {
                        try {
                            (parent?.getChildAt(0) as TextView).setTextAppearance(com.pays.pos.R.style.SpinnerTheme1);
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }


                }

                override fun onNothingSelected(parent: AdapterView<*>?) {

                }

            }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun onClick() {


        binding.header.txtSave.setOnClickListener {
            viewModel.sameAsAddressValueChanges(binding.chksameasbilling.isChecked)
            if (isEdit) {
                var id1: Int? = null
                var id2: Int? = null

                if (viewModel.listAddress.size == 2) {
                    id1 = viewModel.listAddress[0].id!!
                    id2 = viewModel.listAddress[1].id!!
                } else if (viewModel.listAddress.size == 1) {

                    if (viewModel.listAddress[0].type_of_address == "Shipping") {
                        id1 = viewModel.listAddress[0].id!!
                    } else if (viewModel.listAddress[0].type_of_address == "Billing") {
                        id2 = viewModel.listAddress[0].id!!
                    }
                }
//                if (viewModel.listAddress.size == 1) {
//                    id1 = viewModel.listAddress[0].id!!
//                } else if (viewModel.listAddress.size == 2) {
//                    id2 = viewModel.listAddress[1].id!!
//                }
                listAddress = arrayListOf()

                if (binding.edtStreet.text.toString().isNotEmpty())
                    listAddress.add(
                        CreateCustomerRequestModel.Customer.Addresses(
                            id1,
                            binding.edtStreet.text.toString(),
                            binding.edtSuite.text.toString(),
                            binding.edtCity.text.toString(),
                            binding.edtState.text.toString(),
                            binding.edtAddress.selectedItem.toString(),
                            binding.edtZip.text.toString(),
                            "Shipping",
                            0.0,
                            0.0,
                            "false"
                        )
                    )
                if (binding.edtStreetDel.text.toString().isNotEmpty()) {
                    listAddress.add(
                        CreateCustomerRequestModel.Customer.Addresses(
                            id2,
                            binding.edtStreetDel.text.toString(),
                            binding.edtSuiteDel.text.toString(),
                            binding.edtCityDel.text.toString(),
                            binding.edtStateDel.text.toString(),
                            binding.edtAddressDel.selectedItem.toString(),
                            binding.edtZipDel.text.toString(),
                            "Billing",
                            0.0,
                            0.0,
                            "false"
                        )
                    )
                } else {
                    if (viewModel.listAddress.size == 2) {
                        listAddress.add(
                            CreateCustomerRequestModel.Customer.Addresses(
                                id2,
                                viewModel.listAddress[1].address1,
                                viewModel.listAddress[1].address2,
                                viewModel.listAddress[1].city,
                                viewModel.listAddress[1].state,
                                viewModel.listAddress[1].country,
                                viewModel.listAddress[1].postcode,
                                "Billing",
                                0.0,
                                0.0,
                                "true"
                            )
                        )
                    }

                }

            } else {
                listAddress = arrayListOf()
                if (binding.edtStreet.text.toString().isNotEmpty())
                    listAddress.add(
                        CreateCustomerRequestModel.Customer.Addresses(
                            null,
                            binding.edtStreet.text.toString(),
                            binding.edtSuite.text.toString(),
                            binding.edtCity.text.toString(),
                            binding.edtState.text.toString(),
                            binding.edtAddress.selectedItem.toString(),
                            binding.edtZip.text.toString(),
                            "Shipping",
                            0.0,
                            0.0,
                            "false"
                        )
                    )
                if (binding.edtStreetDel.text.toString().isNotEmpty())
                    listAddress.add(
                        CreateCustomerRequestModel.Customer.Addresses(
                            null,
                            binding.edtStreetDel.text.toString(),
                            binding.edtSuiteDel.text.toString(),
                            binding.edtCityDel.text.toString(),
                            binding.edtStateDel.text.toString(),
                            binding.edtAddressDel.selectedItem.toString(),
                            binding.edtZipDel.text.toString(),
                            "Billing",
                            0.0,
                            0.0,
                            "false"
                        )
                    )
            }

            viewModel.submit(listAddress, isFromPhoneOrderEdit)
        }
        binding.chksameasbilling.setOnClickListener {
            if (binding.edtStreet.text.toString().isNotEmpty()) {
                binding.edtStreetDel.clearFocus()
                viewModel.same_as_billing_address.value = binding.chksameasbilling.isChecked
                if (binding.chksameasbilling.isChecked) {
                    changeField = true
                    setTextWatcherForAddressField(true)
                    if (binding.edtStreet.text.toString().trim().isNotEmpty())
                        binding.edtStreetDel.setText(binding.edtStreet.text.toString())
                    binding.edtSuiteDel.setText(binding.edtSuite.text.toString())
                    binding.edtCityDel.setText(binding.edtCity.text.toString())
                    binding.edtStateDel.setText(binding.edtState.text.toString())
                    binding.edtZipDel.setText(binding.edtZip.text.toString())
                    if (binding.edtAddress.selectedItem.toString() == "United States") {
                        binding.edtAddressDel.setSelection(0)
                    } else {
                        binding.edtAddressDel.setSelection(1)
                    }
                } else {
                    changeField = false
                    setTextWatcherForAddressField(false)
                    if (binding.edtStreetDel.text.toString().trim().isNotEmpty())
                        binding.edtStreetDel.text.clear()
                    binding.edtSuiteDel.setText("")
                    binding.edtCityDel.setText("")
                    binding.edtStateDel.setText("")
                    binding.edtZipDel.setText("")
                    binding.edtAddressDel.setSelection(0)
                }
            } else {
                AlertUtils.showCustomAlertWithListenerWithOK(
                    requireContext(),
                    "Please Enter Delivery Address.",
                )
                { _, _ ->
                    binding.chksameasbilling.isChecked = false
                }

            }
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
                        viewModel.addCustomerDetails.value?.data?.birth_day =
                            dayOfMonth.toString()
                        viewModel.addCustomerDetails.value?.data?.birth_month =
                            (monthOfYear + 1).toString()
                        viewModel.addCustomerDetails.value?.data?.birthday_year =
                            year.toString()


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
        LogUtil.logE(TAG, "DatePickerInside  ")
    }

    private fun navigate() {

        LogUtil.logE(TAG, "POPBACKCUSTOMER")
        viewModel.customerModel.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { updatedCustomerModel ->
                activity?.let {
                    if (prefProvider.getValue(
                            Constants.ORDER_TYPE,
                            Constants.TAKEOUT
                        ) == Constants.GIFT_CARD
                    ) {
                        AlertUtils.showCustomAlertWithListenerWithOK(
                            it,
                            "Your profile is updated.",
                        )
                        { _, _ ->

                            if(prefProvider.getValue(Constants.ORDER_TYPE, Constants.TAKEOUT)==Constants.GIFT_CARD){
                                moveToCheckout(updatedCustomerModel)
                            }
                        }
                    }

                }

            }
        }

        viewModel._Basedata.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { baseResponse ->
                if (prefProvider.getValue(
                        Constants.ORDER_TYPE,
                        Constants.TAKEOUT
                    ) != Constants.GIFT_CARD
                ) {
                    activity?.let {
                        AlertUtils.showCustomAlertWithListenerWithOK(
                            it,
                            baseResponse.message,
                        )
                        { _, _ ->

                            val navControll = findNavController()
                            navControll.previousBackStackEntry?.savedStateHandle?.set(
                                com.pays.pos.data.remote.Constants.KEY,
                                com.pays.pos.data.remote.Constants.CUSTOMERDETAILS
                            )
                            navControll.popBackStack()
                        }


                    }
                }
            }
        }
        viewModel.updatedCustomer.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { customer ->
                onUpdatingCustomer(customer)
            }
        }
    }

    private fun moveToCheckout(updatedCustomerModel: TbCustomer) {

        prefProvider.setValue(
            Constants.CUSTOMER_NAME,
            updatedCustomerModel.first_name + " " + updatedCustomerModel.last_name
        )

        prefProvider.setValue(
            Constants.RECEIPT_CUSTOMER_NAME,
            updatedCustomerModel.first_name + " " + updatedCustomerModel.last_name
        )

        updatedCustomerModel.id?.let { prefProvider.setValueInt(Constants.CUSTOMER_ID, it) }

        prefProvider.saveCustomerData(updatedCustomerModel)

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
        tbItem.orderTypeId = 0
        tbItem.orderType = Constants.GIFT_CARD
        tbItem.orderTypeName = Constants.GIFT_CARD

        cm.apply {
            employeeID = prefProvider.getValueInt(Constants.EMPLOYEE_ID, -1)
            terminalId = prefProvider.getValueInt(Constants.TERMINAL_ID, -1)
            isOpenOrder = false
            orderTypeId = 0
            orderType = Constants.GIFT_CARD
            orderTypeName = Constants.GIFT_CARD
        }

        dashboardViewModel.addCart(cm)
        dashboardViewModel.addOrderItemsToCartItems(listOf(tbItem))

        val bundle = Bundle()
        bundle.putBoolean("update", true)
        bundle.putDouble("totalPrice", totalPrice)
        bundle.putDouble("finalprice", totalPrice)
        bundle.putDouble("cashDiscountSurcharge",0.0)
        bundle.putDouble("subTotalPrice", totalPrice)
        bundle.putDouble("totalTax", 0.0)
        bundle.putDouble("totalDiscount", 0.0)
        bundle.putDouble("totalServiceCharge", 0.0)
        bundle.putParcelable("cartList", cm)

        val navOptions = NavOptions.Builder()
            .setPopUpTo(com.pays.pos.R.id.addEditCustomer, true)
            .build()

        findNavController().navigate(
            com.pays.pos.R.id.action_addEditCustomer_to_paymentBoldPosFragment,
            bundle, navOptions
        )
    }

    private fun onUpdatingCustomer(customer: TbCustomer){
        prefProvider.setValue(
            Constants.RECEIPT_CUSTOMER_NAME,
            customer.first_name + " " + customer.last_name
        )

        prefProvider.setValue(
            Constants.CUSTOMER_NAME,
            customer.first_name + " " + customer.last_name
        )
        prefProvider.setValueboolean(Constants.LOYALTY_ADDED, false)
        prefProvider.setValueboolean(Constants.IS_UPDATE_ORDER_LOYALTY_APPLIED, false)
        customer.id?.let { prefProvider.setValueInt(Constants.CUSTOMER_ID, it) }
        prefProvider.saveCustomerData(customer)
        findNavController().popBackStack()
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

    override fun onTextChanges() {
        changeField = false

        binding.chksameasbilling.isChecked = false

        //  setTextWatcherForAddressField(true)

    }

    override fun oncheckBox(b: Boolean) {
        Log.d(TAG, "oncheckBox: ${b}")
        binding.chksameasbilling.isChecked = b
    }

    fun checkedAllFieldAreSame(): Boolean {
        var delivery_Address :StringBuffer = StringBuffer()
        var billing_Address :StringBuffer = StringBuffer()
        delivery_Address.append(binding.edtStreet.text.toString())
        delivery_Address.append(binding.edtSuite.text.toString())
        delivery_Address.append(binding.edtStreet.text.toString())
        delivery_Address.append(binding.edtStreet.text.toString())
        delivery_Address.append(binding.edtStreet.text.toString())

        return false
    }
}
