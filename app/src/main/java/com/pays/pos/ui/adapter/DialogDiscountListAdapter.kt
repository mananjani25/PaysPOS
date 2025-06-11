package com.pays.pos.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.R
import com.pays.pos.data.entities.TbDiscount
import com.pays.pos.databinding.ViewDialogDiscountListBinding
import com.pays.pos.databinding.ViewDialogDiscountListUpdateBinding

class DialogDiscountListAdapter : RecyclerView.Adapter<DialogDiscountListAdapter.MyViewHolder>() {
    var selectedPosition = -1
    private var previousSelectedPosition = -1
    private lateinit var listner: DiscountInterface

    inner class MyViewHolder(private val binding: ViewDialogDiscountListUpdateBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(model: TbDiscount, position: Int) {
            if (selectedPosition == position) {
                binding.linearParent.background =
                    binding.root.context.getDrawable(R.drawable.button_selected)
                binding.txtValue.setTextColor(binding.root.context.resources.getColor(R.color.white))
                binding.txtName.setTextColor(binding.root.context.resources.getColor(R.color.white))

            } else {
                binding.linearParent.background =
                    binding.root.context.getDrawable(R.drawable.background_square_border_grey)
                binding.txtValue.setTextColor(binding.root.context.resources.getColor(R.color.txtColor))
                binding.txtName.setTextColor(binding.root.context.resources.getColor(R.color.txtColor))
            }

            val context = binding.root.context
            if (model.discountType == context.getString(R.string.disc_percentage)) {
                binding.txtValue.text = "" + String.format(
                    context.getString(R.string.format),
                    model.percentage
                ) + " " + context.getString(R.string.percentage_symbol)
            } else {
                binding.txtValue.text = context.getString(R.string.symbole) + " " + String.format(
                    context.getString(R.string.format),
                    model.percentage
                )
            }
            binding.model = model
            binding.executePendingBindings()

        }

        init {
            binding.root.setOnClickListener {
                previousSelectedPosition = selectedPosition
                selectedPosition = layoutPosition
                if(previousSelectedPosition == selectedPosition){
                    selectedPosition = -1
                }
                notifyDataSetChanged()
                listner.selectedItem(discountList[layoutPosition], selectedPosition)
            }
        }
    }

    var discountList = ArrayList<TbDiscount>()
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DialogDiscountListAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewDialogDiscountListUpdateBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: DialogDiscountListAdapter.MyViewHolder, position: Int) {
        holder.bind(discountList[position], position)
    }

    override fun getItemCount(): Int {
        return discountList.size
    }

    fun setList(list: List<TbDiscount>) {
        this.discountList.apply {
            clear()
            addAll(list)
        }
        notifyDataSetChanged()
    }

    fun getItem(position: Int): TbDiscount {
        return discountList[position]
    }

    interface DiscountInterface {
        fun selectedItem(model: TbDiscount, pos: Int)
    }

    fun setListner(Mlistner: DiscountInterface) {
        listner = Mlistner

    }

    /*fun getSelectedItem():TbDiscount{
       return  if (selectedPosition == -1){

        }
        else{
            discountList[selectedPosition]
        }

    }*/

    @SuppressLint("NotifyDataSetChanged")
    fun clearSelectedItem() {
        selectedPosition = -1
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setSelected(id:Int?) {
        for (i in 0 until discountList.size){
            if (id == discountList[i].id){
                selectedPosition = i
                notifyDataSetChanged()
            }
        }

    }

}