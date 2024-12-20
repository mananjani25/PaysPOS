package com.pays.pos.data.model.dejavoo

import com.google.gson.annotations.SerializedName
import org.simpleframework.xml.Element
import org.simpleframework.xml.Root

@Root(name = "response", strict = false)
data class DejavooResponse(
    @field:Element(name = "Message", required = false)
    var message: String? = null,

    @field:Element(name = "ResultCode", required = false)
    var resultCode: String? = null,

    @field:Element(name = "RespMSG", required = false)
    var respMSG: String? = null,

    @SerializedName("nameValuePairs" ) var nameValuePairs : NameValuePairs? = NameValuePairs()
){


    data class NameValuePairs (

        @SerializedName("iposhpresponse" ) var iposhpresponse : Iposhpresponse? = Iposhpresponse()

    ){
        data class Iposhpresponse (

            @SerializedName("nameValuePairs" ) var nameValuePairs : NameValuePairs? = NameValuePairs()

        ){

            data class NameValuePairs (

                @SerializedName("responseCode"           ) var responseCode           : String? = null,
                @SerializedName("responseMessage"        ) var responseMessage        : String? = null,
                @SerializedName("transactionReferenceId" ) var transactionReferenceId : String? = null,
                @SerializedName("transactionType"        ) var transactionType        : String? = null,
                @SerializedName("transactionId"          ) var transactionId          : String? = null,
                @SerializedName("amount"                 ) var amount                 : String? = null,
                @SerializedName("responseApprovalCode"   ) var responseApprovalCode   : String? = null,
                @SerializedName("rrn"                    ) var rrn                    : String? = null,
                @SerializedName("transactionNumber"      ) var transactionNumber      : String? = null,
                @SerializedName("batchNumber"            ) var batchNumber            : String? = null,
                @SerializedName("totalAmount"            ) var totalAmount            : String? = null

            )
        }
    }
}
