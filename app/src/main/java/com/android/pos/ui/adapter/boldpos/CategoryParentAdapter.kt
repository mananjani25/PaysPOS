package com.android.pos.ui.adapter.boldpos

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.CategoryParentModel
import com.android.pos.databinding.ViewParentCategoryBinding
import com.android.pos.ui.adapter.CategoryTabAdapter1

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
            Log.e("CartegoryParent", "bindingAdapterPosition  $bindingAdapterPosition")

            binding.rvCategory.adapter =
                CategoryAdapter(
                    binding.root.context,
                    model.list,
                    object : CategoryTabAdapter1.TabListner {
                        override fun onTabSelected(pos: Int) {
                            Log.e(TAG, "getCatPOS  ${pos}")
                            selectedParentPos = bindingAdapterPosition
                            selectedCategoryPos = pos
                            listner.onCategorySelected(bindingAdapterPosition, pos)
                        }

                    })
            binding.rvCategory.isNestedScrollingEnabled = false


        }


    }


    fun addList(tmpList: List<CategoryParentModel>) {
        Log.e(TAG,"tmpListtmpList:  ${tmpList.size}")
        list.clear()
        list = arrayListOf()
        list.addAll(tmpList)
        notifyDataSetChanged()
    }

    fun getList(): List<CategoryParentModel> {
        return list
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
          Log.e(TAG, "onTabSelected  ${pos}")
          listner.onCategorySelected()

      }*/

    interface CategoryParentListner {
        fun onCategorySelected(parentPosition: Int, childPosition: Int)
        fun onPositionChanged(position: Int)
    }

}