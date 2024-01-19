package com.pays.pos.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "CashDiscount")
data class CashDiscountModel(

    @SerializedName("rate_or_amount")
    val rate_or_amount: Double,
    @SerializedName("created_at")
    val createdAt: String,
    @PrimaryKey
    @SerializedName("id")
    val id: Int,
    @SerializedName("is_active")
    var is_active: Boolean = false,
    @SerializedName("location_id")
    val locationId: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("option_type")
    val option_type: String,
    @SerializedName("amount_type")
    val amount_type: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    @SerializedName("is_deleted")
    val isDeleted: Boolean
) : Parcelable
