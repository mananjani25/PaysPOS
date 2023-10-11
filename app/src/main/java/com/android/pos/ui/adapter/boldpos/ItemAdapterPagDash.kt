package com.android.pos.ui.adapter.boldpos

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.data.entities.TbItem
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.ADD_VALUE
import com.android.pos.data.remote.Constants.BALANCE_INQUIRY
import com.android.pos.data.remote.Constants.DEFAULT_ORDER
import com.android.pos.data.remote.Constants.SELL_CARD
import com.android.pos.data.remote.Constants.TAKEOUT
import com.android.pos.databinding.ViewCategoryItemBoldBinding
import com.android.pos.di.PrefProvider
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.LogUtil
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.callback.ItemListner
import com.android.pos.utils.extensions.gone
import com.android.pos.utils.extensions.visible
import org.greenrobot.eventbus.EventBus


class ItemAdapterPagDash(
    val listener: ItemListner,
    var lastChecked: TextView? = null,
    var prefProvider: PrefProvider? = null
) : PagingDataAdapter<TbItem, ItemAdapterPagDash.ViewHolder>(
    ItemListPageAdapter.DIFF_CALLBACK
) {

    private var mpos: Int = -2


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ItemAdapterPagDash.ViewHolder {
        return ViewHolder(
            ViewCategoryItemBoldBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    inner class ViewHolder(val binding: ViewCategoryItemBoldBinding) :
        RecyclerView.ViewHolder(binding.root) {

        val checkedTextView = binding.txtCategoryName


        @SuppressLint("ResourceType")
        fun bind(model: TbItem?, position: Int) {

            var itename_price: StringBuffer = StringBuffer()
            if (model?.hide_status == "HideForToday" || model?.hide_status == "HideForIndefinitely") {
                if (model.name.length >= 30) {
                    setItemNameWhileHide(
                        model.name.substring(
                            0,
                            30
                        ) + "...", "\nSOLD OUT", binding.txtCategoryName
                    )
                } else {
                    setItemNameWhileHide(model.name, "\n\nSOLD OUT", binding.txtCategoryName)
                }
            } else {
                if (model?.name?.length!! >= 30) {
                    if (getItemPriceIsValid(model.price).isNotEmpty()) {
                        itename_price.append(
                            model.name.substring(
                                0,
                                30
                            ) + "...\n" + getItemPriceIsValid(model.price)
                        )
                        binding.txtCategoryName.text = itename_price
                    } else {
//                        itename_price.append(
//                            model.name.substring(
//                                0,
//                                40
//                            ) + "..."
//                        )
                        if(model.name.length >= 40){
                            itename_price.append(
                                model.name.substring(
                                    0,
                                    40
                                ) + "..."
                            )
                        }else{
                            itename_price.append(
                                model.name.substring(
                                    0,
                                    model.name.length
                                ) + "..."
                            )
                        }
                        binding.txtCategoryName.text = itename_price
                    }
                } else {
                    if (getItemPriceIsValid(model.price).isNotEmpty()) {
                        binding.txtCategoryName.text =
                            "" + model.name + "\n" + getItemPriceIsValid(model.price)
                    } else {
                        binding.txtCategoryName.text = "" + model.name
                    }

                }
            }




            if (model.modifier_set_ids.isNotEmpty() || model.variationsAttributes.isNotEmpty()) {
                binding.viewLineFormodifier.visible()
            } else {
                binding.viewLineFormodifier.gone()
            }

            binding.txtPrice.gone()
            if (mpos == absoluteAdapterPosition) {

                if (lastChecked != null) {
                    lastChecked?.isSelected = false
                }
                lastChecked = checkedTextView
                binding.txtCategoryName.isSelected = false
            } else {
                binding.txtCategoryName.isSelected = false

            }

            binding.root.setOnClickListener {

                /*  if (prefProvider?.getValue(Constants.ORDER_TYPE, "").equals("")) {
                      AlertUtils.showCustomAlert(
                          binding.root.context,
                          "Please select order type to add item"
                      )
                      return@setOnClickListener
                  } else*/
                if (lastChecked != null) {
                    lastChecked?.isSelected = false
                }
                lastChecked = checkedTextView
                if (model?.hide_status == "HideForToday" || model?.hide_status == "HideForIndefinitely") {
                    AlertUtils.showCustomAlert(binding.root.context, model.name + " is sold out.")
                    return@setOnClickListener
                } else if(model.name == SELL_CARD || model.name == ADD_VALUE || model.name == BALANCE_INQUIRY){
                    listener.onItemSelected(model)
                    return@setOnClickListener
                } else {
                    try {
                        getItem(position)?.let {
                            LogUtil.logE(
                                "ITemAdapter",
                                "onClickposition  ${position}  itemname ${it.name} itemQty = ${it.itemQuantity}"
                            )
                            if (prefProvider?.getValue(Constants.ORDER_TYPE, "").equals("")) {
                                prefProvider?.setValue(Constants.ORDER_TYPE, TAKEOUT)
                                prefProvider?.setValue(Constants.ORDER_TYPE_NAME, DEFAULT_ORDER)
                                EventBus.getDefault().post("EventBus")
                            }

                            listener.onItemSelected(it)
                            return@setOnClickListener


                        }
                    } catch (e: Exception) {
                        return@setOnClickListener
                        e.printStackTrace()
                    }

                }


            }
        }


    }


    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<TbItem>() {
            override fun areItemsTheSame(oldItem: TbItem, newItem: TbItem): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: TbItem, newItem: TbItem): Boolean {
                return oldItem.itemId == newItem.itemId
            }

        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)


    }

    fun getItemPriceIsValid(amount: Double): String {
        return if (prefProvider?.getValueboolean(
                Constants.ONLY_SHOW_PRICE_GREATER_THAN_ZERO,
                false
            ) == false
        ) {
            amount.let { MethodUtils.roundOffAmount(it) }
        } else if (amount > 0.0) {
            amount.let { MethodUtils.roundOffAmount(it) }
        } else {
            ""
        }


    }

    fun setItemNameWhileHide(first: String, next: String, txtCategoryName: TextView) {
        txtCategoryName.setText(first + next, TextView.BufferType.SPANNABLE)
        val s: Spannable = txtCategoryName.text as Spannable
        val start: Int = first.length
        val end: Int = start + next.length
        s.setSpan(ForegroundColorSpan(Color.RED), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        s.setSpan(StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        s.setSpan(StrikethroughSpan(), 0, start, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    fun setPos(selectedId: Int) {
        mpos = selectedId
    }

    fun clearData() {
        snapshot().toCollection(arrayListOf()).clear()
        notifyDataSetChanged()
    }


}