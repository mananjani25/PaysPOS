package com.android.pos.data.entities

import android.os.Parcelable
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.android.pos.data.typeconvert.TypeConvertersTax
import kotlinx.parcelize.Parcelize


@TypeConverters(TypeConvertersTax::class)
@Parcelize
data class TaxModel(
    var createdAt: String? = "",
    var flateRate: String? = "",
    @PrimaryKey
    var id: Int = 0,
    var inventoryIds: String? = "",
    var isDefault: Boolean = false,
    var name: String? = "",
    var rate: String? = "",
    var taxType: String? = "",
    var updatedAt: String? = "",
) : Parcelable

