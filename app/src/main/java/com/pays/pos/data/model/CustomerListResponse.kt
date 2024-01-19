package com.pays.pos.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.pays.pos.data.entities.TbCustomer
import com.pays.pos.data.model.responseModel.BaseResponse
import com.pays.pos.data.typeconvert.TCCustomer
import com.pays.pos.data.typeconvert.TypeConvertersIds
import com.pays.pos.data.typeconvert.TypeConvertorAddress
import com.pays.pos.data.typeconvert.TypeConvertorPhone
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize


@Parcelize
data class CustomerListResponse(
    @SerializedName("data")
    val `data`: List<TbCustomer>,
) : BaseResponse(), Parcelable


