package com.pays.pos.ui.fragments.magtek

import android.content.Context
import com.pays.pos.data.model.responseModel.VenueDetailsResponse
import com.pays.pos.data.remote.Constants.AUTHORIZE
import com.pays.pos.data.remote.Constants.CAPTURE
import com.pays.pos.data.remote.Constants.CHASE_GATEWAY
import com.pays.pos.data.remote.Constants.ELAVON_GATEWAY
import com.pays.pos.data.remote.Constants.EPX_GATEWAY
import com.pays.pos.data.remote.Constants.FIRST_DATA_GATEWAY
import com.pays.pos.data.remote.Constants.HEARTLAND_GATEWAY
import com.pays.pos.data.remote.Constants.MAGENSA_SETTINGS1
import com.pays.pos.data.remote.Constants.RAPID_PILOT
import com.pays.pos.data.remote.Constants.RAPID_PRODUCTION
import com.pays.pos.data.remote.Constants.REFUND1
import com.pays.pos.data.remote.Constants.TSYS_GATEWAY
import com.pays.pos.data.remote.Constants.TSYS_PILOT
import com.pays.pos.data.remote.Constants.TSYS_PRODUCTION
import com.pays.pos.data.remote.Constants.VANIT_EXORESS_GATEWAY
import com.pays.pos.data.remote.Constants.VOID
import com.pays.pos.di.PrefProvider
import com.pays.pos.utils.MethodUtils
import com.pays.pos.utils.MethodUtils.Companion.getTwoDecimalWithZero
import com.pays.pos.utils.MethodUtils.Companion.toPrecision
import com.pays.pos.utils.Pref
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.DecimalFormat
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MagtekRequestUtils @Inject constructor(
    @ApplicationContext private val mContext: Context,
    val prefProvider: PrefProvider
) {


    var magensaSettingModel: VenueDetailsResponse.Data.MagensaSettings? = null

    init {
//        val magensaSettings = prefProvider.getValue(MAGENSA_SETTINGS, "")

        getValue()

    }

    private fun getValue() {
        val magensaSettings = Pref.getValue(mContext, MAGENSA_SETTINGS1, "")

        magensaSettingModel =
            Gson().fromJson(magensaSettings, VenueDetailsResponse.Data.MagensaSettings::class.java)
    }

    //SALE, AUTHORIZE, CAPTURE, VOID, REFUND,FORCE, REJECT. In case of JSON payload, provide
    //enum-integer-value i.e., 1, 2, 3, 4, 5, 6, 7 respectively


    fun processManualEntry(
        payableAmount: Double,
        cardNumber: String,
        expDate: String,
        cardCVV: String
    ): JsonArray {

        getValue()

        val jsonArray = JsonArray()

        if (processorName() == TSYS_PRODUCTION || processorName() == TSYS_PILOT) {

            val processCardSwipeRequest = ManualEntryRequestItem(
                authentication = authentication(),
                customerTransactionID = System.currentTimeMillis().toString(),
                manualEntryInput = ManualEntryRequestItem.ManualEntryInput(
                    cVV = cardCVV,
                    expirationDate = expDate,
                    pAN = cardNumber
                ),
                transactionInput = ProcessCardSwipeRequest.TransactionInput(
                    amount = getPayableAmount(payableAmount).toPrecision(2),
                    processorName = processorName(),
                    transactionType = AUTHORIZE
                )
            )

            val jsonObject = Gson().toJson(processCardSwipeRequest)
            val jsonElement = Gson().fromJson(jsonObject, JsonObject::class.java)

            jsonArray.add(jsonElement)

        } else if (processorName() == RAPID_PRODUCTION || processorName() == RAPID_PILOT) {

            val processCardSwipeRequest = ManualEntryRequestRapid(
                authentication = authentication(),
                customerTransactionID = System.currentTimeMillis().toString(),
                manualEntryInput = ManualEntryRequestRapid.ManualEntryInput(
                    cVV = cardCVV,
                    expirationDate = expDate,
                    pAN = cardNumber
                ),
                transactionInput = ProcessCardSwipeRequest.TransactionInputRapid(
                    amount = payableAmount.toInt(),
                    processorName = processorName(),
                    transactionType = AUTHORIZE
                )
            )

            val jsonObject = Gson().toJson(processCardSwipeRequest)
            val jsonElement = Gson().fromJson(jsonObject, JsonObject::class.java)

            jsonArray.add(jsonElement)
        }



        return jsonArray
    }


    fun processCardSwipe(
        payableAmount: Double,
        ksn: String,
        magnePrint: String,
        magnePrintStatus: String,
        track2: String
    ): JsonArray {

        getValue()

        val jsonArray = JsonArray()

        if (processorName() == TSYS_PRODUCTION || processorName() == TSYS_PILOT) {

            val processCardSwipeRequest = ProcessCardSwipeRequest(
                authentication = authentication(),
                customerTransactionID = System.currentTimeMillis().toString(),
                cardSwipeInput = ProcessCardSwipeRequest.CardSwipeInput(
                    encryptedCardSwipe = ProcessCardSwipeRequest.CardSwipeInput.EncryptedCardSwipe(
                        kSN = ksn,
                        magnePrint = magnePrint,
                        magnePrintStatus = magnePrintStatus,
                        track2 = track2
                    )
                ),
                transactionInput = ProcessCardSwipeRequest.TransactionInput(
                    amount = getPayableAmount(payableAmount).toPrecision(2),
                    processorName = processorName(),
                    transactionType = AUTHORIZE
                )
            )

            val jsonObject = Gson().toJson(processCardSwipeRequest)
            val jsonElement = Gson().fromJson(jsonObject, JsonObject::class.java)

            jsonArray.add(jsonElement)
        } else if (processorName() == RAPID_PRODUCTION || processorName() == RAPID_PILOT) {

            val processCardSwipeRequest = ProcessCardSwipeRequestRapid(
                authentication = authenticationRapid(),
                customerTransactionID = System.currentTimeMillis().toString(),
                cardSwipeInput = ProcessCardSwipeRequestRapid.CardSwipeInput(
                    encryptedCardSwipe = ProcessCardSwipeRequestRapid.CardSwipeInput.EncryptedCardSwipe(
                        kSN = ksn,
                        magnePrint = magnePrint,
                        magnePrintStatus = magnePrintStatus,
                        track2 = track2
                    )
                ),
                transactionInput = ProcessCardSwipeRequestRapid.TransactionInputRapid(
                    amount = payableAmount.toInt(),
                    processorName = processorName(),
                    transactionType = AUTHORIZE
                )
            )

            val jsonObject = Gson().toJson(processCardSwipeRequest)
            val jsonElement = Gson().fromJson(jsonObject, JsonObject::class.java)

            jsonArray.add(jsonElement)
        }



        return jsonArray
    }


    fun processData(payableAmount: Double, data: String, transactionType: Int): JsonArray {

        val jsonArray = JsonArray()

        getValue()




        if (processorName() == TSYS_PRODUCTION || processorName() == TSYS_PILOT) {


            val transactionInput = ProcessCardSwipeRequest.TransactionInput(
                amount = getPayableAmount(payableAmount).toPrecision(2),
                processorName = processorName(),
                transactionType = transactionType
            )

            val processDataRequest = ProcessDataRequest(
                authentication = authentication(),
                customerTransactionID = System.currentTimeMillis().toString(),
                dataInput = ProcessDataRequest.DataInput(
                    data = data,
                    dataFormatType = 0,
                    encryptionInfo = ProcessDataRequest.DataInput.EncryptionInfo(
                        encryptionType = "80",
                        numberOfPaddedBytes = "0"
                    ),
                    isEncrypted = true,
                    paymentMode = 2
                ),
                transactionInput = transactionInput!!
            )

            val jsonObject = Gson().toJson(processDataRequest)
            val jsonElement = Gson().fromJson(jsonObject, JsonObject::class.java)

            jsonArray.add(jsonElement)

        } else if (processorName() == RAPID_PRODUCTION || processorName() == RAPID_PILOT) {


            val transactionInput = ProcessCardSwipeRequest.TransactionInputRapid(
                amount = payableAmount.toInt(),
                processorName = processorName(),
                transactionType = transactionType,
                transactionInputDetailsRapid("")
            )

            val processDataRequest = ProcessDataRequestRapid(
                authentication = authentication(),
                customerTransactionID = System.currentTimeMillis().toString(),
                dataInput = ProcessDataRequest.DataInput(
                    data = data,
                    dataFormatType = 0,
                    encryptionInfo = ProcessDataRequest.DataInput.EncryptionInfo(
                        encryptionType = "80",
                        numberOfPaddedBytes = "0"
                    ),
                    isEncrypted = true,
                    paymentMode = 2
                ),
                transactionInput = transactionInput
            )

            val jsonObject = Gson().toJson(processDataRequest)
            val jsonElement = Gson().fromJson(jsonObject, JsonObject::class.java)

            jsonArray.add(jsonElement)
        }



        return jsonArray
    }

    private fun getPayableAmount(price: Double): Double {
        if (processorName() == TSYS_PRODUCTION || processorName() == TSYS_PILOT) {

            val amount = price/100.00
            val df = DecimalFormat("0.00")
           return java.lang.Double.valueOf(df.format(amount))

//            return getTwoDecimalWithZero((price / 100.00))
        } else if (processorName() == RAPID_PRODUCTION || processorName() == RAPID_PILOT) {
            return price
        }
        return 0.0
    }

    // Rapid Connect (First Data Nashville/Omaha/North) -- REFUND(5)/CAPTURE(3)
    fun processTokenFirstData(
        payableAmount: Double,
        token: String,
        customerTransactionID: String,
        payloadResponseValue: String,
        transactionType: Int,
    ): JsonArray {

        getValue()
        val jsonArray = JsonArray()
        if (processorName() == TSYS_PRODUCTION || processorName() == TSYS_PILOT) {
            val processTokenRequest = ProcessTokenRequest(
                additionalRequestData = additionalRequestDataList(payloadResponseValue),
                authentication = authentication(),
                customerTransactionID = customerTransactionID,
                token = token,
                transactionInput = ProcessCardSwipeRequest.TransactionInput(
                    amount = getPayableAmount(payableAmount).toPrecision(2),
                    processorName = processorName(),
                    transactionType = transactionType,
                    transactionInputDetails = transactionInputDetails("")
                )
            )

            val jsonObject = Gson().toJson(processTokenRequest)
            val jsonElement = Gson().fromJson(jsonObject, JsonObject::class.java)


            jsonArray.add(jsonElement)

        } else if (processorName() == RAPID_PRODUCTION || processorName() == RAPID_PILOT) {

            val processTokenRequest = ProcessTokenRequestRapid(
                additionalRequestData = additionalRequestDataList(payloadResponseValue),
                authentication = authentication(),
                customerTransactionID = customerTransactionID,
                token = token,
                transactionInput = ProcessCardSwipeRequest.TransactionInputRapid(
                    amount = payableAmount.toInt(),
                    processorName = processorName(),
                    transactionType = transactionType,
                    transactionInputDetails = transactionInputDetailsRapid("")
                )
            )

            val jsonObject = Gson().toJson(processTokenRequest)
            val jsonElement = Gson().fromJson(jsonObject, JsonObject::class.java)


            jsonArray.add(jsonElement)
        }


        return jsonArray
    }

    //-----------------------------------------------------Elavon--------------------------------------------------------------

    // Elavon (Converge) -- REFUND(5)
    fun processTokenElavon(
        payableAmount: Double,
        token: String,
        customerTransactionID: String,
        lastRecordNumber: String,

        ): JsonArray {

        val jsonArray = JsonArray()

        val processTokenRequest = ProcessTokenRequest(
            additionalRequestData = additionalRequestDataList(""),
            authentication = authentication(),
            customerTransactionID = customerTransactionID,
            token = token,
            transactionInput = ProcessCardSwipeRequest.TransactionInput(
                amount = payableAmount.toPrecision(2),
                processorName = processorName(),
                transactionType = REFUND1,
                transactionInputDetails = transactionInputDetails(lastRecordNumber)
            )
        )

        val jsonObject = Gson().toJson(processTokenRequest)
        val jsonElement = Gson().fromJson(jsonObject, JsonObject::class.java)

        jsonArray.add(jsonElement)

        return jsonArray
    }

    // Elavon (Converge) -- VOID(4)
    fun processTokenElavonVoid(
        payableAmount: Double,
        token: String,
        customerTransactionID: String,
        lastRecordNumber: String,
        responseCode: String,

        ): JsonArray {

        val jsonArray = JsonArray()

        val processTokenRequest = ProcessTokenRequest(
            additionalRequestData = additionalRequestDataList(""),
            authentication = authentication(),
            customerTransactionID = customerTransactionID,
            token = token,
            transactionInput = ProcessCardSwipeRequest.TransactionInput(
                amount = payableAmount.toPrecision(2),
                processorName = processorName(),
                transactionType = VOID,
                transactionInputDetails = transactionInputDetailsElavon(
                    lastRecordNumber,
                    responseCode
                )
            )
        )

        val jsonObject = Gson().toJson(processTokenRequest)
        val jsonElement = Gson().fromJson(jsonObject, JsonObject::class.java)

        jsonArray.add(jsonElement)

        return jsonArray
    }

    //-------------------------------------------------------EPX------------------------------------------------------------


    // EPX - CAPTURE(capture without tip)/VOID/REFUND
    fun processReferenceIDEPX(
        payableAmount: Double,
        customerTransactionID: String,
        transactionID: String,
        transactionType: Int
    ): JsonArray {

        val processTokenRequest = ProcessTokenRequest(
            authentication = authentication(),
            customerTransactionID = customerTransactionID,
            transactionInput = ProcessCardSwipeRequest.TransactionInput(
                amount = payableAmount.toPrecision(2),
                processorName = processorName(),
                transactionType = transactionType,
                referenceTransactionID = transactionID,
            )
        )

        val jsonObject = Gson().toJson(processTokenRequest)
        val jsonElement = Gson().fromJson(jsonObject, JsonObject::class.java)
        val jsonArray = JsonArray()
        jsonArray.add(jsonElement)

        return jsonArray
    }


    // EPX - FORCE(capture with tip)
    fun processReferenceIDEPXForce(
        payableAmount: Double,
        customerTransactionID: String,
        transactionID: String,
        transactionType: Int,
        tipAmount: String,

        ): JsonArray {

        val processTokenRequest = ProcessTokenRequest(
            authentication = authentication(),
            customerTransactionID = customerTransactionID,
            transactionInput = ProcessCardSwipeRequest.TransactionInput(
                amount = payableAmount.toPrecision(2),
                processorName = processorName(),
                transactionType = transactionType,
                referenceTransactionID = transactionID,
                transactionInputDetails = transactionInputDetails(tipAmount)
            )
        )

        val jsonObject = Gson().toJson(processTokenRequest)
        val jsonElement = Gson().fromJson(jsonObject, JsonObject::class.java)
        val jsonArray = JsonArray()
        jsonArray.add(jsonElement)

        return jsonArray
    }


    //-------------------------------------------------------Vantiv Express (WorldPay)------------------------------------------------------------


    // Vantiv Express (WorldPay) - CAPTURE
    fun processReferenceIDCapture(
        payableAmount: Double,
        customerTransactionID: String,
        transactionID: String,
        referenceAuthCode: String,
        cardInputCode: String,
    ): JsonArray {

        val processTokenRequest = ProcessTokenRequest(
            authentication = authentication(),
            customerTransactionID = customerTransactionID,
            transactionInput = ProcessCardSwipeRequest.TransactionInput(
                amount = payableAmount.toPrecision(2),
                processorName = processorName(),
                transactionType = CAPTURE,
                referenceTransactionID = transactionID,
                referenceAuthCode = referenceAuthCode,
                transactionInputDetails = transactionInputDetails(cardInputCode)
            )
        )

        val jsonObject = Gson().toJson(processTokenRequest)
        val jsonElement = Gson().fromJson(jsonObject, JsonObject::class.java)
        val jsonArray = JsonArray()
        jsonArray.add(jsonElement)

        return jsonArray
    }


    // Vantiv Express (WorldPay) - REFUND
    fun processReferenceIDRefund(
        payableAmount: Double,
        customerTransactionID: String,
        transactionID: String,
        referenceAuthCode: String,

        ): JsonArray {

        val processTokenRequest = ProcessTokenRequest(
            authentication = authentication(),
            customerTransactionID = customerTransactionID,
            transactionInput = ProcessCardSwipeRequest.TransactionInput(
                amount = payableAmount.toPrecision(2),
                processorName = processorName(),
                transactionType = REFUND1,
                referenceTransactionID = transactionID,
                referenceAuthCode = referenceAuthCode
            )
        )

        val jsonObject = Gson().toJson(processTokenRequest)
        val jsonElement = Gson().fromJson(jsonObject, JsonObject::class.java)
        val jsonArray = JsonArray()
        jsonArray.add(jsonElement)

        return jsonArray
    }

    // Vantiv Express (WorldPay) - VOID
    fun processReferenceIDVoid(
        payableAmount: Double,
        customerTransactionID: String,
        transactionID: String,
        reversalType: String,
        referenceAuthCode: String,

        ): JsonArray {

        val processTokenRequest = ProcessTokenRequest(
            authentication = authentication(),
            customerTransactionID = customerTransactionID,
            transactionInput = ProcessCardSwipeRequest.TransactionInput(
                amount = payableAmount.toPrecision(2),
                processorName = processorName(),
                transactionType = VOID,
                referenceTransactionID = transactionID,
                referenceAuthCode = referenceAuthCode,
                transactionInputDetails = transactionInputDetails(reversalType)
            )
        )

        val jsonObject = Gson().toJson(processTokenRequest)
        val jsonElement = Gson().fromJson(jsonObject, JsonObject::class.java)
        val jsonArray = JsonArray()
        jsonArray.add(jsonElement)

        return jsonArray
    }

    //--------------------------------------------------------------Chase (Orbital)-----------------------------------------------------


    // Chase (Orbital) - CAPTURE/REFUND (retail or restaurant)
    fun processTokenChase(
        payableAmount: Double,
        token: String,
        customerTransactionID: String,
        priorAuthCd: String,
        transactionType: Int,

        ): JsonArray {

        val jsonArray = JsonArray()


        val processTokenRequest = ProcessTokenRequest(
            authentication = authentication(),
            customerTransactionID = customerTransactionID,
            token = token,
            transactionInput = ProcessCardSwipeRequest.TransactionInput(
                amount = payableAmount.toPrecision(2),
                processorName = processorName(),
                transactionType = transactionType,
                transactionInputDetails = transactionInputDetails(priorAuthCd)
            )
        )

        val jsonObject = Gson().toJson(processTokenRequest)
        val jsonElement = Gson().fromJson(jsonObject, JsonObject::class.java)

        jsonArray.add(jsonElement)

        return jsonArray
    }


    // Chase (Orbital) - REFUND (retail only)
    fun processTokenChaseRefund(
        payableAmount: Double,
        customerTransactionID: String,
        priorAuthCd: String,
        referenceTransactionID: String,
        transactionType: Int,

        ): JsonArray {

        val jsonArray = JsonArray()


        val processTokenRequest = ProcessTokenRequest(
            authentication = authentication(),
            customerTransactionID = customerTransactionID,
            transactionInput = ProcessCardSwipeRequest.TransactionInput(
                amount = payableAmount.toPrecision(2),
                processorName = processorName(),
                transactionType = transactionType,
                referenceAuthCode = priorAuthCd,
                referenceTransactionID = referenceTransactionID,
            )
        )

        val jsonObject = Gson().toJson(processTokenRequest)
        val jsonElement = Gson().fromJson(jsonObject, JsonObject::class.java)

        jsonArray.add(jsonElement)

        return jsonArray
    }


    //-------------------------------------------------------------Heartland------------------------------------------------------

    // Heartland - VOID/REFUND
    fun processReferenceIHeartland(
        payableAmount: Double,
        customerTransactionID: String,
        transactionID: String,
        referenceAuthCode: String,
        transactionType: Int
    ): JsonArray {

        val processTokenRequest = ProcessTokenRequest(
            authentication = authentication(),
            customerTransactionID = customerTransactionID,
            transactionInput = ProcessCardSwipeRequest.TransactionInput(
                amount = payableAmount.toPrecision(2),
                processorName = processorName(),
                transactionType = transactionType,
                referenceTransactionID = transactionID,
                referenceAuthCode = referenceAuthCode
            )
        )

        val jsonObject = Gson().toJson(processTokenRequest)
        val jsonElement = Gson().fromJson(jsonObject, JsonObject::class.java)
        val jsonArray = JsonArray()
        jsonArray.add(jsonElement)

        return jsonArray
    }

    // Heartland - CAPTURE
    fun processReferenceIdHeartlandCapture(
        payableAmount: Double,
        customerTransactionID: String,
        transactionID: String,
        referenceAuthCode: String,
        tipAdjust: String,
    ): JsonArray {

        val processTokenRequest = ProcessTokenRequest(
            authentication = authentication(),
            customerTransactionID = customerTransactionID,
            transactionInput = ProcessCardSwipeRequest.TransactionInput(
                amount = payableAmount.toPrecision(2),
                processorName = processorName(),
                transactionType = CAPTURE,
                referenceTransactionID = transactionID,
                referenceAuthCode = referenceAuthCode,
                transactionInputDetails = transactionInputDetails(tipAdjust)
            )
        )

        val jsonObject = Gson().toJson(processTokenRequest)
        val jsonElement = Gson().fromJson(jsonObject, JsonObject::class.java)
        val jsonArray = JsonArray()
        jsonArray.add(jsonElement)

        return jsonArray
    }

    //--------------------------------------------------------------TSYS (MultiPass)----------------------------------------------

    // TSYS - VOID/REFUND
    fun processReferenceIDTSYS(
        payableAmount: Double,
        customerTransactionID: String,
        transactionID: String,
        transactionType: Int
    ): JsonArray {

        val processTokenRequest = ProcessTokenRequest(
            authentication = authentication(),
            customerTransactionID = customerTransactionID,
            transactionInput = ProcessCardSwipeRequest.TransactionInput(
                processorName = processorName(),
                transactionType = transactionType,
                referenceTransactionID = transactionID,
                amount = payableAmount.toPrecision(2)
            )
        )

        val jsonObject = Gson().toJson(processTokenRequest)
        val jsonElement = Gson().fromJson(jsonObject, JsonObject::class.java)
        val jsonArray = JsonArray()
        jsonArray.add(jsonElement)

        return jsonArray
    }


    // TSYS - CAPTURE
    fun processReferenceIDTSYSCapture(
        payableAmount: Double,
        customerTransactionID: String,
        transactionID: String,
        tip: Double,
    ): JsonArray {

        val list = ArrayList<TsysCaptureRequest.TransactionInput.TransactionInputDetail>()

        val mcc = TsysCaptureRequest.TransactionInput.TransactionInputDetail(
            key = "tip",
            value = MethodUtils.roundOffAmountString(tip)

        )
        list.add(mcc)

        val processTokenRequest = TsysCaptureRequest(
            authentication = TsysCaptureRequest.Authentication(
                customerCode = customerCode(),
                password = password(),
                username = userName()
            ),
            customerTransactionID = customerTransactionID,
            transactionInput = TsysCaptureRequest.TransactionInput(
                amount = MethodUtils.roundOffAmountString(payableAmount + tip),
                processorName = processorName(),
                referenceTransactionID = transactionID,
                transactionInputDetails = list,
                transactionType = CAPTURE
            )
        )

//        val processTokenRequest = ProcessTokenRequest(
//            authentication = authentication(),
//            customerTransactionID = customerTransactionID,
//            transactionInput = ProcessCardSwipeRequest.TransactionInput(
//                amount = MethodUtils.roundOffAmountDouble(payableAmount),
//                processorName = processorName(),
//                transactionType = CAPTURE,
//                referenceTransactionID = transactionID,
//                transactionInputDetails = transactionInputDetails(tip)
//            )
//        )

        val jsonObject = Gson().toJson(processTokenRequest)
        val jsonElement = Gson().fromJson(jsonObject, JsonObject::class.java)
        val jsonArray = JsonArray()
        jsonArray.add(jsonElement)

        return jsonArray
    }

    //----------------------------------------------------------------------------------------------------------------------------


    private fun authentication() = ProcessCardSwipeRequest.Authentication(
        customerCode = customerCode(),
        password = password(),
        username = userName()
    )

    private fun authenticationRapid() = ProcessCardSwipeRequestRapid.Authentication(
        customerCode = customerCode(),
        password = password(),
        username = userName()
    )

    private fun transactionInputDetails(keyValuePair: String): List<ProcessCardSwipeRequest.TransactionInput.KeyValue> {

        val list = ArrayList<ProcessCardSwipeRequest.TransactionInput.KeyValue>()

        when {
            ELAVON_GATEWAY == gatewayName() -> {

                val mcc = ProcessCardSwipeRequest.TransactionInput.KeyValue(
                    key = "LastRecordNumber",
                    value = "1"

                )
                list.add(mcc)

            }
            FIRST_DATA_GATEWAY == gatewayName() -> {

                val mcc = ProcessCardSwipeRequest.TransactionInput.KeyValue(
                    key = "MCC",
                    value = mccCode()

                )
                val partAuthorityApprovalCapable =
                    ProcessCardSwipeRequest.TransactionInput.KeyValue(
                        key = "PartAuthrztnApprvlCapablt",
                        value = "1"

                    )
                list.add(mcc)
                list.add(partAuthorityApprovalCapable)
            }
            CHASE_GATEWAY == gatewayName() -> {

                val mcc = ProcessCardSwipeRequest.TransactionInput.KeyValue(
                    key = "priorAuthCd",
                    value = keyValuePair

                )
                val partAuthorityApprovalCapable =
                    ProcessCardSwipeRequest.TransactionInput.KeyValue(
                        key = "partialAuthInd",
                        value = "1"

                    )
                list.add(mcc)
                list.add(partAuthorityApprovalCapable)
            }
            ELAVON_GATEWAY == gatewayName() -> {

                val mcc = ProcessCardSwipeRequest.TransactionInput.KeyValue(
                    key = "LastRecordNumber",
                    value = keyValuePair

                )
                list.add(mcc)
            }
            EPX_GATEWAY == gatewayName() -> {

                val mcc = ProcessCardSwipeRequest.TransactionInput.KeyValue(
                    key = "TipAmount",
                    value = keyValuePair

                )
                list.add(mcc)
            }

            HEARTLAND_GATEWAY == gatewayName() -> {

                val mcc = ProcessCardSwipeRequest.TransactionInput.KeyValue(
                    key = "TipAdjust",
                    value = keyValuePair

                )
                list.add(mcc)
            }

            VANIT_EXORESS_GATEWAY == gatewayName() -> {

                val mcc = ProcessCardSwipeRequest.TransactionInput.KeyValue(
                    key = "CardInputCode",
                    value = keyValuePair

                )
                list.add(mcc)
            }

            TSYS_GATEWAY == gatewayName() -> {

                val mcc = ProcessCardSwipeRequest.TransactionInput.KeyValue(
                    key = "tip",
                    value = MethodUtils.roundOffAmountStringToDouble(keyValuePair)

                )
                list.add(mcc)
            }
        }
        return list
    }


    private fun transactionInputDetailsRapid(keyValuePair: String): List<ProcessCardSwipeRequest.TransactionInputRapid.KeyValue> {

        val list = ArrayList<ProcessCardSwipeRequest.TransactionInputRapid.KeyValue>()

        when {
            ELAVON_GATEWAY == gatewayName() -> {

                val mcc = ProcessCardSwipeRequest.TransactionInputRapid.KeyValue(
                    key = "LastRecordNumber",
                    value = "1"

                )
                list.add(mcc)

            }
            FIRST_DATA_GATEWAY == gatewayName() -> {

                val mcc = ProcessCardSwipeRequest.TransactionInputRapid.KeyValue(
                    key = "MCC",
                    value = mccCode()

                )
                val partAuthorityApprovalCapable =
                    ProcessCardSwipeRequest.TransactionInputRapid.KeyValue(
                        key = "PartAuthrztnApprvlCapablt",
                        value = "1"

                    )
                list.add(mcc)
                list.add(partAuthorityApprovalCapable)
            }
            CHASE_GATEWAY == gatewayName() -> {

                val mcc = ProcessCardSwipeRequest.TransactionInputRapid.KeyValue(
                    key = "priorAuthCd",
                    value = keyValuePair

                )
                val partAuthorityApprovalCapable =
                    ProcessCardSwipeRequest.TransactionInputRapid.KeyValue(
                        key = "partialAuthInd",
                        value = "1"

                    )
                list.add(mcc)
                list.add(partAuthorityApprovalCapable)
            }
            ELAVON_GATEWAY == gatewayName() -> {

                val mcc = ProcessCardSwipeRequest.TransactionInputRapid.KeyValue(
                    key = "LastRecordNumber",
                    value = keyValuePair

                )
                list.add(mcc)
            }
            EPX_GATEWAY == gatewayName() -> {

                val mcc = ProcessCardSwipeRequest.TransactionInputRapid.KeyValue(
                    key = "TipAmount",
                    value = keyValuePair

                )
                list.add(mcc)
            }

            HEARTLAND_GATEWAY == gatewayName() -> {

                val mcc = ProcessCardSwipeRequest.TransactionInputRapid.KeyValue(
                    key = "TipAdjust",
                    value = keyValuePair

                )
                list.add(mcc)
            }

            VANIT_EXORESS_GATEWAY == gatewayName() -> {

                val mcc = ProcessCardSwipeRequest.TransactionInputRapid.KeyValue(
                    key = "CardInputCode",
                    value = keyValuePair

                )
                list.add(mcc)
            }

            TSYS_GATEWAY == gatewayName() -> {

                val mcc = ProcessCardSwipeRequest.TransactionInputRapid.KeyValue(
                    key = "tip",
                    value = keyValuePair

                )
                list.add(mcc)
            }
        }
        return list
    }


    private fun transactionInputDetailsElavon(
        keyValuePair: String,
        responseCode: String
    ): List<ProcessCardSwipeRequest.TransactionInput.KeyValue> {

        val list = ArrayList<ProcessCardSwipeRequest.TransactionInput.KeyValue>()

        val mcc = ProcessCardSwipeRequest.TransactionInput.KeyValue(
            key = "LastRecordNumber",
            value = keyValuePair

        )
        val partAuthorityApprovalCapable =
            ProcessCardSwipeRequest.TransactionInput.KeyValue(
                key = "ResponseCode",
                value = responseCode

            )
        list.add(mcc)
        list.add(partAuthorityApprovalCapable)


        return list
    }

    fun gatewayName(): String {

        if (processorName() == RAPID_PILOT || processorName() == RAPID_PRODUCTION) {
            return FIRST_DATA_GATEWAY
        } else if (processorName() == TSYS_PILOT || processorName() == TSYS_PRODUCTION) {
            return TSYS_GATEWAY
        } else if (processorName() == "VantivExpress - Pilot" || processorName() == "VantivExpress - Production") {
            return VANIT_EXORESS_GATEWAY
        } else if (processorName() == "Chase - Pilot" || processorName() == "Chase - Production") {
            return CHASE_GATEWAY
        } else if (processorName() == "Heartland - Pilot" || processorName() == "Heartland - Production") {
            return HEARTLAND_GATEWAY
        } else if (processorName() == "EPX - Pilot" || processorName() == "EPX - Production") {
            return EPX_GATEWAY
        }

        return ""
    }

    fun isRetail(): Boolean {

        return true
    }

    private fun additionalRequestDataList(payloadResponseValue: String): List<ProcessCardSwipeRequest.TransactionInput.KeyValue> {

        val list = ArrayList<ProcessCardSwipeRequest.TransactionInput.KeyValue>()

        val mcc = ProcessCardSwipeRequest.TransactionInput.KeyValue(
            key = "PayloadResponse",
            value = payloadResponseValue

        )
        list.add(mcc)

        return list
    }

    private fun processorName(): String {
        return magensaSettingModel?.processor_name.toString()/*RAPID_PRODUCTION*/
    }

    fun customerName(): String {
        return magensaSettingModel?.customer_name.toString()/*"SAM'S SHOP"*/
    }

    private fun customerCode(): String {
        return magensaSettingModel?.customer_code.toString()/*"OR10249676"*/
    }

    fun userName(): String {
        return magensaSettingModel?.user_name.toString()/*"MAG032795798"*/
    }

    private fun mccCode(): String {
        return magensaSettingModel?.mcc_code.toString()/*"7399"*/
    }

    fun password(): String {
        return magensaSettingModel?.password.toString()/*"cV!IRXl8wnHCS6"*/
    }

}

