package com.android.pos.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.navigation.fragment.NavHostFragment.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.entities.TbItem
import com.android.pos.data.model.DineInModel
import com.android.pos.data.remote.Constants
import com.android.pos.databinding.ViewDineInItemBinding
import com.android.pos.ui.activities.SwipeHelper
import com.android.pos.utils.callback.MyCallback
import com.android.pos.utils.extensions.alert
import com.google.gson.Gson

class DineInAdapter : RecyclerView.Adapter<DineInAdapter.MyViewHolder>(), MyCallback {
    private var list: ArrayList<DineInModel> = arrayListOf()
    private lateinit var listner: DineInCallback
    private lateinit var itemAdapter: CartAdapter
    private val TAG = "DineInAdapter"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DineInAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewDineInItemBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DineInAdapter.MyViewHolder, position: Int) {
        holder.bind(list.get(position))
    }

    override fun getItemCount(): Int {
        return list.size
    }

    inner class MyViewHolder(private val binding: ViewDineInItemBinding) :
        RecyclerView.ViewHolder(binding.root), MyCallback {

        fun bind(model: DineInModel) {
            binding.model = model
            binding.executePendingBindings()
            itemAdapter = CartAdapter()
            binding.rvCart.adapter = itemAdapter
            //swipeListener(binding.rvCart, layoutPosition, binding.root.context)

            itemAdapter.addCart(list.get(layoutPosition).items)
            Log.e(TAG, "Customer:  ${list.get(layoutPosition).customer}")
            if (list.get(layoutPosition).customer != null) {
                binding.txtTableName.setText(
                    list.get(layoutPosition).customer?.first_name + " " + list.get(
                        layoutPosition
                    ).customer?.last_name
                )
                binding.txtCrtNewCustomer.setText(binding.root.resources.getString(R.string.remove_customer))
            } else {
                binding.txtTableName.setText(list.get(layoutPosition).title)
                binding.txtCrtNewCustomer.setText(binding.root.resources.getString(R.string.assign_customer))
            }
            if (layoutPosition == list.get(0).selectedPosition) {
                binding.rvCart.visibility = View.VISIBLE
                binding.constraintHeader.setBackground(binding.root.context.getDrawable(R.drawable.background_dine_in_selected))
                binding.txtTableName.setTextColor(binding.root.context.resources.getColor(R.color.white))
                binding.imgOrderMenu.setColorFilter(binding.root.context.resources.getColor(R.color.white))

                if (layoutPosition == 0) {
                    binding.imgProfile.setColorFilter(binding.root.context.resources.getColor(R.color.white))
                }

            } else {
                binding.rvCart.visibility = View.VISIBLE
                binding.constraintHeader.setBackground(
                    binding.root.context.getDrawable(R.drawable.background_dine_in_unselected)
                )
                binding.txtTableName.setTextColor(binding.root.context.resources.getColor(R.color.txtColor))
                binding.imgOrderMenu.setColorFilter(binding.root.context.resources.getColor(R.color.txtColor))
                if (layoutPosition == 0) {
                    binding.imgProfile.setColorFilter(binding.root.context.resources.getColor(R.color.txtColor))
                }
            }


            if (layoutPosition == 0) {
                binding.imgProfile.setImageDrawable(binding.root.context.getDrawable(R.drawable.ic_simple_table))
                binding.imgOrderMenu.visibility = View.GONE

            } else {
                binding.imgProfile.setImageDrawable(binding.root.context.getDrawable(R.drawable.ic_group_person))
                binding.imgOrderMenu.visibility = View.VISIBLE
            }
            // list.get(0).headerPosition = layoutPosition

            itemAdapter.setCallback(this)
        }

        init {

            binding.txtCrtNewCustomer.setOnClickListener {
                Log.e(TAG, "DineInlayoutPosition:  $layoutPosition")
                if (list.get(layoutPosition).customer != null) {
                    //list.get(layoutPosition).customer = null
                    listner.onCustomerClicked(layoutPosition, true)
                    binding.llCustomerDialog.visibility = View.GONE

                } else {
                    listner.onCustomerClicked(layoutPosition, false)
                }

            }

            binding.imgOrderMenu.setOnClickListener {
                if (binding.llCustomerDialog.visibility == View.VISIBLE) {
                    binding.llCustomerDialog.visibility = View.GONE
                } else {
                    binding.llCustomerDialog.visibility = View.VISIBLE
                }
            }

            binding.constraintHeader.setOnClickListener {
                if (binding.llCustomerDialog.visibility == View.VISIBLE) {
                    binding.llCustomerDialog.visibility = View.GONE
                }

                //  listner.onHeaderSelected(layoutPosition)
                list.get(0).selectedPosition = layoutPosition
                notifyDataSetChanged()
            }
        }

        override fun onItemClickListener(view: View?, data: TbItem, position: Int?) {
            list.get(0).itemPosition = position
            list.get(0).headerPosition = layoutPosition
            position?.let { listner.onItemSelected(layoutPosition, it, data) }

        }

    }

    fun setList(list: ArrayList<DineInModel>) {
        for (i in 0 until list.size) {
            if (list[i].items.isNotEmpty()) {
               /* list[i].items.forEachIndexed { index, it ->
                    if (it.isDestroy) {
                        list[i].items.removeAt(index)

                    }
                }*/

                val it: MutableIterator<TbItem> = list.get(i).items.iterator()
                while (it.hasNext()) {
                    val s: TbItem = it.next()
                    if (s.isDestroy) {
                        it.remove()
                    }
                }
            }


        }

        {

        }

        this.list = list
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addItem(item: TbItem, selectedPos: Int) {
        val items: ArrayList<TbItem> = list.get(selectedPos).items
        items.add(item)
        list.add(selectedPos, DineInModel(0, false, selectedPos, items = items))
        notifyDataSetChanged()
    }

    fun setListner(listner: DineInCallback) {
        this.listner = listner
    }

    interface DineInCallback {
        fun onHeaderSelected(position: Int)
        fun onItemSelected(headerPosition: Int, position: Int, item: TbItem)
        fun onCustomerClicked(position: Int, isRemoved: Boolean)
        fun onItemDelete(position: Int, itemPosition: Int, data: TbItem)
    }

    fun getHeaderPosition(): Int {
        return list.get(0).selectedPosition
    }

    fun getItem(position: Int): DineInModel {
        return list.get(position)
    }

    fun getList(): ArrayList<DineInModel> {
        return this.list
    }

    override fun onItemClickListener(view: View?, data: TbItem, position: Int?) {
        Log.e(TAG, "DineInItem:  ${Gson().toJson(data)}")
    }

    private fun swipeListener(recyclerView: RecyclerView, headerPosition: Int, context: Context) {


        val itemTouchHelper = ItemTouchHelper(object : SwipeHelper(recyclerView) {
            override fun instantiateUnderlayButton(position: Int): List<UnderlayButton> {


                val deleteButton = deleteButton(position, context, headerPosition)
                /*  val markAsUnreadButton = markAsUnreadButton(position)
                  val archiveButton = archiveButton(position)*/
                return listOf(deleteButton)
            }
        })

        itemTouchHelper.attachToRecyclerView(recyclerView)

    }

    private fun deleteButton(
        position: Int,
        context: Context,
        headerPos: Int
    ): SwipeHelper.UnderlayButton {
        return SwipeHelper.UnderlayButton(
            context,
            "Delete",
            14.0f,
            R.color.delete,
            object : SwipeHelper.UnderlayButtonClickListener {
                override fun onClick() {
                    list.get(0).headerPosition = headerPos
                    list.get(0).itemPosition = position
                    listner.onItemDelete(
                        headerPos,
                        position,
                        list.get(headerPos).items.get(position)
                    )
                }
            })
    }

}