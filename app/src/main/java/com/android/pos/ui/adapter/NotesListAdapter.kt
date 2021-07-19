package com.android.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.responseModel.GetTipReponse
import com.android.pos.data.model.responseModel.NoteResponse
import com.android.pos.databinding.ViewNoteItemBinding

class NotesListAdapter() :
    RecyclerView.Adapter<NotesListAdapter.MyViewHolder>() {

    var noteList = ArrayList<NoteResponse.Data>()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): NotesListAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewNoteItemBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    fun addNotes(noteList: List<NoteResponse.Data>) {
        this.noteList.apply {
            clear()
            addAll(noteList)
        }
    }

    fun getItem(position: Int): NoteResponse.Data {
        return noteList[position]
    }

    override fun onBindViewHolder(holder: NotesListAdapter.MyViewHolder, position: Int) {
        val itemBinding = holder.noteItemBinding
        itemBinding.noteModel = noteList[position]

        itemBinding.executePendingBindings()
    }

    override fun getItemCount(): Int {
        return noteList.size

    }

    inner class MyViewHolder(val noteItemBinding: ViewNoteItemBinding) :
        RecyclerView.ViewHolder(noteItemBinding.root)

}