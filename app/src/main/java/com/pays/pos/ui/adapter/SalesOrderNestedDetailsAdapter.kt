package com.pays.pos.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.data.model.responseModel.report.KeyValueWithString
import com.pays.pos.databinding.ViewSalesOrdersNestedBinding
import com.pays.pos.utils.extensions.gone
import com.pays.pos.utils.extensions.visible

class SalesOrderNestedDetailsAdapter :
    RecyclerView.Adapter<SalesOrderNestedDetailsAdapter.MyViewHolder>() {

    private var arrayList = ArrayList<KeyValueWithString>()

    inner class MyViewHolder(private val binding: ViewSalesOrdersNestedBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(keyValueList: KeyValueWithString) {
            binding.keyValue = keyValueList

            if (arrayList.size < 2) {
                binding.txtKey.visible()
            } else binding.txtKey.gone()
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewSalesOrdersNestedBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(arrayList[position])
    }

    override fun getItemCount(): Int {
        return arrayList.size
    }

    fun add(arrayList: ArrayList<KeyValueWithString>) {
        this.arrayList.clear()
        this.arrayList = arrayList

        notifyDataSetChanged()
    }
}