package com.pays.pos.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.data.entities.Modifier
import com.pays.pos.data.entities.Option
import com.pays.pos.databinding.ViewModifiersRemoveBinding
import com.pays.pos.databinding.ViewOptionRemoveBinding
import com.pays.pos.utils.EditTextWatcher
import com.pays.pos.utils.EditTextWatcherOption
import com.pays.pos.utils.MethodUtils
import com.pays.pos.utils.PriceTextWatcher
import java.util.*
import kotlin.collections.ArrayList

class OptionAdapter(private val isEdit: Boolean) :
    RecyclerView.Adapter<OptionAdapter.MyViewHolder>() {
    var list = ArrayList<Option>()
    var deletedList = ArrayList<Option>()

    inner class MyViewHolder(private val binding: ViewOptionRemoveBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Option) {
            binding.model = item
            binding.executePendingBindings()

            if(bindingAdapterPosition==0){
                binding.firstViewOoption.visibility= View.VISIBLE
            }else{
                binding.firstViewOoption.visibility= View.GONE
            }
            binding.edtName.addTextChangedListener(EditTextWatcherOption(binding.edtName, item))
            binding.llParent.requestFocus()
            binding.edtName.setText(item.name)
            binding.edtName.setSelection(binding.edtName.text!!.length)

        }

        init {


            binding.imgRemove.setOnClickListener {

                if (isEdit && list[bindingAdapterPosition].id != null) {
                    list[bindingAdapterPosition]._destroy = true
                    deletedList.add(list[bindingAdapterPosition])
                }
                list.removeAt(bindingAdapterPosition)
                notifyItemRemoved(bindingAdapterPosition)


            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): OptionAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewOptionRemoveBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: OptionAdapter.MyViewHolder, position: Int) {

        holder.bind(list.get(position))
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun add(modifierSet: Option) {
        list.add(modifierSet)
        notifyDataSetChanged()
    }

    fun getItem(pos: Int): Option {
        return list[pos]
    }


    fun getAll(): ArrayList<Option> {
        return list
    }

    fun getDelete(): ArrayList<Option> {
        return deletedList
    }


    fun addAll(modifiers: List<Option>) {
        list.addAll(modifiers)
        notifyDataSetChanged()
    }

    fun onItemMove(fromPosition: Int?, toPosition: Int?): Boolean {
        fromPosition?.let {
            toPosition?.let {
                if (fromPosition < toPosition) {
                    for (i in fromPosition until toPosition) {
                        Collections.swap(list, i, i + 1)
                    }
                } else {
                    for (i in fromPosition downTo toPosition + 1) {
                        Collections.swap(list, i, i - 1)
                    }
                }
                notifyItemMoved(fromPosition, toPosition)
                return true
            }
        }
        return false
    }

}