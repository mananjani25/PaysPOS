package com.android.pos.ui.fragments.team

import androidx.lifecycle.ViewModel
import com.android.pos.data.remote.Constants.LOCATION_ID
import com.android.pos.data.repositories.PosRepository
import com.android.pos.di.PrefProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CreateTeamViewModel @Inject constructor(
    posRepository: PosRepository,
    prefProvider: PrefProvider
) : ViewModel() {

    val locationId = prefProvider.getValueInt(LOCATION_ID, 0)

    val employeeData = posRepository.employeesList(locationId)

}