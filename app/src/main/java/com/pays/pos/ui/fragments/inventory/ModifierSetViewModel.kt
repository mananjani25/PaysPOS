package com.pays.pos.ui.fragments.inventory

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pays.pos.data.entities.ModifierSet
import com.pays.pos.data.entities.TbItem
import com.pays.pos.data.model.responseModel.BaseResponse
import com.pays.pos.data.repositories.PosRepository
import com.pays.pos.utils.Event
import com.pays.pos.utils.statusUtils.Resource
import com.pays.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ModifierSetViewModel @Inject constructor(
    private val posRepository: PosRepository,
) : ViewModel() {

    val categories = posRepository.getCategoryList()
    val unhideCategories = posRepository.unhideCategoryList()

    fun modifierSets(): LiveData<Resource<List<ModifierSet>>> {
        return posRepository.modifierSetsList()
    }

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _data = MutableLiveData<Event<BaseResponse?>>()
    val data: LiveData<Event<BaseResponse?>> = _data

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    fun deleteDatabase(catId: Int) {
        viewModelScope.launch {
            posRepository.deleteModifierSet(catId)
        }
    }

    fun deleteModifierSet(id: Int) {
        _showProgress.value = Event(true)

        viewModelScope.launch {


            val resource = posRepository.deleteModifierSetCall(id)
            when (resource.status) {
                Status.SUCCESS -> {

                    _showProgress.value = Event(false)

                    resource.data.let {
                        if (it?.status == 200) {
                            resource.data?.let { baseResponse ->

                                posRepository.deleteModifierSet(id)
                                removeModifierIdFromItemsList(id)
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

    // Remove modifier set id from Item's modifier_set_ids list
    private suspend fun removeModifierIdFromItemsList(modifierId: Int) {
        val allItemsInDb: List<TbItem?>? = posRepository.fetchAllItemsList()
        if (allItemsInDb?.isNotEmpty() == true) {
            allItemsInDb.forEach { item ->
                if (item?.modifier_set_ids?.isNotEmpty() == true) {
                    val modifiersList = item.modifier_set_ids as ArrayList<Int>
                    if (modifiersList.contains(modifierId)) {
                        modifiersList.remove(modifierId)
                    }
                    posRepository.updateModifiersForItem(modifiersList, item.itemId)
                }
            }
        }
    }


    fun reOrderModifier(catId: Int, oldPos: Int, newPos: Int) {
        _showProgress.value = Event(true)

        viewModelScope.launch {

            val resource = posRepository.reOrderModifierCall(catId, oldPos, newPos)
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


    fun reOrder(allModifierSet: ArrayList<ModifierSet>) {

        if (allModifierSet.isNotEmpty()) {
            viewModelScope.launch {
                posRepository.updateModifierSort(allModifierSet)
            }
        }

    }
}