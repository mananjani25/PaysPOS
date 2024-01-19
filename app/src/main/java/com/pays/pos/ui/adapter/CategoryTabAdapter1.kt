package com.pays.pos.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.data.model.CategoryTabModel
import com.pays.pos.data.remote.Constants.VERTICAL
import com.pays.pos.databinding.ViewDashboardTabItemBinding
import com.pays.pos.databinding.ViewTabVerticalBinding

class CategoryTabAdapter1(
    val context: Context,
    var list: ArrayList<CategoryTabModel>,
    val listner: TabListner
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    val TAG = "CategoryTabAdapter1"
    var notSelected: Boolean = true


    inner class MyTabVerticalHolder(private val binding: ViewTabVerticalBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CategoryTabModel) {
            binding.model = item
            binding.executePendingBindings()
            if (layoutPosition == 0) {
                binding.viewTop.visibility = View.VISIBLE
            } else {
                binding.viewTop.visibility = View.GONE
            }

        }

        init {
            binding.root.setOnClickListener {
                listner.onTabSelected(layoutPosition)
                for (i in 0 until list.size) {

                    list.get(i).isSelected = i == layoutPosition
                    if (i == layoutPosition) {
                        notSelected = false

                    }

                }


                notifyDataSetChanged()
            }


        }

    }

    inner class MyViewHolder(private val binding: ViewDashboardTabItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CategoryTabModel) {
            binding.model = item
            binding.executePendingBindings()
        }

        init {
            binding.root.setOnClickListener {
                listner.onTabSelected(layoutPosition)
                for (i in 0 until list.size) {
                    list.get(i).isSelected = i == layoutPosition
                }

                notifyDataSetChanged()
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {

        if (viewType == 0) {

            return MyTabVerticalHolder(
                ViewTabVerticalBinding.inflate(
                    LayoutInflater.from(context),
                    parent,
                    false
                )
            )

        } else {

            return MyViewHolder(
                ViewDashboardTabItemBinding.inflate(
                    LayoutInflater.from(context),
                    parent,
                    false
                )
            )
        }


    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == 0) {
            (holder as MyTabVerticalHolder).bind(list[position])
        } else {
            (holder as MyViewHolder).bind(list[position])
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    interface TabListner {
        fun onTabSelected(pos: Int)
    }

    override fun getItemViewType(position: Int): Int {
        return if (list.get(position).type == VERTICAL) {
            0
        } else {
            1
        }

    }

    fun addAll(categoryList: List<CategoryTabModel>) {
        list = categoryList as ArrayList<CategoryTabModel>
        notifyDataSetChanged()
    }

}