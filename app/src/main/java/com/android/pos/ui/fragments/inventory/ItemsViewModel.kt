package com.android.pos.ui.fragments.inventory

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagedList
import com.android.pos.data.db.AppDatabase
import com.android.pos.data.entities.TbItem
import com.android.pos.data.model.responseModel.BaseResponse
import com.android.pos.data.repositories.PosRepository
import com.android.pos.utils.Event
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
    val showItemsList = posRepository.unhideItemList()

    var itemCount = 50

    fun _getItems(): LiveData<PagedList<TbItem>> {
        Log.e("passedItemitemCount","passedItemitemCount  ${itemCount}")
        return posRepository.getPaginationList(itemCount)
    }


    fun deleteAndHide(id: Int, deleteAndHide: Boolean, isHideItemScreen: Boolean) {
        _showProgress.value = Event(true)

        viewModelScope.launch {
            val resource =
                if (isHideItemScreen) {
                    posRepository.itemHide(id, deleteAndHide)
                } else if (deleteAndHide) {
                    posRepository.itemHide(id, !deleteAndHide)
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
                                if (isHideItemScreen) {
                                    appDatabase.itemDao().updateShowItem(id)
                                } else if (deleteAndHide) {
                                    appDatabase.itemDao().update(id)
                                } else
                                    appDatabase.itemDao().deleteItem(id)
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


    fun reOrderItem(itemId: Int, oldPos: Int, newPos: Int) {
        _showProgress.value = Event(true)

        viewModelScope.launch {

            val resource = posRepository.reOrderItemCall(itemId, oldPos, newPos)
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

    fun reOrder(allItems: ArrayList<TbItem>) {

        if (allItems.isNotEmpty()) {
            viewModelScope.launch {
                appDatabase.itemDao().addAllItem(allItems)
            }
        }
    }

}