package com.pays.pos.ui.fragments.magtekPro

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pays.pos.data.db.AppDatabase
import com.pays.pos.data.entities.ActivePaymentGateway
import com.pays.pos.data.repositories.PosRepository
import com.pays.pos.di.PrefProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PaymentGatewayTypeViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val appDatabase: AppDatabase,
    private val prefProvider: PrefProvider
):ViewModel() {

    private var _activePaymentGatewayObservable = MutableLiveData<List<ActivePaymentGateway>>()
    public var activePaymentGatewayObservable:LiveData<List<ActivePaymentGateway>> = _activePaymentGatewayObservable

   fun getActivePaymentGatewayType() {
       viewModelScope.launch(Dispatchers.IO) {
           _activePaymentGatewayObservable.postValue(posRepository.getActivePaymentGateway())
       }
    }
}