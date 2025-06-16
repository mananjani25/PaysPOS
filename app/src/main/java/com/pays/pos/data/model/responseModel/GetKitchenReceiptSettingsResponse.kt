package com.pays.pos.data.model.responseModel


import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

data class GetKitchenReceiptSettingsResponse(
    @SerializedName("data")
    val `data`: Data?=null,
    @SerializedName("message")
    val message: String,
    @SerializedName("status")
    val status: Int,
    @SerializedName("type")
    val type: String
) {
    @SuppressLint("ParcelCreator")
    @Keep
@Entity(tableName = "TbKitchenSettings")
    data class Data(
        @SerializedName("created_at")
        val createdAt: String ="",
        @SerializedName("fonts")
        val fonts: String="",
        @PrimaryKey
        @SerializedName("id")
        val id: Int?=null,
        @SerializedName("location_id")
        val locationId: Int?=null,
        @SerializedName("show_category")
        val showCategory: Boolean=false,
        @SerializedName("show_customer_address")
        val showCustomerAddress: Boolean=false,
        @SerializedName("show_customer_name")
        val showCustomerName: Boolean=false,
        @SerializedName("show_customer_phone")
        val showCustomerPhone: Boolean=false,
        @SerializedName("show_items_in_group")
        val showItemsInGroup: Boolean=false,
        @SerializedName("show_order_note")
        val showOrderNote: Boolean=false,
        @SerializedName("show_order_type")
        val showOrderType: Boolean=false,
        @SerializedName("show_team_member")
        val showTeamMember: Boolean=false,
        @SerializedName("updated_at")
        val updatedAt: String=""
    ) : Parcelable {
        override fun describeContents(): Int {
            TODO("Not yet implemented")
        }

        override fun writeToParcel(p0: Parcel, p1: Int) {
            TODO("Not yet implemented")
        }
    }
}