package com.android.pos.ui.fragments.printerqueue

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.LOCATION_ID
import com.android.pos.databinding.FragmentPrinterQueueListBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.boldpos.PrinterQueueList
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.runOnUiThread
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
    private val viewModel by viewModels<PrinterQueueViewModel>()

    private var requestURL: String = ""

    @Inject
    lateinit var prefProvider: PrefProvider

    private val TAG = "PrinterQueue"

    private lateinit var adapter: PrinterQueueList

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
        showObserver()
        requestURL = prefProvider?.getValue(
            Constants.BASE_URL_NEW,
            ""
        ) + "printer_queues/get_orders_to_be_print_list"

        setAdapter()

        binding.header.txtTitle.text = "Printer Queue"

        onClick()

        connectActionCable()
    }

    private fun showObserver() {
        viewModel.showProgress.observe(requireActivity()) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        }

        viewModel.snackbarText.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                AlertUtils.showCustomAlert(requireContext(), it)
                adapter.clearData()
            }
        }
    }

    private fun setAdapter() {
        adapter = PrinterQueueList()
        binding.rvPrinterQueueList.adapter = adapter
        binding.rvPrinterQueueList.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        //   binding.rvPrinterQueueList.addItemDecoration(DividerItemDecoration(requireContext(),DividerItemDecoration.VERTICAL))
    }

    override fun onPause() {
        consumer?.disconnect()
        super.onPause()


    }

    private fun connectActionCable() {
        val uri = URI(Constants.PRINTER_QUEUE_CONNECTION_URL_SNACKPOS)
        consumer = ActionCable.createConsumer(uri)

        // 2. Create subscription
        val appearanceChannel = Channel("PrinterQueueDataChannel")
        appearanceChannel.addParam("id", prefProvider.getValueInt(LOCATION_ID, 0))
        subscription = consumer?.subscriptions?.create(appearanceChannel)

        if (subscription != null) {
            subscription?.onConnected {
                Log.e(TAG, "onConnected: " + requestURL)

                val params = JsonObject()
                params.addProperty("url", requestURL)
                params.addProperty("id", prefProvider.getValueInt(LOCATION_ID, 0))
                subscription?.perform("received", params)

            }?.onRejected { Log.e(TAG, "onRejected") }?.onReceived {
                Log.e(TAG, "getJsonElementData: ${Gson().toJson(it)}")
                if (it != null) {
                    if (it.asJsonObject.has("printer_queue_data")) {

                        if (it.asJsonObject.get("printer_queue_data").asJsonObject.has("data")) {
                            var data =
                                it.asJsonObject.get("printer_queue_data").asJsonObject.get("data").asJsonArray

                            if (data.size() != 0) {
                                var listPrinterQueue: ArrayList<com.android.pos.data.model.PrinterQueueList> =
                                    arrayListOf()
                                data.forEach {
                                    listPrinterQueue.add(
                                        com.android.pos.data.model.PrinterQueueList(
                                            id = 0,
                                            printerName = it.asJsonObject.get("printer_name").asString,
                                            orderIds = it.asJsonObject.get("order_ids").asString,
                                            totalCount = it.asJsonObject.get("orders_count").asInt
                                        )
                                    )
                                }
                                runOnUiThread(Runnable {
                                    Log.e(TAG,"checkRunUITHREAD")

                                    adapter.addData(listPrinterQueue)
                                })

                            } else {
                                runOnUiThread({
                                    adapter.clearData()

                                })
                            }


                        }


                    }
                }


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
            findNavController().navigate(R.id.action_printer_queue_to_dashboardCategoryNew)
        }

        binding.header.imgBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.txtClearPrinterQueue.setOnClickListener {
            viewModel.clearPrinterQueue()

        }
    }
}