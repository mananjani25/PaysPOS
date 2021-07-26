package com.android.pos.ui.fragments.createcategory

import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import com.android.pos.R
import com.android.pos.data.entities.TbCategory
import com.android.pos.data.entities.TbItem
import com.android.pos.data.model.requestModel.CreateCategoryRequestModel
import com.android.pos.data.repositories.PosRepository
import com.android.pos.utils.Event
import com.android.pos.utils.statusUtils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CreateCategoryViewModel @Inject constructor(
    private val posRepository: PosRepository
) :
    ViewModel() {

    val categoryDetails = MutableLiveData(CreateCategoryRequestModel())


    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    // private var catId: Int = -1
    private var isEdit: Boolean = false

    private val catId = MutableLiveData<Int>()

    private val _getInventory = catId.switchMap { id ->
        posRepository.getInventory(id)
    }
    val getInventory: LiveData<Resource<List<TbItem?>>> = _getInventory


    val items = posRepository.getItemsList()


    fun isEditData(isEdit: Boolean, catId: Int) {
        this.isEdit = isEdit
        this.catId.value = catId
    }


    fun categoryData(categoryData: TbCategory) {
        categoryDetails.value?.categoryName = categoryData.name
    }

    //  val getInventory = catId.value?.let { posRepository.getInventory(it) }

    fun submit() {

        if (TextUtils.isEmpty(categoryDetails.value?.categoryName?.trim())) {
            _snackbarText.value = Event(R.string.category_name_validate)
        } else {
            //_showProgress.value = Event(true)

            if (isEdit) {

            } else {

            }
            /*val data = HashMap<String, String>()

            viewModelScope.launch {
                val resource = userRepository.userLogIn(data)
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
            }*/

        }

    }
}