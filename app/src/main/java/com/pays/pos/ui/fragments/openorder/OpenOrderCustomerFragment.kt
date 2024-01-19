package com.pays.pos.ui.fragments.openorder

import `in`.madapps.placesautocomplete.PlaceAPI
import `in`.madapps.placesautocomplete.adapter.PlacesAutoCompleteAdapter
import `in`.madapps.placesautocomplete.listener.OnPlacesDetailsListener
import `in`.madapps.placesautocomplete.model.Place
import `in`.madapps.placesautocomplete.model.PlaceDetails
import android.annotation.SuppressLint
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import com.pays.pos.R
import com.pays.pos.R.color
import com.pays.pos.data.entities.TbAddress
import com.pays.pos.data.entities.TbCustomer
import com.pays.pos.data.entities.TbPhones
import com.pays.pos.databinding.FragmentOpenOrderBinding
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.MethodUtils
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@AndroidEntryPoint
class OpenOrderCustomerFragment : Fragment(), View.OnClickListener {
    private var customerID: Int? = null
    private lateinit var deliveryType: String
    private var country = arrayOf("United States", "Canada")
    private lateinit var binding: FragmentOpenOrderBinding
    private lateinit var placesApi: PlaceAPI
    private var currentSelectedDate: Long? = null
    private var selectedHour: Int? = null
    private var selectedMinute: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOpenOrderBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        setupUI()
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    private fun setupUI() {
        binding.txtCustomerDetails.setOnClickListener(this)
        binding.txtDeliveryAddress.setOnClickListener(this)
        binding.txtBillingAddress.setOnClickListener(this)
        binding.header.imgBack.setOnClickListener(this)
        binding.llSearch.setOnClickListener(this)
        binding.etSearch.setOnClickListener(this)
        binding.btnClearDelivery.setOnClickListener(this)
        binding.btnClearBill.setOnClickListener(this)
        binding.edtDate.setOnClickListener(this)
        binding.edtTime.setOnClickListener(this)
        binding.btnClearCustomer.setOnClickListener(this)
        binding.btnCancelCustomer.setOnClickListener(this)
        binding.header.txtSave.setOnClickListener(this)
        binding.header.txtSave.setOnClickListener(this)
        binding.btnCancelDelivery.setOnClickListener(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            onDateSelected(System.currentTimeMillis())
            onTimeSelected(
                selectedHour ?: LocalDateTime.now().hour,
                selectedMinute ?: LocalDateTime.now().minute
            )
        }
        setPhoneCountry()

        placesApi =
            PlaceAPI.Builder().apiKey(binding.root.context.getString(R.string.api_key))
                .build(binding.root.context)

        binding.edtStreet.setAdapter(PlacesAutoCompleteAdapter(binding.root.context, placesApi))
        binding.edtStreetBill.setAdapter(PlacesAutoCompleteAdapter(binding.root.context, placesApi))

        binding.edtStreet.setOnItemClickListener { parent, view, position, id ->
            val place = parent.getItemAtPosition(position) as Place

            placesApi.fetchPlaceDetails(place.id, object : OnPlacesDetailsListener {
                override fun onError(errorMessage: String) {

                }

                override fun onPlaceDetailsFetched(placeDetails: PlaceDetails) {

                    MethodUtils.hideKeyboard(requireActivity())
                    val gcd = Geocoder(context, Locale.getDefault())
                    val address: List<Address> =
                        gcd.getFromLocation(placeDetails.lat, placeDetails.lng, 1)

                    if (address.isNotEmpty()) {

                        binding.edtStreet.setText(placeDetails.name)
                        binding.edtSuite.setText(placeDetails.name)
                        binding.edtCity.setText(address[0].locality)
                        binding.edtState.setText(address[0].adminArea)
                        binding.edtZip.setText(address[0].postalCode)


                    }

                }

            })

        }

        binding.edtStreetBill.setOnItemClickListener { parent, view, position, id ->
            val place = parent.getItemAtPosition(position) as Place

            placesApi.fetchPlaceDetails(place.id, object : OnPlacesDetailsListener {
                override fun onError(errorMessage: String) {

                }

                override fun onPlaceDetailsFetched(placeDetails: PlaceDetails) {

                    MethodUtils.hideKeyboard(requireActivity())

                    val gcd = Geocoder(context, Locale.getDefault())
                    val address: List<Address> =
                        gcd.getFromLocation(placeDetails.lat, placeDetails.lng, 1)

                    if (address.isNotEmpty()) {

                        binding.edtStreetBill.setText(placeDetails.name)
                        binding.edtSuiteBill.setText(placeDetails.name)
                        binding.edtCityBill.setText(address[0].locality)
                        binding.edtStateBill.setText(address[0].adminArea)
                        binding.edtZipBill.setText(address[0].postalCode)


                    }

                }

            })

        }

        setFragmentResultListener("request_key_customer") { requestKey: String, bundle: Bundle ->
            val result = bundle.getParcelable<TbCustomer>("data")
            if (result != null) {

                setupCustomer(result)
            }
        }

        binding.rdGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                binding.rdPickUp.id -> {
                    deliveryType = binding.rdPickUp.text.toString()
                }
                binding.rdDelivery.id -> {
                    deliveryType = binding.rdDelivery.text.toString()
                }
            }
        }
    }

    private fun setupCustomer(customer: TbCustomer) {

        customerID = customer.id

        binding.edtFirstName.setText(customer.first_name)
        binding.edtLastName.setText(customer.last_name)
        binding.edtPhoneNo.setText(AlertUtils.usNumberFormat(customer.phones[0].phone_number))
        binding.edtEmail.setText(customer.email)
        binding.edtCompany.setText(customer.company)

        if (customer.addresses.size == 1) {
            binding.edtStreet.setText(customer.addresses[0].street)
            binding.edtSuite.setText(customer.addresses[0].address2)
            binding.edtCity.setText(customer.addresses[0].city)
            binding.edtState.setText(customer.addresses[0].state)
            binding.edtZip.setText(customer.addresses[0].postcode)
        }
        if (customer.addresses.size == 2) {
            binding.edtStreetBill.setText(customer.addresses[1].street)
            binding.edtSuiteBill.setText(customer.addresses[1].address2)
            binding.edtCityBill.setText(customer.addresses[1].city)
            binding.edtStateBill.setText(customer.addresses[1].state)
            binding.edtZipBill.setText(customer.addresses[1].postcode)
        }


    }

    @SuppressLint("SetTextI18n")
    override fun onClick(v: View?) {

        when (v?.id) {
            R.id.txtCustomerDetails -> {

                binding.llSearch.visibility = View.VISIBLE
                binding.scrollViewCustomer.visibility = View.VISIBLE
                binding.llDeliveryAddress.visibility = View.GONE
                binding.llBillingAddress.visibility = View.GONE
                binding.viewCustomer.background=resources.getDrawable(R.drawable.button_action_hover)
                binding.viewDelivery.setBackgroundResource(0)
                binding.viewBilling.setBackgroundResource(0)
                binding.txtCustomerDetails.background=resources.getDrawable(R.drawable.button_action_hover)
                binding.txtDeliveryAddress.setTextColor(resources.getColor(color.drawerBack50))
                binding.txtBillingAddress.setTextColor(resources.getColor(color.drawerBack50))

            }
            R.id.txtDeliveryAddress -> {
                binding.scrollViewCustomer.visibility = View.GONE
                binding.llDeliveryAddress.visibility = View.VISIBLE
                binding.llBillingAddress.visibility = View.GONE
                binding.llSearch.visibility = View.GONE
                binding.txtCustomerDetails.setTextColor(resources.getColor(color.drawerBack50))
                binding.txtDeliveryAddress.background=resources.getDrawable(R.drawable.button_action_hover)
                binding.txtBillingAddress.setTextColor(resources.getColor(color.drawerBack50))
                binding.viewCustomer.setBackgroundResource(0)
                binding.viewDelivery.background=resources.getDrawable(R.drawable.button_action_hover)
                binding.viewBilling.setBackgroundResource(0)

            }
            R.id.txtBillingAddress -> {
                binding.scrollViewCustomer.visibility = View.GONE
                binding.llDeliveryAddress.visibility = View.GONE
                binding.llSearch.visibility = View.GONE
                binding.llBillingAddress.visibility = View.VISIBLE
                binding.txtCustomerDetails.setTextColor(resources.getColor(color.drawerBack50))
                binding.txtDeliveryAddress.setTextColor(resources.getColor(color.drawerBack50))
                binding.txtBillingAddress.background=resources.getDrawable(R.drawable.button_action_hover)
                binding.viewCustomer.setBackgroundResource(0)
                binding.viewDelivery.setBackgroundResource(0)
                binding.viewBilling.background=resources.getDrawable(R.drawable.button_action_hover)

            }
            R.id.imgBack -> {
                findNavController().navigateUp()
            }
            R.id.llSearch -> {
                findNavController().navigate(
                    R.id.action_openOrderCustomerFragment_to_assignCustomerOrderFragment
                )
            }
            R.id.etSearch -> {
                findNavController().navigate(
                    R.id.action_openOrderCustomerFragment_to_assignCustomerOrderFragment
                )
            }
            R.id.btnClearDelivery -> {

                binding.edtStreet.setText("")
                binding.edtSuite.setText("")
                binding.edtCity.setText("")
                binding.edtState.setText("")
                binding.edtZip.setText("")
            }
            R.id.btnClearBill -> {

                binding.edtStreetBill.setText("")
                binding.edtSuiteBill.setText("")
                binding.edtCityBill.setText("")
                binding.edtStateBill.setText("")
                binding.edtZipBill.setText("")
            }

            R.id.btnCancelCustomer -> {
                findNavController().navigateUp()
            }
            R.id.btnCancelDelivery -> {
                findNavController().navigateUp()
            }
            R.id.btnClearCustomer -> {

                binding.edtFirstName.setText("")
                binding.edtLastName.setText("")
                binding.edtPhoneNo.setText("")
                binding.edtEmail.setText("")
                binding.edtNote.setText("")
                binding.edtDate.text = ""
                binding.edtTime.text = ""
            }

            R.id.edtDate -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    showDatePicker()
                }
            }

            R.id.edtTime -> {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    showTimePicker()
                }

            }

            R.id.txtSave -> {


                if (validation()) {

                    val phonesList: ArrayList<TbPhones> =
                        arrayListOf()

                    if (binding.edtPhoneNo.text?.isNotEmpty()!!) {

                        val phone = TbPhones(
                            null, binding.edtPhoneNo.text.toString().trim().replace(
                                ("[\\D]").toRegex(),
                                ""
                            )
                        )
                        phonesList.add(phone)
                    }

                    val list: ArrayList<TbAddress> = arrayListOf()

                    if (binding.edtStreet.text.toString().trim().isNotEmpty()) {

                        val address = TbAddress(
                            null,
                            binding.edtStreet.text.toString().trim(),
                            binding.edtSuite.text.toString().trim(),
                            binding.edtCity.text.toString().trim(),
                            binding.edtState.text.toString().trim(),
                            binding.spDelivery.selectedItem.toString(),
                            binding.edtZip.text.toString().trim(),
                            "",
                            "0.0",
                            "0.0",
                            "Shipping",
                            "",
                            binding.edtStreet.text.toString().trim(),
                        )
                        list.add(address)
                    }

                    if (binding.edtStreetBill.text.toString().trim().isNotEmpty()) {

                        val address = TbAddress(
                            null,
                            binding.edtStreetBill.text.toString().trim(),
                            binding.edtSuiteBill.text.toString().trim(),
                            binding.edtCityBill.text.toString().trim(),
                            binding.edtStateBill.text.toString().trim(),
                            binding.spBill.selectedItem.toString(),
                            binding.edtZipBill.text.toString().trim(),
                            "",
                            "0.0",
                            "0.0",
                            "Billing",
                            "",
                            binding.edtStreetBill.text.toString().trim(),
                        )
                        list.add(address)
                    }


                    val customer = TbCustomer(
                        customerID,
                        MethodUtils.getText(binding.edtFirstName),
                        MethodUtils.getText(binding.edtLastName),
                        "",
                        MethodUtils.getText(binding.edtEmail),
                        false,
                        false,
                        0,
                        MethodUtils.getText(binding.edtCompany),
                        phonesList,
                        list
                    )


                    val result = Bundle().apply {
                        putParcelable("data", customer)
                        putString("DATE", binding.edtDate.text.toString())
                        putString("TIME", binding.edtTime.text.toString())
                        putBoolean("OPEN_ORDER", true)
                    }
                    setFragmentResult("request_key_customer_open_order", result)

                    findNavController().navigateUp()

                }
            }
        }

    }

    private fun validation(): Boolean {

        return when {
            TextUtils.isEmpty(binding.edtFirstName.text.toString().trim()) -> {
                AlertUtils.showCustomAlert(
                    requireContext(),
                    getString(R.string.first_name_validate)
                )
                false
            }
/*
            TextUtils.isEmpty(binding.edtPhoneNo.text.toString().trim()) -> {
                AlertUtils.showCustomAlert(requireContext(), getString(R.string.phone_no_validate))
                false
            }
*/
/*
            binding.edtPhoneNo.text.toString().trim().length < 14 -> {
                AlertUtils.showCustomAlert(
                    requireContext(),
                    getString(R.string.valid_phone_no_validate)
                )
                false
            }
*/
            else -> true
        }

    }

    private fun setPhoneCountry() {
        val adapter =
            ArrayAdapter(requireContext(), R.layout.row_spinner_county, country)
        adapter.setDropDownViewResource(R.layout.row_spinner_county)

        binding.spDelivery.adapter = adapter
        binding.spBill.adapter = adapter

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showDatePicker() {
        val selectedDateInMillis = currentSelectedDate ?: System.currentTimeMillis()

        MaterialDatePicker.Builder.datePicker().setSelection(selectedDateInMillis).build().apply {
            addOnPositiveButtonClickListener { dateInMillis -> onDateSelected(dateInMillis) }
        }.show(parentFragmentManager, MaterialDatePicker::class.java.canonicalName)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun onDateSelected(dateTimeStampInMillis: Long) {
        currentSelectedDate = dateTimeStampInMillis
        val dateTime: LocalDateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(
                currentSelectedDate!!
            ), ZoneId.systemDefault()
        )
        val dateAsFormattedText: String =
            dateTime.format(DateTimeFormatter.ofPattern("MMM-dd-yyyy"))
        binding.edtDate.text = dateAsFormattedText
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showTimePicker() {
        val hour = selectedHour ?: LocalDateTime.now().hour
        val minute = selectedMinute ?: LocalDateTime.now().minute

        MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(hour)
            .setMinute(minute)
            .build()
            .apply {
                addOnPositiveButtonClickListener { onTimeSelected(this.hour, this.minute) }
            }.show(parentFragmentManager, MaterialTimePicker::class.java.canonicalName)
    }

    private fun onTimeSelected(hour: Int, minute: Int) {
        selectedHour = hour
        selectedMinute = minute

        binding.edtTime.text = MethodUtils.getTime(selectedHour!!, selectedMinute!!)
    }
}