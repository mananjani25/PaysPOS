package com.android.pos.ui.fragments.settings.hardware.printerqueue

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.model.PrinterQueueModel
import com.android.pos.data.remote.Constants.LOCATION_ID
import com.android.pos.databinding.FragmentPrinterQueueBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.PrinterQueueListAdapter
import com.android.pos.ui.fragments.settings.hardware.printer.PrinterViewModel
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.SwipeHelper
import com.android.pos.utils.extensions.alert
import com.google.gson.Gson
import com.hosopy.actioncable.ActionCable
import com.hosopy.actioncable.Channel
import com.hosopy.actioncable.Consumer
import com.hosopy.actioncable.Subscription
import java.net.URI
import com.hosopy.actioncable.ActionCableException

import com.google.gson.JsonElement
import com.hosopy.actioncable.Subscription.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.google.gson.JsonObject


@AndroidEntryPoint
class PrinterQueue : Fragment() {
    private lateinit var binding: FragmentPrinterQueueBinding
    private val list: ArrayList<PrinterQueueModel> = arrayListOf()
    private lateinit var adapter: PrinterQueueListAdapter
    private val TAG = "PrinterQueue"

    @Inject
    lateinit var prefProvider: PrefProvider

    private val viewModel by viewModels<PrinterQueueViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPrinterQueueBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        observeShowProgress()
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
        // appearanceChannel.addParam("id",prefProvider.getValueInt(LOCATION_ID,0))
        val subscription: Subscription = consumer.subscriptions.create(appearanceChannel)

        subscription
            .onConnected {
                Log.e(TAG, "onActionConnected")
                val params = JsonObject()
                params.addProperty("id", prefProvider.getValueInt(LOCATION_ID, 0))
                subscription.perform("received", params)
            }.onRejected {
                Log.e(TAG, "onActiononRejected")
            }.onReceived {
                Log.e(TAG, "onActiononReceived  " + Gson().toJson(it))
            }.onDisconnected {
                Log.e(TAG, "onActiononDisconnected")
            }.onFailed {
                Log.e(TAG, "onActiononFailed")
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

        object : SwipeHelper(activity, binding.rvPrinterQueueList) {
            override fun instantiateUnderlayButton(
                viewHolder: RecyclerView.ViewHolder?,
                underlayButtons: MutableList<UnderlayButton>?
            ) {
                underlayButtons?.add(UnderlayButton("Delete", 0, Color.parseColor("#FF3C30")) {
                    Log.e(TAG, "position  ${it}")
                    deletePrinterQueue(adapter.getList().get(it).id)
                })
            }

        }

    }

    private fun observeShowProgress() {

        viewModel.showProgress.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        })
    }

    private fun deletePrinterQueue(id: Int) {
        alert(
            getString(R.string.tv_pos),
            getString(R.string.delete_printer_message)
        ) {
            positiveButton(getString(R.string.tv_delete)) {
                viewModel.deleteQueuePrinter(id)

            }
            negativeButton(R.string.tv_cancel) {

            }
        }
    }
}