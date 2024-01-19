package com.pays.pos.ui.fragments.inventory

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pays.pos.data.entities.TbCategory
import com.pays.pos.data.model.responseModel.BaseResponse
import com.pays.pos.data.repositories.PosRepository
import com.pays.pos.data.repositories.TaxServiceChargeRepository
import com.pays.pos.utils.Event
import com.pays.pos.utils.statusUtils.Resource
import com.pays.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val taxServiceChargeRepository: TaxServiceChargeRepository
) : ViewModel() {

    val categories = posRepository.getCategoryList()

    val categoriesAll = posRepository.getCategoryListAll()
    val unhideCategories = posRepository.unhideCategoryList()
    val taxList = taxServiceChargeRepository.getTaxList()

    val enableTaxes = taxServiceChargeRepository.enableTaxes()

    fun _getCategories(): LiveData<Resource<List<TbCategory>>> {
        return posRepository.getCategoryListIWCAll()
    }

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _data = MutableLiveData<Event<BaseResponse?>>()
    val data: LiveData<Event<BaseResponse?>> = _data

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    fun deleteCategory(catId: Int, isHide: Boolean, isHideCategoryScreen: Boolean) {
        _showProgress.value = Event(true)

        viewModelScope.launch {

            val resource = if (isHideCategoryScreen) {
                posRepository.hideCategoryCall(catId, isHide)
            } else if (isHide) {
                posRepository.hideCategoryCall(catId, !isHide)
            } else {
                posRepository.deleteCategoryCall(catId)
            }

            when (resource.status) {
                Status.SUCCESS -> {

                    _showProgress.value = Event(false)

                    resource.data.let {
                        if (it?.status == 200) {
                            resource.data?.let { baseResponse ->
                                _data.value = Event(baseResponse)
                                if (isHideCategoryScreen) {
                                    posRepository.hideCategory(catId, isHide)
                                } else if (isHide) {
                                    posRepository.hideCategory(catId, !isHide)
                                } else {
                                    posRepository.deleteCategory(catId)
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


    fun reOrderCategory(catId: Int, oldPos: Int, newPos: Int) {
        _showProgress.value = Event(true)

        viewModelScope.launch {

            val resource = posRepository.reOrderCategoryCall(catId, oldPos, newPos)
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


    fun reOrder(allCategories: ArrayList<TbCategory>) {

        if (allCategories.isNotEmpty()) {
            viewModelScope.launch {
               // posRepository.deleteAllCategories()
                posRepository.updateCategorySort(allCategories)
            }
        }

    }
}