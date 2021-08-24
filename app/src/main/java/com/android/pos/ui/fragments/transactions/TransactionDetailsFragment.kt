package com.android.pos.ui.fragments.transactions

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.pos.R
import com.android.pos.data.entities.Employee
import com.android.pos.data.entities.TbOrderType
import com.android.pos.data.entities.TeamRole
import com.android.pos.data.model.responseModel.VenueDetailsResponse
import com.android.pos.data.remote.Constants
import com.android.pos.databinding.FragmentTransactionBinding
import com.android.pos.databinding.FragmentTransactionDetailsBinding
import com.android.pos.ui.activities.MainActivity
import com.android.pos.ui.adapter.TransactionAdapter
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.liveSnackBar
import com.android.pos.utils.extensions.showAlert
import com.android.pos.utils.statusUtils.Status
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import kotlin.collections.ArrayList

@AndroidEntryPoint
class TransactionDetailsFragment : Fragment() {

    private lateinit var binding: FragmentTransactionDetailsBinding


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_transaction_details,
                container,
                false
            )

        // binding.viewModel = viewModel


        binding.lifecycleOwner = this
        return binding.root
    }
}