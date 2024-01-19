package com.pays.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.data.model.responseModel.NoteResponse
import com.pays.pos.databinding.ViewGuestTotalNumberBinding

class GuestListAdapter :
    RecyclerView.Adapter<GuestListAdapter.MyViewHolder>() {

    var noteList = ArrayList<Int>()
    private lateinit var listner:GuestListner

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): GuestListAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewGuestTotalNumberBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    fun addGuests(noteList: ArrayList<Int>) {
        this.noteList.apply {
            clear()
            addAll(noteList)
        }
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: GuestListAdapter.MyViewHolder, position: Int) {
        val itemBinding = holder.noteItemBinding
        itemBinding.tvGuestNumberCount.setText("" + noteList[position])

        itemBinding.executePendingBindings()
    }

    override fun getItemCount(): Int {
        return noteList.size

    }

    fun setListner(listner:GuestListner) {
        this.listner = listner

    }

    inner class MyViewHolder(val noteItemBinding: ViewGuestTotalNumberBinding) :
        RecyclerView.ViewHolder(noteItemBinding.root){

            init {

                itemView.setOnClickListener {
                    listner.onGuestSelected(noteList.get(layoutPosition))
                }
            }
        }


    interface GuestListner {
        fun onGuestSelected(numberOfGuest: Int)
    }
}