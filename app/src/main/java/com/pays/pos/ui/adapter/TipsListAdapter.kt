package com.pays.pos.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.data.model.responseModel.GetTipReponse
import com.pays.pos.data.model.responseModel.NoteResponse
import com.pays.pos.databinding.ViewTipItemBinding
import com.pays.pos.ui.fragments.settings.tip.TipListViewModel
import com.pays.pos.utils.callback.ItemCallback
import com.pays.pos.utils.extensions.setOnSingleClickListener
import java.util.*
import kotlin.collections.ArrayList

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

    fun getAll(): ArrayList<GetTipReponse.Data> {
        return tipList
    }

    fun onItemMove(fromPosition: Int?, toPosition: Int?): Boolean {
        fromPosition?.let {
            toPosition?.let {
                if (fromPosition < toPosition) {
                    for (i in fromPosition until toPosition) {
                        Collections.swap(tipList, i, i + 1)

                        val order1: Int = tipList[i].sort
                        val order2: Int = tipList[i + 1].sort
                        tipList[i].sort = order2
                        tipList[i + 1].sort = order1
                    }
                } else {
                    for (i in fromPosition downTo toPosition + 1) {
                        Collections.swap(tipList, i, i - 1)

                        val order1: Int = tipList[i].sort
                        val order2: Int = tipList[i - 1].sort
                        tipList[i].sort = (order2)
                        tipList[i - 1].sort = (order1)
                    }
                }
                notifyItemMoved(fromPosition, toPosition)
                return true
            }
        }
        return false
    }


    inner class MyViewHolder(val tipItemBinding: ViewTipItemBinding) :
        RecyclerView.ViewHolder(tipItemBinding.root){

        init {
            tipItemBinding.layoutMenu.imgOrderMenu.setOnSingleClickListener {
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