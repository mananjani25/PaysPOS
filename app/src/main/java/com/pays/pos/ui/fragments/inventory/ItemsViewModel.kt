package com.pays.pos.ui.fragments.inventory

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.pays.pos.data.db.AppDatabase
import com.pays.pos.data.entities.TbItem
import com.pays.pos.data.model.responseModel.BaseResponse
import com.pays.pos.data.model.responseModel.InventoryCountsResponse
import com.pays.pos.data.repositories.PosRepository
import com.pays.pos.utils.Event
import com.pays.pos.utils.statusUtils.Resource
import com.pays.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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


    val items = posRepository.getWholeItemsWithManualFromPos()
    val hideItemsListPos = posRepository.unhideItemListPOS()
    val hideItemsListWebsite = posRepository.unhideItemListWebsite()

    fun getAllItemsList(callback: (List<TbItem?>) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch { callback(posRepository.getAllItemsList()) }
    }

    fun getItemsList(screenType: String): LiveData<Resource<List<TbItem?>>> {
        return if (screenType == "tax") {
            posRepository.getWholeItemsWithManualFromPos()
        } else {
            posRepository.getWholeItemFromPos()
        }
    }

    fun inventoryCounts(): LiveData<Resource<InventoryCountsResponse>> =
        posRepository.inventoryCounts()

    var itemCount = 50


    fun allItemsQuery(desc: String): Flow<PagingData<TbItem>> = Pager(
        config = PagingConfig(
            pageSize = 20,
            enablePlaceholders = false,
        )
    ) {
        appDatabase.itemDao().getItemSearchResults(desc)
    }.flow.cachedIn(viewModelScope)


    val allItems: Flow<PagingData<TbItem>> = Pager(
        config = PagingConfig(
            pageSize = 50,
            enablePlaceholders = false
        )
    ) {
        appDatabase.itemDao().getPaginationList()
    }.flow.cachedIn(viewModelScope)


    fun deleteItems(id: Int) {
        _showProgress.value = Event(true)

        viewModelScope.launch {
            val resource = posRepository.deleteItem(id)


            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)

                    resource.data.let {
                        if (it?.status == 200) {
                            resource.data?.let { response ->
                                _data.value = Event(response)
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

    fun hideItems(id: Int, hide_status: String, type: String) {
        _showProgress.value = Event(true)

        viewModelScope.launch {
            val resource = if (type == "website") {
                posRepository.hideItemWebsite(id, hide_status)
            } else {
                posRepository.itemHide(id, hide_status)
            }

            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)

                    resource.data.let {
                        if (it?.status == 200) {
                            resource.data?.let { response ->
                                _data.value = Event(response)
                                if (type=="website"){
                                    appDatabase.itemDao().updateItemWebsite(id,hide_status)
                                }else{
                                    appDatabase.itemDao().updateItemPos(id,hide_status)
                                }
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
    fun unHideItems(id: Int,  type: String) {
        _showProgress.value = Event(true)

        viewModelScope.launch {
            val resource = if (type == "website") {
                posRepository.hideItemWebsite(id, "UnHideOnWebsite")
            } else {
                posRepository.itemHide(id, "UnHide")
            }

            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)

                    resource.data.let {
                        if (it?.status == 200) {
                            resource.data?.let { response ->
                                _data.value = Event(response)
                                if (type=="website"){
                                    appDatabase.itemDao().updateItemWebsite(id,"UnHideOnWebsite")
                                }else{
                                    appDatabase.itemDao().updateItemPos(id,"UnHide")
                                }
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