package com.pays.pos.data.entities

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Keep
@Entity(tableName = "PAXData")
data class PAXData(

    @PrimaryKey
    @SerializedName("GlobalUid")
    var globalUid: String = "",

    @SerializedName("ExtData")
    var extData: String = "",

    @SerializedName("RefNumber")
    var refNumber: String = "",

    @SerializedName("ECRRefNumber")
    var eCRRefNumber: String = "",

    @SerializedName("PAXtoken")
    var paxToken: String = "",

    @SerializedName("cardLastDigits")
    var cardLastDigits: String = "",

    @SerializedName("EDCType")
    var EDCType: String = "",


)
