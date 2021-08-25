package com.android.pos.ui.fragments.settings.hardware

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SimpleAdapter
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.pos.data.model.PrinterListModel
import com.android.pos.data.remote.Constants.DISCOVERY_INTERVAL

import com.android.pos.data.remote.Constants.WIFI
import com.android.pos.databinding.FragmentPrinterBinding
import com.android.pos.ui.adapter.PrinterListAdapter
import com.epson.epos2.Epos2Exception
import com.epson.epos2.discovery.Discovery
import com.epson.epos2.discovery.DiscoveryListener
import com.epson.epos2.discovery.FilterOption
import com.epson.epsonio.*
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import java.lang.Exception
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import com.epson.epsonio.EpsonIoException
import android.bluetooth.BluetoothDevice

import android.bluetooth.BluetoothAdapter

import android.content.Intent
import java.util.*
import android.bluetooth.BluetoothSocket
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.pos.utils.printer.PrinterClass
import com.android.pos.utils.printer.PrinterClass.language
import com.epson.eposprint.BatteryStatusChangeEventListener
import com.epson.eposprint.Builder
import com.epson.eposprint.Print
import com.epson.eposprint.StatusChangeEventListener
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URLDecoder
import kotlin.collections.ArrayList


//Original New
@AndroidEntryPoint
class Printer : Fragment(), Runnable, PrinterListAdapter.PrinterListInterface,
    StatusChangeEventListener, BatteryStatusChangeEventListener {
    private lateinit var binding: FragmentPrinterBinding
    var mBluetoothAdapter: BluetoothAdapter? = null
    var deviceList: Array<DeviceInfo>? = null
    var kitchenPrintList: ArrayList<PrinterListModel> = arrayListOf()
    var customerPrintList: ArrayList<PrinterListModel> = arrayListOf()


    //private var mFilterOption: FilterOption? = null
    private lateinit var customerAdapter: PrinterListAdapter
    private lateinit var kitchenAdapter: PrinterListAdapter
    private lateinit var availableNetworkAdapter: PrinterListAdapter
    var printerList: ArrayList<HashMap<String, String>> = arrayListOf()
    var printerListAdapter: SimpleAdapter? = null
    var scheduler: ScheduledExecutorService? = null
    var future: ScheduledFuture<*>? = null
    private var mFilterOption: com.epson.epos2.discovery.FilterOption? = null
    var handler = Handler()
    var mmSocket: BluetoothSocket? = null
    var mmDevice: BluetoothDevice? = null

    // needed for communication to bluetooth device / network
    var mmOutputStream: OutputStream? = null
    var mmInputStream: InputStream? = null
    var workerThread: Thread? = null

    lateinit var readBuffer: ByteArray
    var readBufferPosition = 0

    @Volatile
    var stopWorker = false
    private val TAG = "Printer"
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPrinterBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        printerList = ArrayList()

        return binding.root
    }

    private fun getPrinterList() {

        //  Finder.start(requireContext(), DevType.BLUETOOTH, null)


    }

    private fun startFinder() {
        scheduler = Executors.newSingleThreadScheduledExecutor()
        if (scheduler == null) {
            return
        }

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

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        kitchenAdapter = PrinterListAdapter()
        kitchenAdapter.setList(kitchenPrintList)
        kitchenAdapter.setListner(this)
        customerAdapter = PrinterListAdapter()
        customerAdapter.setList(customerPrintList)
        customerAdapter.setListner(this)
        availableNetworkAdapter = PrinterListAdapter()
        availableNetworkAdapter.setListner(this)
        binding.rvAvailablePrinter.adapter = availableNetworkAdapter
        binding.rvAvailablePrinter.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                LinearLayoutManager.VERTICAL
            )
        )
        binding.rvKitchenPrinter.adapter = kitchenAdapter
        binding.rvKitchenPrinter.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                LinearLayoutManager.VERTICAL
            )
        )
        binding.rvCustomerPrinter.adapter = customerAdapter
        binding.rvCustomerPrinter.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                LinearLayoutManager.VERTICAL
            )
        )

        mFilterOption = FilterOption()
        mFilterOption!!.setDeviceType(Discovery.TYPE_PRINTER)
        startFinder()


        /* try {
             Discovery.start(requireContext(), mFilterOption, mDiscoveryListener)
         } catch (e: Exception) {
             Log.e(TAG, "PrinterException:      ${e.message}")
             e.printStackTrace()
         }*/


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
            // openBT()
            //findBT()
        } catch (ex: IOException) {
            ex.printStackTrace()
        }


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

        binding.imgSync.setOnClickListener {
            startFinder()

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
        stopFinder()
        PrinterClass.closePrinter()


    }

    fun stopFinder() {
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
        scheduler?.schedule(this, 0, TimeUnit.MILLISECONDS)
        /*future = scheduler?.scheduleWithFixedDelay(
            this,
            0,
            DISCOVERY_INTERVAL.toLong(),
            TimeUnit.MILLISECONDS
        )*/
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

                    val listData: ArrayList<PrinterListModel> = arrayListOf()
                    for (i in 0 until list!!.size) {
                        listData.add(
                            PrinterListModel(
                                printerName = list!!.get(i).printerName,
                                connectionType = WIFI,
                                isActive = false,
                                deviceModel = list!!.get(i)
                            )
                        )


                    }
                    availableNetworkAdapter.setList(listData)


                }
            }
        }


        try {
            deviceList = Finder.getDeviceInfoList(com.epson.epsonio.FilterOption.PARAM_DEFAULT)
            Log.e(TAG, "deviceList  ${Gson().toJson(deviceList)}")


            handler.post(UpdateListThread(deviceList))

            if (deviceList == null || deviceList?.size == 0) {

            } else {
                stopFinder()
            }


        } catch (e: Exception) {
            return
        }

    }

    @Throws(IOException::class)
    fun openBT() {
        try {

            // Standard SerialPortService ID
            val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
            mmSocket = mmDevice?.createRfcommSocketToServiceRecord(uuid)
            mmSocket?.connect()
            mmOutputStream = mmSocket?.getOutputStream()
            mmInputStream = mmSocket?.getInputStream()
            beginListenForData()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // this will find a bluetooth printer device
    fun findBT() {
        try {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (mBluetoothAdapter == null) {

            }
            if (!mBluetoothAdapter!!.isEnabled()) {
                val enableBluetooth = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBluetooth, 0)
            }
            val pairedDevices: Set<BluetoothDevice> = mBluetoothAdapter!!.getBondedDevices()
            Log.e(TAG, "pairedDevices:   ${Gson().toJson(pairedDevices)}")
            if (pairedDevices.size > 0) {
                for (device in pairedDevices) {

                    // RPP300 is the name of the bluetooth printer device
                    // we got this name from the list of paired devices
                    if (device.name == "RPP300") {
                        mmDevice = device

                        break
                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /*
 * after opening a connection to bluetooth printer device,
 * we have to listen and check if a data were sent to be printed.
 */
    fun beginListenForData() {
        try {
            val handler = Handler()

            // this is the ASCII code for a newline character
            val delimiter: Byte = 10
            stopWorker = false
            readBufferPosition = 0
            readBuffer = ByteArray(1024)
            workerThread = Thread {
                while (!Thread.currentThread().isInterrupted && !stopWorker) {
                    try {
                        val bytesAvailable = mmInputStream?.available()
                        if (bytesAvailable != null) {
                            if (bytesAvailable > 0) {
                                val packetBytes = ByteArray(bytesAvailable)
                                mmInputStream!!.read(packetBytes)
                                for (i in 0 until bytesAvailable) {
                                    val b = packetBytes[i]
                                    if (b == delimiter) {
                                        val encodedBytes = ByteArray(readBufferPosition)
                                        System.arraycopy(
                                            readBuffer, 0,
                                            encodedBytes, 0,
                                            encodedBytes.size
                                        )

                                        // specify US-ASCII encoding
                                        val data =
                                            URLDecoder.decode(encodedBytes.toString(), "UTF-8")
                                        readBufferPosition = 0

                                        // tell the user data were sent to bluetooth printer device
                                        handler.post { }
                                    } else {
                                        readBuffer[readBufferPosition++] = b
                                    }
                                }
                            }
                        }
                    } catch (ex: IOException) {
                        stopWorker = true
                    }
                }
            }
            workerThread!!.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPrinterSelected(printerListModel: PrinterListModel) {
        Log.e(TAG, "printerListModel:  ${Gson().toJson(printerListModel)}")
        onInitPrinter(printerListModel)
    }

    override fun onPrinterActive(printerListModel: PrinterListModel) {
        Log.e(TAG, "printerListModel:  ${Gson().toJson(printerListModel)}")
        printerListModel.isActive = true
        if (printerListModel.printerName == "TM-U220") {
            kitchenAdapter.addItem(printerListModel)
        } else {
            customerAdapter.addItem(printerListModel)
        }

    }

    private fun onInitPrinter(printerListModel: PrinterListModel) {
        //open

        initPrinter(printerListModel)


    }

    private fun initPrinter(printerListModel: PrinterListModel) {

        var printer: Print? = Print(requireContext())
        if (printer != null) {
            printer.setStatusChangeEventCallback(this)
            printer.setBatteryStatusChangeEventCallback(this)
        }

        val enabled = Print.TRUE
        Log.e(TAG, "MacAddress:  ${printerListModel.deviceModel.macAddress}")
        Log.e(TAG, "IPAddress:  ${printerListModel.deviceModel.ipAddress}")

        try {

            printer?.openPrinter(
                Print.DEVTYPE_TCP,
                printerListModel.deviceModel.macAddress,
                enabled,
                1000
            )

        } catch (e: Exception) {
            Log.e(TAG, "Exception:  " + e.message)
            printer = null
            return
        }
        PrinterClass.setPrinter(printer)


        showPrinterStatus(printerListModel)


    }

    private fun showPrinterStatus(printerListModel: PrinterListModel) {
        var builder: Builder? = null
        var method = ""
        try {
            method = "Builder"
            builder = Builder(printerListModel.printerName, language, requireActivity())


            //send builder data(empty builder data)
            val status = IntArray(1)
            val battery = IntArray(1)

            Log.e(TAG, "getPrinterCheck:  ${PrinterClass.getPrinter().toString()}")

            try {
                PrinterClass.getPrinter()?.sendData(builder,0,status,battery)
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG,"PrinterError: "+e.message)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStatusChangeEvent(p0: String?, p1: Int) {
        Log.e(TAG, "onStatusChanged  ${p0}")

    }

    override fun onBatteryStatusChangeEvent(p0: String?, p1: Int) {
        Log.e(TAG, "onBatteryLevelChange  ${p0}")
    }


}