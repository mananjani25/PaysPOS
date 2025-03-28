package com.pays.pos.ui.fragments.eGiftCard

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
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.pays.pos.data.model.requestModel.CreateCustomerRequestModel
import com.pays.pos.databinding.FragmentAddNewCustomerForGiftCardBinding
import com.pays.pos.ui.fragments.customer.AddCustomerViewModel
import com.pays.pos.ui.fragments.settings.business.AutoCompleteAdapter
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.CustomerAddressTextWatcher
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.MethodUtils
import com.pays.pos.utils.ProgressUtils
import com.pays.pos.utils.callback.AddressTextChangeListner
import com.pays.pos.utils.extensions.liveSnackBar
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class AddNewCustomerForGiftCardFragment : Fragment(), AddressTextChangeListner {

    private lateinit var binding: FragmentAddNewCustomerForGiftCardBinding
    private val TAG = "AddNewCustomerGC"
    private val viewModel by viewModels<AddCustomerViewModel>()

    private var listAddress: ArrayList<CreateCustomerRequestModel.Customer.Addresses> =
        arrayListOf()
    private var country = arrayOf("United States", "Canada")

    private var placesClient: PlacesClient? = null
    var adapter1: AutoCompleteAdapter? = null
    private var changeField: Boolean = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddNewCustomerForGiftCardBinding.inflate(layoutInflater)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        setUpSnackBar()
        showObserveProgress()
        navigate()
        setPhoneCountry()

        binding.edtStreet.setOnTouchListener { _, _ ->
            binding.nestedScrollView.smoothScrollTo(500, 500)
            false
        }

        binding.chkIsLoyalty.isChecked = true

        return binding.root
    }

    private fun setPhoneCountry() {
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, country)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

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
                    (parent?.getChildAt(0) as TextView).setTextAppearance(com.pays.pos.R.style.SpinnerTheme1); }


            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

        }
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

        binding.chkIsLoyalty.setOnClickListener {
            viewModel.enroll_to_loyalty.value = binding.chkIsLoyalty.isChecked
        }
        binding.header.txtTitle.text =
            getString(com.pays.pos.R.string.lbl_add_customer_to_gift_card)
        binding.header.txtSave.text = getString(com.pays.pos.R.string.save)
        setCountryAddress()

        viewModel.enroll_to_loyalty.value = true
        viewModel.same_as_billing_address.value = false

        binding.header.imgBack.setOnClickListener {
            val navController = findNavController()
            navController.previousBackStackEntry?.savedStateHandle?.set(
                com.pays.pos.data.remote.Constants.KEY,
                com.pays.pos.data.remote.Constants.CUSTOMERDETAILS
            )
            navController.popBackStack()
        }

        binding.edtBirthDay.setOnClickListener {
            LogUtil.logE(TAG, "DatePicker  ")
            showDatePicker()

        }

        setTextWatcherForAddressField()

    }

    private fun setTextWatcherForAddressField() {
        binding.edtZip.addTextChangedListener(
            CustomerAddressTextWatcher(
                binding.edtZip,
                changeField,
                this,
                false,
                10
            )
        )

        binding.edtSuite.addTextChangedListener(
            CustomerAddressTextWatcher(
                binding.edtSuite,
                changeField,
                this,
                false
            )
        )
        binding.edtCity.addTextChangedListener(
            CustomerAddressTextWatcher(
                binding.edtCity,
                changeField,
                this, false
            )
        )
        binding.edtState.addTextChangedListener(
            CustomerAddressTextWatcher(
                binding.edtState,
                changeField,
                this, false
            )
        )
    }

    private fun setPlaceApi() {
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
            AutoCompleteAdapter(
                requireContext(),
                it
            )
        }
        adapter1?.setCountry("US")
        binding.edtStreet.setAdapter(adapter1)

        binding.edtStreet.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {}

        })

    }

    // Suggestions for address
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
                        binding.edtStreet.clearFocus()
                        binding.edtStreet.isFocusableInTouchMode = false
                        binding.edtStreet.isFocusable = false
                        binding.edtStreet.isFocusableInTouchMode = true
                        binding.edtStreet.isFocusable = true

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

    private fun setCountryAddress() {
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, country)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.edtAddress.adapter = adapter
        binding.edtAddress.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
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
                        com.pays.pos.R.style.SpinnerTheme
                    )
                } else {
                    (parent?.getChildAt(0) as TextView).setTextAppearance(com.pays.pos.R.style.SpinnerTheme); }

            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}

        }

    }

    private fun onClick() {

        binding.header.txtSave.setOnClickListener {
            var zipText = binding.edtZip.text.toString().trim()

            if (zipText.length > 10) {
                binding.edtZip.requestFocus() // Moves cursor to ZIP field
            } else {
                viewModel.sameAsAddressValueChanges(false)

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
                viewModel.submit(listAddress, false)
            }
        }
    }

    private fun showDatePicker() {
        val c = Calendar.getInstance()
        val mYear = c.get(Calendar.YEAR)
        val mMonth = c.get(Calendar.MONTH)
        val mDay = c.get(Calendar.DAY_OF_MONTH)

        val datePicker =
            DatePickerDialog(
                requireContext(),
                android.R.style.Theme_Material_Light_Dialog,
                { _, year, monthOfYear, dayOfMonth ->
                    viewModel.addCustomerDetails.value?.data?.birth_day =
                        dayOfMonth.toString()
                    viewModel.addCustomerDetails.value?.data?.birth_month =
                        (monthOfYear + 1).toString()
                    viewModel.addCustomerDetails.value?.data?.birthday_year =
                        year.toString()


                    val calendar = Calendar.getInstance()
                    calendar.set(year, monthOfYear, dayOfMonth)
                    val outputFormat = SimpleDateFormat("MMM-dd-yyyy", Locale.getDefault())
                    val dateString = outputFormat.format(calendar.time)

                    binding.edtBirthDay.text = dateString
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

        viewModel._Basedata.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { baseResponse ->
                activity?.let {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        it,
                        baseResponse.message,
                    )
                    { _, _ ->

                        val navController = findNavController()
                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            com.pays.pos.data.remote.Constants.KEY,
                            com.pays.pos.data.remote.Constants.CUSTOMERDETAILS
                        )
                        navController.popBackStack()
                    }

                }
            }
        }

    }

    override fun onTextChanges() {
        changeField = false
    }

    override fun oncheckBox(b: Boolean) {
        Log.d(TAG, "onCheckBox: $b")
    }

}