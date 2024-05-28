package com.pays.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pays.pos.data.model.responseModel.GetKitchenReceiptSettingsResponse

@Dao
interface KitchenSettingsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(model: GetKitchenReceiptSettingsResponse.Data)

    @get:Query("select * from TbKitchenSettings")
    val getKitchenSettings: LiveData<GetKitchenReceiptSettingsResponse.Data>

    @Query("select * from TbKitchenSettings")
    suspend fun getKitchenSettingsNormalData(): GetKitchenReceiptSettingsResponse.Data

    @Query("DELETE FROM TbKitchenSettings")
    suspend fun delete()
}