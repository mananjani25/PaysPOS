package com.android.pos.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "PAXData")
data class PAXData(

    @PrimaryKey
    @SerializedName("GlobalUid")
    var GlobalUid: String = "",

    @SerializedName("ExtData")
    var ExtData: String = "",

    @SerializedName("RefNumber")
    var RefNumber: String = "",

    @SerializedName("ECRRefNumber")
    var ECRRefNumber: String = "",

    @SerializedName("PAXtoken")
    var PAXtoken: String = "",

    @SerializedName("cardLastDigits")
    var cardLastDigits: String = "",

    @SerializedName("EDCType")
    var EDCType: String = "",


)
