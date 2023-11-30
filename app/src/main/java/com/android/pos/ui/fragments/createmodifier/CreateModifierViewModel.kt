package com.android.pos.ui.fragments.createmodifier

import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.R
import com.android.pos.data.entities.Modifier
import com.android.pos.data.entities.TbItem
import com.android.pos.data.model.requestModel.CreateModifierRequest
import com.android.pos.data.model.requestModel.CreateModifierRequestModel
import com.android.pos.data.model.requestModel.ModifierSet
import com.android.pos.data.model.responseModel.CreateModifierSetResponse
import com.android.pos.data.remote.Constants
import com.android.pos.data.repositories.PosRepository
import com.android.pos.di.PrefProvider
import com.android.pos.utils.Event
import com.android.pos.utils.statusUtils.Resource
import com.android.pos.utils.statusUtils.Status
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateModifierViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val prefProvider: PrefProvider
) :
    ViewModel() {

    private var modifierSetId: Int? = null
    private var isEdit: Boolean = false
    private var itemIdsViewModel = ArrayList<Int>()
    var list = ArrayList<Modifier>()
    var deleteList = ArrayList<Modifier>()

    val modifierDetails = MutableLiveData(CreateModifierRequestModel())

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    private var _data = MutableLiveData<Event<String>>()
    val data: LiveData<Event<String>> = _data
    var isUpdated: Boolean = false

    fun setItemIds(itemIds: ArrayList<Int>) {
        this.itemIdsViewModel = itemIds
    }

    fun submit() {

        val data = modifierDetails.value

        if (TextUtils.isEmpty(data?.modifierName?.trim())) {
            _snackbarText.value = Event(R.string.modifier_name_validate)
        } else if (!validateModifierSetsName()) {
            _snackbarText.value = Event(R.string.modifier_set_name_validate)
        } else {
            _showProgress.value = Event(true)


            val modifierSets = CreateModifierRequest().apply {

                val modifierSets = ModifierSet().apply {
                    name = data?.modifierName!!.trim().replace("\\s+".toRegex(), " ")
                    locationId = prefProvider.getValueInt(Constants.LOCATION_ID, -1)
                    itemIds = itemIdsViewModel


                    modifiersAttributes = if (isEdit) {
                        list.addAll(deleteList)
                        list
                    } else {
                        list
                    }
                }
                modifierSet = modifierSets

            }

            viewModelScope.launch {

                val resource: Resource<CreateModifierSetResponse> = if (isEdit) {
                    posRepository.updateModifierSets(modifierSetId!!, modifierSets)
                } else {
                    posRepository.createModifierSet(modifierSets)
                }

                when (resource.status) {
                    Status.SUCCESS -> {
                        _showProgress.value = Event(false)

                        resource.data.let { modifierSetResponse ->
                            if (modifierSetResponse?.status == 200) {
                                resource.data?.let {

                                    isUpdated = true
                                    _data.value = Event(it.message)
                                    updateModifiersJSON(modifierSetId!!,modifierSets.modifierSet.modifiersAttributes)
                                    updateModifierDataInItem(it.data.modifierSet)
                                    posRepository.addModifierSets(it.data.modifierSet)
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

    private fun updateModifiersJSON(modId: Int, modifierList: List<Modifier>) {
        CoroutineScope(Dispatchers.IO).launch {
            posRepository.updateModifierJSON(modId, Gson().toJson(modifierList))
        }
    }

    private fun validateModifierSetsName(): Boolean {
        val similarItemsList = findAllDuplicatesNames(list)
        return similarItemsList.isEmpty()
    }

    private fun findAllDuplicatesNames(modifierList: ArrayList<Modifier>): Set<Modifier> {
        return modifierList.filter { item -> modifierList.count { (it.name == item.name) } > 1 }
            .toSet()
    }

    private fun findAllDuplicatesPrices(modifierList: ArrayList<Modifier>): Set<Modifier> {
        return modifierList.filter { item -> modifierList.count { (it.price == item.price) } > 1 }
            .toSet()
    }

    fun setModifiers(modifierList: ArrayList<Modifier>) {
        modifierList.forEach {
            it.name = it.name.trim().replace("\\s+".toRegex(), " ")
        }
        this.list = modifierList
    }

    fun setData(edit: Boolean, name: String, id: Int?) {
        isEdit = edit
        modifierDetails.value?.modifierName = name
        modifierSetId = id
    }

    fun setDeleteModifiers(delete: ArrayList<Modifier>) {
        deleteList = delete
    }

    private fun updateModifierDataInItem(modifierSet: com.android.pos.data.entities.ModifierSet) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var tempModifierSetIds: ArrayList<Int> = arrayListOf()
                val oldModifierSet: com.android.pos.data.entities.ModifierSet? =
                    modifierSet.id?.let { posRepository.getSingleModifier(it) }
                val oldItemIds: List<Int>? = oldModifierSet?.itemIds
                // Remove modifier set ids from Items
                oldItemIds?.forEach {
                    if (!modifierSet.itemIds.contains(it)) {
                        val item: TbItem? = posRepository.getSingleItem(it)
                        if (item?.modifier_set_ids != null) {
                            tempModifierSetIds = item.modifier_set_ids as ArrayList<Int>
                            if (tempModifierSetIds.isNotEmpty()) {
                                tempModifierSetIds.remove(modifierSet.id)
                                posRepository.updateModifiersForItem(tempModifierSetIds, it)
                            }
                        }
                    }
                }

                // Add modifier set id to Items
                modifierSet.itemIds.forEach {
                    if (oldItemIds != null) {
                        if (!oldItemIds.contains(it)) {
                            val item: TbItem? = posRepository.getSingleItem(it)
                            tempModifierSetIds = (item?.modifier_set_ids as ArrayList<Int>?)!!
                            if (tempModifierSetIds.isNotEmpty()) {
                                if (!tempModifierSetIds.contains(element = modifierSet.id)) {
                                    tempModifierSetIds.add(modifierSet.id!!)
                                    posRepository.updateModifiersForItem(tempModifierSetIds, it)
                                }
                            } else {
                                tempModifierSetIds.add(modifierSet.id!!)
                                posRepository.updateModifiersForItem(tempModifierSetIds, it)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}