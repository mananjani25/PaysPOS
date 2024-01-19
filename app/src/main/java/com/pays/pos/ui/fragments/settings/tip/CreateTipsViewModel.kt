package com.pays.pos.ui.fragments.settings.tip

import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pays.pos.R
import com.pays.pos.data.model.requestModel.CreateTipRequestModel
import com.pays.pos.data.model.responseModel.CreateTipResponse
import com.pays.pos.data.model.responseModel.GetTipReponse
import com.pays.pos.data.remote.Constants.LOCATION_ID
import com.pays.pos.data.repositories.PosRepository
import com.pays.pos.data.repositories.TipDiscountRepository
import com.pays.pos.di.PrefProvider
import com.pays.pos.utils.Event
import com.pays.pos.utils.statusUtils.Resource
import com.pays.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class CreateTipsViewModel @Inject constructor(
    private val tipDiscountRepository: TipDiscountRepository,
    private val prefProvider: PrefProvider
) : ViewModel() {

    private var _isActive: Boolean = false
    val createTipDetails = MutableLiveData(CreateTipRequestModel())

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _data = MutableLiveData<Event<CreateTipResponse?>>()
    val data: LiveData<Event<CreateTipResponse?>> = _data

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    private var tipId: Int = -1

    private var isEdit: Boolean = false

    private lateinit var tipData: CreateTipRequestModel

    private lateinit var resource: Resource<CreateTipResponse>


    fun setTipData(tipData: GetTipReponse.Data) {
        createTipDetails.value?.name = tipData.name
        createTipDetails.value?.rate = tipData.rate

        _isActive = tipData.isActive
    }

    fun isEditData(isEdit: Boolean, tipId: Int) {
        this.tipId = tipId
        this.isEdit = isEdit
    }

    fun submit(rate_double: Double) {
        val value = createTipDetails.value
        if (TextUtils.isEmpty(value?.name?.trim())) {
            _snackbarText.value = Event(R.string.tip_name_validate)
        } else if (rate_double == 0.0) {
            _snackbarText.value = Event(R.string.tip_rate_validate)
        } else {
            _showProgress.value = Event(true)

            if (isEdit) {
                tipData = CreateTipRequestModel().apply {
                    id = tipId
                    name = value!!.name.trim().replace("\\s+".toRegex(), " ")
                    rate = rate_double
                    isActive = _isActive
                    locationId = prefProvider.getValueInt(LOCATION_ID, -1)
                }
            } else {
                tipData = CreateTipRequestModel().apply {
                    name = value!!.name.trim().replace("\\s+".toRegex(), " ")
                    rate = rate_double
                    isActive = true
                    locationId = prefProvider.getValueInt(LOCATION_ID, -1)
                }
            }


            viewModelScope.launch {
                resource = if (isEdit) {
                    tipDiscountRepository.updateTip(tipId, tipData)
                } else {
                    tipDiscountRepository.createTips(tipData)
                }

                when (resource.status) {
                    Status.SUCCESS -> {
                        _showProgress.value = Event(false)
                        resource.data.let { logInResponse ->
                            if (logInResponse?.status == 200) {

                                resource.data?.let { createTipResponse ->

                                    val tip = GetTipReponse.Data(
                                        name = createTipResponse.data.name,
                                        id = createTipResponse.data.id,
                                        locationId = createTipResponse.data.locationId,
                                        isActive = createTipResponse.data.isActive,
                                        rate = createTipResponse.data.rate,
                                        createdAt = createTipResponse.data.createdAt,
                                        updatedAt = createTipResponse.data.updatedAt,
                                        sort = createTipResponse.data.sort
                                    )
                                    tipDiscountRepository.createTipsDatabase(tip)
                                    _data.value = Event(createTipResponse)

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