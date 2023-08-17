package com.android.pos.ui.fragments.phoneOrder

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Point
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.text.style.UnderlineSpan
import android.util.Log
import android.util.Patterns
import android.view.*
import android.widget.AdapterView
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.*
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.android.pos.BuildConfig
import com.android.pos.R
import com.android.pos.data.entities.TbAddress
import com.android.pos.data.entities.TbCustomer
import com.android.pos.data.entities.TbPhones
import com.android.pos.data.model.requestModel.CreateCustomerRequestModel
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.AUTH_TOKEN
import com.android.pos.data.remote.Constants.DELIVERY
import com.android.pos.data.remote.Constants.IS_CLOCKOUT
import com.android.pos.data.remote.Constants.ORDER_COMPLETED
import com.android.pos.data.remote.Constants.PICK_UP
import com.android.pos.databinding.FragmentLoginBinding
import com.android.pos.databinding.FragmentPhoneOrderBinding
import com.android.pos.di.ApiModule.BASE_URL
import com.android.pos.di.HostSelectionInterceptor
import com.android.pos.di.PrefProvider
import com.android.pos.ui.activities.MainActivity
import com.android.pos.ui.fragments.loginscreen.LoginViewModel
import com.android.pos.ui.fragments.settings.business.AutoCompleteAdapter
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.Event
import com.android.pos.utils.LogUtil
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.gone
import com.android.pos.utils.extensions.liveSnackBar
import com.android.pos.utils.extensions.setOnSingleClickListener
import com.android.pos.utils.extensions.visible
import com.android.pos.utils.statusUtils.Status
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.ArrayList
import javax.inject.Inject


@AndroidEntryPoint
class PhoneOrderFragment : Fragment() {


    private lateinit var customer: TbCustomer
    private var orderType: String = PICK_UP
    private var isDelivey = false
    private var customerID: Int? = null
    private var isPickUp = true
    private lateinit var binding: FragmentPhoneOrderBinding

    private val viewModel by viewModels<LoginViewModel>()

    var placesClient: PlacesClient? = null
    var adapter1: AutoCompleteAdapter? = null
    var selectedCustomer: TbCustomer? = null


    @set:Inject
    internal var prefProvider: PrefProvider? = null

    @set:Inject
    var hostSelectionInterceptor: HostSelectionInterceptor? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_phone_order, container, false)
        binding.lifecycleOwner = this
        manageDeliveryTypeView()
        resultListener()
        placesClientInit()
        setupSnackbar()
        observeShowProgress()
        navigate()
        clickEvent()


        return binding.root
    }

    private fun resultListener() {

        setFragmentResultListener("request_key_customer_phone_order") { requestKey: String, bundle: Bundle ->
            val result = bundle.getParcelable<TbCustomer>("data")
            if (result != null) {
                selectedCustomer = result
                setupCustomer(result)
            }
        }
    }

    private fun setupCustomer(customer: TbCustomer) {

        customerID = customer.id

        binding.edtFName.setText(customer.first_name)
        binding.edtLName.setText(customer.last_name)

        prefProvider?.setValue(
            Constants.CUSTOMER_NAME,
            customer.first_name + " " + customer.last_name
        )

        if (customer.phones.isNotEmpty())
            binding.edtPhoneNo.setText(AlertUtils.usNumberFormat(customer.phones[0].phone_number))
        binding.edtEmail.setText(customer.email)

        if (customer.addresses.isNotEmpty()) {
            binding.edtStreet.setText(customer.addresses[0].street)
            binding.edtSuite.setText(customer.addresses[0].address2)
            binding.edtCity.setText(customer.addresses[0].city)
            binding.edtState.setText(customer.addresses[0].state)
            binding.edtZip.setText(customer.addresses[0].postcode)
        }


    }

    private fun manageDeliveryTypeView(){
        if(orderType == PICK_UP) {
            orderType = PICK_UP
            isPickUp = true
            isDelivey = false
            binding.txtPickup.setBackgroundResource(R.drawable.button_action_hover)
            binding.txtDelivery.setBackgroundResource(R.drawable.background_square_border_grey)
            binding.txtAddress.gone()
        } else {
            orderType = DELIVERY
            isDelivey = true
            isPickUp = false
            binding.txtPickup.setBackgroundResource(R.drawable.background_square_border_grey)
            binding.txtDelivery.setBackgroundResource(R.drawable.button_action_hover)
            binding.txtAddress.visible()
        }
    }

    private fun placesClientInit() {

        if (!Places.isInitialized()) {
            Places.initialize(
                requireContext(),
                binding.root.context.getString(com.android.pos.R.string.api_key)
            )
        }

        placesClient = Places.createClient(requireContext())

        binding.edtStreet.threshold = 1
        binding.edtStreet.onItemClickListener = autocompleteClickListener
        adapter1 = placesClient?.let { AutoCompleteAdapter(requireContext(), it) }
        adapter1?.setCountry("US")
        binding.edtStreet.setAdapter(adapter1)
    }

    private fun clickEvent() {

        binding.imgBack.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.txtHome.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.txtPickup.setOnSingleClickListener {

            orderType = PICK_UP
            isPickUp = true
            isDelivey = false
            binding.txtPickup.setBackgroundResource(R.drawable.button_action_hover)
            binding.txtDelivery.setBackgroundResource(R.drawable.background_square_border_grey)
            binding.txtAddress.gone()
        }
        binding.txtDelivery.setOnSingleClickListener {
            orderType = DELIVERY
            isDelivey = true
            isPickUp = false
            binding.txtPickup.setBackgroundResource(R.drawable.background_square_border_grey)
            binding.txtDelivery.setBackgroundResource(R.drawable.button_action_hover)
            binding.txtAddress.visible()
        }

        binding.llSearch.setOnSingleClickListener {

            val bundle = Bundle().apply {
                putBoolean("PhoneOrder", true)
            }
            findNavController().navigate(
                R.id.action_phoneOrderFragment_to_assignCustomerOrderFragment,
                bundle
            )
        }
        binding.etSearch.setOnSingleClickListener {
            val bundle = Bundle().apply {
                putBoolean("PhoneOrder", true)
            }
            findNavController().navigate(
                R.id.action_phoneOrderFragment_to_assignCustomerOrderFragment,
                bundle
            )
        }

        binding.txtNext.setOnSingleClickListener {
            if (binding.edtFName.text.toString().trim().isEmpty()) {
                AlertUtils.showCustomAlert(
                    requireContext(),
                    getString(R.string.first_name_validate)
                )

            } else if (binding.edtPhoneNo.rawText.toString().trim().isEmpty()) {
                AlertUtils.showCustomAlert(requireContext(), getString(R.string.phone_validate))

            } else if (binding.edtPhoneNo.rawText.toString().trim().length < 10) {
                AlertUtils.showCustomAlert(
                    requireContext(),
                    getString(R.string.valid_phone_validate)
                )
            } else if (!TextUtils.isEmpty(
                    binding.edtEmail.text.toString().trim()
                ) && !Patterns.EMAIL_ADDRESS.matcher(
                    binding.edtEmail.text.toString().trim()
                ).matches()
            ) {
                AlertUtils.showCustomAlert(
                    requireContext(),
                    getString(R.string.valid_email_validate)
                )
            } else if (isDelivey && binding.edtStreet.text.toString().trim().isEmpty()) {
                AlertUtils.showCustomAlert(requireContext(), "Please enter address")
            }
            else if (isDelivey && binding.edtZip.text.toString().trim().isEmpty()) {
                AlertUtils.showCustomAlert(requireContext(), "Please enter zipcode")
            } else {


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
                        if (isPickUp) PICK_UP else DELIVERY,
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


                customer = TbCustomer(
                    customerID,
                    MethodUtils.getText(binding.edtFName),
                    MethodUtils.getText(binding.edtLName),
                    "",
                    MethodUtils.getText(binding.edtEmail),
                    selectedCustomer?.enroll_to_loyalty ?: false,
                    false,
                    selectedCustomer?.final_reward ?: 0,
                    "",
                    phonesList,
                    list
                )


                if (customerID == null) {

                    var listAddress: ArrayList<CreateCustomerRequestModel.Customer.Addresses> =
                        arrayListOf()

                    val phoneId: Int? = null
                    val addCustomerData = CreateCustomerRequestModel().apply {
                        data?.first_name = MethodUtils.getText(binding.edtFName)
                        data?.last_name = MethodUtils.getText(binding.edtLName)
                        data?.email = MethodUtils.getText(binding.edtEmail)

                        val phone = CreateCustomerRequestModel.Customer.Phone(
                            id = phoneId,
                            phone_number = binding.edtPhoneNo.text.toString().trim().replace(
                                ("[\\D]").toRegex(),
                                ""
                            )
                        )

                        data?.phones_attributes?.add(
                            0, phone
                        )

                        listAddress = arrayListOf()
                        if (binding.edtStreet.text.toString().trim().isNotEmpty()) {
                            listAddress.add(
                                CreateCustomerRequestModel.Customer.Addresses(
                                    null,
                                    binding.edtStreet.text.toString(),
                                    binding.edtSuite.text.toString(),
                                    binding.edtCity.text.toString(),
                                    binding.edtState.text.toString(),
                                    "United States",
                                    binding.edtZip.text.toString(),
                                    "Shipping",
                                    0.0,
                                    0.0,
                                    "false"
                                )
                            )
                        }

                        data?.addresses_attributes = listAddress
                    }


                    viewModel.createCustomer(addCustomerData)




                } else {
                    redirectToMain(customer)
                }
            }
        }
    }

    private fun redirectToMain(customer: TbCustomer) {

        prefProvider?.setValue(
            Constants.CUSTOMER_NAME,
            customer.first_name + " " + customer.last_name
        )
        prefProvider?.saveCustomerData(customer)

        val result = Bundle().apply {
            putParcelable("data", customer)
            putBoolean("OPEN_ORDER", true)
            putString("TYPE", orderType)
        }
        prefProvider?.setValue(Constants.ORDER_TYPE, Constants.PHONE_ORDER)
        prefProvider?.setValue(Constants.DELIVERY_TYPE, orderType)
        setFragmentResult("request_key_customer", result)

        findNavController().navigateUp()
    }


    private fun observeShowProgress() {

        viewModel.showProgress.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        }


        viewModel.dataCustomer.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {

                    redirectToMain(customer)
                }
            }
        }

    }

    private fun navigate() {

        viewModel.data.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    val bundle = Bundle().apply {
                        putBoolean("isLogin", true)
                    }
                    findNavController().navigate(R.id.action_login_to_passcode, bundle)
                }
            }
        }

    }

    private fun setupSnackbar() {
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)
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
                    Place.Field.ID,
                    Place.Field.NAME,
                    Place.Field.ADDRESS,
                    Place.Field.ADDRESS_COMPONENTS,
                    Place.Field.LAT_LNG
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

}