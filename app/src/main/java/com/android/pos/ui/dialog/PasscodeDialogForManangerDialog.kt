package com.android.pos.ui.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.android.pos.R
import com.android.pos.databinding.PasscodeDialogForManangerFragmentBinding

class PasscodeDialogForManangerDialog : DialogFragment() {

    lateinit var binding:PasscodeDialogForManangerFragmentBinding
    companion object {
        fun newInstance() = PasscodeDialogForManangerDialog()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.passcode_dialog_for_mananger_fragment, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        val back = ColorDrawable(Color.WHITE)
        val inset = InsetDrawable(back, 150, 100, 150, 100)
        dialog?.window?.setBackgroundDrawable(inset);
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

    }

}