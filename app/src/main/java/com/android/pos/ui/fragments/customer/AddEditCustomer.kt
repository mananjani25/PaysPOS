package com.android.pos.ui.fragments.customer

import `in`.madapps.placesautocomplete.PlaceAPI
import `in`.madapps.placesautocomplete.adapter.PlacesAutoCompleteAdapter
import `in`.madapps.placesautocomplete.listener.OnPlacesDetailsListener
import `in`.madapps.placesautocomplete.model.Place
import `in`.madapps.placesautocomplete.model.PlaceDetails
import android.app.DatePickerDialog
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.remote.Constants.CUSTOMERDETAILS
import com.android.pos.data.remote.Constants.KEY
import com.android.pos.databinding.FragmentAddEditCustomerBinding
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.liveSnackBar
import com.google.android.libraries.places.api.Places
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import com.google.android.libraries.places.api.net.PlacesClient
import okhttp3.internal.notify
import okhttp3.internal.notifyAll


@AndroidEntryPoint
class AddEditCustomer : Fragment() {
    private lateinit var binding: FragmentAddEditCustomerBinding
    private var isEdit = false
    private val TAG = "AddEditCustomer"
    private val viewModel by viewModels<AddCustomerViewModel>()
    private var currentSelectedDate: Long? = null
    private lateinit var placesApi: PlaceAPI


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


        isEdit = requireArguments().getBoolean("isEdit", false)
        Log.e(TAG, "isEdit  $isEdit")

        searchPlaces()

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


            if (editModel?.phones?.size != 0) {
                viewModel.phoneNo.value =
                    AlertUtils.usNumberFormat(editModel?.phones?.get(0)?.phone_number!!).toString()
            }
            if (editModel.email != null) {
                viewModel.addCustomerDetails.value?.data?.email = editModel?.email
            }

            if (editModel.addresses.size > 0) {
                binding.edtAddress.setText("United States")

                binding.edtStreet.setText("" + editModel.addresses.get(0).address1)
                viewModel.setAddress1(editModel.addresses.get(0).address1)
                binding.edtSuite.setText("" + editModel.addresses.get(0).address2)
                viewModel.setAddress2(editModel.addresses.get(0).address2)
                binding.edtCity.setText("" + editModel.addresses.get(0).city)
                viewModel.setCity(editModel.addresses.get(0).city)
                binding.edtZip.setText("" + editModel.addresses.get(0).postcode)
                viewModel.setPinCode(editModel.addresses.get(0).postcode.toString())
                binding.edtState.setText("" + editModel.addresses.get(0).state.toString())
                viewModel.setState(editModel.addresses.get(0).state)

                /*binding.edtStreet.setText(editModel.addresses.get(0).address1)
                binding.edtSuite.setText(editModel.addresses.get(0).address2)
                binding.edtCity.setText(editModel.addresses.get(0).city)
                binding.edtState.setText(editModel.addresses.get(0).state)
                binding.edtZip.setText(editModel.addresses.get(0).postcode.toString())
*/
            }
            binding.edtCompany.setText("company")
            if (editModel.birth_date != null) {
                binding.edtBirthDay.setText("${editModel.birth_date}")
            }


        } else {
            binding.txtCustomerType.setText("New Customer")


        }
        binding.imgBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.edtBirthDay.setOnClickListener {
            Log.e(TAG, "DatePicker  ")
            showDatePicker()

        }

        /* binding.edtStreet.addTextChangedListener(object : TextWatcher {
             override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

             }

             override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                     searchPlaces(binding.edtStreet.text.toString())



             }

             override fun afterTextChanged(s: Editable?) {

             }

         })
 */
    }

    private fun searchPlaces() {
        placesApi = PlaceAPI.Builder().apiKey(getString(R.string.api_key)).build(requireActivity())
        binding.edtStreet.setAdapter(PlacesAutoCompleteAdapter(requireContext(), placesApi))
        binding.edtStreet.setOnItemClickListener { parent, view, position, id ->
            val place = parent.getItemAtPosition(position) as Place

            Log.e(TAG, "placeJson:  ${Gson().toJson(place)}")
            //binding.edtStreet.setText("${place.description}")
            placesApi.fetchPlaceDetails(place.id, object : OnPlacesDetailsListener {
                override fun onError(errorMessage: String) {

                }

                override fun onPlaceDetailsFetched(placeDetails: PlaceDetails) {
                    decodeLocation(placeDetails.lat, placeDetails.lng, placeDetails.name)

                    Log.e(TAG, "placeDetails:  ${Gson().toJson(placeDetails.name)}")

                }

            })

        }


    }

    private fun decodeLocation(lat: Double, lng: Double, place: String) {
        val gcd: Geocoder = Geocoder(requireContext(), Locale.getDefault())
        var address: List<Address> = gcd.getFromLocation(lat, lng, 1)

        Log.e(TAG, "CountryNAme ${address.get(0).countryName}")
        if (address.size > 0) {


/*

            viewModel.address1.value = place.toString()
            viewModel.address2.value = place.toString()
            viewModel.city.value = address.get(0).locality.toString()
            viewModel.state.value = address.get(0).adminArea.toString()
            viewModel.pin.value = address.get(0).postalCode.toString()
*/

            Log.e("Addredd", "adminArea:   ${address.get(0).adminArea}")

            binding.edtStreet.setText(place)
            viewModel.setAddress1(place)
            binding.edtSuite.setText(place)
            viewModel.setAddress2(place)
            binding.edtCity.setText(address.get(0).locality)
            viewModel.setCity(address.get(0).locality)
            binding.edtState.setText(address.get(0).adminArea)
            viewModel.setState(address.get(0).adminArea)
            binding.edtZip.setText(address.get(0).postalCode)
            viewModel.setPinCode(address.get(0).postalCode)
            //  binding.executePendingBindings()


            Log.e(TAG, "TextSetted")


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

}