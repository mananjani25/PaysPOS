package com.pays.pos.data.model.responseModel.allOrders

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class AllOrdersCountResponse(
    @SerializedName("data")
    val `data`: Data?,
    @SerializedName("message")
    val message: String?, // Orders count successfully fetched
    @SerializedName("status")
    val status: Int?, // 200
    @SerializedName("type")
    val type: String? // Success
) {
    @Keep
    data class Data(
        @SerializedName("all_orders")
        val allOrders: AllOrders?,
        @SerializedName("open_orders")
        val openOrders: OpenOrders?,
        @SerializedName("phone_orders")
        val phoneOrders: PhoneOrders?,
        @SerializedName("third_party_online_orders")
        val thirdPartyOnlineOrders: ThirdPartyOnlineOrders?,
        @SerializedName("web_orders")
        val webOrders: WebOrders?
    ) {
        @Keep
        data class AllOrders(
            @SerializedName("all")
            val all: Int?, // 84
            @SerializedName("completed")
            val completed: Int?, // 8
            @SerializedName("in_progress")
            val inProgress: Int?, // 20
            @SerializedName("pending")
            val pending: Int?, // 45
            @SerializedName("rejected")
            val rejected: Int?, // 10
            @SerializedName("upcoming")
            val upcoming: Int? // 0
        )

        @Keep
        data class OpenOrders(
            @SerializedName("active")
            val active: Int?, // 3
            @SerializedName("all")
            val all: Int?, // 4
            @SerializedName("cancelled")
            val cancelled: Int?, // 1
            @SerializedName("completed")
            val completed: Int? // 0
        )

        @Keep
        data class PhoneOrders(
            @SerializedName("active")
            val active: Int?, // 2
            @SerializedName("all")
            val all: Int?, // 3
            @SerializedName("cancelled")
            val cancelled: Int?, // 0
            @SerializedName("completed")
            val completed: Int? // 1
        )

        @Keep
        data class ThirdPartyOnlineOrders(
            @SerializedName("all")
            val all: Int?, // 73
            @SerializedName("completed")
            val completed: Int?, // 4
            @SerializedName("in_progress")
            val inProgress: Int?, // 20
            @SerializedName("pending")
            val pending: Int?, // 39
            @SerializedName("readyForPickup")
            val readyForPickup: Int?, // 4
            @SerializedName("rejected")
            val rejected: Int?, // 9
            @SerializedName("upcoming")
            val upcoming: Int? // 0
        )

        @Keep
        data class WebOrders(
            @SerializedName("all")
            val all: Int?, // 4
            @SerializedName("completed")
            val completed: Int?, // 3
            @SerializedName("in_progress")
            val inProgress: Int?, // 0
            @SerializedName("pending")
            val pending: Int?, // 1
            @SerializedName("rejected")
            val rejected: Int?, // 0
            @SerializedName("upcoming")
            val upcoming: Int? // 0
        )
    }
}