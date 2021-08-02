package com.android.pos.ui.dialog

import android.graphics.Point
import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment
import com.android.pos.databinding.DailogSplitAmountBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SplitAmountFragment : DialogFragment() {

    private lateinit var binding: DailogSplitAmountBinding

    companion object {
        fun newInstance() = SplitAmountFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DailogSplitAmountBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        setupData()

        return binding.root
    }

    private fun setupData() {

        binding.txtCustom.setOnClickListener {

        }
    }

    override fun onResume() {
        super.onResume()

        val window: Window? = dialog!!.window
        val size = Point()
        val display: Display = window?.windowManager?.defaultDisplay!!
        display.getSize(size)
        val width: Int = size.x
        window.setLayout((width * 0.50).toInt(), WindowManager.LayoutParams.MATCH_PARENT)
        window.setGravity(Gravity.CENTER)
    }
}