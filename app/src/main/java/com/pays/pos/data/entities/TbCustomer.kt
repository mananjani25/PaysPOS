package com.pays.pos.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.pays.pos.data.model.CustomerSearchList
import com.pays.pos.data.model.responseModel.GetOrderDetailsResponse
import com.pays.pos.data.model.responseModel.OnlineOrderResponseModel
import com.pays.pos.data.model.responseModel.OpenOrderResponse
import com.pays.pos.data.typeconvert.TypeConvertorAddress
import com.pays.pos.data.typeconvert.TypeConvertorPhone
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "TbCustomer")
data class TbCustomer(
    @PrimaryKey
    @SerializedName("id") val id: Int?,
    @SerializedName("first_name") val first_name: String?,
    @SerializedName("last_name") val last_name: String?,
    @SerializedName("birth_date") val birth_date: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("enroll_to_loyalty") var enroll_to_loyalty: Boolean? = false,
    @SerializedName("same_as_billing_address") var same_as_billing_address: Boolean? = false,
    @SerializedName("final_reward") var final_reward: Int? = 0,
    @SerializedName("company") val company: String? = null,
    @TypeConverters(_root_ide_package_.com.pays.pos.data.typeconvert.TypeConvertorPhone::class)
    @SerializedName("phones") val phones: List<TbPhones> = listOf(),
    @TypeConverters(_root_ide_package_.com.pays.pos.data.typeconvert.TypeConvertorAddress::class)
    @SerializedName("addresses") val addresses: List<TbAddress> = listOf(),
    var isSelcted: Boolean = false
) : Parcelable {
    companion object {
        fun customerMapping(customer: OpenOrderResponse.Data.Order.Customer): TbCustomer {
            return TbCustomer(
                id = customer.id,
                first_name = customer.firstName,
                last_name = customer.lastName,
                birth_date = customer.birthDate,
                email = customer.email,
                enroll_to_loyalty = customer.enroll_to_loyalty,
                final_reward = customer.final_reward,
                company = customer.company,
            )
        }

        fun customerMapping(customer: GetOrderDetailsResponse.Data.Customer): TbCustomer {
            return TbCustomer(
                id = customer.id,
                first_name = customer.firstName,
                last_name = customer.lastName,
                birth_date = customer.birthDate,
                email = customer.email,
                same_as_billing_address = customer.same_as_billing_address,
                enroll_to_loyalty = customer.enroll_to_loyalty,
                final_reward = customer.final_reward,
                company = customer.company,
            )
        }

        fun customerMapping(customer: CustomerSearchList.Data): TbCustomer {
            return TbCustomer(
                id = customer.id,
                first_name = customer.first_name,
                last_name = customer.last_name,
                birth_date = customer.birth_date,
                email = customer.email,
                enroll_to_loyalty = customer.enroll_to_loyalty,
                same_as_billing_address = customer.same_as_billing_address,
                final_reward = customer.final_reward,
                company = customer.company,
                phones = customer.phones,
                addresses = customer.addresses
            )
        }

        fun customerMapping(customer: OnlineOrderResponseModel.Data.Customer): TbCustomer? {
            return TbCustomer(
                id = customer.id,
                first_name = customer.firstName,
                last_name = customer.lastName,
                birth_date = customer.birthDate,
                email = customer.email,
                enroll_to_loyalty = customer.enroll_to_loyalty,
                final_reward = customer.final_reward,
                company = customer.company,
            )
        }
    }
}
