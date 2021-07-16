package com.android.pos.ui.fragments.settings.tax

import androidx.lifecycle.ViewModel
import com.android.pos.data.repositories.PosRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TaxListViewModel @Inject constructor(
    private val posRepository: PosRepository
) : ViewModel() {

    val getTaxList = posRepository.getTaxList()
}