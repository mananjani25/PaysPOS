package com.android.pos.ui.fragments.loginscreen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.remote.Constants.USERNAME
import com.android.pos.databinding.FragmentScheduledShiftBinding
import com.android.pos.di.PrefProvider
import com.android.pos.utils.TimeFormatUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ScheduledShifts : Fragment() {
    lateinit var binding: FragmentScheduledShiftBinding

    @Inject
    lateinit var prefProvider: PrefProvider

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_scheduled_shift, container, false)
        binding.lifecycleOwner = this

        binding.txtTitle.text = prefProvider.getValue(USERNAME, "")
        binding.txtTime.text = TimeFormatUtils.showCurrentTime()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onClick()
    }

    private fun onClick() {


        binding.txtClockIn.setOnClickListener {
            findNavController().navigate(R.id.action_scheduledShifts_to_passcode)

        }
        binding.imgCancel.setOnClickListener {
            activity?.finish()
        }
    }
}