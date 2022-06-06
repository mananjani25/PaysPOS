package com.android.pos.ui.adapter.boldpos

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.entities.TaxData
import com.android.pos.data.entities.TbItem
import com.android.pos.databinding.LayoutTaxBifurcationBinding
import com.android.pos.databinding.ViewItemCartBinding
import com.android.pos.ui.adapter.CartItemModifierAdapter
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.extensions.strike
import com.google.gson.Gson

class TaxBirfurcationAdapter : RecyclerView.Adapter<TaxBirfurcationAdapter.MyViewHolder>() {
    var taxlist = ArrayList<TaxData>()

    inner class MyViewHolder(private val binding: LayoutTaxBifurcationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TaxData, pos: Int) {
            binding.txtName.text = item.name
            binding.txtValue.text = item.itemPricing
        }


    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TaxBirfurcationAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = LayoutTaxBifurcationBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaxBirfurcationAdapter.MyViewHolder, position: Int) {
        holder.bind(taxlist[position], position)
    }

    fun setList(list: ArrayList<TaxData>) {
        taxlist = list
        notifyDataSetChanged()

    }

    override fun getItemCount(): Int {
        return taxlist.size
    }
}