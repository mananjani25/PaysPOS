package com.android.pos.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.responseModel.NoteResponse
import com.android.pos.databinding.ViewNoteItemBinding
import com.android.pos.ui.fragments.settings.notes.NoteListViewModel
import com.android.pos.utils.callback.ItemCallback

class NotesListAdapter(val viewModel: NoteListViewModel, val isAdd: Boolean) :
    RecyclerView.Adapter<NotesListAdapter.MyViewHolder>() {

    var noteList = ArrayList<NoteResponse.Data>()

    private var mCallback: ItemCallback? = null
    fun setCallback(callback: ItemCallback) {
        mCallback = callback
    }

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
        itemBinding.viewModel = viewModel

        if (isAdd) {
            itemBinding.imgCheckBox.visibility = View.GONE
            itemBinding.layoutMenu.imgOrderMenu.visibility = View.GONE
        } else {
            itemBinding.imgCheckBox.visibility = View.VISIBLE
            itemBinding.layoutMenu.imgOrderMenu.visibility = View.VISIBLE

        }

        itemBinding.executePendingBindings()
    }

    override fun getItemCount(): Int {
        return noteList.size

    }

    inner class MyViewHolder(val noteItemBinding: ViewNoteItemBinding) :
        RecyclerView.ViewHolder(noteItemBinding.root) {

        init {
            if (isAdd){
                noteItemBinding.root.setOnClickListener {
                    mCallback?.onItemClickListener(it, bindingAdapterPosition)
                }
            }
            else{
                noteItemBinding.layoutMenu.imgOrderMenu.setOnClickListener {
                    mCallback?.onItemClickListener(it, bindingAdapterPosition)
                }

            }

        }
    }

}