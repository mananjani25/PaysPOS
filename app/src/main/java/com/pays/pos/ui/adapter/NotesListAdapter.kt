package com.pays.pos.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.data.entities.TbItem
import com.pays.pos.data.model.responseModel.NoteResponse
import com.pays.pos.databinding.ViewNoteItemBinding
import com.pays.pos.ui.fragments.settings.notes.NoteListViewModel
import com.pays.pos.utils.callback.ItemCallback
import com.pays.pos.utils.extensions.setOnSingleClickListener
import java.util.*
import kotlin.collections.ArrayList

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
            itemBinding.imageCheck.visibility = View.GONE
        } else {
            itemBinding.imgCheckBox.visibility = View.VISIBLE
            itemBinding.layoutMenu.imgOrderMenu.visibility = View.VISIBLE
            itemBinding.imageCheck.visibility = View.VISIBLE

        }

        itemBinding.executePendingBindings()
    }

    override fun getItemCount(): Int {
        return noteList.size

    }

    fun getAll(): ArrayList<NoteResponse.Data> {
        return noteList
    }

    inner class MyViewHolder(val noteItemBinding: ViewNoteItemBinding) :
        RecyclerView.ViewHolder(noteItemBinding.root) {

        init {
            if (isAdd) {
                noteItemBinding.root.setOnClickListener {
                    mCallback?.onItemClickListener(it, bindingAdapterPosition)
                }
            } else {
                noteItemBinding.layoutMenu.imgOrderMenu.setOnSingleClickListener {
                    mCallback?.onItemClickListener(it, bindingAdapterPosition)
                }

            }

        }
    }

    fun onItemMove(fromPosition: Int?, toPosition: Int?): Boolean {
        fromPosition?.let {
            toPosition?.let {
                if (fromPosition < toPosition) {
                    for (i in fromPosition until toPosition) {
                        Collections.swap(noteList, i, i + 1)

                        val order1: Int = noteList[i].sort
                        val order2: Int = noteList[i + 1].sort
                        noteList[i].sort = order2
                        noteList[i + 1].sort = order1
                    }
                } else {
                    for (i in fromPosition downTo toPosition + 1) {
                        Collections.swap(noteList, i, i - 1)

                        val order1: Int = noteList[i].sort
                        val order2: Int = noteList[i - 1].sort
                        noteList[i].sort = (order2)
                        noteList[i - 1].sort = (order1)
                    }
                }
                notifyItemMoved(fromPosition, toPosition)
                return true
            }
        }
        return false
    }

}