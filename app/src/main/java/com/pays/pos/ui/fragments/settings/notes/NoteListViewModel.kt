package com.pays.pos.ui.fragments.settings.notes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pays.pos.data.db.AppDatabase
import com.pays.pos.data.entities.TbItem
import com.pays.pos.data.model.responseModel.BaseResponse
import com.pays.pos.data.model.responseModel.CreateNoteResponse
import com.pays.pos.data.model.responseModel.NoteResponse
import com.pays.pos.data.repositories.PosRepository
import com.pays.pos.utils.Event
import com.pays.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.ArrayList

@HiltViewModel
class NoteListViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val appDatabase: AppDatabase
) : ViewModel() {

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _data = MutableLiveData<Event<CreateNoteResponse?>>()
    val data: LiveData<Event<CreateNoteResponse?>> = _data

    private val _data1 = MutableLiveData<Event<BaseResponse?>>()
    val data1: LiveData<Event<BaseResponse?>> = _data1

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    private val _notifydata = MutableLiveData<Event<Boolean?>>()
    val notifydata: LiveData<Event<Boolean?>> = _notifydata

    val getTaxList = posRepository.getNoteList()
    val taxListActive = posRepository.taxListActive()

    fun isNoteActive(noteDataItem: NoteResponse.Data) {

        // _showProgress.value = Event(true)

        viewModelScope.launch {
            noteDataItem.isActive = !noteDataItem.isActive

            val resource =
                posRepository.noteActive(noteDataItem.id, noteDataItem.isActive)

            when (resource.status) {
                Status.SUCCESS -> {

                    //   _showProgress.value = Event(false)

                    resource.data.let {
                        if (it?.status == 200) {
                            resource.data?.let { baseResponse ->
                                posRepository.noteActiveDatabase(
                                    noteDataItem.id,
                                    noteDataItem.isActive
                                )
                                _notifydata.value = Event(true)

                            }
                        } else {
                            _snackbarText.value = Event(resource.message)
                        }

                    }

                }

                Status.ERROR -> {
                    _snackbarText.value = Event(resource.message)
                    //_showProgress.value = Event(false)
                }

                Status.LOADING -> {
                    // _showProgress.value = Event(true)
                }
            }
        }


    }

    fun delete(id: Int) {
        _showProgress.value = Event(true)

        /*val data = HashMap<String, String>()
        data["id"] = id.toString()*/

        viewModelScope.launch {
            val resource = posRepository.deleteNote(id)
            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)

                    resource.data.let {
                        if (it?.status == 200) {
                            resource.data?.let { createTaxResponse ->
                                posRepository.deleteNoteDatabase(id)
                                _data.value = Event(createTaxResponse)
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

    fun reOrderItem(itemId: Int, oldPos: Int, newPos: Int) {
        _showProgress.value = Event(true)

        viewModelScope.launch {

            val resource = posRepository.reOrderNote(itemId, oldPos, newPos)
            when (resource.status) {
                Status.SUCCESS -> {

                    _showProgress.value = Event(false)

                    resource.data.let {
                        if (it?.status == 200) {
                            resource.data?.let { baseResponse ->
                                _data1.value = Event(baseResponse)
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

    fun reOrder(allItems: ArrayList<NoteResponse.Data>) {

        if (allItems.isNotEmpty()) {
            viewModelScope.launch {
                appDatabase.notesDao().addAllNotes(allItems)
            }
        }
    }
}