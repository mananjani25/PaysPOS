package com.pays.pos.ui.adapter

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.TextAppearanceSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.MainApplication
import com.pays.pos.R
import com.pays.pos.data.entities.TaxData
import com.pays.pos.databinding.ViewTaxBinding


class TaxesAdapter(private val isChoose: Boolean) :
    RecyclerView.Adapter<TaxesAdapter.MyViewHolder>() {

    var taxList = ArrayList<TaxData>()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val binding = ViewTaxBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return taxList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        if (taxList.isNotEmpty()) {
            holder.bind(taxList[position])
        }
    }

    inner class MyViewHolder(private val binding: ViewTaxBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(taxData: TaxData) {

            //discount name and rate
            setupNameAndRate(taxData.name, taxData.showFormattedTaxRate())

            if (absoluteAdapterPosition == 0) {
                binding.firstViewTax.visibility = View.VISIBLE
            } else {
                binding.firstViewTax.visibility = View.GONE
            }
            //selection option
            if (isChoose) {
                if (taxData.isChecked == true) {
                    binding.imgCheck.setImageResource(R.drawable.ic_outline_radio_button_checked)
                } else {
                    binding.imgCheck.setImageResource(R.drawable.ic_uncheck_circle)
                }
            } else {
                binding.imgCheck.setImageResource(R.drawable.ic_arrow_forward)
            }
        }

        private fun setupNameAndRate(name: String?, rate: String?) {
            binding.txtTax.text = rate
            binding.txtTitle.text = name
        }

        init {
            binding.imgCheck.setOnClickListener {
                if (isChoose) {
                    val tax = taxList[bindingAdapterPosition]
                    tax.isChecked = !(tax.isChecked ?: false)
                    notifyItemChanged(bindingAdapterPosition)
                }
            }
        }
    }

    fun addList(taxList: List<TaxData>) {
        this.taxList.clear()
        this.taxList.addAll(taxList)
        notifyDataSetChanged()
    }
}