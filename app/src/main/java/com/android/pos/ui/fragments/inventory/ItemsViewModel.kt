package com.android.pos.ui.fragments.inventory

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.data.db.AppDatabase
import com.android.pos.data.model.responseModel.BaseResponse
import com.android.pos.data.repositories.PosRepository
import com.android.pos.utils.Event
import com.android.pos.utils.statusUtils.Resource
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ItemsViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val appDatabase: AppDatabase,
) : ViewModel() {

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _data = MutableLiveData<Event<BaseResponse?>>()
    val data: LiveData<Event<BaseResponse?>> = _data

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress


    val items = posRepository.getItemsList()


    fun deleteAndHide(id: Int, deleteAndHide: Boolean) {
        _showProgress.value = Event(true)

        viewModelScope.launch {

            val data = HashMap<String, String>()
            data["id"] = id.toString()
            data["is_hide"] = 1.toString()
            val resource: Resource<BaseResponse> = if (deleteAndHide) {
                posRepository.itemHide(id, data)
            } else {
                posRepository.deleteItem(id)
            }


            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)

                    resource.data.let {
                        if (it?.status == 200) {
                            resource.data?.let { response ->
                                _data.value = Event(response)
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

    fun dbDeleteAndHide(itemId: Int, deleteAndHide: Boolean) {

        viewModelScope.launch {
            if (deleteAndHide) {
                appDatabase.itemDao().update(itemId)
            } else
                appDatabase.itemDao().deleteItem(itemId)
        }

    }

}