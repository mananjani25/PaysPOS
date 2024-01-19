package com.pays.pos.ui.fragments.openorder

import android.text.TextUtils
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pays.pos.R
import com.pays.pos.data.entities.TbCustomer
import com.pays.pos.data.model.requestModel.CreateCustomerRequestModel
import com.pays.pos.data.model.responseModel.BaseResponse
import com.pays.pos.data.model.responseModel.CreateCustomerReponse
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.repositories.PosRepository
import com.pays.pos.di.PrefProvider
import com.pays.pos.utils.Event
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.statusUtils.Resource
import com.pays.pos.utils.statusUtils.Status
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OpenOrderCustomerViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val prefProvider: PrefProvider
):ViewModel()
{
    private lateinit var resource: Resource<CreateCustomerReponse>
    private val TAG = "OpenCustomerViewModel"
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

    val _Basedata = MutableLiveData<Event<TbCustomer?>>()

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


        LogUtil.logE(TAG, "listAddress:  ${Gson().toJson(listAddress)}")
        addCustomerDetails.value?.data?.final_reward = 0
        addCustomerDetails.value?.data?.enroll_to_loyalty = enroll_to_loyalty.value
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
        }
        /*else if (TextUtils.isEmpty(value?.data?.last_name?.trim())) {
            _snackbarText.value = Event(R.string.last_name_validate)
        }*/
        /* else if (value?.data?.phones_attributes?.size == 0) {

             _snackbarText.value = Event(R.string.phone_no_validate)
         }*/ /*else if (value?.data?.phones_attributes?.get(0)?.phone_number?.length!! < 10) {
            _snackbarText.value = Event(R.string.valid_phone_no_validate)
        }*/
        /* else if (TextUtils.isEmpty(value?.data?.email)) {
             _snackbarText.value = Event(R.string.email_validate)
         } else if (!Patterns.EMAIL_ADDRESS.matcher(value?.data?.email).matches()) {
             _snackbarText.value = Event(R.string.valid_email_validate)
         }*/

        /*else if (TextUtils.isEmpty(value?.data?.company?.trim())) {
            _snackbarText.value = Event(R.string.company_name_validate)
        }*/
        /*else if (TextUtils.isEmpty(value?.data?.birth_day) || TextUtils.isEmpty(value?.data?.birth_month) || TextUtils.isEmpty(
                value?.data?.birthday_year
            )
        ) {
            _snackbarText.value = Event(R.string.birth_date_validation)
        }*/ else {
            _showProgress.value = Event(true)
            addCustomerData = CreateCustomerRequestModel().apply {


                LogUtil.logE("DaataJson", "PassData  ${Gson().toJson(value?.data)}")
                data?.first_name = value?.data?.first_name!!
                data?.last_name = value?.data?.last_name!!

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

                data?.addresses_attributes?.addAll(value.data?.addresses_attributes!!)


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
                                        final_reward = customerListReposne.data.final_reward,
                                        company = customerListReposne.data.company
                                    )


                                    posRepository.addCustomer(model)

                                    _Basedata.value = Event(model)

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