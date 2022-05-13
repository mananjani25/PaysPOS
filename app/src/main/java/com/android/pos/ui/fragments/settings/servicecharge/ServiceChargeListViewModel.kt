package com.android.pos.ui.fragments.settings.servicecharge

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.data.entities.TbServiceCharge
import com.android.pos.data.model.responseModel.CreateServiceChargeResponse
import com.android.pos.data.model.responseModel.ServiceChargeUpdate
import com.android.pos.data.remote.Constants
import com.android.pos.data.repositories.TaxServiceChargeRepository
import com.android.pos.di.PrefProvider
import com.android.pos.utils.Event
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import hilt_aggregated_deps._com_android_pos_ui_dialog_IssueRefundDialog_GeneratedInjector
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


    private val _dataUpdate = MutableLiveData<Event<ServiceChargeUpdate?>>()
    val dataupdate: LiveData<Event<ServiceChargeUpdate?>> = _dataUpdate

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    private val _notifydata = MutableLiveData<Event<CreateServiceChargeResponse.Data?>>()
    val notifydata: LiveData<Event<CreateServiceChargeResponse.Data?>> = _notifydata


    val getDiscountList = taxServiceChargeRepository.getServiceChargeList()

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
        data: CreateServiceChargeResponse.Data
    ) {

        serviceChargeList.forEach {
            if (data.id == it.id) {
                it.isEnabled = data.isEnabled
            } else
                it.isEnabled = false
        }
        viewModelScope.launch {
            taxServiceChargeRepository.addServiceCharges(serviceChargeList)
        }
    }


    fun updateServiceCharge(
        takeoutEnable: Boolean,
        dineinEnable: Boolean,
        locationId: Int
    ) {
        viewModelScope.launch {
            _showProgress.value = Event(true)
            viewModelScope.launch {
                val resource =
                    taxServiceChargeRepository.updateServiceChargeEnable(locationId, takeoutEnable)
                when (resource.status) {
                    Status.SUCCESS -> {
                        _showProgress.value = Event(false)

                        resource.data.let {
                            if (it?.status == 200) {
                                resource.data?.let { servicechargeupdate ->
                                    prefProvider.setValueboolean(
                                        Constants.SERVICECHARGE_TAKEOUT_OPENORDER,
                                        takeoutEnable
                                    )
//                                    prefProvider.setValueboolean(Constants.SERVICECHARGE_DINEIN_ORDER,dineinEnable)
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