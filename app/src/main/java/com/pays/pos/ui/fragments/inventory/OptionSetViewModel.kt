package com.pays.pos.ui.fragments.inventory

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pays.pos.data.entities.OptionSet
import com.pays.pos.data.model.responseModel.BaseResponse
import com.pays.pos.data.repositories.PosRepository
import com.pays.pos.utils.Event
import com.pays.pos.utils.statusUtils.Resource
import com.pays.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OptionSetViewModel @Inject constructor(
    private val posRepository: PosRepository,
) : ViewModel() {

    fun optionSets(): LiveData<Resource<List<OptionSet>>> {
        return posRepository.getOptionSet()
    }

    fun optionSetLocalList(): LiveData<Resource<List<OptionSet>>> {
        return posRepository.getOptionListData()
    }


    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _data = MutableLiveData<Event<BaseResponse?>>()
    val data: LiveData<Event<BaseResponse?>> = _data

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress


    fun deleteOptionSet(id: Int) {
        _showProgress.value = Event(true)

        viewModelScope.launch {


            val resource = posRepository.deleteOptionSet(id)
            when (resource.status) {
                Status.SUCCESS -> {

                    _showProgress.value = Event(false)

                    resource.data.let {
                        if (it?.status == 200) {
                            resource.data?.let { baseResponse ->
                                _data.value = Event(baseResponse)

                                posRepository.deleteOptionSetDatabase(id)

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


    fun reOrderOption(catId: Int, oldPos: Int, newPos: Int) {
        Log.e("ReorderOption", "oldPos:  ${oldPos}  newPos:${newPos}")
        _showProgress.value = Event(true)

        viewModelScope.launch {

            val resource = posRepository.reOrderOptionSet(catId, oldPos, newPos)
            when (resource.status) {
                Status.SUCCESS -> {

                    _showProgress.value = Event(false)

                    resource.data.let {
                        if (it?.status == 200) {
                            resource.data?.let { baseResponse ->
                                _data.value = Event(baseResponse)
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


    fun reOrder(allModifierSet: ArrayList<OptionSet>) {

        if (allModifierSet.isNotEmpty()) {
            viewModelScope.launch {
                posRepository.updateOptionSort(allModifierSet)
            }
        }

    }
}