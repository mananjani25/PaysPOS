package com.android.pos.ui.fragments.settings.kitchenreceipt

import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Update
import com.android.pos.R
import com.android.pos.data.db.AppDatabase
import com.android.pos.data.entities.TaxData
import com.android.pos.data.model.requestModel.CreateTaxRequestModel
import com.android.pos.data.model.requestModel.UpdateKitchenReceiptRequestModel
import com.android.pos.data.model.responseModel.CreateTaxResponse
import com.android.pos.data.model.responseModel.GetKitchenReceiptSettingsResponse
import com.android.pos.data.model.responseModel.GetTaxResponse
import com.android.pos.data.remote.Constants.LOCATION_ID
import com.android.pos.data.repositories.TaxServiceChargeRepository
import com.android.pos.di.PrefProvider
import com.android.pos.utils.Event
import com.android.pos.utils.statusUtils.Resource
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class KitchenReceiptViewModel @Inject constructor(
    private val taxServiceChargeRepository: TaxServiceChargeRepository,
    private val prefProvider: PrefProvider,
    private val appDatabase: AppDatabase
) : ViewModel() {
    private val TAG = "KitchenReceiptViewModel"
    internal val _snackbarText = MutableLiveData<Event<Any?>>()
    internal val _showProgress = MutableLiveData<Event<Boolean>>()

    val kitchenData = MutableLiveData<GetKitchenReceiptSettingsResponse.Data>()
    val kitchenUpdateData = MutableLiveData<UpdateKitchenReceiptRequestModel>()
    val kitchenId = MutableLiveData<Int>()
    private val _data = MutableLiveData<Event<String>>()
    val data: LiveData<Event<String>> = _data
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    fun updateKitchen(model: UpdateKitchenReceiptRequestModel) {

        _showProgress.value = Event(true)
        viewModelScope.launch {

            val resource =
                taxServiceChargeRepository.updateKitchenReceiptSettings(kitchenId.value, model)
            when (resource.status) {
                Status.LOADING -> {
                    _showProgress.value = Event(true)
                }
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    _data.value = Event(resource.data?.message!!)
                    appDatabase.kitchenSettingsDao().add(resource.data.data)

                }
                Status.ERROR -> {
                    _snackbarText.value = Event(true)

                }

            }

        }
    }

    fun getKitchenSettings() = taxServiceChargeRepository.getKitchenReceiptSettings()

    init {


        /*_showProgress.value = Event(true)
        viewModelScope.launch {

            val resources = taxServiceChargeRepository.getKitchenReceiptSettings()
            when (resources.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    resources.data.let {
                        kitchenData.value = resources.data!!
                        kitchenId.value = resources.data.data.id
                        if (it != null) {
                            appDatabase.kitchenSettingsDao().add(it.data)

                        }
                    }

                }
                Status.ERROR -> {
                    _snackbarText.value = Event(resources.message)
                    _showProgress.value = Event(false)
                }
                Status.LOADING -> {
                    _showProgress.value = Event(true)

                }

            }
        }*/

    }
}

