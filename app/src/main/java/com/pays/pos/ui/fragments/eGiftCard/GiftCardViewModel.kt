package com.pays.pos.ui.fragments.eGiftCard

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
import com.pays.pos.logger.MessageEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
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

    private val _giftCardError = MutableLiveData<Event<String>>()
    val giftCardError: LiveData<Event<String>> = _giftCardError

    private val _showProgressCash = MutableLiveData<Event<Boolean>>()
    val showProgressCash: LiveData<Event<Boolean>> = _showProgressCash

    var magensaResponse: String? = null

    var paxResponse: String = ""
    var cardNumberLast4: String = ""
    var cardNamePax: String = ""
    var transactionID: String = ""



    fun setMagensaResponse(response: String?, cardNumber1: String) {
        magensaResponse = response
        cardNumberLast4 = cardNumber1
    }

    fun createSellGiftCardRequestUsingCash(): SellGiftCardRequestModel {

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
                payment_type = "Cash",
                sub_total = giftCardPurchaseAmount.toDouble(),
                terminal_id = prefProvider.getValueInt(Constants.TERMINAL_ID, 0),
                transaction_id = ""
            )

        val giftCard = GiftCard(
            gift_card_type = "Digital",//Physical
            name = "",
            amount = giftCardPurchaseAmount,
            customer_id = prefProvider.getValueInt(Constants.CUSTOMER_ID, 0),
            location_id = prefProvider.getValueInt(Constants.LOCATION_ID, 1),
            password = "",
            payment_attributes = paymentAttributes)

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
            gift_card_type = "Digital",//Physical
            name = "",
            amount = giftCardPurchaseAmount,
            customer_id = prefProvider.getValueInt(Constants.CUSTOMER_ID, 0),
            location_id = prefProvider.getValueInt(Constants.LOCATION_ID, 1),
            password = "",
            payment_attributes = paymentAttributes
        )

        return SellGiftCardRequestModel(gift_card = giftCard)
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
                                EventBus.getDefault().post(MessageEvent("${Constants.LINE_BREAK_TAB} CART_MODEL_CLEAR Thread.dumpStack(): it1 -> ${Gson().toJson(Thread.currentThread().stackTrace)}"))
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

    fun createAddValueInGiftCardRequestUsingCash(): GiftCardAddValueRequest {

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
                payment_type = "Cash",
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
                                EventBus.getDefault().post(MessageEvent("${Constants.LINE_BREAK_TAB} CART_MODEL_CLEAR Thread.dumpStack(): it1 -> ${Gson().toJson(Thread.currentThread().stackTrace)}"))
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
                        if (it.data==null && it.message.isNotEmpty()){
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