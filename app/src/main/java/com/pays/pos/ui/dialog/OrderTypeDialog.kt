package com.pays.pos.ui.dialog

import android.graphics.Point
import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.pays.pos.R
import com.pays.pos.data.entities.TbOrderType
import com.pays.pos.databinding.DailogOrderTypeBinding
import com.pays.pos.ui.adapter.OrderTypeAdapter
import com.pays.pos.utils.callback.ItemCallback
import dagger.hilt.android.AndroidEntryPoint
import java.util.*


@AndroidEntryPoint
class OrderTypeDialog : DialogFragment(), ItemCallback {
    private var orderTypeList: ArrayList<TbOrderType>? = null
    private lateinit var orderTypeAdapter: OrderTypeAdapter
    private lateinit var binding: DailogOrderTypeBinding

    companion object {
        fun newInstance() = OrderTypeDialog()

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dailog_order_type, container, false)
        binding.lifecycleOwner = this

        orderTypeList = requireArguments().getParcelableArrayList("data")

        setAdapter(orderTypeList)

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()

        val window: Window? = dialog!!.window
        val size = Point()
        val display: Display = window?.windowManager?.defaultDisplay!!
        display.getSize(size)
        val width: Int = size.x
        window.setLayout((width * 0.30).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
        window.setGravity(Gravity.CENTER)
    }

    private fun setAdapter(orderTypeList: ArrayList<TbOrderType>?) {

        orderTypeAdapter = OrderTypeAdapter()
        orderTypeAdapter.setCallback(this)
        binding.rvOrderType.adapter = orderTypeAdapter

        if (orderTypeList != null) {
            orderTypeAdapter.addAll(orderTypeList)
        }

    }

    override fun onItemClickListener(view: View?, pos: Int) {
        val orderType = orderTypeAdapter.getItem(pos)

        val result = Bundle().apply {
            putParcelable("data", orderType)
        }
        setFragmentResult("request_key_orderType", result)
        dismiss()


    }
}