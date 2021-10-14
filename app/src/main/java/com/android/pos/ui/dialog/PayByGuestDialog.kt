package com.android.pos.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.android.pos.databinding.DailogAddDiscountBinding
import com.android.pos.databinding.PaymentFragmentBinding
import com.android.pos.ui.adapter.DialogDiscountListAdapter

class PayByGuestDialog : DialogFragment() {

    private lateinit var binding: PaymentFragmentBinding

    companion object {
        fun newInstance() = PayByGuestDialog()

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = PaymentFragmentBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }


}