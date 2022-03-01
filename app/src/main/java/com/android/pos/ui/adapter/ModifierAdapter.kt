package com.android.pos.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.entities.Modifier
import com.android.pos.databinding.ViewModifiersRemoveBinding

import com.android.pos.utils.EditTextWatcher
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.PriceTextWatcher
import java.util.*
import kotlin.collections.ArrayList

class ModifierAdapter(private val isEdit: Boolean) :
    RecyclerView.Adapter<ModifierAdapter.MyViewHolder>() {
    var list = ArrayList<Modifier>()
    var deletedList = ArrayList<Modifier>()

    inner class MyViewHolder(private val binding: ViewModifiersRemoveBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Modifier) {
            binding.model = item
            binding.executePendingBindings()

            if(bindingAdapterPosition==0){
                binding.firstViewCreateModifier.visibility = View.VISIBLE
            }else{
                binding.firstViewCreateModifier.visibility = View.GONE
            }
            binding.edtName.addTextChangedListener(EditTextWatcher(binding.edtName, item))
            binding.llParent.requestFocus()
            binding.edtName.setText(item.name)
            binding.edtName.setSelection(binding.edtName.text!!.length)

            binding.edtPrice.addTextChangedListener(PriceTextWatcher(binding.edtPrice, item))
            MethodUtils.setPriceEditText(binding.edtPrice, item.price)
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
    ): ModifierAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewModifiersRemoveBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: ModifierAdapter.MyViewHolder, position: Int) {

        holder.bind(list.get(position))
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun add(modifierSet: Modifier) {
        list.add(modifierSet)
        notifyDataSetChanged()
    }

    fun getItem(pos: Int): Modifier {
        return list[pos]
    }


    fun getAll(): ArrayList<Modifier> {
        return list
    }

    fun getDelete(): ArrayList<Modifier> {
        return deletedList
    }


    fun addAll(modifiers: List<Modifier>) {
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