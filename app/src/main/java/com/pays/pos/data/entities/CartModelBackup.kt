package com.pays.pos.data.entities

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.pays.pos.data.model.DineInModel
import com.pays.pos.data.typeconvert.TypeConvertersItems
import kotlinx.parcelize.Parcelize

@Parcelize
@Keep
@Entity(tableName = "CartModelBackup")
class CartModelBackup :Parcelable {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
    var data: String = ""
}