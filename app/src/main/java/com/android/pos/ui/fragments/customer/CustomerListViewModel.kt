package com.android.pos.ui.fragments.customer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.data.model.responseModel.BaseResponse
import com.android.pos.data.model.responseModel.orderhistory.Orders
import com.android.pos.data.repositories.PosRepository
import com.android.pos.di.PrefProvider
import com.android.pos.utils.Event
import com.android.pos.utils.statusUtils.Resource
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
public class CustomerListViewModel @Inject constructor(
    private val posRepository: PosRepository,
    prefProvider: PrefProvider
) : ViewModel() {

    var customerId: String? = ""
    private lateinit var resource: Resource<BaseResponse>

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val mdata = MutableLiveData<Event<BaseResponse?>>()
    val data: LiveData<Event<BaseResponse?>> = mdata


    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    private val _orderHistory = MutableLiveData<Event<List<Orders>?>>()
    val orderHistory: LiveData<Event<List<Orders>?>> = _orderHistory

    fun customerList(data: LinkedHashMap<String, String>) =
        posRepository.customerListPagination(data)


    fun getData() {
        _showProgress.value = Event(true)


    }

    fun deleteTbl() {
        viewModelScope.launch {

            posRepository.deleteCustomer()
        }
    }

    fun delete(id: Int?) {
        viewModelScope.launch {
            resource = posRepository.deleteCustomer(id)

            when (resource.status) {
                Status.SUCCESS -> {

                    _showProgress.value = Event(false)
                    resource.data.let {
                        if (it?.status == 200) {
                            resource.data?.let {
                                posRepository.deleteCustomerDataBase(id)
                                mdata.value = Event(it)
                            }
                        } else {
                            _snackbarText.value = Event(resource.message)
                        }
                    }

                }
                Status.LOADING -> {
                    _showProgress.value = Event(true)

                }
                Status.ERROR -> {
                    _snackbarText.value = Event(resource.message)
                    _showProgress.value = Event(false)

                }

            }
        }


    }

    fun getReportSummary() {

        _showProgress.value = Event(true)
        viewModelScope.launch {

            val resourceReport =
                posRepository.getOrderHistory(
                    id = customerId ?: "",
                )
            when (resourceReport.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    resourceReport.data.let {
                        _orderHistory.postValue(Event(it?.data?.ordersList))
                    }
                }

                Status.ERROR -> {
                    _snackbarText.value = Event(resourceReport.message)
                    _showProgress.value = Event(false)
                }

                Status.LOADING -> {
                    _showProgress.value = Event(true)
                }
            }
        }
    }

}