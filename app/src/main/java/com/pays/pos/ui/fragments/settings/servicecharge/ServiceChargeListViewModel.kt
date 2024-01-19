package com.pays.pos.ui.fragments.settings.servicecharge

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pays.pos.data.entities.TbServiceCharge
import com.pays.pos.data.model.responseModel.CreateServiceChargeResponse
import com.pays.pos.data.model.responseModel.ServiceChargeListResponse
import com.pays.pos.data.model.responseModel.ServiceChargeUpdate
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.repositories.TaxServiceChargeRepository
import com.pays.pos.di.PrefProvider
import com.pays.pos.utils.Event
import com.pays.pos.utils.statusUtils.Resource
import com.pays.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ServiceChargeListViewModel @Inject constructor(
    private val taxServiceChargeRepository: TaxServiceChargeRepository,
    private val prefProvider: PrefProvider,
) : ViewModel() {

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _data = MutableLiveData<Event<CreateServiceChargeResponse?>>()
    val data: LiveData<Event<CreateServiceChargeResponse?>> = _data


    private val _servicedata = MutableLiveData<Event<ServiceChargeListResponse.Data>>()
    val servicedata: LiveData<Event<ServiceChargeListResponse.Data>> = _servicedata


    private val _dataUpdate = MutableLiveData<Event<ServiceChargeUpdate?>>()
    val dataupdate: LiveData<Event<ServiceChargeUpdate?>> = _dataUpdate

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    private val _notifydata = MutableLiveData<Event<CreateServiceChargeResponse.Data?>>()
    val notifydata: LiveData<Event<CreateServiceChargeResponse.Data?>> = _notifydata



     fun getServiceChargeWholeList() {
        _showProgress.value = Event(true)
        viewModelScope.launch {
            val responsee =
                taxServiceChargeRepository.getServiceChargeWholeList(prefProvider.getValueInt(Constants.TERMINAL_ID, -1))
            when (responsee.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    responsee.data.let {
                        if (it?.status == 200) {
                            _servicedata.value = Event(it.data)
                            _showProgress.value = Event(false)
                        } else {
                            _snackbarText.value = Event(responsee.message)
                            _showProgress.value = Event(false)
                        }
                    }


                }
                Status.ERROR -> {
                    _snackbarText.value = Event(responsee.message)
                    _showProgress.value = Event(false)
                }

                Status.LOADING -> {
                    _showProgress.value = Event(true)
                }
            }

        }
    }

    fun isSerChargeActive(serChargeItem: TbServiceCharge) {

        // _showProgress.value = Event(true)

        viewModelScope.launch {
            serChargeItem.isEnabled = !serChargeItem.isEnabled

            val resource =
                taxServiceChargeRepository.serChargeActive(
                    serChargeItem.id,
                    serChargeItem.isEnabled
                )

            when (resource.status) {
                Status.SUCCESS -> {

                    //   _showProgress.value = Event(false)

                    resource.data.let {
                        if (it?.status == 200) {
                            resource.data?.let { baseResponse ->

                                _notifydata.value = Event(baseResponse.data)

                            }
                        } else {
                            _snackbarText.value = Event(resource.message)
                        }

                    }

                }

                Status.ERROR -> {
                    _snackbarText.value = Event(resource.message)
                    //_showProgress.value = Event(false)
                }

                Status.LOADING -> {
                    // _showProgress.value = Event(true)
                }
            }
        }


    }

    fun delete(id: Int) {
        _showProgress.value = Event(true)

        viewModelScope.launch {
            val resource = taxServiceChargeRepository.deleteServiceCharge(id)
            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)

                    resource.data.let {
                        if (it?.status == 200) {
                            resource.data?.let { createTaxResponse ->
                                taxServiceChargeRepository.deleteSerChargeDatabase(id)
                                _data.value = Event(createTaxResponse)
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

    fun updateData(data: CreateServiceChargeResponse.Data) {

        viewModelScope.launch {
            taxServiceChargeRepository.serChargeActiveDatabase(
                data.id,
                data.isEnabled
            )
        }

    }

    fun updateData(
        serviceChargeList: ArrayList<TbServiceCharge>,
    ) {
        viewModelScope.launch {
            taxServiceChargeRepository.addServiceCharges(serviceChargeList)
        }
    }


    fun updateServiceCharge(
        enableservice: Boolean,
        isFromTakeout:Boolean,
        locationId: Int
    ) {
        viewModelScope.launch {
            _showProgress.value = Event(true)
            viewModelScope.launch {
                var resource:Resource<ServiceChargeUpdate>?=null
                resource = if(isFromTakeout){
                    taxServiceChargeRepository.updateServiceChargeEnable(locationId, enableservice)
                }else{
                    taxServiceChargeRepository.updateServiceChargeDineinEnable(locationId, enableservice)
                }
                when (resource.status) {
                    Status.SUCCESS -> {
                        _showProgress.value = Event(false)

                        resource.data.let {
                            if (it?.status == 200) {
                                resource.data?.let { servicechargeupdate ->


                                    prefProvider.setValueboolean(
                                        Constants.SERVICECHARGE_TAKEOUT_OPENORDER,
                                        servicechargeupdate.data.serviceChargeEnable
                                    )
                                    prefProvider.setValueboolean(Constants.SERVICECHARGE_DINEIN_ORDER,servicechargeupdate.data.enableDineInServiceCharge)
                                    _dataUpdate.value = Event(servicechargeupdate)
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