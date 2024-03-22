package com.pays.pos.data.entities

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
@Keep
@Entity(tableName = "TbTax")
data class TaxData(
    @SerializedName("created_at")
    val createdAt: String?,
    @PrimaryKey
    @SerializedName("id")
    val id: Int,
    @SerializedName("location_id")
    var locationId: Int,
    @SerializedName("name")
    val name: String?,
    @SerializedName("rate")
    var rate: Double,
    @SerializedName("tax_type")
    var taxType: String?,
    @SerializedName("updated_at")
    val updatedAt: String?,
    @SerializedName("is_active")
    var isActive: Boolean = false,
    @SerializedName("is_default")
    val isDefault: Boolean,
    @SerializedName("is_custom_amount")
    val isCustomAmount: Boolean,
    @SerializedName("item_pricing")
    var itemPricing: String?,
    @SerializedName("item_ids")
    val itemIds: List<Int>,
    var orderTaxId: Int? = null,
    var isChecked: Boolean? = false,
    var totalTaxTypePrice: Double = 0.0,
    var subTotalAmount: Double = 0.0,
    var percentage_value: Double = 0.0,
    @SerializedName("is_deleted")
    var isDeleted: Boolean = false

) : Parcelable {
    fun showFormattedTaxRate() = String.format(
        "%.2f", rate ?: 0.0
    ) + "%"
}
