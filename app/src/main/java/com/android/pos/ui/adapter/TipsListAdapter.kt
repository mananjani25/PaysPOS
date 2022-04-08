package com.android.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.responseModel.GetTipReponse
import com.android.pos.databinding.ViewTipItemBinding
import com.android.pos.ui.fragments.settings.tip.TipListViewModel
import com.android.pos.utils.callback.ItemCallback

class TipsListAdapter(val viewModel: TipListViewModel) : RecyclerView.Adapter<TipsListAdapter.MyViewHolder>() {

    private val tipList = ArrayList<GetTipReponse.Data>()
    private var mCallback: ItemCallback? = null
    fun setCallback(callback: ItemCallback) {
        mCallback = callback
    }
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TipsListAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewTipItemBinding.inflate(inflater, parent, false)

        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TipsListAdapter.MyViewHolder, position: Int) {
        val itemBinding = holder.tipItemBinding
        itemBinding.tipModel = tipList[position]
        itemBinding.viewModel = viewModel

        itemBinding.executePendingBindings()
    }

    override fun getItemCount() = tipList.size

    fun addTips(tipList: List<GetTipReponse.Data>) {

        this.tipList.apply {
            clear()
            addAll(tipList)
        }
    }

    fun getItem(position:Int): GetTipReponse.Data {
        return tipList[position]
    }

    inner class MyViewHolder(val tipItemBinding: ViewTipItemBinding) :
        RecyclerView.ViewHolder(tipItemBinding.root){

        init {
            tipItemBinding.layoutMenu.imgOrderMenu.setOnClickListener {
                mCallback?.onItemClickListener(it, position)
            }
        }

        /* init {

             tipItemBinding.imgCheckBox.setOnClickListener {
                 tipList.get(layoutPosition).isChecked =  !tipList.get(layoutPosition).isChecked
                 notifyDataSetChanged()

             }

         }*/

    }}