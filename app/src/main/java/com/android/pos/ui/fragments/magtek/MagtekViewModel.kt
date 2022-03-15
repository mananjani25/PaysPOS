package com.android.pos.ui.fragments.magtek

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.data.entities.TbCardReader
import com.android.pos.data.model.requestModel.OrderCancelRequest
import com.android.pos.data.model.responseModel.BaseResponse
import com.android.pos.data.model.responseModel.OpenOrderResponse
import com.android.pos.data.model.responseModel.PrinterResponse
import com.android.pos.data.repositories.PosRepository
import com.android.pos.utils.Event
import com.android.pos.utils.statusUtils.Resource
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MagtekViewModel @Inject constructor(
    private val posRepository: PosRepository
) : ViewModel() {

    fun cardReaderList() = posRepository.getCardReaderList()

    fun cardReaderById(id: String) = posRepository.getCardReaderList(id)


    fun addCardReader(tbCardReader: TbCardReader) {
        viewModelScope.launch {

            posRepository.addCardReader(tbCardReader)
        }

    }

    fun updateCardReader(tbCardReader: TbCardReader) {
        viewModelScope.launch {

            posRepository.updateCardReader(tbCardReader)
        }

    }

    fun deleteTable() {

        viewModelScope.launch {
            posRepository.deleteTable()
        }
    }

}