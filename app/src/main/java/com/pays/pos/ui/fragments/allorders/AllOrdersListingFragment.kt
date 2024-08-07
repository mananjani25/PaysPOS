package com.pays.pos.ui.fragments.allorders

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.epson.epos2.printer.Printer
import com.epson.eposprint.Builder
import com.epson.eposprint.Print
import com.epson.eposprint.StatusChangeEventListener
import com.google.gson.Gson
import com.pays.pos.R
import com.pays.pos.data.entities.*
import com.pays.pos.data.model.CancelOnlineWebOrderModel
import com.pays.pos.data.model.requestModel.OrderItemVariationAttribute
import com.pays.pos.data.model.requestModel.RefundRequestModelOnlineOrder
import com.pays.pos.data.model.responseModel.*
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.ALL_ORDER_TAB
import com.pays.pos.data.remote.Constants.EMPLOYEE_NAME
import com.pays.pos.data.remote.Constants.IS_FROM_ALL_ORDER
import com.pays.pos.data.remote.Constants.OLD_ITEM_BASE_CUSTOM_ITEM
import com.pays.pos.data.remote.Constants.ONLINE_ORDER_TAB
import com.pays.pos.data.remote.Constants.OPEN_ORDER_TAB
import com.pays.pos.data.remote.Constants.ORDER_NUMBER_STARTING_FROM_ONE
import com.pays.pos.data.remote.Constants.ORDER_TYPE
import com.pays.pos.data.remote.Constants.PHONE_ORDER_TAB
import com.pays.pos.data.remote.Constants.SUNMI_PRINTER
import com.pays.pos.data.remote.Constants.THIRD_PARTY_ORDER_TAB
import com.pays.pos.databinding.AllOrdersListingFragmentBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.di.RolePermission
import com.pays.pos.logger.MessageEvent
import com.pays.pos.ui.adapter.AllOrderAdapter
import com.pays.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.pays.pos.ui.fragments.onlineorder.OnlineDetailViewModel
import com.pays.pos.ui.fragments.orders.ActiveOrderViewModel
import com.pays.pos.ui.fragments.settings.hardware.printer.BluetoothUtil
import com.pays.pos.ui.fragments.settings.hardware.printer.SunmiPrintHelper
import com.pays.pos.utils.*
import com.pays.pos.utils.callback.OrderCallBack
import com.pays.pos.utils.extensions.*
import com.pays.pos.utils.printer.PrinterClass
import com.pays.pos.utils.statusUtils.Status
import com.starmicronics.stario10.InterfaceType
import com.starmicronics.stario10.StarConnectionSettings
import com.starmicronics.stario10.StarPrinter
import com.starmicronics.stario10.StarSpoolJobSettings
import com.starmicronics.stario10.starxpandcommand.DocumentBuilder
import com.starmicronics.stario10.starxpandcommand.MagnificationParameter
import com.starmicronics.stario10.starxpandcommand.PrinterBuilder
import com.starmicronics.stario10.starxpandcommand.StarXpandCommandBuilder
import com.starmicronics.stario10.starxpandcommand.printer.Alignment
import com.starmicronics.stario10.starxpandcommand.printer.CutType
import com.starmicronics.stario10.starxpandcommand.printer.InternationalCharacterType
import com.sunmi.externalprinterlibrary.api.ConnectCallback
import com.sunmi.externalprinterlibrary.api.SunmiPrinter
import com.sunmi.externalprinterlibrary.api.SunmiPrinterApi
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.lang.Runnable
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject
import kotlin.math.abs


// This fragment contains listing of orders based on selected order types and order status
@AndroidEntryPoint
class AllOrdersListingFragment(
    var orderStatus: String,
    var orderStatusLabel: String,
    var startDateTime: String?,
    var endDateTime: String?,
    var orderTab: String,
    var orderTabTypeId: String,
) : Fragment(),
    OrderCallBack, StatusChangeEventListener {

    private var isPrintCustomer = false
    var ordertypelist: ArrayList<TbOrderType> = arrayListOf()
    private var isEmployeeAtoZ: Boolean = false
    private var isStationAtoZ: Boolean = false
    private var isPrint: Boolean = true
    private val viewModel by viewModels<AllOrdersViewModel>()
    private val ordersViewModel by activityViewModels<AllOrdersViewModel>()
    private val dashboardViewModel by activityViewModels<DashBoardCategoryViewModel>()
    private val onlineDetailViewModel by activityViewModels<OnlineDetailViewModel>()
    private val activeOrderViewModel by viewModels<ActiveOrderViewModel>()
    lateinit var binding: AllOrdersListingFragmentBinding
    private lateinit var refundData: RefundRequestModelOnlineOrder
    private lateinit var startDate: DatePickerDialog.OnDateSetListener
    private lateinit var endDate: DatePickerDialog.OnDateSetListener
    private lateinit var startTime: TimePickerDialog.OnTimeSetListener
    private lateinit var endTime: TimePickerDialog.OnTimeSetListener
    private lateinit var adapter: AllOrderAdapter
    var myCalendar = Calendar.getInstance()
    var myCalendar1 = Calendar.getInstance()
    val myCalendar2 = Calendar.getInstance()
    val myCalendar3 = Calendar.getInstance()
    private val TAG = "AllOrdersListingFrag"
    private var kitchenSettingModel = GetKitchenReceiptSettingsResponse.Data()
    private var customerSettingModel = GetCustomerReceiptSettingsResponse.Data()
    private var tipsList: List<GetTipReponse.Data> = listOf()

    /*Star label printer - START*/
    lateinit var settings: StarConnectionSettings
    lateinit var printer: StarPrinter
    /*Star label printer - END*/

    private var oneItemPerReceipt: Boolean = true

    var removedPos = 0

    @Inject
    lateinit var prefProvider: PrefProvider

    @Inject
    lateinit var rolePermission: RolePermission
    var broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            var refresh = intent?.getBooleanExtra("refresh", false)
            if (refresh == true) {
//
//
//                val navController = findNavController()
//
//                val currentDestinationId = navController.currentDestination?.id
//                currentDestinationId?.let { navController.popBackStack(it, false) }
//                currentDestinationId?.let { navController.navigate(it) }

                dashboardViewModel.refreshLiveData.value = true

                adapter.orderList.clear()
                adapter.filterList.clear()
                getAllOrders()
            }

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        requireActivity().unregisterReceiver(broadcastReceiver)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapter()

        onlineDetailViewModel.refresh.observe(viewLifecycleOwner,
            object : androidx.lifecycle.Observer<Boolean> {
                override fun onChanged(t: Boolean?) {
                    t?.let {
                        if (it) {
                            refreshCurrentFragment()
                        }
                    }

                }
            })
        adapter.enableReprintKitchenReceiptButton()

        observeShowProgress()
        requireActivity().registerReceiver(
            broadcastReceiver,
            IntentFilter(Constants.ONLINE_ORDER_REFRESH)
        )
        binding.txtOrderWillAppear.text = "$orderStatusLabel order will appear here."

        searchFilter()
        getAllOrders()
        setupEmployeeSort()
        setupStationSort()

        if (orderTab == OPEN_ORDER_TAB) {
            binding.lblDelivery.gone()
        } else {
            binding.lblDelivery.visible()
        }

        requireActivity().supportFragmentManager.setFragmentResultListener(
            "request_key_time",
            viewLifecycleOwner
        ) { requestKey: String, bundle: Bundle ->
            var time = bundle.getInt("time")
            var order_id = bundle.getInt("order_id")
            removedPos = order_id
            acceptedAndDeclineOrder("", time, order_id, true)
        }

        requireActivity().supportFragmentManager.setFragmentResultListener(
            "request_for_rejectOrder",
            viewLifecycleOwner
        ) { requestKey: String, bundle: Bundle ->
            var order_id = bundle.getInt("order_id")
            acceptedAndDeclineOrder("", 0, order_id, false)
        }
    }

    // to sort order's listing based on stations
    private fun setupStationSort() {
        binding.lnrStationSort.setOnClickListener {
            if (isStationAtoZ) {
                binding.imgIndicatorStation.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        binding.root.resources,
                        R.drawable.ic_arrow_drop_down,
                        binding.root.resources.newTheme()
                    )
                )
                val filteredList = adapter.orderList.sortedBy { it.terminalId }
                adapter.add(filteredList.toCollection(arrayListOf()), orderTab)
            } else {
                binding.imgIndicatorStation.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        binding.root.resources,
                        R.drawable.ic_solid_up_arrow,
                        binding.root.resources.newTheme()
                    )
                )
                val filteredList = adapter.orderList.sortedByDescending { it.terminalId }
                adapter.add(filteredList.toCollection(arrayListOf()), orderTab)
            }
            binding.imgIndicatorStation.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.white
                )
            )
            isStationAtoZ = !isStationAtoZ
        }
    }

    // to sort order's listing based on employees
    private fun setupEmployeeSort() {
        binding.lnrEmployeeSort.setOnClickListener {
            if (isEmployeeAtoZ) {
                binding.imgIndicatorEmployee.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        binding.root.resources,
                        R.drawable.ic_arrow_drop_down,
                        binding.root.resources.newTheme()
                    )
                )
                val filteredList = adapter.orderList.sortedBy { it.employee?.firstName }
                adapter.add(filteredList.toCollection(arrayListOf()), orderTab)

            } else {
                binding.imgIndicatorEmployee.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        binding.root.resources,
                        R.drawable.ic_solid_up_arrow,
                        binding.root.resources.newTheme()
                    )
                )
                val filteredList = adapter.orderList.sortedByDescending { it.employee?.firstName }
                adapter.add(filteredList.toCollection(arrayListOf()), orderTab)
            }
            binding.imgIndicatorEmployee.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.white
                )
            )
            isEmployeeAtoZ = !isEmployeeAtoZ
        }
    }

    // To accept/decline online or phone orders
    private fun acceptedAndDeclineOrder(
        pax_data: String,
        time: Int,
        orderId: Int,
        is_accepted: Boolean
    ) {
        if (is_accepted) {
            makeAcceptedDeclinedServerCall(time, orderId, is_accepted)
        } else {
            if (pax_data.isNotEmpty()) {
                var CUST_NBR = ""
                var MERCH_NBR = ""
                var DBA_NBR = ""
                var TERMINAL_NBR = ""
                var TRAN_TYPE = "CCE7"
                var BATCH_ID = ""
                var TRAN_NBR = ""
                var ORIG_AUTH_GUID = ""
                var CARD_ENT_METH = ""
                var AMOUNT = ""
                var AUTH_GUID = ""

                var key = ""
                var value = ""
                val factory: XmlPullParserFactory = XmlPullParserFactory.newInstance()
                factory.setNamespaceAware(true)
                val xpp: XmlPullParser = factory.newPullParser()

                xpp.setInput(StringReader(pax_data))
                var eventType = xpp.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_DOCUMENT) {
                        println("Start document")
                    } else if (eventType == XmlPullParser.START_TAG) {

                        try {
                            key = xpp.getAttributeValue(0)
                        } catch (e: Exception) {
                            key = ""
                        }
                    } else if (eventType == XmlPullParser.END_TAG) {

                    } else if (eventType == XmlPullParser.TEXT) {
                        println("Text " + xpp.text)
                        if (!value.equals(xpp.text)) {
                            value = xpp.text
                        }
                    }

                    if (key.equals("CUST_NBR")) {
                        CUST_NBR = value
                    } else if (key.equals("MERCH_NBR")) {
                        MERCH_NBR = value
                    } else if (key.equals("DBA_NBR")) {
                        DBA_NBR = value
                    } else if (key.equals("TERMINAL_NBR")) {
                        TERMINAL_NBR = value
                    } else if (key.equals("BATCH_ID")) {
                        BATCH_ID = value
                    } else if (key.equals("TRAN_NBR")) {
                        TRAN_NBR = value
                    } else if (key.equals("AUTH_GUID")) {
                        AUTH_GUID = value
                    } else if (key.equals("AUTH_AMOUNT")) {
                        AMOUNT = value
                    }

                    eventType = xpp.next()
                }


                CoroutineScope(Dispatchers.IO).launch {
                    val queue = Volley.newRequestQueue(requireContext())
                    var url = ""
                    if (Constants.isPaxInDebugMode) {
                        url = Constants.paxDebug
                    } else {
                        url = Constants.paxLive
                    }
                    val getRequest: StringRequest = object : StringRequest(
                        Request.Method.POST, url,
                        object : Response.Listener<String?> {
                            override fun onResponse(response: String?) {
                                // response
                                var AUTH_RESP_TEXT = ""
                                Log.d("Response", response!!)
                                xpp.setInput(StringReader(response))
                                var eventType = xpp.eventType
                                while (eventType != XmlPullParser.END_DOCUMENT) {
                                    if (eventType == XmlPullParser.START_DOCUMENT) {
                                        println("Start document")
                                    } else if (eventType == XmlPullParser.START_TAG) {

                                        try {
                                            key = xpp.getAttributeValue(0)
                                        } catch (e: Exception) {
                                            key = ""
                                        }
                                    } else if (eventType == XmlPullParser.END_TAG) {

                                    } else if (eventType == XmlPullParser.TEXT) {
                                        println("Text " + xpp.text)
                                        if (!value.equals(xpp.text)) {
                                            value = xpp.text
                                        }
                                    }

                                    if (key.equals("AUTH_RESP_TEXT")) {
                                        AUTH_RESP_TEXT = value
                                    }

                                    eventType = xpp.next()
                                }

                                if (AUTH_RESP_TEXT.contains("UNABLE")) { // batch closed and need to call refund
                                    /*Make refund Call*/
                                    makeRefundCallToNAB(
                                        xpp,
                                        time,
                                        orderId,
                                        is_accepted,
                                        CUST_NBR,
                                        MERCH_NBR,
                                        DBA_NBR,
                                        TERMINAL_NBR,
                                        TRAN_TYPE,
                                        BATCH_ID,
                                        TRAN_NBR,
                                        AMOUNT,
                                        AUTH_GUID
                                    )
                                } else if (AUTH_RESP_TEXT.contains("APPROVAL")) { // void and refund success
                                    /*Make server call*/
                                    ProgressUtils.dismissProgressDialog()
                                    makeAcceptedDeclinedServerCall(time, orderId, is_accepted)
                                }

                            }
                        },
                        object : Response.ErrorListener {
                            override fun onErrorResponse(error: VolleyError) {
                                // TODO Auto-generated method stub
                                Log.d("ERROR", "error => $error")
                            }
                        }
                    ) {
                        @Throws(AuthFailureError::class)
                        override fun getHeaders(): Map<String, String> {
                            val params: MutableMap<String, String> = HashMap()
                            params["Accept"] = "*/*"
                            params["Cache-Control"] = "no-cache"
                            params["Host"] = "secure.epxuap.com"
                            params["Accept-Encoding"] = "gzip, deflate, br"
                            params["Connection"] = "keep-alive"
                            params["Content-Type"] = "application/x-www-form-urlencoded"
                            return params
                        }

                        @Throws(AuthFailureError::class)
                        override fun getParams(): Map<String, String>? {
                            val params: MutableMap<String, String> = HashMap()
                            params["CUST_NBR"] = CUST_NBR
                            params["MERCH_NBR"] = MERCH_NBR
                            params["DBA_NBR"] = DBA_NBR
                            params["TERMINAL_NBR"] = TERMINAL_NBR
                            params["TRAN_TYPE"] = "CCE7"
                            params["BATCH_ID"] = BATCH_ID
                            params["TRAN_NBR"] = TRAN_NBR
                            params["CARD_ENT_METH"] = "Z"
                            params["INDUSTRY_TYPE"] = "E"
                            params["ORIG_AUTH_GUID"] = ORIG_AUTH_GUID
                            return params
                        }
                    }
                    queue.add(getRequest)
                    withContext(Dispatchers.Main) {
                        ProgressUtils.showProgressDialog(requireActivity())
                    }
                }
            }
        }


    }

    private fun makeAcceptedDeclinedServerCall(time: Int, orderId: Int, is_accepted: Boolean) {
        var employee_id = prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
        var terminal_id = prefProvider.getValueInt(Constants.TERMINAL_ID, 0)
        viewModel.acceptedAndDeclineOrder(
            time,
            orderId,
            is_accepted,
            employee_id,
            terminal_id
        ).observe(requireActivity()) { it ->

            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        getAllOrders()
//                        if (!is_accepted) {
                        if (removedPos > 0) {
                            /*   val orderListIndex = adapter.orderList.indexOfFirst{
                                   it.id == removedPos
                               }
*/

                            ordersViewModel.refreshOrderCount.value = true

                            val filterListIndex = adapter.filterList.indexOfFirst {
                                it.id == removedPos
                            }
                            adapter.filterList.removeAt(filterListIndex)

                            adapter.notifyDataSetChanged()
                            removedPos = 0
                        }
//                        }

                        ProgressUtils.dismissProgressDialog()
                        resource.data?.let {
                            LogUtil.logE(TAG, "getREsponseForOnline  ${Gson().toJson(it)}")
                            removedPos = 0
                            if (it.data.orderItems.isNotEmpty()) {
                                getKitchenPrinters(it.data)
                            }
                        }
                    }

                    Status.ERROR -> {
                        removedPos = 0
                        ProgressUtils.dismissProgressDialog()
                        binding.root.showAlert(resource.message)

                    }

                    Status.LOADING -> {
//                        ProgressUtils.showProgressDialog(requireActivity())
                    }
                }
            }
        }
    }

    fun makeRefundCallToNAB(
        xpp: XmlPullParser,
        time: Int,
        orderId: Int,
        is_accepted: Boolean,
        CUST_NBR: String,
        MERCH_NBR: String,
        DBA_NBR: String,
        TERMINAL_NBR: String,
        TRAN_TYPE: String,
        BATCH_ID: String,
        TRAN_NBR: String,
        AMOUNT: String,
        ORIG_AUTH_GUID: String
    ) {
        val queue = Volley.newRequestQueue(requireContext())
        var url = ""
        if (Constants.isPaxInDebugMode) {
            url = Constants.paxDebug
        } else {
            url = Constants.paxLive
        }
        val getRequest: StringRequest = object : StringRequest(
            Request.Method.POST, url,
            object : Response.Listener<String?> {
                override fun onResponse(response: String?) {
                    // response
                    Log.d("Response", response!!)
                    var AUTH_RESP_TEXT = ""
                    var key = ""
                    var value = ""

                    Log.d("Response", response!!)
                    xpp.setInput(StringReader(response))
                    var eventType = xpp.eventType
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_DOCUMENT) {
                            println("Start document")
                        } else if (eventType == XmlPullParser.START_TAG) {

                            try {
                                key = xpp.getAttributeValue(0)
                            } catch (e: Exception) {
                                key = ""
                            }
                        } else if (eventType == XmlPullParser.END_TAG) {

                        } else if (eventType == XmlPullParser.TEXT) {
                            println("Text " + xpp.text)
                            if (!value.equals(xpp.text)) {
                                value = xpp.text
                            }
                        }

                        if (key.equals("AUTH_RESP_TEXT")) {
                            AUTH_RESP_TEXT = value
                        }

                        eventType = xpp.next()
                    }

                    if (!AUTH_RESP_TEXT.contains("APPROVAL")) {


                        /*if (!(context as AppCompatActivity).isFinishing()) {
                            requireActivity().runOnUiThread(object:Runnable{
                                override fun run() {
                                    AlertUtils.showAlert(requireActivity(),AUTH_RESP_TEXT)
                                }

                            })

                        }*/
                        /* else{
                            try{
                                AlertUtils.showAlert(requireActivity(),AUTH_RESP_TEXT)

                            }catch (e:Exception){
                                try{
                                    AlertUtils.showAlert(requireContext(),AUTH_RESP_TEXT)
                                }catch (e:Exception){

                                }
                            }
                        }*/
                    }

                    makeAcceptedDeclinedServerCall(time, orderId, is_accepted)


                }
            },
            object : Response.ErrorListener {
                override fun onErrorResponse(error: VolleyError) {
                    // TODO Auto-generated method stub
                    Log.d("ERROR", "error => $error")
                }
            }
        ) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val params: MutableMap<String, String> = HashMap()
                params["Accept"] = "*/*"
                params["Cache-Control"] = "no-cache"
                params["Host"] = "secure.epxuap.com"
                params["Accept-Encoding"] = "gzip, deflate, br"
                params["Connection"] = "keep-alive"
                params["Content-Type"] = "application/x-www-form-urlencoded"
                return params
            }

            @Throws(AuthFailureError::class)
            override fun getParams(): Map<String, String>? {
                val params: MutableMap<String, String> = HashMap()
                params["CUST_NBR"] = CUST_NBR
                params["MERCH_NBR"] = MERCH_NBR
                params["DBA_NBR"] = DBA_NBR
                params["TERMINAL_NBR"] = TERMINAL_NBR
                params["TRAN_TYPE"] = "CCE9"
                params["BATCH_ID"] = BATCH_ID
                params["TRAN_NBR"] = TRAN_NBR
                params["CARD_ENT_METH"] = "Z"
                params["AMOUNT"] = AMOUNT
                params["ORIG_AUTH_GUID"] = ORIG_AUTH_GUID
                params["INDUSTRY_TYPE"] = "E"
                return params
            }
        }
        queue.add(getRequest)


    }

    fun getValueFromXml(
        xmlString: String,
        key: String
    ): kotlin.collections.HashMap<String, String>? {

        var dataList = HashMap<String, String>()
        var key = ""
        var value = ""
        val factory: XmlPullParserFactory = XmlPullParserFactory.newInstance()
        factory.setNamespaceAware(true)
        val xpp: XmlPullParser = factory.newPullParser()

        xpp.setInput(StringReader(xmlString))
        var eventType = xpp.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_DOCUMENT) {
                println("Start document")
            } else if (eventType == XmlPullParser.START_TAG) {

                try {
                    key = xpp.getAttributeValue(0)
                } catch (e: Exception) {
                    key = ""
                }
            } else if (eventType == XmlPullParser.END_TAG) {

            } else if (eventType == XmlPullParser.TEXT) {
                println("Text " + xpp.text)
                if (!value.equals(xpp.text)) {
                    value = xpp.text
                }
            }

            dataList.put(key, value)
            eventType = xpp.next()
        }

        return dataList

    }

    // To update order
    private fun updateOrder(orderId: Int, order_status: String) {
        viewModel.updateOnlineOrder(
            orderId,
            order_status
        ).observe(viewLifecycleOwner) { it ->

            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        resource.data?.let {
                            getAllOrders()
                        }

                        ordersViewModel.refreshOrderCount.value = true

                        // Remove Completed order from list
                        adapter.filterList.removeIf { it.id == orderId }
                        adapter.notifyDataSetChanged()
                    }

                    Status.ERROR -> {
                        ProgressUtils.dismissProgressDialog()
                        binding.root.showAlert(resource.message)

                    }

                    Status.LOADING -> {
                        ProgressUtils.showProgressDialog(requireActivity())
                    }
                }
            }
        }
    }

    // Fetch all orders from api
    private fun getAllOrders() {

        var paymentStatus: String = when (orderStatusLabel) {
            "Pending", "InProgress" -> {
                "Unpaid"
            }

            "Completed" -> {
                "Paid"
            }

            "Rejected" -> {
                "Cancelled"
            }

            else -> {
                ""
            }
        }

        if (orderTab == ALL_ORDER_TAB && orderStatusLabel == "InProgress") {
            paymentStatus = ""
        }

//        if (orderTab == OPEN_ORDER_TAB || orderTab == PHONE_ORDER_TAB) {
//            orderStatusLabel = ""
//        }

        //Added to reflect online orders and web orders in same tab


//         /*   runBlocking {
//                val res =
//                    CoroutineScope(Dispatchers.IO).async { dashboardViewModel.getAllOrderTypes() }
//                        .await()
//
//                if (orderTabTypeId.isNotEmpty()) {
//                    runBlocking {
//
//                        if (res.isNotEmpty()) {
//
//                            val found =
//                                res.filter { it.id == orderTabTypeId.toInt() && (it.orderType == ONLINE_ORDER_TAB || it.orderType == THIRD_PARTY_ORDER_TAB) }
//
//                            if (found.isNotEmpty()) {
//                                val list =
//                                    res.filter { it.orderType == ONLINE_ORDER_TAB || it.orderType == THIRD_PARTY_ORDER_TAB }
//                                orderTabTypeId = "[${list[0].id},${list[1].id}]"
//                            }
//                        }
//                    }
//
//                } else {
//                    if (orderTab == PHONE_ORDER_TAB) {
//                        val phoneOrderId = res.filter { it -> it.orderType == PHONE_ORDER_TAB }
//                        orderTabTypeId =
//                            "" + if (phoneOrderId.isNotEmpty()) phoneOrderId.first().id else ""
//                    }
//                }
//            }*/

        try {
            runBlocking {
                val res =
                    withContext(CoroutineScope(Dispatchers.IO).coroutineContext) { dashboardViewModel.getAllOrderTypes() }

                if (orderTabTypeId.isNotEmpty()) {
                    val relevantOrders =
                        res.filter { it.orderType == ONLINE_ORDER_TAB || it.orderType == THIRD_PARTY_ORDER_TAB }
                    val found = relevantOrders.find { it.id == orderTabTypeId.toInt() }

                    if (found != null) {
                        val listIds =
                            relevantOrders.joinToString(separator = ",") { it.id.toString() }
                        orderTabTypeId = "[$listIds]"
                    }
                } else {
                    if (orderTab == PHONE_ORDER_TAB) {
                        val phoneOrderId = res.find { it.orderType == PHONE_ORDER_TAB }
                        orderTabTypeId = phoneOrderId?.id.toString()
                    }
                }
            }



            viewModel.getAllOrders(
                viewModel.startDate.value.toString(),
                viewModel.endDate.value.toString(),
                orderStatusLabel,
                paymentStatus,
                orderTabTypeId
            ).observe(viewLifecycleOwner) { it ->


                Log.d("08JUNE23", "getAllOrders response: CALLED")
                it?.let { resource ->
                    when (resource.status) {
                        Status.SUCCESS -> {
                            ProgressUtils.dismissProgressDialog()
                            resource.data?.let {

                                if (it.data.isNotEmpty()) {
                                    when (orderTab) {
                                        ALL_ORDER_TAB -> {
                                            binding.tvOrderType.visible()
                                            binding.tvOrderStatus.visible()
                                        }

                                        Constants.ONLINE_ORDER_TAB, Constants.THIRD_PARTY_ORDER_TAB -> {
                                            binding.tvOrderStatus.visible()
                                        }

                                        OPEN_ORDER_TAB -> {
                                            binding.lblDelivery.gone()
                                        }

                                    }
                                    binding.rvOpenOrder.visibility = View.VISIBLE
                                    binding.llNoData.visibility = View.GONE
                                    val data = it.data

                                    adapter.add(data, orderTab)
                                    LogUtil.logE("DATA", data.size.toString())

                                } else {
                                    binding.llNoData.visibility = View.VISIBLE
                                    binding.txtNodata.text = it.message
                                    binding.rvOpenOrder.visibility = View.GONE

                                }
                                val intent = Intent()
                                intent.action = "allOrderCounts"
                                intent.putExtra("isCount", true)
                                intent.putExtra("orderStatus", orderStatus)
                                intent.putExtra("count", it.data.size)
                                intent.putExtra(
                                    "start_date",
                                    viewModel.startDate.value.toString()
                                )
                                intent.putExtra("end_date", viewModel.endDate.value.toString())
                                requireContext().sendBroadcast(intent)
                            }
                        }

                        Status.ERROR -> {
                            ProgressUtils.dismissProgressDialog()
                            binding.root.showAlert(resource.message)

                        }

                        Status.LOADING -> {
                            ProgressUtils.showProgressDialog(requireActivity())
                        }
                    }
                }
            }
        } catch (e: Exception) {
        }
    }

    private fun observeShowProgress() {

        viewModel.showProgress.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        }

        activeOrderViewModel.data.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { baseResponse ->
                activity?.let {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        it, baseResponse.message
                    ) { _, _ ->
                        var intent = Intent()
                        intent.action = "allOrderCounts"
                        intent.putExtra("isCount", false)
                        intent.putExtra("start_date", viewModel.startDate.value.toString())
                        intent.putExtra("end_date", viewModel.endDate.value.toString())
                        intent.putExtra("position", 3)
                        requireContext().sendBroadcast(intent)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (orderStatus == "4" || (endDateTime != null && SimpleDateFormat(
                "MM/dd/yyyy", Locale.getDefault()
            ).parse(endDateTime).after(Calendar.getInstance().time))
        ) {
            viewModel.setCurrentDate(Calendar.getInstance(), "", "", orderStatus)
        } else {
            viewModel.setCurrentDate(
                Calendar.getInstance(), startDateTime, endDateTime, orderStatus
            )
        }

        lifecycleScope.launch {
            oneItemPerReceipt = dashboardViewModel.getLabelPrinterSettingsData().oneItemPerReciept
        }

//        viewModel.setCurrentDate(Calendar.getInstance(), "", "", orderStatus)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = AllOrdersListingFragmentBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        getKitchenReceiptSettings()
        startDatePickerObserver()
        endDatePickerObserver()
        getCustomerReceiptSettings()
        observeTipsList()
        cancelOrderObserver()

        startTime = TimePickerDialog.OnTimeSetListener { view, hour, minute ->
            val timecalender = Calendar.getInstance()
            timecalender.set(Calendar.HOUR_OF_DAY, hour)
            timecalender.set(Calendar.MINUTE, minute)
            viewModel.startDate.value =
                timeCalculateForStartEndTime(hour, minute, "isstart")
            if (differnceTrue(viewModel.startDate.value!!, viewModel.endDate.value) <= 30) {
                /*checkFilter = true
                currentPage = 1
                apiCallTimeSheet()*/
                getAllOrders()
            } else {
                AlertUtils.showCustomAlertWithListenerWithOK(
                    requireActivity(),
                    "Please Select date in 30 Days."
                ) { _, _ ->
                }
            }
        }

        endTime = TimePickerDialog.OnTimeSetListener { view, hour, minute ->
            val timecalender = Calendar.getInstance()
            timecalender.set(Calendar.HOUR_OF_DAY, hour)
            timecalender.set(Calendar.MINUTE, minute)
            viewModel.endDate.value = timeCalculateForStartEndTime(hour, minute, "isend")
            /*checkFilter = true
            currentPage = 1*/
            if (differnceTrue(viewModel.endDate.value!!, viewModel.startDate.value) <= 30)
                getAllOrders()
            else {
                AlertUtils.showCustomAlertWithListenerWithOK(
                    requireActivity(),
                    "Please Select date in 30 Days."
                ) { _, _ ->
                }
            }

        }

        startDate =
            DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                myCalendar.set(Calendar.YEAR, year)
                myCalendar.set(Calendar.MONTH, monthOfYear)
                myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                TimePickerDialog(
                    requireActivity(),
                    android.R.style.Theme_Material_Light_Dialog,
                    startTime,
                    myCalendar2.get(2),
                    myCalendar2.get(2),
                    false
                ).show()
            }

        endDate =
            DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                myCalendar1.set(Calendar.YEAR, year)
                myCalendar1.set(Calendar.MONTH, monthOfYear)
                myCalendar1.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                TimePickerDialog(
                    requireActivity(),
                    android.R.style.Theme_Material_Light_Dialog,
                    endTime,
                    myCalendar3.get(2),
                    myCalendar3.get(2),
                    false
                ).show()

            }
        return binding.root
    }

    fun cancelOrderObserver() {
        onlineDetailViewModel.cancelOnlineWebOrderLiveData.observe(viewLifecycleOwner) {
            if (it.isRefunded) {

                makeAcceptedDeclinedServerCall(
                    0,
                    it.orderId,
                    false
                )
            }

            onlineDetailViewModel.cancelOnlineWebOrderLiveData.value?.isRefunded = false
        }

        onlineDetailViewModel.dataRefundDone.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { createTaxResponse ->
                activity?.let {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        it, createTaxResponse.message
                    ) { _, _ ->

                        val result = Bundle().apply {
                            refundData.paymentRefund?.orderId?.let { it1 ->
                                putInt(
                                    "order_id", it1
                                )
                            }
                        }
                        requireActivity().supportFragmentManager.setFragmentResult(
                            "request_for_rejectOrder", result
                        )
                        findNavController().navigateUp()
                    }
                }
            }
        }
    }

    private fun getCustomerReceiptSettings() {
        activeOrderViewModel.getCustomerReceiptSettings().observe(viewLifecycleOwner) {
            if (it != null) {
                customerSettingModel = it
            }

        }

    }

    private fun observeTipsList() {
        activeOrderViewModel.getTipsList().observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                tipsList = it
            }
        }
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

    fun getSaltString(reqLent: Int): String? {
        val SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"
        val salt = StringBuilder()
        val rnd = Random()
        while (salt.length < reqLent) { // length of the random string.
            val index = (rnd.nextFloat() * SALTCHARS.length).toInt()
            salt.append(SALTCHARS[index])
        }
        return salt.toString()
    }

    fun timeCalculateForStartEndTime(hour: Int, minute: Int, isStart: String): String {
        var timestring = ""
        var hoursfinal: Int = 0
        if ((hour == 12 && minute > 0) || (hour > 12 && minute > 0) || (hour > 12 && minute == 0)) {
            if (hour == 12) {
                hoursfinal = hour
            } else {
                hoursfinal = hour - 12
            }
            if (hoursfinal < 10) {
                if (minute < 10) {
                    timestring = "0$hoursfinal:0$minute PM"
                } else {
                    timestring = "0$hoursfinal:$minute PM"
                }
            } else {
                if (minute < 10) {
                    timestring = "$hoursfinal:0$minute PM"
                } else {
                    timestring = "$hoursfinal:$minute PM"
                }
            }
        } else {
            if (hour == 0) {
                if (minute < 10) {
                    timestring = "${hour.plus(12)}:0$minute AM"
                } else {
                    timestring = "${hour.plus(12)}:$minute AM"
                }
            } else {
                if (hour < 10) {
                    if (minute < 10) {
                        timestring = "0$hour:0$minute AM"
                    } else {
                        timestring = "0$hour:$minute AM"
                    }
                } else {
                    if (minute < 10) {
                        timestring = "$hour:0$minute AM"
                    } else {
                        timestring = "$hour:$minute AM"
                    }
                }
            }

        }

        val myFormat = "MM/dd/yyyy" //In which you need put here
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        var startDatestring = ""
        if (isStart == "isstart") {
            startDatestring = sdf.format(myCalendar.time)
        } else {
            startDatestring = sdf.format(myCalendar1.time)
        }
        return "$startDatestring $timestring"
    }

    private fun differnceTrue(date1: String, date2: String?): Long {
        var dateType1: Date
        var dateType2: Date
        var daydifference = "0".toLong()
//        11/30/2021 09:40 AM
        try {
            var dates = SimpleDateFormat("MM/dd/yyyy")
            dateType1 = dates.parse(date1.substringBefore(" "))
            dateType2 = dates.parse(date2?.substringBefore(" "))
            var differencedate = abs(dateType1.time - dateType2.time)
            daydifference = differencedate / (24 * 60 * 60 * 1000)
            Log.d("yash", "differnceTrue: " + daydifference)
            return daydifference
        } catch (e: Exception) {
        }
        return daydifference
    }

    private fun startDatePickerObserver() {
        viewModel.startDateSelection.observe(requireActivity()) { event ->
            event.getContentIfNotHandled()?.let {
                //currentPage = 1
                myCalendar = Calendar.getInstance()
                myCalendar.add(Calendar.DATE, 0)
                Log.d(
                    TAG,
                    "startDatePickerObserver: " + myCalendar.get(Calendar.DAY_OF_MONTH)
                )
                Log.d(TAG, "startDatePickerObserver: " + myCalendar.get(Calendar.MONTH))
                Log.d(
                    TAG, "startDatePickerObserver: " + myCalendar
                        .get(Calendar.YEAR)
                )
                Log.d(TAG, "startDatePickerObserver: " + myCalendar.time)
                var datePickerDialog: DatePickerDialog = DatePickerDialog(
                    requireActivity(),
                    android.R.style.Theme_Material_Light_Dialog,
                    startDate,
                    myCalendar
                        .get(Calendar.YEAR),
                    myCalendar.get(Calendar.MONTH),
                    myCalendar.get(Calendar.DAY_OF_MONTH)

                )
                datePickerDialog.show()
                if (orderStatus == "4") {
//                    datePickerDialog.datePicker.minDate = myCalendar.timeInMillis
//                    var temp_calender = Calendar.getInstance()
//                    temp_calender.add(Calendar.DATE, 7)
//                    datePickerDialog.datePicker.maxDate = temp_calender.timeInMillis
                } else {
                    datePickerDialog.datePicker.maxDate = Date().time
                }


            }

        }
    }

    private fun endDatePickerObserver() {
        viewModel.endDateSelection.observe(requireActivity()) { event ->
            event.getContentIfNotHandled()?.let {
                if (orderStatus == "4") {
                    myCalendar1 = Calendar.getInstance()
                    myCalendar1.add(Calendar.DATE, 7)
                }
                var datePickerDialog: DatePickerDialog = DatePickerDialog(
                    requireActivity(),
                    android.R.style.Theme_Material_Light_Dialog,
                    endDate,
                    myCalendar1
                        .get(Calendar.YEAR),
                    myCalendar1.get(Calendar.MONTH),
                    myCalendar1.get(Calendar.DAY_OF_MONTH)

                )
                if (orderStatus == "4") {
//                    datePickerDialog.datePicker.minDate = Date().time
                } else {
                    datePickerDialog.datePicker.maxDate = Date().time
                }
                datePickerDialog.show()
            }
        }
    }


    private fun setupAdapter() {

        binding.rvOpenOrder.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayoutManager.VERTICAL
            )
        )

        adapter = AllOrderAdapter(requireContext(), prefProvider)
        adapter.setCallback(this)
        binding.rvOpenOrder?.adapter = adapter
    }

    // To search orders based on order id, employee, customer
    private fun searchFilter() {

        binding.autoSearch.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(
                s: CharSequence,
                start: Int,
                before: Int,
                count: Int
            ) {
                if (s.toString() == " ") {
                    binding.autoSearch.setText("")
                }

            }

            override fun beforeTextChanged(
                s: CharSequence,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun afterTextChanged(s: Editable) {

                adapter.filter.filter(s.toString().trim())

            }
        })
    }

    override fun onItemClickListener(view: View?, pos: Int, status: String) {
        val order = adapter.getItem(pos)
        when (status) {
            "accepted" -> {
                isPrint = true
                if (findNavController().currentDestination?.id == R.id.allOrdersFragment) {

                    findNavController().navigate(
                        R.id.action_allOrders_to_addOnlneTime,
                        bundleOf(
                            "order_id" to adapter.filterList[pos].id
                        )
                    )
                }
            }

            "Completed" -> {
                alert("", "Are you sure, you want to complete this order ?") {
                    this.positiveButton("YES") {
                        updateOrder(adapter.filterList[0].id, status)

                        ordersViewModel.changeTabPosition.value = 2
                    }
                    this.negativeButton("NO") {
                    }

                }
            }

            "rejected" -> {

                if ((orderTab == ALL_ORDER_TAB || orderTab == ONLINE_ORDER_TAB)
                    && ((adapter.orderList[pos].orderType == ONLINE_ORDER_TAB) || (adapter.orderList[pos].orderType == THIRD_PARTY_ORDER_TAB))
                ) {
                    alert("", "Are you sure, you want to reject this order ?") {

                        this.positiveButton("YES") {
                            removedPos = order.id
                            var employeeIdtemp =
                                prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
                            var terminal_id =
                                prefProvider.getValueInt(Constants.TERMINAL_ID, 0)
                            var orderItemRefundsAttributesList =
                                ArrayList<RefundRequestModelOnlineOrder.PaymentRefund.OrderItemRefundsAttribute>()
                            adapter.orderList[pos].orderItems.forEach { item ->
                                val orderItemRefundsAttributeModel =
                                    RefundRequestModelOnlineOrder.PaymentRefund.OrderItemRefundsAttribute()

                                orderItemRefundsAttributeModel.amount = item.totalPrice
                                orderItemRefundsAttributeModel.employeeId = employeeIdtemp
                                orderItemRefundsAttributeModel.orderId = item.orderId
                                orderItemRefundsAttributeModel.refundType = 0
                                orderItemRefundsAttributeModel.paymentId =
                                    adapter.orderList[pos].payments[0].id
                                orderItemRefundsAttributeModel.orderItemId = item.id
                                orderItemRefundsAttributeModel.quantity = item.quantity
                                orderItemRefundsAttributesList.add(
                                    orderItemRefundsAttributeModel
                                )
                            }

                            refundData = RefundRequestModelOnlineOrder().apply {
                                paymentRefund =
                                    RefundRequestModelOnlineOrder.PaymentRefund().apply {
                                        amount =
                                            adapter.orderList[pos].payments[0].amount + adapter.orderList[pos].payments[0].tips
                                        orderId = adapter.orderList[pos].id
                                        paymentId = adapter.orderList[pos].payments[0].id
                                        employeeId = employeeIdtemp
                                        taxRefunded =
                                            adapter.orderList[pos].payments[0].taxAmount
                                        tipsRefunded =
                                            adapter.orderList[pos].payments[0].tips
                                        terminalId = terminal_id
                                        serviceChargeRefunded =
                                            adapter.orderList[pos].payments[0].serviceChargeAmount
                                        cash_discount_or_surcharge_refunded =
                                            adapter.orderList[pos].payments[0].cashDiscount
                                        subtotal_refunded =
                                            adapter.orderList[pos].payments[0].subTotal
                                        orderItemRefundsAttributes =
                                            orderItemRefundsAttributesList
                                    }
                            }
                            val bundle = Bundle().apply {
                                putParcelable("refundData", refundData)
                                putDouble(
                                    "refundAmount",
                                    adapter.orderList[pos].payments[0].amount + adapter.orderList[pos].payments[0].tips
                                )
                                putString(
                                    "paymentType",
                                    adapter.orderList[pos].payments[0].paymentType
                                )
                                putString(
                                    "magensa_response_data",
                                    adapter.orderList[pos].magensa_response_data
                                )

                                putString(
                                    "pax_response_data",
                                    adapter.orderList[pos].payments.get(0).pax_data
                                )
                            }
                            bundle.putString("isFrom", "rejectOnlineOrder")


                            val cancelOnlineWebOrderModel = CancelOnlineWebOrderModel(
                                0,
                                adapter.filterList[pos].id,
                                false,
                                isRefunded = false
                            )

                            onlineDetailViewModel.cancelOnlineWebOrderLiveData.value =
                                cancelOnlineWebOrderModel


                            if (prefProvider.isAdmin() || prefProvider.isManager()) {
                                if (findNavController().currentDestination?.id == R.id.allOrdersFragment) {
                                    findNavController().navigate(
                                        R.id.action_allOrder_to_reasonForrefundonline,
                                        bundle
                                    )
                                }
                            } else {
                                if (findNavController().currentDestination?.id == R.id.allOrdersFragment) {
                                    findNavController().navigate(
                                        R.id.action_allOrder_to_passcodeManager,
                                        bundle
                                    )
                                }
                            }
                        }
                        this.negativeButton("NO") {
                        }

                    }
                } else if ((orderTab == ALL_ORDER_TAB || orderTab == THIRD_PARTY_ORDER_TAB)
                    && adapter.orderList[pos].orderType == THIRD_PARTY_ORDER_TAB
                ) {
                    alert("", "Are you sure, you want to reject this order ?") {

                        this.positiveButton("YES") {
                            removedPos = pos
                            acceptedAndDeclineOrder(
                                order.payments.get(0).pax_data,
                                0,
                                adapter.filterList[pos].id,
                                false
                            )
                        }
                        this.negativeButton("NO") {
                        }

                    }
                }


            }

            "UPDATE" -> {
                prefProvider.setValue(OLD_ITEM_BASE_CUSTOM_ITEM, Gson().toJson(order.orderItems))

                prefProvider.setValueInt("ORDER_ID", -1)

                dashboardViewModel.activeOrderTypeName = order.orderType
                dashboardViewModel.activeOrderTypeText = order.orderTypeName
                dashboardViewModel.activeOrderTypeId = order.id

                CoroutineScope(Dispatchers.IO).async {
                    try {
                        var orderTypebackup = OrderTypeBackup()
                        orderTypebackup.orderType = order.orderTypeId
                        orderTypebackup.employeeId = prefProvider.employeeId()
                        orderTypebackup.orderTypeName = order.orderTypeName
                        dashboardViewModel.insertOrderTypeBackup(orderTypebackup)
                    } catch (e: Exception) {
                    }
                }

                dashboardViewModel.deleteCartBeforeSwitch()
                dashboardViewModel.clearCartModelBackup()
                prefProvider.setValue(Constants.OLD_ITEM, "")
                var itemDiscountTotal: Double = 0.0
                var itemPassDis: Double = 0.0
                order.orderItems.forEach {
                    if (it.discountAmount != 0.0) {
                        itemDiscountTotal += MethodUtils.roundOffAmountDouble(it.discountAmount)
                    }
                    if (it.discountAmount != 0.0 && it.quantity > 1) {

                        //  itemDiscountTotal += MethodUtils.roundOffAmountDouble(it.discountAmount / it.quantity)
                        it.discountAmount =
                            MethodUtils.roundOffAmountDouble(it.discountAmount / it.quantity)
                    }
                    it.orderItemOriginalModifiers = it.orderItemModifiers
                }
                LogUtil.logE(TAG, "itemDiscountTotal:  ${itemDiscountTotal}")
                LogUtil.logE(TAG, "totalOrderDiscount  ${order.totalDiscount}")

                order.totalDiscount = order.totalDiscount - itemDiscountTotal

                LogUtil.logE(
                    TAG,
                    "OpenORderUpdateOrder:  ${Gson().toJson(order.orderItems)}"
                )

                if (order.orderType == OPEN_ORDER_TAB) {
                    prefProvider.setValue(Constants.ORDER_TYPE, Constants.OPEN_ORDER)
                    prefProvider.setValue(Constants.ORDER_TYPE_NAME, Constants.OPEN_ORDER_)
                } else if (order.orderType == PHONE_ORDER_TAB) {
                    prefProvider.setValue(Constants.ORDER_TYPE, Constants.PHONE_ORDER)
                    prefProvider.setValue(Constants.ORDER_TYPE_NAME, Constants.PHONE_ORDER_)
                } else if (order.orderType.equals("KioskOpenorder")) {
                    prefProvider.setValue(Constants.ORDER_TYPE, order.orderType)
                    prefProvider.setValue(Constants.ORDER_TYPE_NAME, order.orderTypeName)
                }

                prefProvider.setValueInt(Constants.ORDER_TYPE_ID, order.orderTypeId)

                dashboardViewModel.activeOrderTypeName = order.orderType
                dashboardViewModel.activeOrderTypeText = order.orderTypeName
                dashboardViewModel.activeOrderTypeId = order.id
                CoroutineScope(Dispatchers.IO).launch {
                    dashboardViewModel.updateOrderTypeBackup(
                        order.orderTypeId,
                        order.orderTypeName,
                        prefProvider.employeeId()
                    )
                }

                if (order.customer != null) {
                    prefProvider.setValue(
                        Constants.CUSTOMER_NAME,
                        order.customer.firstName + " " + order.customer.lastName
                    )

                    prefProvider.setValue(
                        Constants.RECEIPT_CUSTOMER_NAME,
                        order.customer.firstName + " " + order.customer.lastName
                    )

                    prefProvider.setValueInt(Constants.CUSTOMER_ID, order.customer.id)

                    prefProvider.saveCustomerData(TbCustomer.customerMapping(order.customer))
                }
                prefProvider.setValue(

                    Constants.OPEN_ORDER_ITEMS,
                    Gson().toJson(order.orderItems)
                )

                prefProvider.setValueboolean(Constants.OPEN_ORDER_UPDATE_FOR_PRINT, true)

                /*we are using this to check whether the note is updated or not, if yes then we will print the *****Updated***** on the kitchen receipt*/
                prefProvider.setValue(Constants.orderNoteOld, order.note)

                val updatedCartModel = generateCartModelFromOrderModel(order)


                val completePrice = order.subTotal + updatedCartModel.discountPrice

                updatedCartModel.discountSelectdValue = order.totalDiscount / completePrice * 100

                dashboardViewModel.addCart(updatedCartModel)
                dashboardViewModel.setUpdatedCartModel(updatedCartModel)
                generateCartItemsListFromOrderModel(order)?.let {
                    Log.d("27OCT23", "generated List: ${Gson().toJson(it)}")
                    var itemDiscount = 0.0
                    it.forEach {
                        itemDiscount += it.discountPrice
                        it.itemOriginalModifiersList = it.modifiers
                    }

                    dashboardViewModel.isUpdatedOnce = true

                    dashboardViewModel.addOrderItemsToCartItems(it)

                }

                val bundle = Bundle()
                bundle.putBoolean("update", true)
                bundle.putInt("orderId", order.id)
                if (!order.payments.isNullOrEmpty()) {
                    bundle.putInt("paymentId", order.payments[0].id)
                    bundle.putString("paymentOfflineId", order.payments[0].offlineId)
                } else {
                    bundle.putString("paymentOfflineId", randomOfflineId())
                }
                bundle.putString("orderOfflineId", order.offlineId)
                bundle.putBoolean("isFromActiveOrder", true)
                bundle.putBoolean("isLoyaltyApplied", order.isLoyaltyApplied)

                prefProvider.setValueboolean(Constants.IS_UPDATE_ORDER, true)
                prefProvider.setValueboolean(
                    Constants.IS_UPDATE_ORDER_LOYALTY_APPLIED,
                    order.isLoyaltyApplied
                )
                prefProvider.setValueboolean(
                    Constants.LOYALTY_ADDED,
                    order.isLoyaltyApplied
                )
                prefProvider.setValueboolean(
                    Constants.IS_UPDATE_ORDER_FROM_ACTIVE_ORDER,
                    true
                )
                prefProvider.setValueInt(Constants.IS_UPDATE_ORDER_ID, order.id)

                if (!order.payments.isNullOrEmpty()) {
                    prefProvider.setValueInt(
                        Constants.IS_UPDATE_ORDER_PAYMENT_ID,
                        order.payments[0].id
                    )
                    prefProvider.setValue(
                        Constants.IS_UPDATE_ORDER_PAY_OFFLINE_ID,
                        order.payments[0].offlineId
                    )
                } else {
                    prefProvider.setValue(
                        Constants.IS_UPDATE_ORDER_PAY_OFFLINE_ID,
                        randomOfflineId()
                    )
                }
                prefProvider.setValue(Constants.IS_UPDATE_ORDER_OFFLINE_ID, order.offlineId)

                EventBus.getDefault().post(
                    MessageEvent(
                        "${Constants.LINE_BREAK_TAB} AllOrdersListingFragment.kt  UPDATE -> bundle -> ${
                            Gson().toJson(bundle)
                        }"
                    )
                )

                try {
                    EventBus.getDefault().post(
                        MessageEvent(
                            "${Constants.LINE_BREAK_TAB} AllOrdersListingFragment.kt  UPDATE -> IS_UPDATE_ORDER_OFFLINE_ID -> ${
                                prefProvider.getValue(
                                    Constants.IS_UPDATE_ORDER_OFFLINE_ID,
                                    "null"
                                )
                            }, " +

                                    "IS_UPDATE_ORDER_PAYMENT_ID-> ${
                                        prefProvider.getValueInt(
                                            Constants.IS_UPDATE_ORDER_PAYMENT_ID,
                                            -99
                                        )
                                    }, " +
                                    "IS_UPDATE_ORDER_PAY_OFFLINE_ID-> ${
                                        prefProvider.getValue(
                                            Constants.IS_UPDATE_ORDER_PAY_OFFLINE_ID,
                                            "null"
                                        )
                                    }, " +
                                    "IS_UPDATE_ORDER-> ${
                                        prefProvider.getValueboolean(
                                            Constants.IS_UPDATE_ORDER,
                                            true
                                        )
                                    }, " +
                                    "IS_UPDATE_ORDER_LOYALTY_APPLIED-> ${
                                        prefProvider.getValueboolean(
                                            Constants.IS_UPDATE_ORDER_LOYALTY_APPLIED,
                                            false
                                        )
                                    }, " +
                                    "LOYALTY_ADDED-> ${
                                        prefProvider.getValueboolean(
                                            Constants.LOYALTY_ADDED,
                                            false
                                        )
                                    }, " +
                                    "IS_UPDATE_ORDER_FROM_ACTIVE_ORDER-> ${
                                        prefProvider.getValueboolean(
                                            Constants.IS_UPDATE_ORDER_FROM_ACTIVE_ORDER,
                                            true
                                        )
                                    }, " +
                                    "IS_UPDATE_ORDER_ID-> ${
                                        prefProvider.getValueInt(
                                            Constants.IS_UPDATE_ORDER_ID,
                                            -99
                                        )
                                    }, " +
                                    "orderNoteOld-> ${
                                        prefProvider.getValue(
                                            Constants.orderNoteOld,
                                            "null"
                                        )
                                    }, " +
                                    "OPEN_ORDER_UPDATE_FOR_PRINT-> ${
                                        prefProvider.getValueboolean(
                                            Constants.OPEN_ORDER_UPDATE_FOR_PRINT,
                                            true
                                        )
                                    }," +
                                    "OPEN_ORDER_ITEMS-> ${
                                        prefProvider.getValue(
                                            Constants.OPEN_ORDER_ITEMS,
                                            "null"
                                        )
                                    }, " +
                                    "CUSTOMER_NAME-> ${
                                        prefProvider.getValue(
                                            Constants.CUSTOMER_NAME,
                                            "null"
                                        )
                                    }, " +
                                    "RECEIPT_CUSTOMER_NAME-> ${
                                        prefProvider.getValue(
                                            Constants.RECEIPT_CUSTOMER_NAME,
                                            "null"
                                        )
                                    }, " +
                                    "CUSTOMER_ID-> ${
                                        prefProvider.getValueInt(
                                            Constants.CUSTOMER_ID,
                                            -99
                                        )
                                    }, " +
                                    "Customer_Data-> ${
                                        prefProvider.getCustomerData()
                                    }, " +
                                    "ORDER_TYPE_ID-> ${
                                        prefProvider.getValueInt(
                                            Constants.ORDER_TYPE_ID,
                                            -99
                                        )
                                    }, " +
                                    "ORDER_TYPE-> ${
                                        prefProvider.getValue(
                                            Constants.ORDER_TYPE,
                                            "null"
                                        )
                                    }, " +
                                    "ORDER_TYPE_NAME-> ${
                                        prefProvider.getValue(
                                            Constants.ORDER_TYPE_NAME,
                                            "null"
                                        )
                                    }, " +
                                    "OLD_ITEM_BASE_CUSTOM_ITEM-> ${
                                        prefProvider.getValue(
                                            OLD_ITEM_BASE_CUSTOM_ITEM,
                                            "null"
                                        )
                                    }"
                        )
                    )


                } catch (e: Exception) {
                    EventBus.getDefault()
                        .post(MessageEvent("${Constants.LINE_BREAK_TAB} AllOrdersListingFragment.kt  UPDATE -> prefException -> ${e.printStackTrace()}"))

                }

                if (findNavController().currentDestination?.id == R.id.allOrdersFragment) {

                    // dashboardViewModel.fromAllOrderFragmentUpdate = true

                    findNavController().navigate(
                        R.id.action_allOrder_to_dashboardCategoryBoldPOS, bundle
                    )
                }

//                findNavController().navigateUp()


            }

            "PAY" -> {

                try {
                    prefProvider.setValueInt("ORDER_ID", -1)

                    dashboardViewModel.activeOrderTypeName = order.orderType
                    dashboardViewModel.activeOrderTypeText = order.orderTypeName
                    dashboardViewModel.activeOrderTypeId = order.orderTypeId

                    lifecycleScope.launch {
                        try {
                            var orderTypebackup = OrderTypeBackup()
                            orderTypebackup.orderType = order.orderTypeId
                            orderTypebackup.employeeId = prefProvider.employeeId()
                            orderTypebackup.orderTypeName = order.orderTypeName
                            dashboardViewModel.insertOrderTypeBackup(orderTypebackup)
                        } catch (e: Exception) {
                        }
                    }


                } catch (e: Exception) {

                }
                prefProvider.setValue(OLD_ITEM_BASE_CUSTOM_ITEM, Gson().toJson(order.orderItems))

                prefProvider.setValue("PaidAmount", "")
                prefProvider.setValue(Constants.WHOLE_AMOUNT, "")
                prefProvider.setValueInt("cardCount", 0)
                prefProvider.setValue(Constants.SUB_TOTAL, "")
                prefProvider.setValue(Constants.CASH_DISCOUNT_SURCHARGE, "")
                prefProvider.setValue(Constants.TOTAL_DISCOUNT, "")
                prefProvider.setValue(Constants.TIP, "")
                prefProvider.setValue(Constants.TAX_CHARGE, "")
                prefProvider.setValue(Constants.SERVICE_CHARGE, "")

                dashboardViewModel.deleteCart()
                EventBus.getDefault().post(
                    MessageEvent(
                        "${Constants.LINE_BREAK_TAB} PosRepository.kt_CART_MODEL_CLEAR Thread.dumpStack(): it1 -> ${
                            Gson().toJson(Thread.currentThread().stackTrace)
                        }"
                    )
                )

                if (order.orderType == OPEN_ORDER_TAB) {
                    prefProvider.setValue(Constants.ORDER_TYPE, Constants.OPEN_ORDER)
                    prefProvider.setValue(Constants.ORDER_TYPE_NAME, Constants.OPEN_ORDER_)
                } else if (order.orderType == PHONE_ORDER_TAB) {
                    prefProvider.setValue(Constants.ORDER_TYPE, Constants.PHONE_ORDER)
                    prefProvider.setValue(Constants.ORDER_TYPE_NAME, Constants.PHONE_ORDER_)
                }

                prefProvider.setValueInt(Constants.ORDER_TYPE_ID, order.orderTypeId)


                var itemDiscountTotal: Double = 0.0
                var itemPassDis: Double = 0.0
                order.orderItems.forEach {
                    if (it.discountAmount != 0.0) {
                        itemDiscountTotal += MethodUtils.roundOffAmountDouble(it.discountAmount)
                    }
                    if (it.discountAmount != 0.0 && it.quantity > 1) {

                        //  itemDiscountTotal += MethodUtils.roundOffAmountDouble(it.discountAmount / it.quantity)
                        it.discountAmount =
                            MethodUtils.roundOffAmountDouble(it.discountAmount / it.quantity)
                    }
                }
                LogUtil.logE(TAG, "itemDiscountTotal:  ${itemDiscountTotal}")
                LogUtil.logE(TAG, "totalOrderDiscount  ${order.totalDiscount}")

                order.totalDiscount = order.totalDiscount - itemDiscountTotal

                if (order.customer != null) {
                    prefProvider.setValue(
                        Constants.CUSTOMER_NAME,
                        order.customer.firstName + " " + order.customer.lastName
                    )

                    prefProvider.setValue(
                        Constants.RECEIPT_CUSTOMER_NAME,
                        order.customer.firstName + " " + order.customer.lastName
                    )
                    prefProvider.setValueInt(Constants.CUSTOMER_ID, order.customer.id)
                    prefProvider.saveCustomerData(TbCustomer.customerMapping(order.customer))
                }

                LogUtil.logE(TAG, "getOrder  ${Gson().toJson(order)}")
                val updatedCartModel = generateCartModelFromOrderModel(order)

                val completePrice = order.subTotal + updatedCartModel.discountPrice

                updatedCartModel.discountSelectdValue = order.totalDiscount / completePrice * 100

                dashboardViewModel.addCart(updatedCartModel)
                dashboardViewModel.setUpdatedCartModel(updatedCartModel)
                generateCartItemsListFromOrderModel(order)?.let {
                    Log.d("27OCT23", "generated List: ${Gson().toJson(it)}")
                    var itemDiscount = 0.0
                    it.forEach {
                        itemDiscount += it.discountPrice
                        it.itemOriginalModifiersList = it.modifiers
                    }
                    dashboardViewModel.addOrderItemsToCartItems(it)
                }

                val bundle = Bundle()
                bundle.putBoolean("update", true)
                bundle.putDouble("totalPrice", order.totalAmount)
                bundle.putDouble("finalprice", order.totalAmount)
                bundle.putDouble(
                    "cashDiscountSurcharge",
                    MethodUtils.calculateCashDiscount(
                        order.subTotal,
                        prefProvider,
                        requireContext()
                    )
                )
                bundle.putDouble("subTotalPrice", order.subTotal)
                bundle.putDouble("totalTax", order.totalTaxAmount)
                bundle.putDouble("totalDiscount", order.totalDiscount)
                bundle.putDouble("totalServiceCharge", order.totalServiceCharges)
                bundle.putString("future_delivery_date", order.futureDeliveryDate)
                bundle.putString("future_delivery_time", order.futureDeliveryTime)
                bundle.putParcelable("cartList", updatedCartModel)

                bundle.putInt("orderId", order.id)
                LogUtil.logE("orderId :: ", order.id.toString())
                if (order.payments.isNotEmpty()) {
                    bundle.putInt("paymentId", order.payments[0].id)
                    bundle.putString("paymentOfflineId", order.payments[0].offlineId)
                } else {
                    bundle.putString("paymentOfflineId", randomOfflineId())
                }
                bundle.putString("orderOfflineId", order.offlineId)

                val redeemLoyaltyInfo = RedeemLoyaltyInfo()
                val loyaltyProgramsModel = LoyaltyProgramsModel(
                    0.0,
                    "",
                    order.loyaltyProgramId,
                    order.isLoyaltyApplied,
                    order.locationId,
                    "",
                    0,
                    "",
                    ""
                )
                redeemLoyaltyInfo.loyaltyProgramsModel = loyaltyProgramsModel
                //redeemLoyaltyInfo.isLoyaltyApplied = order.isLoyaltyApplied
                redeemLoyaltyInfo.needToApplyLoyalty = order.isLoyaltyApplied
                redeemLoyaltyInfo.usedLoyaltyAmount = order.loyaltyAmount
                redeemLoyaltyInfo.usedLoyaltyPoints = order.usedRewardPoints

                bundle.putString(
                    "redeemLoyalty",
                    Gson().toJson(redeemLoyaltyInfo)
                )

                bundle.putString(
                    "orderType_to_check_kiosk",
                    order.orderType
                )
                bundle.putBoolean("isFromActiveOrder", true)
                bundle.putBoolean("isLoyaltyApplied", order.isLoyaltyApplied)
                prefProvider.setValueboolean(
                    Constants.IS_UPDATE_ORDER_LOYALTY_APPLIED,
                    order.isLoyaltyApplied
                )
                prefProvider.setValueboolean(
                    Constants.LOYALTY_ADDED,
                    order.isLoyaltyApplied
                )
                prefProvider.setValueboolean(IS_FROM_ALL_ORDER, true)

                EventBus.getDefault().post(
                    MessageEvent(
                        "${Constants.LINE_BREAK_TAB} AllOrdersListingFragment.kt  PAY bundle -> ${
                            Gson().toJson(bundle)
                        }"
                    )
                )

                try {
                    EventBus.getDefault().post(
                        MessageEvent(
                            "${Constants.LINE_BREAK_TAB} AllOrdersListingFragment.kt  PAY preferences, " +
                                    "OLD_ITEM_BASE_CUSTOM_ITEM -> ${
                                        Gson().toJson(
                                            prefProvider.getValue(
                                                OLD_ITEM_BASE_CUSTOM_ITEM,
                                                "null"
                                            )
                                        )
                                    }, " +
                                    "PaidAmount -> ${
                                        prefProvider.getValue(
                                            "PaidAmount",
                                            "null"
                                        )
                                    }, " +
                                    "Constants.WHOLE_AMOUNT -> ${
                                        prefProvider.getValue(
                                            Constants.WHOLE_AMOUNT,
                                            "null"
                                        )
                                    }, " +
                                    "cardCount -> ${prefProvider.getValueInt("cardCount", -99)}, " +
                                    "Constants.SUB_TOTAL -> ${
                                        prefProvider.getValue(
                                            Constants.SUB_TOTAL,
                                            "null"
                                        )
                                    }, " +
                                    "Constants.CASH_DISCOUNT_SURCHARGE -> ${
                                        prefProvider.getValue(
                                            Constants.CASH_DISCOUNT_SURCHARGE,
                                            "null"
                                        )
                                    }, " +
                                    "Constants.TOTAL_DISCOUNT -> ${
                                        prefProvider.getValue(
                                            Constants.TOTAL_DISCOUNT,
                                            "null"
                                        )
                                    }, " +
                                    "Constants.TIP -> ${
                                        prefProvider.getValue(
                                            Constants.TIP,
                                            "null"
                                        )
                                    }, " +
                                    "Constants.TAX_CHARGE -> ${
                                        prefProvider.getValue(
                                            Constants.TAX_CHARGE,
                                            "null"
                                        )
                                    }, " +
                                    "Constants.SERVICE_CHARGE -> ${
                                        prefProvider.getValue(
                                            Constants.SERVICE_CHARGE,
                                            "null"
                                        )
                                    }, " +
                                    "Constants.ORDER_TYPE -> ${
                                        prefProvider.getValue(
                                            Constants.ORDER_TYPE,
                                            "null"
                                        )
                                    }, " +
                                    "Constants.ORDER_TYPE_NAME -> ${
                                        prefProvider.getValue(
                                            Constants.ORDER_TYPE_NAME,
                                            "null"
                                        )
                                    }, " +
                                    "Constants.ORDER_TYPE -> ${
                                        prefProvider.getValue(
                                            Constants.ORDER_TYPE,
                                            "null"
                                        )
                                    }, " +
                                    "Constants.ORDER_TYPE_NAME -> ${
                                        prefProvider.getValue(
                                            Constants.ORDER_TYPE_NAME,
                                            "null"
                                        )
                                    }, " +
                                    "Constants.ORDER_TYPE_ID -> ${
                                        prefProvider.getValueInt(
                                            Constants.ORDER_TYPE_ID,
                                            -99
                                        )
                                    }, " +
                                    "Constants.CUSTOMER_ID -> ${
                                        prefProvider.getValueInt(
                                            Constants.CUSTOMER_ID,
                                            -99
                                        )
                                    }, " +
                                    "getCustomerData() -> ${prefProvider.getCustomerData()}, " +
                                    "IS_FROM_ALL_ORDER -> ${
                                        prefProvider.getValueboolean(
                                            IS_FROM_ALL_ORDER,
                                            true
                                        )
                                    }, " +
                                    "Constants.LOYALTY_ADDED -> ${
                                        prefProvider.getValueboolean(
                                            Constants.LOYALTY_ADDED,
                                            false
                                        )
                                    }, " +
                                    "Constants.IS_UPDATE_ORDER_LOYALTY_APPLIED -> ${
                                        prefProvider.getValueboolean(
                                            Constants.IS_UPDATE_ORDER_LOYALTY_APPLIED,
                                            false
                                        )
                                    }, " +
                                    "Constants.RECEIPT_CUSTOMER_NAME -> ${
                                        prefProvider.getValue(
                                            Constants.RECEIPT_CUSTOMER_NAME,
                                            "null"
                                        )
                                    }, " +
                                    "Constants.CUSTOMER_NAME -> ${
                                        prefProvider.getValue(
                                            Constants.CUSTOMER_NAME,
                                            "null"
                                        )
                                    }, " +
                                    ""
                        )
                    )
                } catch (e: Exception) {
                    EventBus.getDefault()
                        .post(MessageEvent("${Constants.LINE_BREAK_TAB} AllOrdersListingFragment.kt  PAY -> prefException -> ${e.printStackTrace()}"))
                }


                findNavController().navigate(
                    R.id.action_allOrder_to_paymentBoldPosFragment,
                    bundle
                )

            }

            Constants.PRINT_UNPAID -> {
                isPrintCustomer = true
//               This is only a fix from android side, the DeliveryType should come empty from server side when the order is OpenOrder
//                if (order.orderType.equals("OpenOrder", true)) {
//                    order.orderType = ""
//                }
                getCustomerPrinters(order, status)
            }

            Constants.PRINT_PAID -> {
                isPrintCustomer = true
//               This is only a fix from android side, the DeliveryType should come empty from server side when the order is OpenOrder
                if (order.orderType.equals("OpenOrder", true)) {
                    order.orderType = ""
                }
                getCustomerPrinters(order, status)
            }

            "CANCEL" -> {//cancel order
                if (rolePermission.hasCancelOrderPermission(binding.root)) {
                    val bundle = Bundle().apply {
                        /* putParcelable("refundData", refundData)
                         putDouble("refundAmount", subTotalPrice)*/

                        putInt("orderId", order.id)
                        putString("startDate", viewModel.startDate.value.toString())
                        putString("endDate", viewModel.endDate.value.toString())
                    }

                    findNavController().navigate(
                        R.id.action_allOrder_to_reason_for_cancel_order_dialog,
                        bundle
                    )
                }
            }

            "REPRINT_KITCHEN_RECEIPT" -> {
                isPrint = true
                getKitchenPrinters(order)
            }
        }
    }

    override fun noDataAvailableFilter() {
        runOnUiThread {
            binding.llNoData.visible()
            binding.txtNodata.text = requireContext().getText(R.string.no_data_available)
        }
        Log.d("noDataAvailableFilter", "no data available")
    }

    override fun hideNoDataAvailable() {
        runOnUiThread {
            binding.llNoData.gone()
        }
        Log.d("noDataAvailableFilter", "hide")
    }

    // get available customer printer's list
    private fun getCustomerPrinters(order: OnlineOrderResponseModel.Data, type: String) {

        activeOrderViewModel.getCustomerPrinterList().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    ProgressUtils.dismissProgressDialog()
                    if (it.data != null) {
                        val customerList = it.data

                        if (isPrintCustomer == true) {
                            isPrintCustomer = false
                            customerList.forEach {
                                if (it.status) {
                                    initPrinter(it, Constants.CUSTOMER, order, type)
                                }


                            }
                        }


                    }


                }

                Status.ERROR -> {

                    ProgressUtils.dismissProgressDialog()

                }

                Status.LOADING -> {
                    ProgressUtils.showProgressDialog(requireActivity())

                }

            }

        }

    }

    private fun initPrinter(
        customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters,
        type: String,
        order: OnlineOrderResponseModel.Data,
        printType: String
    ) {

        if (customerReceiptPrinters.name.startsWith(SUNMI_PRINTER, true)) {

            SunmiPrinterApi.getInstance()
                .setPrinter(
                    SunmiPrinter.SunmiBlueToothPrinter,
                    customerReceiptPrinters.ipAddress
                )


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
                            generatePrintSunmi(
                                customerReceiptPrinters,
                                type,
                                order,
                                printType
                            )


                        }

                        override fun onDisconnect() {
                            println("onDisconnect")
                        }

                    })
            } else {
                generatePrintSunmi(customerReceiptPrinters, type, order, printType)


            }


        } else if (customerReceiptPrinters.name.startsWith(
                Constants.SUNMI_INNER_PRINTER,
                true
            )
        ) {

            SunmiPrintHelper.getInstance().initSunmiPrinterService(requireContext())
            viewLifecycleOwner.lifecycleScope.launch {
                delay(100)
                setServiceForCustomer(customerReceiptPrinters, type, order, printType)
            }


        } else {
            PrinterClass.closePrinter()
            if (PrinterClass.getPrinter() == null) {
                var printer: Print? = Print(requireContext())
                if (printer != null) {
                    //  printer.setStatusChangeEventCallback(this)
                    // printer.setBatteryStatusChangeEventCallback(this)
                }

                val enabled = Print.FALSE

                try {
                    var interval: Int = 1000
                    if (customerReceiptPrinters.printer_type == Constants.BLUETOOTH) {
                        interval = PrinterClass.BLUETOOTH_TIMEOUT
                    }
                    printer?.openPrinter(

                        if (customerReceiptPrinters.printer_type == Constants.BLUETOOTH) {
                            Print.DEVTYPE_BLUETOOTH
                        } else {
                            Print.DEVTYPE_TCP
                        },
                        customerReceiptPrinters.ipAddress,
                        enabled,
                        1000
                    )
                    //printer?.setStatusChangeEventCallback(this)

                } catch (e: Exception) {
                    LogUtil.logE(TAG, "PrinterException: " + e.message)
                    printer = null
                    return
                }
                try {

                    if (printer != null) {
                        PrinterClass.setPrinter(printer)

                        generatePrint(customerReceiptPrinters, type, order, printType)

                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                LogUtil.logE(TAG, "PrinterIsNotNull:")
            }
        }

    }

    private fun generatePrint(
        customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters,
        type: String,
        receiptModel: OnlineOrderResponseModel.Data,
        printType: String
    ) {
        var builder: Builder? = null
        LogUtil.logE(TAG, "customerSettingModel:  ${Gson().toJson(customerSettingModel)}")
        try {
            builder =
                Builder(
                    if (customerReceiptPrinters.name.substring(0, 6).toString()
                            .lowercase() == "TM-m30".lowercase()
                    ) {
                        "TM-m30"
                    } else {
                        customerReceiptPrinters.name
                    }, PrinterClass.language, requireActivity()
                )

            LogUtil.logE(
                TAG,
                "getVanueLogo:  ${prefProvider.getValue(Constants.VENUE_LOGO, "")}"
            )


            if (customerSettingModel.showOrderIdTop) {
                builder.addFeedLine(1)
                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(2, 2)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )
                if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                    builder.addText("OrderID:" + receiptModel.custom_order_id)
                } else {
                    builder.addText("OrderID:" + receiptModel.id)
                }

                builder.addFeedLine(1)
            }


            if (customerSettingModel.showVenueLogo && prefProvider.getValue(
                    Constants.VENUE_LOGO,
                    ""
                )
                    .isNotEmpty()
            ) {
                builder.addFeedLine(1)
                builder.addTextAlign(Builder.ALIGN_CENTER)

                /* var bitmap = getBitmapFromURL(prefProvider.getValue(VENUE_LOGO, ""))*/

                val decodedString: ByteArray = android.util.Base64.decode(
                    prefProvider.getValue(Constants.VENUE_LOGO, ""),
                    android.util.Base64.DEFAULT
                )
                val bitmap: Bitmap =
                    BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)

                val newBitmap = Bitmap.createScaledBitmap(bitmap!!, 210, 210, true)
                builder.addImage(
                    newBitmap, 0, 0,
                    newBitmap.width, newBitmap.height, Builder.COLOR_1, Builder.MODE_MONO,
                    Builder.HALFTONE_DITHER, 1.0
                )
            }

            builder.addFeedLine(1)
            builder.addTextLang(Builder.LANG_EN)

            builder.addTextFont(Builder.FONT_E)

            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(2, 2)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )
            builder.addTextAlign(Builder.ALIGN_CENTER)
            if (printType == Constants.PRINT_PAID) {

                builder.addText("Paid" + "\n")
            } else {
                builder.addText("Unpaid" + "\n")

            }
            builder.addFeedLine(1)


            builder.addTextSize(2, 2)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
            )
            builder.addTextAlign(Builder.ALIGN_CENTER)

            addBuilderText(
                builder,
                prefProvider.getValue(Constants.BUSINESS_NAME, "").toString()
            )

            if (customerSettingModel.showVenueAddress) {
                builder.addFeedLine(1)
                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)

                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                addBuilderText(
                    builder,
                    prefProvider.getValue(
                        Constants.BUSINESS_ADDRESS, prefProvider.getValue(
                            Constants.BUSINESS_ADDRESS, ""
                        )
                    ).toString()
                )
            }
            if (customerSettingModel.showVenuePhone) {
                builder.addFeedLine(1)

                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                addBuilderText(
                    builder,
                    MethodUtils.getUSFormatNumber(
                        prefProvider.getValue(Constants.BUSINESS_PHONE_NO, "").toString()
                    )
                )
            }

            if (customerSettingModel.showWebsiteAddress) {
                builder.addFeedLine(1)

                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                addBuilderText(
                    builder,
                    prefProvider.getValue(Constants.BUSINESS_WEBSITE, "")
                )
            }

            if (customerSettingModel.showOrderType) {
                builder.addFeedLine(1)

                builder.addTextFont(Builder.FONT_E)

                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(2, 2)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addText(receiptModel?.orderTypeName + "\n")
            }

            if (receiptModel?.orderType?.lowercase() == Constants.PHONE_ORDER.lowercase()
                || receiptModel?.orderType?.lowercase() == Constants.OPEN_ORDER.lowercase()
            ) {


                builder.addFeedLine(1)

                builder.addTextFont(Builder.FONT_E)

                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(2, 2)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addText(receiptModel?.deliveryType + "\n")


            }




            if (customerSettingModel.fonts == Constants.LARGE) {


                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                builder.addText("ReceiptID:" + receiptModel?.offlineId)

                if (customerSettingModel.showTeam && receiptModel?.employee?.name != null) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )


                    builder.addText("Employee:" + receiptModel?.employee?.name)

                }

                if (customerSettingModel.showOrderTime) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )

                    builder.addText(
                        "Order Time:" + Constants.getReceiptFormatDateFromUTCServer(
                            requireContext(),
                            receiptModel?.createdAt.toString()
                        )
                    )

                }

                if (customerSettingModel.showPrintTime) {


                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {


                        val current = LocalDateTime.now()
                        val formatter = DateTimeFormatter.ofPattern("MMM-dd-yyyy hh:mm:a")
                        val formatted = current.format(formatter)

                        builder.addTextLineSpace(30)
                        builder.addFeedUnit(30)
                        builder.addTextFont(Builder.FONT_E)
                        builder.addTextAlign(Builder.ALIGN_LEFT)
                        builder.addTextLang(Builder.LANG_EN)
                        addCustomerTextSize(builder, customerSettingModel.fonts)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.COLOR_1
                        )
                        builder.addText("Print Time:" + formatted)
                    }


                }
            } else {


                builder.addFeedLine(1)
                builder.addTextFont(Builder.FONT_E)
                //  builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        "ReceiptID:" + receiptModel?.offlineId,
                        "",
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )

                if (customerSettingModel.showTeam) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    //  builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )


                    builder.addText(
                        padLine(
                            if (customerSettingModel.showTeam && receiptModel?.employee?.name != null) {
                                "Employee:" + receiptModel.employee?.name
                            } else {
                                ""
                            },
                            "",
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )

                }
                if (customerSettingModel.showOrderTime) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    //  builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )

                    builder.addText(
                        padLine(
                            if (customerSettingModel.showOrderTime) {
                                "Order Time:" + Constants.getReceiptFormatDateFromUTCServer(
                                    requireContext(),
                                    receiptModel?.createdAt.toString()
                                )
                            } else {
                                ""
                            },
                            "",
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )

                }


                if (customerSettingModel.showPrintTime) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {


                        val current = LocalDateTime.now()
                        val formatter = DateTimeFormatter.ofPattern("MMM-dd-yyyy hh:mm:a")
                        val formatted = current.format(formatter)
                        builder.addTextLineSpace(30)
                        builder.addFeedUnit(30)
                        builder.addTextFont(Builder.FONT_E)
                        //  builder.addTextAlign(Builder.ALIGN_LEFT)
                        builder.addTextLang(Builder.LANG_EN)
                        addCustomerTextSize(builder, customerSettingModel.fonts)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.COLOR_1
                        )

                        builder.addText(
                            padLine(
                                if (customerSettingModel.showPrintTime) {
                                    "Print Time:" + formatted
                                } else {
                                    ""
                                },
                                "",
                                if (customerSettingModel.fonts == Constants.LARGE) {
                                    24
                                } else {
                                    48
                                }
                            )
                        )

                    }
                }
            }

            builder.addFeedLine(1)

            addHorizontalLine(builder)



            receiptModel.orderItems.let {
                addOrderItemOnlineOrder(
                    builder,
                    it,
                    customerSettingModel.fonts,
                    customerSettingModel.showModifiers
                )
            }

            builder.addFeedLine(2)

            if (receiptModel?.totalDiscount != null) {
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                builder.addText(
                    padLine(
                        "Total Discount",

                        if (receiptModel.totalDiscount == 0.0) {
                            "$" + MethodUtils.roundOffAmountString(receiptModel.totalDiscount)
                        } else {
                            "-$" + MethodUtils.roundOffAmountString(receiptModel.totalDiscount)
                        },
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )

            }

            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)
            builder.addTextFont(Builder.FONT_E)
            // builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            addCustomerTextSize(builder, customerSettingModel.fonts)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )

            builder.addText(
                padLine(
                    "Sub Total",
                    "$" + MethodUtils.roundOffAmountString(receiptModel.subTotal),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        24
                    } else {
                        48
                    }
                )
            )


            if (receiptModel?.totalTaxAmount != null) {
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        "Tax",
                        "$" + MethodUtils.roundOffAmountString(receiptModel.totalTaxAmount),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }

            val result =
                prefProvider.getValueboolean(
                    Constants.SERVICECHARGE_TAKEOUT_OPENORDER,
                    false
                )
            if (receiptModel.totalServiceCharges != null && result) {
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        "Service Charge",
                        "$" + MethodUtils.roundOffAmountString(receiptModel.totalServiceCharges),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }



            if (receiptModel?.totalTips != 0.0) {

                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        "Tips",
                        "$" + receiptModel.totalTips?.let {
                            MethodUtils.roundOffAmountString(
                                it
                            )
                        },
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }




            if (receiptModel.cash_discount_or_surcharge != 0.0 && customerSettingModel.showCashDisSurCharg) {
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )


                if (receiptModel.payments.isNotEmpty() && receiptModel.payments.get(
                        receiptModel.payments.size - 1
                    ).paymentType.lowercase() == "Card".lowercase()
                ) {
                    builder.addText(
                        padLine(
                            Constants.SURCHARGE_TEXT,
                            "$" + MethodUtils.roundOffAmountString(receiptModel.cash_discount_or_surcharge!!),
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )
                } else {

                    builder.addText(
                        padLine(
                            "Cash Discount",
                            if (receiptModel.cash_discount_or_surcharge == 0.0) {
                                "$" + MethodUtils.roundOffAmountString(receiptModel.cash_discount_or_surcharge!!)
                            } else {
                                "-$" + MethodUtils.roundOffAmountString(receiptModel.cash_discount_or_surcharge!!)
                            },
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )

                }
            }

            if (receiptModel?.isLoyaltyApplied == true && receiptModel?.loyaltyAmount != 0.0) {


                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        "Used Loyalty Amount",
                        "-$" + receiptModel?.loyaltyAmount?.let {
                            MethodUtils.roundOffAmountString(
                                it
                            )
                        },
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )



                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        "Used Loyalty Points",
                        receiptModel?.usedRewardPoints.toString(),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )

            }


            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)


            if (receiptModel.totalAmount != null) {
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )
                var totalAmt = MethodUtils.roundOffAmountDouble(receiptModel.totalAmount)
                /*if (receiptModel.totalDiscount != 0.0) {
                    totalAmt =
                        (totalAmt - MethodUtils.roundOffAmountDouble(receiptModel.totalDiscount))

                }*/

                builder.addText(
                    padLine(
                        "Total Price",
                        "$" + MethodUtils.roundOffAmountString(totalAmt),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )

            }

            if (receiptModel.cashDiscountType == "CashDiscount"
            ) {

                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        "Pay by Cash",
                        "$" + MethodUtils.roundOffAmountString(
                            receiptModel.totalAmount - MethodUtils.getLatestCashDiscountOrSurCharge(
                                receiptModel.totalAmount,
                                prefProvider,
                                requireContext()
                            )
                        ),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )

                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        "Pay by Card",
                        "$" + MethodUtils.roundOffAmountString(receiptModel.totalAmount),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )


            } else if (receiptModel.cashDiscountType == "SurCharge"
            ) {

                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        "Pay by Cash",
                        "$" + MethodUtils.roundOffAmountString(receiptModel.totalAmount),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )

                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        "Pay by Card",
                        "$" + MethodUtils.roundOffAmountString(
                            receiptModel.totalAmount + MethodUtils.getLatestCashDiscountOrSurCharge(
                                receiptModel.totalAmount,
                                prefProvider,
                                requireContext()
                            )
                        ),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )

            }

            if (customerSettingModel.showRefundAmount && printType == Constants.PRINT_PAID) {
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        "Change Amount",
                        "$" + MethodUtils.roundOffAmountString(0.00),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }

            if (receiptModel?.totalTips == 0.0 && printType == Constants.PRINT_PAID) {
                builder.addFeedLine(1)
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                var tip = ""

                if (receiptModel.totalTips != 0.0) {
                    tip = receiptModel.totalTips.toString()
                }
                builder.addText(
                    padLine(
                        "Tips",
                        if (customerSettingModel.showTipLineForCash) {
                            "_____________"
                        } else {
                            ""
                        },
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }


            if (customerSettingModel.showTipSuggestion) {
                builder.addFeedLine(1)
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )
                builder.addText(
                    padLine(
                        "Additional Tips",
                        "",
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )


                builder.addFeedLine(1)

                addHorizontalLine(builder)

                if (tipsList.isNotEmpty()) {
                    addTipsList(
                        builder,
                        tipsList,
                        receiptModel.totalAmount.toDouble(),
                        customerSettingModel.fonts
                    )

                }
            }

            if (printType == Constants.PRINT_PAID) {
                builder.addFeedLine(1)
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        "Transaction ID",
                        receiptModel.payments.get(0).id.toString(),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }

            if (printType == Constants.PRINT_PAID) {
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        "Transaction Type",
                        receiptModel.payments.get(0).paymentType,
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )

            }
            if (customerSettingModel.showCustomerAddress or customerSettingModel.showCustomerPhone or customerSettingModel.showCustomerName) {

                if (receiptModel.customer != null) {

                    builder.addFeedLine(1)
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)

                    builder.addTextFont(Builder.FONT_E)
                    // builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )
                    builder.addText(
                        padLine(
                            "Customer Details",
                            "",
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )

                    builder.addFeedLine(1)

                    addHorizontalLine(builder)
                    builder.addFeedLine(1)
                    if (customerSettingModel.showCustomerName) {

                        builder.addTextFont(Builder.FONT_E)
                        // builder.addTextAlign(Builder.ALIGN_LEFT)
                        builder.addTextLang(Builder.LANG_EN)
                        addCustomerTextSize(builder, customerSettingModel.fonts)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.COLOR_1
                        )

                        builder.addText(receiptModel.customer.firstName + " " + receiptModel.customer.lastName)
                    }

                    if (customerSettingModel.showCustomerPhone) {
                        if (receiptModel?.customer?.phones?.isNotEmpty()) {
                            builder.addTextLineSpace(30)
                            builder.addFeedUnit(30)
                            builder.addTextFont(Builder.FONT_E)
                            //builder.addTextAlign(Builder.ALIGN_LEFT)
                            builder.addTextLang(Builder.LANG_EN)
                            addCustomerTextSize(builder, customerSettingModel.fonts)
                            builder.addTextStyle(
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.COLOR_1
                            )

                            var phoneNoFormatted = MethodUtils.getUSFormatNumber(
                                receiptModel?.customer?.phones?.get(receiptModel?.customer?.phones?.size - 1).phoneNumber
                            )
                            LogUtil.logE(TAG, "phoneNoFormatted:  ${phoneNoFormatted}")
                            builder.addText(phoneNoFormatted)

                        }


                    }




                    if (customerSettingModel.showCustomerAddress) {
                        if (receiptModel.customer?.addresses?.isNotEmpty() == true) {

                            builder.addTextLineSpace(30)
                            builder.addFeedUnit(30)
                            builder.addTextFont(Builder.FONT_E)
                            //builder.addTextAlign(Builder.ALIGN_LEFT)
                            builder.addTextLang(Builder.LANG_EN)
                            addCustomerTextSize(builder, customerSettingModel.fonts)
                            builder.addTextStyle(
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.COLOR_1
                            )
                            receiptModel.customer?.addresses.filter { it.typeOfAddress == Constants.SHIPPING_ADDRESS }
                                .forEach {

                                    if (it.typeOfAddress.equals(
                                            Constants.SHIPPING_ADDRESS,
                                            ignoreCase = true
                                        )
                                    ) {
                                        builder.addText(
                                            it.fullAddress
                                        )
                                    }
                                }

//                            builder.addText(receiptModel.customer?.addresses?.get(receiptModel.customer?.addresses?.size - 1)?.fullAddress)
                        }
                    }

                }
            }


            if (receiptModel.note != null && receiptModel.note != "" && customerSettingModel.showOrderNote) {

                builder.addFeedLine(2)
                builder.addTextFont(Builder.FONT_B)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )
                builder.addText("Order Note")
                builder.addFeedLine(1)

                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(1, 1)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                builder.addText(receiptModel.note)
            }
            if (printType == Constants.PRINT_PAID) {
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                //customer signature line.
                builder.addText(
                    padLine(
                        "Customer Signature",
                        addHorizontalHalfCustomerReceiptLine(customerSettingModel.fonts),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }




            if (customerSettingModel.showQrCode) {
                builder.addFeedLine(1)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                val bitmap = generateQRCode(receiptModel.digitalReceiptUrl)
                LogUtil.logE(TAG, "BitmapHeight ${bitmap.height}")
                LogUtil.logE(TAG, "BitmapWidth ${bitmap.width}")
                val newBitmap = Bitmap.createScaledBitmap(bitmap, 210, 210, true)
                builder.addImage(
                    newBitmap, 0, 0,
                    newBitmap.width, newBitmap.height, Builder.COLOR_1, Builder.MODE_MONO,
                    Builder.HALFTONE_DITHER, 1.0
                )
            }

            builder.addFeedLine(2)

            builder.addCut(Builder.CUT_FEED)

            val status = IntArray(1)
            val battery = IntArray(1)


            try {
                PrinterClass.getPrinter()?.sendData(
                    builder,
                    PrinterClass.BLUETOOTH_TIMEOUT, status, battery
                )

                PrinterClass.closePrinter()
                findNavController().navigate(R.id.action_orderCompleteFragment_to_dashboardCategoryNew)
                //PrinterClass.getPrinter()?.sendData(builder, 0, status, battery)
            } catch (e: Exception) {
                PrinterClass.closePrinter()
                e.printStackTrace()
                LogUtil.logE(TAG, "PrinterError: " + e.localizedMessage)
            }


        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun generateQRCode(qrcodeStaticUrl: String): Bitmap {

        val manager =
            requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager?

        // initializing a variable for default display.

        // initializing a variable for default display.
        val display: Display = manager!!.defaultDisplay

        // creating a variable for point which
        // is to be displayed in QR Code.

        // creating a variable for point which
        // is to be displayed in QR Code.
        val point = Point()
        display.getSize(point)

        // getting width and
        // height of a point

        // getting width and
        // height of a point
        val width: Int = point.x
        val height: Int = point.y

        // generating dimension from width and height.

        // generating dimension from width and height.
        var dimen = if (width < height) width else height
        dimen = dimen * 3 / 4

        LogUtil.logE(TAG, "getDimen:  ${dimen}")
        return net.glxn.qrgen.android.QRCode.from(qrcodeStaticUrl).bitmap()


    }

    // to generate customer receipt in sunmi printer
    private fun generatePrintSunmi(
        customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters,
        type: String,
        receiptModel: OnlineOrderResponseModel.Data,
        printType: String
    ) {
        try {

            PrintSunmiUtils.fontSize(customerSettingModel.fonts)

            if (customerSettingModel.showOrderIdTop) {
                if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                    PrintSunmiUtils.orderIdLarge("OrderID:" + receiptModel.custom_order_id)
                } else {
                    PrintSunmiUtils.orderIdLarge("OrderID:" + receiptModel?.id)
                }
                SunmiPrinterApi.getInstance().lineWrap(1)
            }


            if (customerSettingModel.showVenueLogo && prefProvider.getValue(
                    Constants.VENUE_LOGO,
                    ""
                )
                    .isNotEmpty()
            ) {

                printBusinessLogo()
            }


            if (printType == Constants.PRINT_PAID) {
                PrintSunmiUtils.paidStatus("Paid")
            } else {
                PrintSunmiUtils.paidStatus("Unpaid")
            }

            PrintSunmiUtils.printBusinessDetails(
                prefProvider.getValue(Constants.BUSINESS_NAME, ""),
                if (customerSettingModel.showVenueAddress) prefProvider.getValue(
                    Constants.BUSINESS_ADDRESS,
                    ""
                ) else "",
                if (customerSettingModel.showVenuePhone) prefProvider.getValue(
                    Constants.BUSINESS_PHONE_NO,
                    ""
                ) else ""
            )

            if (customerSettingModel.showWebsiteAddress) {
                PrintSunmiUtils.venueWebsite(
                    prefProvider.getValue(
                        Constants.BUSINESS_WEBSITE,
                        ""
                    )
                )
            }

            if (customerSettingModel.showOrderType) {
                PrintSunmiUtils.printOrderType(receiptModel?.orderTypeName?.trim())
            }


            if (receiptModel?.orderType?.lowercase() == Constants.PHONE_ORDER.lowercase()
//                || receiptModel?.orderType?.lowercase() == Constants.OPEN_ORDER.lowercase()
            ) {

                PrintSunmiUtils.deliveryType(receiptModel?.deliveryType?.trim())

            }


            SunmiPrinterApi.getInstance().lineWrap(1)


            if (customerSettingModel.fonts == Constants.LARGE) {


                PrintSunmiUtils.receiptID("ReceiptID:" + receiptModel?.offlineId)


                if (customerSettingModel.showTeam && receiptModel?.employee?.name != null) {

                    PrintSunmiUtils.employee("Employee:" + receiptModel?.employee?.name)

                }

                if (customerSettingModel.showOrderTime) {


                    PrintSunmiUtils.orderTime(
                        "Order Time:" + Constants.getReceiptFormatDateFromUTCServer(
                            requireContext(),
                            receiptModel?.createdAt.toString()
                        )
                    )

                }

                if (customerSettingModel.showPrintTime) {


                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                        PrintSunmiUtils.orderTime(
                            "Print Time:" + Constants.getCurrentTimeFromTimeZone(
                                requireContext(),
                                MethodUtils.formatted()
                            )
                        )
                    }


                }
            } else {


                val str = padLine(

                    "ReceiptID:" + receiptModel?.offlineId,
                    "",
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString().trim()

                PrintSunmiUtils.orderId(str)
                if (customerSettingModel.showTeam) {


                    val empName = padLine(
                        if (customerSettingModel.showTeam && receiptModel?.employee?.name != null) {
                            "Employee:" + receiptModel.employee?.name
                        } else {
                            ""
                        },
                        "",
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()

                    PrintSunmiUtils.employee(empName)
                }
                if (customerSettingModel.showOrderTime) {


                    val orderTime = padLine(
                        if (customerSettingModel.showOrderTime) {
                            "Order Time:" + Constants.getReceiptFormatDateFromUTCServer(
                                requireContext(),
                                receiptModel?.createdAt.toString()
                            )
                        } else {
                            ""
                        },
                        "",
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()

                    PrintSunmiUtils.orderTime(orderTime)
                }


                if (customerSettingModel.showPrintTime) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {


                        val current = LocalDateTime.now()
                        val formatter = DateTimeFormatter.ofPattern("MMM-dd-yyyy hh:mm:a")
                        val formatted = current.format(formatter)


                        val printTime = padLine(
                            if (customerSettingModel.showPrintTime) {
                                "Print Time:$formatted"
                            } else {
                                ""
                            },
                            "",
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                23
                            } else {
                                48
                            }
                        ).toString()

                        PrintSunmiUtils.orderTime(printTime)

                    }
                }
            }


            PrintSunmiUtils.addHorizontal()



            receiptModel.orderItems.let {
                addOrderItemOnlineOrderSunmi(
                    it,
                    customerSettingModel.fonts,
                    customerSettingModel.showModifiers
                )
            }

            SunmiPrinterApi.getInstance().lineWrap(2)


            if (receiptModel?.totalDiscount != null) {

                val str1 = padLine(
                    "Total Discount",

                    if (receiptModel.totalDiscount == 0.0) {
                        "$" + MethodUtils.roundOffAmountString(receiptModel.totalDiscount)
                    } else {
                        "-$" + MethodUtils.roundOffAmountString(receiptModel.totalDiscount)
                    },
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.totalDiscount(str1)

            }


            val str2 = padLine(
                "Sub Total",
                "$" + MethodUtils.roundOffAmountString(receiptModel.subTotal),
                if (customerSettingModel.fonts == Constants.LARGE) {
                    23
                } else {
                    48
                }
            ).toString()

            PrintSunmiUtils.subTotal(str2)


            if (receiptModel?.totalTaxAmount != null) {


                val str3 = padLine(
                    "Tax",
                    "$" + MethodUtils.roundOffAmountString(receiptModel.totalTaxAmount),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.tax(str3)
            }

            val result =
                prefProvider.getValueboolean(
                    Constants.SERVICECHARGE_TAKEOUT_OPENORDER,
                    false
                )

            if (result) {

                if (receiptModel.totalServiceCharges != null) {


                    val str4 = padLine(
                        "Service Charge",
                        "$" + MethodUtils.roundOffAmountString(receiptModel.totalServiceCharges),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                    PrintSunmiUtils.serviceCharge(str4)
                }
            }


            if (receiptModel?.totalTips != 0.0) {


                val str8 = padLine(
                    "Tips",
                    "$" + receiptModel.totalTips?.let {
                        MethodUtils.roundOffAmountString(
                            it
                        )
                    },
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.tip(str8)


            }




            if (receiptModel.cash_discount_or_surcharge != 0.0 && customerSettingModel.showCashDisSurCharg) {


                if (receiptModel.payments.isNotEmpty() && receiptModel.payments.get(
                        receiptModel.payments.size - 1
                    ).paymentType.lowercase() == "Card".lowercase()
                ) {

                    val str8 = padLine(
                        Constants.SURCHARGE_TEXT,
                        "$" + MethodUtils.roundOffAmountString(receiptModel.cash_discount_or_surcharge!!),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                    PrintSunmiUtils.surCharge(str8)

                } else {

                    val str8 = padLine(
                        "Cash Discount",
                        if (receiptModel.cash_discount_or_surcharge == 0.0) {
                            "$" + MethodUtils.roundOffAmountString(receiptModel.cash_discount_or_surcharge!!)
                        } else {
                            "-$" + MethodUtils.roundOffAmountString(receiptModel.cash_discount_or_surcharge!!)
                        },
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                    PrintSunmiUtils.cashDiscount(str8)

                }
            }

            if (receiptModel?.isLoyaltyApplied == true && receiptModel?.loyaltyAmount != 0.0) {

                val str8 = padLine(
                    "Used Loyalty Amount",
                    "-$" + receiptModel?.loyaltyAmount?.let {
                        MethodUtils.roundOffAmountString(
                            it
                        )
                    },
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()

                PrintSunmiUtils.loyaltyAmount(str8)

                val str9 = padLine(
                    "Used Loyalty Points",
                    receiptModel?.usedRewardPoints.toString(),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()

                PrintSunmiUtils.loyaltyPoint(str9)


            }




            SunmiPrinterApi.getInstance().lineWrap(1)

            if (receiptModel.totalAmount != null) {

                val totalAmt = MethodUtils.roundOffAmountDouble(receiptModel.totalAmount)

                val str5 = padLine(
                    "Total Price",
                    "$" + MethodUtils.roundOffAmountString(totalAmt),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.totalPrice(str5)

            }

            if (receiptModel.cashDiscountType == "CashDiscount"
            ) {

                val str5 = padLine(
                    "Pay by Cash",
                    "$" + MethodUtils.roundOffAmountString(
                        receiptModel.totalAmount - MethodUtils.getLatestCashDiscountOrSurCharge(
                            receiptModel.totalAmount,
                            prefProvider,
                            requireContext()
                        )
                    ),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.totalPrice(str5)

                val totalAmt1 = MethodUtils.roundOffAmountDouble(receiptModel.totalAmount)
                val str51 = padLine(
                    "Pay by Card",
                    "$" + MethodUtils.roundOffAmountString(totalAmt1),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.totalPrice(str51)
            } else if (receiptModel.cashDiscountType == "SurCharge"
            ) {

                val str5 = padLine(
                    "Pay by Cash",
                    "$" + MethodUtils.roundOffAmountString(receiptModel.totalAmount),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.totalPrice(str5)

                val totalAmt1 = MethodUtils.roundOffAmountDouble(
                    receiptModel.totalAmount + MethodUtils.getLatestCashDiscountOrSurCharge(
                        receiptModel.totalAmount,
                        prefProvider,
                        requireContext()
                    )
                )
                val str51 = padLine(
                    "Pay by Card",
                    "$" + MethodUtils.roundOffAmountString(totalAmt1),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.totalPrice(str51)

            }

            if (customerSettingModel.showRefundAmount && printType == Constants.PRINT_PAID) {

                val str7 = padLine(
                    "Change Amount",
                    "$" + MethodUtils.roundOffAmountString(0.00),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.changeAmount(str7)
                SunmiPrinterApi.getInstance().lineWrap(2)


            }

            if (receiptModel?.totalTips == 0.0 && printType == Constants.PRINT_PAID) {


                if (customerSettingModel.showTipLineForCash) {

                    if (customerSettingModel.fonts == Constants.LARGE) {
                        PrintSunmiUtils.tips("Tips      _____________")
                        SunmiPrinterApi.getInstance().lineWrap(1)
                    } else {
                        PrintSunmiUtils.tips("Tips                              _____________")
                    }

                }

            }

            SunmiPrinterApi.getInstance().lineWrap(1)


            if (customerSettingModel.showTipSuggestion) {


                PrintSunmiUtils.additionalTips()

                if (tipsList.isNotEmpty()) {
                    addTipsList(
                        tipsList,
                        receiptModel.totalAmount.toDouble(),
                        customerSettingModel.fonts
                    )

                }
            }

            if (printType == Constants.PRINT_PAID) {

                val str10 = padLine(
                    "Transaction ID",
                    receiptModel.payments.get(0).transactionId,
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.transactionId(str10)
            }

            if (printType == Constants.PRINT_PAID) {

                val str11 = padLine(
                    "Transaction Type",
                    receiptModel.payments.get(0).paymentType,
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.transactionType(str11)
            }
            if (customerSettingModel.showCustomerAddress or customerSettingModel.showCustomerPhone or customerSettingModel.showCustomerName) {

                SunmiPrinterApi.getInstance().lineWrap(1)

                if (receiptModel.customer != null) {

                    PrintSunmiUtils.customerDetails()

                    if (customerSettingModel.showCustomerName) {
                        PrintSunmiUtils.customerName(receiptModel.customer.firstName + " " + receiptModel.customer.lastName)
                    }

                    if (customerSettingModel.showCustomerPhone) {
                        if (receiptModel?.customer?.phones?.isNotEmpty()) {

                            val phoneNoFormatted = MethodUtils.getUSFormatNumber(
                                receiptModel?.customer?.phones?.get(receiptModel?.customer?.phones?.size - 1).phoneNumber
                            )
                            PrintSunmiUtils.customerPhone(phoneNoFormatted)

                        }
                    }

                    if (customerSettingModel.showCustomerAddress) {
                        if (receiptModel.customer?.addresses?.isNotEmpty() == true) {

//                            PrintSunmiUtils.customerAddress(
//                                receiptModel.customer?.addresses?.get(receiptModel.customer?.addresses?.size - 1)?.fullAddress
//                            )

                            receiptModel.customer?.addresses.filter { it.typeOfAddress == Constants.SHIPPING_ADDRESS }
                                .forEach {

                                    if (it.typeOfAddress.equals(
                                            Constants.SHIPPING_ADDRESS,
                                            ignoreCase = true
                                        )
                                    ) {
                                        PrintSunmiUtils.customerAddress(
                                            it.fullAddress
                                        )
                                    }
                                }
                        }
                    }

                }
            }


            if (receiptModel.note != null && receiptModel.note != "" && customerSettingModel.showOrderNote) {
                SunmiPrinterApi.getInstance().lineWrap(1)
                PrintSunmiUtils.orderNote(receiptModel.note)
            }
            SunmiPrinterApi.getInstance().lineWrap(2)
            if (printType == Constants.PRINT_PAID) {
                val str8 = padLine(
                    "Customer Signature",
                    "     _________________________",
                    48
                ).toString()

                PrintSunmiUtils.customerSignature(str8)
            }

            if (customerSettingModel.showQrCode) {

                PrintSunmiUtils.qrCode(receiptModel.digitalReceiptUrl)
            }

            PrintSunmiUtils.cutPaper()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun printBusinessLogo() {
        val decodedString: ByteArray = Base64.decode(
            prefProvider.getValue(Constants.VENUE_LOGO, ""),
            Base64.DEFAULT
        )
        val bitmap: Bitmap =
            BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)

        val newBitmap = Bitmap.createScaledBitmap(bitmap!!, 210, 210, true)

        PrintSunmiUtils.printLogo(newBitmap)

    }

    private fun generateCartModelFromOrderModel(order: OnlineOrderResponseModel.Data): CartModel {
        LogUtil.logE("futureDeliveryDate  ", Gson().toJson(order))
        return CartModel().apply {
            terminalId = prefProvider.getValueInt(Constants.TERMINAL_ID, -1)
            employeeID = prefProvider.getValueInt(Constants.EMPLOYEE_ID, -1)
            locationId = order.locationId
            orderTypeId = order.orderTypeId
            orderType = order.orderType
            orderTypeName = order.orderType
            futureDeliveryDate = order.futureDeliveryDate.toString()
            isOpenOrder = true
            serviceCharge = serviceChargesList(order)
            customer = assignCustomer(order)
            note = order.note
            discountPrice = order.totalDiscount
            deliveryType = order.deliveryType ?: ""
            taxlistDynamic = getTaxBirfucationList(order.orderItems)
        }
    }

    private fun generateCartItemsListFromOrderModel(order: OnlineOrderResponseModel.Data): List<TbCartItem>? {
        return inventoryListNew(order)
    }

    // calculate taxes of order items
    private fun getTaxBirfucationList(orderItems: List<OnlineOrderResponseModel.Data.OrderItem>): ArrayList<TaxData> {
        var taxListDynamic: ArrayList<TaxData> = arrayListOf()
        if (orderItems.isNotEmpty()) {
            orderItems.forEach { orderItem ->
                var totalPrice =
                    (orderItem.price * orderItem.quantity) - (orderItem.discountAmount * orderItem.quantity)
                orderItem.orderItemModifiers.forEach { orderItemModifier ->
                    totalPrice += orderItemModifier.price * orderItemModifier.quantity
                }
                Log.d(TAG, "navigate: itemPrice : $totalPrice")
                var totaltaxtemp = 0.0
                orderItem.orderItemTax.forEach { orderItemTaxe ->
                    if (taxListDynamic?.isNotEmpty() == true) {
                        var found = -1
                        taxListDynamic.forEachIndexed { index, taxData ->
                            if (taxData.orderTaxId == orderItemTaxe.taxId) {
                                found = index
                                return@forEachIndexed
                            }
                        }
                        if (found == -1) {
                            var taxData: TaxData = TaxData(
                                orderItemTaxe.createdAt,
                                orderItemTaxe.taxId,
                                0,
                                orderItemTaxe.name,
                                orderItemTaxe.rate,
                                orderItemTaxe.taxType,
                                orderItemTaxe.updatedAt,
                                true,
                                orderItemTaxe.isDefault,
                                false,
                                "",
                                listOf(orderItemTaxe.orderItemId),
                                orderItemTaxe.taxId,
                                false,
                                getTaxFromTotalPrice(
                                    orderItemTaxe,
                                    totalPrice,
                                    orderItem
                                ),
                                totalPrice
                            )
                            taxListDynamic?.add(taxData)
                        } else {
                            taxListDynamic!![found].totalTaxTypePrice =
                                taxListDynamic!![found].totalTaxTypePrice + getTaxFromTotalPrice(
                                    orderItemTaxe,
                                    totalPrice,
                                    orderItem
                                )
                            taxListDynamic!![found].subTotalAmount =
                                taxListDynamic!![found].subTotalAmount + totalPrice
                        }
                        Log.d(TAG, "found : " + found)
                    } else {
                        var taxData: TaxData = TaxData(
                            orderItemTaxe.createdAt,
                            orderItemTaxe.taxId,
                            0,
                            orderItemTaxe.name,
                            orderItemTaxe.rate,
                            orderItemTaxe.taxType,
                            orderItemTaxe.updatedAt,
                            true,
                            orderItemTaxe.isDefault,
                            false,
                            "",
                            listOf(orderItemTaxe.orderItemId),
                            orderItemTaxe.taxId,
                            false,
                            getTaxFromTotalPrice(
                                orderItemTaxe,
                                totalPrice,
                                orderItem
                            ),
                            totalPrice
                        )
                        taxListDynamic.add(taxData)
                    }


                    Log.d(TAG, "navigate: " + totaltaxtemp)
                }

            }

            Log.d(TAG, "navigate: list " + Gson().toJson(taxListDynamic))
        }
        return taxListDynamic
    }


    fun getTaxFromTotalPrice(
        orderItemTaxe: OnlineOrderResponseModel.Data.OrderItem.OrderItemTax,
        totalPrice: Double,
        item: OnlineOrderResponseModel.Data.OrderItem
    ): Double {
        var totaltaxtemp = 0.0


        totaltaxtemp += if (orderItemTaxe.taxType == "Percentage") {
            if (totalPrice < 0.0) {

                String.format("%.2f", 0.00)
                    .toDouble()
            } else {
                val itemTaxPrice =
                    (orderItemTaxe.rate * totalPrice) / 100
                LogUtil.logE("itemTaxPrice", "" + itemTaxPrice)
                itemTaxPrice
            }

        } else {
            Log.d("yash", "taxCalculation: " + orderItemTaxe.taxType)
            if (totalPrice <= 0.0) {
                String.format("%.2f", 0.00)
                    .toDouble()
            } else {
                String.format("%.2f", orderItemTaxe.rate * item.quantity)
                    .toDouble()
            }
        }
        return totaltaxtemp
    }

    private fun inventoryList(order: OnlineOrderResponseModel.Data): List<TbItem>? {

        val inventoryModelList = ArrayList<TbItem>()

        order.orderItems.forEach {
            var ismanualsale = false
            var mannual_Sale_ID = ""
            if (it.itemId == 1) {
                mannual_Sale_ID = UUID.randomUUID().toString()
                ismanualsale = true
            }
            val items = TbItem().apply {
                orderItemId = it.id
                itemId = it.itemId
                id = it.custom_item_id
                name = it.itemName
                isManualSales = ismanualsale
                manualSaleId = mannual_Sale_ID
                price = it.price
                isEdited = it.isEdited
                itemQuantity = it.quantity
                sku = ""
                isHide = false
                sort = 0
                categoryId = it.categoryId
                categoryName = ""
                taxes = taxes(it.orderItemTax, order.locationId)
                modifier_set_ids = modifiersIds(it.orderItemModifiers)
                modifiers = modifierSets(it.orderItemModifiers)
                discountPrice = it.discountAmount
                discountType = it.discountType
                if (it.discountId != null)
                    discountId = it.discountId
                if (it.order_item_variation != null)
                    variationsAttributes = variationAtt(it.order_item_variation)
                note = it.note
            }

            inventoryModelList.add(items)

        }

        return inventoryModelList
    }

    private fun inventoryListNew(order: OnlineOrderResponseModel.Data): List<TbCartItem>? {

        val inventoryModelList = ArrayList<TbCartItem>()

        order.orderItems.forEach {
            var ismanualsale = false
            var mannual_Sale_ID = ""
            if (it.itemId == 1) {
                mannual_Sale_ID = UUID.randomUUID().toString()
                ismanualsale = true
            }
            val items = TbCartItem().apply {
                orderItemId = it.id
                itemId = it.itemId
                id = it.custom_item_id
                name = it.itemName
                isManualSales = ismanualsale
                manualSaleId = mannual_Sale_ID
                price = it.price
                isEdited = it.isEdited
                itemQuantity = it.quantity
                sku = ""
                isHide = false
                sort = 0
                categoryId = it.categoryId
                categoryName = ""
                taxes = taxes(it.orderItemTax, order.locationId)
                modifier_set_ids = modifiersIds(it.orderItemModifiers)
                modifiers = modifierSets(it.orderItemModifiers)
                discountPrice = it.discountAmount
                if (it.discountType == null) {
                    discountType = ""
                } else {
                    discountType = it.discountType
                }
                if (it.discountId != null)
                    discountId = it.discountId
                if (it.order_item_variation != null)
                    variationsAttributes = variationAtt(it.order_item_variation)
                note = it.note
                employeeID = prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
                orderType = prefProvider.getValue(Constants.ORDER_TYPE, "")
            }

            inventoryModelList.add(items)

        }

        return inventoryModelList
    }

    private fun variationAtt(variation: OrderItemVariationAttribute): List<VariationsAttribute> {

        val variationsAttributeList = ArrayList<VariationsAttribute>()

        val variationsAttribute = VariationsAttribute()
        variationsAttribute.id = variation.variationId
        variationsAttribute.name = variation.name
        variationsAttribute.price = variation.price
        variationsAttribute.orderVariationId = variation.id
        variationsAttributeList.add(variationsAttribute)

        return variationsAttributeList
    }

    private fun modifierSets(orderItemModifiers: List<OnlineOrderResponseModel.Data.OrderItem.OrderItemModifier>): List<Modifier> {

        val modifierList = ArrayList<Modifier>()

        orderItemModifiers.forEach {

            val modifier = Modifier().apply {
                id = it.modifierId
                modifierSetId = it.modifierSetId
                name = it.name
                price = it.price
                itemQuantity = it.quantity
                orderModifierId = it.id
                modifier_quantity = it.modifier_quantity

            }
            modifierList.add(modifier)
        }

        return modifierList
    }

    private fun modifiersIds(orderItemModifiers: List<OnlineOrderResponseModel.Data.OrderItem.OrderItemModifier>): List<Int> {

        val selectedIds = ArrayList<Int>()
        if (orderItemModifiers.isNotEmpty()) {
            orderItemModifiers.forEach {
                selectedIds.add(it.modifierSetId)
            }
        }
        var uniqueSelectedId = HashSet<Int>(selectedIds)
        return uniqueSelectedId.toList()
    }

    private fun taxes(
        taxs: List<OnlineOrderResponseModel.Data.OrderItem.OrderItemTax>,
        locationId: Int
    ): List<TaxData>? {
        val taxList = ArrayList<TaxData>()

        taxs.forEach {
            val tax = TaxData(
                it.createdAt,
                it.taxId,
                locationId,
                it.name,
                it.rate,
                it.taxType,
                it.updatedAt,
                true,
                it.isDefault,
                false,
                "",
                listOf(),
                it.id
            )
            taxList.add(tax)
        }

        return taxList
    }

    private fun assignCustomer(order: OnlineOrderResponseModel.Data): TbCustomer {

        val phoneList = ArrayList<TbPhones>()
        order.customer?.phones?.forEach {
            val phone = TbPhones(it.id, it.phoneNumber)
            phoneList.add(phone)
        }

        val addressList = ArrayList<TbAddress>()
        order.customer?.addresses?.forEach {
            val address = TbAddress(
                it.id,
                it.address1,
                it.address2,
                it.city,
                it.state,
                it.country ?: "",
                it.postcode ?: "",
                "",
                it.latitude ?: "",
                it.longitude ?: "",
                "",
                it.fullAddress,
                it.street
            )
            addressList.add(address)
        }

        return TbCustomer(
            order.customer?.id,
            order.customer?.firstName.toString(),
            order.customer?.lastName.toString(),
            order.customer?.birthDate.toString(),
            order.customer?.email.toString(),
            false,
            false,
            0,
            order.customer?.company.toString(),
            phoneList,
            addressList
        )
    }


    private fun serviceChargesList(order: OnlineOrderResponseModel.Data): List<TbServiceCharge> {

        val serviceChargeList = ArrayList<TbServiceCharge>()

        order.orderServiceCharges.forEach {
            val serviceCharge = TbServiceCharge(
                it.createdAt.toString(),
                it.serviceChargeId,
                true,
                order.locationId,
                it.max_guest_count,
                it.min_guest_count,
                it.name,
                it.order_type,
                it.rate,
                it.updatedAt.toString(),
                isActive = false,
                isChecked = true,
                order_service_charge_id = it.id
            )
            serviceChargeList.add(serviceCharge)
        }

        return serviceChargeList
    }

    // to get kitchen reciept settings
    private fun getKitchenReceiptSettings() {
        viewModel.getKitchenReceiptSettings().observe(viewLifecycleOwner) {

            if (it != null) {
                kitchenSettingModel = it
            }
        }
    }

    // To get connected kitchen printers
    private fun getKitchenPrinters(data: OnlineOrderResponseModel.Data) {
        CoroutineScope(Dispatchers.IO).launch {
            var it = viewModel.getKitchenPrinterList()

            if (isPrint) {

                isPrint = false
                it?.forEach {
                    if (it.status && checkItemsforPrinterOnlineOrder(
                            data.orderItems, it.printerCategories.toCollection(
                                arrayListOf()
                            )
                        )
                    ) {

                        Log.d("getKitchenPrinterList", "getKitchenPrinterList mmm")

                        initKitchenPrinter(
                            it,
                            Constants.KITCHEN,
                            data
                        )

                    }
                }
            }

            /*viewModel.getKitchenPrinterList().observe(viewLifecycleOwner) { it ->
                when (it.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()


                        if (isPrint) {

                            isPrint = false
                            it.data?.forEach {
                                if (it.status && checkItemsforPrinterOnlineOrder(
                                        data.orderItems, it.printerCategories.toCollection(
                                            arrayListOf()
                                        )
                                    )
                                ) {

                                    Log.d("getKitchenPrinterList", "getKitchenPrinterList mmm")

                                    initKitchenPrinter(
                                        it,
                                        Constants.KITCHEN,
                                        data
                                    )
                                    getAllOrders()

                                }
                            }
                        }
                    }

                    Status.LOADING -> {
                        ProgressUtils.showProgressDialog(requireActivity())
                    }

                    Status.ERROR -> {
                        ProgressUtils.dismissProgressDialog()
                        findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_allOrdersFragment)

                    }
                }

            }*/
        }

    }

    private fun initKitchenPrinter(
        data: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String,
        orderData: OnlineOrderResponseModel.Data
    ) {
        if (data.name.startsWith(SUNMI_PRINTER, true)) {

            try {
                SunmiPrinterApi.getInstance()
                    .setPrinter(SunmiPrinter.SunmiBlueToothPrinter, data.ipAddress)
            } catch (e: Exception) {
                SunmiPrinterApi.getInstance()
                    .setPrinter(SunmiPrinter.SunmiNetPrinter, data.ipAddress)
            }

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

                            Log.d("tracking printers", "In IF")
                            CoroutineScope(Dispatchers.Main).launch {

                                initKitchenPrinter(data, type, orderData)
                            }
                        }

                        override fun onDisconnect() {
                            println("onDisconnect")
                        }

                    })
            } else {
                Log.d("tracking printers", "In Else")
                generateKitchenReceiptSunmi(data, type, orderData)
            }

        } else if (data.name.startsWith(Constants.SUNMI_INNER_PRINTER, true)) {

            SunmiPrintHelper.getInstance().initSunmiPrinterService(requireContext())
            CoroutineScope(Dispatchers.IO).launch {
                delay(100)
                setServiceForKitchen(data, type, orderData)
            }
        } else if (data.name.contains("TSP", ignoreCase = true)) {
            settings = StarConnectionSettings(InterfaceType.Lan, data.macAddress)
            printer = StarPrinter(settings, requireContext())

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val builder = StarXpandCommandBuilder()

                    var printerBuilder = PrinterBuilder()

                    with(printerBuilder) {
                        styleInternationalCharacter(InternationalCharacterType.Usa)
                        styleCharacterSpace(0.0)
                        styleAlignment(Alignment.Center)

                        if (!oneItemPerReceipt) {
                            orderData.orderItems.forEach { item ->
                                data.printerCategories.toCollection(arrayListOf())?.forEach {
                                    if (it?.id == item.categoryId) {
                                        if (it.categoryActive && it.printerEnable) {
                                            for (singularity in 1..item.quantity) {

                                                add(
                                                    PrinterBuilder()
                                                        .styleBold(true)
                                                        .styleMagnification(
                                                            MagnificationParameter(3, 3)
                                                        )
                                                        .actionPrintText(
                                                            "OrderId: ${orderData.custom_order_id}"
                                                        )
                                                )

                                                actionFeedLine(1)

                                                add(
                                                    PrinterBuilder()
                                                        .styleBold(true)
                                                        .styleMagnification(
                                                            MagnificationParameter(2, 2)
                                                        )
                                                        .actionPrintText(
                                                            "${orderData.orderTypeName}"
                                                        )
                                                )

                                                actionFeedLine(1)

                                                if (orderData.orderType.contains("Phone", true)) {
                                                    add(
                                                        PrinterBuilder()
                                                            .styleBold(true)
                                                            .styleMagnification(
                                                                MagnificationParameter(2, 2)
                                                            )
                                                            .actionPrintText(
                                                                "${orderData.deliveryType}"
                                                            )
                                                    )

                                                    actionFeedLine(1)
                                                }

                                                add(
                                                    PrinterBuilder()
                                                        .styleAlignment(Alignment.Left)
                                                        .styleMagnification(
                                                            MagnificationParameter(2, 2)
                                                        )
                                                        .actionPrintText(
                                                            content = addSingleReprintOrdersForStarKitchen(
                                                                1,
                                                                item,
                                                                data.printerCategories.toCollection(
                                                                    arrayListOf()
                                                                )
                                                            )
                                                        )
                                                )

                                                actionFeedLine(1)

                                                var printedName = StringBuilder("")
                                                orderData.customer?.firstName?.let { firstName ->
                                                    orderData.customer?.lastName?.let { lastName ->
                                                        if (kitchenSettingModel.showCustomerName) {
                                                            if (!firstName.contains(
                                                                    "customer",
                                                                    ignoreCase = true
                                                                )
                                                            ) {
                                                                printedName.append(firstName)
                                                                printedName.append(" ")
                                                            }

                                                            if (!lastName.isBlank()) {
                                                                printedName.append(lastName)
                                                            }

                                                            if (printedName.isNotEmpty()) {
                                                                add(
                                                                    PrinterBuilder()
                                                                        .styleAlignment(Alignment.Left)
                                                                        .styleBold(true)
                                                                        .actionPrintText(
                                                                            content = "Customer Details\n"
                                                                        )
                                                                )

                                                                add(
                                                                    PrinterBuilder()
                                                                        .styleAlignment(Alignment.Center)
                                                                        .actionPrintText(
                                                                            content =
                                                                            "--------------------------------------------"
                                                                        )
                                                                )

                                                                add(
                                                                    PrinterBuilder()
                                                                        .styleAlignment(Alignment.Left)
                                                                        .actionPrintText(
                                                                            content = printedName.toString()
                                                                        )
                                                                )

                                                            }
                                                        }

                                                    }
                                                }

                                                actionFeedLine(1)

                                                add(
                                                    PrinterBuilder()
                                                        .actionPrintText(
                                                            Constants.getReceiptFormatDateFromUTCServer(
                                                                requireContext(),
                                                                orderData?.createdAt.toString()
                                                            )
                                                        )
                                                )

                                                printerBuilder.actionFeedLine(1)
                                                actionCut(CutType.Partial)

                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            add(
                                PrinterBuilder()
                                    .styleBold(true)
                                    .styleMagnification(
                                        MagnificationParameter(3, 3)
                                    )
                                    .actionPrintText(
                                        "OrderId: ${orderData.custom_order_id}"
                                    )
                            )

                            styleAlignment(Alignment.Center)

                            add(
                                PrinterBuilder()
                                    .styleBold(true)
                                    .actionPrintText(
                                        if (kitchenSettingModel.showOrderType)
                                            orderData.orderType
                                        else ""
                                    )
                            )

                            actionFeedLine(1)

                            if (orderData.orderType == Constants.PHONE_ORDER_) {
                                add(
                                    PrinterBuilder()
                                        .styleBold(true)
                                        .actionPrintText(
                                            orderData.deliveryType
                                        )
                                )

                                actionFeedLine(1)
                            }

                            add(
                                PrinterBuilder()
                                    .actionPrintText(
                                        "Employee:${
                                            prefProvider.getValue(
                                                Constants.EMPLOYEE_NAME,
                                                ""
                                            )
                                        }"
                                    )
                            )
                            actionFeedLine(1)

                            add(
                                PrinterBuilder()
                                    .actionPrintText(
                                        Constants.getReceiptFormatDateFromUTCServer(
                                            requireContext(),
                                            orderData.createdAt.toString()
                                        )
                                    )
                            )

                            actionFeedLine(1)

                            add(
                                PrinterBuilder()
                                    .styleBold(true)
                                    .actionPrintText(
                                        "--------------------------------------------"
                                    )
                            )

                            actionFeedLine(1)

                            add(
                                PrinterBuilder()
                                    .styleAlignment(Alignment.Left)
                                    .actionPrintText(
                                        content = addReprintOrdersForStarKitchen(
                                            orderData.orderItems!!,
                                            data.printerCategories.toCollection(arrayListOf())
                                        )
                                    )
                            )

                            actionFeedLine(1)
                            if (orderData.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote == true) {
                                add(
                                    PrinterBuilder()
                                        .styleAlignment(Alignment.Center)
                                        .styleBold(true)
                                        .actionPrintText(
                                            content = if (orderData.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote == true) {
                                                "--------------------------------------------\nOrder Note\n "
                                            } else ""
                                        )
                                )
                            }
                            if (orderData.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote == true) {
                                add(
                                    PrinterBuilder()
                                        .styleAlignment(Alignment.Center)
                                        .actionPrintText(
                                            content = if (orderData.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote == true) {
                                                orderData.note.toString()
                                            } else ""
                                        )
                                )
                            }
                            actionFeedLine(1)
                            if (kitchenSettingModel.showCustomerName && (orderData.customer?.firstName != null || orderData.customer?.lastName != null)) {
                                add(
                                    PrinterBuilder()
                                        .styleAlignment(Alignment.Left)
                                        .styleBold(true)
                                        .actionPrintText(
                                            content = if (kitchenSettingModel.showCustomerName && (orderData.customer?.firstName != null || orderData.customer?.lastName != null)) {
                                                "Customer Details\n"
                                            } else ""
                                        )
                                )
                            }

                            if (kitchenSettingModel.showCustomerName && (orderData.customer?.firstName != null || orderData.customer?.lastName != null)) {
                                add(
                                    PrinterBuilder()
                                        .styleAlignment(Alignment.Center)
                                        .actionPrintText(
                                            content = if (kitchenSettingModel.showCustomerName && (orderData.customer?.firstName != null || orderData.customer?.lastName != null)) {
                                                "--------------------------------------------"
                                            } else ""
                                        )
                                )
                            }
                            if (kitchenSettingModel.showCustomerName && (orderData.customer?.firstName != null || orderData.customer?.lastName != null)) {
                                add(
                                    PrinterBuilder()
                                        .styleAlignment(Alignment.Left)
                                        .actionPrintText(
                                            content = if (kitchenSettingModel.showCustomerName && (orderData.customer?.firstName != null || orderData.customer?.lastName != null)) {
                                                orderData.customer?.firstName + " " + orderData.customer?.lastName
                                            } else ""
                                        )
                                )
                            }
                            if (kitchenSettingModel.showCustomerPhone && orderData.customer?.phones?.get(
                                    0
                                ) != null
                            ) {
                                add(
                                    PrinterBuilder()
                                        .styleAlignment(Alignment.Left)
                                        .actionPrintText(
                                            content = if (kitchenSettingModel.showCustomerPhone && orderData.customer?.phones?.get(
                                                    0
                                                ) != null
                                            ) {

                                                var phoneNumber =
                                                    orderData.customer?.phones?.get(
                                                        0
                                                    )?.phoneNumber.toString()
                                                if (phoneNumber.length != 10) {
                                                    // Handle invalid input (must be 10 digits)
                                                    "Invalid phone number"
                                                }

                                                val areaCode = phoneNumber.substring(0, 3)
                                                val firstPart = phoneNumber.substring(3, 6)
                                                val secondPart = phoneNumber.substring(6)

                                                "($areaCode)$firstPart-$secondPart"

                                            } else ""
                                        )
                                )
                            }
                            printerBuilder.actionFeedLine(1).actionCut(CutType.Partial)
                        }

                    }


//                    printerBuilder.actionFeedLine(1).actionCut(CutType.Partial)

                    var document = DocumentBuilder()
                        .addPrinter(printerBuilder)
                    builder.addDocument(
                        document
                    )

                    val commands = builder.getCommands()

                    printer.openAsync().await()

//                    val jobSettings = StarSpoolJobSettings(true, 30, "Print from Android")

                    printer.printAsync(commands).await()

                    try {
                        SunmiPrintHelper.getInstance().openCashBox()
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                    }

                    Log.d("Printing", "Success")
                } catch (e: Exception) {
                    Log.d("Printing", "Error: ${e}")
                } finally {
                    printer.closeAsync().await()
                }

                adapter.enableReprintKitchenReceiptButton()
            }

        } else {

            if (!data.name.substring(0, 6).toString().lowercase()
                    .contains("TM-m".lowercase())
            ) {
                var mPrinter = if (data.name.substring(0, 6).toString().lowercase()
                        .contains("TM-m".lowercase())
                ) {
                    Log.e(TAG, "YesContains")
                    Printer(
                        Printer.TM_M30,
                        Printer.MODEL_ANK, requireContext()
                    )
                } else {
                    Printer(
                        Printer.TM_U220,
                        Printer.MODEL_ANK, requireContext()
                    )


                }

                mPrinter.setReceiveEventListener { printer, i, printerStatusInfo, s ->

                    Log.e(
                        TAG,
                        "PrinterEvent  ${Gson().toJson(printerStatusInfo)} other1 ${s}  other2 ${i}"
                    )
                    if (printerStatusInfo.online == 1) {
                        try {
                            printer.disconnect()

                        } catch (e: java.lang.Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                try {
                    Log.e(TAG, "printerDataType:  ${data.printer_type}")

                    var printerAdd =
                        if (data.printer_type == Constants.BLUETOOTH) "BT:" + data.macAddress else "TCP:" + data.ipAddress
                    mPrinter.connect(
                        printerAdd,
                        Printer.PARAM_DEFAULT
                    )
                    mPrinter.startMonitor()

                    generateKitchenReceiptU220(data, type, orderData, mPrinter)

                    // generateReceiptForU220(mPrinter, data, type)

                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            } else {
                PrinterClass.closePrinter()
                if (PrinterClass.getPrinter() == null) {
                    //  printerDialog.show(requireContext())

                    var printer: Print? = Print(requireContext())
                    /*if (printer != null) {
                   printer.setStatusChangeEventCallback(this)
                   printer.setBatteryStatusChangeEventCallback(this)
               }*/


                    val enabled = Print.FALSE

                    try {

                        printer?.openPrinter(
                            if (data.printer_type == Constants.BLUETOOTH) {
                                Print.DEVTYPE_BLUETOOTH
                            } else {
                                Print.DEVTYPE_TCP
                            },
                            data.ipAddress,
                            enabled,
                            1000
                        )
                        printer?.setStatusChangeEventCallback(this)

                    } catch (e: Exception) {
                        //  printerDialog.dismiss()
                        LogUtil.logE(TAG, "PrinterException: " + e.message)
                        printer = null
                        return
                    }

                    if (printer != null) {
                        PrinterClass.setPrinter(printer)


                        generateKitchenReceipt(data, type, orderData)

                    }

                } else {
                    LogUtil.logE(TAG, "PrinterIsNotNull:")
                }

            }

        }

    }

    private fun generateKitchenReceiptU220(
        customerReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String,
        orderData: OnlineOrderResponseModel.Data,
        builder: Printer
    ) {

        try {
            val pname = if (customerReceiptPrinters.name.substring(0, 6).toString()
                    .lowercase() == "TM-m30".lowercase()
            ) {
                "TM-m30"
            } else {
                customerReceiptPrinters.name
            }

            var fontSizeH = 1
            var fontSizeW = 1
            when (kitchenSettingModel.fonts) {
                Constants.SMALL -> {
                    fontSizeH = 1
                    fontSizeW = 1
                }

                Constants.MEDIUM -> {
                    fontSizeH = 1
                    fontSizeW = 2
                }

                Constants.LARGE -> {
                    fontSizeH = 2
                    fontSizeW = 2
                }


            }


            builder.addFeedUnit(30)
            builder.addFeedLine(2)
            if (customerReceiptPrinters.name.substring(0, 4)
                    .equals("TM-U", true) || customerReceiptPrinters.name.contains("U")
            ) {
                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(2, 2)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )
                if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                    builder.addText("OrderID:" + orderData.custom_order_id)
                } else {
                    builder.addText("OrderID:" + orderData.id)

                }
                builder.addFeedUnit(30)
                builder.addFeedLine(1)

                if (kitchenSettingModel.showOrderType) {


                    builder.addFeedLine(0)
                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )
                    builder.addTextAlign(Builder.ALIGN_CENTER)

                    addBuilderTextForU220(builder, orderData.orderTypeName.toString())

                    if ((orderData.orderType.equals(Constants.PHONE_ORDER, true) ||
                                orderData.orderType.equals("OnlineWebOrder", true) ||
                                orderData.orderType.equals("Online Order", true) ||
                                orderData.orderType.equals(
                                    "OnlineOrder",
                                    true
                                )) && orderData.deliveryType != null
                    ) {
                        builder.addFeedLine(1)
                        builder.addTextFont(Builder.FONT_E)
                        builder.addTextLang(Builder.LANG_EN)
                        builder.addTextSize(fontSizeH, fontSizeW)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.TRUE,
                            Builder.COLOR_1
                        )
                        builder.addTextAlign(Builder.ALIGN_CENTER)

                        addBuilderTextForU220(builder, orderData.deliveryType.toString())
                        builder.addFeedLine(1)
                    }
                }


                if (kitchenSettingModel.showTeamMember) {

                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    //  builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )
                    builder.addText(
                        padLine(
                            "Employee:" + prefProvider.getValue(EMPLOYEE_NAME, ""), "",
                            33
                        )
                    )

                }
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                //  builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(fontSizeH, fontSizeW)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        Constants.getReceiptFormatDateFromUTCServer(
                            requireContext(),
                            orderData.createdAt
                        ),
                        "",
                        33
                    )
                )

                builder.addFeedLine(1)

                builder.addTextFont(Builder.FONT_B)
                //builder.addTextLineSpace(20)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(fontSizeH, fontSizeW)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                addHorizontalKitchenLineForU220(builder)


                addOrdersForKitchenOnlineOrderU220(
                    builder,
                    orderData.orderItems,
                    fontSizeH,
                    fontSizeW,
                    customerReceiptPrinters.printerCategories.toCollection(arrayListOf())
                )


                if (orderData.note.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {
                    builder.addFeedUnit(30)
                    builder.addFeedLine(1)
                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextAlign(Builder.ALIGN_LEFT)
                    //builder.addTextLineSpace(20)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )
                    builder.addText("Order Note")

                    builder.addFeedUnit(30)

                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextAlign(Builder.ALIGN_CENTER)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )


                    builder.addText(orderData.note.toString())
                }


                if (kitchenSettingModel.showCustomerAddress != false or kitchenSettingModel.showCustomerPhone != false or kitchenSettingModel.showCustomerName != false) {
                    if (orderData.customer != null) {

                        builder.addFeedUnit(30)
                        builder.addFeedLine(1)
                        builder.addTextFont(Builder.FONT_E)
                        //builder.addTextLineSpace(20)
                        builder.addTextAlign(Builder.ALIGN_LEFT)
                        builder.addTextLang(Builder.LANG_EN)
                        builder.addTextSize(fontSizeH, fontSizeW)
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
                        builder.addTextSize(fontSizeH, fontSizeW)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.COLOR_1
                        )
                        addHorizontalKitchenLineForU220(builder)

                        if (kitchenSettingModel.showCustomerName) {

                            builder.addFeedUnit(30)
                            builder.addTextFont(Builder.FONT_E)
                            builder.addTextAlign(Builder.ALIGN_LEFT)
                            //builder.addTextLineSpace(20)
                            builder.addTextLang(Builder.LANG_EN)
                            builder.addTextSize(fontSizeH, fontSizeW)
                            builder.addTextStyle(
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.TRUE,
                                Builder.COLOR_1
                            )
                            builder.addText(orderData.customer.firstName + " " + orderData.customer.lastName)

                        }


                        if (kitchenSettingModel.showCustomerPhone) {

                            if (orderData.customer.phones.isNotEmpty() == true) {
                                builder.addFeedUnit(30)
                                builder.addTextFont(Builder.FONT_E)
                                builder.addTextAlign(Builder.ALIGN_LEFT)
                                //builder.addTextLineSpace(20)
                                builder.addTextLang(Builder.LANG_EN)
                                builder.addTextSize(fontSizeH, fontSizeW)
                                builder.addTextStyle(
                                    Builder.FALSE,
                                    Builder.FALSE,
                                    Builder.TRUE,
                                    Builder.COLOR_1
                                )
                                builder.addText(
                                    MethodUtils.getUSFormatNumber(
                                        orderData.customer.phones?.get(
                                            orderData.customer.phones.size - 1
                                        )?.phoneNumber
                                    )
                                )
                            }

                        }
                        /* builder.addTextLineSpace(30)
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
                 builder.addText(receiptModel?.order?.customer?.email)*/


                        if (orderData.customer.addresses.isNotEmpty() == true) {

                            builder.addFeedUnit(30)
                            builder.addTextFont(Builder.FONT_E)
                            builder.addTextAlign(Builder.ALIGN_LEFT)
                            //builder.addTextLineSpace(20)
                            builder.addTextLang(Builder.LANG_EN)
                            builder.addTextSize(fontSizeH, fontSizeW)
                            builder.addTextStyle(
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.TRUE,
                                Builder.COLOR_1
                            )

                            orderData.customer.addresses.filter {
                                it.typeOfAddress == Constants.BILLING_ADDRESS
                            }

                                .forEach {

                                    if (it.typeOfAddress.equals(
                                            Constants.BILLING_ADDRESS,
                                            ignoreCase = true
                                        )
                                    ) {
                                        builder.addText(
                                            it.fullAddress
                                        )
                                    }
                                }
//                            builder.addText(orderData?.data?.customer?.addresses.get(orderData?.data?.customer?.addresses.size - 1).fullAddress)
                        }
                    }


                }
            } else {


                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(2, 2)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                    builder.addText("OrderID:" + orderData.custom_order_id)
                } else {
                    builder.addText("OrderID:" + orderData.id)
                }
                builder.addFeedUnit(30)
                builder.addFeedLine(1)

                if (kitchenSettingModel.showOrderType) {


                    builder.addFeedLine(0)
                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )
                    builder.addTextAlign(Builder.ALIGN_CENTER)

                    addBuilderTextForU220(builder, orderData.orderTypeName.toString())

                    if ((orderData.orderType.equals(Constants.PHONE_ORDER, true) ||
                                orderData.orderType.equals("OnlineWebOrder", true) ||
                                orderData.orderType.equals("Online Order", true) ||
                                orderData.orderType.equals(
                                    "OnlineOrder",
                                    true
                                )) && orderData.deliveryType != null
                    ) {
                        builder.addFeedLine(1)
                        builder.addTextFont(Builder.FONT_E)
                        builder.addTextLang(Builder.LANG_EN)
                        builder.addTextSize(fontSizeH, fontSizeW)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.TRUE,
                            Builder.COLOR_1
                        )
                        builder.addTextAlign(Builder.ALIGN_CENTER)

                        addBuilderTextForU220(builder, orderData.deliveryType.toString())
                        builder.addFeedLine(1)
                    }
                }



                if (kitchenSettingModel.showTeamMember) {

                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    //  builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )
                    builder.addText(
                        padLine(
                            "Employee:" + prefProvider.getValue(EMPLOYEE_NAME, ""), "",
                            if (kitchenSettingModel.fonts == Constants.LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )

                }
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                //  builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(fontSizeH, fontSizeW)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        Constants.getReceiptFormatDateFromUTCServer(
                            requireContext(),
                            orderData.createdAt
                        ),
                        "",
                        if (kitchenSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )


                builder.addFeedLine(1)

                addHorizontalKitchenLineForU220(builder)


                addOrdersForKitchenOnlineOrderU220(
                    builder,
                    orderData.orderItems,
                    fontSizeH,
                    fontSizeW,
                    customerReceiptPrinters.printerCategories.toCollection(arrayListOf())
                )


                if (orderData.note.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {
                    builder.addFeedUnit(30)
                    builder.addFeedLine(1)
                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextAlign(Builder.ALIGN_LEFT)
                    //builder.addTextLineSpace(20)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )
                    builder.addText("Order Note")

                    builder.addFeedUnit(30)

                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextAlign(Builder.ALIGN_CENTER)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )


                    builder.addText(orderData.note.toString())
                }


                if (kitchenSettingModel.showCustomerAddress != false or kitchenSettingModel.showCustomerPhone != false or kitchenSettingModel.showCustomerName != false) {
                    if (orderData.customer != null) {

                        builder.addFeedUnit(30)
                        builder.addFeedLine(1)
                        builder.addTextFont(Builder.FONT_E)
                        //builder.addTextLineSpace(20)
                        builder.addTextAlign(Builder.ALIGN_LEFT)
                        builder.addTextLang(Builder.LANG_EN)
                        builder.addTextSize(fontSizeH, fontSizeW)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.TRUE,
                            Builder.COLOR_1
                        )
                        builder.addText("Customer Details" + "\n")

                        builder.addFeedLine(1)
                        addHorizontalKitchenLineForU220(builder)

                        if (kitchenSettingModel.showCustomerName) {

                            builder.addFeedUnit(30)
                            builder.addTextFont(Builder.FONT_E)
                            builder.addTextAlign(Builder.ALIGN_LEFT)
                            //builder.addTextLineSpace(20)
                            builder.addTextLang(Builder.LANG_EN)
                            builder.addTextSize(fontSizeH, fontSizeW)
                            builder.addTextStyle(
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.TRUE,
                                Builder.COLOR_1
                            )
                            builder.addText(orderData.customer?.firstName + " " + orderData.customer?.lastName)

                        }


                        if (kitchenSettingModel.showCustomerPhone) {

                            if (orderData.customer.phones.isNotEmpty() == true) {
                                builder.addFeedUnit(30)
                                builder.addTextFont(Builder.FONT_E)
                                builder.addTextAlign(Builder.ALIGN_LEFT)
                                //builder.addTextLineSpace(20)
                                builder.addTextLang(Builder.LANG_EN)
                                builder.addTextSize(fontSizeH, fontSizeW)
                                builder.addTextStyle(
                                    Builder.FALSE,
                                    Builder.FALSE,
                                    Builder.TRUE,
                                    Builder.COLOR_1
                                )
                                builder.addText(
                                    MethodUtils.getUSFormatNumber(
                                        orderData.customer.phones.get(
                                            orderData.customer.phones.size - 1
                                        )?.phoneNumber
                                    )
                                )
                            }

                        }
                        /* builder.addTextLineSpace(30)
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
                 builder.addText(receiptModel?.order?.customer?.email)*/


                        if (orderData.customer.addresses.isNotEmpty() == true) {

                            builder.addFeedUnit(30)
                            builder.addTextFont(Builder.FONT_E)
                            builder.addTextAlign(Builder.ALIGN_LEFT)
                            //builder.addTextLineSpace(20)
                            builder.addTextLang(Builder.LANG_EN)
                            builder.addTextSize(fontSizeH, fontSizeW)
                            builder.addTextStyle(
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.TRUE,
                                Builder.COLOR_1
                            )

                            orderData.customer.addresses.filter { it.typeOfAddress == Constants.BILLING_ADDRESS }
                                .forEach {

                                    if (it.typeOfAddress.equals(
                                            Constants.BILLING_ADDRESS,
                                            ignoreCase = true
                                        )
                                    ) {
                                        builder.addText(
                                            it.fullAddress
                                        )
                                    }
                                }
//                            builder.addText(orderData?.data?.customer?.addresses.get(orderData?.data?.customer?.addresses.size - 1).fullAddress)
                        }
                    }


                }
            }
        } catch (e: Exception) {
            // printerDialog.dismiss()
            e.printStackTrace()
        }

        builder?.addFeedLine(2)

        builder?.addCut(Builder.CUT_FEED)

        try {
            builder.sendData(Printer.PARAM_DEFAULT)


        } catch (e: java.lang.Exception) {

            e.printStackTrace()
        }

        /*val status = IntArray(1)
        val battery = IntArray(1)

        var timeOut = PrinterClass.SEND_TIMEOUT
        if (customerReceiptPrinters.printer_type == Constants.BLUETOOTH) {
            timeOut = PrinterClass.BLUETOOTH_TIMEOUT
        }

        if (customerReceiptPrinters.name.substring(0, 6).toString()
                .lowercase() == "TM-m30".lowercase() && customerReceiptPrinters.printer_type != Constants.BLUETOOTH
        ) {

            timeOut = 1000
        }


*/
        /*  try {
              PrinterClass.getPrinter()?.sendData(
                  builder,
                  timeOut, status, battery
              )

              //printerDialog.dismiss()
              PrinterClass.closePrinter()

              //PrinterClass.getPrinter()?.sendData(builder, 0, status, battery)
          } catch (e: Exception) {
  //                printerDialog.dismiss()
              PrinterClass.closePrinter()
              e.printStackTrace()
              LogUtil.logE(TAG, "PrinterError: " + e.localizedMessage)
          }
  */
    }

    private fun generateKitchenReceipt(
        customerReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String,
        orderData: OnlineOrderResponseModel.Data,
    ) {
        var builder: Builder? = null
        try {
            val pname = if (customerReceiptPrinters.name.substring(0, 6).toString()
                    .lowercase() == "TM-m30".lowercase()
            ) {
                "TM-m30"
            } else {
                customerReceiptPrinters.name
            }

            var fontSizeH = 1
            var fontSizeW = 1
            when (kitchenSettingModel.fonts) {
                Constants.SMALL -> {
                    fontSizeH = 1
                    fontSizeW = 1
                }

                Constants.MEDIUM -> {
                    fontSizeH = 1
                    fontSizeW = 2
                }

                Constants.LARGE -> {
                    fontSizeH = 2
                    fontSizeW = 2
                }


            }

            builder = Builder(pname, PrinterClass.language, requireActivity())
            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)
            builder.addFeedLine(2)
            if (customerReceiptPrinters.name.substring(0, 4)
                    .equals("TM-U", true) || customerReceiptPrinters.name.contains("U")
            ) {
                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(2, 2)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )
                if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                    builder.addText("OrderID:" + orderData.custom_order_id)
                } else {
                    builder.addText("OrderID:" + orderData.id)

                }
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addFeedLine(1)

                if (kitchenSettingModel.showOrderType) {


                    builder.addFeedLine(0)
                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )
                    builder.addTextAlign(Builder.ALIGN_CENTER)

                    addBuilderText(builder, orderData.orderTypeName.toString())

                    if ((orderData.orderType.equals(Constants.PHONE_ORDER, true) ||
                                orderData.orderType.equals("OnlineWebOrder", true) ||
                                orderData.orderType.equals("Online Order", true) ||
                                orderData.orderType.equals(
                                    "OnlineOrder",
                                    true
                                )) && orderData.deliveryType != null
                    ) {
                        builder.addFeedLine(1)
                        builder.addTextFont(Builder.FONT_E)
                        builder.addTextLang(Builder.LANG_EN)
                        builder.addTextSize(fontSizeH, fontSizeW)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.TRUE,
                            Builder.COLOR_1
                        )
                        builder.addTextAlign(Builder.ALIGN_CENTER)

                        addBuilderText(builder, orderData.deliveryType.toString())
                        builder.addFeedLine(1)
                    }
                }

                if (kitchenSettingModel.showTeamMember) {

                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    //  builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )
                    builder.addText(
                        padLine(
                            "Employee:" + prefProvider.getValue(EMPLOYEE_NAME, ""), "",
                            33
                        )
                    )

                }
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                //  builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(fontSizeH, fontSizeW)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        Constants.getReceiptFormatDateFromUTCServer(
                            requireContext(),
                            orderData.createdAt
                        ),
                        "",
                        33
                    )
                )

                builder.addFeedLine(1)

                builder.addTextFont(Builder.FONT_B)
                //builder.addTextLineSpace(20)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(fontSizeH, fontSizeW)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                addHorizontalKitchenLine(builder)


                addOrdersForKitchenOnlineOrder(
                    builder,
                    orderData.orderItems,
                    fontSizeH,
                    fontSizeW,
                    customerReceiptPrinters.printerCategories.toCollection(arrayListOf())
                )


                if (orderData.note.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addFeedLine(1)
                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextAlign(Builder.ALIGN_LEFT)
                    //builder.addTextLineSpace(20)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
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
                    builder.addTextAlign(Builder.ALIGN_CENTER)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )


                    builder.addText(orderData.note.toString())
                }


                if (kitchenSettingModel.showCustomerAddress != false or kitchenSettingModel.showCustomerPhone != false or kitchenSettingModel.showCustomerName != false) {
                    if (orderData.customer != null) {

                        builder.addTextLineSpace(30)
                        builder.addFeedUnit(30)
                        builder.addFeedLine(1)
                        builder.addTextFont(Builder.FONT_E)
                        //builder.addTextLineSpace(20)
                        builder.addTextAlign(Builder.ALIGN_LEFT)
                        builder.addTextLang(Builder.LANG_EN)
                        builder.addTextSize(fontSizeH, fontSizeW)
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
                        builder.addTextSize(fontSizeH, fontSizeW)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.COLOR_1
                        )
                        addHorizontalKitchenLine(builder)

                        if (kitchenSettingModel.showCustomerName) {

                            builder.addTextLineSpace(30)
                            builder.addFeedUnit(30)
                            builder.addTextFont(Builder.FONT_E)
                            builder.addTextAlign(Builder.ALIGN_LEFT)
                            //builder.addTextLineSpace(20)
                            builder.addTextLang(Builder.LANG_EN)
                            builder.addTextSize(fontSizeH, fontSizeW)
                            builder.addTextStyle(
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.TRUE,
                                Builder.COLOR_1
                            )
                            builder.addText(orderData.customer.firstName + " " + orderData.customer.lastName)

                        }


                        if (kitchenSettingModel.showCustomerPhone) {

                            if (orderData.customer.phones.isNotEmpty() == true) {
                                builder.addTextLineSpace(30)
                                builder.addFeedUnit(30)
                                builder.addTextFont(Builder.FONT_E)
                                builder.addTextAlign(Builder.ALIGN_LEFT)
                                //builder.addTextLineSpace(20)
                                builder.addTextLang(Builder.LANG_EN)
                                builder.addTextSize(fontSizeH, fontSizeW)
                                builder.addTextStyle(
                                    Builder.FALSE,
                                    Builder.FALSE,
                                    Builder.TRUE,
                                    Builder.COLOR_1
                                )
                                builder.addText(
                                    MethodUtils.getUSFormatNumber(
                                        orderData.customer.phones.get(
                                            orderData.customer.phones.size - 1
                                        )?.phoneNumber
                                    )
                                )
                            }

                        }
                        /* builder.addTextLineSpace(30)
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
                 builder.addText(receiptModel?.order?.customer?.email)*/


                        if (orderData.customer.addresses.isNotEmpty() == true) {

                            builder.addTextLineSpace(30)
                            builder.addFeedUnit(30)
                            builder.addTextFont(Builder.FONT_E)
                            builder.addTextAlign(Builder.ALIGN_LEFT)
                            //builder.addTextLineSpace(20)
                            builder.addTextLang(Builder.LANG_EN)
                            builder.addTextSize(fontSizeH, fontSizeW)
                            builder.addTextStyle(
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.TRUE,
                                Builder.COLOR_1
                            )

                            orderData.customer.addresses.filter {
                                it.typeOfAddress == Constants.BILLING_ADDRESS
                            }

                                .forEach {

                                    if (it.typeOfAddress.equals(
                                            Constants.BILLING_ADDRESS,
                                            ignoreCase = true
                                        )
                                    ) {
                                        builder.addText(
                                            it.fullAddress
                                        )
                                    }
                                }
//                            builder.addText(orderData?.data?.customer?.addresses.get(orderData?.data?.customer?.addresses.size - 1).fullAddress)
                        }
                    }


                }
            } else {


                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(2, 2)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                    builder.addText("OrderID:" + orderData.custom_order_id)
                } else {
                    builder.addText("OrderID:" + orderData.id)
                }
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addFeedLine(1)

                if (kitchenSettingModel.showOrderType) {


                    builder.addFeedLine(0)
                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )
                    builder.addTextAlign(Builder.ALIGN_CENTER)

                    addBuilderText(builder, orderData.orderTypeName.toString())

                    if ((orderData.orderType.equals(Constants.PHONE_ORDER, true) ||
                                orderData.orderType.equals("OnlineWebOrder", true) ||
                                orderData.orderType.equals("Online Order", true) ||
                                orderData.orderType.equals(
                                    "OnlineOrder",
                                    true
                                )) && orderData.deliveryType != null
                    ) {
                        builder.addFeedLine(1)
                        builder.addTextFont(Builder.FONT_E)
                        builder.addTextLang(Builder.LANG_EN)
                        builder.addTextSize(fontSizeH, fontSizeW)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.TRUE,
                            Builder.COLOR_1
                        )
                        builder.addTextAlign(Builder.ALIGN_CENTER)

                        addBuilderText(builder, orderData.deliveryType.toString())
                        builder.addFeedLine(1)
                    }
                }



                if (kitchenSettingModel.showTeamMember) {

                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    //  builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )
                    builder.addText(
                        padLine(
                            "Employee:" + prefProvider.getValue(EMPLOYEE_NAME, ""), "",
                            if (kitchenSettingModel.fonts == Constants.LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )

                }
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                //  builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(fontSizeH, fontSizeW)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        Constants.getReceiptFormatDateFromUTCServer(
                            requireContext(),
                            orderData.createdAt
                        ),
                        "",
                        if (kitchenSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )


                builder.addFeedLine(1)

                addHorizontalLine(builder)


                addOrdersForKitchenOnlineOrder(
                    builder,
                    orderData.orderItems,
                    fontSizeH,
                    fontSizeW,
                    customerReceiptPrinters.printerCategories.toCollection(arrayListOf())
                )


                if (orderData.note.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addFeedLine(1)
                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextAlign(Builder.ALIGN_LEFT)
                    //builder.addTextLineSpace(20)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
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
                    builder.addTextAlign(Builder.ALIGN_CENTER)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )


                    builder.addText(orderData.note.toString())
                }


                if (kitchenSettingModel.showCustomerAddress != false or kitchenSettingModel.showCustomerPhone != false or kitchenSettingModel.showCustomerName != false) {
                    if (orderData.customer != null) {

                        builder.addTextLineSpace(30)
                        builder.addFeedUnit(30)
                        builder.addFeedLine(1)
                        builder.addTextFont(Builder.FONT_E)
                        //builder.addTextLineSpace(20)
                        builder.addTextAlign(Builder.ALIGN_LEFT)
                        builder.addTextLang(Builder.LANG_EN)
                        builder.addTextSize(fontSizeH, fontSizeW)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.TRUE,
                            Builder.COLOR_1
                        )
                        builder.addText("Customer Details" + "\n")

                        builder.addFeedLine(1)
                        addHorizontalLine(builder)

                        if (kitchenSettingModel.showCustomerName) {

                            builder.addTextLineSpace(30)
                            builder.addFeedUnit(30)
                            builder.addTextFont(Builder.FONT_E)
                            builder.addTextAlign(Builder.ALIGN_LEFT)
                            //builder.addTextLineSpace(20)
                            builder.addTextLang(Builder.LANG_EN)
                            builder.addTextSize(fontSizeH, fontSizeW)
                            builder.addTextStyle(
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.TRUE,
                                Builder.COLOR_1
                            )
                            builder.addText(orderData.customer.firstName + " " + orderData.customer.lastName)

                        }


                        if (kitchenSettingModel.showCustomerPhone) {

                            if (orderData.customer.phones.isNotEmpty() == true) {
                                builder.addTextLineSpace(30)
                                builder.addFeedUnit(30)
                                builder.addTextFont(Builder.FONT_E)
                                builder.addTextAlign(Builder.ALIGN_LEFT)
                                //builder.addTextLineSpace(20)
                                builder.addTextLang(Builder.LANG_EN)
                                builder.addTextSize(fontSizeH, fontSizeW)
                                builder.addTextStyle(
                                    Builder.FALSE,
                                    Builder.FALSE,
                                    Builder.TRUE,
                                    Builder.COLOR_1
                                )
                                builder.addText(
                                    MethodUtils.getUSFormatNumber(
                                        orderData.customer.phones.get(
                                            orderData.customer.phones.size - 1
                                        ).phoneNumber
                                    )
                                )
                            }

                        }
                        /* builder.addTextLineSpace(30)
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
                 builder.addText(receiptModel?.order?.customer?.email)*/


                        if (orderData.customer.addresses.isNotEmpty() == true) {

                            builder.addTextLineSpace(30)
                            builder.addFeedUnit(30)
                            builder.addTextFont(Builder.FONT_E)
                            builder.addTextAlign(Builder.ALIGN_LEFT)
                            //builder.addTextLineSpace(20)
                            builder.addTextLang(Builder.LANG_EN)
                            builder.addTextSize(fontSizeH, fontSizeW)
                            builder.addTextStyle(
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.TRUE,
                                Builder.COLOR_1
                            )

                            orderData.customer.addresses.filter { it.typeOfAddress == Constants.BILLING_ADDRESS }
                                .forEach {

                                    if (it.typeOfAddress.equals(
                                            Constants.BILLING_ADDRESS,
                                            ignoreCase = true
                                        )
                                    ) {
                                        builder.addText(
                                            it.fullAddress
                                        )
                                    }
                                }
//                            builder.addText(orderData?.data?.customer?.addresses.get(orderData?.data?.customer?.addresses.size - 1).fullAddress)
                        }
                    }


                }
            }
        } catch (e: Exception) {
            // printerDialog.dismiss()
            e.printStackTrace()
        }

        builder?.addFeedLine(2)

        builder?.addCut(Builder.CUT_FEED)

        val status = IntArray(1)
        val battery = IntArray(1)

        var timeOut = PrinterClass.SEND_TIMEOUT
        if (customerReceiptPrinters.printer_type == Constants.BLUETOOTH) {
            timeOut = PrinterClass.BLUETOOTH_TIMEOUT
        }

        if (customerReceiptPrinters.name.substring(0, 6).toString()
                .lowercase() == "TM-m30".lowercase() && customerReceiptPrinters.printer_type != Constants.BLUETOOTH
        ) {

            timeOut = 1000
        }



        try {
            PrinterClass.getPrinter()?.sendData(
                builder,
                timeOut, status, battery
            )

            //printerDialog.dismiss()
            PrinterClass.closePrinter()

            //PrinterClass.getPrinter()?.sendData(builder, 0, status, battery)
        } catch (e: Exception) {
//                printerDialog.dismiss()
            PrinterClass.closePrinter()
            e.printStackTrace()
            LogUtil.logE(TAG, "PrinterError: " + e.localizedMessage)
        }


    }


    private fun generateKitchenReceiptSunmi(
        customerReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String,
        orderData: OnlineOrderResponseModel.Data
    ) {

        try {

            PrintSunmiUtils.fontSize(kitchenSettingModel.fonts)
            SunmiPrinterApi.getInstance().lineWrap(2)
            if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                PrintSunmiUtils.orderIdSunmi(
                    "OrderID:" + orderData.custom_order_id
                )
            } else {
                PrintSunmiUtils.orderIdSunmi(
                    "OrderID:" + orderData.id
                )
            }

            SunmiPrinterApi.getInstance().lineWrap(1)

            if (kitchenSettingModel.showOrderType) {
                PrintSunmiUtils.printOrderType(orderData.orderTypeName.toString())
                SunmiPrinterApi.getInstance().lineWrap(1)

                if ((orderData.orderType.equals(Constants.PHONE_ORDER, true) ||
                            orderData.orderType.equals("OnlineWebOrder", true) ||
                            orderData.orderType.equals("Online Order", true) ||
                            orderData.orderType.equals(
                                "OnlineOrder",
                                true
                            )) && orderData.deliveryType != null
                ) {
                    PrintSunmiUtils.printOrderType(orderData.deliveryType.toString())
                    SunmiPrinterApi.getInstance().lineWrap(1)
                }
            }

            if (kitchenSettingModel.showTeamMember) {

                PrintSunmiUtils.employee(
                    padLine(
                        "Employee:" + prefProvider.getValue(EMPLOYEE_NAME, ""), "",
                        if (kitchenSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                )


            }

            PrintSunmiUtils.orderTime(
                padLine(
                    Constants.getReceiptFormatDateFromUTCServer(
                        requireContext(),
                        orderData.createdAt
                    ),
                    "",
                    if (kitchenSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
            )

            PrintSunmiUtils.addHorizontal()


            addOrdersForKitchenOnlineOrderSunmi(
                orderData.orderItems,
                customerReceiptPrinters.printerCategories.toCollection(arrayListOf())
            )


            if (orderData.note.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {
                SunmiPrinterApi.getInstance().lineWrap(1)
                PrintSunmiUtils.orderNote(orderData?.note.toString())
            }


            if (kitchenSettingModel.showCustomerAddress != false or kitchenSettingModel.showCustomerPhone != false or kitchenSettingModel.showCustomerName != false) {
                if (orderData.customer != null) {
                    SunmiPrinterApi.getInstance().lineWrap(1)
                    PrintSunmiUtils.customerDetails()

                    if (kitchenSettingModel.showCustomerName) {
                        PrintSunmiUtils.customerName(orderData.customer.firstName + " " + orderData.customer.lastName)

                    }


                    if (kitchenSettingModel.showCustomerPhone) {

                        if (orderData.customer.phones.isNotEmpty() == true) {

                            PrintSunmiUtils.customerPhone(
                                MethodUtils.getUSFormatNumber(
                                    orderData.customer.phones.get(
                                        orderData.customer.phones.size - 1
                                    ).phoneNumber
                                )

                            )
                        }

                    }


                    if (orderData.customer.addresses.isNotEmpty() == true) {

//                        orderData.customer.addresses.filter { typeOfAddress == Constants.BILLING_ADDRESS }
//
//                            .forEach {
//
//                                if (typeOfAddress.equals(
//                                        Constants.BILLING_ADDRESS,
//                                        ignoreCase = true
//                                    )
//                                ) {
//                                    PrintSunmiUtils.customerAddress(
//                                        fullAddress
//                                    )
//                                }
//                            }


                        PrintSunmiUtils.customerAddress(
                            orderData.customer.addresses.get(
                                orderData.customer.addresses.size - 1
                            ).fullAddress
                        )
                    }
                }


            }

            PrintSunmiUtils.cutPaper()
        } catch (e: Exception) {
            // printerDialog.dismiss()
            e.printStackTrace()
        }


    }

    private fun setServiceForCustomer(
        customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters,
        type: String,
        order: OnlineOrderResponseModel.Data,
        printType: String
    ) {
        if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.FoundSunmiPrinter) {

            LogUtil.logE("SunmiPrintHelper", "FoundSunmiPrinter")

            if (!BluetoothUtil.isBlueToothPrinter) {

                generatePrintSunmiInner(customerReceiptPrinters, type, order, printType)

            }

        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.CheckSunmiPrinter) {
            Handler(Looper.getMainLooper()).postDelayed({
                setServiceForCustomer(
                    customerReceiptPrinters,
                    type,
                    order,
                    printType
                )
            }, 2000)
            LogUtil.logE("SunmiPrintHelper", "CheckSunmiPrinter")
        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.LostSunmiPrinter) {

            LogUtil.logE("SunmiPrintHelper", "LostSunmiPrinter")
        } else {
            LogUtil.logE("SunmiPrintHelper", "ELSE")
        }
    }

    private fun generatePrintSunmiInner(
        customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters,
        type: String,
        receiptModel: OnlineOrderResponseModel.Data,
        printType: String
    ) {
        try {

            PrintSunmiUtils.fontSizeInner(customerSettingModel.fonts)

            SunmiPrintHelper.getInstance().initPrinter()
            if (customerSettingModel.showOrderIdTop) {
                if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                    PrintSunmiUtils.headerText("OrderID:" + receiptModel?.custom_order_id)
                } else {
                    PrintSunmiUtils.headerText("OrderID:" + receiptModel?.id)
                }
                SunmiPrintHelper.getInstance().lineWrap(1)
            }


            if (customerSettingModel.showVenueLogo && prefProvider.getValue(
                    Constants.VENUE_LOGO,
                    ""
                )
                    .isNotEmpty()
            ) {

                PrintSunmiUtils.printLogoInner(
                    prefProvider.getValue(
                        Constants.VENUE_LOGO,
                        ""
                    )
                )
            }


            if (printType == Constants.PRINT_PAID) {
                PrintSunmiUtils.headerText("Paid")
            } else {
                PrintSunmiUtils.headerText("Unpaid")
            }

            PrintSunmiUtils.printBusinessDetailsInner(
                prefProvider.getValue(Constants.BUSINESS_NAME, ""),
                if (customerSettingModel.showVenueAddress) prefProvider.getValue(
                    Constants.BUSINESS_ADDRESS,
                    ""
                ) else "", prefProvider.getValue(
                    Constants.BUSINESS_PHONE_NO,
                    ""
                )
                /* if (customerSettingModel.showVenuePhone) prefProvider.getValue(
                     Constants.BUSINESS_PHONE_NO,
                     ""
                 ) else ""*/
            )
            if (customerSettingModel.showWebsiteAddress) {
                PrintSunmiUtils.venueWebsiteInner(
                    prefProvider.getValue(
                        Constants.BUSINESS_WEBSITE,
                        ""
                    )
                )
            } else {
                SunmiPrintHelper.getInstance().lineWrap(1)
            }
            if (customerSettingModel.showOrderType) {
                PrintSunmiUtils.headerText(receiptModel?.orderTypeName?.trim())
            }


            if (receiptModel?.orderType?.lowercase() == Constants.PHONE_ORDER.lowercase()
            // || receiptModel?.orderType?.lowercase() == Constants.OPEN_ORDER.lowercase()
            ) {
                PrintSunmiUtils.headerText(receiptModel?.deliveryType)

            }
            SunmiPrintHelper.getInstance().lineWrap(1)




            if (customerSettingModel.fonts == Constants.LARGE) {


                PrintSunmiUtils.normalText("ReceiptID:" + receiptModel?.offlineId)


                if (customerSettingModel.showTeam && receiptModel?.employee?.name != null) {

                    PrintSunmiUtils.normalText("Employee:" + receiptModel?.employee?.name)

                }

                if (customerSettingModel.showOrderTime) {


                    PrintSunmiUtils.normalText(
                        "Order Time:" + Constants.getReceiptFormatDateFromUTCServer(
                            requireContext(),
                            receiptModel?.createdAt.toString()
                        )
                    )

                }

                if (customerSettingModel.showPrintTime) {


                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                        PrintSunmiUtils.normalText(
                            "Print Time:" + Constants.getCurrentTimeFromTimeZone(
                                requireContext(),
                                MethodUtils.formatted()
                            )
                        )
                    }


                }
            } else {

                PrintSunmiUtils.normalText("ReceiptID:" + receiptModel?.offlineId)
                if (customerSettingModel.showTeam) {


                    val empName = padLine(
                        if (customerSettingModel.showTeam && receiptModel?.employee?.name != null) {
                            "Employee:" + receiptModel.employee?.name
                        } else {
                            ""
                        },
                        "",
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()

                    PrintSunmiUtils.normalText(empName)
                }
                if (customerSettingModel.showOrderTime) {


                    val orderTime = padLine(
                        if (customerSettingModel.showOrderTime) {
                            "Order Time:" + Constants.getReceiptFormatDateFromUTCServer(
                                requireContext(),
                                receiptModel?.createdAt.toString()
                            )
                        } else {
                            ""
                        },
                        "",
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()

                    PrintSunmiUtils.normalText(orderTime)
                }


                if (customerSettingModel.showPrintTime) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                        val printTime = padLine(
                            if (customerSettingModel.showPrintTime) {
                                "Print Time:${MethodUtils.formatted()}"
                            } else {
                                ""
                            },
                            "",
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                23
                            } else {
                                48
                            }
                        ).toString()

                        PrintSunmiUtils.normalText(printTime)

                    }
                }
            }


            PrintSunmiUtils.addHorizontalInner()



            receiptModel.orderItems.let {
                addOrderItemOnlineOrderSunmiInner(
                    it,
                    customerSettingModel.fonts,
                    customerSettingModel.showModifiers
                )
            }

            SunmiPrintHelper.getInstance().lineWrap(2)


            if (receiptModel?.totalDiscount != null) {

                val str1 = padLine(
                    "Total Discount",

                    if (receiptModel.totalDiscount == 0.0) {
                        "$" + MethodUtils.roundOffAmountString(receiptModel.totalDiscount)
                    } else {
                        "-$" + MethodUtils.roundOffAmountString(receiptModel.totalDiscount)
                    },
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.normalText(str1)

            }


            val str2 = padLine(
                "Sub Total",
                "$" + MethodUtils.roundOffAmountString(receiptModel.subTotal),
                if (customerSettingModel.fonts == Constants.LARGE) {
                    23
                } else {
                    48
                }
            ).toString()

            PrintSunmiUtils.normalText(str2)


            if (receiptModel?.totalTaxAmount != null) {


                val str3 = padLine(
                    "Tax",
                    "$" + MethodUtils.roundOffAmountString(receiptModel.totalTaxAmount),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.normalText(str3)
            }

            val result =
                prefProvider.getValueboolean(
                    Constants.SERVICECHARGE_TAKEOUT_OPENORDER,
                    false
                )

            if (receiptModel.totalServiceCharges != null && result) {

                val str4 = padLine(
                    "Service Charge",
                    "$" + MethodUtils.roundOffAmountString(receiptModel.totalServiceCharges),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.normalText(str4)
            }

            if (receiptModel?.totalTips != 0.0) {


                val str8 = padLine(
                    "Tips",
                    "$" + receiptModel.totalTips?.let {
                        MethodUtils.roundOffAmountString(
                            it
                        )
                    },
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.normalText(str8)


            }




            if (receiptModel.cash_discount_or_surcharge != 0.0 && customerSettingModel.showCashDisSurCharg) {


                if (receiptModel.payments.isNotEmpty() && receiptModel.payments.get(
                        receiptModel.payments.size - 1
                    ).paymentType.lowercase() == "Card".lowercase()
                ) {

                    val str8 = padLine(
                        Constants.SURCHARGE_TEXT,
                        "$" + MethodUtils.roundOffAmountString(receiptModel.cash_discount_or_surcharge!!),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                    PrintSunmiUtils.normalText(str8)

                } else {

                    val str8 = padLine(
                        "Cash Discount",
                        if (receiptModel.cash_discount_or_surcharge == 0.0) {
                            "$" + MethodUtils.roundOffAmountString(receiptModel.cash_discount_or_surcharge!!)
                        } else {
                            "-$" + MethodUtils.roundOffAmountString(receiptModel.cash_discount_or_surcharge!!)
                        },
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                    PrintSunmiUtils.normalText(str8)

                }
            }

            if (receiptModel?.isLoyaltyApplied == true && receiptModel?.loyaltyAmount != 0.0) {

                val str8 = padLine(
                    "Used Loyalty Amount",
                    "-$" + receiptModel?.loyaltyAmount?.let {
                        MethodUtils.roundOffAmountString(
                            it
                        )
                    },
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()

                PrintSunmiUtils.normalText(str8)

                val str9 = padLine(
                    "Used Loyalty Points",
                    receiptModel?.usedRewardPoints.toString(),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()

                PrintSunmiUtils.normalText(str9)


            }




            SunmiPrintHelper.getInstance().lineWrap(1)

            if (receiptModel.totalAmount != null) {

                val totalAmt = MethodUtils.roundOffAmountDouble(receiptModel.totalAmount)

                val str5 = padLine(
                    "Total Price",
                    "$" + MethodUtils.roundOffAmountString(totalAmt),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.boldText(str5)

            }

            if (receiptModel.cashDiscountType == "CashDiscount"
            ) {

                val str5 = padLine(
                    "Pay by Cash",
                    "$" + MethodUtils.roundOffAmountString(
                        receiptModel.totalAmount - MethodUtils.getLatestCashDiscountOrSurCharge(
                            receiptModel.totalAmount,
                            prefProvider,
                            requireContext()
                        )
                    ),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.boldText(str5)

                val totalAmt1 = MethodUtils.roundOffAmountDouble(receiptModel.totalAmount)
                val str51 = padLine(
                    "Pay by Card",
                    "$" + MethodUtils.roundOffAmountString(totalAmt1),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.boldText(str51)
            } else if (receiptModel.cashDiscountType == "SurCharge"
            ) {

                val str5 = padLine(
                    "Pay by Cash",
                    "$" + MethodUtils.roundOffAmountString(receiptModel.totalAmount),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.boldText(str5)

                val totalAmt1 = MethodUtils.roundOffAmountDouble(
                    receiptModel.totalAmount + MethodUtils.getLatestCashDiscountOrSurCharge(
                        receiptModel.totalAmount,
                        prefProvider,
                        requireContext()
                    )
                )
                val str51 = padLine(
                    "Pay by Card",
                    "$" + MethodUtils.roundOffAmountString(totalAmt1),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.boldText(str51)

            }

            if (customerSettingModel.showRefundAmount && printType == Constants.PRINT_PAID) {

                val str7 = padLine(
                    "Change Amount",
                    "$" + MethodUtils.roundOffAmountString(0.00),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.boldText(str7)
                SunmiPrintHelper.getInstance().lineWrap(2)


            }

            if (receiptModel?.totalTips == 0.0 && printType == Constants.PRINT_PAID) {


                var tip = ""

                if (receiptModel.totalTips != 0.0) {
                    tip = receiptModel.totalTips.toString()
                }
                if (customerSettingModel.showTipLineForCash) {

                    if (customerSettingModel.fonts == Constants.LARGE) {
                        PrintSunmiUtils.boldText("Tips      _____________")
                        SunmiPrintHelper.getInstance().lineWrap(1)
                    } else {
                        PrintSunmiUtils.boldText("Tips                              _____________")
                    }
                }

            }

            SunmiPrintHelper.getInstance().lineWrap(1)


            if (customerSettingModel.showTipSuggestion) {


                PrintSunmiUtils.additionalTipsInner()

                if (tipsList.isNotEmpty()) {
                    addTipsListInner(
                        tipsList,
                        receiptModel.totalAmount.toDouble(),
                        customerSettingModel.fonts
                    )

                }
            }

            if (printType == Constants.PRINT_PAID) {

                val str10 = padLine(
                    "Transaction ID",
                    receiptModel.payments.get(0).transactionId,
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.normalText(str10)
            }

            if (printType == Constants.PRINT_PAID) {

                val str11 = padLine(
                    "Transaction Type",
                    receiptModel.payments.get(0).paymentType,
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.normalText(str11)
            }
            if (customerSettingModel.showCustomerAddress or customerSettingModel.showCustomerPhone or customerSettingModel.showCustomerName) {

                SunmiPrintHelper.getInstance().lineWrap(1)

                if (receiptModel.customer != null) {

                    PrintSunmiUtils.customerDetailsInner()

                    if (customerSettingModel.showCustomerName) {
                        PrintSunmiUtils.normalText(receiptModel.customer.firstName + " " + receiptModel.customer.lastName)
                    }

                    if (customerSettingModel.showCustomerPhone) {
                        if (receiptModel?.customer?.phones?.isNotEmpty()) {

                            val phoneNoFormatted = MethodUtils.getUSFormatNumber(
                                receiptModel?.customer?.phones?.get(receiptModel?.customer?.phones?.size - 1).phoneNumber
                            )
                            PrintSunmiUtils.normalText(phoneNoFormatted)

                        }
                    }

                    if (customerSettingModel.showCustomerAddress) {
                        if (receiptModel.customer?.addresses?.isNotEmpty() == true) {

                            receiptModel.customer?.addresses.filter { it.typeOfAddress == Constants.SHIPPING_ADDRESS }
                                .forEach {

                                    if (it.typeOfAddress.equals(
                                            Constants.SHIPPING_ADDRESS,
                                            ignoreCase = true
                                        )
                                    ) {
                                        PrintSunmiUtils.normalText(
                                            it.fullAddress
                                        )
                                    }
                                }

//                            PrintSunmiUtils.normalText(
//                                receiptModel.customer?.addresses?.get(receiptModel.customer?.addresses?.size - 1)?.fullAddress
//                            )
                        }
                    }

                }
            }


            if (receiptModel.note != null && receiptModel.note != "" && customerSettingModel.showOrderNote) {
                SunmiPrintHelper.getInstance().lineWrap(1)
                PrintSunmiUtils.orderNoteInner(receiptModel.note)
            }
            SunmiPrintHelper.getInstance().lineWrap(2)

            if (printType == Constants.PRINT_PAID) {
                if (customerSettingModel.fonts == Constants.LARGE) {
                    PrintSunmiUtils.boldText("Customer Signature ____")
                } else {
                    PrintSunmiUtils.boldText("Customer Signature           __________________")
                }

                SunmiPrintHelper.getInstance().lineWrap(2)
            }
            if (customerSettingModel.showQrCode) {

                PrintSunmiUtils.qrCodeInner(receiptModel.digitalReceiptUrl)
            }

            PrintSunmiUtils.cutPaperInner()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setServiceForKitchen(
        data: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String,
        orderData: OnlineOrderResponseModel.Data
    ) {
        if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.FoundSunmiPrinter) {

            LogUtil.logE("SunmiPrintHelper", "FoundSunmiPrinter")

            if (!BluetoothUtil.isBlueToothPrinter) {

                generateKitchenReceiptSunmiInner(data, type, orderData)

            }

        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.CheckSunmiPrinter) {
            Handler(Looper.getMainLooper()).postDelayed({
                setServiceForKitchen(data, type, orderData)
            }, 2000)
            LogUtil.logE("SunmiPrintHelper", "CheckSunmiPrinter")
        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.LostSunmiPrinter) {

            LogUtil.logE("SunmiPrintHelper", "LostSunmiPrinter")
        } else {
            LogUtil.logE("SunmiPrintHelper", "ELSE")
        }
    }


    private fun generateKitchenReceiptSunmiInner(
        customerReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String,
        orderData: OnlineOrderResponseModel.Data
    ) {

        try {

            SunmiPrintHelper.getInstance().initPrinter()
            SunmiPrintHelper.getInstance().lineWrap(2)
            if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                PrintSunmiUtils.headerText("OrderID:" + orderData.custom_order_id)
            } else {
                PrintSunmiUtils.headerText("OrderID:" + orderData.id)
            }
            SunmiPrintHelper.getInstance().lineWrap(1)


            if (kitchenSettingModel.showOrderType) {
                PrintSunmiUtils.headerText(orderData.orderTypeName.toString())
                SunmiPrintHelper.getInstance().lineWrap(1)

                if ((orderData.orderType.equals(Constants.PHONE_ORDER, true) ||
                            orderData.orderType.equals("OnlineWebOrder", true) ||
                            orderData.orderType.equals("Online Order", true) ||
                            orderData.orderType.equals(
                                "OnlineOrder",
                                true
                            )) && orderData.deliveryType != null
                ) {
                    PrintSunmiUtils.headerText(orderData.deliveryType.toString())
                    SunmiPrintHelper.getInstance().lineWrap(1)
                }
            }

            if (kitchenSettingModel.showTeamMember) {

                PrintSunmiUtils.normalTextLarge(
                    "Employee:" + prefProvider.getValue(
                        EMPLOYEE_NAME,
                        ""
                    )
                )


            }

            PrintSunmiUtils.normalTextLarge(
                Constants.getReceiptFormatDateFromUTCServer(
                    requireContext(),
                    orderData.createdAt
                )
            )



            PrintSunmiUtils.addHorizontalInner()


            addOrdersForKitchenOnlineOrderSunmiInner(
                orderData.orderItems,
                customerReceiptPrinters.printerCategories.toCollection(arrayListOf())
            )


            if (orderData.note.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {

                PrintSunmiUtils.orderNoteInnerLarge(orderData.note.toString())
            }


            if (kitchenSettingModel.showCustomerAddress != false or kitchenSettingModel.showCustomerPhone != false or kitchenSettingModel.showCustomerName != false) {
                if (orderData.customer != null) {

                    PrintSunmiUtils.customerDetailsInner()

                    if (kitchenSettingModel.showCustomerName) {
                        PrintSunmiUtils.normalTextLarge(orderData.customer.firstName + " " + orderData.customer.lastName)

                    }


                    if (kitchenSettingModel.showCustomerPhone) {

                        if (orderData.customer.phones.isNotEmpty() == true) {

                            PrintSunmiUtils.normalTextLarge(
                                MethodUtils.getUSFormatNumber(
                                    orderData.customer.phones?.get(
                                        orderData.customer.phones.size - 1
                                    ).phoneNumber
                                )

                            )
                        }

                    }


                    if (orderData.customer.addresses.isNotEmpty() == true) {


//                        PrintSunmiUtils.normalTextLarge(
//                            orderData?.data?.customer?.addresses.get(
//                                orderData?.data?.customer?.addresses.size - 1
//                            ).fullAddress
//                        )


                        orderData.customer.addresses.filter { it.typeOfAddress == Constants.BILLING_ADDRESS }
                            .forEach {

                                if (it.typeOfAddress.equals(
                                        Constants.BILLING_ADDRESS,
                                        ignoreCase = true
                                    )
                                ) {
                                    PrintSunmiUtils.normalTextLarge(
                                        it.fullAddress
                                    )
                                }
                            }
                    }
                }


            }

            PrintSunmiUtils.cutPaperInner()
        } catch (e: Exception) {
            // printerDialog.dismiss()
            e.printStackTrace()
        }


    }

    override fun onStatusChangeEvent(p0: String?, p1: Int) {

    }


    private fun refreshCurrentFragment() {
        getAllOrders()
    }

}
