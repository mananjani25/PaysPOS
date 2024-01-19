package com.pays.pos.ui.fragments.settings.notes

import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pays.pos.R
import com.pays.pos.data.model.requestModel.CreateNoteRequest
import com.pays.pos.data.model.responseModel.BaseResponse
import com.pays.pos.data.model.responseModel.CreateNoteResponse
import com.pays.pos.data.model.responseModel.GetTaxResponse
import com.pays.pos.data.model.responseModel.NoteResponse
import com.pays.pos.data.remote.Constants.LOCATION_ID
import com.pays.pos.data.repositories.PosRepository
import com.pays.pos.di.PrefProvider
import com.pays.pos.utils.Event
import com.pays.pos.utils.statusUtils.Resource
import com.pays.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class CreateNoteViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val prefProvider: PrefProvider
) : ViewModel() {

    val createNoteDetails = MutableLiveData(CreateNoteRequest())

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _data = MutableLiveData<Event<CreateNoteResponse?>>()
    val data: LiveData<Event<CreateNoteResponse?>> = _data

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    private var taxId: Int = -1
    private var isActive :Boolean  =false
    private var isEdit: Boolean = false

    private lateinit var noteData: CreateNoteRequest

    private lateinit var resource: Resource<CreateNoteResponse>

    fun isEditData(isEdit: Boolean, taxId: Int) {
        this.taxId = taxId
        this.isEdit = isEdit
    }

    fun setNoteData(noteData: NoteResponse.Data) {
        createNoteDetails.value?.note!!.name = noteData.name

    }

    fun submit() {
        val value = createNoteDetails.value
        if (TextUtils.isEmpty(value?.note!!.name.trim())) {
            _snackbarText.value = Event(R.string.note_name_validate)
        } else {
            _showProgress.value = Event(true)

            if (isEdit) {
                noteData = CreateNoteRequest().apply {
                    note.name = value.note.name.trim().replace("\\s+".toRegex(), " ")
                    note.isActive = true
                    note.locationId = prefProvider.getValueInt(LOCATION_ID, -1)
                }
            } else {
                noteData = CreateNoteRequest().apply {

                    note.name = value.note.name.trim().replace("\\s+".toRegex(), " ")
                    note.isActive = true
                    note.locationId = prefProvider.getValueInt(LOCATION_ID, -1)
                }
            }


            viewModelScope.launch {
                resource = if (isEdit) {
                    posRepository.updateNote(taxId, noteData)
                } else {
                    posRepository.createNote(noteData)
                }

                when (resource.status) {
                    Status.SUCCESS -> {
                        _showProgress.value = Event(false)
                        resource.data.let { logInResponse ->
                            if (logInResponse?.status == 200) {

                                resource.data?.let { noteResponse ->

                                    val note = NoteResponse.Data(
                                        createdAt = noteResponse.data.createdAt,
                                        id = noteResponse.data.id,
                                        isActive = noteResponse.data.isActive,
                                        locationId = noteResponse.data.locationId,
                                        name = noteResponse.data.name,
                                        sort = noteResponse.data.sort,
                                        updatedAt = noteResponse.data.updatedAt
                                    )
                                    posRepository.createNoteDatabase(note)
                                    _data.value = Event(noteResponse)

                                }
                            } else {
                                _snackbarText.value = Event(resource.message)
                            }
                        }
                    }

                    Status.ERROR -> {
                        _snackbarText.value = Event(resource.message)
                        _showProgress.value = Event(false)
                    }

                    Status.LOADING -> {
                        _showProgress.value = Event(true)
                    }
                }
            }

        }

    }


}