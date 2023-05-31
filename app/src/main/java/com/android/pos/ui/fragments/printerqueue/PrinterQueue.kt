package com.android.pos.ui.fragments.printerqueue

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.pos.data.remote.Constants.LOCATION_ID
import com.android.pos.databinding.FragmentPrinterQueueListBinding
import com.android.pos.di.PrefProvider
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.hosopy.actioncable.ActionCable
import com.hosopy.actioncable.Channel
import com.hosopy.actioncable.Consumer
import com.hosopy.actioncable.Subscription
import dagger.hilt.android.AndroidEntryPoint
import java.net.URI
import javax.inject.Inject

@AndroidEntryPoint
class PrinterQueue : Fragment() {

    private lateinit var binding: FragmentPrinterQueueListBinding
    private var subscription: Subscription? = null
    private var consumer: Consumer? = null

    @Inject
    lateinit var prefProvider: PrefProvider

    private val TAG = "PrinterQueue"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPrinterQueueListBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.header.txtTitle.text = "Printer Queue"

        onClick()

        connectActionCable()
    }

    private fun connectActionCable() {
        val uri = URI("wss://hugepos.com/cable")
        consumer = ActionCable.createConsumer(uri)

        // 2. Create subscription
        val appearanceChannel = Channel("printer_queue_data")
        appearanceChannel.addParam("id", prefProvider.getValueInt(LOCATION_ID, 0))
        subscription = consumer?.subscriptions?.create(appearanceChannel)

        if (subscription != null) {
            subscription?.onConnected {
                Log.e(TAG,"onConnected")
                val params = JsonObject()
                params.addProperty("url", "get_orders_to_be_print_list")
                subscription?.perform("received", params)

            }?.onRejected { Log.e(TAG, "onRejected") }?.onReceived {
                Log.e(TAG, "getJsonElementData: ${Gson().toJson(it)}")

            }?.onDisconnected {
                Log.e(TAG, "onDisconnect")
            }?.onFailed {
                Log.e(TAG, "onFailed")
            }
        }

        consumer?.connect()

    }

    private fun onClick() {
        binding.header.txtSave.setOnClickListener {


        }

        binding.header.imgBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }
}