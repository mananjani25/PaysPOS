package com.android.pos.ui.fragments.settings.tip

import androidx.lifecycle.ViewModel
import com.android.pos.data.repositories.PosRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TipListViewModel @Inject constructor(
    private val posRepository: PosRepository
) : ViewModel() {

    val getTipList = posRepository.getTipList()
}