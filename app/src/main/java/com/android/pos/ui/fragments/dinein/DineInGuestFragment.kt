package com.android.pos.ui.fragments.dinein

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.android.pos.databinding.FragmentDineInGuestBinding
import com.android.pos.ui.adapter.GuestListAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DineInGuestFragment : Fragment() {
    private lateinit var binding: FragmentDineInGuestBinding

    private lateinit var guestListAdapter: GuestListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDineInGuestBinding.inflate(inflater, container, false)

        setUpRecyclerView()

        return binding.root
    }

    private fun setUpRecyclerView() {
        guestListAdapter = GuestListAdapter()
        binding.rvNumberOfGuests.adapter = guestListAdapter

        val numberOfGuestList = ArrayList<Int>()
        for (i in 1..20) {
            numberOfGuestList.add(i)
        }
        guestListAdapter.addGuests(numberOfGuestList)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

}