package com.android.pos.ui.fragments.settings.hardware.printerqueue

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.model.PrinterQueueModel
import com.android.pos.databinding.FragmentPrinterQueueBinding
import com.android.pos.ui.adapter.PrinterQueueListAdapter
import com.hosopy.actioncable.ActionCable
import com.hosopy.actioncable.Channel
import com.hosopy.actioncable.Consumer
import com.hosopy.actioncable.Subscription
import java.net.URI
import com.hosopy.actioncable.ActionCableException

import com.google.gson.JsonElement
import com.hosopy.actioncable.Subscription.*


class PrinterQueue : Fragment() {
    private lateinit var binding: FragmentPrinterQueueBinding
    private val list: ArrayList<PrinterQueueModel> = arrayListOf()
    private lateinit var adapter: PrinterQueueListAdapter
    private val TAG = "PrinterQueue"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPrinterQueueBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setAdapter()
        onClick()
        connectActionCable()

    }

    private fun connectActionCable() {
        // 1. Setup
        val uri = URI("wss://possoft.io/cable")
        val consumer: Consumer = ActionCable.createConsumer(uri)

        // 2. Create subscription
        val appearanceChannel = Channel("KitchenChannel")
        val subscription: Subscription = consumer.subscriptions.create(appearanceChannel)

        subscription
            .onConnected {
                Log.e(TAG,"onActionConnected")
            }.onRejected {
                Log.e(TAG,"onActiononRejected")
            }.onReceived {
                Log.e(TAG,"onActiononReceived")
            }.onDisconnected {
                Log.e(TAG,"onActiononDisconnected")
            }.onFailed {
                Log.e(TAG,"onActiononFailed")
            }

        // 3. Establish connection
        consumer.connect();

    }

    private fun onClick() {
        binding.imgClose.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.txtHome.setOnClickListener {
            findNavController().navigate(R.id.action_printerQueue_to_dashboardCategoryNew)
        }
    }

    private fun setAdapter() {
        list.add(
            PrinterQueueModel(
                0,
                "BKJH976LO65",
                "54",
                "Open Order",
                45.00,
                "CREDIT CARD",
                "Terminal One",
                "IN PROCESS"
            )
        )
        list.add(
            PrinterQueueModel(
                0,
                "BKJH976LO65",
                "55",
                "Dine In",
                114.49,
                "CREDIT CARD",
                "Terminal One",
                "PENDING"
            )
        )
        list.add(
            PrinterQueueModel(
                0,
                "BKJH976LO65",
                "56",
                "Take Out",
                123.69,
                "CREDIT CARD",
                "Terminal One",
                "IN PROCESS"
            )
        )
        list.add(
            PrinterQueueModel(
                0,
                "BKJH976LO65",
                "54",
                "Open Order",
                67.00,
                "CASH",
                "Terminal One",
                "IN PROCESS"
            )
        )
        adapter = PrinterQueueListAdapter()
        binding.rvPrinterQueueList.adapter = adapter
        adapter.setList(list)

    }
}