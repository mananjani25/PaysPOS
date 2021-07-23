package com.android.pos.ui.fragments.inventory

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.data.model.responseModel.CreateTaxResponse
import com.android.pos.data.repositories.PosRepository
import com.android.pos.utils.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val posRepository: PosRepository,
) : ViewModel() {

    val categories = posRepository.getCategoryList()

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _data = MutableLiveData<Event<CreateTaxResponse?>>()
    val data: LiveData<Event<CreateTaxResponse?>> = _data

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    fun deleteCategory(catId: Int) {
        _showProgress.value = Event(true)

        viewModelScope.launch {

            //api call then on success database call
            posRepository.deleteCategory(catId)

            /*when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)

                    resource.data.let {
                        if (it?.status == 200) {
                            resource.data?.let {createTaxResponse->
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
            }*/
        }
    }

}