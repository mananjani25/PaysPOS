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
import com.pays.pos.data.remote.Constants
import com.pays.pos.databinding.DailogCardRederBinding
import com.pays.pos.databinding.DailogOrderTypeBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.ui.adapter.OrderTypeAdapter
import com.pays.pos.utils.callback.ItemCallback
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject


@AndroidEntryPoint
class CardRederDialog : DialogFragment() {

    private lateinit var binding: DailogCardRederBinding

    @Inject
    lateinit var prefProvider: PrefProvider

    companion object {
        fun newInstance() = CardRederDialog()

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dailog_card_reder, container, false)
        binding.lifecycleOwner = this


        val device = prefProvider.getValueInt(Constants.MAGTEK_HARDWARE, 0)

        if (device == 0) {
            binding.imgDynamo.setImageResource(R.drawable.device_card_selected)
            binding.imgDynaFlex.setImageResource(R.drawable.device_card)
        } else {
            binding.imgDynamo.setImageResource(R.drawable.device_card)
            binding.imgDynaFlex.setImageResource(R.drawable.device_card_selected)
        }

        binding.iimgBack.setOnClickListener {
            dialog?.cancel()
            dialog?.dismiss()
        }
        binding.llDynamo.setOnClickListener {
            prefProvider.setValueInt(Constants.MAGTEK_HARDWARE, 0)
            findNavController().navigate(R.id.action_cardRederDialog_to_magtekFragment)
        }

        binding.llDynaFlex.setOnClickListener {
            prefProvider.setValueInt(Constants.MAGTEK_HARDWARE, 1)
            findNavController().navigate(R.id.action_cardRederDialog_to_magtekProFragment)

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
        window.setLayout((width * 0.40).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
        window.setGravity(Gravity.CENTER)
    }

}