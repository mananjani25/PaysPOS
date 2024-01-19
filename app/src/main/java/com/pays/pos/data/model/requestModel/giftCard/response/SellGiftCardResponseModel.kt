package com.pays.pos.data.model.requestModel.giftCard.response

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class SellGiftCardResponseModel(
    @SerializedName("data")
    val `data`: Data? = null,
    val message: String,
    val status: Int,
    val type: String
){
    data class Data(
        @SerializedName("gift_card")
        val gift_card: GiftCard
    ): Parcelable {
        override fun describeContents(): Int {
            return 0
        }

        override fun writeToParcel(dest: Parcel?, flags: Int) {

        }

        companion object CREATOR : Parcelable.Creator<Data> {
            override fun createFromParcel(parcel: Parcel): Data {
                return Data(TODO("Order"))
            }

            override fun newArray(size: Int): Array<Data?> {
                return arrayOfNulls(size)
            }
        }
    }
}