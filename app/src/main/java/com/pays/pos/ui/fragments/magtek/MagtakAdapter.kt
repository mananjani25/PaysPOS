package com.pays.pos.ui.fragments.magtek

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.data.entities.TbCardReader
import com.pays.pos.databinding.ViewMagtekDeviceBinding
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.callback.ItemCallback

class MagtakAdapter :
    RecyclerView.Adapter<MagtakAdapter.MyViewHolder>() {

    var list = ArrayList<TbCardReader>()
    private var resourceId: Int = 0

    private lateinit var mCallback: ItemCallback
    fun setCallback(callback: ItemCallback) {
        mCallback = callback
    }

    inner class MyViewHolder(private val binding: ViewMagtekDeviceBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: TbCardReader) {
            binding.model = item
            binding.executePendingBindings()

            LogUtil.logE("MyViewHolder", item.mcAddress)

            if (item.status == 0) {
                binding.txtStatus.text = "Connect"
            } else {
                binding.txtStatus.text = "Disconnect"
            }


            binding.txtStatus.setOnClickListener {

                mCallback.onItemClickListener(it, bindingAdapterPosition)
            }
        }

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MagtakAdapter.MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewMagtekDeviceBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MagtakAdapter.MyViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun add(bluetoothDevice: TbCardReader?) {

        if (!alreadyDevice(bluetoothDevice)) {
            bluetoothDevice?.let { list.add(it) }
            notifyDataSetChanged()
        }


    }

    private fun alreadyDevice(bluetoothDevice: TbCardReader?): Boolean {

        list.forEach {
            return if (bluetoothDevice != null) {
                bluetoothDevice.mcAddress.replace(":", "") == it.mcAddress
            } else {
                false
            }

        }
        return false

    }

    fun clear() {
        list.clear()
        notifyDataSetChanged()
    }

    fun update(resourceId: Int, status: Int) {
        if (list.isNotEmpty())
            list[resourceId].status = status
        notifyDataSetChanged()
    }

    fun getItem(selectedPos: Int): TbCardReader? {
        if (list.isNotEmpty())
            return list[selectedPos]
        return null
    }
}