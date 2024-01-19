package com.pays.pos.data.model.responseModel


import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pays.pos.data.entities.TaxData
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class GetTaxResponse(
    @SerializedName("data")
    val `data`: List<TaxData>,
    @SerializedName("message")
    val message: String,
    @SerializedName("status")
    val status: Int,
    @SerializedName("type")
    val type: String
) : Parcelable