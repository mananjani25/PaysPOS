package com.pays.pos.ui.fragments.magtekPro

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.databinding.ViewMagtekProDeviceBinding
import com.pays.pos.utils.callback.ItemCallback
import com.pays.pos.utils.extensions.setOnSingleClickListener
import com.magtek.mobile.android.mtusdk.IDevice

class MagtakProAdapter :
    RecyclerView.Adapter<MagtakProAdapter.MyViewHolder>() {

    var list = ArrayList<IDevice>()
    var isConnected = false

    private lateinit var mCallback: ItemCallback
    fun setCallback(callback: ItemCallback) {
        mCallback = callback
    }

    inner class MyViewHolder(private val binding: ViewMagtekProDeviceBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: IDevice) {
            binding.model = item
            binding.executePendingBindings()

            if (!isConnected) {
                binding.txtStatus.text = "Connect"
            } else {
                binding.txtStatus.text = "Disconnect"
            }

            binding.txtStatus.setOnSingleClickListener {

                if (isConnected) {
                    mCallback.onItemClickListener(null, bindingAdapterPosition)
                } else

                    mCallback.onItemClickListener(it, bindingAdapterPosition)
            }
        }

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MagtakProAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewMagtekProDeviceBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MagtakProAdapter.MyViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun add(iDevice: List<IDevice>) {
        list = iDevice as ArrayList<IDevice>
        notifyDataSetChanged()
    }

    fun clear() {
        list.clear()
        notifyDataSetChanged()
    }

    fun getItem(pos: Int): IDevice {

        return list[pos]
    }

    fun update(isConnected: Boolean) {
        this.isConnected = isConnected
        notifyDataSetChanged()
    }

}