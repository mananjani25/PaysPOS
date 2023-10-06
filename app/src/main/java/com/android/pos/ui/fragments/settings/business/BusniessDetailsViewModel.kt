package com.android.pos.ui.fragments.settings.business

import android.text.TextUtils
import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.data.db.AppDatabase
import com.android.pos.data.entities.BusinessAddress
import com.android.pos.data.entities.TbBusinessDetails
import com.android.pos.data.model.responseModel.BaseResponse
import com.android.pos.data.model.responseModel.BusinessResponse
import com.android.pos.data.remote.Constants
import com.android.pos.data.repositories.PosRepository
import com.android.pos.di.PrefProvider
import com.android.pos.utils.Event
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BusniessDetailsViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val appDatabase: AppDatabase,
    private val prefProvider: PrefProvider
) : ViewModel() {


    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _data = MutableLiveData<Event<BusinessResponse?>>()
    val data: LiveData<Event<BusinessResponse?>> = _data

    private val _data1 = MutableLiveData<Event<BaseResponse?>>()
    val data1: LiveData<Event<BaseResponse?>> = _data1


    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    private val _notifydata = MutableLiveData<Event<Boolean?>>()
    val notifydata: LiveData<Event<Boolean?>> = _notifydata


    val getTimeZones = posRepository.getTimeZones()
    val getBusinessData = posRepository.getBusinessData()


    fun submit(model: TbBusinessDetails) {


        when {
            TextUtils.isEmpty(model.business_name?.trim()) -> {
                _snackbarText.value = Event("Please enter business name")
            }
            TextUtils.isEmpty(model.phone_number?.trim()) -> {
                _snackbarText.value = Event("Please enter business phone number")
            }
            (model.phone_number?.replace("[^0-9]".toRegex(), "")!!.length < 10) -> {
                _snackbarText.value = Event("Please enter valid business phone number")
            }
            (!TextUtils.isEmpty(model.phone_number_2?.trim()) && (model.phone_number_2?.replace(
                "[^0-9]".toRegex(),
                ""
            )!!.length < 10)) -> {
                _snackbarText.value = Event("Please enter valid business phone number2")
            }
            TextUtils.isEmpty(model.customer_contact_email?.trim()) -> {
                _snackbarText.value = Event("Please enter business email address")
            }
            TextUtils.isEmpty(model.businessAddress[0].address1.trim()) -> {
                _snackbarText.value = Event("Please enter business address")
            }
            TextUtils.isEmpty(model.businessAddress[0].city.trim()) -> {
                _snackbarText.value = Event("Please enter business address city")
            }
            TextUtils.isEmpty(model.businessAddress[0].state.trim()) -> {
                _snackbarText.value = Event("Please enter business address state")
            }
            TextUtils.isEmpty(model.businessAddress[0].postcode.trim()) -> {
                _snackbarText.value = Event("Please enter business address postcode")
            }
            (!TextUtils.isEmpty(model.business_website?.trim()) && !Patterns.WEB_URL.matcher(model.business_website?.trim().toString()).matches())-> {
                _snackbarText.value = Event("Please enter valid business website")
            }
            else -> {
                _showProgress.value = Event(true)


                viewModelScope.launch {

                    val resource = posRepository.updateBusiness(model.id, model)

                    when (resource.status) {
                        Status.SUCCESS -> {
                            _showProgress.value = Event(false)

                            resource.data.let {
                                if (it?.status == 200) {

                                    resource.data?.let { customerListReposne ->


                                        val model = TbBusinessDetails()
                                        model.id = customerListReposne.data.id
                                        model.business_name = customerListReposne.data.businessName
                                        model.business_website =
                                            customerListReposne.data.businessWebsite
                                        model.phone_number = customerListReposne.data.phoneNumber
                                        model.phone_number_2 = customerListReposne.data.phoneNumber2
                                        model.time_zone = customerListReposne.data.timeZone
                                        model.customer_contact_email =
                                            customerListReposne.data.customerContactEmail

                                        prefProvider.setValue(
                                            Constants.BUSINESS_NAME,
                                            customerListReposne.data.businessName
                                        )
                                        prefProvider.setValue(
                                            Constants.SYSTEM_TIMEZONE,
                                            customerListReposne.data.timeZone
                                        )
                                        prefProvider.setValue(
                                            Constants.BUSINESS_PHONE_NO,
                                            customerListReposne.data.phoneNumber
                                        )
                                        prefProvider.setValue(
                                            Constants.BUSINESS_WEBSITE,
                                            customerListReposne.data.businessWebsite
                                        )
                                        //Need to add address in string in below prefs
                                        /*if (it.settingData.data.address != null) {
                                            prefProvider.setValue(
                                                Constants.BUSINESS_ADDRESS,
                                                it.settingData.data.address
                                            )
                                        }*/

                                        val address =
                                            customerListReposne.data.addressAttributes.addressableType.let { it1 ->
                                                BusinessAddress(
                                                    bid = customerListReposne.data.addressAttributes.bid,
                                                    address1 = customerListReposne.data.addressAttributes.address1,
                                                    address2 = customerListReposne.data.addressAttributes.address2,
                                                    city = customerListReposne.data.addressAttributes.city,
                                                    state = customerListReposne.data.addressAttributes.state,
                                                    country = customerListReposne.data.addressAttributes.country,
                                                    postcode = customerListReposne.data.addressAttributes.postcode,
                                                    addressableType = it1,
                                                    addressableId = customerListReposne.data.addressAttributes.addressableId,
                                                    createdAt = customerListReposne.data.addressAttributes.createdAt,
                                                    updatedAt = customerListReposne.data.addressAttributes.updatedAt,
                                                    latitude = customerListReposne.data.addressAttributes.latitude,
                                                    longitude = customerListReposne.data.addressAttributes.longitude,
                                                    typeOfAddress = customerListReposne.data.addressAttributes.typeOfAddress

                                                )
                                            }

                                        model.businessAddress = listOf(address)


                                        appDatabase.businessDetailsDao().add(model)

                                        _data.value = Event(resource.data)


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

}