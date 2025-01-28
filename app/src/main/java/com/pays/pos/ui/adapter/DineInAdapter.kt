package com.pays.pos.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.get
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.R
import com.pays.pos.data.entities.TbCartItem
import com.pays.pos.data.entities.TbItem
import com.pays.pos.data.model.DineInModel
import com.pays.pos.databinding.ViewDineInItemBinding
import com.pays.pos.ui.activities.SwipeHelper
import com.pays.pos.ui.adapter.boldpos.CartAdapter
import com.pays.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.pays.pos.ui.fragments.dashboard.bolddashboard.DashboardCategoryBoldPOS
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.callback.MyCallback
import com.pays.pos.utils.extensions.gone
import com.pays.pos.utils.extensions.setOnSingleClickListener

class DineInAdapter(val dashBoardCategoryViewModel: DashBoardCategoryViewModel? = null) : RecyclerView.Adapter<DineInAdapter.MyViewHolder>() {
    private var list: ArrayList<DineInModel> = arrayListOf()
    private var itemList: ArrayList<TbCartItem> = arrayListOf()
    private lateinit var listner: DineInCallback
    private lateinit var itemAdapter: com.pays.pos.ui.adapter.boldpos.CartAdapter
    private val TAG = "DineInAdapter"
    private var isFromPay = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DineInAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewDineInItemBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DineInAdapter.MyViewHolder, position: Int) {
        holder.bind(list.get(position), itemList , position)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    inner class MyViewHolder(private val binding: ViewDineInItemBinding) :
        RecyclerView.ViewHolder(binding.root), MyCallback {

        fun bind(model: DineInModel , itemList : ArrayList<TbCartItem>, position: Int) {
            binding.model = model
            binding.executePendingBindings()
            itemAdapter = CartAdapter()
            binding.rvCart.adapter = itemAdapter
            itemAdapter.setCallback(this)
            //swipeListener(binding.rvCart, layoutPosition, binding.root.context)
            itemList.forEach {
                if (it.guestIndexForDineIn == layoutPosition){
                    itemAdapter.addItemInList(it)
                    itemAdapter.notifyDataSetChanged()
                }
            }

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
                binding.constraintHeader.setBackground(binding.root.context.getDrawable(R.color.btnColorDark))
                binding.txtTableName.setTextColor(binding.root.context.resources.getColor(R.color.white))
                binding.imgOrderMenu.setColorFilter(binding.root.context.resources.getColor(R.color.white))

                if (layoutPosition == 0) {
                    binding.imgProfile.setColorFilter(binding.root.context.resources.getColor(R.color.white))
                } else {
                    binding.imgProfile.colorFilter = null
                }

            } else {
                binding.rvCart.visibility = View.VISIBLE
                binding.constraintHeader.setBackground(
                    binding.root.context.getDrawable(R.drawable.background_dine_in_selected)
                )
                binding.txtTableName.setTextColor(binding.root.context.resources.getColor(R.color.white))
                binding.imgOrderMenu.setColorFilter(binding.root.context.resources.getColor(R.color.white))
                if (layoutPosition == 0) {
                    binding.imgProfile.setColorFilter(binding.root.context.resources.getColor(R.color.white))
                } else {
                    // binding.imgProfile.setColorFilter(binding.root.context.resources.getColor(R.color.white))
                    binding.imgProfile.colorFilter = null
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

            if (isFromPay){
                binding.imgOrderMenu.gone()
            }


        }

        init {

            binding.txtCrtNewCustomer.setOnClickListener {
                if (list.get(layoutPosition).customer != null) {
                    //list.get(layoutPosition).customer = null
                    listner.onCustomerClicked(layoutPosition, true)
                    binding.llCustomerDialog.visibility = View.GONE

                } else {
                    list.get(0).selectedPosition = bindingAdapterPosition
                    listner.onCustomerClicked(layoutPosition, false)
                }

            }

            val popupMenu = PopupMenu(itemView.context, binding.imgOrderMenu)
            popupMenu.menuInflater.inflate(R.menu.assign_customer_menu, popupMenu.menu)

            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.assign_customer -> {

                        if (dashBoardCategoryViewModel?.isItemEditInProgress != true) {

                            if (list.get(layoutPosition).customer != null) {
                                //list.get(layoutPosition).customer = null
                                listner.onCustomerClicked(layoutPosition, true)
                                binding.llCustomerDialog.visibility = View.GONE

                            } else {
                                listner.onCustomerClicked(layoutPosition, false)
                            }
                        } else {
                            AlertUtils.showCustomAlert(
                                itemView.context,
                                "Cannot change guest as already updating another item"
                            )
                        }

                    }

                    R.id.remove_guest -> {
                        listner.onRemoveGuest(layoutPosition)
                    }
                }
                true
            }

            binding.imgOrderMenu.setOnSingleClickListener {

                if (list[layoutPosition].customer == null) {
                    popupMenu.menu.get(0).setTitle("Assign Customer")
                } else {
                    popupMenu.menu.get(0).setTitle("Remove Customer")
                }
                popupMenu.menu[1].isVisible = list[layoutPosition].items.size <= 0

                popupMenu.show()

            }

            binding.constraintHeader.setOnClickListener {

                if(dashBoardCategoryViewModel?.isItemEditInProgress != true) {

                    if (binding.llCustomerDialog.visibility == View.VISIBLE) {
                        binding.llCustomerDialog.visibility = View.GONE
                    }
                    list.get(0).selectedPosition = layoutPosition
                }
                listner.onHeaderSelected(layoutPosition)

                notifyDataSetChanged()
            }
        }

        override fun onItemClickListener(view: View?, data: TbCartItem, position: Int) {
            list.get(0).itemPosition = position
            list.get(0).headerPosition = layoutPosition
            listner.onItemSelected(bindingAdapterPosition, position, data)

        }

        override fun onCartItemClickListener(view: View?, data: TbCartItem, position: Int) {
            TODO("Not yet implemented")
        }

    }

    fun setList(list: ArrayList<DineInModel> , itemList : ArrayList<TbCartItem>) {
        /*
        for (i in 0 until list.size) {
            if (list[i].items.isNotEmpty()) {
               *//* list[i].items.forEachIndexed { index, it ->
                    if (it.isDestroy) {
                        list[i].items.removeAt(index)

                    }
                }*//*

                val it: MutableIterator<TbItem> = list.get(i).items.iterator()
                while (it.hasNext()) {
                    val s: TbItem = it.next()
                    if (s.isDestroy) {
                        it.remove()
                    }
                }
            }


        }
        */

        var filteredList: ArrayList<DineInModel> = arrayListOf()

        list.forEach {
            if(!it.isDestroy) {
                filteredList.add(it)
            }
        }
        this.itemList = itemList
        this.list = filteredList
        notifyDataSetChanged()
    }

    fun setItemList( itemList : ArrayList<TbCartItem>){
        this.itemList = itemList
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addItem(item: TbItem, selectedPos: Int) {
        val items: ArrayList<TbItem> = list.get(selectedPos).items
        items.add(item)
        list.add(selectedPos, DineInModel(0, false, selectedPos, items = items,))
        notifyDataSetChanged()
    }

    fun setListner(listner: DineInCallback) {
        this.listner = listner
    }

    interface DineInCallback {
        fun onHeaderSelected(position: Int)
        fun onItemSelected(headerPosition: Int, position: Int, item: TbCartItem)
        fun onCustomerClicked(position: Int, isRemoved: Boolean)
        fun onItemDelete(position: Int, itemPosition: Int, data: TbCartItem)
        fun onRemoveGuest(position: Int)
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
                        itemList[position]
//                        list.get(headerPos).items.get(position)
                    )
                }
            })
    }


    fun clearList() {
        list.clear()
        list = arrayListOf()
        notifyDataSetChanged()
    }

    fun isFromPayment(fromPayment: Boolean) {
        isFromPay = fromPayment

    }

    fun setLayoutPos(dineInHeaderPosition: Int) {
        list.get(0).selectedPosition = dineInHeaderPosition
        notifyDataSetChanged()
    }
}