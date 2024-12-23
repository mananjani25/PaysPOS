package com.pays.pos.data.model.valor

import com.google.gson.annotations.SerializedName

data class ValorSuccessResponse(

    @SerializedName("nameValuePairs") var nameValuePairs: NameValuePairs? = NameValuePairs()

) {
    data class NameValuePairs(

        @SerializedName("note") var note: String? = null,
        @SerializedName("error_no") var errorNo: String? = null,
        @SerializedName("msg") var msg: String? = null,
        @SerializedName("response") var response: Response? = Response()

    ) {
        data class Response(

            @SerializedName("nameValuePairs") var nameValuePairs: NameValuePairs? = NameValuePairs()

        ) {
            data class NameValuePairs(
                @SerializedName("MSG") var MSG: String? = null,
                @SerializedName("ERROR_CODE") var ERRORCODE: String? = null,
                @SerializedName("ERROR_MSG") var ERRORMSG: String? = null,
                @SerializedName("STATE") var STATE: Int? = null,
                @SerializedName("EPI") var EPI: String? = null,
                @SerializedName("SERIAL_NO") var SERIALNO: String? = null,
                @SerializedName("MER_TXN_ID") var MERTXNID: String? = null,
                @SerializedName("STAN_ID") var STANID: String? = null,
                @SerializedName("INVOICENUMBER") var INVOICENUMBER: String? = null,
                @SerializedName("INITIATOR_NAME") var INITIATORNAME: String? = null,
                @SerializedName("AMOUNT") var AMOUNT: String? = null,
                @SerializedName("PARTIAL") var PARTIAL: String? = null,
                @SerializedName("ISSUER") var ISSUER: String? = null,
                @SerializedName("MASKED_PAN") var MASKEDPAN: String? = null,
                @SerializedName("RRN") var RRN: String? = null,
                @SerializedName("CODE") var CODE: String? = null,
                @SerializedName("AUTH_RSP_TEXT") var AUTHRSPTEXT: String? = null,
                @SerializedName("DATE") var DATE: String? = null,
                @SerializedName("TRAN_NO") var TRANNO: String? = null,
                @SerializedName("BATCH_NO") var BATCHNO: String? = null,
                @SerializedName("AID") var AID: String? = null,
                @SerializedName("TRAN_TYPE") var TRANTYPE: String? = null,
                @SerializedName("TRAN_METHOD") var TRANMETHOD: String? = null,
                @SerializedName("SURCHARGE_AMOUNT") var SURCHARGEAMOUNT: String? = null,
                @SerializedName("ENTRY_MODE") var ENTRYMODE: String? = null,
                @SerializedName("EXPIRY_DATE") var EXPIRYDATE: String? = null,
                @SerializedName("CARDHOLDER_NAME") var CARDHOLDERNAME: String? = null,
                @SerializedName("ADDINFO_DATA1     ") var ADDINFODATA1: String? = null,
                @SerializedName("ADDINFO_DATA2     ") var ADDINFODATA2: String? = null,
                @SerializedName("ADDINFO_TEXT1     ") var ADDINFOTEXT1: String? = null,
                @SerializedName("ADDINFO_TEXT2     ") var ADDINFOTEXT2: String? = null,
                @SerializedName("TXN_ID") var TXNID: String? = null,
                @SerializedName("TOTAL_AMOUNT") var TOTALAMOUNT: String? = null

            )

        }
    }


}