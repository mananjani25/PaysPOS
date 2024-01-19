package com.pays.pos.data.model.responseModel


import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pays.pos.data.entities.Employee
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class EmployeeListResponse(
    @SerializedName("data")
    val `data`: Data,
) : BaseResponse(), Parcelable {
    @Parcelize
    data class Data(
        @SerializedName("employees")
        val employees: List<Employee>
    ) : Parcelable
}