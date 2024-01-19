package com.pays.pos.ui.fragments.settings.hardware.scangun

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.databinding.ViewScannerBinding
import com.pays.pos.utils.extensions.visible
import com.pays.pos.utils.scanner.helpers.AvailableScanner

class ScannerListAdapter(
    val isConnectedList: Boolean,
    val callBack: ((View, AvailableScanner) -> Unit)?
) :
    RecyclerView.Adapter<ScannerListAdapter.MyViewHolder>() {

    var arrayList = ArrayList<AvailableScanner>()

    inner class MyViewHolder(private val binding: ViewScannerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(availableScanner: AvailableScanner) {

            //available scanner
            binding.txtScannerId.text =
                "${availableScanner.scannerName} : (${availableScanner.scannerAddress})"

            //connect, disconnect
            binding.txtDisConnect.visible()
            if (isConnectedList) {
                binding.txtDisConnect.text = "Disconnect"
                binding.txtDisConnect.visible()
            } else {
                binding.txtDisConnect.text = "Connect"
            }
            /*if (isConnectedList) {
                binding.txtConnect.gone()
            } else {
                binding.txtConnect.visible()
                if (availableScanner.isConnected) {
                    binding.txtConnect.text = "Disconnect"
                } else {
                    binding.txtConnect.text = "Connect"
                }
            }*/

            binding.txtDisConnect.setOnClickListener { view ->
                view?.let {
                    callBack?.invoke(it, arrayList[absoluteAdapterPosition])
                }
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ViewScannerBinding.inflate(inflater, parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(arrayList[position])
    }

    override fun getItemCount(): Int {
        return arrayList.size
    }

    fun add(arrayList: List<AvailableScanner>?) {
        this.arrayList.clear()
        if (arrayList?.isNotEmpty() == true) {
            this.arrayList.addAll(arrayList)
        }
        notifyDataSetChanged()
    }

}