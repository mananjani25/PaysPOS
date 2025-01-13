package com.pays.pos.data.model.valor

import com.google.gson.annotations.SerializedName

data class ValorSuccessResponse(

    @SerializedName("nameValuePairs") var nameValuePairs: NameValuePairs? = NameValuePairs(),

) {


    data class Data (

        @SerializedName("CARD_TYPE"               ) var CARDTYPE             : String? = null,
        @SerializedName("EPI"                     ) var EPI                  : String? = null,
        @SerializedName("LABEL"                   ) var LABEL                : String? = null,
        @SerializedName("DATE"                    ) var DATE                 : String? = null,
        @SerializedName("REF_TXN_ID"              ) var REFTXNID             : String? = null,
        @SerializedName("TIME"                    ) var TIME                 : String? = null,
        @SerializedName("TIMEZONE"                ) var TIMEZONE             : String? = null,
        @SerializedName("AUTH_CODE"               ) var AUTHCODE             : String? = null,
        @SerializedName("APPROVAL_CODE"           ) var APPROVALCODE         : String? = null,
        @SerializedName("TOKEN"                   ) var TOKEN                : String? = null,
        @SerializedName("RRN"                     ) var RRN                  : String? = null,
        @SerializedName("TXN_TYPE"                ) var TXNTYPE              : String? = null,
        @SerializedName("CARD_SCHEME"             ) var CARDSCHEME           : String? = null,
        @SerializedName("PAN"                     ) var PAN                  : String? = null,
        @SerializedName("RESPONSE_CODE"           ) var RESPONSECODE         : String? = null,
        @SerializedName("CARDHOLDER_NAME"         ) var CARDHOLDERNAME       : String? = null,
        @SerializedName("DBA_NAME"                ) var DBANAME              : String? = null,
        @SerializedName("INVOICE_NO"              ) var INVOICENO            : String? = null,
        @SerializedName("POS_ENTRY_MODE"          ) var POSENTRYMODE         : String? = null,
        @SerializedName("BASE_AMOUNT"             ) var BASEAMOUNT           : String? = null,
        @SerializedName("CUSTOM_FEE_AMOUNT"       ) var CUSTOMFEEAMOUNT      : String? = null,
        @SerializedName("NET_AMOUNT"              ) var NETAMOUNT            : String? = null,
        @SerializedName("TIP_AMOUNT"              ) var TIPAMOUNT            : String? = null,
        @SerializedName("TAX_AMOUNT"              ) var TAXAMOUNT            : String? = null,
        @SerializedName("DEVICE_MODEL"            ) var DEVICEMODEL          : String? = null,
        @SerializedName("BATCH_NO"                ) var BATCHNO              : String? = null,
        @SerializedName("STAN_NO"                 ) var STANNO               : String? = null,
        @SerializedName("TRAN_NO"                 ) var TRANNO               : String? = null,
        @SerializedName("POS_CONDITION_CODE"      ) var POSCONDITIONCODE     : String? = null,
        @SerializedName("ORDER_DESCRIPTION"       ) var ORDERDESCRIPTION     : String? = null,
        @SerializedName("PHONE"                   ) var PHONE                : String? = null,
        @SerializedName("EMAIL"                   ) var EMAIL                : String? = null,
        @SerializedName("VT_USERNAME"             ) var VTUSERNAME           : String? = null,
        @SerializedName("LINE_ITEMS"              ) var LINEITEMS            : String? = null,
        @SerializedName("DISCOUNT_LIST"           ) var DISCOUNTLIST         : String? = null,
        @SerializedName("DISPLAY_MESSAGE"         ) var DISPLAYMESSAGE       : String? = null,
        @SerializedName("IS_SETTLED"              ) var ISSETTLED            : String? = null,
        @SerializedName("REFUND_AMOUNT"           ) var REFUNDAMOUNT         : String? = null,
        @SerializedName("REFUND_COUNT"            ) var REFUNDCOUNT          : String? = null,
        @SerializedName("IS_AUTH_COMPLETED"       ) var ISAUTHCOMPLETED      : String? = null,
        @SerializedName("IS_REVERSAL"             ) var ISREVERSAL           : String? = null,
        @SerializedName("IS_VOID"                 ) var ISVOID               : String? = null,
        @SerializedName("TXN_ORIG_DATE"           ) var TXNORIGDATE          : String? = null,
        @SerializedName("ADDITIONAL_DATA_LABEL_1" ) var ADDITIONALDATALABEL1 : String? = null,
        @SerializedName("ADDITIONAL_DATA_VALUE_1" ) var ADDITIONALDATAVALUE1 : String? = null,
        @SerializedName("ADDITIONAL_DATA_LABEL_2" ) var ADDITIONALDATALABEL2 : String? = null,
        @SerializedName("ADDITIONAL_DATA_VALUE_2" ) var ADDITIONALDATAVALUE2 : String? = null,
        @SerializedName("SUBSCRIPTION_ID"         ) var SUBSCRIPTIONID       : String? = null,
        @SerializedName("ECOMM_CHANNEL"           ) var ECOMMCHANNEL         : String? = null

    )
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