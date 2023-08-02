package com.android.pos.ui.fragments.customer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.data.entities.TbCustomer
import com.android.pos.data.model.CustomerSearchList
import com.android.pos.data.model.responseModel.BaseResponse
import com.android.pos.data.model.responseModel.GetOrderDetailsResponse
import com.android.pos.data.model.responseModel.orderhistory.Orders
import com.android.pos.data.remote.Constants
import com.android.pos.data.repositories.PosRepository
import com.android.pos.data.repositories.TaxServiceChargeRepository
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
    private val taxServiceChargeRepository: TaxServiceChargeRepository,
    private val prefProvider: PrefProvider
) : ViewModel() {

    var customerId: String? = ""
    private lateinit var resource: Resource<BaseResponse>

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _orderResponse = MutableLiveData<Event<GetOrderDetailsResponse.Data>>()
    val orderResponse: LiveData<Event<GetOrderDetailsResponse.Data>> = _orderResponse


    val _customerListResponse = MutableLiveData<Event<ArrayList<TbCustomer?>>>()
    val _customerNoDataFound = MutableLiveData<Event<String>>()


    private val mdata = MutableLiveData<Event<BaseResponse?>>()
    val data: LiveData<Event<BaseResponse?>> = mdata


    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    private val _orderHistory = MutableLiveData<Event<List<Orders>?>>()
    val orderHistory: LiveData<Event<List<Orders>?>> = _orderHistory

    val serviceCharges = posRepository.serviceChargeList()

    var itemlist = posRepository.getWholeItemFromPos()

    val enableTaxes = taxServiceChargeRepository.enableTaxes()  // fetch active taxes from DB

    fun customerList(data: LinkedHashMap<String, String>) =
        posRepository.customerListPagination(data)

    fun deleteCart() {
        viewModelScope.launch {
            posRepository.deleteCart(prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0))
        }
    }

    fun getData() {
        _showProgress.value = Event(true)
    }

    fun deleteTbl() {
        viewModelScope.launch {

            //   posRepository.deleteCustomer()
        }
    }

    fun searchByTextCustomer(query: String) {
        viewModelScope.launch {
            val resource: Resource<CustomerSearchList> = posRepository.searchCustomer(query)
            when (resource.status) {
                Status.SUCCESS -> {
                    resource.data?.let { response ->
                        if (response.status == 200) {
                            resource.data?.let { customerlist ->
                                var customerdatalist: ArrayList<TbCustomer?> =
                                    arrayListOf<TbCustomer?>()
                                customerlist.data.forEach {
                                    customerdatalist.add(
                                        TbCustomer(
                                            id = it.id,
                                            first_name = it.first_name,
                                            last_name = it.last_name,
                                            birth_date = it.birth_date,
                                            email = it.email,
                                            enroll_to_loyalty = it.enroll_to_loyalty,
                                            final_reward = it.final_reward,
                                            company = it.company,
                                            phones = it.phones,
                                            addresses = it.addresses
                                        )
                                    )
                                }
                                if (customerlist.data.isNotEmpty()) {
                                    _customerListResponse.value = Event(customerdatalist)
                                } else {
                                    _customerNoDataFound.value = Event(customerlist.message)
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

    fun getReportSummary(isFromSearch: Boolean) {

        if (!isFromSearch) {
            _showProgress.value = Event(true)
        }
        viewModelScope.launch {

            val resourceReport =
                posRepository.getOrderHistory(
                    id = customerId ?: "",
                )
            when (resourceReport.status) {
                Status.SUCCESS -> {
                    if (!isFromSearch) {
                        _showProgress.value = Event(false)
                    }
                    resourceReport.data.let {
                        _orderHistory.postValue(Event(it?.data?.ordersList))
                    }
                }

                Status.ERROR -> {
                    _snackbarText.value = Event(resourceReport.message)
                    if (!isFromSearch) {
                        _showProgress.value = Event(false)
                    }
                }

                Status.LOADING -> {
                    if (!isFromSearch) {
                        _showProgress.value = Event(true)
                    }
                }
            }
        }
    }

    fun apiCallOrderDetails(orderId: Int) {
        viewModelScope.launch {

            _showProgress.value = Event(true)
            val resource = posRepository.orderDetailsById(orderId)


            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    resource.data.let { logInResponse ->
                        if (logInResponse?.status == 200) {
                            resource.data?.data?.let { order ->
                                _orderResponse.value = Event(order)
                                //_data.value = Event(createTaxResponse)
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

    fun showError(message: String) {
        _snackbarText.value = Event(message)
    }
}