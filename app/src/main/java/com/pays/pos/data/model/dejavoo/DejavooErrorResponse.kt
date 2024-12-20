package com.pays.pos.data.model.dejavoo

import com.google.gson.annotations.SerializedName

data class DejavooErrorResponse (

    @SerializedName("networkResponse"      ) var networkResponse      : NetworkResponse?  = NetworkResponse(),
    @SerializedName("networkTimeMs"        ) var networkTimeMs        : Int?              = null,
    @SerializedName("stackTrace"           ) var stackTrace           : ArrayList<String> = arrayListOf(),
    @SerializedName("suppressedExceptions" ) var suppressedExceptions : ArrayList<String> = arrayListOf(),
    @SerializedName("errors" ) var errors : ArrayList<Errors> = arrayListOf()
){

    data class Errors (
        @SerializedName("field") var field            : String?  = null,
        @SerializedName("message") var message          : String?  = null,
        @SerializedName("bulbIconRequired") var bulbIconRequired : Boolean? = null
    )


    data class NetworkResponse (

        @SerializedName("allHeaders"    ) var allHeaders    : ArrayList<AllHeaders> = arrayListOf(),
        @SerializedName("data"          ) var data          : ArrayList<Int>        = arrayListOf(),
        @SerializedName("headers"       ) var headers       : Headers?              = Headers(),
        @SerializedName("networkTimeMs" ) var networkTimeMs : Int?                  = null,
        @SerializedName("notModified"   ) var notModified   : Boolean?              = null,
        @SerializedName("statusCode"    ) var statusCode    : Int?                  = null

    ){

        data class AllHeaders (

            @SerializedName("mName"  ) var mName  : String? = null,
            @SerializedName("mValue" ) var mValue : String? = null

        )

        data class Headers (

            @SerializedName("Cache-Control"               ) var Cache_Control               : String? = null,
            @SerializedName("Connection"                  ) var Connection                  : String? = null,
            @SerializedName("Content-Type"                ) var Content_Type                : String? = null,
            @SerializedName("Date"                        ) var Date                        : String? = null,
            @SerializedName("Expires"                     ) var Expires                     : String? = null,
            @SerializedName("Pragma"                      ) var Pragma                      : String? = null,
            @SerializedName("Transfer-Encoding"           ) var Transfer_Encoding           : String? = null,

        )
    }
}