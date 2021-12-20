package com.android.pos.ui.fragments.settings.hardware.printerqueue

import androidx.lifecycle.ViewModel
import com.android.pos.data.db.AppDatabase
import com.android.pos.data.repositories.PosRepository
import com.android.pos.di.PrefProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PrinterQueueViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val appDatabase: AppDatabase,
    private val prefProvider: PrefProvider,
):ViewModel() {
    private val TAG = "PrinterQueueViewModel"

}