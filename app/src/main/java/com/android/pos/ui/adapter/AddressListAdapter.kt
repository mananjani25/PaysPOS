package com.android.pos.ui.adapter

import `in`.madapps.placesautocomplete.PlaceAPI
import `in`.madapps.placesautocomplete.adapter.PlacesAutoCompleteAdapter
import `in`.madapps.placesautocomplete.listener.OnPlacesDetailsListener
import `in`.madapps.placesautocomplete.model.Place
import `in`.madapps.placesautocomplete.model.PlaceDetails
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.model.requestModel.CreateCustomerRequestModel
import com.android.pos.databinding.ViewCustomerAddressBinding
import com.google.gson.Gson
import java.util.*
import kotlin.collections.ArrayList

class AddressListAdapter(
    val context: Context,
    var list: ArrayList<CreateCustomerRequestModel.Customer.Addresses>,
    val listner:AddressInterface
) : RecyclerView.Adapter<AddressListAdapter.MyViewHolder>() {
    private val TAG = "AddressListAdapter"
    private lateinit var placesApi: PlaceAPI

    inner class MyViewHolder(private val binding: ViewCustomerAddressBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(model: CreateCustomerRequestModel.Customer.Addresses) {
            if (layoutPosition == 0){
                binding.imgDelete.visibility = View.GONE
            }
            else{
                binding.imgDelete.visibility = View.VISIBLE
            }
            binding.model = model
            binding.executePendingBindings()

        }

        init {
            binding.imgDelete.setOnClickListener {
                listner.onDeleteItem(layoutPosition)
            }

            binding.edtAddress.setText("United States")

            placesApi =
                PlaceAPI.Builder().apiKey(context.getString(R.string.api_key)).build(context)
            binding.edtStreet.setAdapter(PlacesAutoCompleteAdapter(context, placesApi))
            binding.edtStreet.setOnItemClickListener { parent, view, position, id ->
                val place = parent.getItemAtPosition(position) as Place

                Log.e(TAG, "placeJson:  ${Gson().toJson(place)}")
                //binding.edtStreet.setText("${place.description}")
                placesApi.fetchPlaceDetails(place.id, object : OnPlacesDetailsListener {
                    override fun onError(errorMessage: String) {

                    }

                    override fun onPlaceDetailsFetched(placeDetails: PlaceDetails) {
                        decodeLocation(placeDetails.lat, placeDetails.lng, placeDetails.name)

                        val gcd: Geocoder = Geocoder(context, Locale.getDefault())
                        var address: List<Address> =
                            gcd.getFromLocation(placeDetails.lat, placeDetails.lng, 1)
                        Log.e(TAG, "CountryNAme ${address.get(0).countryName}")

                        if (address.isNotEmpty()) {
                            binding.edtStreet.setText(placeDetails.name)
                            binding.edtSuite.setText(placeDetails.name)
                            binding.edtCity.setText(address.get(0).locality)
                            binding.edtState.setText(address.get(0).adminArea)
                            binding.edtZip.setText(address.get(0).postalCode)

                        }


                        Log.e(TAG, "placeDetails:  ${Gson().toJson(placeDetails.name)}")

                    }

                })

            }


        }

    }

    private fun decodeLocation(lat: Double, lng: Double, place: String) {

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AddressListAdapter.MyViewHolder {
        val binding =
            ViewCustomerAddressBinding.inflate(LayoutInflater.from(context), parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: AddressListAdapter.MyViewHolder, position: Int) {
        holder.bind(list[position])

    }

    override fun getItemCount(): Int {
        return list.size
    }

    interface AddressInterface{
        fun onDeleteItem(pos:Int)
    }

}