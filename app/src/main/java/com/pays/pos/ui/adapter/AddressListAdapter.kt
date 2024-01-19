package com.pays.pos.ui.adapter

import `in`.madapps.placesautocomplete.PlaceAPI
import `in`.madapps.placesautocomplete.adapter.PlacesAutoCompleteAdapter
import `in`.madapps.placesautocomplete.listener.OnPlacesDetailsListener
import `in`.madapps.placesautocomplete.model.Place
import `in`.madapps.placesautocomplete.model.PlaceDetails
import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.R
import com.pays.pos.data.model.requestModel.CreateCustomerRequestModel
import com.pays.pos.databinding.ViewCustomerAddressBinding
import com.pays.pos.utils.LogUtil
import com.google.gson.Gson
import java.util.*


class AddressListAdapter(val refreshCallBack: (Int) -> Unit, val context: Context) :
    RecyclerView.Adapter<AddressListAdapter.MyViewHolder>() {
    private val TAG = "AddressListAdapter"
    private lateinit var placesApi: PlaceAPI
    private var list: ArrayList<CreateCustomerRequestModel.Customer.Addresses> = arrayListOf()
    private var templist: ArrayList<CreateCustomerRequestModel.Customer.Addresses> = arrayListOf()
    private var country = arrayOf("United States", "Canada")


    fun addData(model: CreateCustomerRequestModel.Customer.Addresses) {
        list.add(model)
        notifyItemInserted(list.size)
        templist.add(model)
        notifyItemInserted(list.size)
        //notifyItemRangeInserted(0,list.size )
    }


    inner class MyViewHolder(private val binding: ViewCustomerAddressBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val edtStreet: AutoCompleteTextView = binding.root.findViewById(R.id.edtStreet)
        val edtSuite: EditText = binding.root.findViewById(R.id.edtSuite)
        val edtCity: EditText = binding.root.findViewById(R.id.edtCity)
        val edtState: EditText = binding.root.findViewById(R.id.edtState)
        val edtZip: EditText = binding.root.findViewById(R.id.edtZip)
        val edtCountry: Spinner = binding.root.findViewById(R.id.edtAddress)


        fun bind(model: CreateCustomerRequestModel.Customer.Addresses, pos: Int) {
            if (pos == 0) {
                binding.imgDelete.visibility = View.GONE
            } else {
                binding.imgDelete.visibility = View.VISIBLE
            }


        }

        init {

            binding.imgDelete.setOnClickListener {

                if (templist.get(bindingAdapterPosition).id != null) {
                    templist.get(bindingAdapterPosition)._destroy = "true"
                } else {
                    templist.removeAt(bindingAdapterPosition)
                }

                list.removeAt(bindingAdapterPosition)
                notifyItemRemoved(bindingAdapterPosition)

            }
            val adapter =
                ArrayAdapter(binding.root.context, android.R.layout.simple_spinner_item, country)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.edtAddress.adapter = adapter

            binding.edtStreet.setOnFocusChangeListener { v, hasFocus ->

                edtStreet.dismissDropDown()
            }



            binding.edtAddress.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        LogUtil.logE(TAG, "OnItemSelected ${country[position]}")
                        list[bindingAdapterPosition].country = country[position]
                        notifyItemChanged(bindingAdapterPosition)
                        notifyItemRangeChanged(bindingAdapterPosition, list.size)

                        if (list[bindingAdapterPosition].country?.lowercase() == "Canada".lowercase()) {
                            parent?.getChildAt(1)?.isSelected = true
                        } else {

                            parent?.getChildAt(0)?.isSelected = true

                        }

                        /*if (Build.VERSION.SDK_INT < 23) {
                            if (country[position].lowercase() == "Canada".lowercase()) {


                                (parent?.getChildAt(1) as TextView).setTextAppearance(
                                    view?.context,
                                    R.style.SpinnerTheme
                                )
                            } else {


                                (parent?.getChildAt(0) as TextView).setTextAppearance(
                                    view?.context,
                                    R.style.SpinnerTheme
                                )
                            }
                        } else {
                            if (country[position].lowercase() == "Canada".lowercase()) {


                                parent?.getChildAt(1)?.isSelected = true
                            } else {

                                parent?.getChildAt(0)?.isSelected = true

                            }
                        }*/


                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {

                    }

                }
            //binding.edtAddress.setText("United States")

            placesApi =
                PlaceAPI.Builder().apiKey(binding.root.context.getString(R.string.api_key))
                    .build(binding.root.context)


            edtStreet.setAdapter(PlacesAutoCompleteAdapter(binding.root.context, placesApi))
            edtStreet.setOnItemClickListener { parent, view, position, id ->
                val place = parent.getItemAtPosition(position) as Place

                //binding.edtStreet.setText("${place.description}")
                placesApi.fetchPlaceDetails(place.id, object : OnPlacesDetailsListener {
                    override fun onError(errorMessage: String) {
                    }

                    override fun onPlaceDetailsFetched(placeDetails: PlaceDetails) {

                        decodeLocation(placeDetails.lat, placeDetails.lng, placeDetails.name)

                        val gcd = Geocoder(itemView.context, Locale.getDefault())
                        /* val address: List<Address> =
                             gcd.getFromLocation(placeDetails.lat, placeDetails.lng, 1)*/

                        var street = ""
                        var suite = ""
                        var city = ""
                        var state = ""
                        var zip = ""
                        placeDetails.address.forEach {
                            it.type.forEach { type ->
                                if (type.trim().lowercase() == "street_number".trim().lowercase()) {
                                    street += it.longName
                                } else if (type.trim().lowercase() == "route".trim().lowercase()) {
                                    street += it.longName
                                } else if (type.trim().lowercase() == "neighborhood".trim()
                                        .lowercase()
                                ) {
                                    suite = it.longName
                                } else if (type.trim().lowercase() == "locality".trim()
                                        .lowercase()
                                ) {
                                    city = it.longName
                                } else if (type.trim()
                                        .lowercase() == "administrative_area_level_1".trim()
                                        .lowercase()
                                ) {
                                    state = it.longName
                                } else if (type.trim().lowercase() == "postal_code".trim()
                                        .lowercase()
                                ) {
                                    zip = it.longName
                                }

                            }

                        }


                        if (placeDetails.address.isNotEmpty()) {
                            try {


                                templist[layoutPosition].address1 = street
                                templist[layoutPosition].address2 = suite
                                templist[layoutPosition].city = city
                                templist[layoutPosition].country =
                                    binding.edtAddress.selectedItem.toString()

                                templist[layoutPosition].state = state
                                templist[layoutPosition].postcode = zip


                                list[layoutPosition].address1 = street
                                list[layoutPosition].address2 = suite
                                list[layoutPosition].city = city
                                list[layoutPosition].country =
                                    binding.edtAddress.selectedItem.toString()

                                list[layoutPosition].state = state
                                list[layoutPosition].postcode = zip





                                LogUtil.logE(TAG, "Updatelist:  ${Gson().toJson(templist)}")
                            } catch (e: Exception) {
                                LogUtil.logE(TAG, "exception in pplaces api")
                            } finally {
                                LogUtil.logE(TAG, "notify callback")
                                refreshCallBack.invoke(layoutPosition)
                            }
                        }

                        LogUtil.logE(TAG, "placeDetails:  ${Gson().toJson(placeDetails.name)}")

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
            ViewCustomerAddressBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: AddressListAdapter.MyViewHolder, position: Int) {

        holder.bind(list[position], position)


        holder.edtStreet.setText(list[position].address1)
        holder.edtZip.setText(list[position].postcode)
        holder.edtCity.setText(list[position].city)
        holder.edtSuite.setText(list[position].address2)
        holder.edtState.setText(list[position].state)

        if (list[position].country?.isNotEmpty() == true) {

            if (list[position].country?.lowercase() == "United States".lowercase() && holder.edtCountry.adapter != null) {
                holder.edtCountry.setSelection(0)
            } else {
                holder.edtCountry.setSelection(1)
            }
        }

        /*  if (list[position].country.isNullOrEmpty()) {
              if (list[position].country?.lowercase() == "Canada".lowercase()) {

                  if (Build.VERSION.SDK_INT < 23) {

                      (holder.edtCountry.getChildAt(1) as TextView).setTextAppearance(
                          holder.edtCountry.context,
                          R.style.SpinnerTheme
                      )
                  } else {
                      (holder.edtCountry.getChildAt(1) as TextView).setTextAppearance(R.style.SpinnerTheme);
                  }
              } else if (list[position].country?.lowercase() == "United States".lowercase()){

                  if (Build.VERSION.SDK_INT < 23) {
                      (holder.edtCountry.getChildAt(0) as TextView).setTextAppearance(
                          holder.edtCountry.context,
                          R.style.SpinnerTheme
                      )
                  } else {
                      (holder.edtCountry.getChildAt(0) as TextView).setTextAppearance(R.style.SpinnerTheme);
                  }
              }
          }*/

        /* if (list[position].country.isNullOrEmpty()) {
             if (list[position].country?.lowercase() == "Canada".lowercase()) {
                 holder.edtCountry.setSelection(1)
                 if (Build.VERSION.SDK_INT < 23) {

                     (holder.edtCountry.getChildAt(1) as TextView).setTextAppearance(
                         holder.edtCountry.context,
                         R.style.SpinnerTheme
                     )
                 } else {
                     (holder.edtCountry.getChildAt(1) as TextView).setTextAppearance(R.style.SpinnerTheme);
                 }
             } else if (list[position].country?.lowercase() == "United States".lowercase()){
                 holder.edtCountry.setSelection(0)
                 if (Build.VERSION.SDK_INT < 23) {
                     (holder.edtCountry.getChildAt(0) as TextView).setTextAppearance(
                         holder.edtCountry.context,
                         R.style.SpinnerTheme
                     )
                 } else {
                     (holder.edtCountry.getChildAt(0) as TextView).setTextAppearance(R.style.SpinnerTheme);
                 }
             }
         }*/


        /*  holder.edtStreet.setText(list[position].address1)
          holder.edtSuite.setText(list[position].address2)
          holder.edtCity.setText(list[position].city)
          holder.edtState.setText(list[position].state)
          holder.edtZip.setText(list[position].postcode)
  */

    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun getList(): ArrayList<CreateCustomerRequestModel.Customer.Addresses> {
        return templist
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setAddress(listAdd: ArrayList<CreateCustomerRequestModel.Customer.Addresses>) {
        this.list = listAdd
        this.templist.addAll(listAdd)
        notifyDataSetChanged()

    }

    interface AddressInterface {
        fun onDeleteItem(pos: Int)
    }

}