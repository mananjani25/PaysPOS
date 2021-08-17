package com.android.pos.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.android.pos.data.entities.TbCustomer
import com.android.pos.data.model.responseModel.BaseResponse
import com.android.pos.data.typeconvert.TCCustomer
import com.android.pos.data.typeconvert.TypeConvertersIds
import com.android.pos.data.typeconvert.TypeConvertorAddress
import com.android.pos.data.typeconvert.TypeConvertorPhone
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize


@Parcelize
data class CustomerListResponse(
    @SerializedName("data")
    val `data`: List<TbCustomer>,
) : BaseResponse(), Parcelable


