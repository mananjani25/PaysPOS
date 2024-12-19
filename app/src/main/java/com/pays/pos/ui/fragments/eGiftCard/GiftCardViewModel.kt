package com.pays.pos.ui.fragments.eGiftCard

import android.view.inputmethod.CorrectionInfo
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pays.pos.data.db.AppDatabase
import com.pays.pos.data.model.requestModel.giftCard.request.GiftCard
import com.pays.pos.data.model.requestModel.giftCard.request.GiftCardAddValueRequest
import com.pays.pos.data.model.requestModel.giftCard.request.GiftCardCheckBalanceRequest
import com.pays.pos.data.model.requestModel.giftCard.request.SellGiftCardRequestModel
import com.pays.pos.data.model.requestModel.giftCard.response.GiftCardAddValueResponse
import com.pays.pos.data.model.requestModel.giftCard.response.GiftCardCheckBalanceResponse
import com.pays.pos.data.model.requestModel.giftCard.response.SellGiftCardResponseModel
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.repositories.PosRepository
import com.pays.pos.di.PrefProvider
import com.pays.pos.ui.fragments.magtek.PaymentResponse
import com.pays.pos.utils.Event
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.MethodUtils
import com.pays.pos.utils.MethodUtils.Companion.generateRandomNumbers
import com.pays.pos.utils.statusUtils.Resource
import com.pays.pos.utils.statusUtils.Status
import com.google.gson.Gson
import com.pays.pos.data.model.requestModel.CashLogRequest
import com.pays.pos.data.remote.Constants.ENDPOINT_URL
import com.pays.pos.data.remote.Constants.GIFT_CARD_NUMBER
import com.pays.pos.data.remote.Constants.PHYSICAL_GIFT_CARD_NUMBER
import com.pays.pos.data.remote.Constants.SOAP_ACTION
import com.pays.pos.logger.MessageEvent
import com.squareup.okhttp.Callback
import com.squareup.okhttp.MediaType
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Protocol
import com.squareup.okhttp.Request
import com.squareup.okhttp.RequestBody
import com.squareup.okhttp.Response
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.greenrobot.eventbus.EventBus
import org.json.JSONObject
import org.json.XML
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

import java.io.StringReader

import java.util.TimeZone
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class GiftCardViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val appDatabase: AppDatabase,
    private val prefProvider: PrefProvider
) : ViewModel() {

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _giftCardData = MutableLiveData<Event<SellGiftCardResponseModel?>>()
    val giftCardData: LiveData<Event<SellGiftCardResponseModel?>> = _giftCardData

    private val _addValueInGiftCardData = MutableLiveData<Event<SellGiftCardResponseModel?>>()
    val addValueInGiftCardData: LiveData<Event<SellGiftCardResponseModel?>> =
        _addValueInGiftCardData

    private val _giftCardCheckBalanceData = MutableLiveData<Event<GiftCardCheckBalanceResponse?>>()
    val giftCardCheckBalanceData: LiveData<Event<GiftCardCheckBalanceResponse?>> =
        _giftCardCheckBalanceData

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    private val _showGiftCardProgress = MutableLiveData<Event<Boolean>>()
    val showGiftCardProgress : LiveData<Event<Boolean>> = _showGiftCardProgress

    private val _giftCardError = MutableLiveData<Event<String>>()
    val giftCardError: LiveData<Event<String>> = _giftCardError

    private val _showProgressCash = MutableLiveData<Event<Boolean>>()
    val showProgressCash: LiveData<Event<Boolean>> = _showProgressCash

    var magensaResponse: String? = null

    var paxResponse: String = ""
    var cardNumberLast4: String = ""
    var cardNamePax: String = ""
    var transactionID: String = ""


    fun clearGiftCardObserver(){
        _giftCardError.value= Event("")
        _giftCardCheckBalanceData.value= Event(null)
    }
    fun setMagensaResponse(response: String?, cardNumber1: String) {
        magensaResponse = response
        cardNumberLast4 = cardNumber1
    }

    fun createSellGiftCardRequestUsingCash(paymentType:String?=""): SellGiftCardRequestModel {

        val giftCardPurchaseAmount =
            prefProvider.getValue(Constants.GIFT_CARD_PURCHASE_AMOUNT, "0.0")

        val paymentAttributes =
            com.pays.pos.data.model.requestModel.giftCard.request.PaymentAttributes(
                amount = giftCardPurchaseAmount.toDouble(),
                card_name = "",
                card_number = "",
                employee_id = prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0),
                magensa_response = "",
                offline_id = MethodUtils.randomOfflineId(
                    prefProvider.getValueInt(Constants.LOCATION_ID, -1).toString()
                ),
                payable_type = "GiftCard",
                payment_type = paymentType?:"Cash",
                sub_total = giftCardPurchaseAmount.toDouble(),
                terminal_id = prefProvider.getValueInt(Constants.TERMINAL_ID, 0),
                transaction_id = ""
            )

        val giftCard = GiftCard(
            gift_card_type = prefProvider.getValue(Constants.GIFT_CARD_TYPE,""),//"Digital",//Physical
            name = if (prefProvider.getValue(Constants.GIFT_CARD_TYPE,"").equals("Physical",true)){prefProvider.getValue(Constants.PHYSICAL_GIFT_CARD_NUMBER,"")} else{""},
            amount = giftCardPurchaseAmount,
            customer_id = prefProvider.getValueInt(Constants.CUSTOMER_ID, 0),
            location_id = prefProvider.getValueInt(Constants.LOCATION_ID, 1),
            password = "",
            payment_attributes = paymentAttributes
        )

        return SellGiftCardRequestModel(gift_card = giftCard)
    }

    fun createSellGiftCardRequestUsingCard(): SellGiftCardRequestModel {

        var cardNumber = ""
        var cardName = ""
        var transactionId = ""

        val giftCardPurchaseAmount =
            prefProvider.getValue(Constants.GIFT_CARD_PURCHASE_AMOUNT, "0.0")
        var paymentAttributes: com.pays.pos.data.model.requestModel.giftCard.request.PaymentAttributes? =
            null




        if (magensaResponse != null) {
            try {
                if (!magensaResponse!!.contains('<') && !magensaResponse!!.contains('>')) {
                    val model = Gson().fromJson(
                        magensaResponse,
                        PaymentResponse.PaymentResponseItem::class.java
                    )



                    if (model.dataOutput != null) {
                        LogUtil.logE("dataOutput", Gson().toJson(model))
                        cardNumber = model.dataOutput.PANLast4
                        var cardN = ""
                        model.dataOutput.additionalOutputData?.forEach {
                            LogUtil.logE("additionalOutputData", it.key)
                            if (it.key == "CardType") {
                                cardN = it.value
                            }
                        }
                        cardName = cardN
                    }

                    if (model.cardSwipeOutput != null) {
                        LogUtil.logE("cardSwipeOutput", Gson().toJson(model))
                        cardNumber = model.cardSwipeOutput.pANLast4
                        var cardN = ""
                        model.cardSwipeOutput.additionalOutputData?.forEach {
                            if (it.key == "CardType") {
                                cardN = it.value
                            }
                        }

                        cardName = cardN
                    }


                    if (model.transactionOutput?.transactionOutputDetails?.isNotEmpty() == true) {
                        var CardType = ""
                        model.transactionOutput.transactionOutputDetails.forEach {
                            if (it.key == "CardType") {
                                CardType = it.value
                            }
                        }

                        cardName = CardType
                        cardNumber =
                            if (cardNumberLast4.isNotEmpty()) cardNumberLast4.takeLast(4) else ""
                    }
                    transactionId = model.transactionOutput?.transactionID.toString()
                }
            } catch (e: Exception) {
            }

        } else {
            cardName = cardNamePax
            cardNumber = cardNumberLast4
            transactionId = transactionID
            magensaResponse = paxResponse
        }

        paymentAttributes =
            com.pays.pos.data.model.requestModel.giftCard.request.PaymentAttributes(
                amount = giftCardPurchaseAmount.toDouble(),
                card_name = cardName,
                card_number = cardNumber,
                card_type = 0,
                employee_id = prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0),
                magensa_response = magensaResponse,
                offline_id = MethodUtils.randomOfflineId(
                    prefProvider.getValueInt(Constants.LOCATION_ID, -1).toString()
                ),
                payable_type = "GiftCard",
                payment_type = "Card",
                sub_total = giftCardPurchaseAmount.toDouble(),
                terminal_id = prefProvider.getValueInt(Constants.TERMINAL_ID, 0),
                transaction_id = transactionId
            )

        val giftCard = GiftCard(
            gift_card_type = /*"Digital"*/ prefProvider.getValue(Constants.GIFT_CARD_TYPE,""),//Physical
            name = if (prefProvider.getValue(Constants.GIFT_CARD_TYPE,"").equals("Physical",true)){prefProvider.getValue(Constants.PHYSICAL_GIFT_CARD_NUMBER,"")} else{""},
            amount = giftCardPurchaseAmount,
            customer_id = prefProvider.getValueInt(Constants.CUSTOMER_ID, 0),
            location_id = prefProvider.getValueInt(Constants.LOCATION_ID, 1),
            password = "",
            payment_attributes = paymentAttributes
        )

        return SellGiftCardRequestModel(gift_card = giftCard)
    }

    //add balance to physical gift card
    fun addBalanceToPhysicalGiftCard(myRequest: SellGiftCardRequestModel?) {
        CoroutineScope(Dispatchers.Main).launch {
            _showGiftCardProgress.value = Event(true)
        }
        val soapRequest = createSoapRequest("m117115rgw","T98PZAGEHT",prefProvider.getValue(PHYSICAL_GIFT_CARD_NUMBER,""),myRequest?.gift_card?.amount ?: "")
        sendSoapRequest(soapRequest, onSuccess = {response->
            val endingBalance = parseSoapResponse(response)
            CoroutineScope(Dispatchers.Main).launch {
                _showGiftCardProgress.value = Event(false)
                myRequest?.let { sellGiftCard(it) }
            }
            Log.e("PhysicalGiftCard","endingBalance:  ${endingBalance}")
        }, onError = {error->
            CoroutineScope(Dispatchers.Main).launch {
                _showGiftCardProgress.value = Event(false)
            }
            Log.e("PhysicalGiftCard","error:  ${error.message}")

        })


    }

    fun parseSoapResponse(response: String): String {
        // Extract specific elements from the XML response using an XML parser
        // Placeholder implementation
        val regex = "<PurseBalances>(.*?)</PurseBalances>".toRegex()
        return regex.find(response)?.groups?.get(1)?.value ?: "No balance found"
    }

    fun convertXmlToJson(xmlString: String): String {
        return try {
            // Convert XML string to JSON object
            val jsonObject = XML.toJSONObject(xmlString)

            // Pretty print the JSON object
            jsonObject.toString(4) // Indent with 4 spaces
        } catch (e: Exception) {
            e.printStackTrace()
            "Error converting XML to JSON: ${e.message}"
        }
    }

    fun checkPhysicalCardBalanceBeforePayment(soapRequest: String, onSuccess: (String) -> Unit, onError: (Throwable) -> Unit){
        var client   =  okhttp3.OkHttpClient.Builder().protocols(listOf(okhttp3.Protocol.HTTP_1_1)).build()

        val body = okhttp3.RequestBody.create(
            "text/xml; charset=utf-8".toMediaTypeOrNull(),
            soapRequest
        )
        val request = okhttp3.Request.Builder()
            .url(ENDPOINT_URL)
            .post(body)
            .addHeader("Content-Type", "text/xml; charset=utf-8")
            .addHeader("SOAPAction", SOAP_ACTION)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback{
            override fun onFailure(call: Call, e: IOException) {
                Log.e("PhysicalGiftCard","onFailure")
                e?.let { onError(it) }
            }

            override fun onResponse(call: Call, response: okhttp3.Response) {
                Log.e("PhysicalGiftCardBalance","onResponse: ")
                if (response?.isSuccessful == true) {
                    Log.e("PhysicalGiftCard","onResponseBalance:  ")
                        onSuccess(response.body?.string()?:"")
                  /*  response.body?.toString()?.let {
                        var getGiftCardBalance =  parseXMLData(response.body?.string()?:"")
                        Log.e("PhysicalGiftCard","checkResponse:  ${getGiftCardBalance}")
                    }*/ ?: onError(IOException("Empty response"))
                } else {
                    onError(IOException("HTTP ${response.code} ${Gson().toJson(response?.body?.toString())}"))
                }
            }

        })



    }

    fun sendSoapCheckBalanceRequest(soapRequest: String, onSuccess: (String) -> Unit, onError: (Throwable) -> Unit){

        var client   =  okhttp3.OkHttpClient.Builder().protocols(listOf(okhttp3.Protocol.HTTP_1_1)).build()

        val body = okhttp3.RequestBody.create(
            "text/xml; charset=utf-8".toMediaTypeOrNull(),
            soapRequest
        )
        val request = okhttp3.Request.Builder()
            .url(ENDPOINT_URL)
            .post(body)
            .addHeader("Content-Type", "text/xml; charset=utf-8")
            .addHeader("SOAPAction", SOAP_ACTION)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback{
            override fun onFailure(call: Call, e: IOException) {
                Log.e("PhysicalGiftCard","onFailure")
                e?.let { onError(it) }
            }

            override fun onResponse(call: Call, response: okhttp3.Response) {
                Log.e("PhysicalGiftCardBalance","onResponse: ")
                if (response?.isSuccessful == true) {
                    Log.e("PhysicalGiftCard","onResponseBalance:  ")
                    response.body?.toString()?.let {
                        onSuccess(it)
                       var getGiftCardBalance =  parseXMLData(response.body?.string()?:"")

                        var model =GiftCardCheckBalanceResponse(message =response.message , status = 0, type = "", data = getGiftCardBalance?.toDouble()
                            ?.let { it1 -> GiftCardCheckBalanceResponse.Data(it1) })
                        CoroutineScope(Dispatchers.Main).launch {
                            _giftCardCheckBalanceData.value = Event(model)
                        }





                        Log.e("PhysicalGiftCard","checkResponse:  ${getGiftCardBalance}")
                    } ?: onError(IOException("Empty response"))
                } else {
                    onError(IOException("HTTP ${response.code} ${Gson().toJson(response?.body?.toString())}"))
                }
            }

        })



    }

    private fun parseXMLData(data: String) : String{
        var key = ""
        var value = ""
        var balances =""
      var newData = data.replace("\\u003c", "<")
            .replace("\\u003e", ">")
            .replace("\\u003d", "=")
            .replace("\\\"", "\"")

        Log.e("PhysicalGiftCard","newDAta: ${newData}")
        val purchaseBalances = parseXmlResponse(newData).get("PurseBalances") as List<String>
        Log.e("PhysicalGiftCard","purchaseBalances:  ${purchaseBalances}")
        if (purchaseBalances.isNotEmpty()){
            return purchaseBalances[0].toString()
          /*  if (response?.status == 200) {
                _giftCardCheckBalanceData.value = Event(purchaseBalances[0])
            } else {
                _snackbarText.value = Event(resource.message)
            }*/
        }
        else{
            return "0.00"
        }






    }

    fun parseXmlResponse(xml: String): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        val purseBalances = mutableListOf<String>() // To hold multiple decimal values
        try {
            // Create a new XmlPullParser
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(xml.reader()) // Set the XML string as input

            var eventType = parser.eventType
            var currentTag: String? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name // Start reading a tag
                    }
                    XmlPullParser.TEXT -> {
                        if (currentTag == "decimal") {
                            purseBalances.add(parser.text.trim()) // Add decimals to list
                        } else if (currentTag != null && parser.text.isNotBlank()) {
                            result[currentTag] = parser.text.trim()
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        currentTag = null // Reset the tag
                    }
                }
                eventType = parser.next() // Move to the next XML element
            }

            // Add the list of PurseBalances to the result map
            result["PurseBalances"] = purseBalances
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return result
    }

    fun sendSoapRequest(soapRequest: String, onSuccess: (String) -> Unit, onError: (Throwable) -> Unit) {
        var client   =  okhttp3.OkHttpClient.Builder().protocols(listOf(okhttp3.Protocol.HTTP_1_1)).build()

        val body = okhttp3.RequestBody.create(
            "text/xml; charset=utf-8".toMediaTypeOrNull(),
            soapRequest
        )
        val request = okhttp3.Request.Builder()
            .url(ENDPOINT_URL)
            .post(body)
            .addHeader("Content-Type", "text/xml; charset=utf-8")
            .addHeader("SOAPAction", SOAP_ACTION)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback{
            override fun onFailure(call: Call, e: IOException) {
                Log.e("PhysicalGiftCard","onFailure")
                e?.let { onError(it) }
            }

            override fun onResponse(call: Call, response: okhttp3.Response) {
                Log.e("PhysicalGiftCard","onResponsecheckBf: ${response.isSuccessful}")
                if (response?.isSuccessful == true) {
                    val response = response.body?.toString()
                    Log.e("PhysicalGiftCard","onResponse:  ${Gson().toJson(response)}")
                    response?.let {
                        onSuccess(it)
                    } ?: onError(IOException("Empty response"))
                } else {
                    onError(IOException("HTTP ${response.code} ${Gson().toJson(response?.body?.toString())}"))
                }
            }

        })

    }


    fun createSoapRequest(username: String, password: String, giftCardNumber: String, transactionAmount: String): String {
        val formattedDateTime = getFormattedDateTime()
        return """
        <?xml version="1.0" encoding="utf-8"?>
        <soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
                       xmlns:xsd="http://www.w3.org/2001/XMLSchema" 
                       xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
            <soap:Body>
                <AuthenticateAndAuthorizeTransaction xmlns="${Constants.NAMESPACE}">
                <credential>
                        <Username>${username}</Username>
                        <Password>${password}</Password>
                    </credential>
                    <authRequest>
                        <Account>$giftCardNumber</Account>
                        <TransactionAmount>$transactionAmount</TransactionAmount>
                        <TerminalDateTime>${formattedDateTime}</TerminalDateTime>
                        <TransactionType>Load</TransactionType>
                        <PurseNumber>0</PurseNumber>
                        <SchemeNumber>1</SchemeNumber>
                     </authRequest>
                </AuthenticateAndAuthorizeTransaction>
            </soap:Body>
        </soap:Envelope>
    """.trimIndent()
    }
    // purchase new gift card
    fun sellGiftCard(sellGiftCardRequestModel: SellGiftCardRequestModel) {

        if (checkIsCashPaymentTypeForGiftCard(sellGiftCardRequestModel)) {
            _showProgressCash.value = Event(true)
        } else
            _showProgress.value = Event(true)

        viewModelScope.launch {

            val resource: Resource<SellGiftCardResponseModel> =
                posRepository.sellGiftCard(sellGiftCardRequestModel)

            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    resource.data.let { response ->
                        if (response?.status == 200) {
                            resource.data?.data?.gift_card?.let {
                                if (!it.payments[it.payments.size - 1].payment_type.equals("External")) {
//                                This cashlog call is independent, thats the reason it is not chained with any flow or call
                                    val cashLogRequest = CashLogRequest(
                                        sellGiftCardRequestModel.gift_card.amount.toDouble(),
                                        prefProvider.getValueInt(
                                            Constants.EMPLOYEE_ID, 0
                                        ),
                                        "in",
                                        it.id,//This may be wrong
                                        it.payments[it.payments.size - 1].id,
                                        "Gift card purchase",
                                        prefProvider.getValueInt(Constants.TERMINAL_ID, -1),
                                        null,
                                        null
                                    )
                                    cashLogApiOnGiftCardSellOrAdd(cashLogRequest)
                                }
                            }
                            resource.data?.let { sellGiftCardResponse ->

                                sellGiftCardResponse.data?.let {

                                    prefProvider.setValueInt(
                                        Constants.PAYMENT_ID,
                                        sellGiftCardResponse.data.gift_card.payments[0].id
                                    )

                                    prefProvider.setValueInt(
                                        Constants.PAYMENT_ID_FOR_CUSTOMER_DISPLAY,
                                        sellGiftCardResponse.data.gift_card.payments[0].id
                                    )
                                }
                                EventBus.getDefault().post(
                                    MessageEvent(
                                        "${Constants.LINE_BREAK_TAB} CART_MODEL_CLEAR Thread.dumpStack(): it1 -> ${
                                            Gson().toJson(Thread.currentThread().stackTrace)
                                        }"
                                    )
                                )
                                posRepository.deleteCart(
                                    prefProvider.getValueInt(
                                        Constants.EMPLOYEE_ID,
                                        0
                                    )
                                )

                                _giftCardData.value = Event(sellGiftCardResponse)

                            }

                        } else {
                            _snackbarText.value = Event(resource.message)
                        }
                    }

                }

                Status.ERROR -> {
                    _snackbarText.value = Event(resource.message)

                    if (checkIsCashPaymentTypeForGiftCard(sellGiftCardRequestModel)) {
                        _showProgressCash.value = Event(false)
                    } else
                        _showProgress.value = Event(false)

                }

                Status.LOADING -> {
                    if (checkIsCashPaymentTypeForGiftCard(sellGiftCardRequestModel)) {
                        _showProgressCash.value = Event(true)
                    } else
                        _showProgress.value = Event(true)

                }
            }
        }
    }

    private suspend fun cashLogApiOnGiftCardSellOrAdd(cashLogRequest: CashLogRequest) {


        val resource = posRepository.cashInOut(cashLogRequest)

        when (resource.status) {
            Status.SUCCESS -> {
                _showProgress.value = Event(false)
                resource.data.let { response ->
                    if (response?.status == 200) {

                        resource.data?.let {

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

    fun getFormattedDateTime(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        return dateFormat.format(Date())
    }

    fun createAddValueInGiftCardRequestUsingCash(paymentType: String = ""): GiftCardAddValueRequest {

        val giftCardPurchaseAmount =
            prefProvider.getValue(Constants.GIFT_CARD_PURCHASE_AMOUNT, "0.0")
        val giftCardNumber = prefProvider.getValue(Constants.GIFT_CARD_NUMBER, "")


        val paymentAttributes =
            GiftCardAddValueRequest.GiftCardAmountTab.PaymentAttributes(
                amount = giftCardPurchaseAmount.toDouble(),
                card_name = "",
                card_number = "",
                employee_id = prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0),
                magensa_response = "",
                offline_id = MethodUtils.randomOfflineId(
                    prefProvider.getValueInt(Constants.LOCATION_ID, -1).toString()
                ),
                payable_type = "GiftCardAmountTab",
                payment_type = if (paymentType.isNotEmpty()){
                    Constants.EXTERNAL_PAYMENT
                }else{
                    "Cash"
                },
                sub_total = giftCardPurchaseAmount.toDouble(),
                terminal_id = prefProvider.getValueInt(Constants.TERMINAL_ID, 0),
                transaction_id = ""
            )

        val giftCard = GiftCardAddValueRequest.GiftCard(
            gift_card_type = "Digital",//Physical
            name = giftCardNumber,
            added_amount = giftCardPurchaseAmount.toDouble(),
        )

        val giftCardAmountTab = GiftCardAddValueRequest.GiftCardAmountTab(
            payment_attributes = paymentAttributes
        )

        return GiftCardAddValueRequest(
            gift_card = giftCard,
            gift_card_amount_tab = giftCardAmountTab
        )
    }

    fun createAddValueInGiftCardRequestUsingCard(): GiftCardAddValueRequest {

        val giftCardPurchaseAmount =
            prefProvider.getValue(Constants.GIFT_CARD_PURCHASE_AMOUNT, "0.0")
        val giftCardNumber = prefProvider.getValue(Constants.GIFT_CARD_NUMBER, "")

        var paymentAttributes: GiftCardAddValueRequest.GiftCardAmountTab.PaymentAttributes? = null

        var cardNumber = ""
        var cardName = ""
        var transactionId = ""

        if (magensaResponse != null) {
            try {
                if (!magensaResponse!!.contains('<') && !magensaResponse!!.contains('>')) {

                    val model = Gson().fromJson(
                        magensaResponse,
                        PaymentResponse.PaymentResponseItem::class.java
                    )

                    if (model.dataOutput != null) {
                        LogUtil.logE("dataOutput", Gson().toJson(model))
                        cardNumber = model.dataOutput.PANLast4
                        var cardN = ""
                        model.dataOutput.additionalOutputData?.forEach {
                            LogUtil.logE("additionalOutputData", it.key)
                            if (it.key == "CardType") {
                                cardN = it.value
                            }
                        }
                        cardName = cardN
                    }

                    if (model.cardSwipeOutput != null) {
                        LogUtil.logE("cardSwipeOutput", Gson().toJson(model))
                        cardNumber = model.cardSwipeOutput.pANLast4
                        var cardN = ""
                        model.cardSwipeOutput.additionalOutputData?.forEach {
                            if (it.key == "CardType") {
                                cardN = it.value
                            }
                        }

                        cardName = cardN
                    }


                    if (model.transactionOutput?.transactionOutputDetails?.isNotEmpty() == true) {
                        var CardType = ""
                        model.transactionOutput.transactionOutputDetails.forEach {
                            if (it.key == "CardType") {
                                CardType = it.value
                            }
                        }

                        cardName = CardType
                        cardNumber =
                            if (cardNumberLast4.isNotEmpty()) cardNumberLast4.takeLast(4) else ""
                    }

                    transactionId = model.transactionOutput?.transactionID.toString()
                }
            } catch (e: IllegalStateException) {

            }
        } else {
            cardName = cardNamePax
            cardNumber = cardNumberLast4
            transactionId = transactionID
            magensaResponse = paxResponse
        }

        paymentAttributes =
            GiftCardAddValueRequest.GiftCardAmountTab.PaymentAttributes(
                amount = giftCardPurchaseAmount.toDouble(),
                card_name = cardName,
                card_number = cardNumber,
                card_type = 0,
                employee_id = prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0),
                magensa_response = magensaResponse,
                offline_id = MethodUtils.randomOfflineId(
                    prefProvider.getValueInt(Constants.LOCATION_ID, -1).toString()
                ),
                payable_type = "GiftCardAmountTab",
                payment_type = "Card",
                sub_total = giftCardPurchaseAmount.toDouble(),
                terminal_id = prefProvider.getValueInt(Constants.TERMINAL_ID, 0),
                transaction_id = transactionId
            )


        val giftCard = GiftCardAddValueRequest.GiftCard(
            gift_card_type = "Digital",//Physical
            name = giftCardNumber,
            added_amount = giftCardPurchaseAmount.toDouble(),
        )

        val giftCardAmountTab = GiftCardAddValueRequest.GiftCardAmountTab(
            payment_attributes = paymentAttributes
        )

        return GiftCardAddValueRequest(
            gift_card = giftCard,
            gift_card_amount_tab = giftCardAmountTab
        )
    }
    //Add money in existing Physical Gift Card
    fun addValueInPhysicalGiftCard(
        isCashPaymentType: Boolean,
        giftCardAddValueRequest: GiftCardAddValueRequest){
        CoroutineScope(Dispatchers.Main).launch {
            _showGiftCardProgress.value = Event(true)
        }

        val soapRequest = createSoapRequest("m117115rgw","T98PZAGEHT",prefProvider.getValue(PHYSICAL_GIFT_CARD_NUMBER,""),giftCardAddValueRequest.gift_card.added_amount.toString() ?: "")
        sendSoapRequest(soapRequest, onSuccess = {response->
            val endingBalance = parseSoapResponse(response)
            CoroutineScope(Dispatchers.Main).launch {
                _showGiftCardProgress.value = Event(false)
                addValueInGiftCard(isCashPaymentType,giftCardAddValueRequest)
            }
            Log.e("PhysicalGiftCard","endingBalance:  ${endingBalance}")
        }, onError = {error->
            Log.e("PhysicalGiftCard","error:  ${error.message}")
            CoroutineScope(Dispatchers.Main).launch {
                _showGiftCardProgress.value = Event(false)
            }

        })

    }

    // Add money in existing gift card
    fun addValueInGiftCard(
        isCashPaymentType: Boolean,
        giftCardAddValueRequest: GiftCardAddValueRequest
    ) {

        if (isCashPaymentType) {
            _showProgressCash.value = Event(true)
        } else
            _showProgress.value = Event(true)

        viewModelScope.launch {

            val resource: Resource<SellGiftCardResponseModel> =
                posRepository.addValueInGiftCard(giftCardAddValueRequest)

            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    resource.data.let { response ->
                        if (response?.status == 200) {

                            resource.data?.data?.gift_card?.let {
                                if (it.payments[it.payments.size - 1].payment_type.equals("Cash",ignoreCase = true)) {
//                                This cashlog call is independent, thats the reason it is not chained with any flow or call
                                    val cashLogRequest = CashLogRequest(
                                        giftCardAddValueRequest.gift_card.added_amount,
                                        prefProvider.getValueInt(
                                            Constants.EMPLOYEE_ID, 0
                                        ),
                                        "in",
                                        it.id,//This may be wrong
                                        it.payments[it.payments.size - 1].id,
                                        "Gift card recharge",
                                        prefProvider.getValueInt(Constants.TERMINAL_ID, -1),
                                        null,
                                        null
                                    )
                                    cashLogApiOnGiftCardSellOrAdd(cashLogRequest)
                                }
                            }

                            resource.data?.let { addValueInGiftCardResponse ->

                                addValueInGiftCardResponse.data?.let {

                                    prefProvider.setValueInt(
                                        Constants.PAYMENT_ID,
                                        addValueInGiftCardResponse.data.gift_card.payments[0].id
                                    )

                                    prefProvider.setValueInt(
                                        Constants.PAYMENT_ID_FOR_CUSTOMER_DISPLAY,
                                        addValueInGiftCardResponse.data.gift_card.payments[0].id
                                    )
                                }
                                EventBus.getDefault().post(
                                    MessageEvent(
                                        "${Constants.LINE_BREAK_TAB} CART_MODEL_CLEAR Thread.dumpStack(): it1 -> ${
                                            Gson().toJson(Thread.currentThread().stackTrace)
                                        }"
                                    )
                                )
                                posRepository.deleteCart(
                                    prefProvider.getValueInt(
                                        Constants.EMPLOYEE_ID,
                                        0
                                    )
                                )

                                _addValueInGiftCardData.value = Event(addValueInGiftCardResponse)

                            }

                        } else {
                            _snackbarText.value = Event(resource.message)
                        }
                    }

                }

                Status.ERROR -> {
                    _snackbarText.value = Event(resource.message)

                    if (isCashPaymentType) {
                        _showProgressCash.value = Event(false)
                    } else
                        _showProgress.value = Event(false)

                }

                Status.LOADING -> {
                    if (isCashPaymentType) {
                        _showProgressCash.value = Event(true)
                    } else
                        _showProgress.value = Event(true)

                }
            }
        }
    }
    fun physicalGiftCardCheckBalanceBeforePay(giftCardCheckBalanceRequest: GiftCardCheckBalanceRequest) {
        try {
            val soapRequest = checkBalanceRequest("m117115rgw", "T98PZAGEHT", giftCardCheckBalanceRequest.name)
            CoroutineScope(Dispatchers.Main).launch {
                _showGiftCardProgress.value = Event(true)
            }
            checkPhysicalCardBalanceBeforePayment(soapRequest, onSuccess = { response ->
                val endingBalance = parseXMLData(response)

                Log.e("PhysicalGiftCardBalance", "endingBalance:  ${endingBalance}")
                CoroutineScope(Dispatchers.Main).launch {
                    val res = GiftCardCheckBalanceResponse(
                        type = "Physical",
                        status = 200,
                        message = "",
                        data = GiftCardCheckBalanceResponse.Data(endingBalance.toDouble())
                    )

                    _showGiftCardProgress.value = Event(false)
                    _giftCardCheckBalanceData.value = Event(res)

                }

            }, onError = { error ->

                CoroutineScope(Dispatchers.Main).launch {
                    _showGiftCardProgress.value = Event(false)
                    _snackbarText.value = Event(error.message)
                }
                Log.e("PhysicalGiftCardBalance", "error:  ${error.message}")

            })

        }catch (e:Exception){
            e.printStackTrace()
        }



    }

    //check physical gift card balance

    fun physcialGiftCardCheckBalance(giftCardCheckBalanceRequest: GiftCardCheckBalanceRequest){

        val soapRequest = checkBalanceRequest("m117115rgw","T98PZAGEHT",giftCardCheckBalanceRequest.name)
        CoroutineScope(Dispatchers.Main).launch {
            _showGiftCardProgress.value = Event(true)
        }
        sendSoapCheckBalanceRequest(soapRequest, onSuccess = {response->
            val endingBalance = parseSoapResponse(response)

            CoroutineScope(Dispatchers.Main).launch {
                _showGiftCardProgress.value = Event(false)
            }
            Log.e("PhysicalGiftCardBalance","endingBalance:  ${endingBalance}")
        }, onError = {error->
            CoroutineScope(Dispatchers.Main).launch {
                _showGiftCardProgress.value = Event(false)
                _snackbarText.value = Event(error.message)
            }
            Log.e("PhysicalGiftCardBalance","error:  ${error.message}")

        })


    }


    fun checkBalanceRequest(username: String, password: String, giftCardNumber: String):String{

            val formattedDateTime = getFormattedDateTime()
        Log.e("PhysicalGiftCard","formattedDateTime:  ${formattedDateTime}")
            return """
        <?xml version="1.0" encoding="utf-8"?>
        <soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
                       xmlns:xsd="http://www.w3.org/2001/XMLSchema" 
                       xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
            <soap:Body>
                <AuthenticateAndAuthorizeTransaction xmlns="${Constants.NAMESPACE}">
                <credential>
                        <Username>${username}</Username>
                        <Password>${password}</Password>
                    </credential>
                    <authRequest>
                        <Account>$giftCardNumber</Account>
                        <TerminalDateTime>${formattedDateTime}</TerminalDateTime>
                        <TransactionType>BalanceInquiry</TransactionType>
                        <PurseNumber>0</PurseNumber>
                        <SchemeNumber>1</SchemeNumber>
                     </authRequest>
                </AuthenticateAndAuthorizeTransaction>
            </soap:Body>
        </soap:Envelope>
    """.trimIndent()


    }
    // check gift card balance of existing gift card
    fun giftCardCheckBalance(giftCardCheckBalanceRequest: GiftCardCheckBalanceRequest) {

        _showProgress.value = Event(true)

        viewModelScope.launch {

            val resource: Resource<GiftCardCheckBalanceResponse> =
                posRepository.giftCardCheckBalance(giftCardCheckBalanceRequest)

            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    resource.data.let { response ->
                        if (response?.status == 200) {
                            _giftCardCheckBalanceData.value = Event(response)
                        } else {
                            _snackbarText.value = Event(resource.message)
                        }
                    }
                    resource.data?.let {
                        if (it.data == null && it.message.isNotEmpty()) {
                            _giftCardError.postValue(Event(it.message))
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

    private fun checkIsCashPaymentTypeForGiftCard(sellGiftCardRequestModel: SellGiftCardRequestModel): Boolean {

        return sellGiftCardRequestModel.gift_card.payment_attributes?.payment_type.equals(
            "Cash",
            ignoreCase = true
        )
    }


}