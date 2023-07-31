package com.android.pos.ui.fragments.customer

import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.R
import com.android.pos.data.entities.TbCustomer
import com.android.pos.data.model.requestModel.CreateCustomerRequestModel
import com.android.pos.data.model.responseModel.BaseResponse
import com.android.pos.data.model.responseModel.CreateCustomerReponse
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.DELIVERY
import com.android.pos.data.remote.Constants.DELIVERY_TYPE
import com.android.pos.data.remote.Constants.GIFT_CARD
import com.android.pos.data.remote.Constants.ORDER_TYPE
import com.android.pos.data.remote.Constants.PICK_UP
import com.android.pos.data.remote.Constants.TAKEOUT
import com.android.pos.data.repositories.PosRepository
import com.android.pos.di.PrefProvider
import com.android.pos.utils.Event
import com.android.pos.utils.LogUtil
import com.android.pos.utils.statusUtils.Resource
import com.android.pos.utils.statusUtils.Status
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddCustomerViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val prefProvider: PrefProvider
) : ViewModel() {
    private lateinit var resource: Resource<CreateCustomerReponse>
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

    private val _updatedCustomer = MutableLiveData<Event<TbCustomer>>()
    val updatedCustomer: LiveData<Event<TbCustomer>> = _updatedCustomer

    private val _customerModel = MutableLiveData<Event<TbCustomer>>()
    val customerModel: LiveData<Event<TbCustomer>> = _customerModel

    val addCustomerDetails = MutableLiveData(CreateCustomerRequestModel())
    var listAddress: ArrayList<CreateCustomerRequestModel.Customer.Addresses> = arrayListOf()

    var phoneId: Int? = null
    val addressId = ""
    val phoneNo = MutableLiveData<String>()
    var address1 = MutableLiveData<String>()
    val address2 = MutableLiveData<String>()
    val city = MutableLiveData<String>()
    val state = MutableLiveData<String>()
    var enroll_to_loyalty = MutableLiveData<Boolean>()
    var same_as_billing_address = MutableLiveData<Boolean>()
    var pin = MutableLiveData<String>()


    var straddress1: String = ""
    var straddress2: String = ""
    var strcity: String = ""
    var strstate = ""
    var strPin = ""
    var isEmptyAddress = false


    fun sameAsAddressValueChanges(boolean: Boolean){
        this.same_as_billing_address.value = boolean
    }
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
        this.listAddress = list
    }

    fun setState(str: String) {
        this.strstate = str
    }

    fun setPinCode(str: String) {
        this.strPin = str
    }


    fun isEditData(isEditData: Boolean, id: Int) {
        this.isEdit = isEditData
        this.customerID = id
    }

    fun submit(listAddress: ArrayList<CreateCustomerRequestModel.Customer.Addresses>, isFromPhoneOrderEdit: Boolean) {
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


        LogUtil.logE(TAG, "listAddress:  ${Gson().toJson(this.listAddress)}")
        addCustomerDetails.value?.data?.final_reward = 0
        addCustomerDetails.value?.data?.enroll_to_loyalty = enroll_to_loyalty.value
        addCustomerDetails.value?.data?.same_as_billing_address = same_as_billing_address.value
        addCustomerDetails.value?.data?.addresses_attributes = listAddress


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
        } else if (value?.data?.phones_attributes?.size != 0 && value?.data?.phones_attributes?.get(
                0
            )?.phone_number?.length != 10
        ) {
            _snackbarText.value = Event(R.string.valid_phone_no_validate)
        }else if (!TextUtils.isEmpty(value?.data?.email?.trim()) && !Patterns.EMAIL_ADDRESS.matcher(value.data?.email?.trim())
                .matches()
        ) {
            _snackbarText.value = Event(R.string.valid_email_validate)
        } else if (isFromPhoneOrderEdit && (prefProvider.getValue(DELIVERY_TYPE, PICK_UP) == PICK_UP
                    || prefProvider.getValue(DELIVERY_TYPE, PICK_UP) == DELIVERY)
            && value.data?.phones_attributes?.size == 0
        ) {
            _snackbarText.value = Event(R.string.phone_no_validate)
        } else if (isFromPhoneOrderEdit && (prefProvider.getValue(DELIVERY_TYPE, PICK_UP) == PICK_UP
                    || prefProvider.getValue(DELIVERY_TYPE, PICK_UP) == DELIVERY)
            && value.data?.phones_attributes?.get(0)?.phone_number?.length!! < 10
        ) {
            _snackbarText.value = Event(R.string.valid_phone_no_validate)
        } else if (isFromPhoneOrderEdit && prefProvider.getValue(DELIVERY_TYPE, PICK_UP) == DELIVERY
            && value.data?.addresses_attributes?.size == 0
        ) {
            _snackbarText.value = Event(R.string.please_enter_address)
        } else if (isFromPhoneOrderEdit && prefProvider.getValue(DELIVERY_TYPE, PICK_UP) == DELIVERY
            && value.data?.addresses_attributes?.get(0)?.postcode?.isEmpty() == true
        ) {
            _snackbarText.value = Event(R.string.please_enter_zipcode)
        } else if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == GIFT_CARD && value.data?.phones_attributes?.size == 0) {
            _snackbarText.value = Event(R.string.phone_no_validate)
        } else if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == GIFT_CARD && value.data?.phones_attributes?.get(0)?.phone_number?.length!! < 10){
            _snackbarText.value = Event(R.string.valid_phone_no_validate)
        } else if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == GIFT_CARD && !TextUtils.isEmpty(value?.data?.email?.trim())
            && !Patterns.EMAIL_ADDRESS.matcher(value.data?.email?.trim()).matches()) {
            _snackbarText.value = Event(R.string.valid_email_validate)
        } else {
            _showProgress.value = Event(true)
            addCustomerData = CreateCustomerRequestModel().apply {


                LogUtil.logE("DaataJson", "PassData  ${Gson().toJson(value?.data)}")
                data?.first_name = value?.data?.first_name!!.replaceFirstChar { it.uppercase() }
                data?.last_name = value?.data?.last_name!!.replaceFirstChar { it.uppercase() }

                val phone = CreateCustomerRequestModel.Customer.Phone(
                    id = phoneId,
                    phone_number = phoneNo.value.toString().replace(
                        ("[\\D]").toRegex(),
                        ""
                    )
                )
                if (isEdit) {
                    phone.id = phoneId
                }
                data?.phones_attributes?.add(
                    0, phone
                )

                data?.email = value.data!!.email
                data?.birth_day = value.data!!.birth_day
                data?.birth_month = value.data!!.birth_month
                data?.birthday_year = value.data!!.birthday_year
                data?.company = value.data!!.company
                data?.enroll_to_loyalty = value.data!!.enroll_to_loyalty
                data?.same_as_billing_address = value.data!!.same_as_billing_address

                data?.addresses_attributes = (value.data?.addresses_attributes!!)


            }

            LogUtil.logE(TAG, "addCustomerDataJson:  ${Gson().toJson(addCustomerData)}")
            LogUtil.logE(TAG, "isEdit:  ${isEdit}")
            LogUtil.logE(TAG, "customerID:  ${customerID}")
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

                                    val model = TbCustomer(
                                        id = customerListReposne.data.id,
                                        first_name = customerListReposne.data.first_name,
                                        last_name = customerListReposne.data.last_name,
                                        birth_date = customerListReposne.data.birth_date,
                                        email = customerListReposne.data.email,
                                        phones = customerListReposne.data.phones,
                                        addresses = customerListReposne.data.addresses,
                                        enroll_to_loyalty = customerListReposne.data.enroll_to_loyalty,
                                        same_as_billing_address = customerListReposne.data.same_as_billing_address,
                                        final_reward = customerListReposne.data.final_reward,
                                        company = customerListReposne.data.company,
                                        isSelcted = true,
                                    )


                                    posRepository.addCustomer(model)

                                    if(isFromPhoneOrderEdit) {
                                        _updatedCustomer.value = Event(model)
                                    }else {
                                        _Basedata.value = Event(customerListReposne)
                                        _customerModel.value = Event(model)
                                    }

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