package com.android.pos.ui.fragments.settings.business

import `in`.madapps.placesautocomplete.PlaceAPI
import `in`.madapps.placesautocomplete.adapter.PlacesAutoCompleteAdapter
import `in`.madapps.placesautocomplete.listener.OnPlacesDetailsListener
import `in`.madapps.placesautocomplete.model.PlaceDetails
import android.R
import android.os.Build
import android.os.Build.ID
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.view.get
import androidx.fragment.app.Fragment
import com.android.pos.databinding.FragmentAddBusnessDetailsBinding
import com.android.pos.utils.MethodUtils
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import java.util.*


class BusinessDetailsFragment : Fragment() {

    private lateinit var binding: FragmentAddBusnessDetailsBinding
    private lateinit var placesApi: PlaceAPI
    private var country = arrayOf("United States", "Canada")

    var placesClient: PlacesClient? = null
    var adapter1: AutoCompleteAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddBusnessDetailsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        setPhoneCountry()
//        setPlaceApi()

        // Setup Places Client
        if (!Places.isInitialized()) {
            Places.initialize(
                requireContext(),
                binding.root.context.getString(com.android.pos.R.string.api_key)
            )
        }

        placesClient = Places.createClient(requireContext())

        initAutoCompleteTextView()

        return binding.root
    }

    private fun initAutoCompleteTextView() {

        binding.edtStreet.threshold = 1
        binding.edtStreet.onItemClickListener = autocompleteClickListener
        adapter1 = AutoCompleteAdapter(requireContext(), placesClient)
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
                        com.android.pos.R.style.SpinnerTheme
                    )
                } else {
                    (parent?.getChildAt(0) as TextView).setTextAppearance(com.android.pos.R.style.SpinnerTheme); }


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
                        com.android.pos.R.style.SpinnerTheme
                    )
                } else {
                    (parent?.getChildAt(0) as TextView).setTextAppearance(com.android.pos.R.style.SpinnerTheme); }


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
                        com.android.pos.R.style.SpinnerTheme
                    )
                    binding.edtStreet.setText("")
                    binding.edtSuite.setText("")
                    binding.edtCity.setText("")
                    binding.edtState.setText("")
                    binding.edtZip.setText("")

                } else {
                    (parent?.getChildAt(0) as TextView).setTextAppearance(com.android.pos.R.style.SpinnerTheme);

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


        binding.edtStreet.setOnFocusChangeListener { v, hasFocus ->
            binding.edtStreet.dismissDropDown()
        }


    }

    private fun setPlaceApi() {
        placesApi =
            PlaceAPI.Builder()
                .apiKey(binding.root.context.getString(com.android.pos.R.string.api_key))
                .build(binding.root.context)

        binding.edtStreet.setAdapter(PlacesAutoCompleteAdapter(binding.root.context, placesApi))
        binding.edtStreet.setOnItemClickListener { parent, view, position, id ->
            val place = parent.getItemAtPosition(position) as Place

            place.id?.let {
                placesApi.fetchPlaceDetails(it, object : OnPlacesDetailsListener {
                    override fun onError(errorMessage: String) {
                    }

                    override fun onPlaceDetailsFetched(placeDetails: PlaceDetails) {

                        var street = ""
                        var suite = ""
                        var city = ""
                        var state = ""
                        var zip = ""
                        placeDetails.address.forEach {
                            it.type.forEach { type ->
                                when {
                                    type.trim().lowercase() == "street_number".trim()
                                        .lowercase() -> {
                                        street += it.longName
                                    }
                                    type.trim().lowercase() == "route".trim().lowercase() -> {
                                        street += it.longName
                                    }
                                    type.trim().lowercase() == "neighborhood".trim()
                                        .lowercase() -> {
                                        suite = it.longName
                                    }
                                    type.trim().lowercase() == "locality".trim()
                                        .lowercase() -> {
                                        city = it.longName
                                    }
                                    type.trim()
                                        .lowercase() == "administrative_area_level_1".trim()
                                        .lowercase() -> {
                                        state = it.longName
                                    }
                                    type.trim().lowercase() == "postal_code".trim()
                                        .lowercase() -> {
                                        zip = it.longName
                                    }
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
                            } catch (e: Exception) {
                            } finally {
                            }
                        }


                    }

                })
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

                        var street = ""
                        var suite = ""
                        var city = ""
                        var state = ""
                        var zip = ""

                        task.place.name?.let { Log.e("Task", it) }
                        task.place.address?.let { Log.e("Task", it) }
                        task.place.addressComponents?.asList()
                            ?.forEachIndexed { index, addressComponent ->

                                addressComponent.types.forEach { type ->

                                    Log.e(
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

                                Log.e("index$index", addressComponent.name)
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