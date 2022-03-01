package com.android.pos.ui.adapter.boldpos

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.model.CategoryTabModel
import com.android.pos.databinding.ViewBoldCategoryBinding
import com.android.pos.ui.adapter.CategoryTabAdapter1

class CategoryAdapter(
    val context: Context,
    var list: ArrayList<CategoryTabModel>,
    val listner: CategoryTabAdapter1.TabListner
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return MyViewHolder(
            ViewBoldCategoryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    inner class MyViewHolder(private val binding: ViewBoldCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

    }

    override fun getItemCount(): Int {
        return list.size

    }
}