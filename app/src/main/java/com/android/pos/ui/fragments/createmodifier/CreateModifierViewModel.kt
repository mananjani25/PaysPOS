package com.android.pos.ui.fragments.createmodifier

import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.R
import com.android.pos.data.model.requestModel.CreateCategoryRequestModel
import com.android.pos.data.model.requestModel.CreateItemRequestModel
import com.android.pos.data.model.requestModel.CreateModifierRequestModel
import com.android.pos.data.repositories.PosRepository
import com.android.pos.utils.Event
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateModifierViewModel @Inject constructor(
    private val posRepository: PosRepository
) :
    ViewModel() {

    val modifierDetails = MutableLiveData(CreateModifierRequestModel())

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    fun submit() {

        if (TextUtils.isEmpty(modifierDetails.value?.modifierName?.trim())) {
            _snackbarText.value = Event(R.string.modifier_name_validate)
        } else {
            _showProgress.value = Event(true)

            val data = HashMap<String, String>()
            /* data["email"] = loginDetails.value?.emailAddress.toString()
             data["password"] = loginDetails.value?.password.toString()*/

            viewModelScope.launch {
                val resource = posRepository.userLogIn(data)
                when (resource.status) {
                    Status.SUCCESS -> {
                        _showProgress.value = Event(false)

                        resource.data.let {
                            if (it?.status == 200) {
                                resource.data?.let {

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