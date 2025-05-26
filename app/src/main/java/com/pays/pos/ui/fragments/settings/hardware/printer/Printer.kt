package com.pays.pos.ui.fragments.settings.hardware.printer


import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.epson.epos2.Epos2Exception
import com.epson.epos2.discovery.Discovery
import com.epson.epos2.discovery.DiscoveryListener
import com.epson.epos2.discovery.FilterOption
import com.epson.epos2.printer.Printer
import com.epson.epos2.printer.PrinterStatusInfo
import com.epson.epos2.printer.ReceiveListener
import com.epson.eposprint.BatteryStatusChangeEventListener
import com.epson.eposprint.Builder
import com.epson.eposprint.Print
import com.epson.eposprint.StatusChangeEventListener
import com.epson.epsonio.*
import com.google.gson.Gson
import com.pays.pos.MainApplication
import com.pays.pos.R
import com.pays.pos.aidl.ICallback
import com.pays.pos.aidl.IWoyouService
import com.pays.pos.data.entities.TbOrderType
import com.pays.pos.data.model.PrinterListModel
import com.pays.pos.data.model.requestModel.CreatePrinterRequestModel
import com.pays.pos.data.model.requestModel.OrderAttributeRequestModel
import com.pays.pos.data.model.requestModel.OrderItemsAttribute
import com.pays.pos.data.model.requestModel.OrderRequestModel
import com.pays.pos.data.model.responseModel.PrinterResponse
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.AVAILABLE
import com.pays.pos.data.remote.Constants.BLUETOOTH
import com.pays.pos.data.remote.Constants.CUSTOMER
import com.pays.pos.data.remote.Constants.DISCOVERY_INTERVAL
import com.pays.pos.data.remote.Constants.EMPLOYEE_ID
import com.pays.pos.data.remote.Constants.EPSONBRAND
import com.pays.pos.data.remote.Constants.IS_MASTER_TERMINAL
import com.pays.pos.data.remote.Constants.IS_PRINTER_QUEUE_ENABLE
import com.pays.pos.data.remote.Constants.KITCHEN
import com.pays.pos.data.remote.Constants.KITCHENANDCUSTOMER
import com.pays.pos.data.remote.Constants.LOCATION_ID
import com.pays.pos.data.remote.Constants.MANUAL_SALE_CATEGORY_ID
import com.pays.pos.data.remote.Constants.MANUAL_SALE_ITEM_ID
import com.pays.pos.data.remote.Constants.PRINTER
import com.pays.pos.data.remote.Constants.TAKEOUT
import com.pays.pos.data.remote.Constants.TERMINAL_ID
import com.pays.pos.data.remote.Constants.WIFI
import com.pays.pos.data.remote.Constants.createCloudPrinterWithName
import com.pays.pos.data.remote.Constants.getCurrentTimeFromTimeZone
import com.pays.pos.databinding.FragmentPrinterBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.ui.activities.MainActivity
import com.pays.pos.ui.adapter.CustomerPrinterListAdapter
import com.pays.pos.ui.adapter.KitchenPrinterListAdapter
import com.pays.pos.ui.adapter.PrinterListAdapter
import com.pays.pos.utils.*
import com.pays.pos.utils.MethodUtils.Companion.getSaltString
import com.pays.pos.utils.extensions.*
import com.pays.pos.utils.printer.PrinterClass
import com.pays.pos.utils.printer.PrinterClass.SEND_TIMEOUT
import com.pays.pos.utils.printer.PrinterClass.language
import com.pays.pos.utils.statusUtils.Status
import com.starmicronics.stario10.*
import com.starmicronics.stario10.starxpandcommand.DocumentBuilder
import com.starmicronics.stario10.starxpandcommand.PrinterBuilder
import com.starmicronics.stario10.starxpandcommand.StarXpandCommandBuilder
import com.starmicronics.stario10.starxpandcommand.printer.*
import com.stealthcopter.networktools.SubnetDevices
import com.stealthcopter.networktools.subnet.Device
import com.sunmi.externalprinterlibrary.api.ConnectCallback
import com.sunmi.externalprinterlibrary.api.SunmiPrinter
import com.sunmi.externalprinterlibrary.api.SunmiPrinterApi
import com.sunmi.externalprinterlibrary2.ResultCallback
import com.sunmi.externalprinterlibrary2.SearchCallback
import com.sunmi.externalprinterlibrary2.SearchMethod
import com.sunmi.externalprinterlibrary2.SunmiPrinterManager
import com.sunmi.externalprinterlibrary2.exceptions.SearchException
import com.sunmi.externalprinterlibrary2.printer.CloudPrinter
import com.sunmi.externalprinterlibrary2.style.CloudPrinterStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.UnsupportedEncodingException
import java.lang.Runnable
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.net.URLDecoder
import java.security.InvalidKeyException
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.Signature
import java.security.SignatureException
import java.security.spec.InvalidKeySpecException
import java.security.spec.PKCS8EncodedKeySpec
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject


//Original New
@AndroidEntryPoint
class Printer : Fragment(), Runnable, PrinterListAdapter.PrinterListInterface,
    StatusChangeEventListener, BatteryStatusChangeEventListener, ICallback,
    SearchCallback, UpdatePrinters, ReceiveListener,
    KitchenPrinterListAdapter.PrinterListInterface,
    CustomerPrinterListAdapter.PrinterListInterface {
    private var cloudPrinter: CloudPrinter? = null
    private var woyouService: IWoyouService? = null
    private lateinit var binding: FragmentPrinterBinding
    var mBluetoothAdapter: BluetoothAdapter? = null
    var deviceList: Array<DeviceInfo>? = null
    var kitchenPrintList: ArrayList<PrinterListModel> = arrayListOf()
    var customerPrintList: ArrayList<PrinterListModel> = arrayListOf()
    var availablePrinterList: ArrayList<PrinterListModel> = arrayListOf()
    var orderTypeList: ArrayList<TbOrderType> = arrayListOf()

    var isOneClick: Boolean = false
    var orderContent: java.lang.StringBuilder = java.lang.StringBuilder()

    //private var mFilterOption: FilterOption? = null
    private lateinit var customerAdapter: CustomerPrinterListAdapter
    private lateinit var kitchenAdapter: KitchenPrinterListAdapter
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

    private var _manager: StarDeviceDiscoveryManager? = null

    private val viewModel by viewModels<PrinterViewModel>()

    var addedCustomerPrinters = false
    var addedKitchenPrinters = false

    var charHSize: Int = 1
    var asciiCharWidth: Int = 12
    var cjkCharWidth: Int = 24


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
        /*
        * This is Printer class. we are manage all hardware related things for printer in this class
        * We can delete printer and update printer from our local database
        * We are using room database to store printer
        *
        * */


        binding = FragmentPrinterBinding.inflate(inflater, container, false)
        try {
            SunmiPrinterManager.getInstance()
                .searchCloudPrinter(requireContext(), SearchMethod.LAN, this)
        } catch (e: SearchException) {
            e.printStackTrace()
        }
        binding.lifecycleOwner = this
        binding.maskLayout?.visible()
        updatePrinter = this
        printerList = ArrayList()

        getOrderTypes()

        setUpHeader()
        onDeleteObserve()
        observePrinterStatus()
        onCreatePrinterObserve()
        onDeleteQueueObserve()

        checkMasterTerminal()
        dialogCallback()

        viewModelObject = viewModel

        return binding.root
    }

    private fun dialogCallback() {
        setFragmentResultListener("request_cloud_serial_number"){resultKey: String, bundle: Bundle ->
            var serialNumber = bundle.getString("serial_number")
            Log.e(TAG,"checkSerialNumber  ${serialNumber}")
            var list: ArrayList<CreatePrinterRequestModel.PrinterSettingsAttributes> =
                arrayListOf()
            var printerListModel = bundle.getParcelable<PrinterListModel>("printerListModel")
            var layoutPosition = bundle?.getInt("layoutPosition")

            printerListModel?.let {
                ifKitchenPrinterSelected(list, it, layoutPosition,serialNumber) }



        }
    }

    private fun deleteAllPrinters() {
        lifecycleScope.launch {
            viewModel.deleteAllKitchenPrinters()
        }
    }


    private fun checkMasterTerminal() {
        if (prefProvider.getValueboolean(IS_PRINTER_QUEUE_ENABLE, false)) {
            if (prefProvider.getValueboolean(IS_MASTER_TERMINAL, false)) {
                binding.txtLabel2.visible()
                binding.linearKitchenPrntData?.visible()
                binding.rvKitchenPrinter.visible()
            } else {
                binding.txtLabel2.gone()
                binding.linearKitchenPrntData?.gone()
                binding.rvKitchenPrinter.gone()
            }
        } else {
            binding.txtLabel2.visible()
            binding.linearKitchenPrntData?.visible()
            binding.rvKitchenPrinter.visible()
        }

//        if (!prefProvider.getValueboolean(IS_MASTER_TERMINAL, false) && prefProvider.getValueboolean(IS_PRINTER_QUEUE_ENABLE, false)) {
//            binding.txtLabel2.gone()
//            binding.linearKitchenPrntData?.gone()
//            binding.rvKitchenPrinter.gone()
//        }
    }

    private fun setUpHeader() {
        binding.header.imgSync.visible()
        binding.header.txtTitle.text = getString(R.string.printers)
        binding.header.txtSave.text = getString(R.string.tv_home)
    }


    private fun getOrderTypes() = viewModel.orderTypes.observe(viewLifecycleOwner) {
        when (it.status) {
            Status.LOADING -> {
                ProgressUtils.showProgressDialog(requireActivity())
            }

            Status.ERROR -> {
                ProgressUtils.dismissProgressDialog()
            }

            Status.SUCCESS -> {
                ProgressUtils.dismissProgressDialog()
                if (it.data != null) {
                    orderTypeList.clear()
                    orderTypeList = arrayListOf()
                    orderTypeList.addAll(it.data.toCollection(ArrayList()))

                }


            }
        }
    }

    private fun startFinder() {
        /* scheduler = Executors.newSingleThreadScheduledExecutor()
         if (scheduler == null) {
             return
         }*/

        try {
            Finder.start(requireContext(), DevType.TCP, "255.255.255.255")

        } catch (e: Exception) {
            LogUtil.logE(TAG, "PrinterFinderError  ${e.message}")

        }

        printersLisFromFinder()

        startStarPrinterDiscovery()

        // start thread
//        future = scheduler!!.schedule(this, 0, TimeUnit.MILLISECONDS)
        /* future = scheduler!!.scheduleWithFixedDelay(
             this,
             0,
             DISCOVERY_INTERVAL.toLong(),
             TimeUnit.MILLISECONDS
         )*/

    }

    private fun startStarPrinterDiscovery() {

        val interfaceTypes = mutableListOf<InterfaceType>()
        interfaceTypes += InterfaceType.Lan
        try {
            this._manager?.stopDiscovery()

            _manager = StarDeviceDiscoveryManagerFactory.create(
                interfaceTypes,
                requireContext()
            )
            _manager?.discoveryTime = 10000
            _manager?.callback = object : StarDeviceDiscoveryManager.Callback {
                override fun onPrinterFound(printer: StarPrinter) {
                    var identifier =
                        "${printer.connectionSettings.identifier}"

                    Log.d("Discovery", "Found printer: ${printer.connectionSettings.identifier}.")

                    /* model : TSP650II, emulation : StarLine, reserved : {
                         bluetoothAddress =
                             null, macAddress = 0011624114E0, specifiedIdentifier = null, usbSerialNumber = null, configGateway = 0.0.0.0, configIPAddress = 0.0.0.0, configPrint = true, configSubnetMask = 0.0.0.0, deviceClass = PRINTER, deviceCommandSet = STAR, deviceManufacture = Star, deviceModel = TSP654 (STR_T-001), deviceStatus = null, dhcp = true, firmwareVersionBoot = V2.0.0, firmwareVersionMain = V5.1.2, gateway = 192.168.0.1, hostName = null, ipAddress = 192.168.0.194, ipAddressProtocol = DHCP, ipVersion = 1, multiSession = false, name = IFBD-HE07/08, nameDetail = null, pldRevision = V1.0.0, productSerialNumber = null, rarp = true, responseVersion = 1.0.1, subnetMask = 255.255.255.0, usedIPAddress = null, usedPort = null
                     }*/

                    var starPrinterData = PrinterListModel(
                        printerName = printer.information?.model?.name,
                        connectionType = WIFI,
                        deviceModel = DeviceInfo(
                            DevType.TCP,
                            identifier,
                            printer.information?.model?.name,
                            identifier,
                            identifier
                        ),
                        type = AVAILABLE,
                        uuid = UUID.randomUUID(),
                        modelName = printer.information?.model?.name

                    )
                    var found = false
                    for (it in kitchenAdapter.dataList) {
                        if (it.modelName.equals(
                                printer.information?.model?.name,
                                ignoreCase = true
                            )
                        ) {
                            found = true
                            availableNetworkAdapter.dataList.forEachIndexed { index, it ->
                                if (it.modelName.equals(
                                        printer.information?.model?.name,
                                        ignoreCase = true
                                    )
                                ) {
                                    availableNetworkAdapter.dataList.removeAt(index)
                                    availableNetworkAdapter.notifyItemChanged(index)
                                }
                            }
                            break
                        }
                    }

                    if (!found) {
                        var flagFound = false
                        for (data in availableNetworkAdapter.dataList) {
                            if (data.modelName.equals(
                                    starPrinterData.modelName,
                                    ignoreCase = true
                                )
                            ) {
                                flagFound = true
                                break
                            }
                        }

                        if (!flagFound) {
                            availableNetworkAdapter.addItem(
                                starPrinterData
                            )
                        }

                    }


                }

                override fun onDiscoveryFinished() {
                    Log.d("Discovery", "Discovery finished.")
                }
            }

            _manager?.startDiscovery()
        } catch (e: Exception) {
            Log.d("Discovery", "Error: ${e}")
        }


    }

    override fun onPause() {
        super.onPause()
        PrinterClass.closePrinter()
        binding.unbind()
        /* stopFinder()

         closeBT()*/
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Binding()
        updatePrinter = this
        //hideLoaderAfterDelay()

        try {
            SunmiPrinterManager.getInstance()
                .searchCloudPrinter(requireContext(), SearchMethod.LAN, this)
        } catch (e: SearchException) {
            e.printStackTrace()
        }
        kitchenAdapter = KitchenPrinterListAdapter()
        customerAdapter = CustomerPrinterListAdapter()
        availableNetworkAdapter = PrinterListAdapter()

        observeShowProgress()
        onDeleteObserve()



        kitchenAdapter = KitchenPrinterListAdapter()
        kitchenAdapter.setList(kitchenPrintList)
        kitchenAdapter.setListner(this)

        customerAdapter.setList(customerPrintList)
        customerAdapter.setListner(this)

        availableNetworkAdapter.setListner(this)
        availableNetworkAdapter.setList(arrayListOf())

        binding.rvAvailablePrinter.itemAnimator = null
        binding.rvAvailablePrinter.adapter = availableNetworkAdapter
        binding.rvAvailablePrinter.isNestedScrollingEnabled = false
        //   binding.rvAvailablePrinter.isLayoutFrozen = true
        binding.rvAvailablePrinter.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                LinearLayoutManager.VERTICAL
            )
        )
        binding.rvKitchenPrinter.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.rvKitchenPrinter.adapter = kitchenAdapter
        binding.rvKitchenPrinter.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                LinearLayoutManager.VERTICAL
            )
        )
        binding.rvCustomerPrinter.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.rvCustomerPrinter.adapter = customerAdapter
        binding.rvCustomerPrinter.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                LinearLayoutManager.VERTICAL
            )
        )

        mFilterOption = FilterOption()
        mFilterOption!!.setDeviceType(Discovery.TYPE_PRINTER)
        syncPrinterList()

        /* try {
             Discovery.start(requireContext(), mFilterOption, mDiscoveryListener)
         } catch (e: Exception) {
             LogUtil.logE(TAG, "PrinterException:      ${e.message}")
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


        //mFilterOption?.setEpsonFilter(Discovery.FILTER_NAME);
        /*
        try {
            Discovery.start(view.context, mFilterOption, mDiscoveryListener)
        } catch (e: Exception) {
            LogUtil.logE(TAG, "GetPinterNameFailed:  " + e.message)
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
              LogUtil.logE(TAG, "ExceptionName:  ${e.message}")
              e.printStackTrace()

          }
          try {
              Discovery.start(view.context, mFilterOption, mDiscoveryListener)
          } catch (e: Exception) {
              LogUtil.logE(TAG, "GetPinterNameFailed:  " + e.message)
              e.printStackTrace()
          }*/

        binding.header.imgBack.setOnClickListener {
            val navController = findNavController()
            navController.previousBackStackEntry?.savedStateHandle?.set(
                Constants.KEY,
                PRINTER
            )
            navController.popBackStack()
        }

        binding.header.txtSave.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.printer) {
                findNavController().navigate(R.id.action_printer_to_dashboardCategoryNew)
            }

            // syncPrinterList(true)

        }

        binding.header.imgSync.setOnSingleClickListener {
            if (!isOneClick) {
                isOneClick = true
                binding.maskLayout?.visible()
                availableNetworkAdapter.clearList()

                //searchBluetooth()
                try {
                    //  stopFinder()

                    syncPrinterList()

                    // startFinder()

                } catch (e: Exception) {
                    e.printStackTrace()
                }

                GlobalScope.launch(Dispatchers.Main) {
                    delay(4000)
                    hideLoaderAfterDelay()
                    isOneClick = false
                    addedCustomerPrinters = false
                    addedKitchenPrinters = false
                }

            }

        }


    }

    private fun getWifiInfo() {
        SubnetDevices.fromLocalAddress().findDevices(object :
            SubnetDevices.OnSubnetDeviceFound {
            override fun onDeviceFound(device: Device?) {
                Log.e(TAG, "onSingleDevice:  ${Gson().toJson(device)}")

            }

            override fun onFinished(devicesFound: ArrayList<Device>?) {
                Log.e(TAG, "devicesFound:  ${Gson().toJson(devicesFound)}")
            }

        })


    }

    private fun onCreatePrinterObserve() {
        viewModel.printerCreatedSucces.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { data ->
                /* viewModel.printerList()
                 viewModel.getKitchenPrinters()*/
                syncPrinterList()


            }
        }

    }

    private fun onDeleteQueueObserve() {
        viewModel.printerQueueDeleteScenario.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { data ->
                LogUtil.logE(TAG, "deleteSuccess")
                AlertUtils.showCustomAlertWithListenerWithOK(requireContext(), data) { _, _ ->
                    // syncPrinterList()

                }

                /*  viewModel.printerList()
                  viewModel.getKitchenPrinters()
                  syncPrinterList()*/


            }
        }
    }


    private fun onDeleteObserve() {
        viewModel.deletePrinter.observe(viewLifecycleOwner) {
            it.getContentIfNotHandled()?.let { data ->
                viewModel.printerList()
                viewModel.getKitchenPrinters()
                syncPrinterList()


            }
        }
    }

    private fun syncPrinterList(saved: Boolean = false, isProgressShow: Boolean = false) {
        runOnUiThread(kotlinx.coroutines.Runnable {
            customerAdapter.clearList()
            kitchenAdapter.clearList()

        })

        allPrinterlist.clear()
        addedCustomerPrinters = false
        addedKitchenPrinters = false
        viewModel.printerList().observe(parentFragment?.viewLifecycleOwner ?: viewLifecycleOwner) {
            when (it.status) {

                Status.SUCCESS -> {

                    ProgressUtils.dismissProgressDialog()

                    val data = it.data


                    if (it.data != null) {

                        val customerData = data
                        runOnUiThread(kotlinx.coroutines.Runnable {
                            customerAdapter.clearList()
                            customerAdapter.setList(arrayListOf())
                        })

                        var customerPrintersList: ArrayList<PrinterListModel> = arrayListOf()
                        if (customerData != null) {
                            for (i in customerData.indices) {
                                customerPrintersList.add(
                                    PrinterListModel(
                                        id = customerData[i].id,
                                        printerName = customerData[i].name,
                                        modelName = customerData[i].modalName,
                                        connectionType = if (customerData[i].printer_type == BLUETOOTH) {
                                            BLUETOOTH
                                        } else {
                                            WIFI
                                        },
                                        isActive = customerData[i].status,
                                        isCustomerActive = customerData[i].customerStatus,
                                        isKitchenActive = customerData[i].kitchenStatus,
                                        type = customerData[i].receiptPrintType,
                                        deviceModel = DeviceInfo(
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
                                        printerModel = customerData[i].orderTypes,
                                        currentPrinterType = CUSTOMER,
                                        printerCategories = customerData[i].printerCategories


                                    )
                                )

                                /*  allPrinterlist.add(
                                      PrinterListModel(
                                          id = customerData[i].id,
                                          printerName = customerData[i].name,
                                          modelName = customerData[i].modalName,
                                          connectionType = if (customerData[i].printer_type == BLUETOOTH) {
                                              BLUETOOTH
                                          } else {
                                              WIFI
                                          },
                                          isActive = customerData[i].status,
                                          type = customerData[i].receiptPrintType,
                                          deviceModel = DeviceInfo(
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
                                          printerModel = customerData[i].orderTypes,
                                          printerCategories = customerData[i].printerCategories


                                      )

                                  )*/

                            }


                            //   viewModel.deleteAllCustomerPrinters()

                            var temp = ""
                            var newList = arrayListOf<PrinterListModel>()

                            customerPrintersList.forEach {

                                if (temp == it.printerName) {
                                    //viewModelObject.deletePrinter(it)
                                    Log.d(
                                        "deDupedNodes",
                                        "Duplicate operaion id = ${it.id} , name = ${it.printerName}"
                                    )
                                } else {
                                    Log.d("deDupedNodes", "Unique opera")
                                    Log.d(
                                        "deDupedNodes",
                                        "Unique operaion id = ${it.id} , name = ${it.printerName}"
                                    )
                                    newList.add(it)
                                }
                                temp = it.printerName!!

                            }

                            customerPrintersList.clear()
                            customerPrintersList = newList


                            runOnUiThread(kotlinx.coroutines.Runnable {
                                customerAdapter.setList(customerPrintersList)
                            })
                            allPrinterlist.addAll(customerPrintersList)
                            addedCustomerPrinters = true
                        } else {
                            addedCustomerPrinters = true
                        }
                        if (addedCustomerPrinters && addedKitchenPrinters) {
                            availableNetworkAdapter.clearList()
                            searchBluetooth()
                            startFinder()
                        }


                    } else {
                        LogUtil.logE(TAG, "ITNotNull  ")
                        //kitchenAdapter.clearList()

                        runOnUiThread(kotlinx.coroutines.Runnable {
                            customerAdapter.clearList()
                        })
                        addedCustomerPrinters = true
                    }

                    /*if (data?.kitchenReceiptPrinters?.isEmpty() == true) {
                        kitchenAdapter.clearList()
                    }
*/

                    /* if (data?.isEmpty() == true) {
                         customerAdapter.clearList()
                     }*/


                    if (saved) {

                        val navController = findNavController()
                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            Constants.KEY,
                            PRINTER
                        )

                        navController.popBackStack()

                    }
                }

                Status.ERROR -> {
                    LogUtil.logE(TAG, "PrinterError ")
                    ProgressUtils.dismissProgressDialog()
                    availableNetworkAdapter.clearList()
                    searchBluetooth()
                    startFinder()
                }

                Status.LOADING -> {
                    ProgressUtils.showProgressDialog(requireActivity())
                }
            }

        }
        getAllKitchenPrintersListFromDB()
        /* lifecycleScope.launch {
             delay(1000)
             getAllKitchenPrintersListFromDB()
             delay(500)
             getAllKitchenPrintersListFromDB()

         }*/
    }

    private fun getAllKitchenPrintersListFromDB() {
        viewModel.viewModelScope.launch {
            ProgressUtils.showProgressDialog(requireActivity())
            try {
                viewModel.getKitchenPrintersList().observe(viewLifecycleOwner, { it ->

                    var kitchenData = it.data


                    Log.e("checkData", "kitchenList  ${kitchenData?.size}")
                    if (kitchenData?.isNotEmpty() == true) {
                        runOnUiThread(kotlinx.coroutines.Runnable {
                            kitchenAdapter.clearList()
                            kitchenAdapter.setList(arrayListOf())
                        })
                        var kitchenPrintersList: ArrayList<PrinterListModel> = arrayListOf()
                        if (kitchenData != null) {
                            for (i in kitchenData.indices) {


                                if (prefProvider.getValueboolean(
                                        IS_PRINTER_QUEUE_ENABLE,
                                        false
                                    ) && prefProvider.getValueboolean(IS_MASTER_TERMINAL, false)
                                ) {
                                    if (kitchenData[i].printer_type == Constants.BLUETOOTH || kitchenData[i].printer_type == Constants.WIFI) {
                                        viewModel.deleteKitchenPrinter(kitchenData[i].id)
                                    } else {
                                        addPrinters(kitchenPrintersList, kitchenData, i)
                                    }
                                } else {
                                    if (kitchenData[i].printer_type == Constants.WIFI) {
                                        if (kitchenData[i].name.equals(
                                                "TM-L100",
                                                ignoreCase = true
                                            ) || kitchenData[i].name.contains(
                                                "TSP",
                                                ignoreCase = true
                                            ) || kitchenData[i].name.contains(
                                                "SP",
                                                ignoreCase = true
                                            )|| kitchenData[i].name.contains(
                                                "Cloud",
                                                ignoreCase = true
                                            ) || kitchenData[i].name.contains(
                                                "TM-U220",
                                                ignoreCase = true
                                            )
                                        ) {
                                            addPrinters(kitchenPrintersList, kitchenData, i)
                                        } else {
                                            viewModel.deleteKitchenPrinter(kitchenData[i].id)
                                        }
                                    } else {
                                        addPrinters(kitchenPrintersList, kitchenData, i)
                                    }
                                }

                            }
                            Log.d(
                                "kitchenPrintersList",
                                "kitchenPrintersList size = ${kitchenPrintersList.size}"
                            )
                            runOnUiThread(kotlinx.coroutines.Runnable {
                                kitchenAdapter.setList(kitchenPrintersList)
                            })
                            allPrinterlist.addAll(kitchenPrintersList)
                            addedKitchenPrinters = true
                        } else {
                            addedKitchenPrinters = true
                        }

                        if (addedCustomerPrinters && addedKitchenPrinters) {
                            availableNetworkAdapter.clearList()
                            searchBluetooth()
                            startFinder()
                            SunmiPrinterManager.getInstance()
                                .searchCloudPrinter(
                                    requireContext(),
                                    SearchMethod.LAN,
                                    this@Printer
                                )
                        }


                    } else {
                        LogUtil.logE(TAG, "ITNotNull  ")
                        runOnUiThread(kotlinx.coroutines.Runnable {
                            kitchenAdapter.clearList()
                        })
                        addedKitchenPrinters = true
                        SunmiPrinterManager.getInstance()
                            .searchCloudPrinter(requireContext(), SearchMethod.LAN, this@Printer)
                    }

                    hideLoaderAfterDelay()
                    ProgressUtils.dismissProgressDialog()
                })
            } catch (e: Exception) {
                ProgressUtils.dismissProgressDialog()
            }
        }
    }

    private fun addPrinters(
        kitchenPrintersList: ArrayList<PrinterListModel>,
        kitchenData: List<PrinterResponse.Data.KitchenReceiptPrinters>,
        i: Int
    ) {

        kitchenPrintersList.add(
            PrinterListModel(
                id = kitchenData[i].id,
                printerName = kitchenData[i].name,
                modelName = kitchenData[i].modalName,
                connectionType = if (kitchenData[i].printer_type == BLUETOOTH) {
                    BLUETOOTH
                } else {
                    WIFI
                },
                isActive = kitchenData[i].status,
                isKitchenActive = kitchenData[i].kitchenStatus,
                isCustomerActive = kitchenData[i].customerStatus,
                type = kitchenData[i].receiptPrintType,
                deviceModel = DeviceInfo(
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
                printerModel = kitchenData[i].orderTypes,
                currentPrinterType = KITCHEN,
                printerCategories = kitchenData[i].printerCategories


            )
        )
    }

    @SuppressLint("MissingPermission")
    private fun searchBluetooth() {
        val bluetoothManager = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter

        if (adapter == null) {
            showRestartHint("Bluetooth not available. Please restart your device.")
            Log.e(TAG, "BluetoothAdapter is null")
            return
        }

        if (!adapter.isEnabled) {
            Toast.makeText(requireContext(), "Bluetooth is turned off. Please enable Bluetooth.", Toast.LENGTH_LONG).show()
            Log.e(TAG, "Bluetooth is disabled.")
            return
        }

        try {
            val bondedDevices = adapter.bondedDevices

            if (bondedDevices.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "No paired devices found. Try pairing a printer.", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "No bonded devices found.")
                return
            }

            bondedDevices.forEach { device ->
                val isAdded = allPrinterlist.any { it.deviceModel?.macAddress == device.address }

                if (!isAdded) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Log.e(TAG, "BluetoothPrinter alias: ${device.alias}")
                    }

                    availableNetworkAdapter.addItem(
                        PrinterListModel(
                            printerName = device.name,
                            connectionType = BLUETOOTH,
                            deviceModel = DeviceInfo(
                                DevType.BLUETOOTH,
                                device.address,
                                device.name,
                                device.address,
                                device.address
                            ),
                            type = AVAILABLE,
                            uuid = UUID.randomUUID(),
                            modelName = device.name
                        )
                    )
                }

                if (device.name == "TM-m30_030295") {
                    mmDevice = device
                }
            }

        } catch (e: TimeoutException) {
            Log.e(TAG, "BluetoothAdapter.getBondedDevices() timed out", e)
            showRestartHint("Bluetooth system not responding. Please restart your device.")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error accessing Bluetooth bonded devices", e)
            showRestartHint("Unexpected Bluetooth error. Try restarting your device.")
        }
    }

    private fun showRestartHint(message: String) {
        AlertUtils.showCustomAlert(requireContext(), message)
    }

    override fun onStop() {
        super.onStop()
        //stop find
        //  stopFinder()
        /*     PrinterClass.closePrinter()
             closeBT()*/


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
                    LogUtil.logE(TAG, "StartPrinterStart")

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
            LogUtil.logE(TAG, "deviceList  ${Gson().toJson(deviceList)}")

            if (deviceList != null) {
                var tempAvailableList: ArrayList<PrinterListModel> = arrayListOf()
                for (i in 0 until deviceList!!.size) {

                    var isAdded: Boolean = false
                    for (j in 0 until allPrinterlist.size) {
                        if (allPrinterlist.get(j).deviceModel?.macAddress?.lowercase() == deviceList!!.get(
                                i
                            ).macAddress.lowercase()
                        ) {
                            isAdded = true
                            break
                        } else {
                            isAdded = false


                        }


                    }
                    if (isAdded == false) {
                        Log.e(TAG, "checkIsNotAdded ${deviceList!!.get(i).printerName.lowercase()}")
                        if (deviceList!!.get(i).printerName.lowercase() == "TM-U220".lowercase()
                        ) {
                            Log.e(TAG, "checkInside 1")
//                            if (prefProvider.getValueboolean(IS_MASTER_TERMINAL, false) == true) {

                                Log.e(TAG, "checkInside 2")

                                /*  tempAvailableList.add(PrinterListModel(
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


                                      ))*/

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
                                        modelName = deviceList!!.get(i).printerName


                                    )
                                )
//                            }
                        } else {
                            /*  tempAvailableList.add(  PrinterListModel(
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


                                  ))*/

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
                                    modelName = deviceList!!.get(i).printerName


                                )
                            )
                        }
                    }


                }

                //availableNetworkAdapter.addAll(tempAvailableList)

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

    private fun printersLisFromFinder() {
        viewModel.viewModelScope.launch {
            try {
                deviceList = Finder.getDeviceInfoList(com.epson.epsonio.FilterOption.PARAM_DEFAULT)
                LogUtil.logE(TAG, "deviceList  ${Gson().toJson(deviceList)}")

                if (deviceList != null) {
                    var tempAvailableList: ArrayList<PrinterListModel> = arrayListOf()
                    for (i in 0 until deviceList!!.size) {

                        var isAdded: Boolean = false
                        for (j in 0 until allPrinterlist.size) {
                            if (allPrinterlist.get(j).deviceModel?.macAddress?.lowercase() == deviceList!!.get(
                                    i
                                ).macAddress.lowercase()
                            ) {
                                isAdded = true
                                break
                            } else {
                                isAdded = false


                            }


                        }
                        if (isAdded == false) {
                            Log.e(
                                TAG,
                                "checkIsNotAdded ${deviceList!!.get(i).printerName.lowercase()}"
                            )
                            if (deviceList!!.get(i).printerName.lowercase() == "TM-U220".lowercase()
                            ) {
                                Log.e(TAG, "checkInside 1")
//                                if (prefProvider.getValueboolean(
//                                        IS_MASTER_TERMINAL,
//                                        false
//                                    ) == true
//                                ) {

                                    Log.e(TAG, "checkInside 2")

                                    /*  tempAvailableList.add(PrinterListModel(
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


                                          ))*/

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
                                            modelName = deviceList!!.get(i).printerName


                                        )
                                    )
//                                }
                            } else {
                                /*  tempAvailableList.add(  PrinterListModel(
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


                                      ))*/
                                Log.e(
                                    TAG,
                                    "availableNetworkAdapter item3 ${deviceList!!.get(i).printerName}"
                                )
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
                                        modelName = deviceList!!.get(i).printerName


                                    )
                                )
                            }
                        }


                    }

                    //availableNetworkAdapter.addAll(tempAvailableList)

                }




                if (deviceList == null) {
                    if (printerList.size > 0) {
                        printerList.clear()
                        // printerListAdapter!!.notifyDataSetChanged()
                    }
                } else if (deviceList!!.size != printerList.size) {
                    printerList.clear()
                    var name: String? = null
                    var address: String? = null
                    for (i in deviceList!!.indices) {
                        name = deviceList!![i].printerName
                        address = deviceList!![i].deviceName
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


//                future?.cancel(false)
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
                return@launch
            }
        }
    }


    @SuppressLint("MissingPermission")
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
    @SuppressLint("MissingPermission")
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
            LogUtil.logE(TAG, "pairedDevices:   ${Gson().toJson(pairedDevices)}")
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

        Log.e(TAG,"checkConnType:  ${printerListModel?.connectionType}")
        printerListModel.printerName?.let {
            with(it) {
                if (it?.startsWith(
                        "CloudPrint",
                        true
                    ) == true && printerListModel?.connectionType == WIFI
                ) {

                    val date = Date()
                    val random = Random()
                    val timestamp = java.lang.String.format("%d", date.time / 1000)

                    val body = java.lang.StringBuilder()
                    body.append("{")
                    body.append(java.lang.String.format("\"sn\":\"%s\"", "${printerListModel.deviceModel?.ipAddress}"))
                    body.append(",")
                    body.append(java.lang.String.format("\"shop_id\":%d", 2241))
                    body.append("}")

                    orderContent.clear()
                    orderContent = java.lang.StringBuilder()
                    lineFeed(4)
                    setAlignment(1)

                    setCharacterSize(2,2)

                    appendText("Test Print")
                    lineFeed(6)
                    cutPaper(true)

                    Log.e("checkKey","pushContent: checkSN:${printerListModel.deviceModel?.ipAddress} ${pushContent(trade_no =
                    String.format("%s_%010d", "${printerListModel.deviceModel?.ipAddress}", System.currentTimeMillis()),
                        "${printerListModel.deviceModel?.ipAddress}", 1, 1, "您有新的订单", 0)}")


                } else {


                    if (it?.startsWith("CloudPrint", true) == true) {
                        if (prefProvider?.getValueboolean(
                                IS_MASTER_TERMINAL,
                                false
                            ) && printerListModel.currentPrinterType == KITCHEN && printerListModel.connectionType == WIFI
                        ) {


                            var orderTypeID: Int = 0
                            orderTypeList.forEach {
                                if (it.orderType == TAKEOUT) {
                                    orderTypeID = it.id
                                }
                            }

                            var listItems: ArrayList<OrderItemsAttribute> = arrayListOf()
                            var orderItem = OrderItemsAttribute()
                            orderItem.itemId = prefProvider.getValueInt(MANUAL_SALE_ITEM_ID, 1)
                            orderItem.category_id =
                                prefProvider.getValueInt(MANUAL_SALE_CATEGORY_ID, 1)
                            orderItem.itemName = "Test Print"
                            listItems.add(orderItem)
                            var orderAttr = OrderAttributeRequestModel()
                            orderAttr.orderTypeId = orderTypeID
                            orderAttr.employeeId = prefProvider.getValueInt(EMPLOYEE_ID, 0)
                            orderAttr.locationId = prefProvider.getValueInt(LOCATION_ID, 0)
                            orderAttr.offlineId = randomOfflineId()
                            orderAttr.paymentStatus = 1
                            orderAttr.orderItemsAttributes = listItems
                            orderAttr.macAddress =
                                printerListModel.deviceModel?.macAddress.toString()


                            var order =
                                OrderRequestModel(order = orderAttr, completed_all_payments = true)


                            viewModel.createPrinterQueueTestOrder(order)

                        } else {

                            printerListModel.deviceModel?.let {
                                GlobalScope.launch(Dispatchers.IO) {
                                    sunmiPrinterInit(it.ipAddress)
                                }
                            }
                        }

                    } else if (it?.startsWith("Printer", true) == true) {
                        if (prefProvider?.getValueboolean(
                                IS_MASTER_TERMINAL,
                                false
                            ) && printerListModel.currentPrinterType == KITCHEN
                        ) {


                            var orderTypeID: Int = 0
                            orderTypeList.forEach {
                                if (it.orderType == TAKEOUT) {
                                    orderTypeID = it.id
                                }
                            }

                            var listItems: ArrayList<OrderItemsAttribute> = arrayListOf()
                            var orderItem = OrderItemsAttribute()
                            orderItem.itemId = prefProvider.getValueInt(MANUAL_SALE_ITEM_ID, 1)
                            orderItem.category_id =
                                prefProvider.getValueInt(MANUAL_SALE_CATEGORY_ID, 1)
                            orderItem.itemName = "Test Print"
                            listItems.add(orderItem)
                            var orderAttr = OrderAttributeRequestModel()
                            orderAttr.orderTypeId = orderTypeID
                            orderAttr.employeeId = prefProvider.getValueInt(EMPLOYEE_ID, 0)
                            orderAttr.locationId = prefProvider.getValueInt(LOCATION_ID, 0)
                            orderAttr.offlineId = randomOfflineId()
                            orderAttr.paymentStatus = 1
                            orderAttr.orderItemsAttributes = listItems
                            orderAttr.macAddress =
                                printerListModel.deviceModel?.macAddress.toString()


                            var order =
                                OrderRequestModel(order = orderAttr, completed_all_payments = true)


                            viewModel.createPrinterQueueTestOrder(order)

                        } else {
                            sunmiLANPrinter(printerListModel)

                        }

                    } else if (it?.startsWith("InnerPrinter", true) == true) {
                        sunmiInnerPrinter(printerListModel.deviceModel?.ipAddress)
                    } else if (it?.equals("Inner Printer", ignoreCase = true)) {
                        landiTestPrint(printerListModel)
                    } else if (it?.equals(
                            "TM-L100",
                            ignoreCase = true
                        ) == true
                    ) {
                        initLabelPrinter(printerListModel)
                    } else if (it?.contains("TSP", ignoreCase = true) == true || it?.contains(
                            "SP",
                            ignoreCase = true
                        ) == true
                    ) {
                        initStarPrinter(printerListModel)
                    } else {
                        Log.e(TAG, "printerListModel  ${Gson().toJson(printerListModel)}")
                        if (prefProvider?.getValueboolean(
                                IS_MASTER_TERMINAL,
                                false
                            ) && printerListModel.currentPrinterType == KITCHEN
                        ) {


                            var orderTypeID: Int = 0
                            orderTypeList.forEach {
                                if (it.orderType == TAKEOUT) {
                                    orderTypeID = it.id
                                }
                            }

                            var listItems: ArrayList<OrderItemsAttribute> = arrayListOf()
                            var orderItem = OrderItemsAttribute()
                            orderItem.itemId = prefProvider.getValueInt(MANUAL_SALE_ITEM_ID, 1)
                            orderItem.category_id =
                                prefProvider.getValueInt(MANUAL_SALE_CATEGORY_ID, 1)
                            orderItem.itemName = "Test Print"
                            listItems.add(orderItem)
                            var orderAttr = OrderAttributeRequestModel()
                            orderAttr.orderTypeId = orderTypeID
                            orderAttr.employeeId = prefProvider.getValueInt(EMPLOYEE_ID, 0)
                            orderAttr.locationId = prefProvider.getValueInt(LOCATION_ID, 0)
                            orderAttr.offlineId = randomOfflineId()
                            orderAttr.paymentStatus = 1
                            orderAttr.orderItemsAttributes = listItems
                            orderAttr.macAddress =
                                printerListModel.deviceModel?.macAddress.toString()


                            var order =
                                OrderRequestModel(order = orderAttr, completed_all_payments = true)


                            viewModel.createPrinterQueueTestOrder(order)
                            onInitPrinter(printerListModel)

                        } else {

                            onInitPrinter(printerListModel)
                        }
                    }
                }

            }
        }

    }

    private fun landiTestPrint(printerListModel: PrinterListModel) {

        val printer = EscPosPrinter(BluetoothPrintersConnections.selectFirstPaired(), 203, 48f, 32)
        printer
            .printFormattedTextAndCut(
                """
        [C]================================
        [L]
        [C] Test Print 
        [L]
        [C]================================
        """.trimIndent()
            )

    }

    private fun initStarPrinter(printerListModel: PrinterListModel) {

        val settings =
            StarConnectionSettings(InterfaceType.Lan, printerListModel.deviceModel!!.macAddress)
        val printer = StarPrinter(settings, requireContext())



        CoroutineScope(Dispatchers.Main).launch {
/*
            printerListModel.deviceModel?.let {
                if (it.printerName.contains("TSP")){

                }else if (it.printerName.contains("SP7")){

                }
            }
*/

            try {
                // TSP100III series and TSP100IIU+ do not support actionPrintText because these products are graphics-only printers.
                // Please use the actionPrintImage method to create printing data for these products.
                // For other available methods, please also refer to "Supported Model" of each method.
                // https://star-m.jp/products/s_print/sdk/starxpand/manual/ja/android-kotlin-api-reference/stario10-star-xpand-command/printer-builder/action-print-image.html
                val builder = StarXpandCommandBuilder()
                builder.addDocument(
                    DocumentBuilder()
                        // To open a cash drawer, comment out the following code.
//                      .addDrawer(
//                          DrawerBuilder()
//                              .actionOpen(OpenParameter())
//                      )
                        .addPrinter(
                            PrinterBuilder()
                                .styleInternationalCharacter(InternationalCharacterType.Usa)
                                .styleCharacterSpace(0.0)
                                .actionFeedLine(1)
                                .styleAlignment(Alignment.Center)
                                .actionPrintText(
                                    "Test Print"
                                )
                                .actionCut(CutType.Partial)
                        )
                )
                val commands = builder.getCommands()
                printer.openAsync().await()
                printer.printAsync(commands).await()


                Log.d("Printing", "Success")
            } catch (e: Exception) {
                e.printStackTrace()
                Log.d("Printing", "Error: ${e.printStackTrace()}")
            } finally {
                printer.closeAsync().await()
            }
        }


    }


    private fun initLabelPrinter(printerListModel: PrinterListModel) {
        var printer: Printer? = null

        try {
            printer = com.epson.epos2.printer.Printer(
                Printer.TM_L100,
                Printer.MODEL_JAPANESE,
                requireContext()
            )

            printer!!.setReceiveEventListener(this)

            try {
                printer.addTextAlign(Printer.ALIGN_CENTER)
                printer.addText("Hello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello WorldHello World")
            } catch (e: Epos2Exception) {
//Displays error messages
                e.printStackTrace()
            }

            try {
                printer.connect(
                    "TCP:" + printerListModel.deviceModel?.ipAddress,
                    Printer.PARAM_DEFAULT
                )
            } catch (e: Epos2Exception) {
//Displays error messages
                e.printStackTrace()

            }

            try {
                printer.sendData(Printer.PARAM_DEFAULT)
            } catch (e: Epos2Exception) {
// Displays error messages
                e.printStackTrace()

// Abort process
            }
        } catch (e: Epos2Exception) {
//Displays error messages
            e.printStackTrace()

        }


    }

    private fun sunmiLANPrinter(printerListModel: PrinterListModel) {
        Log.e(TAG, "CheckTestSunmiLAN")


        var cloudPrinter: CloudPrinter = createCloudPrinterWithName(
            printerListModel?.printerName ?: "",
            printerListModel.deviceModel?.macAddress ?: "",
            9100
        )
        cloudPrinter.connect(requireContext(),
            object : com.sunmi.externalprinterlibrary2.ConnectCallback {
                override fun onConnect() {
                    cloudPrinter.printText("Test Print")

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        cloudPrinter.lineFeed(1)
                        cloudPrinter.printText(
                            getCurrentTimeFromTimeZone(
                                requireContext(),
                                MethodUtils.formatted()
                            )
                        )
                    }
                    cloudPrinter.lineFeed(4)
                    cloudPrinter.cutPaper(true)
                    cloudPrinter.commitTransBuffer(object : ResultCallback {
                        override fun onComplete() {
                            Log.e(TAG, "checkComplete  ")
                        }

                        override fun onFailed(p0: CloudPrinterStatus?) {
                            Log.e(TAG, "cloudFailedStatus")
                        }

                    })
                }

                override fun onFailed(p0: String?) {
                }

                override fun onDisConnect() {
                }

            })


    }

    private fun sunmiInnerPrinter(ipAddress: String?) {

        SunmiPrintHelper.getInstance().initSunmiPrinterService(requireContext())
        setService()


    }

    private fun setService() {
        if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.FoundSunmiPrinter) {

            LogUtil.logE("SunmiPrintHelper", "FoundSunmiPrinter")

            if (!BluetoothUtil.isBlueToothPrinter) {

                LogUtil.logE("SunmiPrintHelper", "isBlueToothPrinter")
                SunmiPrintHelper.getInstance().initPrinter()
                SunmiPrintHelper.getInstance().setAlign(1)
                SunmiPrintHelper.getInstance().lineWrap(2)
                SunmiPrintHelper.getInstance()
                    .printText("Test Print", 30F, true, false, "test1.ttf")
                SunmiPrintHelper.getInstance().lineWrap(1)



                SunmiPrintHelper.getInstance().setAlign(1)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    SunmiPrintHelper.getInstance().printText(
                        getCurrentTimeFromTimeZone(requireContext(), MethodUtils.formatted()),
                        30F,
                        true,
                        false,
                        "test1.ttf"
                    )
                }
                SunmiPrintHelper.getInstance().lineWrap(2)
                LogUtil.logE(TAG, "Here Drawer Code")
                PrintSunmiUtils.cutPaperInner()
                if (woyouService != null) {
                    //   woyouService!!.sendRAWData(byteArrayOf(0x1B, 0x45, 0x01), this)
                } else {
                    val aa = ByteArray(5)

                    aa[0] = 0x10
                    aa[1] = 0x14
                    aa[2] = 0x00
                    aa[3] = 0x00
                    aa[4] = 0x00


                    try {
                        SunmiPrinterApi.getInstance().sendRawData(aa)
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                    }
                    /*  try {
                          SunmiPrintHelper.getInstance().openCashBox()
                      } catch (e: java.lang.Exception) {
                          e.printStackTrace()
                      }*/

                }


            } else {

                LogUtil.logE("SunmiPrintHelper", "isBlueToothPrinter")


                printByBluTooth("ABCDEFGHIJKLMNOPQRSTUVWXYZ")
            }

        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.CheckSunmiPrinter) {
            handler.postDelayed({ setService() }, 2000)
            LogUtil.logE("SunmiPrintHelper", "CheckSunmiPrinter")
        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.LostSunmiPrinter) {

            LogUtil.logE("SunmiPrintHelper", "LostSunmiPrinter")
        } else {
            LogUtil.logE("SunmiPrintHelper", "ELSE")
        }
    }

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, service: IBinder?) {
            LogUtil.logE(TAG, "onServiceConnected  1")
            woyouService = IWoyouService.Stub.asInterface(service)

        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            LogUtil.logE(TAG, "onServiceDisConnected  2")
            woyouService = null


        }

    }

    private fun Binding() {
        val intent = Intent()
        intent.setPackage("com.android.pos")
        intent.action = "com.android.pos.aidl.IWoyouService"
        MainApplication.getInstance()?.applicationContext?.bindService(
            intent,
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }


    private fun printByBluTooth(content: String) {
        try {
            if (true) {
                BluetoothUtil.sendData(ESCUtil.boldOn())
            } else {
                BluetoothUtil.sendData(ESCUtil.boldOff())
            }
            if (true) {
                BluetoothUtil.sendData(ESCUtil.underlineWithOneDotWidthOn())
            } else {
                BluetoothUtil.sendData(ESCUtil.underlineOff())
            }

            BluetoothUtil.sendData(content.toByteArray(charset("GB18030")))
            BluetoothUtil.sendData(ESCUtil.nextLine(3))
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun sunmiPrinterInit(ipAddress: String) {
        Log.e("checkSunmiPrinterIP","ipAddress: ${ipAddress}")

        SunmiPrinterApi.getInstance().setPrinter(SunmiPrinter.SunmiBlueToothPrinter, ipAddress)

        connect()

    }

    fun connect() {
        GlobalScope.launch(Dispatchers.IO) {
            if (!SunmiPrinterApi.getInstance().isConnected) {
                SunmiPrinterApi.getInstance()
                    .connectPrinter(requireContext(), object : ConnectCallback {

                        override fun onFound() {
                            println("onFound")
                        }

                        override fun onUnfound() {
                            println("onUnfound")
                        }

                        override fun onConnect() {
                            println("onConnect")
                            test()
                        }

                        override fun onDisconnect() {
                            println("onDisconnect")
                        }

                    })
            } else {
                test()
            }
        }

    }


    fun test() {
        try {
            if (SunmiPrinterApi.getInstance().isConnected) {
                SunmiPrinterApi.getInstance().printerInit()
                SunmiPrinterApi.getInstance().printText("")
                SunmiPrinterApi.getInstance().lineWrap(2)
                SunmiPrinterApi.getInstance().setAlignMode(1)
                SunmiPrinterApi.getInstance().setFontZoom(2, 2)
                SunmiPrinterApi.getInstance().printText("Test Print")
                SunmiPrinterApi.getInstance().lineWrap(1)
             /*   val current = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    LocalDateTime.now()
                } else {

                }*/
               // val formatter = DateTimeFormatter.ofPattern("MMM-dd-yyyy hh:mm:a")
              //  val formatted = current.format(formatter)
                SunmiPrinterApi.getInstance().setAlignMode(1)
                SunmiPrinterApi.getInstance().setFontZoom(2, 2)
                SunmiPrinterApi.getInstance()
                    .printText("Test Print")
                SunmiPrinterApi.getInstance().lineWrap(2)
                SunmiPrinterApi.getInstance().cutPaper(2, 20)
                LogUtil.logE(TAG, "WOHO SERIESNULL ${woyouService}")
                if (woyouService != null) {
                    LogUtil.logE(TAG, "WOHO SERIES NOT NULL")
                    // ToastUtil.showNormalToast(requireContext(), "Cash Drawer Connected..")
                    //  woyouService!!.sendRAWData(byteArrayOf(0x1B, 0x45, 0x01), this)
                } else {
                    val aa = ByteArray(5)

                    aa[0] = 0x10
                    aa[1] = 0x14
                    aa[2] = 0x00
                    aa[3] = 0x00
                    aa[4] = 0x00


                    try {
                        SunmiPrinterApi.getInstance().sendRawData(aa)
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                    }
                    /* try {
                         SunmiPrintHelper.getInstance().openCashBox()
                     } catch (e: java.lang.Exception) {
                         e.printStackTrace()
                     }*/
                }


            }
        } catch (e: Exception) {

        }
    }

    override fun onPrinterActive(printerListModel: PrinterListModel, layoutPosition: Int) {
        LogUtil.logE(TAG, "printerListModel: ${Gson().toJson(printerListModel)}")


        if (printerListModel.printerName?.startsWith("Cloud",true) == true && printerListModel.connectionType == WIFI){
            var bundle = Bundle()
            bundle.putParcelable("printerListModel",printerListModel)
            bundle.putInt("layoutPosition",layoutPosition)

            findNavController().navigate(R.id.action_printer_to_dialogSNumber,bundle)

        }
        else {

            if (prefProvider.getValueboolean(
                    IS_PRINTER_QUEUE_ENABLE,
                    false
                ) && prefProvider.getValueboolean(IS_MASTER_TERMINAL, false)
            ) {

                Log.e(TAG, "printerListModel:  ${Gson().toJson(printerListModel)}")

                var list: ArrayList<CreatePrinterRequestModel.PrinterSettingsAttributes> =
                    arrayListOf()

                if (printerListModel.printerName?.startsWith(
                        "Cloud",
                        true
                    ) == true && prefProvider.getValueboolean(
                        IS_MASTER_TERMINAL, false
                    ) && printerListModel.connectionType.equals(WIFI, true)
                ) {
                    ifKitchenPrinterSelected(list, printerListModel, layoutPosition)

                } else {

                    ifCustomerPrinterSelected(list, printerListModel, layoutPosition)

                }

            } else {
                setFragmentResultListener("request_printer_type") { requestKey: String, bundle: Bundle ->
                    val data = bundle.getString("type")
                    var list: ArrayList<CreatePrinterRequestModel.PrinterSettingsAttributes> =
                        arrayListOf()
                    LogUtil.logE(TAG, "getOrderTypeList: ${Gson().toJson(orderTypeList)}")
                    printerListModel.printerName?.let {
                        if (((it.contains("TSP", ignoreCase = true)) || (it.contains(
                                "SP",
                                ignoreCase = true
                            ))) && (data?.contains(CUSTOMER, ignoreCase = true) ?: true)
                        ) {
                            AlertUtils.showCustomAlertWithListenerWithOK(
                                requireContext(),
                                getString(R.string.incompatible_printer),
                                object : DialogInterface.OnClickListener {
                                    override fun onClick(p0: DialogInterface?, p1: Int) {
                                        p0?.dismiss()
                                    }

                                })
                        } else {
                            when (data) {
                                KITCHEN -> {
                                    ifKitchenPrinterSelected(
                                        list,
                                        printerListModel,
                                        layoutPosition
                                    )
                                }

                                CUSTOMER -> {
                                    ifCustomerPrinterSelected(
                                        list,
                                        printerListModel,
                                        layoutPosition
                                    )

                                }

                                KITCHENANDCUSTOMER -> {
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

                                    //ip address for bg printer
                                    //if (printerListModel.connectionType == WIFI) "TCP:" + printerListModel.deviceModel?.ipAddress else "BT:" + printerListModel.deviceModel?.ipAddress,

                                    val createBothPrinter = CreatePrinterRequestModel(
                                        name = printerListModel.printerName,
                                        terminalId = prefProvider.getValueInt(TERMINAL_ID, 0),
                                        macAddress = printerListModel.deviceModel?.macAddress,
                                        modalName = printerListModel.printerName,
                                        terminalIds = listOf(
                                            prefProvider.getValueInt(
                                                TERMINAL_ID,
                                                1
                                            )
                                        ),
                                        status = true,
                                        locationId = prefProvider.getValueInt(LOCATION_ID, 1),
                                        receiptPrintType = KITCHENANDCUSTOMER,
                                        printer_type = printerListModel.connectionType,
                                        ip_address = printerListModel.deviceModel?.ipAddress,
                                        printerSettingsAttributes = list
                                    )

                                    viewModel.createPrinter(createBothPrinter, showLoader = false)
                                    availableNetworkAdapter.removeItemAt(layoutPosition)

                                    /* Handler(Looper.getMainLooper()).postDelayed({
                                 syncPrinterList()
                             },1000)*/


                                }

                            }
                        }
                    }


                }
                findNavController().navigate(R.id.action_printer_to_printerTypeSelection)

            }
        }


        /* if (printerListModel.printerName == "TM-U220") {
             printerListModel.type = KITCHEN
             kitchenAdapter.addItem(printerListModel)
         } else {
             printerListModel.type = CUSTOMER
             customerAdapter.addItem(printerListModel)
         }*/
        syncPrinterList()
    }

    private fun ifKitchenPrinterSelected(
        list: ArrayList<CreatePrinterRequestModel.PrinterSettingsAttributes>,
        printerListModel: PrinterListModel,
        layoutPosition: Int,
        serialNumber: String?=null
    ) {
        for (i in 0 until orderTypeList.size) {
            list.add(
                CreatePrinterRequestModel.PrinterSettingsAttributes(
                    printType = KITCHEN,
                    orderTypeId = orderTypeList.get(i).id
                )
            )
        }

        //ip address for bg printer
        //if (printerListModel.connectionType == WIFI) "TCP:" + printerListModel.deviceModel?.ipAddress else "BT:" + printerListModel.deviceModel?.ipAddress
        Log.e("checkIPBEforeAdd","IpAdd:  ${printerListModel.deviceModel?.ipAddress}")

        val createPrinter = CreatePrinterRequestModel(
            name = printerListModel.printerName,
            terminalId = prefProvider.getValueInt(TERMINAL_ID, 0),
            macAddress = printerListModel.deviceModel?.macAddress,
            modalName = printerListModel.printerName,
            terminalIds = listOf(prefProvider.getValueInt(TERMINAL_ID, 1)),
            status = true,
            locationId = prefProvider.getValueInt(LOCATION_ID, 1),
            receiptPrintType = KITCHEN,
            printer_type = printerListModel.connectionType,
            ip_address = if(serialNumber?.length ?: 1 < 3) printerListModel.deviceModel?.ipAddress else serialNumber,
            printerSettingsAttributes = list,
            printerBrand = if (printerListModel.printerName?.startsWith("Cloud", true) == true) {
                Constants.SUNMIBRAND
            }/*else if (printerListModel.printerName?.contains("TSP",ignoreCase = true)==true){
                STAR
            }*/ else {
                EPSONBRAND
            },
            portNo = if (printerListModel.portNo != null && printerListModel.portNo != 0) printerListModel.portNo else 0

        )

        viewModel.createPrinter(createPrinter)
        availableNetworkAdapter.clearList()
//        availableNetworkAdapter.removeItemAt(layoutPosition)
        syncPrinterList()
    }

    private fun ifCustomerPrinterSelected(
        list: ArrayList<CreatePrinterRequestModel.PrinterSettingsAttributes>,
        printerListModel: PrinterListModel,
        layoutPosition: Int
    ) {
        for (i in 0 until orderTypeList.size) {
            list.add(
                CreatePrinterRequestModel.PrinterSettingsAttributes(
                    printType = CUSTOMER,
                    orderTypeId = orderTypeList.get(i).id
                )
            )

        }

        //ip address for bg printer
        //if (printerListModel.connectionType == WIFI) "TCP:" + printerListModel.deviceModel?.ipAddress else "BT:" + printerListModel.deviceModel?.ipAddress
        val createPrinter = CreatePrinterRequestModel(
            name = printerListModel.printerName,
            terminalId = prefProvider.getValueInt(TERMINAL_ID, 0),
            macAddress = printerListModel.deviceModel?.macAddress,
            modalName = printerListModel.printerName,
            terminalIds = listOf(prefProvider.getValueInt(TERMINAL_ID, 1)),
            status = true,
            locationId = prefProvider.getValueInt(LOCATION_ID, 1),
            receiptPrintType = CUSTOMER,
            printer_type = printerListModel.connectionType,
            ip_address = printerListModel.deviceModel?.ipAddress,
            printerSettingsAttributes = list

        )

        viewModel.createPrinter(createPrinter)
        availableNetworkAdapter.clearList()
//        availableNetworkAdapter.removeItemAt(layoutPosition)
        syncPrinterList()
    }

    override fun onEditSelected(printerListModel: PrinterListModel) {
        LogUtil.logE(TAG, "printerListModel:  ${Gson().toJson(printerListModel)}")
        var printerType: String = printerListModel.currentPrinterType ?: ""
        if (printerListModel.currentPrinterType == KITCHEN) {
            var modelPrinter = customerAdapter.getList()
                .find { it.modelName == printerListModel.modelName && it.deviceModel?.macAddress == printerListModel.deviceModel?.macAddress }
            Log.e(TAG, "modelPrinterfindCustomer:  ${Gson().toJson(modelPrinter)}")
            if (modelPrinter != null) {
                printerType = KITCHENANDCUSTOMER
            }
        } else if (printerListModel.currentPrinterType == CUSTOMER) {
            var modelPrinter = kitchenAdapter.getList()
                .find { it.modelName == printerListModel.modelName && it.deviceModel?.macAddress == printerListModel.deviceModel?.macAddress }
            Log.e(TAG, "modelPrinterfindKitchen:  ${Gson().toJson(modelPrinter)}")
            if (modelPrinter != null) {
                printerType = KITCHENANDCUSTOMER
            }
        }

        Log.e(TAG, "checkPrintTypeprinterType: ${printerType}")
        val bundle = Bundle()
        bundle.putParcelable("printerSetting", printerListModel)
        bundle.putString("currentPrinterType", printerType)

        findNavController().navigate(R.id.action_printer_to_editPrinter, bundle)
    }

    override fun onDeletePrinter(printerListModel: PrinterListModel) {

        if (printerListModel.type.lowercase() == KITCHENANDCUSTOMER.lowercase()) {
            if (printerListModel.currentPrinterType == KITCHEN) {
                Log.d("innerPrinterKitchen", "1st stage")
                deletePrinter(printerListModel, CUSTOMER)

            } else {
                deletePrinter(printerListModel, KITCHEN)

            }

        } else {
            deletePrinter(printerListModel)
        }
        syncPrinterList()
    }

    override fun onUpdatePrinterStatus(printerListModel: PrinterListModel, isChecked: Boolean) {

        viewModel.updatePrinterStatus(
            printerListModel.type,
            printerListModel.id!!,
            prefProvider.getValueInt(TERMINAL_ID, 1),
            isChecked
        )
        syncPrinterList()
    }

    override fun onUpdateKitchenPrinterStatus(
        printerListModel: PrinterListModel,
        isChecked: Boolean,
        type: String
    ) {
        viewModel.updatePrinterStatus(
            type,
            printerListModel.id!!,
            prefProvider.getValueInt(TERMINAL_ID, 1),
            isChecked
        )
        syncPrinterList()
    }

    override fun onUpdateCustomerPrinterStatus(
        printerListModel: PrinterListModel,
        isChecked: Boolean,
        type: String
    ) {
        viewModel.updatePrinterStatus(
            type,
            printerListModel.id!!,
            prefProvider.getValueInt(TERMINAL_ID, 1),
            isChecked
        )
        syncPrinterList()
    }

    private fun onInitPrinter(printerListModel: PrinterListModel) {
        //open
        // initPrinter(printerListModel)

        initOldPrinterMethod(printerListModel)

        //initNewPrinter(printerListModel)

    }

    private fun initOldPrinterMethod(printerListModel: PrinterListModel) {
        var builder: Builder? = null
        var method = ""

        builder = Builder(
            if (printerListModel.printerName?.substring(0, 6).toString()
                    .lowercase() == "TM-m30".lowercase()
            ) {
                "TM-m30"
            } else {
                printerListModel.printerName
            }, language, requireActivity()
        )

        builder.addFeedLine(2)



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
        builder.addText("Test Print")

        var mPrinter = if (printerListModel.printerName?.substring(0, 6).toString().lowercase()
                .contains("TM-m".lowercase())
        ) {
            Log.e(TAG, "YesContains")
            Printer(
                Printer.TM_M30,
                Printer.MODEL_ANK, (activity as MainActivity).applicationContext
            )
        } else {
            Printer(
                Printer.TM_U220,
                Printer.MODEL_ANK, (activity as MainActivity).applicationContext
            )


        }
        // Initialize the printer

        mPrinter.setReceiveEventListener { printer, i, printerStatusInfo, s ->

            Log.e(TAG, "PrinterEvent  ${Gson().toJson(printerStatusInfo)} other1 ${s}  other2 ${i}")
            if (printerStatusInfo.online == 1) {
                try {
                    printer.disconnect()

                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            }
        }
        try {

            var printerAdd =
                if (printerListModel.connectionType == BLUETOOTH) "BT:" + printerListModel.deviceModel?.macAddress else "TCP:" + printerListModel.deviceModel?.ipAddress
            Log.e("checkConnec", "${mPrinter.status.connection}")
            try {
                mPrinter.clearCommandBuffer()
                mPrinter.disconnect()


            } catch (e: java.lang.Exception) {
                try {
                    mPrinter.disconnect()
                } catch (e: java.lang.Exception) {

                }
                e.printStackTrace()
            }

            mPrinter.connect(
                printerAdd,
                Printer.PARAM_DEFAULT
            )
            mPrinter.startMonitor()

        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        mPrinter.addFeedLine(2)
        mPrinter.addTextFont(Builder.FONT_C)
        mPrinter.addTextAlign(Builder.ALIGN_CENTER)
        mPrinter.addTextLang(Builder.LANG_EN)
        mPrinter.addTextSize(1, 2)
        mPrinter.addTextStyle(
            Builder.FALSE,
            Builder.FALSE,
            Builder.FALSE,
            Builder.COLOR_1
        )
        mPrinter.addText("Test Print")
        mPrinter.addFeedLine(4)
        mPrinter.addCut(Builder.CUT_FEED)

        try {
            mPrinter.sendData(Printer.PARAM_DEFAULT)
            /*  try {
                  mPrinter.disconnect()
              } catch (e: java.lang.Exception) {
                  e.printStackTrace()
              }*/

        } catch (e: java.lang.Exception) {
            /* try {
                 mPrinter.disconnect()
             } catch (e: Exception) {
                 e.printStackTrace()
             }*/
            e.printStackTrace()
        }

        /*    mPrinter.endTransaction()
            mPrinter.disconnect()*/
        /*  Handler(Looper.getMainLooper()).postDelayed({

          }, 3000)
  */

    }

    private fun initNewPrinter(printerListModel: PrinterListModel) {
        LogUtil.logE(TAG, "printerListModel:  ${Gson().toJson(printerListModel)}")
        // Declare a global instance of Printer

        (activity as MainActivity).runOnUiThread {
            val mPrinter = Printer(
                Printer.TM_U220,
                Printer.MODEL_ANK, (activity as MainActivity).applicationContext
            ) // Initialize the printer


            /*Add a listener to your printer*/

            /* mPrinter.setReceiveEventListener { printer, i, printerStatusInfo, s ->

                 LogUtil.logE(
                     "PrinterDataCh",
                     "${Gson().toJson(printer)}   int: ${i}  printerInfo: ${
                         Gson().toJson(printerStatusInfo)
                     }  string: ${s}"
                 )
             }
 */
            /*  mPrinter.setReceiveEventListener(object : ReceiveListener {
                  override fun onPtrReceive(
                      p0: Printer?,
                      p1: Int,
                      p2: PrinterStatusInfo?,
                      p3: String?
                  ) {
                      Log.e("getPrintReceive", "online ${Gson().toJson(p2)}  data${p3}")
                      mPrinter.endTransaction()
                      mPrinter.disconnect()
                      *//*
                    mPrinter.addFeedLine(2)



                     mPrinter.addTextFont(Builder.FONT_C)
                     mPrinter.addTextAlign(Builder.ALIGN_CENTER)
                     mPrinter.addTextLang(Builder.LANG_EN)
                     mPrinter.addTextSize(1, 2)
                     mPrinter.addTextStyle(
                         Builder.FALSE,
                         Builder.FALSE,
                         Builder.FALSE,
                         Builder.COLOR_1
                     )
                     mPrinter.addText("Test Print")
                     mPrinter.beginTransaction()
                     mPrinter.sendData(Printer.PARAM_DEFAULT)
                     mPrinter.endTransaction()
                     mPrinter.disconnect()
                     *//*
                }

            })
*/

            runOnUiThread(Runnable {


                try {
                    mPrinter.connect(
                        printerListModel.deviceModel?.ipAddress,
                        Printer.PARAM_DEFAULT
                    )
                } catch (e: java.lang.Exception) {
                    try {
                        mPrinter.disconnect()
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                    }
                    LogUtil.logE(TAG, "PrinterConnectFailed")
                    e.printStackTrace()
                }

                mPrinter.addFeedLine(2)



                mPrinter.addTextFont(Builder.FONT_C)
                mPrinter.addTextAlign(Builder.ALIGN_CENTER)
                mPrinter.addTextLang(Builder.LANG_EN)
                mPrinter.addTextSize(1, 2)
                mPrinter.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                mPrinter.addText("Test Print")
                mPrinter.addFeedLine(2)
                mPrinter.addCut(Builder.CUT_FEED)

                try {
                    mPrinter.beginTransaction()
                    mPrinter.sendData(Printer.PARAM_DEFAULT)
                } catch (e: Exception) {
                    try {
                        mPrinter.disconnect()

                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                    }
                    e.printStackTrace()
                }


            })
            /*measureTimeMillis {
                runBlocking {

                }
            }*/
            /*lifecycleScope.launch {


                var result = mPrinter.connect(
                    printerListModel.deviceModel?.macAddress,
                    Printer.PARAM_DEFAULT
                )


*//*                mPrinter.addFeedLine(2)



                mPrinter.addTextFont(Builder.FONT_C)
                mPrinter.addTextAlign(Builder.ALIGN_CENTER)
                mPrinter.addTextLang(Builder.LANG_EN)
                mPrinter.addTextSize(1, t2)
                mPrinter.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                mPrinter.addText("Test Print")
                mPrinter.addFeedLine(2)
                mPrinter.addCut(Builder.CUT_FEED)
                mPrinter.beginTransaction()
                mPrinter.sendData(Printer.PARAM_DEFAULT)*//*
            }*/
            //mPrinter.endTransaction()
            //  mPrinter.disconnect()


            var builder: Builder? = null
            var method = ""

            builder = Builder(
                if (printerListModel.printerName?.substring(0, 6).toString()
                        .lowercase() == "TM-m30".lowercase()
                ) {
                    "TM-m30"
                } else {
                    printerListModel.printerName
                }, language, requireActivity()
            )

            builder.addFeedLine(2)



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
            builder.addText("Test Print")

        }

        /* var printer = Printer(Printer.TM_M30, Printer.MODEL_ANK, requireContext())

         var builder: Builder? = null
         builder = Builder("TM-m30", language, requireActivity())

         builder.addFeedLine(2)



         printer.addTextFont(Builder.FONT_C)
         printer.addTextAlign(Builder.ALIGN_CENTER)
         printer.addTextLang(Builder.LANG_EN)
         printer.addTextSize(1, 2)
         printer.addTextStyle(
             Builder.FALSE,
             Builder.FALSE,
             Builder.FALSE,
             Builder.COLOR_1
         )
         printer.addText("Test Print")


         printer.sendData(Printer.PARAM_DEFAULT)
 */

    }

    private fun initPrinter(printerListModel: PrinterListModel) {
        PrinterClass.setPrinter(null)
        Log.e(TAG, "getprinterListModel:  ${Gson().toJson(printerListModel)}")

        //new code logice for print receipt U220
        var printer = Printer(Printer.TM_U220, Printer.LANG_EN, requireContext())


        val builder = StringBuilder()
        builder.append("test content ")
        builder.append("\n")
        builder.append("test content ")
        builder.append("\n")
        printer.addText(builder.toString())
        /*Adds a sheet cut command "CUT THE SHEET" to the command buffer.*/
        /*Adds a sheet cut command "CUT THE SHEET" to the command buffer.*/
        printer.addCut(Printer.CUT_FEED)
        runOnUiThread(Runnable {
            printer.connect("TCP:" + printerListModel.deviceModel?.macAddress, Print.PARAM_DEFAULT)
            printer.beginTransaction()
            printer.sendData(Printer.PARAM_DEFAULT)
        })


        /*    var printer: Print? = Print(requireContext())
            if (printer != null) {
                printer.setStatusChangeEventCallback(this)
                printer.setBatteryStatusChangeEventCallback(this)
            }


            if (printerListModel.connectionType == "") {
                Log.e(TAG,"ConnectionEmpty")
                findBT()
                openBT()
            } else {

                Log.e(TAG,"ConnectionIsNotEmpty")



                ThreadPoolManager.instance.executeTask(Runnable {
                            try {
                            printer?.openPrinter(
                                if (printerListModel.connectionType == BLUETOOTH) Print.DEVTYPE_BLUETOOTH else Print.DEVTYPE_TCP,
                                printerListModel.deviceModel?.ipAddress
                            )
                                PrinterClass.setPrinter(printer)
                                LogUtil.logE(TAG, "printerListModel:  ${Gson().toJson(printerListModel)}")

                                //generateKitchenReceipt(printerListModel)
                                showPrinterStatus(printerListModel)
                            } catch (e: Exception) {
                                LogUtil.logE(TAG, "Exception:  " + e.message)
                                printer = null

                            }
                })





            }*/
    }

    private fun generateKitchenReceipt(printerListModel: PrinterListModel) {
        var builder: Builder? = null
        var method = ""

        try {
            LogUtil.logE(TAG, "printerName: ${printerListModel.printerName}")
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

            LogUtil.logE("builder", builder.toString())

            //send builder data(empty builder data)
            val status = IntArray(1)
            val battery = IntArray(1)

            LogUtil.logE(TAG, "getPrinterCheck:  ${PrinterClass.getPrinter().toString()}")

            try {
                PrinterClass.getPrinter()?.sendData(builder, SEND_TIMEOUT, status, battery)
                //PrinterClass.getPrinter()?.sendData(builder, 0, status, battery)
                //   PrinterClass.closePrinter()
            } catch (e: Exception) {
                PrinterClass.closePrinter()
                e.printStackTrace()
                LogUtil.logE(TAG, "PrinterError: " + e.localizedMessage)
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
            LogUtil.logE(TAG, "SUBSTR:  ${printerListModel.printerName?.substring(0, 6)}")
            builder = Builder(
                if (printerListModel.printerName?.substring(0, 6).toString()
                        .lowercase() == "TM-m30".lowercase()
                ) {
                    "TM-m30"
                } else {
                    printerListModel.printerName
                }, language, requireActivity()
            )

            builder.addFeedLine(2)
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
            builder.addText("Test Print")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                builder.addFeedLine(1)
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
                val current = LocalDateTime.now()
                val formatter = DateTimeFormatter.ofPattern("MMM-dd-yyyy hh:mm:a")
                val formatted = current.format(formatter)
                builder.addText(getCurrentTimeFromTimeZone(requireContext(), formatted))
            }



            builder.addFeedLine(2)


            builder.addCut(Builder.CUT_FEED)


            //builder.addFeedUnit(30)

            //send builder data(empty builder data)

            val status = IntArray(1)
            val battery = IntArray(1)


            LogUtil.logE(TAG, "getPrinterCheck:  ${PrinterClass.getPrinter().toString()}")



            try {
                PrinterClass.getPrinter()?.sendData(builder, 10000, status, battery)
                //PrinterClass.getPrinter()?.sendData(builder, 0, status, battery)
            } catch (e: Exception) {
                PrinterClass.closePrinter()
                e.printStackTrace()
                LogUtil.logE(TAG, "PrinterError: " + e.localizedMessage)
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
        LogUtil.logE(TAG, "onStatusChanged  ${p0}")

    }

    override fun onBatteryStatusChangeEvent(p0: String?, p1: Int) {
        LogUtil.logE(TAG, "onBatteryLevelChange  ${p0}")
    }

    @SuppressLint("MissingPermission")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {

            val myData: BluetoothDevice =
                data?.getParcelableArrayExtra(BluetoothDevice.EXTRA_NAME) as BluetoothDevice
            LogUtil.logE(TAG, "AvailableName   ${myData.name}")
        } else if (requestCode == 211) {
            val device =
                data!!.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            LogUtil.logE(TAG, "DeviceName  ${device!!.name}")
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

    private fun deletePrinter(printerListModel: PrinterListModel, type: String? = null) {
        Log.d("deletePrinter", "type = $type")
        Log.d("deletePrinter", "id = $id")
        Log.d("innerPrinterKitchen", "2nd stage")

        alert(
            getString(R.string.tv_pos),
            getString(R.string.delete_printer_message)
        ) {
            positiveButton(getString(R.string.tv_delete)) {
                if (type != null) {
                    Log.d("innerPrinterKitchen", "3rd stage")
                    viewModel.deletePrinter(printerListModel, type)
                    Log.d("deletePrinter", "delete type = $type")
                } else {
                    Log.d("innerPrinterKitchen", "4th stage")
                    viewModel.deletePrinter(printerListModel)
                    Log.d("deletePrinter", "delete id = $id")
                }

            }

            negativeButton(R.string.tv_cancel) {
                Log.d("innerPrinterKitchen", "5th stage")
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

    override fun asBinder(): IBinder {
        return woyouService?.asBinder()!!
    }

    override fun onRunResult(isSuccess: Boolean, code: Int, msg: String?) {

    }

    fun randomOfflineId(): String {

        val locationId = prefProvider.getValueInt(Constants.LOCATION_ID, -1).toString()
        val timestamp = System.currentTimeMillis().toString()
        val ss = locationId + timestamp.takeLast(4)
        val reqLent = 12 - ss.length
        val Alphabet = getSaltString(reqLent)
        val timeStampFinal = Alphabet + ss
        LogUtil.logE("timeStampFinal", timeStampFinal)

        return timeStampFinal
    }


    private fun hideLoaderAfterDelay() {
        binding.maskLayout?.gone()
    }

    override fun onFound(p0: CloudPrinter?) {
        Log.e("onFoundSunmiPrint","onFound:  ${Gson().toJson(p0)}")
        Log.e(
            TAG,
            "onFound:  ${Gson().toJson(p0)}  checkPrinterqueue  ${
                prefProvider.getValueboolean(
                    IS_PRINTER_QUEUE_ENABLE,
                    false
                )
            }"
        )
        if (prefProvider.getValueboolean(IS_PRINTER_QUEUE_ENABLE, false) == true || prefProvider.getValueboolean(IS_PRINTER_QUEUE_ENABLE, false) == false) {
            lifecycleScope.launch {


                Log.e(
                    TAG,
                    "sunmiCheckMasterTeminal  ${
                        prefProvider.getValueboolean(
                            IS_MASTER_TERMINAL,
                            false
                        )
                    }"
                )
                if ((prefProvider.getValueboolean(
                        IS_MASTER_TERMINAL,
                        false
                    ) == true || prefProvider.getValueboolean(
                        IS_MASTER_TERMINAL,
                        false
                    ) == false) && checkIsU220() == false
                ) {
                    var tmpInd = -1

                    for (i in 0 until availableNetworkAdapter.getList().size) {
                        val obj = availableNetworkAdapter.getList().get(i)
                        if (obj.printerName.equals(p0?.cloudPrinterInfo?.name, true)) {
                            tmpInd = i

                        }


                    }
                    if (tmpInd != -1) {
                        availableNetworkAdapter.removeItemAt(tmpInd)
                    }

                    delay(2000)

                    var contains: Boolean = false
                    kitchenAdapter.getList().forEach {
                        if (it.printerName.equals(p0?.cloudPrinterInfo?.name, true)) {
                            contains = true
                            return@forEach
                        }

                    }

                    var inAvail: Boolean = false
                    availableNetworkAdapter.getList().forEach {
                        if (it.printerName.equals(
                                p0?.cloudPrinterInfo?.name,
                                true
                            ) && it.connectionType?.equals(WIFI, true) == true
                        ) {
                            inAvail = true


                        }


                    }

                    if (contains == false && inAvail == false) {
                        // checkBluetoothPrinter(p0, availableNetworkAdapter.getList())
                        Log.e("checkAvailSusnf","fskhnklsf")
                        availableNetworkAdapter.addItem(
                            PrinterListModel(
                                printerName = p0?.cloudPrinterInfo?.name,
                                connectionType = WIFI,
                                deviceModel = DeviceInfo(
                                    DevType.TCP,
                                    p0?.cloudPrinterInfo?.name,
                                    p0?.cloudPrinterInfo?.name,
                                    p0?.cloudPrinterInfo?.address,
                                    p0?.cloudPrinterInfo?.address
                                ),
                                type = AVAILABLE,
                                uuid = UUID.randomUUID(),
                                modelName = p0?.cloudPrinterInfo?.name,
                                portNo = p0?.cloudPrinterInfo?.port

                            )
                        )
                    }

                }


            }
        }

    }

    private fun checkBluetoothPrinter(p0: CloudPrinter?, list: List<PrinterListModel>) {
        for (i in 0 until list.size) {

            val obj = list.get(i)

            Log.e(
                TAG,
                "macAddressObj:  ${obj.printerName}  macAddress  ${p0?.cloudPrinterInfo?.name}"
            )
            if (obj.printerName.equals(p0?.getCloudPrinterInfo()?.name, true) == true) {
                Log.e(TAG, "checkDataInside")
                availableNetworkAdapter.removeItem(obj)
            }

        }
        lifecycleScope.launch {
            delay(400)


        }
    }

    fun checkIsU220(): Boolean {
        if (kitchenPrintList.isNotEmpty() && kitchenPrintList.get(0).printerName.equals(
                "TM-U220",
                true
            )
        ) {
            return true
        } else {
            return false
        }
    }

    override fun updatePrinters() {
        Log.d("updatePrinters", "updatePrinters()")
        viewModel.updatePrinter()
    }

    override fun reloadAdapter() {
        syncPrinterList()
    }

    companion object {
        var updatePrinter: UpdatePrinters? = null
        lateinit var viewModelObject: PrinterViewModel

    }

    override fun onDestroy() {
        super.onDestroy()
        updatePrinter = null
    }

    fun observePrinterStatus() {
        viewModel.snackbarText.observe(viewLifecycleOwner, {
            AlertUtils.showCustomAlertWithListenerWithOK(
                requireContext(),
                it.getContentIfNotHandled().toString()
            ) { _, _ ->

            }


        })
    }

    class WrapContentLinearLayoutManager(context: Context, re: Int, reverse: Boolean) :
        LinearLayoutManager(context, re, reverse) {
        //... constructor
        override fun onLayoutChildren(recycler: Recycler, state: RecyclerView.State) {
            try {
                super.onLayoutChildren(recycler, state)
            } catch (e: IndexOutOfBoundsException) {
                Log.e("TAG", "meet a IOOBE in RecyclerView")
            }
        }
    }

    override fun onPtrReceive(p0: Printer?, p1: Int, p2: PrinterStatusInfo?, p3: String?) {
//        Printer receiver for TM-L100


    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Throws(
        NoSuchAlgorithmException::class,
        InvalidKeySpecException::class,
        InvalidKeyException::class,
        SignatureException::class
    )
    fun sign(
        body: String,
        appId: String,
        timestamp: String,
        nonce: String,
        rsaPrivateKey: String
    ): String {
        val content = body + appId + timestamp + nonce
        val keyBytes: ByteArray =
            java.util.Base64.getDecoder().decode(rsaPrivateKey.replace("(\\s)|(--.*--)".toRegex(), ""))
        val pkcs8KeySpec = PKCS8EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        val priKey = keyFactory.generatePrivate(pkcs8KeySpec)
        val signature: Signature = Signature.getInstance("SHA256withRSA")
        signature.initSign(priKey)
        signature.update(content.toByteArray())
        return java.util.Base64.getEncoder().encodeToString(signature.sign())
    }

    fun httpPost(path: String, body: String,sn:String?=null): String? {
        var connection: HttpURLConnection? = null
        var `is`: InputStream? = null
        var os: OutputStream? = null
        var br: BufferedReader? = null
        var result: String? = null

        val date = Date()
        val random = Random()
        val timestamp = String.format("%d", date.time / 1000)
        val nonce = String.format("%06d", random.nextInt(1000000))

        try {
            val url = URL("https://openapi.sunmi.com$path")
            connection = url.openConnection() as HttpURLConnection
            connection!!.requestMethod = "POST"
            connection!!.connectTimeout = 15000
            connection!!.readTimeout = 60000

            connection!!.doOutput = true
            connection!!.doInput = true
            connection!!.setRequestProperty("Sunmi-Appid", Constants.SUNMI_APP_ID)
            connection!!.setRequestProperty("Sunmi-Timestamp", timestamp)
            connection!!.setRequestProperty("Sunmi-Nonce", nonce)
            connection!!.setRequestProperty("Sunmi-Sign", generateSign(body, timestamp, nonce))
            connection!!.setRequestProperty("Source", "openapi")
            connection!!.setRequestProperty("Content-Type", "application/json")
            os = connection!!.outputStream
            os.write(body.toByteArray(charset("UTF-8")))
            if (connection!!.responseCode == 200) {

                /*  if (sn.equals("N434227FT0790")) {
                      orderContent.clear()
                       orderContent = java.lang.StringBuilder()
                      cloudQueuePrinting("N434227FT0738", 2241)
                  }*/

                /* if (path.contains("pushContent")){

                     CoroutineScope(Dispatchers.IO).launch {
                         delay(500)
                         clearPrintJob(sn)
                     }
                 }*/

                `is` = connection!!.inputStream
                br = BufferedReader(InputStreamReader(`is`, "UTF-8"))

                val sbf = StringBuffer()
                var temp: String? = null
                while ((br.readLine().also { temp = it }) != null) {
                    sbf.append(temp)
                    sbf.append("\n")
                }
                result = sbf.toString()
            }
        } catch (e: MalformedURLException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        } finally {
            if (br != null) {
                try {
                    br.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            if (os != null) {
                try {
                    os.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            if (`is` != null) {
                try {
                    `is`.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            connection!!.disconnect()
        }
        return result
    }

    fun bindShop(sn: String?, shop_id: Int): String? {
        val body = java.lang.StringBuilder()
        body.append("{")
        body.append(String.format("\"sn\":\"%s\"", sn))
        body.append(",")
        body.append(String.format("\"shop_id\":%d", shop_id))
        body.append("}")
        return httpPost("/v2/printer/open/open/device/bindShop", body.toString())
    }

    fun onlineStatus(sn: String?): String? {
        val body = java.lang.StringBuilder()
        body.append("{")
        body.append(String.format("\"sn\":\"%s\"", sn))
        body.append("}")
        return httpPost("/v2/printer/open/open/device/onlineStatus", body.toString())
    }

    fun pushContent(
        trade_no: String?,
        sn: String?,
        count: Int,
        order_type: Int,
        media_text: String?,
        cycle: Int
    ): String? {
        val body = java.lang.StringBuilder()
        body.append("{")
        body.append(String.format("\"trade_no\":\"%s\"", "${System.currentTimeMillis()}"))
        body.append(",")
        body.append(String.format("\"sn\":\"%s\"", sn))
        body.append(",")
        body.append(String.format("\"order_type\":%d", order_type))
        body.append(",")
        body.append(java.lang.String.format("\"content\":\"%s\"", orderContent.toString()))
        body.append(",")
        body.append(String.format("\"count\":%d", count))
        body.append(",")
        body.append(String.format("\"media_text\":\"%s\"", media_text))
        body.append(",")
        body.append(String.format("\"cycle\":%d", cycle))
        body.append("}")
        return httpPost("/v2/printer/open/open/device/pushContent", body.toString(),sn)
    }

    fun appendText(text: String) {
        try {
            val bytes = text.toByteArray(charset("UTF-8"))
            for (i in bytes) orderContent.append(String.format("%02x", i))
        } catch (e: UnsupportedEncodingException) {
        }
    }

    fun printAndExitPageMode() {
        orderContent.append("0c")
    }


    fun cutPaper(full_cut: Boolean) {
        orderContent.append("1d56" + (if ((full_cut)) "30" else "31"))
    }
    fun lineFeed(n: Int) {
        for (i in 0 until n) orderContent.append("0a")
    }


    fun setAlignment(n: Int) {
        if (n >= 0 && n <= 2) orderContent.append("1b61" + String.format("%02x", n))
    }

    fun clearPrintJob(sn: String?): String? {
        Log.e(TAG,"chekSNCall: ${sn}")
        val body = java.lang.StringBuilder()
        body.append("{")
        body.append(String.format("\"sn\":\"%s\"", sn))
        body.append("}")
        return httpPost("/v2/printer/open/open/device/clearPrintJob", body.toString(),sn)
    }


    // Append raw data.
    fun appendRawData(bytes: ByteArray) {
        for (i in bytes) orderContent.append(String.format("%02x", i))
    }

    // Append unicode character.
    fun appendUnicode(unicode: Int, count: Int) {
        if (count > 0) {
            val text = StringBuilder()
            for (i in 0 until count) text.append(unicode.toChar())
            appendText(text.toString())
        }

    }

    // [ESC 3] Set line spacing.
    fun setLineSpacing(n: Int) {
        if (n >= 0 && n <= 255) orderContent.append("1b33" + String.format("%02x", n))
    }

    // [ESC !] Set print modes.
    fun setPrintModes(bold: Boolean, double_h: Boolean, double_w: Boolean) {
        var n = 0
        if (bold) n = n or 8
        if (double_h) n = n or 16
        if (double_w) n = n or 32
        charHSize = if ((double_w)) 2 else 1
        orderContent.append("1b21" + String.format("%02x", n))
    }

    // [HT] Jump to next TAB position.
    fun horizontalTab(n: Int) {
        for (i in 0 until n) orderContent.append("09")
    }

    // [ESC $] Set absolute print position.
    fun setAbsolutePrintPosition(n: Int) {
        if (n >= 0 && n <= 65535) orderContent.append(
            "1b24" + String.format(
                "%02x%02x",
                (n and 0xff),
                ((n shr 8) and 0xff)
            )
        )
    }

    // [ESC \] Set relative print position.
    fun setRelativePrintPosition(n: Int) {
        if (n >= -32768 && n <= 32767) orderContent.append(
            "1b5c" + String.format(
                "%02x%02x",
                (n and 0xff),
                ((n shr 8) and 0xff)
            )
        )
    }


    // [ESC -] Set underline mode.
    fun setUnderlineMode(n: Int) {
        if (n >= 0 && n <= 2) orderContent.append("1b2d" + String.format("%02x", n))
    }

    // [GS B] Set black-white reverse mode.
    fun setBlackWhiteReverseMode(enabled: Boolean) {
        orderContent.append("1d42" + (if ((enabled)) "01" else "00"))
    }

    // [ESC {] Set upside down mode.
    fun setUpsideDownMode(enabled: Boolean) {
        orderContent.append("1b7b" + (if ((enabled)) "01" else "00"))
    }

    @Throws(java.lang.Exception::class)
    fun generateSign(body: String, timestamp: String, nonce: String): String {

        val msg = body + "889a389072224d10b641e90b9cc26856" + timestamp + nonce
        val hmacSha256 = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec("1f486ca8d9a341408c8132b23f82f571".encodeToByteArray(), "HmacSHA256")
        hmacSha256.init(secretKey)
        val result = hmacSha256.doFinal(msg.toByteArray(charset("UTF-8")))
        return bytesToHexString(result)
    }

    fun bytesToHexString(bytes: ByteArray): String {
        val hexstr = java.lang.StringBuilder()
        for (i in bytes) hexstr.append(String.format("%02x", i))
        return hexstr.toString()
    }


    fun setCharacterSize(h: Int, w: Int) {
        var n = 0
        if (h >= 1 && h <= 8) n = n or (h - 1)
        if (w >= 1 && w <= 8) {
            n = n or ((w - 1) shl 4)
            charHSize = w
        }
        orderContent.append("1d21" + String.format("%02x", n))
    }


}