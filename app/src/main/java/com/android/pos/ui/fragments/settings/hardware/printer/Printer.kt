package com.android.pos.ui.fragments.settings.hardware.printer

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import android.os.Build
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.pos.data.remote.Constants.BLUETOOTH
import com.android.pos.utils.printer.PrinterClass
import com.android.pos.utils.printer.PrinterClass.SEND_TIMEOUT
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

import android.text.Html
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.viewModels

import com.android.pos.R
import com.android.pos.data.entities.TbOrderType
import com.android.pos.data.model.requestModel.CreatePrinterRequestModel
import com.android.pos.data.remote.Constants.AVAILABLE
import com.android.pos.data.remote.Constants.CUSTOMER
import com.android.pos.data.remote.Constants.KITCHEN
import com.android.pos.data.remote.Constants.LOCATION_ID
import com.android.pos.data.remote.Constants.TERMINAL_ID
import com.android.pos.di.PrefProvider
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.addHorizontalKitchenLine
import com.android.pos.utils.extensions.alert
import com.android.pos.utils.getBitmapFromVectorDrawable


import com.android.pos.utils.padLine
import com.android.pos.utils.printer.PrinterClass.IMAGE_WIDTH_MAX
import com.android.pos.utils.statusUtils.Status
import javax.inject.Inject


//Original New
@AndroidEntryPoint
class Printer : Fragment(), Runnable, PrinterListAdapter.PrinterListInterface,
    StatusChangeEventListener, BatteryStatusChangeEventListener {
    private lateinit var binding: FragmentPrinterBinding
    var mBluetoothAdapter: BluetoothAdapter? = null
    var deviceList: Array<DeviceInfo>? = null
    var kitchenPrintList: ArrayList<PrinterListModel> = arrayListOf()
    var customerPrintList: ArrayList<PrinterListModel> = arrayListOf()
    var availablePrinterList: ArrayList<PrinterListModel> = arrayListOf()
    var orderTypeList: ArrayList<TbOrderType> = arrayListOf()

    //private var mFilterOption: FilterOption? = null
    private lateinit var customerAdapter: PrinterListAdapter
    private lateinit var kitchenAdapter: PrinterListAdapter
    private lateinit var availableNetworkAdapter: PrinterListAdapter
    var printerList: ArrayList<HashMap<String, String>> = arrayListOf()

    var allPrinterlist: ArrayList<PrinterListModel> = arrayListOf()

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


    private val viewModel by viewModels<PrinterViewModel>()

    lateinit var readBuffer: ByteArray
    var readBufferPosition = 0

    @Inject
    lateinit var prefProvider: PrefProvider

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

        getOrderTypes()


        return binding.root
    }

    private fun getOrderTypes() {
        viewModel.orderTypes.observe(viewLifecycleOwner, {
            when (it.status) {
                Status.LOADING -> {
                    ProgressUtils.showProgressDialog(requireActivity())
                }
                Status.ERROR -> {
                    ProgressUtils.dismissProgressDialog()
                }
                Status.SUCCESS -> {
                    ProgressUtils.dismissProgressDialog()
                    Log.e(TAG, "OrderTypesList:  ${Gson().toJson(it.data)}")

                    if (it.data != null) {
                        orderTypeList.addAll(it.data.toCollection(ArrayList()))

                    }


                }
            }
        })
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
        future = scheduler!!.schedule(this, 0, TimeUnit.MILLISECONDS)
        /* future = scheduler!!.scheduleWithFixedDelay(
             this,
             0,
             DISCOVERY_INTERVAL.toLong(),
             TimeUnit.MILLISECONDS
         )*/

    }

    override fun onPause() {
        super.onPause()
        PrinterClass.closePrinter()
        stopFinder()

        closeBT()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeShowProgress()
        onDeleteObserve()
        kitchenAdapter = PrinterListAdapter()
        kitchenAdapter.setList(kitchenPrintList)
        kitchenAdapter.setListner(this)
        customerAdapter = PrinterListAdapter()
        customerAdapter.setList(customerPrintList)
        customerAdapter.setListner(this)
        availableNetworkAdapter = PrinterListAdapter()
        availableNetworkAdapter.setListner(this)
        availableNetworkAdapter.setList(arrayListOf())
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
            // searchBluetooth()
            //findBT()
            //openBT()
            /*val adapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            adapter.startDiscovery()

            val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
            //filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            requireContext().registerReceiver(mReceiver, filter)
*/
            //searchBluetooth()
            //
        } catch (ex: IOException) {
            ex.printStackTrace()
        }

        syncPrinterList()

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
            availableNetworkAdapter.clearList()

            searchBluetooth()
            try {
                stopFinder()
                startFinder()

            } catch (e: Exception) {
                e.printStackTrace()
            }


        }


    }

    private fun onDeleteObserve() {
        viewModel.deletePrinter.observe(viewLifecycleOwner, {
            it.getContentIfNotHandled()?.let { data ->
                Log.e(TAG, "deleteSuccess")
                syncPrinterList()

            }
        })
    }

    private fun syncPrinterList() {
        allPrinterlist.clear()
        viewModel.printerList().observe(viewLifecycleOwner, {
            when (it.status) {

                Status.SUCCESS -> {
                    Log.e(TAG, "SyncPrinterList")
                    ProgressUtils.dismissProgressDialog()

                    val data = it.data


                    Log.e(TAG, "PrinterREsponseData:  ${Gson().toJson(data)}")

                    if (it.data != null) {
                        if (data != null) {
                            val customerData = data
                            customerAdapter.clearList()
                            customerAdapter.setList(arrayListOf())


                            if (customerData != null) {
                                for (i in customerData.indices) {
                                    customerAdapter.addItem(
                                        PrinterListModel(
                                            id = customerData[i].id,
                                            printerName = customerData[i].name,
                                            connectionType = if (customerData[i].printer_type == BLUETOOTH) {
                                                BLUETOOTH
                                            } else {
                                                WIFI
                                            },
                                            isActive = customerData[i].status,
                                            type = customerData[i].receiptPrintType,
                                            DeviceInfo(
                                                if (customerData[i].printer_type == BLUETOOTH) {
                                                    DevType.BLUETOOTH
                                                } else {
                                                    DevType.TCP
                                                },
                                                customerData[i].ipAddress,
                                                customerData[i].name,
                                                customerData[i].ipAddress,
                                                customerData[i].macAddress
                                            ),
                                            printerModel = customerData[i].orderTypes


                                        )
                                    )

                                    allPrinterlist.add(
                                        PrinterListModel(
                                            id = customerData[i].id,
                                            printerName = customerData[i].name,
                                            connectionType = if (customerData[i].printer_type == BLUETOOTH) {
                                                BLUETOOTH
                                            } else {
                                                WIFI
                                            },
                                            isActive = customerData[i].status,
                                            type = customerData[i].receiptPrintType,
                                            DeviceInfo(
                                                if (customerData[i].printer_type == BLUETOOTH) {
                                                    DevType.BLUETOOTH
                                                } else {
                                                    DevType.TCP
                                                },
                                                customerData[i].ipAddress,
                                                customerData[i].name,
                                                customerData[i].ipAddress,
                                                customerData[i].macAddress
                                            ),
                                            printerModel = customerData[i].orderTypes


                                        )

                                    )

                                }
                            }
                        } else {
                            Log.e(TAG, "CustomerListCleared ")
                            customerAdapter.clearList()
                        }


                        /* if (data?.kitchenReceiptPrinters != null) {
                             val kitchenData = data.kitchenReceiptPrinters
                             kitchenAdapter.clearList()
                             kitchenAdapter.setList(arrayListOf())
                             if (kitchenData != null) {
                                 for (i in kitchenData.indices) {
                                     kitchenAdapter.addItem(
                                         PrinterListModel(
                                             id = kitchenData[i].id,
                                             printerName = kitchenData[i].name,
                                             connectionType = if (kitchenData[i].printer_type == BLUETOOTH) {
                                                 BLUETOOTH
                                             } else {
                                                 WIFI
                                             },
                                             isActive = kitchenData[i].status,
                                             type = kitchenData[i].receiptPrintType,
                                             DeviceInfo(
                                                 if (kitchenData[i].printer_type == BLUETOOTH) {
                                                     DevType.BLUETOOTH
                                                 } else {
                                                     DevType.TCP
                                                 },
                                                 kitchenData[i].ipAddress,
                                                 kitchenData[i].name,
                                                 kitchenData[i].ipAddress,
                                                 kitchenData[i].macAddress
                                             ),
                                             printerModel = kitchenData[i].orderTypes


                                         )
                                     )

                                     allPrinterlist.add(
                                         PrinterListModel(

                                             id = kitchenData[i].id,
                                             printerName = kitchenData[i].name,
                                             connectionType = if (kitchenData[i].printer_type == BLUETOOTH) {
                                                 BLUETOOTH
                                             } else {
                                                 WIFI
                                             },
                                             isActive = kitchenData[i].status,
                                             type = kitchenData[i].receiptPrintType,
                                             DeviceInfo(
                                                 if (kitchenData[i].printer_type == BLUETOOTH) {
                                                     DevType.BLUETOOTH
                                                 } else {
                                                     DevType.TCP
                                                 },
                                                 kitchenData[i].ipAddress,
                                                 kitchenData[i].name,
                                                 kitchenData[i].ipAddress,
                                                 kitchenData[i].macAddress
                                             ),
                                             printerModel = kitchenData[i].orderTypes


                                         )
                                     )
                                 }

                             }

                         } else {
                             Log.e(TAG, "CustomerListCleared 2")
                             kitchenAdapter.clearList()
                         }
                     */

                    } else {
                        Log.e(TAG, "ITNotNull  ")
                        kitchenAdapter.clearList()

                        customerAdapter.clearList()
                    }

                    /*if (data?.kitchenReceiptPrinters?.isEmpty() == true) {
                        kitchenAdapter.clearList()
                    }
*/

                    if (data?.isEmpty() == true) {
                        customerAdapter.clearList()
                    }
                    availableNetworkAdapter.clearList()
                    searchBluetooth()
                    startFinder()


                }
                Status.ERROR -> {
                    Log.e(TAG, "PrinterError ")
                    ProgressUtils.dismissProgressDialog()
                    availableNetworkAdapter.clearList()
                    searchBluetooth()
                    startFinder()
                }
                Status.LOADING -> {
                    ProgressUtils.showProgressDialog(requireActivity())
                }
            }

        })

        viewModel.getKitchenPrinters().observe(viewLifecycleOwner, {
            when (it.status) {

                Status.SUCCESS -> {
                    Log.e(TAG, "SyncPrinterList")
                    ProgressUtils.dismissProgressDialog()
                    val data = it.data


                    Log.e(TAG, "PrinterREsponseData:  ${Gson().toJson(data)}")

                    if (it.data != null) {
                        if (data != null) {
                            val kitchenData = data
                            kitchenAdapter.clearList()
                            kitchenAdapter.setList(arrayListOf())
                            if (kitchenData != null) {
                                for (i in kitchenData.indices) {
                                    kitchenAdapter.addItem(
                                        PrinterListModel(
                                            id = kitchenData[i].id,
                                            printerName = kitchenData[i].name,
                                            connectionType = if (kitchenData[i].printer_type == BLUETOOTH) {
                                                BLUETOOTH
                                            } else {
                                                WIFI
                                            },
                                            isActive = kitchenData[i].status,
                                            type = kitchenData[i].receiptPrintType,
                                            DeviceInfo(
                                                if (kitchenData[i].printer_type == BLUETOOTH) {
                                                    DevType.BLUETOOTH
                                                } else {
                                                    DevType.TCP
                                                },
                                                kitchenData[i].ipAddress,
                                                kitchenData[i].name,
                                                kitchenData[i].ipAddress,
                                                kitchenData[i].macAddress
                                            ),
                                            printerModel = kitchenData[i].orderTypes


                                        )
                                    )

                                    allPrinterlist.add(
                                        PrinterListModel(

                                            id = kitchenData[i].id,
                                            printerName = kitchenData[i].name,
                                            connectionType = if (kitchenData[i].printer_type == BLUETOOTH) {
                                                BLUETOOTH
                                            } else {
                                                WIFI
                                            },
                                            isActive = kitchenData[i].status,
                                            type = kitchenData[i].receiptPrintType,
                                            DeviceInfo(
                                                if (kitchenData[i].printer_type == BLUETOOTH) {
                                                    DevType.BLUETOOTH
                                                } else {
                                                    DevType.TCP
                                                },
                                                kitchenData[i].ipAddress,
                                                kitchenData[i].name,
                                                kitchenData[i].ipAddress,
                                                kitchenData[i].macAddress
                                            ),
                                            printerModel = kitchenData[i].orderTypes


                                        )
                                    )
                                }

                            }

                        } else {
                            Log.e(TAG, "CustomerListCleared 2")
                            kitchenAdapter.clearList()
                        }


                    } else {
                        Log.e(TAG, "ITNotNull  ")
                        kitchenAdapter.clearList()


                    }

                    /*if (data?.kitchenReceiptPrinters?.isEmpty() == true) {
                        kitchenAdapter.clearList()
                    }
*/

                    if (data?.isEmpty() == true) {
                        kitchenAdapter.clearList()
                    }
                    availableNetworkAdapter.clearList()
                    searchBluetooth()
                    startFinder()


                }
                Status.ERROR -> {
                    Log.e(TAG, "PrinterError ")
                    ProgressUtils.dismissProgressDialog()
                    availableNetworkAdapter.clearList()
                    searchBluetooth()
                    startFinder()
                }
                Status.LOADING -> {
                    ProgressUtils.showProgressDialog(requireActivity())
                }
            }

        })


    }

    private fun searchBluetooth() {

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mBluetoothAdapter!!.isEnabled) {

            val availableDevices: Set<BluetoothDevice> = mBluetoothAdapter!!.bondedDevices

            for (i in availableDevices) {

                var isAdded: Boolean = false
                for (j in 0 until allPrinterlist.size) {
                    if (allPrinterlist.get(j).deviceModel?.macAddress == i.address) {
                        isAdded = true
                        break
                    } else {
                        isAdded = false

                    }

                }

                if (!isAdded) {
                    Log.e(TAG, "BluetoothPrinterName:  ${i.name}")
                    Log.e(TAG, "BluetoothPrintertype:  ${i.type}")
                    Log.e(TAG, "BluetoothPrinterbondState:  ${i.bondState}")
                    Log.e(TAG, "BluetoothPrinterdeviceClass:  ${i.bluetoothClass.deviceClass}")
                    Log.e(
                        TAG,
                        "BluetoothPrintermajorDeviceClass:  ${i.bluetoothClass.majorDeviceClass}"
                    )
                    Log.e(TAG, "BluetoothPrinteraddress:  ${i.address}")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Log.e(TAG, "BluetoothPrinteralias:  ${i.alias}")
                    }

                    availableNetworkAdapter.addItem(
                        PrinterListModel(
                            printerName = i.name,
                            connectionType = BLUETOOTH,
                            deviceModel = DeviceInfo(
                                DevType.BLUETOOTH,
                                i.address,
                                i.name,
                                i.address,
                                i.address
                            ),
                            type = AVAILABLE,
                            uuid = UUID.randomUUID()

                        )
                    )
                }
                if (i.name == "TM-m30_030295") {
                    mmDevice = i
                }


            }
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
        //  stopFinder()
        PrinterClass.closePrinter()
        closeBT()


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
        // scheduler?.schedule(this, 0, TimeUnit.MILLISECONDS)
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

                    /* for (i in 0 until list!!.size) {
                         availableNetworkAdapter.addItem(
                             PrinterListModel(
                                 printerName = list!![i].printerName,
                                 connectionType = WIFI,
                                 isActive = false,
                                 type = AVAILABLE,
                                 deviceModel = list!!.get(i),

                             )
                         )
                     }*/


                }
            }
        }



        try {
            deviceList = Finder.getDeviceInfoList(com.epson.epsonio.FilterOption.PARAM_DEFAULT)
            Log.e(TAG, "deviceList  ${Gson().toJson(deviceList)}")

            if (deviceList != null) {
                for (i in 0 until deviceList!!.size) {

                    var isAdded: Boolean = false
                    for (j in 0 until allPrinterlist.size) {
                        Log.e(TAG, "DeviceAddress : ${deviceList!!.get(i).macAddress}")
                        Log.e(
                            TAG,
                            "DeviceModelAddress:  ${allPrinterlist.get(j).deviceModel?.macAddress}"
                        )
                        if (allPrinterlist.get(j).deviceModel?.macAddress == deviceList!!.get(i).macAddress) {
                            isAdded = true
                            break
                        } else {
                            isAdded = false


                        }


                    }
                    if (!isAdded) {
                        availableNetworkAdapter.addItem(
                            PrinterListModel(
                                printerName = deviceList!!.get(i).printerName,
                                connectionType = WIFI,
                                deviceModel = DeviceInfo(
                                    DevType.TCP,
                                    deviceList!!.get(i).printerName,
                                    deviceList!!.get(i).deviceName,
                                    deviceList!!.get(i).ipAddress,
                                    deviceList!!.get(i).macAddress
                                ),
                                type = AVAILABLE,


                                )
                        )
                    }

                    Log.e(TAG, "DeviceisAdded:  ${isAdded}")


                }

            }


            handler.post(UpdateListThread(deviceList))
            future?.cancel(false)
            //stopFinder()


            /* if (deviceList?.isNotEmpty() == true) {
                 stopFinder()
             }*/

            /* if (deviceList?.isEmpty() == true) {
                 handler.post(UpdateListThread(deviceList))
             }*/

            /*if (deviceList != null) {
                stopFinder()
            } else {
                handler.post(UpdateListThread(deviceList))
                //startFinder()
            }*/
            // stopFinder()


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
            sendBTData()

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
                this.startActivityForResult(enableBluetooth, 100)
            }
            val pairedDevices: Set<BluetoothDevice> = mBluetoothAdapter!!.getBondedDevices()
            Log.e(TAG, "pairedDevices:   ${Gson().toJson(pairedDevices)}")
            if (pairedDevices.size > 0) {
                for (device in pairedDevices) {

                    // RPP300 is the name of the bluetooth printer device
                    // we got this name from the list of paired devices
                    if (device.name == "TM-m30_03029") {
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
        onInitPrinter(printerListModel)
    }

    override fun onPrinterActive(printerListModel: PrinterListModel) {
        Log.e(TAG, "printerListModel: ${Gson().toJson(printerListModel)}")

        var list: ArrayList<CreatePrinterRequestModel.PrinterSettingsAttributes> = arrayListOf()
        for (i in 0 until orderTypeList.size) {
            list.add(
                CreatePrinterRequestModel.PrinterSettingsAttributes(
                    printType = CUSTOMER,
                    orderTypeId = orderTypeList.get(i).id
                )
            )
            list.add(
                CreatePrinterRequestModel.PrinterSettingsAttributes(
                    printType = KITCHEN,
                    orderTypeId = orderTypeList.get(i).id
                )
            )
        }


        val createPrinter = CreatePrinterRequestModel(
            name = printerListModel.printerName,
            macAddress = printerListModel.deviceModel?.macAddress,
            modalName = printerListModel.deviceModel?.printerName,
            terminalIds = listOf(prefProvider.getValueInt(TERMINAL_ID, 1)),
            status = false,
            locationId = prefProvider.getValueInt(LOCATION_ID, 1),
            receiptPrintType = if (printerListModel.printerName == "TM-U220" ) {
                KITCHEN
            } else {
                CUSTOMER
            },
            printer_type = printerListModel.connectionType,
            ip_address = printerListModel.deviceModel?.ipAddress,
            printerSettingsAttributes = list
        )
        Log.e(TAG, "createPrinterRequestParam:  ${Gson().toJson(createPrinter)}")
        viewModel.createPrinter(createPrinter)
        syncPrinterList()


        /* if (printerListModel.printerName == "TM-U220") {
             printerListModel.type = KITCHEN
             kitchenAdapter.addItem(printerListModel)
         } else {
             printerListModel.type = CUSTOMER
             customerAdapter.addItem(printerListModel)
         }*/

    }


    override fun onEditSelected(printerListModel: PrinterListModel) {

        val bundle = Bundle()
        bundle.putParcelable("printerSetting", printerListModel)

        findNavController().navigate(R.id.action_printer_to_editPrinter, bundle)
    }

    override fun onDeletePrinter(printerListModel: PrinterListModel) {

        deletePrinter(printerListModel.id!!)


    }

    override fun onUpdatePrinterStatus(printerListModel: PrinterListModel, isChecked: Boolean) {

        viewModel.updatePrinterStatus(
            printerListModel.type,
            printerListModel.id!!,
            prefProvider.getValueInt(TERMINAL_ID, 1),
            isChecked
        )
    }

    private fun onInitPrinter(printerListModel: PrinterListModel) {
        //open
        initPrinter(printerListModel)

    }

    private fun initPrinter(printerListModel: PrinterListModel) {
        PrinterClass.setPrinter(null)

        var printer: Print? = Print(requireContext())
        if (printer != null) {
            printer.setStatusChangeEventCallback(this)
            printer.setBatteryStatusChangeEventCallback(this)
        }

        val enabled = Print.FALSE
        Log.e(TAG, "PrinterconnectionType:  ${printerListModel.connectionType}")

        if (printerListModel.connectionType == "") {
            findBT()
            openBT()
        } else {
            try {
                printer?.openPrinter(
                    if (printerListModel.connectionType == BLUETOOTH) Print.DEVTYPE_BLUETOOTH else Print.DEVTYPE_TCP,
                    printerListModel.deviceModel?.ipAddress,
                    enabled,
                    1000
                )
                printer?.setStatusChangeEventCallback(this)


            } catch (e: Exception) {
                Log.e(TAG, "Exception:  " + e.message)
                printer = null
                return
            }
            PrinterClass.setPrinter(printer)
            Log.e(TAG, "printerListModel:  ${Gson().toJson(printerListModel)}")

            generateKitchenReceipt(printerListModel)
            //showPrinterStatus(printerListModel)
        }
    }

    private fun generateKitchenReceipt(printerListModel: PrinterListModel) {
        var builder: Builder? = null
        var method = ""

        try {
            Log.e(TAG, "printerName: ${printerListModel.printerName}")
            builder = Builder("TM-m30", language, requireActivity())


            builder.addTextFont(Builder.FONT_E)
            builder.addTextAlign(Builder.ALIGN_CENTER)
            //builder.addTextLineSpace(20)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(2, 2)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )
            //builder.addTextPosition(1)


            builder.addText("DINE IN")


            builder.addFeedLine(2)
            builder.addTextFont(Builder.FONT_B)
            //builder.addTextLineSpace(20)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 1)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )
            //builder.addTextPosition(1)


            builder.addText(padLine("OrderID:23564", "ReceiptID:REC54646", 40))

            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)

            builder.addTextFont(Builder.FONT_B)
            //builder.addTextLineSpace(20)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 1)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )
            //builder.addTextPosition(1)


            builder.addText(padLine("Employee: David Miller", "", 40))

            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)

            builder.addTextFont(Builder.FONT_B)
            //builder.addTextLineSpace(20)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 1)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )

            builder.addText(padLine("29-Apr-2021 07:15 PM", "", 40))



            builder.addFeedLine(1)

            builder.addTextFont(Builder.FONT_B)
            //builder.addTextLineSpace(20)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 1)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )
            addHorizontalKitchenLine(builder)



            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)

            builder.addTextFont(Builder.FONT_C)
            //builder.addTextLineSpace(20)
            builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 2)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
            )
            //builder.addTextPosition(1)


            builder.addText("1 Chicken Meals")

            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)


            builder.addTextFont(Builder.FONT_C)
            //builder.addTextLineSpace(20)
            builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 2)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
            )
            //builder.addTextPosition(1)


            builder.addText("  Extra Spicy")


            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)


            builder.addTextFont(Builder.FONT_C)
            //builder.addTextLineSpace(20)
            builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 2)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
            )
            //builder.addTextPosition(1)


            builder.addText("  Extra Spicy")
            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)


            builder.addTextFont(Builder.FONT_C)
            //builder.addTextLineSpace(20)
            builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 2)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
            )
            //builder.addTextPosition(1)


            builder.addText("  Note:Not much spicy")



            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)
            builder.addFeedLine(1)
            builder.addTextFont(Builder.FONT_E)
            builder.addTextAlign(Builder.ALIGN_LEFT)
            //builder.addTextLineSpace(20)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 1)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
            )
            builder.addText("Order Note")
            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)

            builder.addTextFont(Builder.FONT_E)
            //builder.addTextLineSpace(20)
            builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 1)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )
            builder.addText("Sugar Free,No herbs & Spices")


            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)
            builder.addFeedLine(1)
            builder.addTextFont(Builder.FONT_E)
            //builder.addTextLineSpace(20)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 1)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
            )
            builder.addText("Customer Details" + "\n")


            builder.addTextFont(Builder.FONT_B)
            //builder.addTextLineSpace(20)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 1)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )
            addHorizontalKitchenLine(builder)


            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)
            builder.addTextFont(Builder.FONT_E)
            builder.addTextAlign(Builder.ALIGN_LEFT)
            //builder.addTextLineSpace(20)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 1)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
            )
            builder.addText("David Miller")

            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)
            builder.addTextFont(Builder.FONT_E)
            builder.addTextAlign(Builder.ALIGN_LEFT)
            //builder.addTextLineSpace(20)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 1)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
            )
            builder.addText("(635)987-3354")

            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)
            builder.addTextFont(Builder.FONT_E)
            builder.addTextAlign(Builder.ALIGN_LEFT)
            //builder.addTextLineSpace(20)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 1)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
            )
            builder.addText("7450 DW 51 FH AT,Suite 503")


            //PrinterReceipt.padLine()
            builder.addFeedLine(2)


            builder.addCut(Builder.CUT_FEED)


            //builder.addFeedUnit(30)

            Log.e("builder", builder.toString())

            //send builder data(empty builder data)
            val status = IntArray(1)
            val battery = IntArray(1)

            Log.e(TAG, "getPrinterCheck:  ${PrinterClass.getPrinter().toString()}")



            try {
                PrinterClass.getPrinter()?.sendData(builder, SEND_TIMEOUT, status, battery)
                //PrinterClass.getPrinter()?.sendData(builder, 0, status, battery)
                //   PrinterClass.closePrinter()
            } catch (e: Exception) {
                PrinterClass.closePrinter()
                e.printStackTrace()
                Log.e(TAG, "PrinterError: " + e.localizedMessage)
            }

            try {
                builder.clearCommandBuffer()
                builder = null
                PrinterClass.closePrinter()
            } catch (e: Exception) {
                builder = null
                PrinterClass.closePrinter()
                e.printStackTrace()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }


    @SuppressLint("UseCompatLoadingForDrawables")
    private fun showPrinterStatus(printerListModel: PrinterListModel) {
        var builder: Builder? = null
        var method = ""

        try {
            builder = Builder(printerListModel.printerName, language, requireActivity())

            builder.addFeedLine(2)
            val bitmap = getBitmapFromVectorDrawable(requireContext(), R.drawable.ic_group)
            Log.e(TAG, "BitmapWidth:  ${bitmap.width}")


            builder.addTextAlign(Builder.ALIGN_CENTER)
            builder.addImage(
                bitmap, 0, 0, Math.min(
                    IMAGE_WIDTH_MAX, bitmap.width
                ), bitmap.height, Builder.COLOR_1, Builder.MODE_MONO,
                Builder.HALFTONE_DITHER, 1.0
            )

            builder.addFeedLine(2)

            builder.addTextFont(Builder.FONT_A)
            //builder.addTextLineSpace(20)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 2)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )
            //builder.addTextPosition(1)
            builder.addTextAlign(Builder.ALIGN_CENTER)

            builder.addText("Food Cafe\n")



            builder.addTextFont(Builder.FONT_C)
            builder.addTextAlign(Builder.ALIGN_CENTER)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 2)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )

            builder.addText("7450 DW 51 FH,AT,Suite 503\n\n")

            builder.addTextFont(Builder.FONT_C)
            builder.addTextAlign(Builder.ALIGN_CENTER)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 2)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )
            builder.addText("(635)987-3354\n")

            builder.addTextFont(Builder.FONT_A)
            builder.addTextAlign(Builder.ALIGN_CENTER)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 2)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )

            builder.addText("www.foodcourt.com\n\n")

            builder.addTextFont(Builder.FONT_C)
            //  builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 2)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )

            builder.addText(padLine("OrderID:23564", "ReceiptID:REC54646", 46))
            //  builder.addText("ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMNOPQRSTUVWXYZ")

            builder.addFeedLine(2)
            builder.addTextFont(Builder.FONT_C)
            //  builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 2)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )

            builder.addText(padLine("Employee: David Miller", "29-Apr-2021 07:15 PM", 46))

            builder.addFeedLine(2)
            //builder.addHLine(0,46,Builder.LINE_THIN_DOUBLE)

            builder.addTextFont(Builder.FONT_C)
            // builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 2)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )

            builder.addText(padLine("1x Chicken Meals", "$9.99", 46))

            builder.addFeedLine(1)
            builder.addTextFont(Builder.FONT_C)
            builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 2)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )
            builder.addTextPosition(4)
            builder.addText(padLine("   Extra Spicy", "$1.99", 46))


            builder.addFeedLine(1)
            builder.addTextFont(Builder.FONT_C)
            // builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 2)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )
            builder.addTextPosition(3)
            builder.addText(padLine(" Extra Spicy", "$1.99", 44))

            builder.addFeedLine(2)

            builder.addTextFont(Builder.FONT_D)
            // builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 1)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )

            builder.addText(padLine("Sub Total", "$9.99", 46))
            builder.addFeedLine(2)

            builder.addTextFont(Builder.FONT_C)
            // builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 1)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )

            builder.addText(padLine("Refund Amount", "$9.99", 46))

            builder.addFeedLine(2)
            builder.addTextFont(Builder.FONT_B)
            // builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 1)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )

            builder.addText(padLine("Service Charge", "$9.99", 46))


            builder.addFeedLine(2)

            builder.addTextFont(Builder.FONT_A)
            // builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 1)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )

            builder.addText(padLine("Total Price", "$9.99", 46))

            builder.addFeedLine(2)

            builder.addTextFont(Builder.FONT_E)
            // builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 1)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )

            builder.addText(padLine("Entertainment(4.00%)", "$9.99", 46))


            builder.addFeedLine(2)

            builder.addTextFont(Builder.FONT_A)
            builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 2)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )

            builder.addText("Cstomer Details")

            builder.addFeedLine(2)

            builder.addTextFont(Builder.FONT_A)
            builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 2)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )

            builder.addText("David Miller")

            builder.addFeedLine(1)
            builder.addTextFont(Builder.FONT_A)
            builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 2)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )

            builder.addText("7450 DW 51 FH AT,Suite 503")


            //PrinterReceipt.padLine()
            builder.addFeedLine(2)


            builder.addCut(Builder.CUT_FEED)


            //builder.addFeedUnit(30)

            Log.e("builder", builder.toString())

            //send builder data(empty builder data)
            val status = IntArray(1)
            val battery = IntArray(1)

            Log.e(TAG, "getPrinterCheck:  ${PrinterClass.getPrinter().toString()}")



            try {
                PrinterClass.getPrinter()?.sendData(builder, SEND_TIMEOUT, status, battery)
                //PrinterClass.getPrinter()?.sendData(builder, 0, status, battery)
            } catch (e: Exception) {
                PrinterClass.closePrinter()
                e.printStackTrace()
                Log.e(TAG, "PrinterError: " + e.localizedMessage)
            }

            try {
                builder.clearCommandBuffer()
                builder = null
                PrinterClass.closePrinter()
            } catch (e: Exception) {
                builder = null
                PrinterClass.closePrinter()
                e.printStackTrace()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {

            val myData: BluetoothDevice =
                data?.getParcelableArrayExtra(BluetoothDevice.EXTRA_NAME) as BluetoothDevice
            Log.e(TAG, "AvailableName   ${myData.name}")
        } else if (requestCode == 211) {
            val device =
                data!!.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            Log.e(TAG, "DeviceName  ${device!!.name}")
        }

    }


    @Throws(IOException::class)
    private fun sendBTData() {

        try {
            val msg =
                Html.fromHtml("<h1>My First Example</h1> <p>My first Print.</p> <p><b>This is a paragraph.</b></p>")

            val arrayOfByte1 = byteArrayOf(27, 33, 0)
            val format = byteArrayOf(27, 33, 0)

            // Bold
            format[2] = (0x8 or arrayOfByte1[2].toInt()).toByte()
            mmOutputStream!!.write(format)
            mmOutputStream!!.write(
                msg.toString().toByteArray(),
                0,
                msg.toString().toByteArray().size
            )

            // tell the user data were sent

        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            var msg: String = "This is My Test Print"
            msg += "\n"

            //  mmOutputStream?.write(msg.toByteArray())

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    // close the connection to bluetooth printer.
    @Throws(IOException::class)
    fun closeBT() {
        try {
            stopWorker = true
            mmOutputStream?.close()
            mmInputStream?.close()
            mmSocket?.close()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun deletePrinter(id: Int) {
        alert(
            getString(R.string.tv_pos),
            getString(R.string.delete_printer_message)
        ) {
            positiveButton(getString(R.string.tv_delete)) {
                viewModel.deletePrinter(id)

            }
            negativeButton(R.string.tv_cancel) {

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
}