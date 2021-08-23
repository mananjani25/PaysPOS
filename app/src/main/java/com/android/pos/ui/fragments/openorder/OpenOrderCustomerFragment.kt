package com.android.pos.ui.fragments.openorder

import `in`.madapps.placesautocomplete.PlaceAPI
import `in`.madapps.placesautocomplete.adapter.PlacesAutoCompleteAdapter
import `in`.madapps.placesautocomplete.listener.OnPlacesDetailsListener
import `in`.madapps.placesautocomplete.model.Place
import `in`.madapps.placesautocomplete.model.PlaceDetails
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.R.color
import com.android.pos.databinding.FragmentOpenOrderBinding
import com.android.pos.utils.MethodUtils
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class OpenOrderCustomerFragment : Fragment(), View.OnClickListener {
    private var country = arrayOf("United States", "Canada")
    private lateinit var binding: FragmentOpenOrderBinding
    private lateinit var placesApi: PlaceAPI
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
        binding.imgBack.setOnClickListener(this)
        binding.llSearch.setOnClickListener(this)
        binding.etSearch.setOnClickListener(this)
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
    }

    override fun onClick(v: View?) {

        when (v?.id) {
            R.id.txtCustomerDetails -> {

                binding.llSearch.visibility = View.VISIBLE
                binding.scrollViewCustomer.visibility = View.VISIBLE
                binding.llDeliveryAddress.visibility = View.GONE
                binding.llBillingAddress.visibility = View.GONE

                binding.viewCustomer.setBackgroundResource(color.txt_color_blue)
                binding.viewDelivery.setBackgroundResource(0)
                binding.viewBilling.setBackgroundResource(0)

                binding.txtCustomerDetails.setTextColor(resources.getColor(color.txt_color_blue))
                binding.txtDeliveryAddress.setTextColor(resources.getColor(color.drawerBack50))
                binding.txtBillingAddress.setTextColor(resources.getColor(color.drawerBack50))

            }
            R.id.txtDeliveryAddress -> {
                binding.scrollViewCustomer.visibility = View.GONE
                binding.llDeliveryAddress.visibility = View.VISIBLE
                binding.llBillingAddress.visibility = View.GONE
                binding.llSearch.visibility = View.GONE

                binding.txtCustomerDetails.setTextColor(resources.getColor(color.drawerBack50))
                binding.txtDeliveryAddress.setTextColor(resources.getColor(color.txt_color_blue))
                binding.txtBillingAddress.setTextColor(resources.getColor(color.drawerBack50))

                binding.viewCustomer.setBackgroundResource(0)
                binding.viewDelivery.setBackgroundResource(color.txt_color_blue)
                binding.viewBilling.setBackgroundResource(0)

            }
            R.id.txtBillingAddress -> {
                binding.scrollViewCustomer.visibility = View.GONE
                binding.llDeliveryAddress.visibility = View.GONE
                binding.llSearch.visibility = View.GONE
                binding.llBillingAddress.visibility = View.VISIBLE

                binding.txtCustomerDetails.setTextColor(resources.getColor(color.drawerBack50))
                binding.txtDeliveryAddress.setTextColor(resources.getColor(color.drawerBack50))
                binding.txtBillingAddress.setTextColor(resources.getColor(color.txt_color_blue))

                binding.viewCustomer.setBackgroundResource(0)
                binding.viewDelivery.setBackgroundResource(0)
                binding.viewBilling.setBackgroundResource(color.txt_color_blue)

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
        }

    }

    private fun setPhoneCountry() {
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, country)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spDelivery.adapter = adapter
        binding.spDelivery.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {

                if (Build.VERSION.SDK_INT < 23) {
                    (parent?.getChildAt(0) as TextView).setTextAppearance(
                        view?.context,
                        R.style.SpinnerTheme
                    )
                } else {
                    if (parent?.getChildAt(0) != null)
                        (parent.getChildAt(0) as TextView).setTextAppearance(R.style.SpinnerTheme); }


            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

        }

        binding.spBill.adapter = adapter
        binding.spBill.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {

                if (Build.VERSION.SDK_INT < 23) {
                    (parent?.getChildAt(0) as TextView).setTextAppearance(
                        view?.context,
                        R.style.SpinnerTheme
                    )
                } else {
                    (parent?.getChildAt(0) as TextView).setTextAppearance(R.style.SpinnerTheme); }


            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

        }
    }
}