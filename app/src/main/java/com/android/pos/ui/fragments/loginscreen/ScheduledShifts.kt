package com.android.pos.ui.fragments.loginscreen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.databinding.FragmentScheduledShiftBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ScheduledShifts : Fragment() {
    lateinit var  binding : FragmentScheduledShiftBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_scheduled_shift,container,false)
        binding.lifecycleOwner = this

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
    }
}