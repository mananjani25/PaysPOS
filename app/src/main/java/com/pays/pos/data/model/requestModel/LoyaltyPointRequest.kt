package com.pays.pos.data.model.requestModel


import com.google.gson.annotations.SerializedName

data class LoyaltyPointRequest(
    @SerializedName("amount")
    val amount: Double?,
    @SerializedName("id")
    val id: Int?,
    @SerializedName("is_enable")
    val isEnable: Boolean?,
    @SerializedName("location_id")
    val locationId: Int?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("reward_point")
    val rewardPoint: Int?,
    @SerializedName("reward_type")
    val rewardType: String?
)