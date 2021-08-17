package com.android.pos.ui.fragments.settings.customerreceipt

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.data.db.AppDatabase
import com.android.pos.data.model.responseModel.GetCustomerReceiptSettingsResponse
import com.android.pos.data.repositories.TaxServiceChargeRepository
import com.android.pos.di.PrefProvider
import com.android.pos.utils.Event
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomerReceiptViewModel @Inject constructor(
    private val taxServiceChargeRepository: TaxServiceChargeRepository,
    private val prefProvider: PrefProvider,
    private val appDatabase: AppDatabase
) : ViewModel() {
    private val TAG = "CustomerReceiptViewModel"
    private val _snackbarText = MutableLiveData<Event<Any?>>()
    private val _showProgress = MutableLiveData<Event<Boolean>>()

    private val customerData = MutableLiveData<GetCustomerReceiptSettingsResponse>()

    private val _data = MutableLiveData<Event<String>>()
    val data: LiveData<Event<String>> = _data
    val showProgress: LiveData<Event<Boolean>> = _showProgress



    init {
        _showProgress.value = Event(true)
        viewModelScope.launch {
            val resource = taxServiceChargeRepository.getCustomerReceiptSettings()
            when (resource.status) {
                Status.ERROR -> {
                    _snackbarText.value = Event(resource.message)
                    _showProgress.value = Event(false)
                }
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    resource.data.let {
                        customerData.value = it

                    }


                }
                Status.LOADING -> {
                    _showProgress.value = Event(true)

                }
            }
        }


    }

}