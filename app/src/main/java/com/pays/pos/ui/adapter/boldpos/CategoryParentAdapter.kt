package com.pays.pos.ui.adapter.boldpos

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.data.model.CategoryParentModel
import com.pays.pos.databinding.ViewParentCategoryBinding
import com.pays.pos.ui.adapter.CategoryTabAdapter1
import com.pays.pos.utils.LogUtil

class CategoryParentAdapter(
    val context: Context,
    var list: ArrayList<CategoryParentModel>,
    val listner: CategoryParentListner
) : RecyclerView.Adapter<CategoryParentAdapter.MyViewHolder>() {
    private val TAG = "CategoryParentAdapter"
    private var pos = 0
    var selectedParentPos: Int = 0
    var selectedCategoryPos: Int = 0

    inner class MyViewHolder(private val binding: ViewParentCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(model: CategoryParentModel) {
            LogUtil.logE("CartegoryParent", "bindingAdapterPosition  $bindingAdapterPosition")

            binding.rvCategory.adapter =
                CategoryAdapter(
                    binding.root.context,
                    model.list,
                    object : CategoryTabAdapter1.TabListner {
                        override fun onTabSelected(pos: Int) {
                            LogUtil.logE(TAG, "getCatPOS  ${pos}")
                            listner.onCategorySelected(bindingAdapterPosition, pos)
                            selectedParentPos = bindingAdapterPosition
                            selectedCategoryPos = pos
                        }

                    })
            binding.rvCategory.isNestedScrollingEnabled = false


        }


    }


    fun addList(tmpList: List<CategoryParentModel>) {
        LogUtil.logE(TAG,"tmpListtmpList:  ${tmpList.size}")
        list.clear()
        list = arrayListOf()
        list.addAll(tmpList)
        notifyDataSetChanged()
    }

    fun getList(): List<CategoryParentModel> {
        return list
    }

    fun clearList(){
        list.clear()
        list = arrayListOf()
        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CategoryParentAdapter.MyViewHolder {
        return MyViewHolder(
            ViewParentCategoryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: CategoryParentAdapter.MyViewHolder, position: Int) {
        holder.bind(list.get(position))

    }

    override fun getItemCount(): Int {
        return list.size
    }

    /*  override fun onTabSelected(pos: Int) {
          LogUtil.logE(TAG, "onTabSelected  ${pos}")
          listner.onCategorySelected()

      }*/

    interface CategoryParentListner {
        fun onCategorySelected(parentPosition: Int, childPosition: Int)
        fun onPositionChanged(position: Int)
    }

}