package com.pays.pos.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.data.model.HardwareModel
import com.pays.pos.databinding.ViewHardwareListBinding

class HardwareListAdapter(val context: Context, val list: ArrayList<HardwareModel>) :
    RecyclerView.Adapter<HardwareListAdapter.MyViewHolder>() {
    private var hardwareListner: HardwareListner? = null
    inner class MyViewHolder(private val binding: ViewHardwareListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(model: HardwareModel) {
            binding.model = model
            binding.executePendingBindings()
        }

        init {

            binding.root.setOnClickListener {
                hardwareListner?.onITemClicked(list[bindingAdapterPosition].title)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val inflater = LayoutInflater.from(context)
        val binding = ViewHardwareListBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    fun setListner(listner:HardwareListner){
        this.hardwareListner=listner
    }
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(list[position])

    }

    override fun getItemCount(): Int {
        return list.size

    }
    interface HardwareListner {
        fun onITemClicked(itemName: String)
    }
}