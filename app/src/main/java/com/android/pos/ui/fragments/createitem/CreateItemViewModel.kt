package com.android.pos.ui.fragments.createitem

import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.R
import com.android.pos.data.model.requestModel.CreateItemRequestModel
import com.android.pos.data.repositories.PosRepository
import com.android.pos.utils.Event
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateItemViewModel @Inject constructor(
    private val posRepository: PosRepository
) :
    ViewModel() {

    val itemDetails = MutableLiveData(CreateItemRequestModel())

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    fun submit() {

        if (TextUtils.isEmpty(itemDetails.value?.itemName?.trim())) {
            _snackbarText.value = Event(R.string.item_name_validate)
        } else if (TextUtils.isEmpty(
                itemDetails.value?.price?.toString()?.trim()
            )
            && itemDetails.value?.price == 0.0
        ) {
            _snackbarText.value = Event(R.string.item_price_validate)
        } else if (TextUtils.isEmpty(itemDetails.value?.sku?.trim())) {
            _snackbarText.value = Event(R.string.item_sku_validate)
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