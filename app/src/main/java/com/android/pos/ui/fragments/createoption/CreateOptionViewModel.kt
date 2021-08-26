package com.android.pos.ui.fragments.createoption

import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.R
import com.android.pos.data.entities.Option
import com.android.pos.data.model.requestModel.CreateOptionRequestModel
import com.android.pos.data.model.responseModel.BaseResponse
import com.android.pos.data.model.responseModel.GetOptionSetResponse
import com.android.pos.data.remote.Constants
import com.android.pos.data.repositories.PosRepository
import com.android.pos.di.PrefProvider
import com.android.pos.utils.Event
import com.android.pos.utils.statusUtils.Resource
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class CreateOptionViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val prefProvider: PrefProvider
) :
    ViewModel() {

    private var optionSetId: Int? = null
    private var isEdit: Boolean = false
    var list = ArrayList<Option>()
    var deleteList = ArrayList<Option>()

    val optionDetails = MutableLiveData(CreateOptionRequestModel())

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    private var _data = MutableLiveData<Event<GetOptionSetResponse>>()
    val data: LiveData<Event<GetOptionSetResponse>> = _data


    fun submit() {

        val data = optionDetails.value

        if (TextUtils.isEmpty(data?.name?.trim())) {
            _snackbarText.value = Event(R.string.option_name_validate)
        } else {
            _showProgress.value = Event(true)


            val optionSets = CreateOptionRequestModel().apply {

                name = data!!.name
                displayName = data!!.displayName
                locationId = prefProvider.getValueInt(Constants.LOCATION_ID, -1)
                optionsAttributes = if (isEdit) {
                    list.addAll(deleteList)
                    list
                } else {
                    list
                }
            }

            viewModelScope.launch {

                val resource: Resource<GetOptionSetResponse>
                if (isEdit) {
                    resource = posRepository.updateOptionSet(optionSetId!!, optionSets)
                } else {
                    resource = posRepository.createOptionSet(optionSets)
                }

                when (resource.status) {
                    Status.SUCCESS -> {
                        _showProgress.value = Event(false)

                        resource.data.let { modifierSetResponse ->
                            if (modifierSetResponse?.status == 200) {
                                resource.data?.let {

                                    _data.value = Event(it)
                                    posRepository.addOptionSetsDatabase(it.data)
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

    fun setModifiers(modifierList: ArrayList<Option>) {
        this.list = modifierList
    }

    fun setData(edit: Boolean, name: String, id: Int?) {
        isEdit = edit
        optionDetails.value?.name = name
        optionSetId = id
    }

    fun setDeleteModifiers(delete: ArrayList<Option>) {
        deleteList = delete
    }


}