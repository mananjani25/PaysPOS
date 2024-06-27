package com.pays.pos.data.entities


import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.pays.pos.data.typeconvert.TCBusiness
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
@Keep
@Entity(tableName = "OrderTypeBackup")
class OrderTypeBackup : Parcelable {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
    var orderType: Int = 0
    var orderTypeName: String = ""
    var employeeId: Int = 0
}