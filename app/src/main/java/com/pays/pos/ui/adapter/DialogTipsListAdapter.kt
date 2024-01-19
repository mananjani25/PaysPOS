package com.pays.pos.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.R
import com.pays.pos.data.model.responseModel.GetTipReponse
import com.pays.pos.data.model.responseModel.NoteResponse
import com.pays.pos.databinding.ViewDialogTipsListBinding

class DialogTipsListAdapter : RecyclerView.Adapter<DialogTipsListAdapter.MyViewHolder>() {
    var selectedPosition = -1
    private lateinit var listner: DiscountInterface

    inner class MyViewHolder(private val binding: ViewDialogTipsListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(model: GetTipReponse.Data, position: Int) {
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
            binding.model = model
            binding.executePendingBindings()

        }

        init {
            binding.root.setOnClickListener {
                selectedPosition = layoutPosition
                notifyDataSetChanged()
                listner.selectedItem(discountList[layoutPosition], layoutPosition)
            }
        }
    }

    var discountList = ArrayList<GetTipReponse.Data>()
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DialogTipsListAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewDialogTipsListBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)

    }

    override fun onBindViewHolder(holder: DialogTipsListAdapter.MyViewHolder, position: Int) {
        holder.bind(discountList[position], position)
    }

    override fun getItemCount(): Int {
        return discountList.size
    }

    fun setList(list: List<GetTipReponse.Data>?) {
        this.discountList.apply {
            clear()
            if (list != null) {
                addAll(list)
            }
        }
        notifyDataSetChanged()
    }

    fun getItem(position: Int): GetTipReponse.Data {
        return discountList[position]
    }

    interface DiscountInterface {
        fun selectedItem(model: GetTipReponse.Data, pos: Int)
    }

    fun setListner(Mlistner: DiscountInterface) {
        listner = Mlistner

    }

    /*fun getSelectedItem():GetTipReponse.Data{
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
    fun setSelected(id: Int?) {
        for (i in 0 until discountList.size) {
            if (id == discountList[i].id) {
                selectedPosition = i
                notifyDataSetChanged()
            }
        }

    }

}