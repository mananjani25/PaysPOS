package com.android.pos.ui.fragments.settings.hardware

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.SimpleAdapter
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.pos.MainApplication
import com.android.pos.data.model.PrinterListModel
import com.android.pos.data.remote.Constants.DISCOVERY_INTERVAL

import com.android.pos.data.remote.Constants.WIFI
import com.android.pos.databinding.FragmentPrinterBinding
import com.android.pos.ui.activities.MainActivity
import com.android.pos.ui.adapter.PrinterListAdapter
import com.epson.epos2.Epos2Exception
import com.epson.epos2.discovery.Discovery
import com.epson.epos2.discovery.DiscoveryListener
import com.epson.epos2.discovery.FilterOption
import com.epson.epsonio.*
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import java.lang.Exception
import java.util.ArrayList
import java.util.HashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import com.epson.epsonio.EpsonIoException


//Original New
@AndroidEntryPoint
class Printer : Fragment(), Runnable {
    private lateinit var binding: FragmentPrinterBinding

    //private var mFilterOption: FilterOption? = null
    private lateinit var customerAdapter: PrinterListAdapter
    private lateinit var kitchenAdapter: PrinterListAdapter
    var printerList: ArrayList<HashMap<String, String>> = arrayListOf()
    var printerListAdapter: SimpleAdapter? = null
    var scheduler: ScheduledExecutorService? = null
    var future: ScheduledFuture<*>? = null
    private var mFilterOption: com.epson.epos2.discovery.FilterOption? = null
    var handler = Handler()
    private val TAG = "Printer"
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPrinterBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        customerAdapter = PrinterListAdapter()



        printerList = ArrayList()

        //congigurePrinter()


        //getPrinterList()

        return binding.root
    }

    private fun getPrinterList() {

        //  Finder.start(requireContext(), DevType.BLUETOOTH, null)


    }

    override fun onPause() {
        super.onPause()
        /*try {
            Finder.stop()
//Exception handling
        } catch (e: EpsonIoException) {
            Log.e(TAG,"PrinterStopError:  ${e.status}")
        }*/
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        kitchenAdapter = PrinterListAdapter()
        binding.rvKitchenPrinter.adapter = kitchenAdapter
        mFilterOption = FilterOption()
        mFilterOption!!.setDeviceType(Discovery.TYPE_PRINTER)

        scheduler = Executors.newSingleThreadScheduledExecutor()

        /* try {
             Discovery.start(requireContext(), mFilterOption, mDiscoveryListener)
         } catch (e: Exception) {
             Log.e(TAG, "PrinterException:      ${e.message}")
             e.printStackTrace()
         }*/

        if (scheduler == null) {
            return
        }


        // stop old finder
       /* while (true) {
            try {
                Finder.stop()
                break
            } catch (e: EpsonIoException) {
                if (e.status != IoStatus.ERR_PROCESSING) {
                    break
                }
            }
        }
*/
        try {
            Finder.start(requireContext(), DevType.TCP, "255.255.255.255")

        } catch (e: EpsonIoException) {
            Log.e(TAG, "PrinterFinderError  ${e.status}")

        }


        // start thread
        future = scheduler!!.scheduleWithFixedDelay(
            this,
            0,
            DISCOVERY_INTERVAL.toLong(),
            TimeUnit.MILLISECONDS
        )
        //mFilterOption?.setEpsonFilter(Discovery.FILTER_NAME);
        /*
        try {
            Discovery.start(view.context, mFilterOption, mDiscoveryListener)
        } catch (e: Exception) {
            Log.e(TAG, "GetPinterNameFailed:  " + e.message)
            e.printStackTrace()
        }
        */
        // restartDiscovery()

        /*  try {
              com.epson.epos2.Log.setLogSettings(
                  requireContext(),
                  com.epson.epos2.Log.PERIOD_TEMPORARY,
                  com.epson.epos2.Log.OUTPUT_STORAGE,
                  "192.168.1.201",
                  220,
                  1,
                  com.epson.epos2.Log.LOGLEVEL_LOW
              )
          } catch (e: Exception) {
              Log.e(TAG, "ExceptionName:  ${e.message}")
              e.printStackTrace()

          }
          try {
              Discovery.start(view.context, mFilterOption, mDiscoveryListener)
          } catch (e: Exception) {
              Log.e(TAG, "GetPinterNameFailed:  " + e.message)
              e.printStackTrace()
          }*/

        binding.imgClose.setOnClickListener {
            findNavController().popBackStack()
        }


    }

    private fun congigurePrinter() {

        //init printer list control


        //start find thread scheduler


        //findStart()
        //restartDiscovery()
    }

    override fun onStop() {
        super.onStop()
        //stop find

        //stop find
        if (future != null) {
            future!!.cancel(false)
            while (!future!!.isDone) {
                try {
                    Thread.sleep(DISCOVERY_INTERVAL.toLong())
                } catch (e: Exception) {
                    break
                }
            }
            future = null
        }
        if (scheduler != null) {
            scheduler!!.shutdown()
            scheduler = null
        }
        //stop old finder
        //stop old finder
        while (true) {
            try {
                Finder.stop()
                break
            } catch (e: EpsonIoException) {
                if (e.status != IoStatus.ERR_PROCESSING) {
                    break
                }
            }
        }
    }

    /*private val mDiscoveryListener =
        DiscoveryListener { deviceInfo ->

            val item = HashMap<String, String>()
            item["PrinterName"] = deviceInfo.deviceName
            item["Target"] = deviceInfo.target
            Log.e(TAG, "Jsonitem:    ${Gson().toJson(item)}")

            val model: PrinterListModel = PrinterListModel()
            model.apply {
                printerName = deviceInfo.deviceName
                connectionType = WIFI

            }

            Log.e(TAG,"getModelmodel:  ${Gson().toJson(model)}")
            customerAdapter.addItem(model)
        }

*/
    private val mDiscoveryListener =
        DiscoveryListener { deviceInfo ->
            requireActivity().runOnUiThread(Runnable {
                val item = HashMap<String, String>()
                item["PrinterName"] = deviceInfo.deviceName
                item["Target"] = deviceInfo.target
                printerList.add(item)

            })
        }

    private fun restartDiscovery() {
        while (true) {
            try {
                Discovery.stop()
                break
            } catch (e: Epos2Exception) {
                if (e.errorStatus != Epos2Exception.ERR_PROCESSING) {
                    return
                } else {
                    Log.e(TAG, "StartPrinterStart")

                    Discovery.start(requireContext(), mFilterOption, mDiscoveryListener)
                }
            }
        }

        try {

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun findStart() {
        if (scheduler == null) {
            return
        }

        //stop old finder

        try {
            Finder.stop()

        } catch (e: EpsonIoException) {
            e.printStackTrace()
            if (e.status != IoStatus.ERR_PROCESSING) {

            }
        }


        //stop find thread
        if (future != null) {
            future!!.cancel(false)
            while (!future!!.isDone) {
                try {
                    Thread.sleep(DISCOVERY_INTERVAL.toLong())
                } catch (e: Exception) {
                    break
                }
            }
            future = null
        }

        //clear list
        printerList!!.clear()
        // printerListAdapter!!.notifyDataSetChanged()

        //get device type and find


        //start thread
        future = scheduler?.scheduleWithFixedDelay(
            this,
            0,
            DISCOVERY_INTERVAL.toLong(),
            TimeUnit.MILLISECONDS
        )
    }


    @SuppressLint("NotifyDataSetChanged")
    override fun run() {
        class UpdateListThread(var list: Array<DeviceInfo>?) :
            Thread() {
            override fun run() {
                if (list == null) {
                    if (printerList.size > 0) {
                        printerList.clear()
                        // printerListAdapter!!.notifyDataSetChanged()
                    }
                } else if (list!!.size != printerList.size) {
                    printerList.clear()
                    var name: String? = null
                    var address: String? = null
                    for (i in list!!.indices) {
                        name = list!![i].printerName
                        address = list!![i].deviceName
                        val item = HashMap<String, String>()
                        item["PrinterName"] = name
                        item["Address"] = address
                        printerList.add(item)
                    }

                    val listData:ArrayList<PrinterListModel> = arrayListOf()
                    for (i in 0 until  list!!.size){
                        listData.add(PrinterListModel(printerName = list!!.get(i).printerName,connectionType = WIFI,isActive = true))


                    }
                    kitchenAdapter.setList(listData)
                    kitchenAdapter.notifyDataSetChanged()



                    Log.e(TAG, "getprinterList:   ${Gson().toJson(printerList)}")
                    printerListAdapter?.notifyDataSetChanged()
                }
            }
        }

        var deviceList: Array<DeviceInfo>? = null
        try {
            deviceList = Finder.getDeviceInfoList(com.epson.epsonio.FilterOption.PARAM_DEFAULT)
            Log.e(TAG,"deviceList  ${Gson().toJson(deviceList)}")

            handler.post(UpdateListThread(deviceList))
        } catch (e: Exception) {
            return
        }

    }
}