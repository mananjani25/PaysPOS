package com.pays.pos.ui.fragments.settings.tax

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pays.pos.data.entities.TaxData
import com.pays.pos.data.model.responseModel.CreateTaxResponse
import com.pays.pos.data.model.responseModel.GetTaxResponse
import com.pays.pos.data.repositories.TaxServiceChargeRepository
import com.pays.pos.utils.Event
import com.pays.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaxListViewModel @Inject constructor(
    private val taxServiceChargeRepository: TaxServiceChargeRepository
) : ViewModel() {

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _data = MutableLiveData<Event<CreateTaxResponse?>>()
    val data: LiveData<Event<CreateTaxResponse?>> = _data

    private val _notifydata = MutableLiveData<Event<Boolean?>>()
    val notifydata: LiveData<Event<Boolean?>> = _notifydata

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    private val _taxesData = MutableLiveData<Event<GetTaxResponse>>()
    val taxesData: LiveData<Event<GetTaxResponse>> = _taxesData

    val getTaxList = taxServiceChargeRepository.getTaxList()

    val taxList = MutableLiveData<List<TaxData>>()
    fun setTaxData() {
        taxList.value = getTaxList.value?.data!!
    }


    fun getTextList() {
        _showProgress.value = Event(true)
        viewModelScope.launch {
            val response =
                taxServiceChargeRepository.getTaxesList()
            when (response.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    response.data.let {
                        if (it?.status == 200) {
                            _taxesData.value = Event(it)
                            _showProgress.value = Event(false)
                            taxServiceChargeRepository.addAllTaxListDatabase(it.data)
                        } else {
                            _snackbarText.value = Event(response.message)
                            _showProgress.value = Event(false)
                        }
                    }
                }
                Status.ERROR -> {
                    _snackbarText.value = Event(response.message)
                    _showProgress.value = Event(false)
                }
                Status.LOADING -> {
                    _showProgress.value = Event(true)
                }
            }
        }
    }

    fun isTaxActive(taxDataItem: TaxData) {

        // _showProgress.value = Event(true)

        viewModelScope.launch {
            taxDataItem.isActive = !taxDataItem.isActive

            val resource =
                taxServiceChargeRepository.taxActive(taxDataItem.id, taxDataItem.isActive)

            when (resource.status) {
                Status.SUCCESS -> {

                    //   _showProgress.value = Event(false)

                    resource.data.let {
                        if (it?.status == 200) {
                            resource.data?.let { baseResponse ->
                                Log.e("checkTax","checkTaxActive ${taxDataItem.isActive}")
                                taxServiceChargeRepository.taxActiveDatabase(
                                    taxDataItem.id,
                                    taxDataItem.isActive
                                )
                                _notifydata.value = Event(true)

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
            val resource = taxServiceChargeRepository.deleteTax(id)
            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)

                    resource.data.let {
                        if (it?.status == 200) {
                            resource.data?.let { createTaxResponse ->
                                taxServiceChargeRepository.deleteTaxDatabase(id)
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
}