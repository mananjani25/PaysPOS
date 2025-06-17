package com.pays.pos.ui.fragments.customer

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.pays.payments.design.Dejavoo
import com.pays.payments.design.PaymentGatewayFactory
import com.pays.payments.design.PaymentGatewayType
import com.pays.payments.design.TransactionType
import com.pays.pos.data.entities.TbCustomer
import com.pays.pos.data.model.CustomerSearchList
import com.pays.pos.data.model.requestModel.CreateCustomerRequestModel
import com.pays.pos.data.model.responseModel.BaseResponse
import com.pays.pos.data.model.responseModel.GetOrderDetailsResponse
import com.pays.pos.data.model.responseModel.giftCardOrderHistory.GiftCardRecord
import com.pays.pos.data.model.responseModel.orderhistory.Orders
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.repositories.PosRepository
import com.pays.pos.data.repositories.TaxServiceChargeRepository
import com.pays.pos.di.PrefProvider
import com.pays.pos.logger.MessageEvent
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.Event
import com.pays.pos.utils.statusUtils.Resource
import com.pays.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.greenrobot.eventbus.EventBus
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import javax.inject.Inject
import javax.xml.parsers.DocumentBuilderFactory

@HiltViewModel
public class CustomerListViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val taxServiceChargeRepository: TaxServiceChargeRepository,
    private val prefProvider: PrefProvider
) : ViewModel() {

    var customerId: String? = ""
    private lateinit var resource: Resource<BaseResponse>

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _orderResponse = MutableLiveData<Event<GetOrderDetailsResponse.Data>>()
    val orderResponse: LiveData<Event<GetOrderDetailsResponse.Data>> = _orderResponse


    val _customerListResponse = MutableLiveData<Event<ArrayList<TbCustomer?>>>()
    val _customerNoDataFound = MutableLiveData<Event<String>>()


    private val mdata = MutableLiveData<Event<BaseResponse?>>()
    val data: LiveData<Event<BaseResponse?>> = mdata


    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    private val _orderHistory = MutableLiveData<Event<List<Orders>?>>()
    val orderHistory: LiveData<Event<List<Orders>?>> = _orderHistory

    private val _giftCardOrderHistory = MutableLiveData<Event<List<GiftCardRecord>?>>()
    val giftCardOrderHistory: LiveData<Event<List<GiftCardRecord>?>> = _giftCardOrderHistory

    private val _token = MutableLiveData<Event<Boolean>>()
    val token :  LiveData<Event<Boolean>> = _token

    val serviceCharges = posRepository.serviceChargeList()

    var itemlist = posRepository.getWholeItemFromPos()

    val enableTaxes = taxServiceChargeRepository.enableTaxes()  // fetch active taxes from DB

    private lateinit var addCustomerData: CreateCustomerRequestModel
    val addCustomerDetails = MutableLiveData(CreateCustomerRequestModel())

    var cardToken = ""

    /*-----------Customer Loyalty----------------*/
    private val _customerCount = MutableLiveData<Event<Boolean>>()
    val customerCount: LiveData<Event<Boolean>> = _customerCount
    /*-----------Customer Loyalty----------------*/


    fun customerList(data: LinkedHashMap<String, String>) =
        posRepository.customerListPagination(data)

    /*-----------Customer Loyalty----------------*/
    suspend fun fetchCustomersList(data: LinkedHashMap<String, String>) =
        posRepository.fetchCustomersList(data)
    /*-----------Customer Loyalty----------------*/


    fun deleteCart() {
        EventBus.getDefault().post(MessageEvent("${Constants.LINE_BREAK_TAB} CustomerListViewModel.kt_CART_MODEL_CLEAR Thread.dumpStack(): it1 -> ${Gson().toJson(Thread.currentThread().stackTrace)}"))
        viewModelScope.launch {
            posRepository.deleteCart(prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0))
        }
    }

    fun getData() {
        _showProgress.value = Event(true)
    }

    fun deleteTbl() {
        viewModelScope.launch {

            //   posRepository.deleteCustomer()
        }
    }

    private var searchJob: Job? = null
    fun searchByTextCustomer(query: String) {
        searchJob?.cancel()
        searchJob=viewModelScope.launch {
            val resource: Resource<CustomerSearchList> = posRepository.searchCustomer(query)
            when (resource.status) {
                Status.SUCCESS -> {
                    resource.data?.let { response ->
                        if (response.status == 200) {
                            resource.data.let { customerList ->

                                val customerDataList: ArrayList<TbCustomer?> = arrayListOf<TbCustomer?>()
                                customerList.data.forEach {
                                    customerDataList.add(
                                        TbCustomer(
                                            id = it.id,
                                            first_name = it.first_name,
                                            last_name = it.last_name,
                                            birth_date = it.birth_date,
                                            email = it.email,
                                            enroll_to_loyalty = it.enroll_to_loyalty,
                                            final_reward = it.final_reward,
                                            company = it.company,
                                            phones = it.phones,
                                            addresses = it.addresses
                                        )
                                    )
                                }
                                if (customerList.data.isNotEmpty()) {
                                    _customerListResponse.value = Event(customerDataList)
                                } else {
                                    _customerNoDataFound.value = Event(customerList.message)
                                }

                            }
                        } else {
                            _snackbarText.value = Event(resource.message)
                        }
                    }
                }
                Status.ERROR -> {
                    _snackbarText.value = Event(resource.message)

                    _showProgress.value = Event(false)
                }
                Status.LOADING -> {
                    _showProgress.value = Event(true)
                }
            }
        }
    }

    fun delete(id: Int?) {
        viewModelScope.launch {
            resource = posRepository.deleteCustomer(id)

            when (resource.status) {
                Status.SUCCESS -> {

                    _showProgress.value = Event(false)
                    resource.data.let {
                        if (it?.status == 200) {
                            resource.data?.let {
                                posRepository.deleteCustomerDataBase(id)
                                mdata.value = Event(it)
                            }
                        } else {
                            _snackbarText.value = Event(resource.message)
                        }
                    }

                }
                Status.LOADING -> {
                    _showProgress.value = Event(true)

                }
                Status.ERROR -> {
                    _snackbarText.value = Event(resource.message)
                    _showProgress.value = Event(false)

                }

            }
        }


    }

    suspend fun getTotalCustomersCount(): Int {
        return posRepository.getTotalCustomersCount()
    }

    /*----------Customer Loyalty------------*/
    fun hasCustomers() {
        CoroutineScope(Dispatchers.IO).launch {
            _customerCount.postValue(Event(posRepository.hasItem()))
        }
    }
    /*----------Customer Loyalty------------*/

    fun getReportSummary(isFromSearch: Boolean) {

        if (!isFromSearch) {
            _showProgress.value = Event(true)
        }
        viewModelScope.launch {

            val resourceReport =
                posRepository.getOrderHistory(
                    id = customerId ?: "",
                )
            when (resourceReport.status) {
                Status.SUCCESS -> {
                    if (!isFromSearch) {
                        _showProgress.value = Event(false)
                    }
                    resourceReport.data.let {
                        _orderHistory.postValue(Event(it?.data?.ordersList))
                        _giftCardOrderHistory.postValue(Event(it?.data?.giftCardsList))
                    }
                }

                Status.ERROR -> {
                    _snackbarText.value = Event(resourceReport.message)
                    if (!isFromSearch) {
                        _showProgress.value = Event(false)
                    }
                }

                Status.LOADING -> {
                    if (!isFromSearch) {
                        _showProgress.value = Event(true)
                    }
                }
            }
        }
    }

    fun apiCallOrderDetails(orderId: Int) {
        viewModelScope.launch {

            _showProgress.value = Event(true)
            val resource = posRepository.orderDetailsById(orderId)


            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    resource.data.let { logInResponse ->
                        if (logInResponse?.status == 200) {
                            resource.data?.data?.let { order ->
                                _orderResponse.value = Event(order)
                                //_data.value = Event(createTaxResponse)


//                                order.orderItems.forEach { orderItem ->
//                                    val tbCartItem = TbCartItem()
//                                    tbCartItem.let {
//
//                                        it.itemId = orderItem.itemId
//                                        it.name = orderItem.itemName
//                                        it.id = orderItem.id
//                                        it.price = orderItem.price
//                                        it.quantity = orderItem.quantity
//                                        it.sort = orderItem.sort
//                                        it.categoryId = orderItem.categoryId
//                                        it.note = orderItem.note
//                                        it.itemQuantity = orderItem.quantity
//                                        it.isChecked = orderItem.isChecked
//                                        it.discountPrice = orderItem.discountAmount
//                                        it.singleItemPrice = orderItem.price
//                                        it.discountId = orderItem.discountId
//                                        it.discountType = orderItem.discountType.toString()
//                                        it.isFired = orderItem.isFired
//                                        it.isPaid = orderItem.isPaid
//                                        it.reorder = true
//                                        it.guestIndexForDineIn = orderItem.guestIndexForDineIn
//                                        it.employeeID = orderItem.employeeId
//                                    }
//
//                                    posRepository.addItemToCart(tbCartItem)
//                                }

                            }
                        } else {
                            _snackbarText.value = Event(resource.message)
                        }
                    }
                }

                Status.ERROR -> {
                    _snackbarText.value = Event(resource.message)
                    _showProgress.value = Event(false)
                }

                Status.LOADING -> {
                    _showProgress.value = Event(true)
                }
            }
        }
    }

    fun showError(message: String) {
        _snackbarText.value = Event(message)
    }

    @Inject
    lateinit var paymentGatewayFactory: PaymentGatewayFactory
    lateinit var paymentCoroutineScope: CoroutineScope

    val paymentCoroutineExceptionHandler =
        CoroutineExceptionHandler { coroutineContext, exception ->
            EventBus.getDefault()
                .post(
                    MessageEvent(
                        "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew makeValorPaymentRequest()-> ${
                            Gson().toJson(
                                exception
                            )
                        } "
                    )
                )
        }

    fun parseXml(xmlContent: String): Document {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        return builder.parse(xmlContent.byteInputStream())
    }

    fun makeDejavooPaymentRequest(context: Context, customerModel: TbCustomer) {

        _showProgress.value = Event(true)

        paymentCoroutineScope = CoroutineScope(Dispatchers.IO + paymentCoroutineExceptionHandler)
        paymentCoroutineScope.launch {
            val gatewayType = PaymentGatewayType.DEJAVOO
            val paymentGateway = paymentGatewayFactory.create(gatewayType)


            /*    Test Credentials
                  registerId = "4986101",
                authKey = "kwg2GRbykg",
                tpn = "659324491704"
                authToken= "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0cG4iOiI2NTkzMjQ0OTE3MDQiLCJlbWFpbCI6InN1cHBvcnQrMUBwYXlzcG9zLmNvbSIsImlhdCI6MTczMzc0ODk4M30.spR9JiJS6jt0VMB0MGu9HZQYKUrNaWV-U_pzQ4VCvYw"*/

            /* Process Payment */
            var dejavoo = Dejavoo(
                registerId =  prefProvider.getValue(
                    Constants.DEJAVOO_REGISTER_ID,""
                ),
                authKey = prefProvider.getValue(
                    Constants.DEJAVOO_AUTH_KEY,""
                ),
                tpn = prefProvider.getValue(
                    Constants.DEJAVOO_TPN,""
                ),
                paymentType = "Credit",
                transType = "Sale",
                amount = "0.50",
                tip = "",
                refId = "Ref${System.currentTimeMillis()}",
                printReceipt = false,
                performedBy = prefProvider.employeeName(),
                isProd = Constants.paymentLive,
                txnType = TransactionType.CREDIT_SALE
            )

            context.let {
                paymentGateway.processPayment(
                    it.applicationContext,
                    dejavoo,
                    onSuccess = { tResponse ->
                        var transactionJsonResponse = Gson().fromJson<String>(
                            tResponse,
                            String::class.java
                        )
                        val factory: XmlPullParserFactory = XmlPullParserFactory.newInstance()
                        factory.setNamespaceAware(true)
                        val xpp: XmlPullParser = factory.newPullParser()
                        xpp.setInput(StringReader(transactionJsonResponse))
                        var eventType = xpp.eventType

                        val parsedXml =
                            parseXml(transactionJsonResponse)/*.getElementsByTagName("xmp").item(0)?.textContent.toString()*/
                        var Message = ""
                        var RefId = ""
                        var ExtData = ""
                        var iPOSToken = ""
                        var rrn = ""
                        var RespMSG = ""
                        with(parseXml(transactionJsonResponse).childNodes.item(0).childNodes.item(0).childNodes) {
                            for (i in 0 until this.length) {

                                when ((this.item(i) as Element).tagName.toString()) {
                                    "Message" ->{
                                        try {
                                            Message =
                                                this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                        } catch (e: Exception) {
                                        }
                                    }
                                    "RefId" -> {
                                        try {
                                            RefId =
                                                this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                        } catch (e: Exception) {
                                        }
                                    }
                                    "RespMSG" -> {
                                        try {
                                            RespMSG =
                                                this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                        } catch (e: Exception) {
                                        }
                                    }
                                    "ExtData" -> {
                                        try {
                                            ExtData =
                                                this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                        } catch (e: Exception) {
                                        }
                                    }
                                    "iPOSToken" -> {
                                        try {
                                            iPOSToken =
                                                this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                            Log.e("Dejavoo iPOSToken: ", iPOSToken)
                                        } catch (e: Exception) {
                                        }
                                    }
                                    else -> {

                                    }
                                }
                            }
                        }

                        if (Message.equals("Canceled") || Message.equals("Error")) {
                            _showProgress.value = Event(false)
                            AlertUtils.showCustomAlert(context, RespMSG.replace("%20", " "))

                        } else if (Message.contains("Approved")) {
                            submit(iPOSToken, true, customerModel)
                        }
                    },
                    onFailure = { errorMessage ->
                        Log.e("Dejavoo: ", errorMessage)
                        _token.value = Event(false)
                        AlertUtils.showCustomAlertWithListenerWithOK(
                            context, errorMessage
                        ) { _, _ ->

                        }

                    }
                )
            }
        }
    }

    fun submit(
        iPOSToken: String = "",
        isTokenize:Boolean = false,
        customerModel: TbCustomer
    ) {

        _showProgress.value = Event(true)




        viewModelScope.launch {
            runBlocking {

                addCustomerData = CreateCustomerRequestModel().apply {

                    Log.e("DataJson", "PassData  ${Gson().toJson(customerModel)}")

                    data?.apply {
                        first_name = customerModel.first_name!!.replaceFirstChar { it.uppercase() }
                        last_name = customerModel.last_name!!.replaceFirstChar { it.uppercase() }


//                        val phone = CreateCustomerRequestModel.Customer.Phone(
//                            id = customerModel.phones[0].id,
//                            phone_number = customerModel.phones[0].phone_number.replace(
//                                ("[\\D]").toRegex(),
//                                ""
//                            )
//                        )
//                        phones_attributes?.add(
//                            0, phone
//                        )

                        email = customerModel.email
                        birth_day = customerModel.birth_date
                        birth_month = customerModel.birth_date.toString()
                        birthday_year = customerModel.birth_date.toString()
                        company = customerModel.company.toString()
                        enroll_to_loyalty = customerModel.enroll_to_loyalty
                        same_as_billing_address = customerModel.same_as_billing_address
                        isTokenized = isTokenize
                        cardToken = iPOSToken

                        addresses_attributes = (customerModel.addresses as ArrayList<CreateCustomerRequestModel.Customer.Addresses>)

                    }
                }

                Log.e("TAG", "addCustomerDataJson:  ${Gson().toJson(addCustomerData)}")
                Log.e("TAG", "customerID:  ${customerId}")
                viewModelScope.launch {

                    val resource = posRepository.updateCustomer(customerId!!.toInt(), addCustomerData)


                    when (resource.status) {
                        Status.SUCCESS -> {
                            _showProgress.value = Event(false)
                            Log.d("C_Loyalty: ", "addCustomerViewModel: Status.SUCCESS...")

                            resource.data.let {
                                if (it?.status == 200) {

                                    resource.data?.let { customerListReposne ->

                                        val model = TbCustomer(
                                            id = customerListReposne.data.id,
                                            first_name = customerListReposne.data.first_name,
                                            last_name = customerListReposne.data.last_name,
                                            birth_date = customerListReposne.data.birth_date,
                                            email = customerListReposne.data.email,
                                            phones = customerListReposne.data.phones,
                                            addresses = customerListReposne.data.addresses,
                                            enroll_to_loyalty = customerListReposne.data.enroll_to_loyalty,
                                            same_as_billing_address = customerListReposne.data.same_as_billing_address,
                                            final_reward = customerListReposne.data.final_reward,
                                            company = customerListReposne.data.company,
                                            cardToken = customerListReposne.data.cardToken,
                                            isTokenized = customerListReposne.data.isTokenized,
                                            isSelcted = true,
                                        )

                                        if (customerListReposne.data.isTokenized) {
                                            _token.value = Event(true)
                                        } else {
                                            _token.value = Event(false)
                                        }



                                        Log.d(
                                            "C_Loyalty: ",
                                            "addCustomerViewModel: posRepository.addCustomer() called..."
                                        )

                                    }

                                } else {
                                    _snackbarText.value = Event(resource.message)
                                    _token.value = Event(false)
                                }
                            }
                        }
                        Status.ERROR -> {
                            /*TODO: Handle Error scenario here*/
                            Log.d("TAG", "addCustomerViewModel: Status.ERROR...")
                            _showProgress.value = Event(false)
                            _token.value = Event(false)
                            _snackbarText.value = Event(resource.message)
                        }
                        Status.LOADING -> {
                            _showProgress.value = Event(true)
                            _token.value = Event(false)
                        }

                    }

                }


            }
        }

//        }


    }
}