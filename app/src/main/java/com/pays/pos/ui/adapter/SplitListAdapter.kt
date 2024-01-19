package com.pays.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.MainApplication
import com.pays.pos.R
import com.pays.pos.data.model.SplitDetailListModel
import com.pays.pos.databinding.ViewSplitListBinding
import com.pays.pos.utils.MethodUtils
import com.pays.pos.utils.MethodUtils.Companion.toPrecision

class SplitListAdapter() : RecyclerView.Adapter<SplitListAdapter.MyViewHolder>() {
    private var list: ArrayList<SplitDetailListModel> = arrayListOf()

    fun setList(list: ArrayList<SplitDetailListModel>) {
        this.list = list
        notifyDataSetChanged()
    }

    inner class MyViewHolder(private val binding: ViewSplitListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(model: SplitDetailListModel) {
            binding.model = model
            binding.executePendingBindings()
            binding.txtSplitAmount.text =""+
                MainApplication.getInstance()!!.getText(R.string.symbole) + MethodUtils.roundOffAmountString(list.get(bindingAdapterPosition).amount)
            binding.txtRemainingAmount.text =
                ""+MainApplication.getInstance()!!.getText(R.string.symbole) + MethodUtils.roundOffAmountString(list.get(bindingAdapterPosition).remainingAmt)
            binding.txtTitle.text =
                list[bindingAdapterPosition].title + " " + (bindingAdapterPosition + 1)

            binding.txtRemainingAmountLabel.text = "Remaining Amount "
            binding.txtSplitAmountLabel.text = "Amount "


        }

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SplitListAdapter.MyViewHolder {
        val binding =
            ViewSplitListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SplitListAdapter.MyViewHolder, position: Int) {
        holder.bind(list[position])

    }

    override fun getItemCount(): Int {
        return list.size

    }
}