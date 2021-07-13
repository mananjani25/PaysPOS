package com.android.pos.ui.fragments.team

import androidx.lifecycle.ViewModel
import com.android.pos.data.repositories.PosRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TeamListViewModel @Inject constructor(
    private val posRepository: PosRepository
) : ViewModel() {

    val employeeData = posRepository.employeesList()

}