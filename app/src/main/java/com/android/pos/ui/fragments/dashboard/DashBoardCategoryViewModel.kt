package com.android.pos.ui.fragments.dashboard

import androidx.lifecycle.ViewModel
import com.android.pos.data.db.AppDatabase
import com.android.pos.data.repositories.PosRepository
import com.android.pos.di.DatabaseModule
import com.android.pos.utils.performGetOperation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DashBoardCategoryViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val appDatabase: AppDatabase,
) : ViewModel() {

    val venueData = posRepository.syncVenueData()


    val venueDataLocal = posRepository.venueDataLocal()

}