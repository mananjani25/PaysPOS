package com.android.pos.ui.fragments.dashboard

import androidx.lifecycle.ViewModel
import com.android.pos.data.repositories.PosRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DashBoardCategoryViewModel @Inject constructor(
    private val posRepository: PosRepository
) : ViewModel() {
    val venueData = posRepository.syncVenueData()
}