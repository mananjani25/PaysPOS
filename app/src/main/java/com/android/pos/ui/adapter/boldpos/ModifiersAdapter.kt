package com.android.pos.ui.adapter.boldpos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.entities.Modifier
import com.android.pos.data.entities.ModifierSet
import com.android.pos.databinding.ViewModifiersBoldBinding
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel

class ModifiersAdapter(
    val viewModel: DashBoardCategoryViewModel,
    private val _itemId: Int,
    private val viewLifecycleOwner: LifecycleOwner
) : RecyclerView.Adapter<ModifiersAdapter.MyViewHolder>() {
    var filterList = ArrayList<ModifierSet>()
    var selectedModifierList = ArrayList<Modifier>()



    inner class MyViewHolder(private var binding: ViewModifiersBoldBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ModifierSet) {


        }


    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ModifiersAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewModifiersBoldBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: ModifiersAdapter.MyViewHolder, position: Int) {
        holder.bind(filterList[holder.bindingAdapterPosition])

    }

    override fun getItemCount(): Int {
        return filterList.size

    }


}