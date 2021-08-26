package com.android.pos.ui.fragments.orders

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.data.repositories.PosRepository
import com.android.pos.utils.Event
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActiveOrderViewModel @Inject constructor(
    private val posRepository: PosRepository
) : ViewModel() {

    val openOrders = posRepository.getOpenOrders()

}