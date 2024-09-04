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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.pays.pos.R
import com.pays.pos.R.color
import com.pays.pos.data.entities.TbAddress
import com.pays.pos.data.entities.TbCustomer
import com.pays.pos.data.entities.TbPhones
import com.pays.pos.data.model.requestModel.CreateCustomerRequestModel
import com.pays.pos.data.remote.Constants
import com.pays.pos.databinding.FragmentOpenOrderNewBinding
import com.pays.pos.ui.adapter.AddressListAdapter
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.MethodUtils
import com.pays.pos.utils.ProgressUtils
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.gson.Gson
import com.pays.pos.logger.MessageEvent
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@AndroidEntryPoint
class OpenOrderCustomerFragmentNew : DialogFragment(), View.OnClickListener {
    private var customerID: Int? = null
    private var enrollToLoyalty: Boolean = false
    private var same_as_billing_address: Boolean = false
    private var finalReward: Int = 0
    private var deliveryType: String = "Pickup"
    private var selectedDate: String? = null
    private var country = arrayOf("United States", "Canada")
    private lateinit var binding: FragmentOpenOrderNewBinding
    private lateinit var placesApi: PlaceAPI
    private var currentSelectedDate: Long? = null
    private var selectedHour: Int? = null
    private var selectedMinute: Int? = null
    private val TAG = "OpenOrderCustomerFra"
    private var isEdit = false
    private val viewModel by viewModels<OpenOrderCustomerViewModel>()
    private lateinit var adapter: AddressListAdapter
    private var type: String = Constants.PICK_UP
    private var addressListNew: ArrayList<CreateCustomerRequestModel.Customer.Addresses> =
        arrayListOf()

    private var addressList: java.util.ArrayList<CreateCustomerRequestModel.Customer.Addresses> =
        arrayListOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOpenOrderNewBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        showObserveProgress()
        setupUI()
        navigate()
        return binding.root
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
        //  binding.rvAddresses.adapter = adapter

    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
/*
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
*/
        setAddress()


        if (!isEdit) {
            var list: java.util.ArrayList<CreateCustomerRequestModel.Customer.Addresses> =
                arrayListOf()
            var model = CreateCustomerRequestModel.Customer.Addresses()
            model.apply {
                latitude = 0.0
                longitude = 0.0
            }
            list.add(model)

            adapter.setAddress(list)
            //  viewModel.setAddressList(adapter.getList())
        }

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


    private fun setupUI() {
        binding.txtCustomerDetails.setOnClickListener(this)
        binding.txtDeliveryAddress.setOnClickListener(this)
        binding.imgBack.setOnClickListener(this)
        binding.llSearch.setOnClickListener(this)
        binding.etSearch.setOnClickListener(this)
        binding.btnClearDelivery.setOnClickListener(this)
        binding.btnClearBill.setOnClickListener(this)
        binding.btnCancelDelivery.setOnClickListener(this)
        binding.edtDate.setOnClickListener(this)
        binding.edtTime.setOnClickListener(this)
        binding.btnClearCustomer.setOnClickListener(this)
        binding.btnCancelCustomer.setOnClickListener(this)
        binding.txtSave.setOnClickListener(this)
        binding.txtPickup.setOnClickListener(this)
        binding.txtDelivery.setOnClickListener(this)

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

        binding.edtStreet.setOnFocusChangeListener { v, hasFocus ->

            LogUtil.logE(TAG, "HasFocusChanged")

        }
        //   binding.edtStreetBill.setAdapter(PlacesAutoCompleteAdapter(binding.root.context, placesApi))

        binding.edtStreet.setOnItemClickListener { parent, view, position, id ->
            val place = parent.getItemAtPosition(position) as Place

            placesApi.fetchPlaceDetails(place.id, object : OnPlacesDetailsListener {
                override fun onError(errorMessage: String) {

                }

                @SuppressLint("LongLogTag")
                override fun onPlaceDetailsFetched(placeDetails: PlaceDetails) {
                    LogUtil.logE(TAG, "onPlaceFatched ${Gson().toJson(placeDetails)}")


                    MethodUtils.hideKeyboard(requireActivity())
                    val gcd = Geocoder(context!!, Locale.getDefault())
                    val address: List<Address> =
                        gcd.getFromLocation(placeDetails.lat, placeDetails.lng, 1)!!


                    LogUtil.logE(TAG, "address   ${Gson().toJson(address)}")

                    if (placeDetails.address.isNotEmpty()) {

                        var state = ""
                        var pincode = ""
                        placeDetails.address.forEach {
                            if (it.type[0].lowercase() == "administrative_area_level_1".lowercase()) {
                                state = it.longName
                            } else if (it.type[0].lowercase() == "postal_code".lowercase()) {
                                pincode = it.longName
                            }


                        }
                        binding.edtStreet.setText(placeDetails.name)
                        binding.edtSuite.setText(placeDetails.name)
                        binding.edtCity.setText(placeDetails.vicinity)
                        binding.edtState.setText(state)
                        binding.edtZip.setText(pincode)
                        binding.edtStreet.dismissDropDown()


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

                    val gcd = Geocoder(context!!, Locale.getDefault())
                    val address: List<Address> =
                        gcd.getFromLocation(placeDetails.lat, placeDetails.lng, 1)!!
                    LogUtil.logE(TAG, "address:  ${Gson().toJson(address)}")

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
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Bundle>("data")
            ?.observe(viewLifecycleOwner) { it ->
                selectedDate = it.getString("SELECTED_DATE")
                binding.edtDate.text = selectedDate
            }

        setFragmentResultListener("request_key_customer") { requestKey: String, bundle: Bundle ->
            val result = bundle.getParcelable<TbCustomer>("data")
            if (result != null) {
                isEdit = bundle.getBoolean("isEdit")
                selectedDate = bundle.getString("SELECTED_DATE")

                binding.edtDate.text = selectedDate
                setupCustomer(result)
            }
        }

//        binding.rdGroup.setOnCheckedChangeListener { _, checkedId ->
//            when (checkedId) {
//                binding.rdPickUp.id -> {
//                    deliveryType = binding.rdPickUp.text.toString()
//                }
//                binding.rdDelivery.id -> {
//                    deliveryType = binding.rdDelivery.text.toString()
//                }
//            }
//        }
    }


    private fun navigate() {

        LogUtil.logE(TAG, "POPBACKCUSTOMER")
        viewModel._Basedata.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let { baseResponse ->
                val phonesList: ArrayList<TbPhones> =
                    arrayListOf()
                activity?.let {
                    if (binding.edtPhoneNo.text?.isNotEmpty()!!) {

                        val phone = TbPhones(
                            null, binding.edtPhoneNo.text.toString().trim().replace(
                                ("[\\D]").toRegex(),
                                ""
                            )
                        )
                        phonesList.add(phone)
                    }
                }


                val list: ArrayList<TbAddress> = arrayListOf()

                for (i in viewModel.listAddress) {
                    list.add(
                        TbAddress(
                            i.id,
                            i.address1,
                            i.address2,
                            i.city,
                            i.state,
                            i.country.toString(),
                            i.postcode,
                            "",
                            i.latitude.toString(),
                            i.longitude.toString(),
                            i.type_of_address,
                            "",
                            i.address1
                        )
                    )
                }


                val customer = TbCustomer(
                    baseResponse.id,
                    MethodUtils.getText(binding.edtFirstName),
                    MethodUtils.getText(binding.edtLastName),
                    "",
                    MethodUtils.getText(binding.edtEmail),
                    enrollToLoyalty,
                    same_as_billing_address,
                    finalReward,
                    MethodUtils.getText(binding.edtCompany),
                    phonesList,
                    list
                )


                val result = Bundle().apply {
                    putParcelable("data", customer)
                    putString("DATE", binding.edtDate.text.toString())
                    putString("TIME", binding.edtTime.text.toString())
                    putString("TYPE", type)
                    putBoolean("OPEN_ORDER", true)
                }
                setFragmentResult("request_key_customer_open_order", result)

                findNavController().navigateUp()


            }
        })

    }


    private fun setupCustomer(customer: TbCustomer) {
        if (isEdit) {

            if (customer?.id != null) {
                viewModel.isEditData(isEdit, customer?.id!!)
            }

            viewModel.addCustomerDetails.value?.data?.first_name = customer?.first_name.toString()
            viewModel.addCustomerDetails.value?.data?.last_name = customer?.last_name.toString()


            if (customer?.phones?.size != 0) {
                viewModel.phoneNo.value =
                    AlertUtils.usNumberFormat(customer?.phones?.get(0)?.phone_number!!).toString()
                viewModel.phoneId = customer.phones[0].id
            }
            viewModel.addCustomerDetails.value?.data?.email = customer.email

            if (customer.addresses.isNotEmpty()) {
                binding.edtStreet.setText(customer.addresses.get(customer.addresses.size - 1).address1)
                binding.edtSuite.setText(customer.addresses.get(customer.addresses.size - 1).address2)
                binding.edtCity.setText(customer.addresses.get(customer.addresses.size - 1).city)
                binding.edtState.setText(customer.addresses.get(customer.addresses.size - 1).state)
                binding.edtZip.setText(customer.addresses.get(customer.addresses.size - 1).postcode)

            }

            if (customer.addresses.isNotEmpty()) {

                addressListNew = arrayListOf()
                LogUtil.logE(TAG, "addressesList1:  ${Gson().toJson(customer.addresses.size)}")

                for (i in 0 until customer.addresses.size) {
                    val obj = customer.addresses.get(i)
                    addressListNew.add(
                        CreateCustomerRequestModel.Customer.Addresses(
                            obj.id,
                            obj.address1,
                            obj.address2,
                            obj.city,
                            obj.state,
                            obj.country,
                            obj.postcode,
                            obj.type_of_address,
                            0.0,
                            0.0

                        )
                    )
                }


                //  adapter.setAddress(list)
            }


        } else
            addAddress()

        customerID = customer.id
        enrollToLoyalty = customer.enroll_to_loyalty ?: false
        same_as_billing_address = customer.same_as_billing_address ?: false
        finalReward = customer.final_reward ?: 0

        binding.edtFirstName.setText(customer.first_name)
        binding.edtLastName.setText(customer.last_name)
        if (customer.phones.size > 0) {
            binding.edtPhoneNo.setText(AlertUtils.usNumberFormat(customer.phones[0].phone_number))
        }
        binding.edtEmail.setText(customer.email)
        binding.edtCompany.setText(customer.company)

        if (customer?.id != null) {
            viewModel.isEditData(isEdit, customer?.id!!)
        }


        if (customer.addresses.isNotEmpty()) {
            var list: java.util.ArrayList<CreateCustomerRequestModel.Customer.Addresses> =
                arrayListOf()
            //addressList = list
            list.add(
                CreateCustomerRequestModel.Customer.Addresses(
                    customer.addresses.get(customer.addresses.size - 1).id,
                    customer.addresses.get(customer.addresses.size - 1).address1,
                    customer.addresses.get(customer.addresses.size - 1).address2,
                    customer.addresses.get(customer.addresses.size - 1).city,
                    customer.addresses.get(customer.addresses.size - 1).state,
                    customer.addresses.get(customer.addresses.size - 1).country,
                    customer.addresses.get(customer.addresses.size - 1).postcode,
                    customer.addresses.get(customer.addresses.size - 1).type_of_address,
                    0.0,
                    0.0,
                )
            )

            addressListNew = arrayListOf()
            LogUtil.logE(TAG, "addressesList2:  ${Gson().toJson(customer.addresses.size)}")

            for (i in 0 until customer.addresses.size) {
                val obj = customer.addresses.get(i)
                addressListNew.add(
                    CreateCustomerRequestModel.Customer.Addresses(
                        obj.id,
                        obj.address1,
                        obj.address2,
                        obj.city,
                        obj.state,
                        obj.country,
                        obj.postcode,
                        obj.type_of_address,
                        0.0,
                        0.0

                    )
                )
            }


            /*  var list: java.util.ArrayList<CreateCustomerRequestModel.Customer.Addresses> = arrayListOf()
              val data=customer.addresses.get(customer.addresses.size-1)
              list.addAll(data)*/
/*
            customer.addresses.forEach {
                list.add(CreateCustomerRequestModel.Customer.Addresses(
                    it.id,it.address1,it.address2,it.city,it.state,it.country,it.postcode,it.type_of_address
                ))
            }
*/


            //  adapter.setAddress(list)
        } else
            addAddress()


/*
        if (customer.addresses.isNotEmpty()) {
            for (i in customer.addresses.indices) {
                if (customer.addresses[i].type_of_address == "Shipping") {
                    if (customer.addresses[i].country == "United States") {
                        binding.spDelivery.setSelection(0)
                    } else if (customer.addresses[i].country == "Canada") {
                        binding.spDelivery.setSelection(1)
                    }
                    binding.edtStreet.setText(customer.addresses[i].street)
                    binding.edtCity.setText(customer.addresses[i].city)
                    binding.edtState.setText(customer.addresses[i].state)
                    binding.edtZip.setText(customer.addresses[i].postcode)
                    binding.edtSuite.setText(customer.addresses[i].address1)
                    break
                }

            }
        }
*/
    }

    @SuppressLint("SetTextI18n")
    override fun onClick(v: View?) {

        when (v?.id) {
            R.id.txtCustomerDetails -> {

                binding.llCustomerDetails.visibility = View.VISIBLE
                binding.llDeliveryAddress.visibility = View.GONE
                binding.llBillingAddress.visibility = View.GONE
                binding.txtCustomerDetails.setTextColor(resources.getColor(color.txtColor))
                binding.txtDeliveryAddress.setTextColor(resources.getColor(color.txtColor))

                binding.txtCustomerDetails.background=resources.getDrawable(R.drawable.button_action_hover)
                binding.txtDeliveryAddress.setBackgroundColor(resources.getColor(color.bg_color))
                binding.txtCustomerDetails.setTextColor(resources.getColor(color.white))
                binding.txtDeliveryAddress.setTextColor(resources.getColor(color.txtColor))


            }
            R.id.txtDeliveryAddress -> {
                binding.llDeliveryAddress.visibility = View.VISIBLE
                binding.llBillingAddress.visibility = View.GONE
                binding.llCustomerDetails.visibility = View.GONE


                binding.txtCustomerDetails.setTextColor(resources.getColor(color.txtColor))
                binding.txtDeliveryAddress.setTextColor(resources.getColor(color.txtColor))

                binding.txtCustomerDetails.setBackgroundColor(resources.getColor(color.bg_color))
                binding.txtDeliveryAddress.background=resources.getDrawable(R.drawable.button_action_hover)

                binding.txtCustomerDetails.setTextColor(resources.getColor(color.txtColor))
                binding.txtDeliveryAddress.setTextColor(resources.getColor(color.white))

            }
            R.id.imgBack -> {
                findNavController().navigateUp()
            }
            R.id.llSearch -> {
                findNavController().navigate(
                    R.id.action_openOrderCustomerFragmentNew_to_assignCustomerOrderFragment
                )
            }
            R.id.etSearch -> {
                val result = Bundle().apply {
                    putString("SELECTED_DATE", binding.edtDate.text.toString())
                }

                findNavController().navigate(
                    R.id.action_openOrderCustomerFragmentNew_to_assignCustomerOrderFragment, result
                )
            }
            R.id.btnClearDelivery -> {
                binding.edtStreet.setText("")
                binding.edtSuite.setText("")
                binding.edtCity.setText("")
                binding.edtState.setText("")
                binding.edtZip.setText("")


                //addAddress()
            }
            R.id.btnCancelDelivery -> {
                findNavController().navigateUp()
                // addAddress()

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
            R.id.txtPickup -> {
                type = Constants.PICK_UP
                binding.txtPickup.setTextColor(resources.getColor(color.txtColor))
                binding.txtDelivery.setTextColor(resources.getColor(color.txtColor))

                binding.txtPickup.background=resources.getDrawable(R.drawable.button_action_hover)
                binding.txtDelivery.setBackgroundColor(resources.getColor(color.bg_color))

                binding.txtPickup.setTextColor(resources.getColor(color.white))
                binding.txtDelivery.setTextColor(resources.getColor(color.txtColor))



            }
            R.id.txtDelivery -> {
                type = Constants.DELIVERY
                binding.txtPickup.setTextColor(resources.getColor(color.txtColor))
                binding.txtDelivery.setTextColor(resources.getColor(color.txtColor))

                binding.txtPickup.setBackgroundColor(resources.getColor(color.bg_color))
                binding.txtDelivery.background=resources.getDrawable(R.drawable.button_action_hover)

                binding.txtPickup.setTextColor(resources.getColor(color.txtColor))
                binding.txtDelivery.setTextColor(resources.getColor(color.white))
            }
            R.id.btnClearCustomer -> {
                binding.edtFirstName.setText("")
                binding.edtLastName.setText("")
                binding.edtPhoneNo.setText("")
                binding.edtEmail.setText("")
                binding.edtNote.setText("")
                binding.edtDate.text = ""
                binding.edtTime.text = ""
                binding.edtCompany.setText("")


            }

            R.id.edtDate -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    selectedDate = null
                    showDatePicker()
                }
            }

            R.id.edtTime -> {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    showTimePicker()
                }

            }

            R.id.txtSave -> {

                /*  val addressList=ArrayList<CreateCustomerRequestModel.Customer.Addresses>()
                  val address1=binding.edtStreet.text.toString().trim()
                  val address2=binding.edtSuite.text.toString().trim()
                  val city=binding.edtCity.text.toString().trim()
                  val state=binding.edtState.text.toString().trim()
                  val zipCode=binding.edtZip.text.toString().trim()
                  addressList.add(CreateCustomerRequestModel.Customer.Addresses(null,address1,address2,city,state))*/
                if (addressListNew.isNotEmpty()) {

                    addressListNew.get(addressListNew.size - 1).address1 =
                        binding.edtStreet.text.toString()
                    addressListNew.get(addressListNew.size - 1).address2 =
                        binding.edtSuite.text.toString()
                    addressListNew.get(addressListNew.size - 1).city =
                        binding.edtCity.text.toString()
                    addressListNew.get(addressListNew.size - 1).state =
                        binding.edtState.text.toString()
                    addressListNew.get(addressListNew.size - 1).postcode =
                        binding.edtZip.text.toString()
                    LogUtil.logE(TAG, "addressListSize:  ${addressListNew.size}")


                    viewModel.setAddressList(addressListNew)

                } else if (binding.edtStreet.text.trim().isNotEmpty()) {
                    addressListNew.add(
                        CreateCustomerRequestModel.Customer.Addresses(

                            address1 = binding.edtStreet.text.toString(),
                            address2 = binding.edtSuite.text.toString(),
                            city = binding.edtCity.text.toString(),
                            state = binding.edtState.text.toString(),
                            country = "United States",
                            postcode = binding.edtZip.text.toString()


                        )
                    )
                    viewModel.setAddressList(addressListNew)

                }
                EventBus.getDefault().post(MessageEvent("${Constants.LINE_BREAK_TAB} OpenOrderCustomerFragmentNew.kt, R.id.txtSave"))

                viewModel.submit()

/*
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
                        enrollToLoyalty,
                        finalReward,
                        MethodUtils.getText(binding.edtCompany),
                        phonesList,
                        list
                    )


                    val result = Bundle().apply {
                        putParcelable("data", customer)
                        putString("DATE", binding.edtDate.text.toString())
                        putString("TIME", binding.edtTime.text.toString())
                        putString("TYPE", type)
                        putBoolean("OPEN_ORDER", true)
                    }
                    setFragmentResult("request_key_customer_open_order", result)

                    findNavController().navigateUp()

                }
*/
            }
        }

    }

    private fun addAddress() {
        var list: java.util.ArrayList<CreateCustomerRequestModel.Customer.Addresses> = arrayListOf()
        var model = CreateCustomerRequestModel.Customer.Addresses()
        model.apply {
            latitude = 0.0
            longitude = 0.0
        }
        list.add(model)

        adapter.setAddress(list)
        // viewModel.setAddressList(adapter.getList())
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
            /*binding.edtPhoneNo.text.toString().trim().length < 14 -> {
                AlertUtils.showCustomAlert(
                    requireContext(),
                    getString(R.string.valid_phone_no_validate)
                )
                false
            }*/
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
        if (selectedDate != null) {
            binding.edtDate.text = selectedDate
        } else {
            binding.edtDate.text = dateAsFormattedText
        }

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