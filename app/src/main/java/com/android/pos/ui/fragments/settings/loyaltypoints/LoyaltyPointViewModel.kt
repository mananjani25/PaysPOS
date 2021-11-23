package com.android.pos.ui.fragments.settings.loyaltypoints

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.R
import com.android.pos.data.entities.LoyaltyProgramsModel
import com.android.pos.data.model.requestModel.LoyaltyPointRequest
import com.android.pos.data.model.responseModel.BaseResponse
import com.android.pos.data.model.responseModel.CreateLoyaltyPointResponse
import com.android.pos.data.remote.Constants
import com.android.pos.data.repositories.TaxServiceChargeRepository
import com.android.pos.di.PrefProvider
import com.android.pos.utils.Event
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoyaltyPointViewModel @Inject constructor(
    private val taxServiceChargeRepository: TaxServiceChargeRepository,
    private val prefProvider: PrefProvider
) : ViewModel() {

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _data = MutableLiveData<Event<BaseResponse?>>()
    val data: LiveData<Event<BaseResponse?>> = _data

    private val _createLoyaltyPointData = MutableLiveData<Event<CreateLoyaltyPointResponse?>>()
    val createLoyaltyPointData: LiveData<Event<CreateLoyaltyPointResponse?>> =
        _createLoyaltyPointData

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    private val _notifydata = MutableLiveData<Event<Int?>>()
    val notifydata: LiveData<Event<Int?>> = _notifydata

    val loyaltyPoints = taxServiceChargeRepository.loyaltyPointList()

    var loyaltyId: Int? = null
    var loyaltyName: String? = null
    var loyaltyTarget: Double? = 0.0
    var loyaltyAmount: Double = 0.0
    var loyaltyPointType: String = ""
    var isEdit: Boolean = false

    fun isLoyaltyPointActive(serChargeItem: LoyaltyProgramsModel) {

        // _showProgress.value = Event(true)

        viewModelScope.launch {
            serChargeItem.isEnable = !serChargeItem.isEnable

            val resource =
                taxServiceChargeRepository.loyaltyPointActive(
                    serChargeItem.id,
                    serChargeItem.isEnable
                )

            when (resource.status) {
                Status.SUCCESS -> {

                    //   _showProgress.value = Event(false)

                    resource.data.let {
                        if (it?.status == 200) {
                            resource.data?.let { baseResponse ->

                                taxServiceChargeRepository.loyaltyProgramActiveDatabase(
                                    serChargeItem.id,
                                    serChargeItem.isEnable
                                )
                                _notifydata.value = Event(serChargeItem.id)

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
            val resource = taxServiceChargeRepository.deleteLoyaltyPoint(id)
            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)

                    resource.data.let {
                        if (it?.status == 200) {
                            resource.data?.let { createTaxResponse ->
                                taxServiceChargeRepository.deleteLoyaltyPointDatabase(id)
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

    fun submit() {
        when {
            loyaltyName.isNullOrEmpty() -> {
                _snackbarText.value = Event(R.string.error_loyalty_name_blank)
            }
            loyaltyTarget == null -> {
                _snackbarText.value = Event(R.string.error_loyalty_point_blank)
            }
            else -> {
                val request = LoyaltyPointRequest(
                    amount = loyaltyAmount,
                    id = if (isEdit) loyaltyId else null,
                    isEnable = false,
                    name = loyaltyName,
                    rewardPoint = loyaltyTarget,
                    rewardType = loyaltyPointType,
                    locationId = prefProvider.getValueInt(Constants.LOCATION_ID, -1)
                )
                viewModelScope.launch {
                    _showProgress.value = Event(true)
                    val resource =
                        if (isEdit) {
                            taxServiceChargeRepository.editLoyaltyPoint(request)
                        } else {
                            taxServiceChargeRepository.createLoyaltyPoint(
                                request
                            )
                        }
                    when (resource.status) {
                        Status.SUCCESS -> {
                            _showProgress.value = Event(false)
                            resource.data.let {
                                if (it?.status == 200) {
                                    _createLoyaltyPointData.postValue(Event(resource.data))

                                    taxServiceChargeRepository.addLoyaltyPointDatabase(it.data)
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

    fun setLoyaltyData(loyaltyProgramsModel: LoyaltyProgramsModel?) {
        isEdit = true
        loyaltyName = loyaltyProgramsModel?.name ?: ""
        loyaltyAmount = loyaltyProgramsModel?.amount ?: 0.0
        loyaltyTarget = loyaltyProgramsModel?.rewardPoint ?: 0.0
        loyaltyId = loyaltyProgramsModel?.id


    }
}