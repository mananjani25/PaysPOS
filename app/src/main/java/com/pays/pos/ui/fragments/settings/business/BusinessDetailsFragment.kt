package com.pays.pos.ui.fragments.settings.business

import android.R
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.pays.pos.data.entities.BusinessAddress
import com.pays.pos.data.entities.TbBusinessDetails
import com.pays.pos.databinding.FragmentAddBusnessDetailsBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.MethodUtils
import com.pays.pos.utils.ProgressUtils
import com.pays.pos.utils.extensions.liveSnackBar
import com.pays.pos.utils.statusUtils.Status
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BusinessDetailsFragment : Fragment() {

    private var businessAddress: BusinessAddress? = null
    private var addressID: Int? = null
    private lateinit var binding: FragmentAddBusnessDetailsBinding
    private var country = arrayOf("United States", "Canada")
    private val viewModel by viewModels<BusniessDetailsViewModel>()

    var placesClient: PlacesClient? = null
    var adapter1: _root_ide_package_.com.pays.pos.ui.fragments.settings.business.AutoCompleteAdapter? = null

    private var timeZoneName = ArrayList<String>()
    private var timeZoneValue = ArrayList<String>()

    @Inject
    lateinit var prefProvider: PrefProvider

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddBusnessDetailsBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        setPhoneCountry()

        if (!Places.isInitialized()) {
            Places.initialize(
                requireContext(),
                binding.root.context.getString(com.pays.pos.R.string.api_key)
            )
        }

        placesClient = Places.createClient(requireContext())

        observeShowProgress()
        initAutoCompleteTextView()

        setupDetails()
        timeZonesObserver()

        return binding.root
    }

    private fun setupDetails() {


    }

    private fun initAutoCompleteTextView() {

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
    }


    private fun setPhoneCountry() {
        val adapter = ArrayAdapter(requireContext(), R.layout.simple_spinner_item, country)
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        binding.spPhone.adapter = adapter
        binding.spPhone2.adapter = adapter
        binding.edtAddress.adapter = adapter

        binding.spPhone.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {

                if (Build.VERSION.SDK_INT < 23) {
                    (parent?.getChildAt(0) as TextView).setTextAppearance(
                        view?.context,
                        com.pays.pos.R.style.SpinnerTheme
                    )
                } else {
                    (parent?.getChildAt(0) as TextView).setTextAppearance(com.pays.pos.R.style.SpinnerTheme); }


            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

        }
        binding.spPhone2.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {

                if (Build.VERSION.SDK_INT < 23) {
                    (parent?.getChildAt(0) as TextView).setTextAppearance(
                        view?.context,
                        com.pays.pos.R.style.SpinnerTheme
                    )
                } else {
                    (parent?.getChildAt(0) as TextView).setTextAppearance(com.pays.pos.R.style.SpinnerTheme); }


            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

        }
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
                    binding.edtStreet.setText("")
                    binding.edtSuite.setText("")
                    binding.edtCity.setText("")
                    binding.edtState.setText("")
                    binding.edtZip.setText("")

                } else {
                    (parent?.getChildAt(0) as TextView).setTextAppearance(com.pays.pos.R.style.SpinnerTheme);

                    binding.edtStreet.setText("")
                    binding.edtSuite.setText("")
                    binding.edtCity.setText("")
                    binding.edtState.setText("")
                    binding.edtZip.setText("")
                }


            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

        }
        binding.edtStreet.setOnFocusChangeListener { _, _ ->
            binding.edtStreet.dismissDropDown()
        }


        binding.btnUpdate.setOnClickListener {


            val address =   if (businessAddress == null && binding.edtStreet.text.trim().isNotEmpty()) {

                  BusinessAddress(
                    address1 = binding.edtStreet.text.toString().trim(),
                    address2 = binding.edtSuite.text.toString().trim(),
                    city = binding.edtCity.text.toString().trim(),
                    state = binding.edtState.text.toString().trim(),
                    country = binding.edtAddress.selectedItem.toString(),
                    postcode = binding.edtZip.text.toString().trim(),


                )
            }
            else{
              businessAddress?.let { it1 ->
                    BusinessAddress(
                        bid = addressID!!,
                        address1 = binding.edtStreet.text.toString().trim(),
                        address2 = binding.edtSuite.text.toString().trim(),
                        city = binding.edtCity.text.toString().trim(),
                        state = binding.edtState.text.toString().trim(),
                        country = binding.edtAddress.selectedItem.toString(),
                        postcode = binding.edtZip.text.toString().trim(),
                        addressableType = it1.addressableType ?: "",
                        addressableId = businessAddress!!.addressableId,
                        createdAt = businessAddress!!.createdAt,
                        updatedAt = businessAddress!!.updatedAt,
                        latitude = businessAddress!!.latitude,
                        longitude = businessAddress!!.longitude,
                        typeOfAddress = businessAddress!!.typeOfAddress

                    )
                }

            }
            val model = TbBusinessDetails()
            model.id = prefProvider.getLocationId()
            model.business_name =
                binding.edtBusinessName.text.toString().trim().replace("\\s+".toRegex(), " ")
            model.business_website = binding.edtWebSite.text.toString()
            model.phone_number = binding.edtPhoneNo.text.toString()
            model.phone_number_1_country = binding.spPhone.selectedItem.toString()
            model.phone_number_2_country = binding.spPhone2.selectedItem.toString()
            model.phone_number_2 = binding.edtPhoneNo2.text.toString()
            model.time_zone = timeZoneValue[binding.spTimeZone.selectedItemPosition]
            model.customer_contact_email = binding.edtEmail.text.toString()

            if (address != null) {
                model.businessAddress = listOf(address)
            } else {
                model.businessAddress = emptyList()
            }
            MethodUtils.hideKeyboard(requireActivity())
            viewModel.submit(model)
        }

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


        viewModel.data.observe(viewLifecycleOwner) { event ->

            event.getContentIfNotHandled()?.let {
                AlertUtils.showCustomAlert(requireActivity(), it.message)
            }

        }


        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)

    }


    private fun timeZonesObserver() {

        viewModel.getTimeZones.observe(viewLifecycleOwner) { it ->

            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {


                        resource.data.let {
                            it?.forEach {
                                LogUtil.logE("key", it.name)
                                timeZoneName.add(it.name)
                                timeZoneValue.add(it.value)
                            }
                        }


                        val adapter =
                            ArrayAdapter(
                                requireContext(),
                                R.layout.simple_spinner_item,
                                timeZoneName
                            )
                        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
                        binding.spTimeZone.adapter = adapter

                        getDeatils()
                    }
                    Status.ERROR -> {
                        ProgressUtils.dismissProgressDialog()
                    }
                    Status.LOADING -> {
                        ProgressUtils.showProgressDialog(requireActivity())
                    }
                }
            }


        }
    }

    private fun getDeatils() {

        viewModel.getBusinessData.observe(viewLifecycleOwner) { it ->

            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {

                        ProgressUtils.dismissProgressDialog()


                        resource.data.let {
                            binding.edtBusinessName.setText(it?.business_name)
                            binding.edtEmail.setText(it?.customer_contact_email)
                            binding.edtWebSite.setText(it?.business_website)
                            binding.edtPhoneNo.setText(AlertUtils.usNumberFormat(it?.phone_number.toString()))
                            binding.edtPhoneNo2.setText(AlertUtils.usNumberFormat(it?.phone_number_2.toString()))

                            if (it?.phone_number_1_country == country[0]) {

                                binding.spPhone.setSelection(0)
                                binding.edtAddress.setSelection(0)

                            } else if (it?.phone_number_1_country == country[1]) {
                                binding.spPhone.setSelection(1)
                                binding.edtAddress.setSelection(1)
                            }

                            if (it?.phone_number_2_country == country[0]) {
                                binding.spPhone2.setSelection(0)
                            } else if (it?.phone_number_2_country == country[1]) {
                                binding.spPhone2.setSelection(1)
                            }

                            binding.spTimeZone.setSelection(timeZoneValue.indexOf(it?.time_zone))

                            binding.edtZip.setText(it?.businessAddress?.get(0)?.postcode ?: "")
                            binding.edtState.setText(it?.businessAddress?.get(0)?.state)
                            binding.edtCity.setText(it?.businessAddress?.get(0)?.city)
                            binding.edtSuite.setText(it?.businessAddress?.get(0)?.address2)
                            binding.edtStreet.setText(it?.businessAddress?.get(0)?.address1)


                            if (businessAddress != null) {
                                addressID = it?.businessAddress?.get(0)?.bid
                                businessAddress = it?.businessAddress?.get(0)
                            }
                        }


                    }
                    Status.ERROR -> {
                        ProgressUtils.dismissProgressDialog()
                    }
                    Status.LOADING -> {
                        ProgressUtils.showProgressDialog(requireActivity())
                    }
                }
            }


        }

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