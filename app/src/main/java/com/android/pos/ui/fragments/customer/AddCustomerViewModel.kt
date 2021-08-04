package com.android.pos.ui.fragments.customer

import android.text.TextUtils
import android.text.TextUtils.replace
import android.util.Log
import android.util.Patterns
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.R
import com.android.pos.data.model.CustomerListResponse
import com.android.pos.data.model.requestModel.CreateCustomerRequestModel
import com.android.pos.data.model.responseModel.BaseResponse
import com.android.pos.data.model.responseModel.CreateCustomerReponse
import com.android.pos.data.remote.Constants
import com.android.pos.data.repositories.PosRepository
import com.android.pos.di.PrefProvider
import com.android.pos.utils.Event
import com.android.pos.utils.statusUtils.Resource
import com.android.pos.utils.statusUtils.Status
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import okhttp3.internal.notify
import org.w3c.dom.Text
import java.util.regex.Pattern
import javax.inject.Inject

@HiltViewModel
class AddCustomerViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val prefProvider: PrefProvider
) : ViewModel() {
    private val TAG = "AddCustomerViewModel"
    private lateinit var addCustomerData: CreateCustomerRequestModel
    val locationId = prefProvider.getValueInt(Constants.LOCATION_ID, 0)

    private var customerID: Int = -1

    private var isEdit: Boolean = false

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _data = MutableLiveData<Event<Boolean?>>()
    val data: LiveData<Event<Boolean?>> = _data

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    val _Basedata = MutableLiveData<Event<BaseResponse?>>()

    val addCustomerDetails = MutableLiveData(CreateCustomerRequestModel())
    var listAddress: ArrayList<CreateCustomerRequestModel.Customer.Addresses> = arrayListOf()

    val phoneNo = MutableLiveData<String>()
    var address1 = MutableLiveData<String>()
    val address2 = MutableLiveData<String>()
    val city = MutableLiveData<String>()
    val state = MutableLiveData<String>()
    var pin = MutableLiveData<String>()


    var straddress1: String = ""
    var straddress2: String = ""
    var strcity: String = ""
    var strstate = ""
    var strPin = ""
    var isEmptyAddress = false


    fun setAddress1(adr: String) {
        this.straddress1 = adr
    }

    fun setAddress2(str: String) {
        this.straddress2 = str
    }

    fun setCity(str: String) {
        this.strcity = str
    }

    fun setAddressList(list: ArrayList<CreateCustomerRequestModel.Customer.Addresses>) {
        this.listAddress.clear()
        this.listAddress = list
    }

    fun setState(str: String) {
        this.strstate = str
    }

    fun setPinCode(str: String) {
        this.strPin = str
    }

    private lateinit var resource: Resource<CreateCustomerReponse>

    fun isEditData(isEditData: Boolean, id: Int) {
        this.isEdit = isEditData
        this.customerID = id
    }

    fun submit() {
        if (phoneNo.value != null) {
            addCustomerDetails.value?.data?.phones_attributes?.add(
                0,
                CreateCustomerRequestModel.Customer.Phone(
                    phone_number =
                    phoneNo.value.toString().replace(
                        ("[\\D]").toRegex(),
                        ""
                    ),
                )
            )
        }


        Log.e("Address1", "address1: ${address1.value}")
        Log.e("Address1", "straddress1: ${straddress1}")


        addCustomerDetails.value?.data?.addresses_attributes?.addAll(listAddress)


        val value = addCustomerDetails.value
        /* value?.data?.addresses_attributes?.forEach {
             if (it.address1.isEmpty()) {

                 isEmptyAddress = true
                 return@forEach
             } else if (it.address2.isEmpty()) {
                 isEmptyAddress = false
                 return@forEach
             } else if (it.city.isEmpty()) {
                 isEmptyAddress = false
                 return@forEach
             } else if (it.state.isEmpty()) {
                 isEmptyAddress = false
                 return@forEach
             } else if (it.postcode.isEmpty()) {
                 //_snackbarText.value = Event(R.string.address_empty_validation)
                 isEmptyAddress = false
                 return@forEach
             } else {
                 isEmptyAddress = false
             }

         }
 */
        if (TextUtils.isEmpty(value?.data?.first_name?.trim())) {
            _snackbarText.value = Event(R.string.first_name_validate)
        } else if (TextUtils.isEmpty(value?.data?.last_name?.trim())) {
            _snackbarText.value = Event(R.string.last_name_validate)
        } else if (value?.data?.phones_attributes?.size == 0) {

            _snackbarText.value = Event(R.string.phone_no_validate)
        } else if (TextUtils.isEmpty(value?.data?.email)) {
            _snackbarText.value = Event(R.string.email_validate)
        } else if (!Patterns.EMAIL_ADDRESS.matcher(value?.data?.email).matches()) {
            _snackbarText.value = Event(R.string.valid_email_validate)
        }

        /*else if (TextUtils.isEmpty(value?.data?.company?.trim())) {
            _snackbarText.value = Event(R.string.company_name_validate)
        }*/
        else if (TextUtils.isEmpty(value?.data?.birth_day) || TextUtils.isEmpty(value?.data?.birth_month) || TextUtils.isEmpty(
                value?.data?.birthday_year
            )
        ) {
            _snackbarText.value = Event(R.string.birth_date_validation)
        } else {
            _showProgress.value = Event(true)
            addCustomerData = CreateCustomerRequestModel().apply {


                Log.e("DaataJson", "PassData  ${Gson().toJson(value?.data)}")
                data?.first_name = value?.data?.first_name!!
                data?.last_name = value?.data?.last_name!!

                data?.phones_attributes?.add(
                    0, CreateCustomerRequestModel.Customer.Phone(
                        phone_number = phoneNo.value.toString().replace(
                            ("[\\D]").toRegex(),
                            ""
                        )
                    )
                )


                data?.email = value.data!!.email
                data?.birth_day = value.data!!.birth_day
                data?.birth_month = value.data!!.birth_month
                data?.birthday_year = value.data!!.birthday_year
                data?.company = value.data!!.company

                data?.addresses_attributes?.addAll(value?.data?.addresses_attributes!!)


            }

            Log.e(TAG, "addCustomerDataJson:  ${Gson().toJson(addCustomerData)}")
            Log.e(TAG, "isEdit:  ${isEdit}")
            Log.e(TAG, "customerID:  ${customerID}")
            viewModelScope.launch {
                resource = if (isEdit) {
                    posRepository.updateCustomer(customerID, addCustomerData)
                } else {

                    posRepository.createCustomer(addCustomerData)
                }

                when (resource.status) {
                    Status.SUCCESS -> {
                        _showProgress.value = Event(false)

                        resource.data.let {
                            if (it?.status == 200) {

                                resource.data?.let { customerListReposne ->

                                    val model = CustomerListResponse.Data(
                                        id = customerListReposne.data.id,
                                        first_name = customerListReposne.data.first_name,
                                        last_name = customerListReposne.data.last_name,
                                        birth_date = customerListReposne.data.birth_date,
                                        email = customerListReposne.data.email,
                                        phones = customerListReposne.data.phones,
                                        addresses = customerListReposne.data.addresses
                                    )

                                    if (isEdit) {
                                        posRepository.updateCustomer(
                                            customerListReposne.data.id,
                                            customerListReposne.data.first_name,
                                            customerListReposne.data.last_name,
                                            customerListReposne.data.email,
                                            customerListReposne.data.birth_date,
                                            customerListReposne.data.phones,
                                            customerListReposne.data.addresses
                                        )


                                    } else {
                                        posRepository.addCustomer(model)
                                    }
                                    _Basedata.value = Event(customerListReposne)

                                }
                            } else {
                                _snackbarText.value = Event(resource.message)
                            }
                        }
                    }
                    Status.ERROR -> {
                        _snackbarText.value = Event(resource.message)
                        _showProgress.value = Event(false)
                    }
                    Status.LOADING -> {
                        _showProgress.value = Event(true)
                    }

                }

            }

        }


    }


}