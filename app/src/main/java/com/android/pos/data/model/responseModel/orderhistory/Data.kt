package com.android.pos.data.model.responseModel.orderhistory

import com.android.pos.data.model.responseModel.giftCardOrderHistory.GiftCardRecord
import com.google.gson.annotations.SerializedName

data class Data(
    @SerializedName("addresses")
    val addresses: List<Addresse>?,
    @SerializedName("birth_date")
    val birthDate: Any?,
    @SerializedName("company")
    val company: String?,
    @SerializedName("email")
    val email: String?,
    @SerializedName("first_name")
    val firstName: String?,
    @SerializedName("id")
    val id: Int?,
    @SerializedName("enroll_to_loyalty")
    val enroll_to_loyalty: Boolean?,
    @SerializedName("final_reward")
    val final_reward: Int?,
    @SerializedName("last_name")
    val lastName: String?,
    @SerializedName("orders_list")
    val ordersList: List<Orders>?,
    @SerializedName("gift_cards_list")
    val giftCardsList: List<GiftCardRecord>?,
    @SerializedName("phones")
    val phones: List<Phone>?
)