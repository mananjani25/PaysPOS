package com.pays.pos.data.model.responseModel

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class AvailableStatu(
    @SerializedName("floor_plan_tables") val floor_plan_tables: List<TableList>,
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String
) : Parcelable {

    @Parcelize
    data class TableList(
        @SerializedName("id") val id: Int,
        @SerializedName("table_name") val tableName: String,
        @SerializedName("table_number") val tableNumber: String,
        @SerializedName("status") val status: String,
        @SerializedName("floor_plan_id") val floorPlanId: Int

    ) : Parcelable {

        override fun toString(): String {
            return tableName.toString()
        }
    }

    override fun toString(): String {
        return name.toString()
    }
}