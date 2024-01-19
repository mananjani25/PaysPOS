package com.pays.pos.data.model.responseModel

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize


@Parcelize
data class AvailableTransferTableList(
    @SerializedName("data")
    val `data`: Data,
    @SerializedName("message")
    val message: String = "",

    @SerializedName("status")
    var status: Int = 0,

    @SerializedName("type")
    val type: String = ""
) : Parcelable {

    @Parcelize
    data class Data(
        @SerializedName("available_status") val available_status: List<AvailableStatu>,
        @SerializedName("occupied_tables") val occupied_tables: List<OccupiedTable>
    ) : Parcelable {}


}